package org.jsoup.internal;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.Parser;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.jsoup.internal.Normalizer.asciiLowerCase;
import static org.jsoup.internal.Normalizer.equalsIgnoreAsciiCase;

/**
 Shares CSS matching rules between selectors and class checks.
 Public for access between jsoup packages; not a supported user API.
 */
public final class CssMatching {
    private static final String[] InsensitiveAttributes = {
        "accept", "accept-charset", "align", "alink", "axis", "bgcolor", "charset", "checked", "clear", "codetype",
        "color",
        "compact", "declare", "defer", "dir", "direction", "disabled", "enctype", "face", "frame", "hreflang",
        "http-equiv",
        "lang", "language", "link", "media", "method", "multiple", "nohref", "noresize", "noshade", "nowrap",
        "readonly",
        "rel", "rev", "rules", "scope", "scrolling",
        "selected", "shape", "target", "text", "type", "valign", "valuetype", "vlink"
    };

    private CssMatching() {
    }

    private static final ThreadLocal<@Nullable DocumentLookup> CurrentDocument = new ThreadLocal<>();

    /**
     Caches the query context's document to avoid repeated ancestor walks. Lookup happens on first evaluation so lazy
     queries use the current document. Restoring the previous lookup after each test supports nested queries.
     */
    public static <T> Predicate<T> withDocument(Node context, Predicate<T> predicate) {
        AtomicReference<@Nullable DocumentLookup> lookup = new AtomicReference<>();
        return candidate -> {
            DocumentLookup active = lookup.get();
            if (active == null) {
                lookup.compareAndSet(null, new DocumentLookup(context, context.ownerDocument()));
                active = lookup.get();
            }
            DocumentLookup previous = CurrentDocument.get();
            CurrentDocument.set(active);
            try {
                return predicate.test(candidate);
            } finally {
                CurrentDocument.set(previous);
            }
        };
    }

    /**
     Reuses the cached document only for the same query context.
     */
    private static @Nullable Document document(Node context) {
        DocumentLookup lookup = CurrentDocument.get();
        return lookup != null && lookup.context == context ? lookup.document : context.ownerDocument();
    }

    /**
     Caches the document only; parser and quirks settings are read as needed.
     */
    private static final class DocumentLookup {
        final Node context;
        final @Nullable Document document;

        DocumentLookup(Node context, @Nullable Document document) {
            this.context = context;
            this.document = document;
        }
    }

    /**
     Uses the context document's parser and the node's namespace to choose HTML rules; detached trees use the namespace.
     */
    public static boolean isHtmlElement(Node context, Node node) {
        if (!(node instanceof Element) || !((Element) node).tag().namespace().equals(Parser.NamespaceHtml))
            return false;
        Document doc = document(context);
        return doc == null || doc.parser().getTreeBuilder() instanceof HtmlTreeBuilder;
    }

    /**
     Checks whether the node's document uses quirks mode.
     */
    public static boolean isQuirksMode(Node node) {
        Document doc = document(node);
        return doc != null && doc.quirksMode() == Document.QuirksMode.quirks;
    }

    /**
     Attribute value case flags: Default uses document rules, Insensitive represents i, and Sensitive represents s.
     */
    public enum CaseSensitivity {Default, Insensitive, Sensitive}

    /**
     Token comparison rules. UnicodeInsensitive is only needed for legacy class matching.
     */
    public enum TokenCase {Sensitive, AsciiInsensitive, UnicodeInsensitive}

    /**
     Attribute selector operators and their CSS syntax.
     */
    public enum AttributeOperator {
        Equals("="), NotEquals("!="), StartsWith("^="), EndsWith("$="), Contains("*="),
        ContainsToken("~="), DashMatch("|=");

        public final String symbol;

        AttributeOperator(String symbol) {
            this.symbol = symbol;
        }
    }

    /**
     Gets an attribute value using HTML or XML name rules, resolving {@code abs:} URLs when needed.
     */
    public static @Nullable String attributeValue(Node node, String key, boolean html) {
        String value = rawAttributeValue(node, key, html);
        // a nonempty literal abs:href attribute takes precedence over resolving href
        if (!key.startsWith("abs:") || value != null && !value.isEmpty()) return value;
        String relative = rawAttributeValue(node, key.substring(4), html);
        if (relative == null) return value;
        String absolute = StringUtil.resolve(node.baseUri(), relative);
        return absolute.isEmpty() ? value : absolute;
    }

