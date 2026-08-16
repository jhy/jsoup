package org.jsoup.internal;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;

import java.util.Locale;

/**
 * Util methods for normalizing strings. Jsoup internal use only, please don't depend on this API.
 */
public final class Normalizer {

    /** Drops the input string to lower case. */
    public static String lowerCase(final String input) {
        return input != null ? input.toLowerCase(Locale.ROOT) : "";
    }

    /** Lower-cases and trims the input string. */
    public static String normalize(final String input) {
        return lowerCase(input).trim();
    }

    /**
     If a string literal, just lower case the string; otherwise lower-case and trim.
     @deprecated internal helper; replace with {@link #lowerCase(String)} for no-trim, or {@link #normalize(String)} for trim + lowercase.
     Will be removed in jsoup 1.24.1.
     */
    @Deprecated
    public static String normalize(final String input, boolean isStringLiteral) {
        return isStringLiteral ? lowerCase(input) : normalize(input);
    }

    /**
     * Gets an XML-safe tag name.
     * @deprecated Internal helper; use {@link Attribute#getValidKey(String, Document.OutputSettings.Syntax)}.
     * Will be removed in jsoup 1.24.1.
     */
    @Deprecated
    public static String xmlSafeTagName(final String tagName) {
        return Attribute.getValidKey(tagName, Document.OutputSettings.Syntax.xml);
    }
}
