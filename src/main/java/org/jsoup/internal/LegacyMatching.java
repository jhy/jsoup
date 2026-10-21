package org.jsoup.internal;

import org.jsoup.internal.CssMatching.AttributeOperator;
import org.jsoup.internal.CssMatching.TokenCase;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jspecify.annotations.Nullable;

import static org.jsoup.internal.Normalizer.asciiLowerCase;
import static org.jsoup.internal.Normalizer.lowerCase;
import static org.jsoup.internal.StringUtil.trimAsciiWhitespace;

/**
 Legacy class and attribute matching, with helpers for parsing and formatting attribute selectors.
 Public for access between jsoup packages; not a supported user API.
 Scheduled for removal with legacy query mode in 1.26.1.
 */
public final class LegacyMatching {
    private LegacyMatching() {
    }

    /**
     Detects a trailing {@code i} or {@code s} that could be parsed as an attribute-value case flag.
     */
    public static boolean hasAttributeFlag(String attribute) {
        String trimmed = trimAsciiWhitespace(attribute);
        int end = trimmed.length() - 1;
        if (end < 1) return false;
        char before = trimmed.charAt(end - 1);
        if (!StringUtil.isWhitespace(before) && before != '\'' && before != '"') return false;
        char flag = asciiLowerCase(trimmed.charAt(end));
        return flag == 'i' || flag == 's';
    }

    /**
     Formats a legacy attribute selector, retaining quotes when needed to distinguish its value from a case flag.
     */
    public static String attributeString(String key, String value, String operator, char quote) {
        String output = value;
        if (hasAttributeFlag(value)) {
            char delimiter = quote != 0 ? quote : value.indexOf('"') >= 0 ? '\'' : '"';
            output = delimiter + value + delimiter;
        }
        return String.format("[%s%s%s]", key, operator, output);
    }

    /**
     Gets an attribute value ignoring name case, including the {@code abs:} prefix.
     */
    public static @Nullable String attribute(Node node, String key) {
        key = asciiLowerCase(key);
        return node.hasAttr(key) ? node.attr(key) : null;
    }

    /**
     Matches an attribute using legacy case and empty-value rules.
     The comparison value must already be lower case.
     */
    public static boolean attributeMatches(Node node, String key, String value, AttributeOperator operator) {
        if (operator == AttributeOperator.NotEquals) return !value.equalsIgnoreCase(node.attr(key));
        String actual = attribute(node, key);
        if (actual == null) return false;
        switch (operator) {
            case Equals:
                return value.equalsIgnoreCase(actual);
            case StartsWith:
                return lowerCase(actual).startsWith(value);
            case EndsWith:
                return lowerCase(actual).endsWith(value);
            case Contains:
                return lowerCase(actual).contains(value);
            default:
                throw new IllegalArgumentException("Unknown legacy attribute operator: " + operator);
        }
    }

    /**
     Matches CSS attribute operators with legacy name lookup and default case rules.
     */
    public static boolean cssAttributeMatches(Node node, String key, String value, AttributeOperator operator,
                                              CssMatching.CaseSensitivity flag) {
        String actual = attribute(node, key);
        if (flag == CssMatching.CaseSensitivity.Default) {
            actual = actual == null ? null : lowerCase(actual);
            value = lowerCase(value);
        }
        return CssMatching.matchesAttributeValue(actual, value, operator, flag == CssMatching.CaseSensitivity.Insensitive);
    }

    /**
     Matches class names ignoring Unicode case. Also preserves {@code hasClass("a b")} matching {@code class="a b"}.
     */
    public static boolean hasClass(Element element, String name) {
        if (element.attributesSize() == 0 || name.isEmpty()) return false;
        String classes = element.attributes().getIgnoreCase("class");
        int len = classes.length(), wantLen = name.length();
        if (len < wantLen) return false;
        if (len == wantLen) return name.equalsIgnoreCase(classes);
        return CssMatching.matchesToken(classes, name, TokenCase.UnicodeInsensitive);
    }

}
