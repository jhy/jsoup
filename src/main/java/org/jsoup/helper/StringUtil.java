package org.jsoup.helper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * A minimal String utility class. Designed for internal jsoup use only.
 */
public final class StringUtil {
    // memoised padding up to 21
    static final String[] padding = {"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ",
        "         ", "          ", "           ", "            ", "             ", "              ", "               ",
        "                ", "                 ", "                  ", "                   ", "                    "};

    /**
     * Join a collection of strings by a separator
     * @param strings collection of string objects
     * @param sep string to place between strings
     * @return joined string
     */
    public static String join(Collection strings, String sep) {
        return join(strings.iterator(), sep);
    }

    /**
     * Join a collection of strings by a separator
     * @param strings iterator of string objects
     * @param sep string to place between strings
     * @return joined string
     */
    public static String join(Iterator strings, String sep) {
        if (!strings.hasNext())
            return "";

        String start = strings.next().toString();
        if (!strings.hasNext()) // only one, avoid builder
            return start;

        StringBuilder sb = new StringBuilder(64).append(start);
        while (strings.hasNext()) {
            sb.append(sep);
            sb.append(strings.next());
        }
        return sb.toString();
    }

    /**
     * Returns space padding
     * @param width amount of padding desired
     * @return string of spaces * width
     */
    public static String padding(int width) {
        if (width < 0)
            throw new IllegalArgumentException("width must be > 0");

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
    public static boolean isBlank(String string) {
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

    /**
     * Normalise the whitespace within this string; multiple spaces collapse to a single, and all whitespace characters
     * (e.g. newline, tab) convert to a simple space
     * @param string content to normalise
     * @return normalised string
     */
    public static String normaliseWhitespace(String string) {
        StringBuilder sb = StringUtil.stringBuilder();
        appendNormalisedWhitespace(sb, string, false);
        return sb.toString();
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
            else {
                accum.appendCodePoint(c);
                lastWasWhite = false;
                reachedNonWhite = true;
            }
        }
    }

    public static boolean in(String needle, String... haystack) {
        for (String hay : haystack) {
            if (hay.equals(needle))
            return true;
        }
        return false;
    }

    public static boolean inSorted(String needle, String[] haystack) {
        return Arrays.binarySearch(haystack, needle) >= 0;
    }

    /**
     * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
     * @param base the existing absolute base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return the resolved absolute URL
     * @throws MalformedURLException if an error occurred generating the URL
     */
    public static URL resolve(URL base, String relUrl) throws MalformedURLException {
        // workaround: java resolves '//path/file + ?foo' to '//path/?foo', not '//path/file?foo' as desired
        if (relUrl.startsWith("?"))
            relUrl = base.getPath() + relUrl;
        // workaround: //example.com + ./foo = //example.com/./foo, not //example.com/foo
        if (relUrl.indexOf('.') == 0 && base.getFile().indexOf('/') != 0) {
            base = new URL(base.getProtocol(), base.getHost(), base.getPort(), "/" + base.getFile());
        }
        return new URL(base, relUrl);
    }

    /**
     * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
     * @param baseUrl the existing absolute base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return an absolute URL if one was able to be generated, or the empty string if not
     */
    public static String resolve(final String baseUrl, final String relUrl) {
        URL base;
        try {
            try {
                base = new URL(baseUrl);
            } catch (MalformedURLException e) {
                // the base is unsuitable, but the attribute/rel may be abs on its own, so try that
                URL abs = new URL(relUrl);
                return abs.toExternalForm();
            }
            return resolve(base, relUrl).toExternalForm();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    /**
     * Maintains a cached StringBuilder, to minimize new StringBuilder GCs. Prevents it from growing to big per thread.
     * Care must be taken to not grab more than one in the same stack (not locked or mutexed or anything).
     * @return an empty StringBuilder
     */
    public static StringBuilder stringBuilder() {
        StringBuilder sb = stringLocal.get();
        if (sb.length() > MaxCachedBuilderSize) {
            sb = new StringBuilder(MaxCachedBuilderSize);
            stringLocal.set(sb);
        } else {
            sb.delete(0, sb.length());
        }
        return sb;

    }

    private static final int MaxCachedBuilderSize = 8 * 1024;
    private static final ThreadLocal<StringBuilder> stringLocal = new ThreadLocal<StringBuilder>(){
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder(MaxCachedBuilderSize);
        }
    };


}
