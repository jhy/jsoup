package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.internal.LegacyMatching;
import org.jsoup.internal.CssMatching;
import org.jsoup.internal.CssMatching.AttributeOperator;
import org.jsoup.internal.CssMatching.CaseSensitivity;
import org.jsoup.internal.QueryMode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.ProcessingInstruction;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.helper.Regex;
import org.jsoup.parser.TokenQueue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.jsoup.internal.Normalizer.lowerCase;
import static org.jsoup.internal.Normalizer.asciiLowerCase;
import static org.jsoup.internal.StringUtil.trimAsciiWhitespace;
import static org.jsoup.internal.StringUtil.normaliseWhitespace;
import static org.jsoup.internal.StringUtil.isWhitespace;


/**
 An Evaluator tests if an element (or a node) meets the selector's requirements. Obtain an evaluator for a given CSS selector
 with {@link Selector#evaluatorOf(String css)}. If you are executing the same selector on many elements (or documents), it
 can be more efficient to compile and reuse an Evaluator than to reparse the selector on each invocation of select().
 <p>Each evaluator captures the behavior selected by {@code jsoup.queryMode} when constructed.
 Standalone evaluators use legacy behavior in {@code check} mode; they do not report compatibility differences.</p>
 <p>If you construct and combine evaluators yourself, each child keeps the matching rules selected when it was created.
 The outer evaluator determines whether searches include the starting element and which element supplies the query context.</p>
 <p>Evaluators are thread-safe and may be used concurrently across multiple documents.</p>

 @see <a href="{@docRoot}/org/jsoup/select/Selector.html#query-compatibility">Query compatibility changes</a> */
public abstract class Evaluator {
    final boolean web; // web query semantics or legacy

    private static final ThreadLocal<@Nullable Boolean> QueryModeOverride = new ThreadLocal<>();

    /**
     Sets the mode for evaluators created by the action, then restores the previous mode.
     */
    static <T> T withMode(boolean web, Supplier<T> action) {
        Boolean previous = QueryModeOverride.get();
        QueryModeOverride.set(web);
        try {
            return action.get();
        } finally {
            if (previous == null) QueryModeOverride.remove();
            else QueryModeOverride.set(previous);
        }
    }

    /**
     Returns the supplied element. If it is a Document, returns its first element child instead,
     or null if it has no element children.
     */
    static @Nullable Element documentElementOrSelf(Element element) {
        return element instanceof Document ? element.firstElementChild() : element;
    }

    protected Evaluator() {
        Boolean web = QueryModeOverride.get();
        this.web = web != null ? web : QueryMode.current() == QueryMode.Web;
    }

    /**
     Provides a Predicate for this Evaluator, matching the test Element.
     * @param root the element the query starts from; see {@link #matches(Element, Element)}
     * @return a predicate that accepts an Element to test for matches with this Evaluator
     * @since 1.17.1
     */
    public Predicate<Element> asPredicate(Element root) {
        return element -> matches(root, element);
    }

    Predicate<Node> asNodePredicate(Element root) {
        return node -> matches(root, node);
    }

    /**
     Tests whether the element matches this evaluator.

     @param root    the query context, used by {@code :scope}; for a Document, {@code :scope} matches its document element
     @param element the element to test
     @return whether the element matches
     */
    public abstract boolean matches(Element root, Element element);

    /**
     Tests a {@code :has()} candidate starting at {@code anchor}, keeping {@code root} as the query context.
     The caller has already checked whether the query accepts this node type.
     */
    boolean matchesRelative(Element root, Node node, Element anchor) {
        return node instanceof Element ? matches(root, (Element) node) : matches(root, (LeafNode) node);
    }

    /** Checks node eligibility before relative matching; a null anchor uses ordinary matching. */
    final boolean matches(Element root, Node node, @Nullable Element anchor) {
        if (anchor == null) return matches(root, node);
        if (node instanceof Element || node instanceof LeafNode && wantsNodes())
            return matchesRelative(root, node, anchor);
        return false;
    }

    final boolean matches(Element root, Node node) {
        if (node instanceof Element) {
            return matches(root, (Element) node);
        } else if (node instanceof LeafNode && wantsNodes()) {
            return matches(root, (LeafNode) node);
        }
        return false;
    }

    boolean matches(Element root, LeafNode leafNode) {
        return false;
    }

    boolean wantsNodes() {
        return false;
    }

