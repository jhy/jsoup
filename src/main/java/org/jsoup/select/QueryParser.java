package org.jsoup.select;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.parser.TokenQueue;

/**
 * Parses a CSS selector into an Evaluator tree.
 */
class QueryParser {
    private final static String[] combinators = {",", ">", "+", "~", " "};

    private TokenQueue tq;
    private String query;
    private List<Evaluator> evals = new ArrayList<Evaluator>();

    /**
     * Create a new QueryParser.
     * @param query CSS query
     */
    private QueryParser(String query) {
        this.query = query;
        this.tq = new TokenQueue(query);
    }

    /**
     * Parse a CSS query into an Evaluator.
     * @param query CSS query
     * @return Evaluator
     */
    public static Evaluator parse(String query) {
        QueryParser p = new QueryParser(query);
        return p.parse();
    }

    /**
     * Parse the query
     * @return Evaluator
     */
    Evaluator parse() {
        tq.consumeWhitespace();

        if (tq.matchesAny(combinators)) { // if starts with a combinator, use root as elements
            evals.add(new StructuralEvaluator.Root());
            combinator(tq.consume());
        } else {
            findElements();
        }

        while (!tq.isEmpty()) {
            // hierarchy and extras
            boolean seenWhite = tq.consumeWhitespace();

            if (tq.matchChomp(",")) { // group or
                CombiningEvaluator.Or or = new CombiningEvaluator.Or(evals);
                evals.clear();
                evals.add(or);
                while (!tq.isEmpty()) {
                    String subQuery = tq.chompTo(",");
                    or.add(parse(subQuery));
                }
            } else if (tq.matchesAny(combinators)) {
                combinator(tq.consume());
            } else if (seenWhite) {
                combinator(' ');
            } else { // E.class, E#id, E[attr] etc. AND
                findElements(); // take next el, #. etc off queue
            }
        }

        if (evals.size() == 1)
            return evals.get(0);

        return new CombiningEvaluator.And(evals);
    }

    private void combinator(char combinator) {
        tq.consumeWhitespace();
        String subQuery = consumeSubQuery(); // support multi > childs
        Evaluator e;

        if (evals.size() == 1)
            e = evals.get(0);
        else
            e = new CombiningEvaluator.And(evals);
        evals.clear();
        Evaluator f = parse(subQuery);

        if (combinator == '>')
            evals.add(new CombiningEvaluator.And(f, new StructuralEvaluator.ImmediateParent(e)));
        else if (combinator == ' ')
            evals.add(new CombiningEvaluator.And(f, new StructuralEvaluator.Parent(e)));
        else if (combinator == '+')
            evals.add(new CombiningEvaluator.And(f, new StructuralEvaluator.ImmediatePreviousSibling(e)));
        else if (combinator == '~')
            evals.add(new CombiningEvaluator.And(f, new StructuralEvaluator.PreviousSibling(e)));
        else
            throw new Selector.SelectorParseException("Unknown combinator: " + combinator);
    }

    private String consumeSubQuery() {
        StringBuilder sq = new StringBuilder();
        while (!tq.isEmpty()) {
            if (tq.matches("("))
                sq.append("(").append(tq.chompBalanced('(', ')')).append(")");
            else if (tq.matches("["))
                sq.append("[").append(tq.chompBalanced('[', ']')).append("]");
            else if (tq.matchesAny(combinators))
                break;
            else
                sq.append(tq.consume());
        }
        return sq.toString();
    }

    private void findElements() {
        if (tq.matchChomp("#"))
            byId();
        else if (tq.matchChomp("."))
            byClass();
        else if (tq.matchesWord())
            byTag();
        else if (tq.matches("["))
            byAttribute();
        else if (tq.matchChomp("*"))
            allElements();
        else if (tq.matchChomp(":lt("))
            indexLessThan();
        else if (tq.matchChomp(":gt("))
            indexGreaterThan();
        else if (tq.matchChomp(":eq("))
            indexEquals();
        else if (tq.matches(":has("))
            has();
        else if (tq.matches(":contains("))
            contains(false);
        else if (tq.matches(":containsOwn("))
            contains(true);
        else if (tq.matches(":matches("))
            matches(false);
        else if (tq.matches(":matchesOwn("))
            matches(true);
        else if (tq.matches(":not("))
            not();
        else // unhandled
            throw new Selector.SelectorParseException("Could not parse query '%s': unexpected token at '%s'", query, tq.remainder());

    }

