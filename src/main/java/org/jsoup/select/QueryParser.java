package org.jsoup.select;

import org.jsoup.helper.Regex;
import org.jsoup.internal.StringUtil;
import org.jsoup.internal.QueryMode;
import org.jsoup.helper.Validate;
import org.jsoup.internal.CssMatching.AttributeOperator;
import org.jsoup.internal.CssMatching.CaseSensitivity;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.ProcessingInstruction;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.TokenQueue;
import org.jsoup.select.HasEvaluator.Traversal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jsoup.internal.Normalizer.asciiLowerCase;
import static org.jsoup.internal.StringUtil.trimAsciiWhitespace;
import static org.jsoup.select.StructuralEvaluator.ImmediateParentRun;

/**
 * Parses a CSS selector into an Evaluator tree.
 */
public class QueryParser implements AutoCloseable {
    private final static char[] Combinators = {'>', '+', '~'}; // ' ' is also a combinator, but found implicitly
    private static final AttributeOperator[] AttributeOperators = AttributeOperator.values();
    private final static char[] SequenceEnders = {',', ')'};
    static final int MaxNestingDepth = 128;

    private final TokenQueue tq;
    private final String query;
    private final boolean web;
    private boolean inNodeContext; // ::comment:contains should act on node value, vs element text
    private int nestingDepth;

    /**
     * Create a new QueryParser.
     * @param query CSS query
     */
    private QueryParser(String query, boolean web) {
        this.web = web;
        Validate.notEmpty(query);
        query = trimAsciiWhitespace(query);
        this.query = query;
        this.tq = new TokenQueue(query);
    }

    /**
     Parse a CSS query into an Evaluator. If you are evaluating the same query repeatedly, it may be more efficient to
     parse it once and reuse the Evaluator.

     @param query CSS query
     @return Evaluator
     @see Selector selector query syntax
     @throws Selector.SelectorParseException if the CSS query is invalid
     */
    public static Evaluator parse(String query) {
        return parse(query, QueryMode.current() == QueryMode.Web);
    }

    static Evaluator parse(String query, boolean web) {
        return Evaluator.withMode(web, () -> {
            try (QueryParser p = new QueryParser(query, web)) {
                return p.parse();
            } catch (IllegalArgumentException e) {
                throw new Selector.SelectorParseException(e.getMessage());
            }
        });
    }

    /**
     Parse the query. We use this simplified expression of the grammar:
     <pre>
     SelectorGroup   ::= Selector (',' Selector)*
     Selector        ::= [ Combinator ] SimpleSequence ( Combinator SimpleSequence )*
     SimpleSequence  ::= [ TypeSelector ] ( ID | Class | Attribute | Pseudo )*
     Pseudo           ::= ':' Name [ '(' SelectorGroup ')' ]
     Combinator      ::= S+         // descendant (whitespace)
     | '>'       // child
     | '+'       // adjacent sibling
     | '~'       // general sibling
     </pre>

     See <a href="https://www.w3.org/TR/selectors-4/#grammar">selectors-4</a> for the real thing
     */
    Evaluator parse() {
        Evaluator eval = parseSelectorGroup().evaluator();
        tq.consumeWhitespace();
        if (!tq.isEmpty())
            throw new Selector.SelectorParseException("Could not parse query '%s': unexpected token at '%s'", query, tq.remainder());
        return eval;
    }

    /** Parses an ordered group of comma-separated selectors. */
    private SelectorGroup parseSelectorGroup() {
        List<ComplexSelector> selectors = new ArrayList<>();
        do {
            selectors.add(parseSelector());
        } while (tq.matchChomp(','));
        return new SelectorGroup(selectors);
    }

    private ComplexSelector parseSelector() {
        tq.consumeWhitespace();
        char leading = tq.matchesAny(Combinators) ? tq.consume() : 0;
        List<Evaluator> compounds = Collections.singletonList(parseSimpleSequence());
        List<Character> combinators = Collections.emptyList();
        while (true) {
            char combinator = tq.consumeWhitespace() ? ' ' : 0;
            if (tq.matchesAny(Combinators)) combinator = tq.consume();
            else if (tq.matchesAny(SequenceEnders) || tq.isEmpty()) break;
            if (combinator == 0) break;
            if (combinators.isEmpty()) {
                compounds = new ArrayList<>(compounds);
                combinators = new ArrayList<>();
            }
            combinators.add(combinator);
            compounds.add(parseSimpleSequence());
        }
        return new ComplexSelector(compounds, combinators, leading);
    }