    /**
     Reset any internal state in this Evaluator before executing a new Collector evaluation.
     */
    protected void reset() {
    }

    final boolean sameType(Node first, Element second) {
        return web ? CssMatching.sameType(first, second) : first.normalName().equals(second.normalName());
    }

    /**
     A relative evaluator cost function. During evaluation, Evaluators are sorted by ascending cost as an optimization.
     * @return the relative cost of this Evaluator
     */
    protected int cost() {
        return 5; // a nominal default cost
    }

    /**
     * Evaluator for tag name
     */
    public static final class Tag extends Evaluator {
        private final String tagName;

        public Tag(String tagName) {
            this.tagName = web ? tagName : asciiLowerCase(tagName);
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (!web) return element.nameIs(tagName);
            return !CssMatching.isHtmlElement(root, element) ? element.tagName().equals(tagName) :
                element.nameIs(asciiLowerCase(tagName));
        }

        @Override protected int cost() {
            return 1;
        }

        @Override
        public String toString() {
            return String.format("%s", tagName);
        }
    }

    /**
     * Evaluator for tag name that starts with prefix; used for ns|*
     */
    public static final class TagStartsWith extends Evaluator {
        private final String tagName;

        public TagStartsWith(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public boolean matches(Element root, Element element) {
            boolean insensitive = !web || CssMatching.isHtmlElement(root, element);
            String name = insensitive ? element.normalName() : element.tagName();
            return name.startsWith(insensitive ? asciiLowerCase(tagName) : tagName);
        }

        @Override
        public String toString() {
            return String.format("%s|*", tagName);
        }
    }


    /**
     * Evaluator for tag name that ends with suffix; used for *|el
     */
    public static final class TagEndsWith extends Evaluator {
        private final String tagName;

        public TagEndsWith(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public boolean matches(Element root, Element element) {
            boolean insensitive = !web || CssMatching.isHtmlElement(root, element);
            String name = insensitive ? element.normalName() : element.tagName();
            return name.endsWith(insensitive ? asciiLowerCase(tagName) : tagName);
        }

        @Override
        public String toString() {
            return String.format("*|%s", tagName);
        }
    }

    /**
     * Evaluator for element id
     */
    public static final class Id extends Evaluator {
        private final String id;

        public Id(String id) {
            this.id = id;
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (!web) return id.equals(element.id());
            String actual = CssMatching.attributeValue(element, "id", CssMatching.isHtmlElement(root, element));
            return actual != null && (CssMatching.isQuirksMode(root) ? org.jsoup.internal.Normalizer.equalsIgnoreAsciiCase(actual, id) : actual.equals(id));
        }

        @Override protected int cost() {
            return 2;
        }
        @Override
        public String toString() {
            return String.format("#%s", id);
        }
    }

    /**
     * Evaluator for element class
     */
    public static final class Class extends Evaluator {
        private final String className;

        public Class(String className) {
            this.className = className;
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (!web) return LegacyMatching.hasClass(element, className);
            return CssMatching.hasClass(root, element, className);
        }

        @Override protected int cost() {
            return 8; // does whitespace scanning; more than .contains()
        }

        @Override
        public String toString() {
            return String.format(".%s", className);
        }

    }

    /** Shares attribute matching between elements and processing instructions. */
    abstract static class AttributeEvaluator extends Evaluator {
        @Override public boolean matches(Element root, Element element) {
            return matchesNode(root, element);
        }

        @Override boolean matches(Element root, LeafNode leafNode) {
            return leafNode instanceof ProcessingInstruction && matchesNode(root, leafNode);
        }

        /** Tests this evaluator against a node's attributes. */
        boolean matchesNode(Element root, Node node) {
            return false;
        }

        @Nullable String attribute(Element root, Node node, String key) {
            return web ? CssMatching.attributeValue(node, key, CssMatching.isHtmlElement(root, node)) :
                LegacyMatching.attribute(node, key);
        }

        boolean matchesValue(Element root, Node node, String key, String value, AttributeOperator operator,
                             CaseSensitivity flag) {
            boolean ignoreCase = flag == CaseSensitivity.Insensitive || flag == CaseSensitivity.Default &&
                CssMatching.isHtmlElement(root, node) && CssMatching.isCaseInsensitiveAttribute(key);
            return CssMatching.matchesAttributeValue(attribute(root, node, key), value, operator, ignoreCase);
        }
    }

