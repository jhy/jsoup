package org.jsoup.select;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Selector Query Parser.
 *
 * @author Jonathan Hedley
 */
public class QueryParserTest {
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
            assertTrue(and.evaluators.get(0) instanceof Evaluator.Tag);
            assertTrue(and.evaluators.get(1) instanceof StructuralEvaluator.Parent);
        }
    }

    @Test public void testParsesMultiCorrectly() {
        Evaluator eval = QueryParser.parse(".foo > ol, ol > li + li");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(2, or.evaluators.size());

        CombiningEvaluator.And andLeft = (CombiningEvaluator.And) or.evaluators.get(0);
        CombiningEvaluator.And andRight = (CombiningEvaluator.And) or.evaluators.get(1);

        assertEquals("ol :ImmediateParent.foo", andLeft.toString());
        assertEquals(2, andLeft.evaluators.size());
        assertEquals("li :prevli :ImmediateParentol", andRight.toString());
        assertEquals(2, andLeft.evaluators.size());
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
        assertEquals("div :parentspan", parse.toString()); // TODO - don't really love that toString() result...
    }
}