    /** Stores selector parts and relationships, such as {@code div.product > a}, for ordinary queries and {@code :has()}. */
    private final class ComplexSelector {
        final List<Evaluator> compounds;
        final List<Character> combinators;
        final char leadingCombinator;

        ComplexSelector(List<Evaluator> compounds, List<Character> combinators, char leading) {
            this.compounds = compounds;
            this.combinators = combinators;
            leadingCombinator = leading;
        }

        Evaluator evaluator() {
            return evaluator(leadingCombinator != 0 ? new StructuralEvaluator.QueryRoot() : null);
        }

        Evaluator relativeEvaluator() {
            if (compounds.size() == 1 && (leadingCombinator != '+' || !wantsNodes()))
                return compounds.get(0); // traversal already guarantees the leading relationship
            return evaluator(leadingCombinator != 0 || web ? new StructuralEvaluator.RelativeAnchor() : null);
        }

        private Evaluator evaluator(@Nullable Evaluator anchor) {
            Evaluator left = compounds.get(0);
            if (anchor != null) left = combinator(anchor, leadingCombinator == 0 ? ' ' : leadingCombinator, left);
            for (int i = 0; i < combinators.size(); i++)
                left = combinator(left, combinators.get(i), compounds.get(i + 1));
            return left;
        }

        boolean wantsNodes() {
            for (Evaluator compound : compounds) if (compound.wantsNodes()) return true;
            return false;
        }

        @Override public String toString() {
            StringBuilder out = new StringBuilder();
            if (leadingCombinator != 0) out.append(leadingCombinator).append(' ');
            out.append(compounds.get(0));
            for (int i = 0; i < combinators.size(); i++) {
                char relation = combinators.get(i);
                out.append(' ');
                if (relation != ' ') out.append(relation).append(' ');
                out.append(compounds.get(i + 1));
            }
            return out.toString();
        }
    }

    /** A parsed selector group that can be compiled or rendered in source order. */
    private static final class SelectorGroup {
        final List<ComplexSelector> selectors;

        /** Creates a group from its selectors in source order. */
        SelectorGroup(List<ComplexSelector> selectors) {
            this.selectors = selectors;
        }

        /** Build the selectors into a single evaluator. */
        Evaluator evaluator() {
            Evaluator first = selectors.get(0).evaluator();
            if (selectors.size() == 1)
                return first;

            ArrayList<Evaluator> evaluators = new ArrayList<>(selectors.size());
            // preserve a leading Or's flat evaluation order, e.g. the two alternatives compiled for *|tag
            if (first instanceof CombiningEvaluator.Or)
                evaluators.addAll(((CombiningEvaluator.Or) first).evaluators);
            else
                evaluators.add(first);
            for (int i = 1; i < selectors.size(); i++)
                evaluators.add(selectors.get(i).evaluator());
            return new CombiningEvaluator.Or(evaluators);
        }

        @Override public String toString() {
            return StringUtil.join(selectors, ", ");
        }
    }

    Evaluator parseSimpleSequence() {
        // SimpleSequence ::= TypeSelector? ( Hash | Class | Pseudo )*
        Evaluator first = null;
        tq.consumeWhitespace();

        // one optional type selector
        if (tq.matchesWord() || tq.matches("*|"))
            first = byTag();
        else if (tq.matchChomp('*'))
            first = new Evaluator.AllElements();

        // use the first subclass when there is no type selector
        if (first == null)
            first = parseSubclass();

        if (first == null)
            throw new Selector.SelectorParseException("Could not parse query '%s': unexpected token at '%s'", query, tq.remainder());

        return parseRemainingSubclasses(first);
    }

    /** Parses and combines any subclasses that follow the first evaluator. */
    private Evaluator parseRemainingSubclasses(Evaluator first) {
        Evaluator next = parseSubclass();
        if (next == null)
            return first;

        ArrayList<Evaluator> evaluators = new ArrayList<>(4);
        evaluators.add(first);
        do {
            evaluators.add(next);
        } while ((next = parseSubclass()) != null);
        return new CombiningEvaluator.And(evaluators);
    }

