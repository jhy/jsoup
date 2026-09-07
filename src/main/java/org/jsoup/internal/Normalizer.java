package org.jsoup.internal;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Util methods for normalizing strings. Jsoup internal use only, please don't depend on this API.
 * <p>ASCII case conversion changes only A-Z; Unicode conversion uses {@link Locale#ROOT}.
 * Neither trims, and the string lowercasing methods return an empty string for null input.</p>
 */
public final class Normalizer {

    /** Lowercases Unicode text. */
    public static String lowerCase(final @Nullable String input) {
        return input != null ? input.toLowerCase(Locale.ROOT) : "";
    }

    /** Lowercases ASCII letters. */
    public static String asciiLowerCase(final @Nullable String input) {
        if (input == null) return "";
        char[] chars = null;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            char lower = asciiLowerCase(c);
            if (c != lower) {
                if (chars == null) chars = input.toCharArray(); // set up on first change
                chars[i] = lower;
            }
        }
        return chars == null ? input : new String(chars);
    }

    /** Lowercases an ASCII letter. */
    public static char asciiLowerCase(char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c;
    }

    /** Compares strings ignoring ASCII case. */
    public static boolean equalsIgnoreAsciiCase(String first, @Nullable String second) {
        if (second == null || first.length() != second.length()) return false;
        for (int i = 0; i < first.length(); i++) {
            if (asciiLowerCase(first.charAt(i)) != asciiLowerCase(second.charAt(i))) return false;
        }
        return true;
    }
}
