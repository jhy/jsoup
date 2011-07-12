package org.jsoup.helper;

import java.util.Collection;
import java.util.Iterator;

/**
 * A minimal String utility class. Designed for interal jsoup use only.
 */
public final class StringUtil {
    // memoised padding up to 10
    private static final String[] padding = {"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ", "         ", "          "};

    /**
     * Join a collection of strings by a seperator
     * @param strings collection of string objects
     * @param sep string to place between strings
     * @return joined string
     */
    public static String join(Collection strings, String sep) {
        return join(strings.iterator(), sep);
    }

    /**
     * Join a collection of strings by a seperator
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
     * Tests if a string is blank: null, emtpy, or only whitespace (" ", \r\n, \t, etc)
     * @param string string to test
     * @return if string is blank
     */
    public static boolean isBlank(String string) {
        if (string == null || string.length() == 0)
            return true;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            if (!Character.isWhitespace(string.codePointAt(i)))
                return false;
        }
        return true;
    }

    /**
     * Tests if a string is numeric, i.e. contains only digit characters
     * @param string string to test
     * @return true if only digit chars, false if empty or null or contains non-digit chrs
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

    public static String normaliseWhitespace(String string) {
        StringBuilder sb = new StringBuilder(string.length());

        boolean lastWasWhite = false;
        boolean modified = false;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            int c = string.codePointAt(i);
            if (Character.isWhitespace(c)) {
                if (lastWasWhite) {
                    modified = true;
                    continue;
                }
                if (c != ' ')
                    modified = true;
                sb.append(' ');
                lastWasWhite = true;
            }
            else {
                sb.appendCodePoint(c);
                lastWasWhite = false;
            }
        }
        return modified ? sb.toString() : string;
    }

    public static boolean in(String needle, String... haystack) {
        for (String hay : haystack) {
            if (hay.equals(needle))
            return true;
        }
        return false;
    }
}
