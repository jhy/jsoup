package org.jsoup.select;

import org.jsoup.helper.Regex;
import org.jsoup.internal.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.TokenQueue;
import org.jsoup.select.HasEvaluator.Scope;
import org.jsoup.select.HasEvaluator.Traversal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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
    private final static String[] AttributeEvals = new String[]{"=", "!=", "^=", "$=", "*=", "~="};
    private final static char[] SequenceEnders = {',', ')'};

    private final TokenQueue tq;
    private final String query;
    private boolean inNodeContext; // ::comment:contains should act on node value, vs element text

    /**
     * Create a new QueryParser.
     * @param query CSS query
     */
    private QueryParser(String query) {
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
        try (QueryParser p = new QueryParser(query)) {
            return p.parse();
        } catch (IllegalArgumentException e) {
            throw new Selector.SelectorParseException(e.getMessage());
        }
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

    /** Parses a selector into an evaluator and records its top-level combinators. */
    private ComplexSelector parseSelector() {
        // Selector ::= [ Combinator ] SimpleSequence ( Combinator SimpleSequence )*
        tq.consumeWhitespace();

        Evaluator left;
        char leading = 0;
        int combinators = 0;
        boolean continuesToSibling = false; // the continuation has a + or ~ combinator, so it may leave the first sibling tree
        boolean relative = tq.matchesAny(Combinators);
        if (relative) {
            // e.g. query is "> div"; left side is the root element
            left = new StructuralEvaluator.Root();
        } else {
            left = parseSimpleSequence();
        }
        Evaluator rightmost = left; // keep each right side before it is folded into left

        while (true) {
            char combinator = 0;
            if (tq.consumeWhitespace())
                combinator = ' ';            // maybe descendant?
            if (tq.matchesAny(Combinators)) // no, explicit
                combinator = tq.consume();
            else if (tq.matchesAny(SequenceEnders)) // , - space after simple like "foo , bar"; ) - close of :has()
                break;

            if (combinator != 0) {
                if (relative && combinators == 0) leading = combinator;
                else if (relative && (combinator == '+' || combinator == '~')) continuesToSibling = true;
                Evaluator right = parseSimpleSequence();
                rightmost = right;
                left = combinator(left, combinator, right);
                combinators++;
            } else {
                break;
            }
        }
        return new ComplexSelector(left, rightmost, leading, combinators, continuesToSibling);
    }

    /**
     A complex selector compiled with details of its top-level combinators.
     Nested selectors are parsed separately and do not affect these details.
     */
    private static final class ComplexSelector {
        final Evaluator evaluator; // complete compiled selector
        final Evaluator rightmost; // evaluator for the final simple sequence
        final char leadingCombinator;
        final int combinatorCount;
        final boolean continuesToSibling; // the continuation has a top-level + or ~ combinator

        /** Creates a complex selector from its evaluator and top-level combinator details. */
        ComplexSelector(Evaluator evaluator, Evaluator rightmost, char leadingCombinator, int combinatorCount,
                        boolean continuesToSibling) {
            this.evaluator = evaluator;
            this.rightmost = rightmost;
            this.leadingCombinator = leadingCombinator;
            this.combinatorCount = combinatorCount;
            this.continuesToSibling = continuesToSibling;
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
            Evaluator evaluator = selectors.get(0).evaluator;
            for (int i = 1; i < selectors.size(); i++)
                evaluator = or(evaluator, selectors.get(i).evaluator);
            return evaluator;
        }

        @Override public String toString() {
            StringBuilder out = new StringBuilder();
            for (ComplexSelector selector : selectors) {
                if (out.length() > 0) out.append(", ");
                out.append(selector.evaluator);
            }
            return out.toString();
        }
    }

    Evaluator parseSimpleSequence() {
        // SimpleSequence ::= TypeSelector? ( Hash | Class | Pseudo )*
        Evaluator left = null;
        tq.consumeWhitespace();

        // one optional type selector
        if (tq.matchesWord() || tq.matches("*|"))
            left = byTag();
        else if (tq.matchChomp('*'))
            left = new Evaluator.AllElements();

        // zero or more subclasses (#, ., [)
        while(true) {
            Evaluator right = parseSubclass();
            if (right != null) {
                left = and(left, right);
            }
            else break; // no more simple tokens
        }

        if (left == null)
            throw new Selector.SelectorParseException("Could not parse query '%s': unexpected token at '%s'", query, tq.remainder());
        return left;
    }

    static Evaluator combinator(Evaluator left, char combinator, Evaluator right) {
        switch (combinator) {
            case '>':
                ImmediateParentRun run = left instanceof ImmediateParentRun ?
                    (ImmediateParentRun) left : new ImmediateParentRun(left);
                run.add(right);
                return run;
            case ' ':
                return and(new StructuralEvaluator.Ancestor(left), right);
            case '+':
                return and(new StructuralEvaluator.ImmediatePreviousSibling(left), right);
            case '~':
                return and(new StructuralEvaluator.PreviousSibling(left), right);
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

    /** Merge two evals into an Or. */
    static Evaluator or(Evaluator left, Evaluator right) {
        if (left instanceof CombiningEvaluator.Or) {
            ((CombiningEvaluator.Or) left).add(right);
            return left;
        }
        return new CombiningEvaluator.Or(left, right);
    }

    /** Merge two evals into an And. */
    static Evaluator and(@Nullable Evaluator left, Evaluator right) {
        if (left == null) return right;
        if (left instanceof CombiningEvaluator.And) {
            ((CombiningEvaluator.And) left).add(right);
            return left;
        }
        return new CombiningEvaluator.And(left, right);
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
        Evaluator left;
        switch (pseudo) {
            case "node":
                left = new NodeEvaluator.InstanceType(Node.class, pseudo);
                break;
            case "leafnode":
                left = new NodeEvaluator.InstanceType(LeafNode.class, pseudo);
                break;
            case "text":
                left = new NodeEvaluator.InstanceType(TextNode.class, pseudo);
                break;
            case "comment":
                left = new NodeEvaluator.InstanceType(Comment.class, pseudo);
                break;
            case "data":
                left = new NodeEvaluator.InstanceType(DataNode.class, pseudo);
                break;
            case "cdata":
                left = new NodeEvaluator.InstanceType(CDataNode.class, pseudo);
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
            Evaluator right;
            while ((right = parseSubclass()) != null) {
                left = and(left, right);
            }
            return left;
        } finally {
            inNodeContext = previousNodeContext;
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
        // todo - these aren't dealing perfectly with case sensitivity. For case sensitive parsers, we should also make
        // the tag in the selector case-sensitive (and also attribute names). But for now, normalize (lower-case) for
        // consistency - both the selector and the element tag
        String tagName = asciiLowerCase(tq.consumeElementSelector());
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
        try (TokenQueue cq = new TokenQueue(tq.chompBalanced('[', ']'))) {
            return evaluatorForAttribute(cq);
        }
    }

    private Evaluator evaluatorForAttribute(TokenQueue cq) {
        String key = cq.consumeToAny(AttributeEvals); // eq, not, start, end, contain, match, (no val)
        key = asciiLowerCase(trimAsciiWhitespace(key));
        Validate.notEmpty(key);
        Validate.isFalse(key.equals("abs:"), "Absolute attribute key must have a name");
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

    private String consumeParens() {
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
     Chooses where to look and what to test for each {@code :has()} alternative.
     Alternatives with the same scope share one traversal, so {@code :has(> a, > span)} visits the subject's children once.
     */
    private static List<Traversal> planTraversals(SelectorGroup group) {
        List<Traversal> traversals = new ArrayList<>();
        for (ComplexSelector selector : group.selectors) {
            Scope scope = traversalScope(selector); // top-level combinators determine how far :has() must traverse
            Evaluator evaluator = traversalEvaluator(selector, scope);
            int last = traversals.size() - 1;
            Traversal previous = last >= 0 ? traversals.get(last) : null;
            if (previous != null && previous.scope == scope) {
                traversals.set(last, new Traversal(or(previous.evaluator, evaluator), scope));
            } else {
                traversals.add(new Traversal(evaluator, scope));
            }
        }
        return traversals;
    }

    /** Uses the rightmost evaluator when the traversal scope already guarantees the leading relationship. */
    private static Evaluator traversalEvaluator(ComplexSelector selector, Scope scope) {
        if (selector.combinatorCount != 1)
            return selector.evaluator;

        char leading = selector.leadingCombinator;
        if (leading == '>' || leading == '~' || (leading == '+' && scope == Scope.NextSibling))
            return selector.rightmost;
        return selector.evaluator;
    }

    /** Chooses where to traverse for a {@code :has()} alternative. */
    private static Scope traversalScope(ComplexSelector selector) {
        char leading = selector.leadingCombinator;
        if (leading == '>' && selector.combinatorCount == 1)
            return Scope.Children;
        if (leading == '+' || leading == '~') {
            if (selector.combinatorCount > 1) {
                if (leading == '+' && !selector.continuesToSibling && !selector.evaluator.wantsNodes())
                    return Scope.NextSiblingTree;
                return Scope.FollowingSiblingTrees;
            }
            if (leading == '+' && !selector.evaluator.wantsNodes())
                return Scope.NextSibling;
            return Scope.FollowingSiblings;
        }
        return Scope.Descendants;
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
        SelectorGroup group = parseSelectorGroup();
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
        String regex = consumeParens(); // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, query + "(regex) query must not be empty");
        Regex pattern = Regex.compile(regex);

        if (inNodeContext)
            return new NodeEvaluator.MatchesValue(pattern);

        return own
            ? new Evaluator.MatchesOwn(pattern)
            : new Evaluator.Matches(pattern);
    }

    // :matches(regex), matchesOwn(regex)
    private Evaluator matchesWholeText(boolean own) {
        String query = own ? ":matchesWholeOwnText" : ":matchesWholeText";
        String regex = consumeParens(); // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, query + "(regex) query must not be empty");

        Regex pattern = Regex.compile(regex);
        return own
            ? new Evaluator.MatchesWholeOwnText(pattern)
            : new Evaluator.MatchesWholeText(pattern);
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