    static Evaluator combinator(Evaluator left, char combinator, Evaluator right) {
        switch (combinator) {
            case '>':
                ImmediateParentRun run = left instanceof ImmediateParentRun ?
                    (ImmediateParentRun) left : new ImmediateParentRun(left);
                run.add(right);
                return run;
            case ' ':
                return new CombiningEvaluator.And(new StructuralEvaluator.Ancestor(left), right);
            case '+':
                return new CombiningEvaluator.And(new StructuralEvaluator.ImmediatePreviousSibling(left), right);
            case '~':
                return new CombiningEvaluator.And(new StructuralEvaluator.PreviousSibling(left), right);
            default:
                throw new Selector.SelectorParseException("Unknown combinator '%s'", combinator);
        }
    }

    @Nullable Evaluator parseSubclass() {
        //  Subclass: ID | Class | Attribute | Pseudo
        if      (tq.matchChomp('#'))    return byId();
        else if (tq.matchChomp('.'))    return byClass();
        else if (tq.matches('['))       return byAttribute();
        else if (tq.matchChomp("::"))   return parseNodeSelector(); // ::comment etc
        else if (tq.matchChomp(':'))    return parsePseudoSelector();
        else                            return null;
    }

    private Evaluator parsePseudoSelector() {
        final String pseudo = tq.consumeCssIdentifier();
        switch (pseudo) {
            case "lt":
                return new Evaluator.IndexLessThan(consumeIndex());
            case "gt":
                return new Evaluator.IndexGreaterThan(consumeIndex());
            case "eq":
                return new Evaluator.IndexEquals(consumeIndex());
            case "has":
                return has();
            case "is":
                return is();
            case "contains":
                return contains(false);
            case "containsOwn":
                return contains(true);
            case "containsWholeText":
                return containsWholeText(false);
            case "containsWholeOwnText":
                return containsWholeText(true);
            case "containsData":
                return containsData();
            case "matches":
                return matches(false);
            case "matchesAttr":
                return matchesAttr();
            case "matchesOwn":
                return matches(true);
            case "matchesWholeText":
                return matchesWholeText(false);
            case "matchesWholeOwnText":
                return matchesWholeText(true);
            case "not":
                return not();
            case "nth-child":
                return cssNthChild(false, false);
            case "nth-last-child":
                return cssNthChild(true, false);
            case "nth-of-type":
                return cssNthChild(false, true);
            case "nth-last-of-type":
                return cssNthChild(true, true);
            case "first-child":
                return new Evaluator.IsFirstChild();
            case "last-child":
                return new Evaluator.IsLastChild();
            case "first-of-type":
                return new Evaluator.IsFirstOfType();
            case "last-of-type":
                return new Evaluator.IsLastOfType();
            case "only-child":
                return new Evaluator.IsOnlyChild();
            case "only-of-type":
                return new Evaluator.IsOnlyOfType();
            case "empty":
                return new Evaluator.IsEmpty();
            case "blank":
                return new NodeEvaluator.BlankValue();
            case "scope":
                return new Evaluator.IsScope();
            case "root":
                return new Evaluator.IsRoot();
            case "matchText": {
                throw new Selector.SelectorParseException(":matchText is no longer supported. Use Element#selectNodes(String, Class) with selector ::text and class TextNode instead."); // todo remove this in 1.25.1
            }
            default:
                throw new Selector.SelectorParseException("Could not parse query '%s': unexpected token at '%s'", query, tq.remainder());
        }
    }

