package org.jsoup.parser;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;

/**
 * Controls parser settings, to optionally preserve tag and/or attribute name case.
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

    static {
        htmlDefault = new ParseSettings(false, false);
        preserveCase = new ParseSettings(true, true);
    }

    private final boolean preserveTagCase;
    private final boolean preserveAttributeCase;

    /**
     * Define parse settings.
     * @param tag preserve tag case?
     * @param attribute preserve attribute name case?
     */
    public ParseSettings(boolean tag, boolean attribute) {
        preserveTagCase = tag;
        preserveAttributeCase = attribute;
    }

    String normalizeTag(String name) {
        name = name.trim();
        if (!preserveTagCase)
            name = name.toLowerCase();
        return name;
    }

    String normalizeAttribute(String name) {
        name = name.trim();
        if (!preserveAttributeCase)
            name = name.toLowerCase();
        return name;
    }

    Attributes normalizeAttributes(Attributes attributes) {
        if (!preserveAttributeCase) {
            for (Attribute attr : attributes) {
                attr.setKey(attr.getKey().toLowerCase());
            }
        }
        return attributes;
    }
}
