package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import javax.annotation.Nullable;
import static org.jsoup.internal.Normalizer.lowerCase;

/**
 * Controls parser case settings, to optionally preserve tag and/or attribute name case.
 */
public class ParseSettings {
    /**
     * HTML default settings: both tag and attribute names are lower-cased during parsing.
     */
    public static final ParseSettings htmlDefault;
    /**
     * Preserve both tag and attribute case.
     */
    public static final ParseSettings preserveCase;
    /**
     * Do not decode entities.
     */
    public static final ParseSettings preserveEntities;

    static {
        htmlDefault = new ParseSettings(false, false);
        preserveCase = new ParseSettings(true, true);
        preserveEntities = new ParseSettings(true, true, true);
    }

    private final boolean preserveTagCase;
    private final boolean preserveAttributeCase;
    private final boolean preserveHTMLEntities;

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
     * Returns true if preserving entities.
     */
    public boolean preserveHTMLEntities() {
        return preserveHTMLEntities;
    }

    /**
     * Define parse settings.
     * @param tag preserve tag case?
     * @param attribute preserve attribute name case?
     */
    public ParseSettings(boolean tag, boolean attribute) {
        this(tag, attribute, false);
    }

    /**
     * Define parse settings.
     * @param tag preserve tag case?
     * @param attribute preserve attribute name case?
     * @param preserveEntities do not encode or decode HTML entities
     */
    public ParseSettings(boolean tag, boolean attribute, boolean preserveEntities) {
        preserveTagCase = tag;
        preserveAttributeCase = attribute;
        preserveHTMLEntities = preserveEntities;
    }

    ParseSettings(ParseSettings copy) {
        this(copy.preserveTagCase, copy.preserveAttributeCase, copy.preserveHTMLEntities);
    }

    /**
     * Normalizes a tag name according to the case preservation setting.
     */
    public String normalizeTag(String name) {
        name = name.trim();
        if (!preserveTagCase)
            name = lowerCase(name);
        return name;
    }

    /**
     * Normalizes an attribute according to the case preservation setting.
     */
    public String normalizeAttribute(String name) {
        name = name.trim();
        if (!preserveAttributeCase)
            name = lowerCase(name);
        return name;
    }

    @Nullable Attributes normalizeAttributes(@Nullable Attributes attributes) {
        if (attributes != null && !preserveAttributeCase) {
            attributes.normalize();
        }
        return attributes;
    }

    /** Returns the normal name that a Tag will have (trimmed and lower-cased) */
    static String normalName(String name) {
        return lowerCase(name.trim());
    }
}
