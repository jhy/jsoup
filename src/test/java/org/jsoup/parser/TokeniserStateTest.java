package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokeniserStateTest {

    final char[] whiteSpace = { '\t', '\n', '\r', '\f', ' ' };
    final char[] quote = { '\'', '"' };

    @Test
    public void ensureSearchArraysAreSorted() {
        char[][] arrays = {
            TokeniserState.attributeNameCharsSorted,
            TokeniserState.attributeValueUnquoted
        };

        for (char[] array : arrays) {
            char[] copy = Arrays.copyOf(array, array.length);
            Arrays.sort(array);
            assertArrayEquals(array, copy);
        }
    }

    @Test
    public void testCharacterReferenceInRcdata() {
        String body = "<textarea>You&I</textarea>";
        Document doc = Jsoup.parse(body);
        Elements els = doc.select("textarea");
        assertEquals("You&I", els.text());
    }

    @Test
    public void testBeforeTagName() {
        for (char c : whiteSpace) {
            String body = String.format("<div%c>test</div>", c);
            Document doc = Jsoup.parse(body);
            Elements els = doc.select("div");
            assertEquals("test", els.text());
        }
    }

    @Test
    public void testEndTagOpen() {
        String body;
        Document doc;
        Elements els;

        body = "<div>hello world</";
        doc = Jsoup.parse(body);
        els = doc.select("div");
        assertEquals("hello world</", els.text());

        body = "<div>hello world</div>";
        doc = Jsoup.parse(body);
        els = doc.select("div");
        assertEquals("hello world", els.text());

        body = "<div>fake</></div>";
        doc = Jsoup.parse(body);
        els = doc.select("div");
        assertEquals("fake", els.text());

        body = "<div>fake</?</div>";
        doc = Jsoup.parse(body);
        els = doc.select("div");
        assertEquals("fake", els.text());
    }

    @Test
    public void testRcdataLessthanSign() {
        String body;
        Document doc;
        Elements els;

        body = "<textarea><fake></textarea>";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("<fake>", els.text());

        body = "<textarea><open";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("", els.text());

        body = "<textarea>hello world</?fake</textarea>";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("hello world</?fake", els.text());
    }

    @Test
    public void testRCDATAEndTagName() {
        for (char c : whiteSpace) {
            String body = String.format("<textarea>data</textarea%c>", c);
            Document doc = Jsoup.parse(body);
            Elements els = doc.select("textarea");
            assertEquals("data", els.text());
        }
    }

    @Test
    public void testCommentEndCoverage() {
        String html = "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --! --- --><p>Hello</p></body></html>";
        Document doc = Jsoup.parse(html);

        Element body = doc.body();
        Comment comment = (Comment) body.childNode(1);
        assertEquals(" <table><tr><td></table> --! --- ", comment.getData());
        Element p = body.child(1);
        TextNode text = (TextNode) p.childNode(0);
        assertEquals("Hello", text.getWholeText());
    }

    @Test
    public void testCommentEndBangCoverage() {
        String html = "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --!---!>--><p>Hello</p></body></html>";
        Document doc = Jsoup.parse(html);

        Element body = doc.body();
        Comment comment = (Comment) body.childNode(1);
        assertEquals(" <table><tr><td></table> --!-", comment.getData());
        Element p = body.child(1);
        TextNode text = (TextNode) p.childNode(0);
        assertEquals("Hello", text.getWholeText());
    }

    @Test
    public void testPublicIdentifiersWithWhitespace() {
        String expectedOutput = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0//EN\">";
        for (char q : quote) {
            for (char ws : whiteSpace) {
                String[] htmls = {
                        String.format("<!DOCTYPE html%cPUBLIC %c-//W3C//DTD HTML 4.0//EN%c>", ws, q, q),
                        String.format("<!DOCTYPE html %cPUBLIC %c-//W3C//DTD HTML 4.0//EN%c>", ws, q, q),
                        String.format("<!DOCTYPE html PUBLIC%c%c-//W3C//DTD HTML 4.0//EN%c>", ws, q, q),
                        String.format("<!DOCTYPE html PUBLIC %c%c-//W3C//DTD HTML 4.0//EN%c>", ws, q, q),
                        String.format("<!DOCTYPE html PUBLIC %c-//W3C//DTD HTML 4.0//EN%c%c>", q, q, ws),
                        String.format("<!DOCTYPE html PUBLIC%c-//W3C//DTD HTML 4.0//EN%c%c>", q, q, ws)
                    };
                for (String html : htmls) {
                    Document doc = Jsoup.parse(html);
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml());
                }
            }
        }
    }

    @Test
    public void testSystemIdentifiersWithWhitespace() {
        String expectedOutput = "<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/REC-html40/strict.dtd\">";
        for (char q : quote) {
            for (char ws : whiteSpace) {
                String[] htmls = {
                        String.format("<!DOCTYPE html%cSYSTEM %chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", ws, q, q),
                        String.format("<!DOCTYPE html %cSYSTEM %chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", ws, q, q),
                        String.format("<!DOCTYPE html SYSTEM%c%chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", ws, q, q),
                        String.format("<!DOCTYPE html SYSTEM %c%chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", ws, q, q),
                        String.format("<!DOCTYPE html SYSTEM %chttp://www.w3.org/TR/REC-html40/strict.dtd%c%c>", q, q, ws),
                        String.format("<!DOCTYPE html SYSTEM%chttp://www.w3.org/TR/REC-html40/strict.dtd%c%c>", q, q, ws)
                    };
                for (String html : htmls) {
                    Document doc = Jsoup.parse(html);
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml());
                }
            }
        }
    }

    @Test
    public void testPublicAndSystemIdentifiersWithWhitespace() {
        String expectedOutput = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0//EN\""
                + " \"http://www.w3.org/TR/REC-html40/strict.dtd\">";
    	for (char q : quote) {
            for (char ws : whiteSpace) {
                String[] htmls = {
                        String.format("<!DOCTYPE html PUBLIC %c-//W3C//DTD HTML 4.0//EN%c"
                                + "%c%chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", q, q, ws, q, q),
                        String.format("<!DOCTYPE html PUBLIC %c-//W3C//DTD HTML 4.0//EN%c"
                                + "%chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", q, q, q, q)
                    };
                for (String html : htmls) {
                    Document doc = Jsoup.parse(html);
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml());
                }
            }
        }
    }

    @Test public void handlesLessInTagThanAsNewTag() {
        // out of spec, but clear author intent
        String html = "<p\n<p<div id=one <span>Two";
        Document doc = Jsoup.parse(html);
        assertEquals("<p></p><p></p><div id=\"one\"><span>Two</span></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testUnconsumeAtBufferBoundary() {
        String triggeringSnippet = "<a href=\"\"foo";
        char[] padding = new char[CharacterReader.readAheadLimit - triggeringSnippet.length() + 2]; // The "foo" part must be just at the limit.
        Arrays.fill(padding, ' ');
        String paddedSnippet = String.valueOf(padding) + triggeringSnippet;
        ParseErrorList errorList = ParseErrorList.tracking(1);

        Parser.parseFragment(paddedSnippet, null, "", errorList);

        assertEquals(CharacterReader.readAheadLimit - 1, errorList.get(0).getPosition());
    }

    @Test
    public void testUnconsumeAfterBufferUp() {
        // test for after consume() a bufferUp occurs (look-forward) but then attempts to unconsume. Would throw a "No buffer left to unconsume"
        String triggeringSnippet = "<title>One <span>Two";
        char[] padding = new char[CharacterReader.readAheadLimit - triggeringSnippet.length() + 8]; // The "<span" part must be just at the limit. The "containsIgnoreCase" scan does a bufferUp, losing the unconsume
        Arrays.fill(padding, ' ');
        String paddedSnippet = String.valueOf(padding) + triggeringSnippet;
        ParseErrorList errorList = ParseErrorList.tracking(1);
        Parser.parseFragment(paddedSnippet, null, "", errorList);
        // just asserting we don't get a WTF on unconsume
    }

    @Test
    public void testOpeningAngleBracketInsteadOfAttribute() {
        String triggeringSnippet = "<html <";
        ParseErrorList errorList = ParseErrorList.tracking(1);

        Parser.parseFragment(triggeringSnippet, null, "", errorList);

        assertEquals(6, errorList.get(0).getPosition());
    }

    @Test
    public void testMalformedSelfClosingTag() {
        String triggeringSnippet = "<html /ouch";
        ParseErrorList errorList = ParseErrorList.tracking(1);

        Parser.parseFragment(triggeringSnippet, null, "", errorList);

        assertEquals(7, errorList.get(0).getPosition());
    }

    @Test
    public void testOpeningAngleBracketInTagName() {
        String triggeringSnippet = "<html<";
        ParseErrorList errorList = ParseErrorList.tracking(1);

        Parser.parseFragment(triggeringSnippet, null, "", errorList);

        assertEquals(5, errorList.get(0).getPosition());
    }

    @Test
    public void rcData() {
        Document doc = Jsoup.parse("<title>One \0Two</title>");
        assertEquals("One �Two", doc.title());
    }

    @Test
    public void plaintext() {
        Document doc = Jsoup.parse("<div>One<plaintext><div>Two</plaintext>\0no < Return");
        assertEquals("<html><head></head><body><div>One<plaintext>&lt;div&gt;Two&lt;/plaintext&gt;�no &lt; Return</plaintext></div></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void nullInTag() {
        Document doc = Jsoup.parse("<di\0v>One</di\0v>Two");
        assertEquals("<di�v>\n One\n</di�v>Two", doc.body().html());
    }

    @Test
    public void attributeValUnquoted() {
        Document doc = Jsoup.parse("<p name=foo&lt;bar>");
        Element p = doc.selectFirst("p");
        assertEquals("foo<bar", p.attr("name"));

        doc = Jsoup.parse("<p foo=");
        assertEquals("<p foo></p>", doc.body().html());
    }
}