    /**
     * Evaluator for attribute name matching
     */
    public static final class Attribute extends AttributeEvaluator {
        private final String key;

        public Attribute(String key) {
            this.key = key;
        }

        @Override boolean matchesNode(Element root, Node node) {
            return web ? attribute(root, node, key) != null : node.hasAttr(asciiLowerCase(key));
        }

        @Override protected int cost() {
            return 2;
        }

        @Override
        public String toString() {
            return String.format("[%s]", key);
        }
    }

    /**
     * Evaluator for attribute name prefix matching
     */
    public static final class AttributeStarting extends AttributeEvaluator {
        private final String keyPrefix;

        public AttributeStarting(String keyPrefix) {
            Validate.notNull(keyPrefix); // OK to be empty - will find elements with any attributes
            this.keyPrefix = keyPrefix;
        }

        @Override boolean matchesNode(Element root, Node node) {
            List<org.jsoup.nodes.Attribute> values = node.attributes().asList();
            for (org.jsoup.nodes.Attribute attribute : values) {
                boolean insensitive = !web || CssMatching.isHtmlElement(root, node) && attribute.namespace().isEmpty();
                String name = insensitive ? asciiLowerCase(attribute.getKey()) : attribute.getKey();
                if (name.startsWith(insensitive ? asciiLowerCase(keyPrefix) : keyPrefix))
                    return true;
            }
            return false;
        }

        @Override protected int cost() {
            return 6;
        }

        @Override
        public String toString() {
            return String.format("[^%s]", keyPrefix);
        }

    }

    /**
     * Evaluator for attribute name/value matching
     */
    public static final class AttributeWithValue extends AttributeKeyPair {
        public AttributeWithValue(String key, String value) {
            super(key, value);
        }

        AttributeWithValue(String key, String value, CaseSensitivity flag) {
            super(key, value, flag);
        }

        @Override boolean matchesNode(Element root, Node node) {
            return matchesValue(root, node, AttributeOperator.Equals);
        }

        @Override protected int cost() {
            return 3;
        }

        @Override
        public String toString() {
            return attributeString("=");
        }

    }

    /**
     * Evaluator for attribute name != value matching
     */
    public static final class AttributeWithValueNot extends AttributeKeyPair {
        public AttributeWithValueNot(String key, String value) {
            super(key, value);
        }

        AttributeWithValueNot(String key, String value, CaseSensitivity flag) {
            super(key, value, flag);
        }

        @Override boolean matchesNode(Element root, Node node) {
            return matchesValue(root, node, AttributeOperator.NotEquals);
        }

        @Override protected int cost() {
            return 3;
        }

        @Override
        public String toString() {
            return attributeString("!=");
        }

    }

    /**
     * Evaluator for attribute name/value matching (value prefix)
     */
    public static final class AttributeWithValueStarting extends AttributeKeyPair {
        public AttributeWithValueStarting(String key, String value) {
            super(key, value);
        }

        AttributeWithValueStarting(String key, String value, CaseSensitivity flag) {
            super(key, value, flag);
        }

        @Override boolean matchesNode(Element root, Node node) {
            return matchesValue(root, node, AttributeOperator.StartsWith);
        }

        @Override protected int cost() {
            return 4;
        }

        @Override
        public String toString() {
            return attributeString("^=");
        }
    }

    /**
     * Evaluator for attribute name/value matching (value ending)
     */
    public static final class AttributeWithValueEnding extends AttributeKeyPair {
        public AttributeWithValueEnding(String key, String value) {
            super(key, value);
        }

        AttributeWithValueEnding(String key, String value, CaseSensitivity flag) {
            super(key, value, flag);
        }

        @Override boolean matchesNode(Element root, Node node) {
            return matchesValue(root, node, AttributeOperator.EndsWith);
        }

        @Override protected int cost() {
            return 4;
        }

        @Override
        public String toString() {
            return attributeString("$=");
        }
    }

    /**
     * Evaluator for attribute name/value matching (value containing)
     */
    public static final class AttributeWithValueContaining extends AttributeKeyPair {
        public AttributeWithValueContaining(String key, String value) {
            super(key, value);
        }

        AttributeWithValueContaining(String key, String value, CaseSensitivity flag) {
            super(key, value, flag);
        }