    /**
     Looks up a literal attribute name without interpreting the abs: prefix.
     */
    private static @Nullable String rawAttributeValue(Node node, String key, boolean html) {
        if (node.attributesSize() == 0) return null;
        for (Attribute attr : node.attributes()) {
            boolean htmlName = html && attr.namespace().isEmpty();
            if (htmlName ? equalsIgnoreAsciiCase(attr.getKey(), key) : attr.getKey().equals(key))
                return attr.getValue();
        }
        return null;
    }

    /**
     Identifies HTML attributes whose values ignore ASCII case by default.
     */
    public static boolean isCaseInsensitiveAttribute(String key) {
        return StringUtil.inSorted(asciiLowerCase(key), InsensitiveAttributes);
    }

    /**
     Matches an attribute value, optionally ignoring ASCII case.
     */
    public static boolean matchesAttributeValue(@Nullable String actual, String value, AttributeOperator operator,
                                                boolean ignoreCase) {
        if (operator == AttributeOperator.NotEquals)
            return !matchesAttributeValue(actual, value, AttributeOperator.Equals, ignoreCase);
        if (actual == null) return false;
        if (ignoreCase) {
            actual = asciiLowerCase(actual);
            value = asciiLowerCase(value);
        }
        switch (operator) {
            case Equals:
                return actual.equals(value);
            case StartsWith:
                return !value.isEmpty() && actual.startsWith(value);
            case EndsWith:
                return !value.isEmpty() && actual.endsWith(value);
            case Contains:
                return !value.isEmpty() && actual.contains(value);
            case DashMatch:
                return actual.equals(value) || actual.startsWith(value + "-");
            case ContainsToken:
                return matchesToken(actual, value, TokenCase.Sensitive);
            default:
                throw new IllegalArgumentException("Unknown attribute operator: " + operator);
        }
    }

    /**
     Matches a complete token separated by ASCII whitespace.
     */
    public static boolean matchesToken(String tokens, String value, TokenCase tokenCase) {
        int len = tokens.length(), wantLen = value.length();
        if (wantLen == 0 || len < wantLen) return false;
        if (len == wantLen) {
            // the whole-string shortcut must still require a single token
            for (int i = 0; i < wantLen; i++) if (StringUtil.isWhitespace(value.charAt(i))) return false;
            if (tokenCase == TokenCase.Sensitive) return value.equals(tokens);
            if (tokenCase == TokenCase.UnicodeInsensitive) return value.equalsIgnoreCase(tokens);
            return matchesAsciiRegion(tokens, 0, value);
        }

        boolean unicodeInsensitive = tokenCase == TokenCase.UnicodeInsensitive;
        for (int i = 0; i < len; ) {
            while (i < len && StringUtil.isWhitespace(tokens.charAt(i))) i++;
            int start = i;
            if (start == len) return false;
            while (i < len && !StringUtil.isWhitespace(tokens.charAt(i))) i++;
            if (i - start == wantLen) {
                boolean matches = tokenCase == TokenCase.AsciiInsensitive ? matchesAsciiRegion(tokens, start, value) :
                    tokens.regionMatches(unicodeInsensitive, start, value, 0, wantLen);
                if (matches) return true;
            }
        }
        return false;
    }

    /**
     Compares a token region ignoring ASCII case, without folding Unicode letters.
     */
    private static boolean matchesAsciiRegion(String tokens, int start, String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Normalizer.asciiLowerCase(tokens.charAt(start + i)) != Normalizer.asciiLowerCase(value.charAt(i)))
                return false;
        }
        return true;
    }

    /**
     Matches a complete class token using the element's document rules.
     */
    public static boolean hasClass(Element element, String name) {
        return hasClass(element, element, name);
    }

    /**
     Matches class names using the query context's HTML/XML and quirks-mode rules.
     */
    public static boolean hasClass(Node context, Element element, String name) {
        if (element.attributesSize() == 0 || name.isEmpty()) return false;
        String classes = isHtmlElement(context, element) ?
            element.attributes().getIgnoreCase("class") : element.attributes().get("class");
        return matchesToken(classes, name, isQuirksMode(context) ? TokenCase.AsciiInsensitive : TokenCase.Sensitive);
    }

    /**
     Checks whether two elements have the same namespace and local name.
     */
    public static boolean sameType(Node first, Element second) {
        if (!(first instanceof Element)) return false;
        Element element = (Element) first;
        return element.tag().namespace().equals(second.tag().namespace()) && element.tag().localName().equals(second.tag().localName());
    }

}
