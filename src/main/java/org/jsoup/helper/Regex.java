package org.jsoup.helper;

import org.jsoup.internal.SharedConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 A regular expression abstraction. Allows jsoup to optionally use the re2j regular expression engine (linear time)
 instead of the JDK's backtracking regex implementation.

 <p>If the {@code com.google.re2j} library is found on the classpath, by default it will be used. You can override this
 by setting {@code -Djsoup.useRe2j=false} to explicitly disable, and use the JDK regex engine.</p>

 <p>(Currently this a simplified implementation for jsoup's specific use; can extend as required.)</p>
 */
public class Regex {
    private static final boolean hasRe2j = hasRe2j();

    private final Pattern jdkPattern;

    Regex(Pattern jdkPattern) {
        this.jdkPattern = jdkPattern;
    }

    /**
     Compile a regex, using re2j if enabled and available; otherwise JDK regex.

     @param regex the regex to compile
     @return the compiled regex
     @throws ValidationException if the regex is invalid
     */
    public static Regex compile(String regex) {
        if (usingRe2j()) {
            return Re2jRegex.compile(regex);
        }

        try {
            return new Regex(Pattern.compile(regex));
        } catch (PatternSyntaxException e) {
            throw new ValidationException("Pattern syntax error: " + e.getMessage());
        }
    }

    /** Wraps an existing JDK Pattern (for API compat); doesn't switch */
    public static Regex fromPattern(Pattern pattern) {
        return new Regex(pattern);
    }

    /**
     Checks if re2j is available (on classpath) and enabled (via system property).
     @return true if re2j is available and enabled
     */
    public static boolean usingRe2j() {
        return hasRe2j && wantsRe2j();
    }

    static boolean wantsRe2j() {
        return Boolean.parseBoolean(System.getProperty(SharedConstants.UseRe2j, "true"));
    }

    static void wantsRe2j(boolean use) {
        System.setProperty(SharedConstants.UseRe2j, Boolean.toString(use));
    }

    static boolean hasRe2j() {
        try {
            Class<?> re2 = Class.forName("com.google.re2j.Pattern", false, Regex.class.getClassLoader()); // check if re2j is in classpath
            try {
                // if it is, and we are on JVM9+, we need to dork around with modules, because re2j doesn't publish a module name.
                // done via reflection so we can still run on JVM 8.
                // todo remove if re2j publishes as a module
                Class<?> moduleCls = Class.forName("java.lang.Module");
                Method getModule = Class.class.getMethod("getModule");
                Object jsoupMod = getModule.invoke(Regex.class);
                Object re2Mod = getModule.invoke(re2);
                boolean reads = (boolean) moduleCls.getMethod("canRead", moduleCls).invoke(jsoupMod, re2Mod);
                if (!reads) moduleCls.getMethod("addReads", moduleCls).invoke(jsoupMod, re2Mod);
            } catch (ClassNotFoundException ignore) {
                // jvm8 - no Module class; so we can use as-is
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false; // no re2j
        } catch (ReflectiveOperationException e) {
            // unexpectedly couldn’t wire modules on 9+; return false to avoid IllegalAccessError later
            System.err.println("Warning: (bug? please report) couldn't access re2j from jsoup due to modules: " + e);
            return false;
        }
    }

    public Matcher matcher(CharSequence input) {
        return new JdkMatcher(jdkPattern.matcher(input));
    }

    @Override
    public String toString() {
        return jdkPattern.toString();
    }

    public interface Matcher {
        boolean find();
    }

    private static final class JdkMatcher implements Matcher {
        private final java.util.regex.Matcher delegate;

        JdkMatcher(java.util.regex.Matcher delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean find() {
            return delegate.find();
        }
    }
}
