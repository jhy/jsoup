package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Token queue tests.
 */
public class TokenQueueTest {
    @Test public void chompBalanced() {
        TokenQueue tq = new TokenQueue(":contains(one (two) three) four");
        String pre = tq.consumeTo("(");
        String guts = tq.chompBalanced('(', ')');
        String remainder = tq.remainder();

        assertEquals(":contains", pre);
        assertEquals("one (two) three", guts);
        assertEquals(" four", remainder);
    }

    @Test public void chompEscapedBalanced() {
        TokenQueue tq = new TokenQueue(":contains(one (two) \\( \\) \\) three) four");
        String pre = tq.consumeTo("(");
        String guts = tq.chompBalanced('(', ')');
        String remainder = tq.remainder();

        assertEquals(":contains", pre);
        assertEquals("one (two) \\( \\) \\) three", guts);
        assertEquals("one (two) ( ) ) three", TokenQueue.unescape(guts));
        assertEquals(" four", remainder);
    }

    @Test public void chompBalancedMatchesAsMuchAsPossible() {
        TokenQueue tq = new TokenQueue("unbalanced(something(or another)) else");
        tq.consumeTo("(");
        String match = tq.chompBalanced('(', ')');
        assertEquals("something(or another)", match);
    }

    @Test public void unescape() {
        assertEquals("one ( ) \\", TokenQueue.unescape("one \\( \\) \\\\"));
    }

    @Test public void unescape_2() {
        assertEquals("\\&", TokenQueue.unescape("\\\\\\&"));
    }

    @Test public void escapeCssIdentifier() {
        assertEquals("one\\#two\\.three\\/four\\\\five", TokenQueue.escapeCssIdentifier("one#two.three/four\\five"));
    }

    @Test public void chompToIgnoreCase() {
        String t = "<textarea>one < two </TEXTarea>";
        TokenQueue tq = new TokenQueue(t);
        String data = tq.chompToIgnoreCase("</textarea");
        assertEquals("<textarea>one < two ", data);

        tq = new TokenQueue("<textarea> one two < three </oops>");
        data = tq.chompToIgnoreCase("</textarea");
        assertEquals("<textarea> one two < three </oops>", data);
    }

    @Test public void addFirst() {
        TokenQueue tq = new TokenQueue("One Two");
        tq.consumeWord();
        tq.addFirst("Three");
        assertEquals("Three Two", tq.remainder());
    }


    @Test public void consumeToIgnoreSecondCallTest() {
        String t = "<textarea>one < two </TEXTarea> third </TEXTarea>";
        TokenQueue tq = new TokenQueue(t);
        String data = tq.chompToIgnoreCase("</textarea>");
        assertEquals("<textarea>one < two ", data);

        data = tq.chompToIgnoreCase("</textarea>");
        assertEquals(" third ", data);
    }

    @Test public void testNestedQuotes() {
        validateNestedQuotes("<html><body><a id=\"identifier\" onclick=\"func('arg')\" /></body></html>", "a[onclick*=\"('arg\"]");
        validateNestedQuotes("<html><body><a id=\"identifier\" onclick=func('arg') /></body></html>", "a[onclick*=\"('arg\"]");
        validateNestedQuotes("<html><body><a id=\"identifier\" onclick='func(\"arg\")' /></body></html>", "a[onclick*='(\"arg']");
        validateNestedQuotes("<html><body><a id=\"identifier\" onclick=func(\"arg\") /></body></html>", "a[onclick*='(\"arg']");
    }

    private static void validateNestedQuotes(String html, String selector) {
        assertEquals("#identifier", Jsoup.parse(html).select(selector).first().cssSelector());
    }

    @Test
    public void chompBalancedThrowIllegalArgumentException() {
        try {
            TokenQueue tq = new TokenQueue("unbalanced(something(or another)) else");
            tq.consumeTo("(");
            tq.chompBalanced('(', '+');
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Did not find balanced marker at 'something(or another)) else'", expected.getMessage());
        }
    }

    @Test
    public void testQuotedPattern() {
        final Document doc = Jsoup.parse("<div>\\) foo1</div><div>( foo2</div><div>1) foo3</div>");
        assertEquals("\n\\) foo1",doc.select("div:matches(" + Pattern.quote("\\)") + ")").get(0).childNode(0).toString());
        assertEquals("\n( foo2",doc.select("div:matches(" + Pattern.quote("(") + ")").get(0).childNode(0).toString());
        assertEquals("\n1) foo3",doc.select("div:matches(" + Pattern.quote("1)") + ")").get(0).childNode(0).toString());
    }

    @Test public void consumeEscapedTag() {
        TokenQueue q = new TokenQueue("p\\\\p p\\.p p\\:p p\\!p");

        assertEquals("p\\p", q.consumeElementSelector());
        assertTrue(q.consumeWhitespace());

        assertEquals("p.p", q.consumeElementSelector());
        assertTrue(q.consumeWhitespace());

        assertEquals("p:p", q.consumeElementSelector());
        assertTrue(q.consumeWhitespace());

        assertEquals("p!p", q.consumeElementSelector());
        assertTrue(q.isEmpty());
    }

    @Test public void consumeEscapedId() {
        TokenQueue q = new TokenQueue("i\\.d i\\\\d");

        assertEquals("i.d", q.consumeCssIdentifier());
        assertTrue(q.consumeWhitespace());

        assertEquals("i\\d", q.consumeCssIdentifier());
        assertTrue(q.isEmpty());
    }
}