        @Override boolean matchesNode(Element root, Node node) {
            return matchesValue(root, node, AttributeOperator.Contains);
        }

        @Override protected int cost() {
            return 6;
        }

        @Override
        public String toString() {
            return attributeString("*=");
        }

    }

    /** Matches a complete attribute token, as in [class~=card]. */
    static final class AttributeWithValueContainingToken extends AttributeKeyPair {
        AttributeWithValueContainingToken(String key, String value, CaseSensitivity flag) {
            super(key, value, flag);
        }

        @Override boolean matchesNode(Element root, Node node) {
            return matchesValue(root, node, AttributeOperator.ContainsToken);
        }

        @Override protected int cost() {
            return 8;
        }

        @Override public String toString() {
            return attributeString("~=");
        }
    }

    /** Matches an attribute value or its hyphen-separated prefix, as in [lang|=en]. */
    static final class AttributeWithValueDashPrefix extends AttributeKeyPair {
        AttributeWithValueDashPrefix(String key, String value, CaseSensitivity flag) {
            super(key, value, flag);
        }

        @Override boolean matchesNode(Element root, Node node) {
            return matchesValue(root, node, AttributeOperator.DashMatch);
        }

        @Override protected int cost() {
            return 4;
        }

        @Override public String toString() {
            return attributeString("|=");
        }
    }

    /**
     * Evaluator for attribute name/value matching (value regex matching)
     */
    public static final class AttributeWithValueMatching extends AttributeEvaluator {
        final String key;
        final Regex pattern;

        public AttributeWithValueMatching(String key, Regex pattern) {
            this.key = trimAsciiWhitespace(key); // preserve key case in serialized queries
            this.pattern = pattern;
        }

        public AttributeWithValueMatching(String key, Pattern pattern) {
            this(key, Regex.fromPattern(pattern));
        }

        @Override boolean matchesNode(Element root, Node node) {
            String value = attribute(root, node, key);
            return value != null && pattern.matcher(value).find();
        }

        @Override protected int cost() {
            return 8;
        }

        @Override
        public String toString() {
            if (!web) return String.format("[%s~=%s]", key, pattern);
            String regex = pattern.toString();
            // protect leading regex whitespace from the selector's whitespace after the comma
            if (!regex.isEmpty() && isWhitespace(regex.charAt(0))) regex = "(?:)" + regex;
            return String.format(":matchesAttr(%s, %s)", TokenQueue.escapeCssIdentifier(key), regex);
        }

    }

    /**
     * Abstract evaluator for attribute name/value matching
     */
    public abstract static class AttributeKeyPair extends AttributeEvaluator {
        final String key;
        final String value;
        final CaseSensitivity flag;
        private final boolean parsedCss; // parsed operands are already decoded; public inputs retain quote handling
        private final char quote; // retains input quoting when unquoted output could look like a case flag

        public AttributeKeyPair(String key, String value) {
            Validate.notEmpty(key);
            Validate.notNull(value);

            key = trimAsciiWhitespace(key);
            this.key = web ? key : asciiLowerCase(key);
            boolean quoted = value.startsWith("'") && value.endsWith("'")
                || value.startsWith("\"") && value.endsWith("\"");
            quote = quoted ? value.charAt(0) : 0;
            if (quoted) {
                Validate.isTrue(value.length() > 1, "Quoted value must have content");
                value = value.substring(1, value.length() - 1);
            }

            this.value = web ? value : lowerCase(value);
            flag = CaseSensitivity.Default;
            parsedCss = false;
        }

        AttributeKeyPair(String key, String value, CaseSensitivity flag) {
            this.key = key;
            this.value = value;
            this.flag = flag;
            parsedCss = true;
            quote = 0;
        }

        boolean matchesValue(Element root, Node node, AttributeOperator operator) {
            if (web) return super.matchesValue(root, node, key, value, operator, flag);
            return parsedCss ? LegacyMatching.cssAttributeMatches(node, key, value, operator, flag) :
                LegacyMatching.attributeMatches(node, key, value, operator);
        }

        String attributeString(String operator) {
            if (!parsedCss && !web) {
                return LegacyMatching.attributeString(key, value, operator, quote);
            }
            String cssValue = value.isEmpty() ? "\"\"" : TokenQueue.escapeCssIdentifier(value);
            String suffix = flag == CaseSensitivity.Insensitive ? " i" : flag == CaseSensitivity.Sensitive ? " s" : "";
            return String.format("[%s%s%s%s]", TokenQueue.escapeCssIdentifier(key), operator, cssValue, suffix);
        }
    }

