package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EvaluatorTest {

    @Test
    public void testTagToString() {
        Evaluator.Tag evaluator = new Evaluator.Tag("div");
        assertEquals("div", evaluator.toString());
    }

    @Test
    public void testTagStartsWithToString() {
        Evaluator.TagStartsWith evaluator = new Evaluator.TagStartsWith("ns");
        assertEquals("ns|*", evaluator.toString());
    }

    @Test
    public void testTagEndsWithToString() {
        Evaluator.TagEndsWith evaluator = new Evaluator.TagEndsWith("div");
        assertEquals("*|div", evaluator.toString());
    }

    @Test
    public void testAttributeToString() {
        Evaluator.Attribute evaluator = new Evaluator.Attribute("example");
        assertEquals("[example]", evaluator.toString());
    }

    @Test
    public void testAttributeStartingToString() {
        Evaluator.AttributeStarting evaluator = new Evaluator.AttributeStarting("example");
        assertEquals("[^example]", evaluator.toString());
    }

    @Test
    public void testAttributeWithValueToString() {
        Evaluator.AttributeWithValue evaluator = new Evaluator.AttributeWithValue("example", "value");
        assertEquals("[example=value]", evaluator.toString());
    }

    @Test
    public void testAttributeWithValueNotToString() {
        Evaluator.AttributeWithValueNot evaluator = new Evaluator.AttributeWithValueNot("example", "value");
        assertEquals("[example!=value]", evaluator.toString());
    }

    @Test
    public void testAttributeWithValueStartingToString() {
        Evaluator.AttributeWithValueStarting evaluator = new Evaluator.AttributeWithValueStarting("example", "value");
        assertEquals("[example^=value]", evaluator.toString());
    }

    @Test
    public void testAttributeWithValueEndingToString() {
        Evaluator.AttributeWithValueEnding evaluator = new Evaluator.AttributeWithValueEnding("example", "value");
        assertEquals("[example$=value]", evaluator.toString());
    }

    @Test
    public void testAttributeWithValueContainingToString() {
        Evaluator.AttributeWithValueContaining evaluator =
            new Evaluator.AttributeWithValueContaining("example", "value");
        assertEquals("[example*=value]", evaluator.toString());
    }

    @Test
    public void testAttributeWithValueMatchingToString() {
        Pattern pattern = Pattern.compile("value");
        Evaluator.AttributeWithValueMatching evaluator = new Evaluator.AttributeWithValueMatching("example", pattern);
        assertEquals("[example~=value]", evaluator.toString());
    }

    @Test
    public void testIdToString() {
        Evaluator.Id evaluator = new Evaluator.Id("exampleId");
        assertEquals("#exampleId", evaluator.toString());
    }

    @Test
    public void testClassToString() {
        Evaluator.Class evaluator = new Evaluator.Class("exampleClass");
        assertEquals(".exampleClass", evaluator.toString());
    }

    @Test
    public void testAllElementsToString() {
        Evaluator.AllElements evaluator = new Evaluator.AllElements();
        assertEquals("*", evaluator.toString());
    }

    @Test
    public void testIndexLessThanToString() {
        Evaluator.IndexLessThan evaluator = new Evaluator.IndexLessThan(5);
        assertEquals(":lt(5)", evaluator.toString());
    }

    @Test
    public void testIndexGreaterThanToString() {
        Evaluator.IndexGreaterThan evaluator = new Evaluator.IndexGreaterThan(5);
        assertEquals(":gt(5)", evaluator.toString());
    }

    @Test
    public void testIndexEqualsToString() {
        Evaluator.IndexEquals evaluator = new Evaluator.IndexEquals(5);
        assertEquals(":eq(5)", evaluator.toString());
    }

    @Test
    public void testIsLastChildToString() {
        Evaluator.IsLastChild evaluator = new Evaluator.IsLastChild();
        assertEquals(":last-child", evaluator.toString());
    }

    @Test
    public void testIsFirstOfTypeToString() {
        Evaluator.IsFirstOfType evaluator = new Evaluator.IsFirstOfType();
        assertEquals(":first-of-type", evaluator.toString());
    }

    @Test
    public void testIsLastOfTypeToString() {
        Evaluator.IsLastOfType evaluator = new Evaluator.IsLastOfType();
        assertEquals(":last-of-type", evaluator.toString());
    }

    @Test
    public void testIsNthChildToStringVariants() {
        Evaluator.IsNthChild evaluator1 = new Evaluator.IsNthChild(0, 3);
        assertEquals(":nth-child(3)", evaluator1.toString());

        Evaluator.IsNthChild evaluator2 = new Evaluator.IsNthChild(2, 0);
        assertEquals(":nth-child(2n)", evaluator2.toString());

        Evaluator.IsNthChild evaluator3 = new Evaluator.IsNthChild(2, 3);
        assertEquals(":nth-child(2n+3)", evaluator3.toString());
    }

    @Test
    public void testIsNthChildToString() {
        Evaluator.IsNthChild evaluator = new Evaluator.IsNthChild(2, 3);
        assertEquals(":nth-child(2n+3)", evaluator.toString());
    }

    @Test
    public void testIsNthLastChildToString() {
        Evaluator.IsNthLastChild evaluator = new Evaluator.IsNthLastChild(2, 3);
        assertEquals(":nth-last-child(2n+3)", evaluator.toString());
    }

    @Test
    public void testIsNthOfTypeToString() {
        Evaluator.IsNthOfType evaluator = new Evaluator.IsNthOfType(2, 3);
        assertEquals(":nth-of-type(2n+3)", evaluator.toString());
    }

    @Test
    public void testIsNthLastOfTypeToString() {
        Evaluator.IsNthLastOfType evaluator = new Evaluator.IsNthLastOfType(2, 3);
        assertEquals(":nth-last-of-type(2n+3)", evaluator.toString());
    }

    @Test
    public void testIsFirstChildToString() {
        Evaluator.IsFirstChild evaluator = new Evaluator.IsFirstChild();
        assertEquals(":first-child", evaluator.toString());
    }

    @Test
    public void testIsRootToString() {
        Evaluator.IsRoot evaluator = new Evaluator.IsRoot();
        assertEquals(":root", evaluator.toString());
    }

    @Test
    public void testIsOnlyChildToString() {
        Evaluator.IsOnlyChild evaluator = new Evaluator.IsOnlyChild();
        assertEquals(":only-child", evaluator.toString());
    }

    @Test
    public void testIsOnlyOfTypeToString() {
        Evaluator.IsOnlyOfType evaluator = new Evaluator.IsOnlyOfType();
        assertEquals(":only-of-type", evaluator.toString());
    }

    @Test
    public void testIsEmptyToString() {
        Evaluator.IsEmpty evaluator = new Evaluator.IsEmpty();
        assertEquals(":empty", evaluator.toString());
    }

    @Test
    public void testContainsTextToString() {
        Evaluator.ContainsText evaluator = new Evaluator.ContainsText("example");
        assertEquals(":contains(example)", evaluator.toString());
    }

    @Test
    public void testContainsWholeTextToString() {
        Evaluator.ContainsWholeText evaluator = new Evaluator.ContainsWholeText("example");
        assertEquals(":containsWholeText(example)", evaluator.toString());
    }

    @Test
    public void testContainsWholeOwnTextToString() {
        Evaluator.ContainsWholeOwnText evaluator = new Evaluator.ContainsWholeOwnText("example");
        assertEquals(":containsWholeOwnText(example)", evaluator.toString());
    }

    @Test
    public void testContainsDataToString() {
        Evaluator.ContainsData evaluator = new Evaluator.ContainsData("example");
        assertEquals(":containsData(example)", evaluator.toString());
    }

    @Test
    public void testContainsOwnTextToString() {
        Evaluator.ContainsOwnText evaluator = new Evaluator.ContainsOwnText("example");
        assertEquals(":containsOwn(example)", evaluator.toString());
    }

    @Test
    public void testMatchesToString() {
        Pattern pattern = Pattern.compile("example");
        Evaluator.Matches evaluator = new Evaluator.Matches(pattern);
        assertEquals(":matches(example)", evaluator.toString());
    }

    @Test
    public void testMatchesOwnToString() {
        Pattern pattern = Pattern.compile("example");
        Evaluator.MatchesOwn evaluator = new Evaluator.MatchesOwn(pattern);
        assertEquals(":matchesOwn(example)", evaluator.toString());
    }

    @Test
    public void testMatchesWholeTextToString() {
        Pattern pattern = Pattern.compile("example");
        Evaluator.MatchesWholeText evaluator = new Evaluator.MatchesWholeText(pattern);
        assertEquals(":matchesWholeText(example)", evaluator.toString());
    }

    @Test
    public void testMatchesWholeOwnTextToString() {
        Pattern pattern = Pattern.compile("example");
        Evaluator.MatchesWholeOwnText evaluator = new Evaluator.MatchesWholeOwnText(pattern);
        assertEquals(":matchesWholeOwnText(example)", evaluator.toString());
    }

    @Test
    public void testMatchTextToString() {
        Evaluator.MatchText evaluator = new Evaluator.MatchText();
        assertEquals(":matchText", evaluator.toString());
    }
}