    /** Parses a node selector and its subclasses in node-value context. */
    private Evaluator parseNodeSelector() {
        final String pseudo = tq.consumeCssIdentifier();
        Evaluator nodeType;
        switch (pseudo) {
            case "node":
                nodeType = new NodeEvaluator.InstanceType(Node.class, pseudo);
                break;
            case "leafnode":
                nodeType = new NodeEvaluator.InstanceType(LeafNode.class, pseudo);
                break;
            case "text":
                nodeType = new NodeEvaluator.InstanceType(TextNode.class, pseudo);
                break;
            case "comment":
                nodeType = new NodeEvaluator.InstanceType(Comment.class, pseudo);
                break;
            case "data":
                nodeType = new NodeEvaluator.InstanceType(DataNode.class, pseudo);
                break;
            case "cdata":
                nodeType = new NodeEvaluator.InstanceType(CDataNode.class, pseudo);
                break;
            case "pi":
                nodeType = piSelector();
                break;
            default:
                throw new Selector.SelectorParseException(
                    "Could not parse query '%s': unknown node type '::%s'", query, pseudo);
        }

        // nested selectors can enter node context again, e.g. ::comment:not(::text):contains(foo)
        // restore the caller's context even if parsing a subclass throws
        boolean previousNodeContext = inNodeContext;
        inNodeContext = true;
        try {
            return parseRemainingSubclasses(nodeType);
        } finally {
            inNodeContext = previousNodeContext;
        }
    }

    /** Parses an optional processing-instruction target. */
    private Evaluator piSelector() {
        if (!tq.matches('('))
            return new NodeEvaluator.InstanceType(ProcessingInstruction.class, "pi");

        String argument = trimAsciiWhitespace(consumeParens());
        Validate.notEmpty(argument, "::pi target must not be empty");
        try (TokenQueue targetQueue = new TokenQueue(argument)) {
            String target = targetQueue.consumeCssIdentifier();
            targetQueue.consumeWhitespace();
            Validate.isTrue(targetQueue.isEmpty(), "::pi target must be a CSS identifier");
            return new NodeEvaluator.ProcessingInstructionTarget(target);
        }
    }

    private Evaluator byId() {
        String id = tq.consumeCssIdentifier();
        Validate.notEmpty(id);
        return new Evaluator.Id(id);
    }

    private Evaluator byClass() {
        String className = tq.consumeCssIdentifier();
        Validate.notEmpty(className);
        return new Evaluator.Class(className.trim());
    }

    private Evaluator byTag() {
        String tagName = tq.consumeElementSelector();
        Validate.notEmpty(tagName);

        // namespaces:
        if (tagName.startsWith("*|")) { // namespaces: wildcard match equals(tagName) or ending in ":"+tagName
            String plainTag = tagName.substring(2); // strip *|
            return new CombiningEvaluator.Or(
                new Evaluator.Tag(plainTag),
                new Evaluator.TagEndsWith(":" + plainTag)
            );
        } else if (tagName.endsWith("|*")) { // ns|*
            String ns = tagName.substring(0, tagName.length() - 2) + ":"; // strip |*, to ns:
            return new Evaluator.TagStartsWith(ns);
        } else if (tagName.contains("|")) { // flip "abc|def" to "abc:def"
            tagName = tagName.replace("|", ":");
        }

        return new Evaluator.Tag(tagName);
    }

    private Evaluator byAttribute() {
        if (!web) return LegacyQueryParser.attribute(tq, query);
        String attribute = peekCssAttribute(query, tq.pos(), true);
        try (TokenQueue css = new TokenQueue(attribute)) {
            Evaluator eval = cssAttribute(css, true);
            consumeAttribute(tq, attribute);
            return eval;
        }
    }

    /** Reads the contents of {@code [...]} without advancing the query, allowing legacy parsing to retry. */
    static String peekCssAttribute(String query, int offset, boolean extendedValues) {
        int start = offset + 1;
        char quote = 0;
        int depth = 1;
        for (int pos = start; pos < query.length(); pos++) {
            char c = query.charAt(pos);
            if (c == '\\') {
                pos++;
                continue;
            }
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (extendedValues && c == '[') {
                depth++; // preserve unquoted values such as http://[::1]/
            } else if (c == ']' && --depth == 0) {
                return query.substring(start, pos);
            }
        }
        throw new Selector.SelectorParseException("Did not find balanced marker at '%s'", query.substring(start));
    }

    static void consumeAttribute(TokenQueue tq, String attribute) {
        for (int i = 0; i < attribute.length() + 2; i++) tq.advance();
    }