    /**
     * Evaluator for any / all element matching
     */
    public static final class AllElements extends Evaluator {

        @Override
        public boolean matches(Element root, Element element) {
            return true;
        }

        @Override protected int cost() {
            return 10;
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    /**
     * Evaluator for matching by sibling index number (e {@literal <} idx)
     */
    public static final class IndexLessThan extends IndexEvaluator {
        public IndexLessThan(int index) {
            super(index);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return (web || root != element) && element.elementSiblingIndex() < index;
        }

        @Override
        public String toString() {
            return String.format(":lt(%d)", index);
        }

    }

    /**
     * Evaluator for matching by sibling index number (e {@literal >} idx)
     */
    public static final class IndexGreaterThan extends IndexEvaluator {
        public IndexGreaterThan(int index) {
            super(index);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.elementSiblingIndex() > index;
        }

        @Override
        public String toString() {
            return String.format(":gt(%d)", index);
        }

    }

    /**
     * Evaluator for matching by sibling index number (e = idx)
     */
    public static final class IndexEquals extends IndexEvaluator {
        public IndexEquals(int index) {
            super(index);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.elementSiblingIndex() == index;
        }

        @Override
        public String toString() {
            return String.format(":eq(%d)", index);
        }

    }

    /**
     * Evaluator for matching the last sibling (css :last-child)
     */
    public static final class IsLastChild extends Evaluator {
		@Override
		public boolean matches(Element root, Element element) {
			final Element p = element.parent();
			return p != null && !(p instanceof Document) && element == p.lastElementChild();
		}

		@Override
		public String toString() {
			return ":last-child";
		}
    }

    public static final class IsFirstOfType extends IsNthOfType {
        public IsFirstOfType() {
            super(0, 1);
        }

		@Override
		public String toString() {
			return ":first-of-type";
		}
    }

    public static final class IsLastOfType extends IsNthLastOfType {
        public IsLastOfType() {
            super(0, 1);
        }

		@Override
		public String toString() {
			return ":last-of-type";
		}
    }


    public static abstract class CssNthEvaluator extends Evaluator {
        /** Step */
        protected final int a;
        /** Offset */
        protected final int b;

        public CssNthEvaluator(int step, int offset) {
            this.a = step;
            this.b = offset;
        }

        public CssNthEvaluator(int offset) {
            this(0, offset);
        }

        @Override
        public boolean matches(Element root, Element element) {
            final Element p = element.parent();
            if (p == null || (p instanceof Document)) return false;

            final int pos = calculatePosition(root, element);
            if (a == 0) return pos == b;

            return (pos - b) * a >= 0 && (pos - b) % a == 0;
        }

        @Override
        public String toString() {
            String format =
                (a == 0) ? ":%s(%3$d)"    // only offset (b)
                : (b == 0) ? ":%s(%2$dn)" // only step (a)
                : ":%s(%2$dn%3$+d)";      // step, offset
            return String.format(format, getPseudoClass(), a, b);
        }

        protected abstract String getPseudoClass();

        protected abstract int calculatePosition(Element root, Element element);
    }


    /**
     * css-compatible Evaluator for :eq (css :nth-child)
     *
     * @see IndexEquals
     */
    public static final class IsNthChild extends CssNthEvaluator {
        public IsNthChild(int step, int offset) {
            super(step, offset);
        }

        @Override
        protected int calculatePosition(Element root, Element element) {
            return element.elementSiblingIndex() + 1;
        }

        @Override
        protected String getPseudoClass() {
            return "nth-child";
        }
    }

    /**
     * css pseudo class :nth-last-child)
     *
     * @see IndexEquals
     */
    public static final class IsNthLastChild extends CssNthEvaluator {
        public IsNthLastChild(int step, int offset) {
            super(step, offset);
        }

        @Override
        protected int calculatePosition(Element root, Element element) {
    	    if (element.parent() == null) return 0;
        	return element.parent().childrenSize() - element.elementSiblingIndex();
        }

		@Override
		protected String getPseudoClass() {
			return "nth-last-child";
		}
    }

    /**
     * css pseudo class nth-of-type
     *
     */
    public static class IsNthOfType extends CssNthEvaluator {
        public IsNthOfType(int step, int offset) {
            super(step, offset);
        }

        @Override protected int calculatePosition(Element root, Element element) {
            Element parent = element.parent();
            if (parent == null)
                return 0;

            int pos = 0;
            final int size = parent.childNodeSize();
            for (int i = 0; i < size; i++) {
                Node node = parent.childNode(i);
                if (sameType(node, element)) pos++;
                if (node == element) break;
            }
            return pos;
        }

        @Override
        protected String getPseudoClass() {
            return "nth-of-type";
        }
    }

    public static class IsNthLastOfType extends CssNthEvaluator {
        public IsNthLastOfType(int step, int offset) {
            super(step, offset);
        }

        @Override
        protected int calculatePosition(Element root, Element element) {
            Element parent = element.parent();
            if (parent == null)
                return 0;

            int pos = 0;
            Element next = element;
            while (next != null) {
                if (sameType(next, element))
                    pos++;
                next = next.nextElementSibling();
            }
            return pos;
        }

        @Override
        protected String getPseudoClass() {
            return "nth-last-of-type";
        }
    }

    /**
     * Evaluator for matching the first sibling (css :first-child)
     */
    public static final class IsFirstChild extends Evaluator {
    	@Override
    	public boolean matches(Element root, Element element) {
    		final Element p = element.parent();
    		return p != null && !(p instanceof Document) && element == p.firstElementChild();
    	}

    	@Override
    	public String toString() {
    		return ":first-child";
    	}
    }

    /**
     * css3 pseudo-class :root
     * @see <a href="http://www.w3.org/TR/selectors/#root-pseudo">:root selector</a>
     *
     */
    public static final class IsRoot extends Evaluator {
    	@Override
    	public boolean matches(Element root, Element element) {
            return element == documentElementOrSelf(root.root());
    	}

        @Override protected int cost() {
            return 1;
        }

    	@Override
    	public String toString() {
    		return ":root";
    	}
    }

    /**
     Matches the original query scope, including within relative selectors.
     */
    static final class IsScope extends Evaluator {
        @Override public boolean matches(Element root, Element element) {
            return element == documentElementOrSelf(root);
        }

        @Override protected int cost() {
            return 1;
        }

        @Override public String toString() {
            return ":scope";
        }
    }

    public static final class IsOnlyChild extends Evaluator {
		@Override
		public boolean matches(Element root, Element element) {
			final Element p = element.parent();
			return p!=null && !(p instanceof Document) && element.siblingElements().isEmpty();
		}
    	@Override
    	public String toString() {
    		return ":only-child";
    	}
    }

    public static final class IsOnlyOfType extends Evaluator {
		@Override
		public boolean matches(Element root, Element element) {
			final Element p = element.parent();
			if (p==null || p instanceof Document) return false;

			int pos = 0;
            Element next = p.firstElementChild();
            while (next != null) {
                if (sameType(next, element))
                    pos++;
                if (pos > 1)
                    break;
                next = next.nextElementSibling();
            }
        	return pos == 1;
		}
    	@Override
    	public String toString() {
    		return ":only-of-type";
    	}
    }

    public static final class IsEmpty extends Evaluator {
        @Override
        public boolean matches(Element root, Element el) {
            for (Node n = el.firstChild(); n != null; n = n.nextSibling()) {
                if (n instanceof TextNode) {
                    if (!((TextNode) n).isBlank())
                        return false; // non-blank text: not empty
                } else if (!(n instanceof Comment || n instanceof ProcessingInstruction ||
                    n instanceof XmlDeclaration || n instanceof DocumentType))
                    return false; // non "blank" element: not empty
            }
            return true;
        }

        @Override
        public String toString() {
            return ":empty";
        }
    }

    /**
     * Abstract evaluator for sibling index matching
     *
     * @author ant
     */
    public abstract static class IndexEvaluator extends Evaluator {
        final int index;

        public IndexEvaluator(int index) {
            this.index = index;
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) text
     */
    public static final class ContainsText extends Evaluator {
        private final String searchText;

        public ContainsText(String searchText) {
            this.searchText = lowerCase(normaliseWhitespace(searchText));
        }

        @Override
        public boolean matches(Element root, Element element) {
            return lowerCase(element.text()).contains(searchText);
        }

        @Override protected int cost() {
            return 10;
        }

        @Override
        public String toString() {
            return String.format(":contains(%s)", searchText);
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) wholeText. Neither the input nor the element text is
     * normalized. <code>:containsWholeText()</code>
     * @since 1.15.1.
     */
    public static final class ContainsWholeText extends Evaluator {
        private final String searchText;

        public ContainsWholeText(String searchText) {
            this.searchText = searchText;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.wholeText().contains(searchText);
        }

        @Override protected int cost() {
            return 10;
        }

        @Override
        public String toString() {
            return String.format(":containsWholeText(%s)", searchText);
        }
    }

    /**
     * Evaluator for matching Element (but <b>not</b> its descendants) wholeText. Neither the input nor the element text is
     * normalized. <code>:containsWholeOwnText()</code>
     * @since 1.15.1.
     */
    public static final class ContainsWholeOwnText extends Evaluator {
        private final String searchText;

        public ContainsWholeOwnText(String searchText) {
            this.searchText = searchText;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.wholeOwnText().contains(searchText);
        }

        @Override
        public String toString() {
            return String.format(":containsWholeOwnText(%s)", searchText);
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) data
     */
    public static final class ContainsData extends Evaluator {
        private final String searchText;

        public ContainsData(String searchText) {
            this.searchText = lowerCase(searchText);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return lowerCase(element.data()).contains(searchText); // not whitespace normalized
        }

        @Override
        public String toString() {
            return String.format(":containsData(%s)", searchText);
        }
    }

    /**
     * Evaluator for matching Element's own text
     */
    public static final class ContainsOwnText extends Evaluator {
        private final String searchText;

        public ContainsOwnText(String searchText) {
            this.searchText = lowerCase(normaliseWhitespace(searchText));
        }

        @Override
        public boolean matches(Element root, Element element) {
            return lowerCase(element.ownText()).contains(searchText);
        }

        @Override
        public String toString() {
            return String.format(":containsOwn(%s)", searchText);
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) text with regex
     */
    public static final class Matches extends Evaluator {
        private final Regex pattern;

        public Matches(Regex pattern) {
            this.pattern = pattern;
        }

        public Matches(Pattern pattern) {
            this(Regex.fromPattern(pattern));
        }

        @Override
        public boolean matches(Element root, Element element) {
            return pattern.matcher(element.text()).find();
        }

        @Override protected int cost() {
            return 8;
        }

        @Override
        public String toString() {
            return String.format(":matches(%s)", pattern);
        }
    }

    /**
     * Evaluator for matching Element's own text with regex
     */
    public static final class MatchesOwn extends Evaluator {
        private final Regex pattern;

        public MatchesOwn(Regex pattern) {
            this.pattern = pattern;
        }

        public MatchesOwn(Pattern pattern) {
            this(Regex.fromPattern(pattern));
        }

        @Override
        public boolean matches(Element root, Element element) {
            return pattern.matcher(element.ownText()).find();
        }

        @Override protected int cost() {
            return 7;
        }

        @Override
        public String toString() {
            return String.format(":matchesOwn(%s)", pattern);
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) whole text with regex.
     * @since 1.15.1.
     */
    public static final class MatchesWholeText extends Evaluator {
        private final Regex pattern;

        public MatchesWholeText(Regex pattern) {
            this.pattern = pattern;
        }

        public MatchesWholeText(Pattern pattern) {
            this(Regex.fromPattern(pattern));
        }

        @Override
        public boolean matches(Element root, Element element) {
            return pattern.matcher(element.wholeText()).find();
        }

        @Override protected int cost() {
            return 8;
        }

        @Override
        public String toString() {
            return String.format(":matchesWholeText(%s)", pattern);
        }
    }

    /**
     * Evaluator for matching Element's own whole text with regex.
     * @since 1.15.1.
     */
    public static final class MatchesWholeOwnText extends Evaluator {
        private final Regex pattern;

        public MatchesWholeOwnText(Regex pattern) {
            this.pattern = pattern;
        }

        public MatchesWholeOwnText(Pattern pattern) {
            this(Regex.fromPattern(pattern));
        }

        @Override
        public boolean matches(Element root, Element element) {
            Regex.Matcher m = pattern.matcher(element.wholeOwnText());
            return m.find();
        }

        @Override protected int cost() {
            return 7;
        }

        @Override
        public String toString() {
            return String.format(":matchesWholeOwnText(%s)", pattern);
        }
    }
}
