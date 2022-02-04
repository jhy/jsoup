package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.parser.TokenQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jsoup.internal.Normalizer.normalize;

/**
 * Parses a CSS selector into an Evaluator tree.
 */
public class QueryParser {
    private final static String[] combinators = {",", ">", "+", "~", " "};
    private static final String[] AttributeEvals = new String[]{"=", "!=", "^=", "$=", "*=", "~="};

    private final TokenQueue tq;
    private final String query;
    private final List<Evaluator> evals = new ArrayList<>();

    /**
     * Create a new QueryParser.
     * @param query CSS query
     */
    private QueryParser(String query) {
        Validate.notEmpty(query);
        query = query.trim();
        this.query = query;
        this.tq = new TokenQueue(query);
    }

    /**
     * Parse a CSS query into an Evaluator.
     * @param query CSS query
     * @return Evaluator
     * @see Selector selector query syntax
     */
    public static Evaluator parse(String query) {
        try {
            QueryParser p = new QueryParser(query);
            return p.parse();
        } catch (IllegalArgumentException e) {
            throw new Selector.SelectorParseException(e.getMessage());
        }
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

            if (tq.matchesAny(combinators)) {
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

        Evaluator rootEval; // the new topmost evaluator
        Evaluator currentEval; // the evaluator the new eval will be combined to. could be root, or rightmost or.
        Evaluator newEval = parse(subQuery); // the evaluator to add into target evaluator
        boolean replaceRightMost = false;

        if (evals.size() == 1) {
            rootEval = currentEval = evals.get(0);
            // make sure OR (,) has precedence:
            if (rootEval instanceof CombiningEvaluator.Or && combinator != ',') {
                currentEval = ((CombiningEvaluator.Or) currentEval).rightMostEvaluator();
                assert currentEval != null; // rightMost signature can return null (if none set), but always will have one by this point
                replaceRightMost = true;
            }
        }
        else {
            rootEval = currentEval = new CombiningEvaluator.And(evals);
        }
        evals.clear();

        // for most combinators: change the current eval into an AND of the current eval and the new eval
        switch (combinator) {
            case '>':
                currentEval = new CombiningEvaluator.And(new StructuralEvaluator.ImmediateParent(currentEval), newEval);
                break;
            case ' ':
                currentEval = new CombiningEvaluator.And(new StructuralEvaluator.Parent(currentEval), newEval);
                break;
            case '+':
                currentEval = new CombiningEvaluator.And(new StructuralEvaluator.ImmediatePreviousSibling(currentEval), newEval);
                break;
            case '~':
                currentEval = new CombiningEvaluator.And(new StructuralEvaluator.PreviousSibling(currentEval), newEval);
                break;
            case ',':
                CombiningEvaluator.Or or;
                if (currentEval instanceof CombiningEvaluator.Or) {
                    or = (CombiningEvaluator.Or) currentEval;
                } else {
                    or = new CombiningEvaluator.Or();
                    or.add(currentEval);
                }
                or.add(newEval);
                currentEval = or;
                break;
            default:
                throw new Selector.SelectorParseException("Unknown combinator '%s'", combinator);
        }

        if (replaceRightMost)
            ((CombiningEvaluator.Or) rootEval).replaceRightMostEvaluator(currentEval);
        else rootEval = currentEval;
        evals.add(rootEval);
    }

    private String consumeSubQuery() {
        StringBuilder sq = StringUtil.borrowBuilder();
        while (!tq.isEmpty()) {
            if (tq.matches("("))
                sq.append("(").append(tq.chompBalanced('(', ')')).append(")");
            else if (tq.matches("["))
                sq.append("[").append(tq.chompBalanced('[', ']')).append("]");
            else if (tq.matchesAny(combinators))
                if (sq.length() > 0)
                    break;
                else
                    tq.consume();
            else
                sq.append(tq.consume());
        }
        return StringUtil.releaseBuilder(sq);
    }

    private void findElements() {
        if (tq.matchChomp("#"))
            byId();
        else if (tq.matchChomp("."))
            byClass();
        else if (tq.matchesWord() || tq.matches("*|"))
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
        else if (tq.matches(":containsWholeText("))
            containsWholeText(false);
        else if (tq.matches(":containsWholeOwnText("))
            containsWholeText(true);
        else if (tq.matches(":containsData("))
            containsData();
        else if (tq.matches(":matches("))
            matches(false);
        else if (tq.matches(":matchesOwn("))
            matches(true);
        else if (tq.matches(":matchesWholeText("))
            matchesWholeText(false);
        else if (tq.matches(":matchesWholeOwnText("))
            matchesWholeText(true);
        else if (tq.matches(":not("))
            not();
		else if (tq.matchChomp(":nth-child("))
        	cssNthChild(false, false);
        else if (tq.matchChomp(":nth-last-child("))
        	cssNthChild(true, false);
        else if (tq.matchChomp(":nth-of-type("))
        	cssNthChild(false, true);
        else if (tq.matchChomp(":nth-last-of-type("))
        	cssNthChild(true, true);
        else if (tq.matchChomp(":first-child"))
        	evals.add(new Evaluator.IsFirstChild());
        else if (tq.matchChomp(":last-child"))
        	evals.add(new Evaluator.IsLastChild());
        else if (tq.matchChomp(":first-of-type"))
        	evals.add(new Evaluator.IsFirstOfType());
        else if (tq.matchChomp(":last-of-type"))
        	evals.add(new Evaluator.IsLastOfType());
        else if (tq.matchChomp(":only-child"))
        	evals.add(new Evaluator.IsOnlyChild());
        else if (tq.matchChomp(":only-of-type"))
        	evals.add(new Evaluator.IsOnlyOfType());
        else if (tq.matchChomp(":empty"))
        	evals.add(new Evaluator.IsEmpty());
        else if (tq.matchChomp(":root"))
        	evals.add(new Evaluator.IsRoot());
        else if (tq.matchChomp(":matchText"))
            evals.add(new Evaluator.MatchText());
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
        evals.add(new Evaluator.Class(className.trim()));
    }

    private void byTag() {
        // todo - these aren't dealing perfectly with case sensitivity. For case sensitive parsers, we should also make
        // the tag in the selector case-sensitive (and also attribute names). But for now, normalize (lower-case) for
        // consistency - both the selector and the element tag
        String tagName = normalize(tq.consumeElementSelector());
        Validate.notEmpty(tagName);

        // namespaces: wildcard match equals(tagName) or ending in ":"+tagName
        if (tagName.startsWith("*|")) {
            String plainTag = tagName.substring(2); // strip *|
            evals.add(new CombiningEvaluator.Or(
                new Evaluator.Tag(plainTag),
                new Evaluator.TagEndsWith(tagName.replace("*|", ":")))
            );
        } else {
            // namespaces: if element name is "abc:def", selector must be "abc|def", so flip:
            if (tagName.contains("|"))
                tagName = tagName.replace("|", ":");

            evals.add(new Evaluator.Tag(tagName));
        }
    }

    private void byAttribute() {
        TokenQueue cq = new TokenQueue(tq.chompBalanced('[', ']')); // content queue
        String key = cq.consumeToAny(AttributeEvals); // eq, not, start, end, contain, match, (no val)
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
    
    //pseudo selectors :first-child, :last-child, :nth-child, ...
    private static final Pattern NTH_AB = Pattern.compile("(([+-])?(\\d+)?)n(\\s*([+-])?\\s*\\d+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern NTH_B  = Pattern.compile("([+-])?(\\d+)");

	private void cssNthChild(boolean backwards, boolean ofType) {
		String argS = normalize(tq.chompTo(")"));
		Matcher mAB = NTH_AB.matcher(argS);
		Matcher mB = NTH_B.matcher(argS);
		final int a, b;
		if ("odd".equals(argS)) {
			a = 2;
			b = 1;
		} else if ("even".equals(argS)) {
			a = 2;
			b = 0;
		} else if (mAB.matches()) {
			a = mAB.group(3) != null ? Integer.parseInt(mAB.group(1).replaceFirst("^\\+", "")) : 1;
			b = mAB.group(4) != null ? Integer.parseInt(mAB.group(4).replaceFirst("^\\+", "")) : 0;
		} else if (mB.matches()) {
			a = 0;
			b = Integer.parseInt(mB.group().replaceFirst("^\\+", ""));
		} else {
			throw new Selector.SelectorParseException("Could not parse nth-index '%s': unexpected format", argS);
		}
		if (ofType)
			if (backwards)
				evals.add(new Evaluator.IsNthLastOfType(a, b));
			else
				evals.add(new Evaluator.IsNthOfType(a, b));
		else {
			if (backwards)
				evals.add(new Evaluator.IsNthLastChild(a, b));
			else
				evals.add(new Evaluator.IsNthChild(a, b));
		}
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
        Validate.notEmpty(subQuery, ":has(selector) subselect must not be empty");
        evals.add(new StructuralEvaluator.Has(parse(subQuery)));
    }

    // pseudo selector :contains(text), containsOwn(text)
    private void contains(boolean own) {
        String query = own ? ":containsOwn" : ":contains";
        tq.consume(query);
        String searchText = TokenQueue.unescape(tq.chompBalanced('(', ')'));
        Validate.notEmpty(searchText, query + "(text) query must not be empty");
        evals.add(own
            ? new Evaluator.ContainsOwnText(searchText)
            : new Evaluator.ContainsText(searchText));
    }

    private void containsWholeText(boolean own) {
        String query = own ? ":containsWholeOwnText" : ":containsWholeText";
        tq.consume(query);
        String searchText = TokenQueue.unescape(tq.chompBalanced('(', ')'));
        Validate.notEmpty(searchText, query + "(text) query must not be empty");
        evals.add(own
            ? new Evaluator.ContainsWholeOwnText(searchText)
            : new Evaluator.ContainsWholeText(searchText));
    }

    // pseudo selector :containsData(data)
    private void containsData() {
        tq.consume(":containsData");
        String searchText = TokenQueue.unescape(tq.chompBalanced('(', ')'));
        Validate.notEmpty(searchText, ":containsData(text) query must not be empty");
        evals.add(new Evaluator.ContainsData(searchText));
    }

    // :matches(regex), matchesOwn(regex)
    private void matches(boolean own) {
        String query = own ? ":matchesOwn" : ":matches";
        tq.consume(query);
        String regex = tq.chompBalanced('(', ')'); // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, query + "(regex) query must not be empty");

        evals.add(own
            ? new Evaluator.MatchesOwn(Pattern.compile(regex))
            : new Evaluator.Matches(Pattern.compile(regex)));
    }

    // :matches(regex), matchesOwn(regex)
    private void matchesWholeText(boolean own) {
        String query = own ? ":matchesWholeOwnText" : ":matchesWholeText";
        tq.consume(query);
        String regex = tq.chompBalanced('(', ')'); // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, query + "(regex) query must not be empty");

        evals.add(own
            ? new Evaluator.MatchesWholeOwnText(Pattern.compile(regex))
            : new Evaluator.MatchesWholeText(Pattern.compile(regex)));
    }

    // :not(selector)
    private void not() {
        tq.consume(":not");
        String subQuery = tq.chompBalanced('(', ')');
        Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty");

        evals.add(new StructuralEvaluator.Not(parse(subQuery)));
    }

    @Override
    public String toString() {
        return query;
    }


}