    /**
     Parses an attribute selector. With {@code extendedValues} false, unquoted values must be CSS identifiers
     so legacy values ending in {@code i} or {@code s} are not mistaken for case flags.
     */
    static Evaluator cssAttribute(TokenQueue cq, boolean extendedValues) {
        cq.consumeWhitespace();
        boolean prefix = cq.matchChomp('^');
        if (prefix && cq.isEmpty()) return new Evaluator.AttributeStarting("");
        String key = consumeAttributeName(cq, cq.matchChomp('*') ? "*" : cq.consumeCssIdentifier());
        cq.consumeWhitespace();
        if (cq.isEmpty()) return prefix ? new Evaluator.AttributeStarting(key) : key.equals("*") ?
            new Evaluator.AttributeStarting("") : new Evaluator.Attribute(key);
        Validate.isFalse(prefix || key.equals("*"), "Attribute value comparison requires an attribute name");
        AttributeOperator operator = null;
        for (AttributeOperator candidate : AttributeOperators) {
            if (cq.matchChomp(candidate.symbol)) { operator = candidate; break; }
        }
        Validate.notNull(operator, "Attribute operator expected");
        AttributeValue operand = consumeAttributeValue(cq, extendedValues);
        String value = operand.value;
        CaseSensitivity flag = operand.flag;
        switch (operator) {
            case Equals: return new Evaluator.AttributeWithValue(key, value, flag);
            case NotEquals: return new Evaluator.AttributeWithValueNot(key, value, flag);
            case StartsWith: return new Evaluator.AttributeWithValueStarting(key, value, flag);
            case EndsWith: return new Evaluator.AttributeWithValueEnding(key, value, flag);
            case Contains: return new Evaluator.AttributeWithValueContaining(key, value, flag);
            case ContainsToken: return new Evaluator.AttributeWithValueContainingToken(key, value, flag);
            case DashMatch: return new Evaluator.AttributeWithValueDashPrefix(key, value, flag);
            default: throw new IllegalArgumentException("Unknown attribute operator: " + operator);
        }
    }

    private static AttributeValue consumeAttributeValue(TokenQueue cq, boolean extendedValues) {
        cq.consumeWhitespace();
        boolean quoted = cq.matches('\'') || cq.matches('"');
        String value;
        CaseSensitivity flag = CaseSensitivity.Default;
        if (extendedValues && !quoted) {
            StringBuilder out = new StringBuilder();
            String whitespace = "";
            while (!cq.isEmpty()) {
                String part = cq.consumeCssValue(true);
                StringBuilder nextWhitespace = new StringBuilder();
                while (cq.matchesWhitespace()) nextWhitespace.append(cq.consume());
                CaseSensitivity nextFlag = attributeCase(part);
                if (cq.isEmpty() && out.length() != 0 && nextFlag != CaseSensitivity.Default)
                    flag = nextFlag;
                else
                    out.append(whitespace).append(part);
                whitespace = nextWhitespace.toString();
            }
            value = out.toString();
        } else {
            Validate.isFalse(cq.isEmpty(), "Attribute value expected");
            value = cq.consumeCssValue();
            boolean separated = cq.consumeWhitespace();
            if (!cq.isEmpty()) {
                Validate.isTrue(quoted || separated, "Attribute flag must follow a quoted value or whitespace");
                String name = cq.consumeCssIdentifier();
                flag = attributeCase(name);
                Validate.isTrue(flag != CaseSensitivity.Default, "Unknown attribute case flag: " + asciiLowerCase(name));
                cq.consumeWhitespace();
            }
        }
        Validate.isTrue(cq.isEmpty(), "Unexpected token after attribute value");
        return new AttributeValue(value, flag);
    }

    private static final class AttributeValue {
        final String value;
        final CaseSensitivity flag;

        AttributeValue(String value, CaseSensitivity flag) {
            this.value = value;
            this.flag = flag;
        }
    }

    private static CaseSensitivity attributeCase(String value) {
        String name = asciiLowerCase(value);
        return name.equals("i") ? CaseSensitivity.Insensitive :
            name.equals("s") ? CaseSensitivity.Sensitive : CaseSensitivity.Default;
    }

    /**
     Allows attribute names containing a colon, including {@code abs:href}.
     */
    private static String consumeAttributeName(TokenQueue cq, String key) {
        Validate.notEmpty(key, "Attribute name expected");
        if (cq.matchChomp(':')) {
            String local = cq.consumeCssIdentifier();
            Validate.isFalse(key.equals("abs") && local.isEmpty(), "Absolute attribute key must have a name");
            Validate.notEmpty(local, "Attribute name expected");
            key += ":" + local;
        }
        return key;
    }

