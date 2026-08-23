package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

        body = "<textarea><fake></TeXtArEa>";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("<fake>", els.text());
        assertEquals(0, doc.select("fake").size());

        body = "<title><p>One</TiTlE>";
        doc = Jsoup.parse(body);
        els = doc.select("title");
        assertEquals("<p>One", els.text());
        assertEquals(0, doc.select("body p").size());

        body = "<textarea><open";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("<open", els.text());

        body = "<textarea>hello world</?fake</textarea>";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("hello world</?fake", els.text());
    }

    @ParameterizedTest
    @MethodSource
    void textEndTagParsing(String input, String expected) {
        Document doc = Jsoup.parseBodyFragment(input);
        assertEquals(expected, TextUtil.normalizeSpaces(doc.body().html()));
    }

    private static Arguments[] textEndTagParsing() {
        return new Arguments[] {
            arguments("<style></t</style><img>", "<style></t</style><img>"),
            arguments("<style></style</style><img>", "<style></style</style><img>"),
            arguments("<script></t</script><img>", "<script></t</script><img>"),
            arguments("<script><!--</t</script><img>", "<script><!--</t</script><img>"),
            arguments("<textarea></t</textarea><img>", "<textarea>&lt;/t</textarea><img>"),
            arguments("<textarea><img>", "<textarea>&lt;img&gt;</textarea>")
        };
    }

    @Test
    void scriptDoubleEscapeTagAtEof() {
        assertEquals("<!--<script", Jsoup.parse("<script><!--<script").expectFirst("script").data());
        assertEquals("<!--<script>foo</script", Jsoup.parse("<script><!--<script>foo</script").expectFirst("script").data());
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

    @Test
    public void testUnconsumeAtBufferBoundary() {
        // test for #1251: put the malformed post-quote attribute character at the reader refill boundary
        String triggeringSnippet = "<a href=\"\"foo";
        char[] padding = new char[CharacterReader.RefillPoint - triggeringSnippet.length() + 2]; // The "foo" part must be just at the limit.
        Arrays.fill(padding, ' ');
        String paddedSnippet = String.valueOf(padding) + triggeringSnippet;
        ParseErrorList errorList = ParseErrorList.tracking(1);

        Parser.parseFragment(paddedSnippet, null, "", errorList);

        assertEquals(CharacterReader.RefillPoint - 1, errorList.get(0).getPosition());
    }

    @Test
    public void testOpeningAngleBracketInsteadOfAttribute() {
        String triggeringSnippet = "<html <";
        ParseErrorList errorList = ParseErrorList.tracking(1);

        Parser.parseFragment(triggeringSnippet, null, "", errorList);

        assertEquals(7, errorList.get(0).getPosition());
    }

    @Test
    public void testMalformedSelfClosingTag() {
        String triggeringSnippet = "<html /ouch";
        ParseErrorList errorList = ParseErrorList.tracking(1);

        Parser.parseFragment(triggeringSnippet, null, "", errorList);

        assertEquals(7, errorList.get(0).getPosition());
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
        assertEquals("<di�v>One</di�v>Two", doc.body().html());
    }

    @Test
    public void attributeValUnquoted() {
        Document doc = Jsoup.parse("<p name=foo&lt;bar>");
        Element p = doc.selectFirst("p");
        assertEquals("foo<bar", p.attr("name"));

        doc = Jsoup.parse("<p foo=");
        assertEquals("<p foo></p>", doc.body().html());
    }

    @ParameterizedTest
    @MethodSource
    void customDataEndTags(String input, String expected) {
        // https://github.com/jhy/jsoup/issues/2332

        TagSet tagSet = TagSet.Html();
        tagSet.valueOf("custom-data", Parser.NamespaceHtml).set(Tag.Data);
        Document doc = Jsoup.parse("<body>" + input, Parser.htmlParser().tagSet(tagSet));
        assertEquals(expected, TextUtil.normalizeSpaces(doc.body().html()));
    }

    private static Arguments[] customDataEndTags() {
        return new Arguments[] {
            arguments("<custom-data>a < > b</custom-data><p>One</p>", "<custom-data>a < > b</custom-data><p>One</p>"),
            arguments("<custom-data></custom-x</custom-data><p>One</p>", "<custom-data></custom-x</custom-data><p>One</p>"),
            arguments("<custom-data>x</CUSTOM-DATA><p>One</p>", "<custom-data>x</custom-data><p>One</p>")
        };
    }

    @Test void customRcdataEndTag() {
        TagSet tagSet = TagSet.Html();
        tagSet.valueOf("custom-rcdata", Parser.NamespaceHtml).set(Tag.RcData);
        Document doc = Jsoup.parse("<body><custom-rcdata>a < > b</custom-rcdata><p>One</p>", Parser.htmlParser().tagSet(tagSet));
        assertEquals("<custom-rcdata>a &lt; &gt; b</custom-rcdata><p>One</p>", TextUtil.normalizeSpaces(doc.body().html()));
    }

    @Test void customTextEndTagAcrossBufferBoundary() {
        String prefix = "custom-";
        String name = prefix + StringUtil.padding(CharacterReader.BufferSize + 1 - prefix.length(), -1).replace(' ', 'x');

        assertCustomTextEndTag(name);
    }

    @Test void customTextEndTagWithNonAsciiLetter() {
        assertCustomTextEndTag("custom-ã");
    }

    private static void assertCustomTextEndTag(String name) {
        for (int textMode : new int[] {Tag.Data, Tag.RcData}) {
            TagSet tagSet = TagSet.Html();
            tagSet.valueOf(name, Parser.NamespaceHtml).set(textMode);
            Document doc = Jsoup.parse("<body><" + name + ">x</" + name + "><p>after</p>", Parser.htmlParser().tagSet(tagSet));

            assertEquals(2, doc.body().childrenSize());
            assertEquals("p", doc.body().child(1).normalName());
            assertEquals("after", doc.body().child(1).text());
        }
    }

    @Test void customDataTagWithHyphenXml() {
        String xml = "<custom-data>a < > b</custom-data><p>One</p><custom-rcdata>a < > b</custom-rcdata><p>Two</p>";
        Parser parser = Parser.xmlParser();
        TagSet tagSet = parser.tagSet();
        tagSet.valueOf("custom-data", Parser.NamespaceXml).set(Tag.Data);
        tagSet.valueOf("custom-rcdata", Parser.NamespaceXml).set(Tag.RcData);

        Document doc = Jsoup.parse(xml, parser);
        assertEquals(
            "<custom-data><![CDATA[a < > b]]></custom-data><p>One</p><custom-rcdata>a &lt; &gt; b</custom-rcdata><p>Two</p>",
            TextUtil.normalizeSpaces(doc.html()));
    }
}
