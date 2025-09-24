package org.jsoup.helper;

import org.jsoup.internal.SharedConstants;

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
        if (hasRe2j && wantsRe2j()) {
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

    static boolean wantsRe2j() {
        return Boolean.parseBoolean(System.getProperty(SharedConstants.UseRe2j, "true"));
    }

    static void wantsRe2j(boolean use) {
        System.setProperty(SharedConstants.UseRe2j, Boolean.toString(use));
    }

    static boolean hasRe2j() {
        try {
            Class.forName("com.google.re2j.Pattern", false, Regex.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
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
