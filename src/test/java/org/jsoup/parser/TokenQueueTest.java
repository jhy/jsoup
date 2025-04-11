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

    // https://github.com/web-platform-tests/wpt/blob/36036fb5212a3fc15fc5750cecb1923ba4071668/dom/nodes/ParentNode-querySelector-escapes.html
    @Test public void consumeCssIdentifier_WebPlatformTests() {
        // - escape hex digit
        assertParsedCssIdentifierEquals("0nextIsWhiteSpace", "\\30 nextIsWhiteSpace");
        assertParsedCssIdentifierEquals("0nextIsNotHexLetters", "\\30nextIsNotHexLetters");
        assertParsedCssIdentifierEquals("0connectHexMoreThan6Hex", "\\000030connectHexMoreThan6Hex");
        assertParsedCssIdentifierEquals("0spaceMoreThan6Hex", "\\000030 spaceMoreThan6Hex");

        // - hex digit special replacement
        // 1. zero points
        assertParsedCssIdentifierEquals("zero\uFFFD", "zero\\0");
        assertParsedCssIdentifierEquals("zero\uFFFD", "zero\\000000");
        // 2. surrogate points
        assertParsedCssIdentifierEquals("\uFFFDsurrogateFirst", "\\d83d surrogateFirst");
        assertParsedCssIdentifierEquals("surrogateSecond\uFFFd", "surrogateSecond\\dd11");
        assertParsedCssIdentifierEquals("surrogatePair\uFFFD\uFFFD", "surrogatePair\\d83d\\dd11");
        // 3. out of range points
        assertParsedCssIdentifierEquals("outOfRange\uFFFD", "outOfRange\\110000");
        assertParsedCssIdentifierEquals("outOfRange\uFFFD", "outOfRange\\110030");
        assertParsedCssIdentifierEquals("outOfRange\uFFFD", "outOfRange\\555555");
        assertParsedCssIdentifierEquals("outOfRange\uFFFD", "outOfRange\\ffffff");

        // - escape anything else
        assertParsedCssIdentifierEquals(".comma", "\\.comma");
        assertParsedCssIdentifierEquals("-minus", "\\-minus");
        assertParsedCssIdentifierEquals("g", "\\g");

        // non edge cases
        assertParsedCssIdentifierEquals("aBMPRegular", "\\61 BMPRegular");
        assertParsedCssIdentifierEquals("\uD83D\uDD11nonBMP", "\\1f511 nonBMP");
        assertParsedCssIdentifierEquals("00continueEscapes", "\\30\\30 continueEscapes");
        assertParsedCssIdentifierEquals("00continueEscapes", "\\30 \\30 continueEscapes");
        assertParsedCssIdentifierEquals("continueEscapes00", "continueEscapes\\30 \\30 ");
        assertParsedCssIdentifierEquals("continueEscapes00", "continueEscapes\\30 \\30");
        assertParsedCssIdentifierEquals("continueEscapes00", "continueEscapes\\30\\30 ");
        assertParsedCssIdentifierEquals("continueEscapes00", "continueEscapes\\30\\30");

        // ident tests case from CSS tests of chromium source: https://goo.gl/3Cxdov
        assertParsedCssIdentifierEquals("hello", "hel\\6Co");
        assertParsedCssIdentifierEquals("&B", "\\26 B");
        assertParsedCssIdentifierEquals("hello", "hel\\6C o");
        assertParsedCssIdentifierEquals("spaces", "spac\\65\r\ns");
        assertParsedCssIdentifierEquals("spaces", "sp\\61\tc\\65\fs");
        assertParsedCssIdentifierEquals("test\uD799", "test\\D799");
        assertParsedCssIdentifierEquals("\uE000", "\\E000");
        assertParsedCssIdentifierEquals("test", "te\\s\\t");
        assertParsedCssIdentifierEquals("spaces in\tident", "spaces\\ in\\\tident");
        assertParsedCssIdentifierEquals(".,:!", "\\.\\,\\:\\!");
        assertParsedCssIdentifierEquals("null\uFFFD", "null\\0");
        assertParsedCssIdentifierEquals("null\uFFFD", "null\\0000");
        assertParsedCssIdentifierEquals("large\uFFFD", "large\\110000");
        assertParsedCssIdentifierEquals("large\uFFFD", "large\\23456a");
        assertParsedCssIdentifierEquals("surrogate\uFFFD", "surrogate\\D800");
        assertParsedCssIdentifierEquals("surrogate\uFFFD", "surrogate\\0DBAC");
        assertParsedCssIdentifierEquals("\uFFFDsurrogate", "\\00DFFFsurrogate");
        assertParsedCssIdentifierEquals("\uDBFF\uDFFF", "\\10fFfF");
        assertParsedCssIdentifierEquals("\uDBFF\uDFFF0", "\\10fFfF0");
        assertParsedCssIdentifierEquals("\uDBC0\uDC0000", "\\10000000");
        assertParsedCssIdentifierEquals("eof\uFFFD", "eof\\");

        assertParsedCssIdentifierEquals("simple-ident", "simple-ident");
        assertParsedCssIdentifierEquals("testing123", "testing123");
        assertParsedCssIdentifierEquals("_underscore", "_underscore");
        assertParsedCssIdentifierEquals("-text", "-text");
        assertParsedCssIdentifierEquals("-m", "-\\6d");
        assertParsedCssIdentifierEquals("--abc", "--abc");
        assertParsedCssIdentifierEquals("--", "--");
        assertParsedCssIdentifierEquals("--11", "--11");
        assertParsedCssIdentifierEquals("---", "---");
        assertParsedCssIdentifierEquals("\u2003", "\u2003");
        assertParsedCssIdentifierEquals("\u00A0", "\u00A0");
        assertParsedCssIdentifierEquals("\u1234", "\u1234");
        assertParsedCssIdentifierEquals("\uD808\uDF45", "\uD808\uDF45");
        assertParsedCssIdentifierEquals("\uFFFD", "\u0000");
        assertParsedCssIdentifierEquals("ab\uFFFDc", "ab\u0000c");
    }

    @Test public void consumeCssIdentifier_additional() {
        assertParsedCssIdentifierEquals("1st", "\\31\r\nst");
        assertParsedCssIdentifierEquals("1", "\\31\r");
        assertParsedCssIdentifierEquals("1a", "\\31\ra");
        assertParsedCssIdentifierEquals("1", "\\031");
        assertParsedCssIdentifierEquals("1", "\\0031");
        assertParsedCssIdentifierEquals("1", "\\00031");
        assertParsedCssIdentifierEquals("1", "\\000031");
        assertParsedCssIdentifierEquals("1", "\\000031");
        assertParsedCssIdentifierEquals("a", "a\\\nb");

        try {
            parseCssIdentifier("");
            fail("Expected failure");
        } catch (IllegalArgumentException e) {
            assertEquals("CSS identifier expected, but end of input found", e.getMessage());
        }
    }

    // Some of jsoup's tests depend on this behavior
    @Test public void consumeCssIdentifier_invalidButSupportedForBackwardsCompatibility() {
        assertParsedCssIdentifierEquals("1", "1");
        assertParsedCssIdentifierEquals("-", "-");
        assertParsedCssIdentifierEquals("-1", "-1");
    }

    private static String parseCssIdentifier(String text) {
        TokenQueue q = new TokenQueue(text);
        return q.consumeCssIdentifier();
    }

    private void assertParsedCssIdentifierEquals(String expected, String cssIdentifier) {
        assertEquals(expected, parseCssIdentifier(cssIdentifier));
    }
}