    /**
     Parses {@code :matchesAttr()}, leaving regex escapes intact.
     */
    private Evaluator matchesAttr() {
        Validate.isTrue(tq.matchChomp('('), ":matchesAttr requires an attribute name and regex");
        tq.consumeWhitespace();
        String key = consumeAttributeName(tq, tq.consumeCssIdentifier());
        tq.consumeWhitespace();
        Validate.isTrue(tq.matchChomp(','), ":matchesAttr requires an attribute name and regex");
        tq.consumeWhitespace();
        String pattern = tq.chompBalancedRemainder('(', ')');
        Validate.notEmpty(pattern, ":matchesAttr requires a regex");
        return new Evaluator.AttributeWithValueMatching(key, Regex.compile(pattern));
    }

    //pseudo selectors :first-child, :last-child, :nth-child, ...
    private static final Pattern NthStepOffset = Pattern.compile("(([+-])?(\\d+)?)n(\\s*([+-])?\\s*\\d+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern NthOffset = Pattern.compile("([+-])?(\\d+)");

    private Evaluator cssNthChild(boolean last, boolean ofType) {
        String arg = asciiLowerCase(trimAsciiWhitespace(consumeParens())); // arg is like "odd", or "-n+2", within nth-child(odd)
        final int step, offset;
        if ("odd".equals(arg)) {
            step = 2;
            offset = 1;
        } else if ("even".equals(arg)) {
            step = 2;
            offset = 0;
        } else {
            Matcher stepOffsetM, stepM;
            if ((stepOffsetM = NthStepOffset.matcher(arg)).matches()) {
                if (stepOffsetM.group(3) != null) // has digits, like 3n+2 or -3n+2
                    step = Integer.parseInt(stepOffsetM.group(1).replaceFirst("^\\+", ""));
                else // no digits, might be like n+2, or -n+2. if group(2) == "-", it’s -1;
                    step = "-".equals(stepOffsetM.group(2)) ? -1 : 1;
                offset =
                    stepOffsetM.group(4) != null ? Integer.parseInt(stepOffsetM.group(4).replaceFirst("^\\+", "")) : 0;
            } else if ((stepM = NthOffset.matcher(arg)).matches()) {
                step = 0;
                offset = Integer.parseInt(stepM.group().replaceFirst("^\\+", ""));
            } else {
                throw new Selector.SelectorParseException("Could not parse nth-index '%s': unexpected format", arg);
            }
        }

        return ofType
            ? (last ? new Evaluator.IsNthLastOfType(step, offset) : new Evaluator.IsNthOfType(step, offset))
            : (last ? new Evaluator.IsNthLastChild(step, offset) : new Evaluator.IsNthChild(step, offset));
    }

    /** Consumes a parenthesized argument, requiring its opening delimiter. */
    private String consumeParens() {
        Validate.isTrue(tq.matches('('), "Expected '(' in query '" + query + "'");
        return tq.chompBalanced('(', ')');
    }

    private int consumeIndex() {
        String index = consumeParens().trim();
        Validate.isTrue(StringUtil.isNumeric(index), "Index must be numeric");
        return Integer.parseInt(index);
    }

    /** Parses {@code :has()}, grouping consecutive alternatives that can share a traversal. */
    private Evaluator has() {
        String err = ":has() must have a selector";
        SelectorGroup group = parseNestedSelectorGroup(err);
        return new HasEvaluator(group.toString(), planTraversals(group));
    }

    /**
     Groups alternatives that check the same elements.
     For example, {@code :has(> a, > span)} checks the children once.
     */
    private List<Traversal> planTraversals(SelectorGroup group) {
        List<Traversal> traversals = new ArrayList<>();
        ArrayList<Evaluator> alternatives = new ArrayList<>();
        boolean startsAtSibling = false, descend = false, nextSiblingOnly = false;
        for (ComplexSelector selector : group.selectors) {
            char leading = selector.leadingCombinator;
            boolean siblings = leading == '+' || leading == '~';
            boolean trees = selector.compounds.size() > 1 || !siblings && leading != '>';
            boolean nextOnly = leading == '+' && !selector.wantsNodes() &&
                !selector.combinators.contains('+') && !selector.combinators.contains('~');
            if (!alternatives.isEmpty() &&
                (siblings != startsAtSibling || trees != descend || nextOnly != nextSiblingOnly)) {
                traversals.add(new Traversal(alternatives, startsAtSibling, descend, nextSiblingOnly));
                alternatives = new ArrayList<>();
            }
            alternatives.add(selector.relativeEvaluator());
            startsAtSibling = siblings;
            descend = trees;
            nextSiblingOnly = nextOnly;
        }
        traversals.add(new Traversal(alternatives, startsAtSibling, descend, nextSiblingOnly));
        return traversals;
    }

    // pseudo selector :is()
    private Evaluator is() {
        return parseNested(StructuralEvaluator.Is::new, ":is() must have a selector");
    }

    /** Parses a nested selector group and applies its compiled evaluator. */
    private Evaluator parseNested(Function<Evaluator, Evaluator> func, String err) {
        return func.apply(parseNestedSelectorGroup(err).evaluator());
    }

    /** Parses a non-empty selector group enclosed in parentheses. */
    private SelectorGroup parseNestedSelectorGroup(String err) {
        Validate.isTrue(tq.matchChomp('('), err);
        tq.consumeWhitespace();
        Validate.isTrue(!tq.isEmpty() && !tq.matches(')'), err);
        Validate.isTrue(nestingDepth < MaxNestingDepth, "Selector nesting depth exceeds maximum of " + MaxNestingDepth);
        nestingDepth++;
        SelectorGroup group = parseSelectorGroup();
        nestingDepth--;
        Validate.isTrue(tq.matchChomp(')'), err);
        return group;
    }

    // pseudo selector :contains(text), containsOwn(text)
    private Evaluator contains(boolean own) {
        String query = own ? ":containsOwn" : ":contains";
        String searchText = TokenQueue.unescape(consumeParens());
        Validate.notEmpty(searchText, query + "(text) query must not be empty");

        if (inNodeContext)
            return new NodeEvaluator.ContainsValue(searchText);

        return own
            ? new Evaluator.ContainsOwnText(searchText)
            : new Evaluator.ContainsText(searchText);
    }

    private Evaluator containsWholeText(boolean own) {
        String query = own ? ":containsWholeOwnText" : ":containsWholeText";
        String searchText = TokenQueue.unescape(consumeParens());
        Validate.notEmpty(searchText, query + "(text) query must not be empty");
        return own
            ? new Evaluator.ContainsWholeOwnText(searchText)
            : new Evaluator.ContainsWholeText(searchText);
    }

    // pseudo selector :containsData(data)
    private Evaluator containsData() {
        String searchText = TokenQueue.unescape(consumeParens());
        Validate.notEmpty(searchText, ":containsData(text) query must not be empty");
        return new Evaluator.ContainsData(searchText);
    }

    // :matches(regex), matchesOwn(regex)
    private Evaluator matches(boolean own) {
        String query = own ? ":matchesOwn" : ":matches";
        Regex pattern = consumeRegex(query);

        if (inNodeContext)
            return new NodeEvaluator.MatchesValue(pattern);

        return own
            ? new Evaluator.MatchesOwn(pattern)
            : new Evaluator.Matches(pattern);
    }

    // :matchesWholeText(regex), matchesWholeOwnText(regex)
    private Evaluator matchesWholeText(boolean own) {
        String query = own ? ":matchesWholeOwnText" : ":matchesWholeText";
        Regex pattern = consumeRegex(query);
        return own
            ? new Evaluator.MatchesWholeOwnText(pattern)
            : new Evaluator.MatchesWholeText(pattern);
    }

    /** Consumes and compiles a non-empty regex argument, preserving its escapes. */
    private Regex consumeRegex(String pseudo) {
        String regex = consumeParens();
        Validate.notEmpty(regex, pseudo + "(regex) query must not be empty");
        return Regex.compile(regex);
    }

    /** Parses the selector group negated by {@code :not()}. */
    private Evaluator not() {
        return parseNested(StructuralEvaluator.Not::new, ":not(selector) subselect must not be empty");
    }

    @Override
    public String toString() {
        return query;
    }

    @Override
    public void close() {
        tq.close();
    }
}
