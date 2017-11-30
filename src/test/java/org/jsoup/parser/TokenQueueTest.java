package org.jsoup.parser;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Token queue tests.
 */
@RunWith(JUnitParamsRunner.class)
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

    @Test @Parameters(method = "singleQuotesInsideDouble, doubleQuotesInsideSingle")
    public void testNestedQuotes(String html, String selector) {
        assertEquals("#identifier", Jsoup.parse(html).select(selector).first().cssSelector());
    }

    public Object[] singleQuotesInsideDouble() {
        return new Object[] {new String[] {
            "<html><body><a id=\"identifier\" onclick=\"func('arg')\" /></body></html>",
            "a[onclick*=\"('arg\"]"},
            new String[] {"<html><body><a id=\"identifier\" onclick=func('arg') /></body></html>",
                "a[onclick*=\"('arg\"]"}};
    }

    public Object[] doubleQuotesInsideSingle() {
        return new Object[] {new String[] {
            "<html><body><a id=\"identifier\" onclick='func(\"arg\")' /></body></html>",
            "a[onclick*='(\"arg']"},
            new String[] {"<html><body><a id=\"identifier\" onclick=func(\"arg\") /></body></html>",
                "a[onclick*='(\"arg']"}};
    }
}
