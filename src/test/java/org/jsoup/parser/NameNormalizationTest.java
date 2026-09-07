package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Range;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class NameNormalizationTest {
    @Test void integrationEncodingMatchesWithoutTrimmingOrUnicodeFolding() {
        for (String encoding : new String[]{"text/html", "application/xhtml+xml"}) {
            for (String value : new String[]{encoding, encoding.toUpperCase(Locale.ROOT)}) {
                Document doc = Jsoup.parse("<math><annotation-xml encoding='" + value + "'><xmp><b>One</b></xmp>");
                assertEquals(Parser.NamespaceHtml, doc.expectFirst("xmp").tag().namespace());
                assertEquals("<b>One</b>", doc.expectFirst("xmp").data());
            }
            for (String whitespace : new String[]{" ", "\t", "\n", "\r", "\f", "\u0001", "\u000b", "\u00a0"}) {
                for (String value : new String[]{whitespace + encoding, encoding + whitespace}) {
                    Document doc = Jsoup.parse("<math><annotation-xml encoding='" + value + "'><xmp><b>One</b></xmp>");
                    assertEquals(Parser.NamespaceMathml, doc.expectFirst("xmp").tag().namespace(), value);
                    assertNotNull(doc.selectFirst("body > b"));
                }
            }
        }
        Document doc = Jsoup.parse("<math><annotation-xml encoding='applıcation/xhtml+xml'><xmp>One</xmp>");
        assertEquals(Parser.NamespaceMathml, doc.expectFirst("xmp").tag().namespace());
    }

    @Test void integrationEncodingAttributeNamesStayDistinct() {
        for (String key : new String[]{"encodıng", "encoding\u0001"}) {
            Document doc = Jsoup.parse("<math><annotation-xml " + key + "='text/html'><xmp>One</xmp>");
            assertEquals(Parser.NamespaceMathml, doc.expectFirst("xmp").tag().namespace());
            assertFalse(doc.expectFirst("annotation-xml").hasAttr("encoding"));
        }
        Document doc = Jsoup.parse("<math><annotation-xml ENCODING='text/html'><xmp>One</xmp>");
        assertEquals(Parser.NamespaceHtml, doc.expectFirst("xmp").tag().namespace());
    }

    @Test void doctypeNameNormalizationRespectsCaseSettings() {
        for (ParseSettings settings : new ParseSettings[]{ParseSettings.htmlDefault, ParseSettings.preserveCase}) {
            Document doc = Jsoup.parse("<!DOCTYPE HTMK\u0001><p>One", Parser.htmlParser().settings(settings));
            assertEquals(settings.preserveTagCase() ? "HTMK\u0001" : "htmK\u0001", doc.documentType().name());
        }
    }

    @Test void namesRetainNonAsciiLettersAndControls() {
        Document doc = Jsoup.parse("<LINK>One</LINK><p\u0001>Two</p\u0001><p>Three</p>");
        Element link = doc.body().child(0);
        assertEquals("linK", link.tagName());
        assertFalse(link.tag().isEmpty());
        assertEquals("One", link.text());
        assertEquals("Two", doc.getElementsByTag("p\u0001").text());
        assertEquals("Three", doc.getElementsByTag(" \tP\n").text());
        assertEquals("Two", doc.select("p\\\u0001").text());
        assertEquals("One", doc.select("LINK").text());
        assertEquals(doc.body().html(), Jsoup.parse(doc.body().html()).body().html());
        assertEquals("p\u0001", new Tag("p\u0001").normalName());
        assertEquals("p\u0001", Tag.valueOf(" p\u0001 ").normalName());
    }

    @Test void attributesKeepIdentityAndSourceRanges() {
        String html = "<p DATA-X\u0001=one data-x=two K=three k=four Ä=five ä=six>";
        for (ParseSettings settings : new ParseSettings[]{ParseSettings.htmlDefault, ParseSettings.preserveCase}) {
            Document doc = Jsoup.parse(html, Parser.htmlParser().settings(settings).setTrackPosition(true));
            Element p = doc.expectFirst("p");
            assertEquals(6, p.attributes().size());
            assertEquals("one", p.attr("data-x\u0001"));
            assertEquals("two", p.attr("data-x"));
            assertEquals("three", p.attr("K"));
            assertEquals("four", p.attr("k"));
            assertEquals("five", p.attr("Ä"));
            assertEquals("six", p.attr("ä"));
            p.attributes().forEach(attr -> assertEquals(attr.getValue(), p.attr(attr.getKey())));
            String firstName = settings.preserveAttributeCase() ? "DATA-X\u0001" : "data-x\u0001";
            Range range = p.attributes().sourceRange(firstName).nameRange();
            assertEquals("DATA-X\u0001", html.substring(range.start().pos(), range.end().pos()));
            assertEquals(6, Jsoup.parse(p.outerHtml()).expectFirst("p").attributes().size());
            assertEquals("one", p.clone().attr("data-x\u0001"));
            p.attr("K", "updated");
            assertEquals("three", p.attr("K"));
            p.removeAttr("k");
            assertTrue(p.hasAttr("K"));
            assertFalse(p.hasAttr("k"));
        }
    }

    @Test void xmlClosesExactNamesAndNormalizesOnlyAsciiWhenRequested() {
        Document doc = Jsoup.parse("<r><a>one</a\u0001><b>two</b></r>", "", Parser.xmlParser());
        assertEquals("a", doc.expectFirst("b").parent().tagName());
        String xml = "<R><x-Ä X='1' x='2' K='3' k='4'>One</x-ä><B>Two</B></x-Ä></R>";
        for (ParseSettings settings : new ParseSettings[]{ParseSettings.preserveCase, ParseSettings.htmlDefault}) {
            doc = Jsoup.parse(xml, "", Parser.xmlParser().settings(settings));
            Element a = doc.child(0).child(0);
            assertEquals("x-Ä", a.tagName());
            assertEquals("x-Ä", a.child(0).parent().tagName());
            assertEquals(settings.preserveAttributeCase() ? 4 : 3, a.attributes().size());
            assertEquals("3", a.attr("K"));
            assertEquals("4", a.attr("k"));
        }
    }

    @Test void rawTextEndNamesUseAsciiAcrossBufferBoundaries() {
        for (int length : new int[]{0, CharacterReader.BufferSize - 8, CharacterReader.BufferSize + 3}) {
            String padding = StringUtil.padding(length, length);
            Document doc = Jsoup.parse("<script>" + padding + "</ſcript><b>literal</b></SCRIPT><p>After</p>");
            assertNull(doc.selectFirst("b"));
            assertEquals("After", doc.expectFirst("p").text());
            assertTrue(doc.expectFirst("script").data().endsWith("</ſcript><b>literal</b>"));
        }
        TagSet tags = TagSet.Html();
        tags.add(new Tag("custom-Ä").set(Tag.Data));
        Document doc = Jsoup.parse("<custom-Ä>One</custom-ä><b>literal</b></CUSTOM-Ä><p>After</p>", Parser.htmlParser().tagSet(tags));
        assertNull(doc.selectFirst("b"));
        assertEquals("After", doc.expectFirst("p").text());
    }
}