    private void byId() {
        String id = tq.consumeCssIdentifier();
        Validate.notEmpty(id);
        evals.add(new Evaluator.Id(id));
    }

    private void byClass() {
        String className = tq.consumeCssIdentifier();
        Validate.notEmpty(className);
        evals.add(new Evaluator.Class(className.trim().toLowerCase()));
    }

    private void byTag() {
        String tagName = tq.consumeElementSelector();
        Validate.notEmpty(tagName);

        // namespaces: if element name is "abc:def", selector must be "abc|def", so flip:
        if (tagName.contains("|"))
            tagName = tagName.replace("|", ":");

        evals.add(new Evaluator.Tag(tagName.trim().toLowerCase()));
    }

    private void byAttribute() {
        TokenQueue cq = new TokenQueue(tq.chompBalanced('[', ']')); // content queue
        String key = cq.consumeToAny("=", "!=", "^=", "$=", "*=", "~="); // eq, not, start, end, contain, match, (no val)
        Validate.notEmpty(key);
        cq.consumeWhitespace();

        if (cq.isEmpty()) {
            if (key.startsWith("^"))
                evals.add(new Evaluator.AttributeStarting(key.substring(1)));
            else
                evals.add(new Evaluator.Attribute(key));
        } else {
            if (cq.matchChomp("="))
                evals.add(new Evaluator.AttributeWithValue(key, cq.remainder()));

            else if (cq.matchChomp("!="))
                evals.add(new Evaluator.AttributeWithValueNot(key, cq.remainder()));

            else if (cq.matchChomp("^="))
                evals.add(new Evaluator.AttributeWithValueStarting(key, cq.remainder()));

            else if (cq.matchChomp("$="))
                evals.add(new Evaluator.AttributeWithValueEnding(key, cq.remainder()));

            else if (cq.matchChomp("*="))
                evals.add(new Evaluator.AttributeWithValueContaining(key, cq.remainder()));

            else if (cq.matchChomp("~="))
                evals.add(new Evaluator.AttributeWithValueMatching(key, Pattern.compile(cq.remainder())));
            else
                throw new Selector.SelectorParseException("Could not parse attribute query '%s': unexpected token at '%s'", query, cq.remainder());
        }
    }

    private void allElements() {
        evals.add(new Evaluator.AllElements());
    }

    // pseudo selectors :lt, :gt, :eq
    private void indexLessThan() {
        evals.add(new Evaluator.IndexLessThan(consumeIndex()));
    }

    private void indexGreaterThan() {
        evals.add(new Evaluator.IndexGreaterThan(consumeIndex()));
    }

    private void indexEquals() {
        evals.add(new Evaluator.IndexEquals(consumeIndex()));
    }

    private int consumeIndex() {
        String indexS = tq.chompTo(")").trim();
        Validate.isTrue(StringUtil.isNumeric(indexS), "Index must be numeric");
        return Integer.parseInt(indexS);
    }

    // pseudo selector :has(el)
    private void has() {
        tq.consume(":has");
        String subQuery = tq.chompBalanced('(', ')');
        Validate.notEmpty(subQuery, ":has(el) subselect must not be empty");
        evals.add(new StructuralEvaluator.Has(parse(subQuery)));
    }

    // pseudo selector :contains(text), containsOwn(text)
    private void contains(boolean own) {
        tq.consume(own ? ":containsOwn" : ":contains");
        String searchText = TokenQueue.unescape(tq.chompBalanced('(', ')'));
        Validate.notEmpty(searchText, ":contains(text) query must not be empty");
        if (own)
            evals.add(new Evaluator.ContainsOwnText(searchText));
        else
            evals.add(new Evaluator.ContainsText(searchText));
    }

    // :matches(regex), matchesOwn(regex)
    private void matches(boolean own) {
        tq.consume(own ? ":matchesOwn" : ":matches");
        String regex = tq.chompBalanced('(', ')'); // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, ":matches(regex) query must not be empty");

        if (own)
            evals.add(new Evaluator.MatchesOwn(Pattern.compile(regex)));
        else
            evals.add(new Evaluator.Matches(Pattern.compile(regex)));
    }

    // :not(selector)
    private void not() {
        tq.consume(":not");
        String subQuery = tq.chompBalanced('(', ')');
        Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty");

        evals.add(new StructuralEvaluator.Not(parse(subQuery)));
    }
}
