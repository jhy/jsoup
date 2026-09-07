package org.jsoup.parser;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Attributes;

import static org.jsoup.internal.Normalizer.asciiLowerCase;

/**
 * Controls parser case settings, to optionally preserve tag and/or attribute name case.
 * Case conversion uses ASCII rules.
 * Programmatic name normalization also trims surrounding ASCII whitespace.
 */
public class ParseSettings {
    /**
     * HTML defaults: lower-case tag and attribute names.
     */
    public static final ParseSettings htmlDefault;
    /**
     * Preserve both tag and attribute case.
     */
    public static final ParseSettings preserveCase;

    static {
        htmlDefault = new ParseSettings(false, false);
        preserveCase = new ParseSettings(true, true);
    }

    private final boolean preserveTagCase;
    private final boolean preserveAttributeCase;

    /**
     * Returns true if preserving tag name case.
     */
    public boolean preserveTagCase() {
        return preserveTagCase;
    }

    /**
     * Returns true if preserving attribute case.
     */
    public boolean preserveAttributeCase() {
        return preserveAttributeCase;
    }

    /**
     * Define parse settings.
     * @param tag preserve tag case?
     * @param attribute preserve attribute name case?
     */
    public ParseSettings(boolean tag, boolean attribute) {
        preserveTagCase = tag;
        preserveAttributeCase = attribute;
    }

    ParseSettings(ParseSettings copy) {
        this(copy.preserveTagCase, copy.preserveAttributeCase);
    }

    /**
     * Normalizes a tag name according to these settings.
     */
    public String normalizeTag(String name) {
        name = StringUtil.trimAsciiWhitespace(name);
        if (!preserveTagCase)
            name = asciiLowerCase(name);
        return name;
    }

    /**
     * Normalizes an attribute name according to these settings.
     */
    public String normalizeAttribute(String name) {
        name = StringUtil.trimAsciiWhitespace(name);
        if (!preserveAttributeCase)
            name = asciiLowerCase(name);
        return name;
    }

    void normalizeAttributes(Attributes attributes) {
        if (!preserveAttributeCase) {
            attributes.normalize();
        }
    }
}
