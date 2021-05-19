package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Selector Query Parser.
 *
 * @author Jonathan Hedley
 */
public class QueryParserTest {

    @Test public void testConsumeSubQuery() {
        Document doc = Jsoup.parse("<html><head>h</head><body>" +
                "<li><strong>l1</strong></li>" +
                "<a><li><strong>l2</strong></li></a>" +
                "<p><strong>yes</strong></p>" +
                "</body></html>");
        assertEquals("l1 l2 yes", doc.body().select(">p>strong,>*>li>strong").text());
        assertEquals("l2 yes", doc.select("body>p>strong,body>*>li>strong").text());
        assertEquals("yes", doc.select(">body>*>li>strong,>body>p>strong").text());
        assertEquals("l2", doc.select(">body>p>strong,>body>*>li>strong").text());
    }

    @Test public void testOrGetsCorrectPrecedence() {
        // tests that a selector "a b, c d, e f" evals to (a AND b) OR (c AND d) OR (e AND f)"
        // top level or, three child ands
        Evaluator eval = QueryParser.parse("a b, c d, e f");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(3, or.evaluators.size());
        for (Evaluator innerEval: or.evaluators) {
            assertTrue(innerEval instanceof CombiningEvaluator.And);
            CombiningEvaluator.And and = (CombiningEvaluator.And) innerEval;
            assertEquals(2, and.evaluators.size());
            assertTrue(and.evaluators.get(0) instanceof StructuralEvaluator.Parent);
            assertTrue(and.evaluators.get(1) instanceof Evaluator.Tag);
        }
    }

    @Test public void testParsesMultiCorrectly() {
        String query = ".foo > ol, ol > li + li";
        Evaluator eval = QueryParser.parse(query);
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(2, or.evaluators.size());

        CombiningEvaluator.And andLeft = (CombiningEvaluator.And) or.evaluators.get(0);
        CombiningEvaluator.And andRight = (CombiningEvaluator.And) or.evaluators.get(1);

        assertEquals(".foo > ol", andLeft.toString());
        assertEquals(2, andLeft.evaluators.size());
        assertEquals("ol > li + li", andRight.toString());
        assertEquals(2, andRight.evaluators.size());
        assertEquals(query, eval.toString());
    }

    @Test public void exceptionOnUncloseAttribute() {
        assertThrows(Selector.SelectorParseException.class, () -> QueryParser.parse("section > a[href=\"]"));
    }

    @Test public void testParsesSingleQuoteInContains() {
        assertThrows(Selector.SelectorParseException.class, () -> QueryParser.parse("p:contains(One \" One)"));
    }


    @Test public void exceptOnEmptySelector() {
        assertThrows(Selector.SelectorParseException.class, () -> QueryParser.parse(""));
    }

    @Test public void exceptOnNullSelector() {
        assertThrows(Selector.SelectorParseException.class, () -> QueryParser.parse(null));
    }

    @Test public void okOnSpacesForeAndAft() {
        Evaluator parse = QueryParser.parse(" span div  ");
        assertEquals("span div", parse.toString());
    }

    @Test public void structuralEvaluatorsToString() {
        String q = "a:not(:has(span.foo)) b d > e + f ~ g";
        Evaluator parse = QueryParser.parse(q);
        assertEquals(q, parse.toString());
    }
}
