package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 Test suite for attribute parser.

 @author Jonathan Hedley, jonathan@hedley.net */
public class AttributeParseTest {

    @Test public void parsesRoughAttributeString() {
        String html = "<a id=\"123\" class=\"baz = 'bar'\" style = 'border: 2px'qux zim foo = 12 mux=18 />";
        // should be: <id=123>, <class=baz = 'bar'>, <qux=>, <zim=>, <foo=12>, <mux.=18>

        Element el = Jsoup.parse(html).getElementsByTag("a").get(0);
        Attributes attr = el.attributes();
        assertEquals(7, attr.size());
        assertEquals("123", attr.get("id"));
        assertEquals("baz = 'bar'", attr.get("class"));
        assertEquals("border: 2px", attr.get("style"));
        assertEquals("", attr.get("qux"));
        assertEquals("", attr.get("zim"));
        assertEquals("12", attr.get("foo"));
        assertEquals("18", attr.get("mux"));
    }

    @Test public void handlesNewLinesAndReturns() {
        String html = "<a\r\nfoo='bar\r\nqux'\r\nbar\r\n=\r\ntwo>One</a>";
        Element el = Jsoup.parse(html).select("a").first();
        assertEquals(2, el.attributes().size());
        assertEquals("bar\r\nqux", el.attr("foo")); // currently preserves newlines in quoted attributes. todo confirm if should.
        assertEquals("two", el.attr("bar"));
    }

    @Test public void parsesEmptyString() {
        String html = "<a />";
        Element el = Jsoup.parse(html).getElementsByTag("a").get(0);
        Attributes attr = el.attributes();
        assertEquals(0, attr.size());
    }

    @Test public void canStartWithEq() {
        String html = "<a =empty />";
        // TODO this is the weirdest thing in the spec - why not consider this an attribute with an empty name, not where name is '='?
        // am I reading it wrong? https://html.spec.whatwg.org/multipage/parsing.html#before-attribute-name-state
        Element el = Jsoup.parse(html).getElementsByTag("a").get(0);
        Attributes attr = el.attributes();
        assertEquals(1, attr.size());
        assertTrue(attr.hasKey("=empty"));
        assertEquals("", attr.get("=empty"));
    }

    @Test public void strictAttributeUnescapes() {
        String html = "<a id=1 href='?foo=bar&mid&lt=true'>One</a> <a id=2 href='?foo=bar&lt;qux&lg=1'>Two</a>";
        Elements els = Jsoup.parse(html).select("a");
        assertEquals("?foo=bar&mid&lt=true", els.first().attr("href"));
        assertEquals("?foo=bar<qux&lg=1", els.last().attr("href"));
    }

    @Test public void moreAttributeUnescapes() {
        String html = "<a href='&wr_id=123&mid-size=true&ok=&wr'>Check</a>";
        Elements els = Jsoup.parse(html).select("a");
        assertEquals("&wr_id=123&mid-size=true&ok=&wr", els.first().attr("href"));
    }

    @Test public void parsesBooleanAttributes() {
        String html = "<a normal=\"123\" boolean empty=\"\"></a>";
        Element el = Jsoup.parse(html).select("a").first();

        assertEquals("123", el.attr("normal"));
        assertEquals("", el.attr("boolean"));
        assertEquals("", el.attr("empty"));

        List<Attribute> attributes = el.attributes().asList();
        assertEquals(3, attributes.size(), "There should be 3 attribute present");

        assertEquals(html, el.outerHtml()); // vets boolean syntax
    }

    @Test public void dropsSlashFromAttributeName() {
        String html = "<img /onerror='doMyJob'/>";
        Document doc = Jsoup.parse(html);
        assertFalse(doc.select("img[onerror]").isEmpty(), "SelfClosingStartTag ignores last character");
        assertEquals("<img onerror=\"doMyJob\">", doc.body().html());

        doc = Jsoup.parse(html, "", Parser.xmlParser());
        assertEquals("<img onerror=\"doMyJob\" />", doc.html());
    }

    @Test void parseAttributesParsesNamesAndValues() {
        Attributes attributes = Parser.xmlParser().parseAttributes(
            "One='1' two = unquoted empty=\"\" flag encoded='&lt;'"
        );

        assertEquals(5, attributes.size());
        assertEquals("1", attributes.get("One"));
        assertEquals("unquoted", attributes.get("two"));
        assertEquals("", attributes.get("empty"));
        assertEquals("", attributes.get("flag"));
        assertEquals("<", attributes.get("encoded"));
    }

    @Test void parseAttributesUsesParserSettings() {
        Attributes html = Parser.htmlParser().parseAttributes("One=1 one=2");
        Attributes xml = Parser.xmlParser().parseAttributes("One=1 one=2");

        assertEquals(1, html.size());
        assertEquals("1", html.get("one"));
        assertEquals(2, xml.size());
        assertEquals("1", xml.get("One"));
        assertEquals("2", xml.get("one"));
    }

    @Test void parseAttributesLeavesParserReusable() {
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Attributes attributes = parser.parseAttributes("one=1 two=2");

        assertEquals("2", attributes.get("two"));
        assertTrue(parser.getErrors().isEmpty());
        assertEquals("Text", parser.parseInput("<p>Text", "").expectFirst("p").text());
    }

    @Test void parseAttributesAcceptsCompleteInputAtEof() {
        for (String input : new String[] {"", "flag", "flag ", "one=1", "one='1'", "one=\"1\""}) {
            Parser parser = Parser.htmlParser().setTrackErrors(10);

            parser.parseAttributes(input);

            assertTrue(parser.getErrors().isEmpty(), input + ": " + parser.getErrors());
        }
    }

    @Test void parseAttributesStopsAtTagCloser() {
        for (String input : new String[] {"foo=bar><a href>text", "foo=bar />text"}) {
            Parser parser = Parser.htmlParser().setTrackErrors(10);

            Attributes attributes = parser.parseAttributes(input);

            assertEquals(1, attributes.size());
            assertEquals("bar", attributes.get("foo"));
            assertEquals(1, parser.getErrors().size());
            assertTrue(parser.getErrors().get(0).getErrorMessage().contains("Unexpected tag closer"));
        }
    }

    @Test void parseAttributesAllowsCloserInQuotedValue() {
        Parser parser = Parser.htmlParser().setTrackErrors(10);

        Attributes attributes = parser.parseAttributes("foo='bar>baz'");

        assertEquals("bar>baz", attributes.get("foo"));
        assertTrue(parser.getErrors().isEmpty());
    }

    @Test void parseAttributesReportsIncompleteValue() {
        Parser parser = Parser.htmlParser().setTrackErrors(10);

        Attributes attributes = parser.parseAttributes("foo=");

        assertTrue(attributes.hasKey("foo"));
        assertEquals(1, parser.getErrors().size());
        assertTrue(parser.getErrors().get(0).getErrorMessage().contains("end of file"));
    }

    @Test void parseAttributesTracksSourceRanges() {
        Parser parser = Parser.xmlParser().setTrackErrors(10).setTrackPosition(true);
        Attributes attributes = parser.parseAttributes("One='1' two='unterminated");

        assertEquals("1,1:0-1,4:3=1,6:5-1,7:6", attributes.sourceRange("One").toString());
        assertEquals("unterminated", attributes.get("two"));
        assertFalse(parser.getErrors().isEmpty());
    }
}
