package org.jsoup.internal;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jspecify.annotations.Nullable;

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
    @Nullable public static String xmlSafeTagName(final String tagname) {
        return Attribute.getValidKey(tagname, Document.OutputSettings.Syntax.xml); // Reuses the Attribute key normal, which is same for xml tag names
    }
}
