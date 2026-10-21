package org.jsoup.select;

import org.jsoup.helper.Regex;
import org.jsoup.helper.Validate;
import org.jsoup.internal.CssMatching.AttributeOperator;
import org.jsoup.internal.CssMatching.CaseSensitivity;
import org.jsoup.internal.LegacyMatching;
import org.jsoup.parser.TokenQueue;
import org.jspecify.annotations.Nullable;

import static org.jsoup.internal.Normalizer.asciiLowerCase;
import static org.jsoup.internal.StringUtil.trimAsciiWhitespace;

/**
 Parses attribute selectors in legacy query mode.
 Scheduled for removal with legacy mode in 1.26.1.
 */
final class LegacyQueryParser {
    private static final String[] AttributeOperators;
    static {
        AttributeOperator[] operators = AttributeOperator.values();
        AttributeOperators = new String[operators.length];
        for (int i = 0; i < operators.length; i++) AttributeOperators[i] = operators[i].symbol;
    }

    private LegacyQueryParser() {}

    /** Parses legacy attribute selectors, also accepting CSS case flags and {@code |=}. */
    static Evaluator attribute(TokenQueue tq, String query) {
        Evaluator css = tryCssAttribute(tq, query);
        if (css != null) return css;
        try (TokenQueue cq = new TokenQueue(tq.chompBalanced('[', ']'))) {
            return rawAttribute(cq, query);
        }
    }

    /** Tries CSS case flags or {@code |=}, leaving the query untouched if legacy parsing is needed. */
    private static @Nullable Evaluator tryCssAttribute(TokenQueue tq, String query) {
        String attribute;
        try {
            attribute = QueryParser.peekCssAttribute(query, tq.pos(), false);
        } catch (Selector.SelectorParseException e) {
            return null; // raw regexes may contain delimiters that aren't balanced CSS
        }
        try (TokenQueue probe = new TokenQueue(attribute)) {
            probe.consumeToAny(AttributeOperators);
            boolean dashPrefix = probe.matches("|=");
            if (probe.matches("~=") || !(dashPrefix || LegacyMatching.hasAttributeFlag(attribute))) return null;

            try (TokenQueue css = new TokenQueue(attribute)) {
                Evaluator eval = QueryParser.cssAttribute(css, false);
                if (dashPrefix || eval instanceof Evaluator.AttributeKeyPair &&
                    ((Evaluator.AttributeKeyPair) eval).flag != CaseSensitivity.Default) {
                    QueryParser.consumeAttribute(tq, attribute);
                    return eval;
                }
                // [attr= i] has no flag; preserve the leading space in its legacy value
            } catch (IllegalArgumentException e) {
                if (dashPrefix) throw e;
                // a trailing i or s may be part of an unquoted legacy value
            }
        }
        return null;
    }

    /** Parses legacy attribute names and values without decoding CSS escapes. */
    private static Evaluator rawAttribute(TokenQueue cq, String query) {
        String key = cq.consumeToAny(AttributeOperators); // eq, not, start, end, contain, match, (no val)
        key = trimAsciiWhitespace(key);
        Validate.notEmpty(key);
        Validate.isFalse(asciiLowerCase(key).equals("abs:"), "Absolute attribute key must have a name");
        cq.consumeWhitespace();
        final Evaluator eval;

        if (cq.isEmpty()) {
            if (key.startsWith("^"))
                eval = new Evaluator.AttributeStarting(key.substring(1));
            else if (key.equals("*")) // any attribute
                eval = new Evaluator.AttributeStarting("");
            else
                eval = new Evaluator.Attribute(key);
        } else {
            if (cq.matchChomp('='))
                eval = new Evaluator.AttributeWithValue(key, cq.remainder());
            else if (cq.matchChomp("!="))
                eval = new Evaluator.AttributeWithValueNot(key, cq.remainder());
            else if (cq.matchChomp("^="))
                eval = new Evaluator.AttributeWithValueStarting(key, cq.remainder());
            else if (cq.matchChomp("$="))
                eval = new Evaluator.AttributeWithValueEnding(key, cq.remainder());
            else if (cq.matchChomp("*="))
                eval = new Evaluator.AttributeWithValueContaining(key, cq.remainder());
            else if (cq.matchChomp("~="))
                eval = new Evaluator.AttributeWithValueMatching(key, Regex.compile(cq.remainder()));
            else
                throw new Selector.SelectorParseException(
                    "Could not parse attribute query '%s': unexpected token at '%s'", query, cq.remainder());
        }
        return eval;
    }
}
