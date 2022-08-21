package org.jsoup.internal;

import org.jsoup.helper.Validate;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 A minimal String utility class. Designed for <b>internal</b> jsoup use only - the API and outcome may change without
 notice.
 */
public final class StringUtil {
    // memoised padding up to 21 (blocks 0 to 20 spaces)
    static final String[] padding = {"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ",
        "         ", "          ", "           ", "            ", "             ", "              ", "               ",
        "                ", "                 ", "                  ", "                   ", "                    "};

    /**
     * Join a collection of strings by a separator
     * @param strings collection of string objects
     * @param sep string to place between strings
     * @return joined string
     */
    public static String join(Collection<?> strings, String sep) {
        return join(strings.iterator(), sep);
    }

    /**
     * Join a collection of strings by a separator
     * @param strings iterator of string objects
     * @param sep string to place between strings
     * @return joined string
     */
    public static String join(Iterator<?> strings, String sep) {
        if (!strings.hasNext())
            return "";

        String start = strings.next().toString();
        if (!strings.hasNext()) // only one, avoid builder
            return start;

        StringJoiner j = new StringJoiner(sep);
        j.add(start);
        while (strings.hasNext()) {
            j.add(strings.next());
        }
        return j.complete();
    }

    /**
     * Join an array of strings by a separator
     * @param strings collection of string objects
     * @param sep string to place between strings
     * @return joined string
     */
    public static String join(String[] strings, String sep) {
        return join(Arrays.asList(strings), sep);
    }

    /**
     A StringJoiner allows incremental / filtered joining of a set of stringable objects.
     @since 1.14.1
     */
    public static class StringJoiner {
        @Nullable StringBuilder sb = borrowBuilder(); // sets null on builder release so can't accidentally be reused
        final String separator;
        boolean first = true;

        /**
         Create a new joiner, that uses the specified separator. MUST call {@link #complete()} or will leak a thread
         local string builder.

         @param separator the token to insert between strings
         */
        public StringJoiner(String separator) {
            this.separator = separator;
        }

        /**
         Add another item to the joiner, will be separated
         */
        public StringJoiner add(Object stringy) {
            Validate.notNull(sb); // don't reuse
            if (!first)
                sb.append(separator);
            sb.append(stringy);
            first = false;
            return this;
        }

        /**
         Append content to the current item; not separated
         */
        public StringJoiner append(Object stringy) {
            Validate.notNull(sb); // don't reuse
            sb.append(stringy);
            return this;
        }

        /**
         Return the joined string, and release the builder back to the pool. This joiner cannot be reused.
         */
        public String complete() {
            String string = releaseBuilder(sb);
            sb = null;
            return string;
        }
    }

    /**
     * Returns space padding (up to the default max of 30). Use {@link #padding(int, int)} to specify a different limit.
     * @param width amount of padding desired
     * @return string of spaces * width
     * @see #padding(int, int) 
      */
    public static String padding(int width) {
        return padding(width, 30);
    }

    /**
     * Returns space padding, up to a max of maxPaddingWidth.
     * @param width amount of padding desired
     * @param maxPaddingWidth maximum padding to apply. Set to {@code -1} for unlimited.
     * @return string of spaces * width
     */
    public static String padding(int width, int maxPaddingWidth) {
        Validate.isTrue(width >= 0, "width must be >= 0");
        Validate.isTrue(maxPaddingWidth >= -1);
        if (maxPaddingWidth != -1)
            width = Math.min(width, maxPaddingWidth);
        if (width < padding.length)
            return padding[width];        
        char[] out = new char[width];
        for (int i = 0; i < width; i++)
            out[i] = ' ';
        return String.valueOf(out);
    }

