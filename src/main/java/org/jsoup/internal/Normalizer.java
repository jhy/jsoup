package org.jsoup.internal;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Util methods for normalizing strings. Jsoup internal use only, please don't depend on this API.
 */
//(Design smell)(Change bidirectional association to unidirectional (true positive))(cyclically-dependent)
public final class Normalizer {
    private static final Pattern INVALID_XML_CHARACTERS = Pattern.compile("[^a-zA-Z0-9-_]");

    /** Drops the input string to lower case. */
    public static String lowerCase(final String input) {
        return input != null ? input.toLowerCase(Locale.ENGLISH) : "";
    }

    /** Lower-cases and trims the input string. */
    public static String normalize(final String input) {
        return lowerCase(input).trim();
    }

    /** If a string literal, just lower case the string; otherwise lower-case and trim. */
    public static String normalize(final String input, boolean isStringLiteral) {
        return isStringLiteral ? lowerCase(input) : normalize(input);
    }

    /** Converts an HTML tag name into an XML-safe tag name by replacing invalid characters. */
    public static String xmlSafeTagName(final String tagName) {
        if (tagName == null || tagName.isEmpty()) {
            return "defaultTag"; // Fallback for empty tag names
        }
        return INVALID_XML_CHARACTERS.matcher(tagName).replaceAll("_"); // Replace invalid chars with "_"
    }
}
