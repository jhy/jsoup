package org.jsoup.internal;

import java.util.Locale;

/**
 * Util methods for normalizing strings. Jsoup internal use only, please don't depend on this API.
 */
public final class Normalizer {

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

    /** Minimal helper to get an otherwise OK HTML name like "foo<bar" to "foo_bar". */
    public static String xmlSafeTagName(final String tagname) {
        // todo - if required we could make a fuller version of this as in Attribute.getValidKey(syntax) in Element. for now, just minimal based on what HtmlTreeBuilder produces
        return tagname.replace('<', '_');
    }
}