    /**
     * Tests if a string is blank: null, empty, or only whitespace (" ", \r\n, \t, etc)
     * @param string string to test
     * @return if string is blank
     */
    public static boolean isBlank(final String string) {
        if (string == null || string.length() == 0)
            return true;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            if (!StringUtil.isWhitespace(string.codePointAt(i)))
                return false;
        }
        return true;
    }

    /**
     Tests if a string starts with a newline character
     @param string string to test
     @return if its first character is a newline
     */
    public static boolean startsWithNewline(final String string) {
        if (string == null || string.length() == 0)
            return false;
        return string.charAt(0) == '\n';
    }

    /**
     * Tests if a string is numeric, i.e. contains only digit characters
     * @param string string to test
     * @return true if only digit chars, false if empty or null or contains non-digit chars
     */
    public static boolean isNumeric(String string) {
        if (string == null || string.length() == 0)
            return false;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            if (!Character.isDigit(string.codePointAt(i)))
                return false;
        }
        return true;
    }

    /**
     * Tests if a code point is "whitespace" as defined in the HTML spec. Used for output HTML.
     * @param c code point to test
     * @return true if code point is whitespace, false otherwise
     * @see #isActuallyWhitespace(int)
     */
    public static boolean isWhitespace(int c){
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Tests if a code point is "whitespace" as defined by what it looks like. Used for Element.text etc.
     * @param c code point to test
     * @return true if code point is whitespace, false otherwise
     */
    public static boolean isActuallyWhitespace(int c){
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == 160;
        // 160 is &nbsp; (non-breaking space). Not in the spec but expected.
    }

    public static boolean isInvisibleChar(int c) {
        return c == 8203 || c == 173; // zero width sp, soft hyphen
        // previously also included zw non join, zw join - but removing those breaks semantic meaning of text
    }

    /**
     * Normalise the whitespace within this string; multiple spaces collapse to a single, and all whitespace characters
     * (e.g. newline, tab) convert to a simple space.
     * @param string content to normalise
     * @return normalised string
     */
    public static String normaliseWhitespace(String string) {
        StringBuilder sb = StringUtil.borrowBuilder();
        appendNormalisedWhitespace(sb, string, false);
        return StringUtil.releaseBuilder(sb);
    }

    /**
     * After normalizing the whitespace within a string, appends it to a string builder.
     * @param accum builder to append to
     * @param string string to normalize whitespace within
     * @param stripLeading set to true if you wish to remove any leading whitespace
     */
    public static void appendNormalisedWhitespace(StringBuilder accum, String string, boolean stripLeading) {
        boolean lastWasWhite = false;
        boolean reachedNonWhite = false;

        int len = string.length();
        int c;
        for (int i = 0; i < len; i+= Character.charCount(c)) {
            c = string.codePointAt(i);
            if (isActuallyWhitespace(c)) {
                if ((stripLeading && !reachedNonWhite) || lastWasWhite)
                    continue;
                accum.append(' ');
                lastWasWhite = true;
            }
            else if (!isInvisibleChar(c)) {
                accum.appendCodePoint(c);
                lastWasWhite = false;
                reachedNonWhite = true;
            }
        }
    }

    public static boolean in(final String needle, final String... haystack) {
        final int len = haystack.length;
        for (int i = 0; i < len; i++) {
            if (haystack[i].equals(needle))
            return true;
        }
        return false;
    }

    public static boolean inSorted(String needle, String[] haystack) {
        return Arrays.binarySearch(haystack, needle) >= 0;
    }

    /**
     Tests that a String contains only ASCII characters.
     @param string scanned string
     @return true if all characters are in range 0 - 127
     */
    public static boolean isAscii(String string) {
        Validate.notNull(string);
        for (int i = 0; i < string.length(); i++) {
            int c = string.charAt(i);
            if (c > 127) { // ascii range
                return false;
            }
        }
        return true;
    }

    private static final Pattern extraDotSegmentsPattern = Pattern.compile("^/((\\.{1,2}/)+)");
    /**
     * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
     * @param base the existing absolute base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return the resolved absolute URL
     * @throws MalformedURLException if an error occurred generating the URL
     */
    public static URL resolve(URL base, String relUrl) throws MalformedURLException {
        relUrl = stripControlChars(relUrl);
        // workaround: java resolves '//path/file + ?foo' to '//path/?foo', not '//path/file?foo' as desired
        if (relUrl.startsWith("?"))
            relUrl = base.getPath() + relUrl;
        // workaround: //example.com + ./foo = //example.com/./foo, not //example.com/foo
        URL url = new URL(base, relUrl);
        String fixedFile = extraDotSegmentsPattern.matcher(url.getFile()).replaceFirst("/");
        if (url.getRef() != null) {
            fixedFile = fixedFile + "#" + url.getRef();
        }
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), fixedFile);
    }

    /**
     * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
     * @param baseUrl the existing absolute base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return an absolute URL if one was able to be generated, or the empty string if not
     */
    public static String resolve(String baseUrl, String relUrl) {
        // workaround: java will allow control chars in a path URL and may treat as relative, but Chrome / Firefox will strip and may see as a scheme. Normalize to browser's view.
        baseUrl = stripControlChars(baseUrl); relUrl = stripControlChars(relUrl);
        try {
            URL base;
            try {
                base = new URL(baseUrl);
            } catch (MalformedURLException e) {
                // the base is unsuitable, but the attribute/rel may be abs on its own, so try that
                URL abs = new URL(relUrl);
                return abs.toExternalForm();
            }
            return resolve(base, relUrl).toExternalForm();
        } catch (MalformedURLException e) {
            // it may still be valid, just that Java doesn't have a registered stream handler for it, e.g. tel
            // we test here vs at start to normalize supported URLs (e.g. HTTP -> http)
            return validUriScheme.matcher(relUrl).find() ? relUrl : "";
        }
    }
    private static final Pattern validUriScheme = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+-.]*:");

    private static final Pattern controlChars = Pattern.compile("[\\x00-\\x1f]*"); // matches ascii 0 - 31, to strip from url
    private static String stripControlChars(final String input) {
        return controlChars.matcher(input).replaceAll("");
    }

    private static final ThreadLocal<Stack<StringBuilder>> threadLocalBuilders = new ThreadLocal<Stack<StringBuilder>>() {
        @Override
        protected Stack<StringBuilder> initialValue() {
            return new Stack<>();
        }
    };

    /**
     * Maintains cached StringBuilders in a flyweight pattern, to minimize new StringBuilder GCs. The StringBuilder is
     * prevented from growing too large.
     * <p>
     * Care must be taken to release the builder once its work has been completed, with {@link #releaseBuilder}
     * @return an empty StringBuilder
     */
    public static StringBuilder borrowBuilder() {
        Stack<StringBuilder> builders = threadLocalBuilders.get();
        return builders.empty() ?
            new StringBuilder(MaxCachedBuilderSize) :
            builders.pop();
    }

    /**
     * Release a borrowed builder. Care must be taken not to use the builder after it has been returned, as its
     * contents may be changed by this method, or by a concurrent thread.
     * @param sb the StringBuilder to release.
     * @return the string value of the released String Builder (as an incentive to release it!).
     */
    public static String releaseBuilder(StringBuilder sb) {
        Validate.notNull(sb);
        String string = sb.toString();

        if (sb.length() > MaxCachedBuilderSize)
            sb = new StringBuilder(MaxCachedBuilderSize); // make sure it hasn't grown too big
        else
            sb.delete(0, sb.length()); // make sure it's emptied on release

        Stack<StringBuilder> builders = threadLocalBuilders.get();
        builders.push(sb);

        while (builders.size() > MaxIdleBuilders) {
            builders.pop();
        }
        return string;
    }

    private static final int MaxCachedBuilderSize = 8 * 1024;
    private static final int MaxIdleBuilders = 8;
}
