package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Range;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class HtmlTextHandlingTest {
    @Test void ignoresInitialNewline() {
        for (String tag : new String[]{"pre", "listing", "textarea"}) {
            for (String newline : new String[]{"\n", "\r", "\r\n", "&#10;", "&#xA;", "&NewLine;"}) {
                Element el = Jsoup.parse("<" + tag + ">" + newline + "\r\nA</" + tag + ">").expectFirst(tag);
                assertEquals("\r\nA", el.wholeText(), tag + ": " + newline);
            }
            assertEquals("\r\nA", Jsoup.parse("<" + tag + ">&#13;\nA</" + tag + ">").expectFirst(tag).wholeText());
            assertEquals(" \nA", Jsoup.parse("<" + tag + "> \nA</" + tag + ">").expectFirst(tag).wholeText());
            for (String newline : new String[]{"\n", "\r", "\r\n", "&#10;"})
                assertEquals(0, Jsoup.parse("<" + tag + ">" + newline + "</" + tag + ">").expectFirst(tag).childNodeSize());
            Element context = new Element(tag);
            assertEquals("\r\nA", ((TextNode) Parser.parseFragment("\r\nA", context, "").get(0)).getWholeText());
        }
        assertEquals("\nA", Jsoup.parse("<pre><!--c-->\nA</pre>").expectFirst("pre").wholeText());
        assertEquals("\nA", Jsoup.parse("<pre><i></i>\nA</pre>").expectFirst("pre").wholeText());
        assertEquals("�\nA", Jsoup.parse("<pre>&#0;\nA</pre>").expectFirst("pre").wholeText());
    }

    @Test void roundTripsLeadingNewlines() {
        for (String tag : new String[]{"pre", "listing", "textarea"}) {
            for (String text : new String[]{"\nA", "\rA", "\r\nA", "\r\rA", "\n\nA"}) {
                for (boolean pretty : new boolean[]{false, true}) {
                    Document doc = Jsoup.parse("<" + tag + ">\n" + text + "</" + tag + ">");
                    doc.outputSettings().prettyPrint(pretty);
                    Element el = doc.expectFirst(tag);
                    assertEquals("<" + tag + ">\n" + text + "</" + tag + ">", el.outerHtml());
                    for (int round = 0; round < 3; round++) {
                        el = Jsoup.parse(el.outerHtml()).expectFirst(tag);
                        assertEquals(text, el.wholeText());
                    }
                    if (!pretty) assertEquals(text, doc.expectFirst(tag).html());
                }
                Element el = new Element(tag).appendChild(new TextNode(text));
                assertEquals(text, Jsoup.parse(el.outerHtml()).expectFirst(tag).wholeText());
            }
            Element referencedCr = Jsoup.parse("<" + tag + ">&#13;A</" + tag + ">").expectFirst(tag);
            assertEquals("\rA", Jsoup.parse(referencedCr.outerHtml()).expectFirst(tag).wholeText());
        }
        assertEquals("<pre><!--c-->\nA</pre>", Jsoup.parse("<pre><!--c-->\nA</pre>").expectFirst("pre").outerHtml());
        Element foreign = new Element(Tag.valueOf("pre", Parser.NamespaceSvg, ParseSettings.preserveCase), "");
        foreign.appendChild(new TextNode("\nA"));
        assertFalse(foreign.outerHtml().contains(">\n\n"));
    }

    @Test void initialNewlineAcrossBuffer() {
        for (String tag : new String[]{"pre", "listing", "textarea"}) {
            for (String newline : new String[]{"\r\n", "\r", "\n", "&#10;", "&NewLine;"}) {
                for (int chunk : new int[]{1, 7, CharacterReader.BufferSize}) {
                    for (int delta = -1; delta <= 1; delta++) {
                        char[] padding = new char[CharacterReader.BufferSize - tag.length() - 2 + delta];
                        Arrays.fill(padding, ' ');
                        String prefix = new String(padding) + "<" + tag + ">";
                        String input = prefix + newline + "\r\nA</" + tag + ">";
                        Document doc = Parser.htmlParser().setTrackPosition(true).parseInput(chunked(input, chunk), "");
                        Element el = doc.expectFirst(tag);
                        assertEquals("\r\nA", el.wholeText());
                        assertEquals(prefix.length() + newline.length(), el.childNode(0).sourceRange().startPos());
                        assertEquals(input.indexOf("</"), el.childNode(0).sourceRange().endPos());
                    }
                }
            }
        }
    }

    @Test void preservesLineEndings() {
        String text = "A\r\nB\rC\nD";
        Document doc = Jsoup.parse("<p x='" + text + "'>" + text + "</p><!--" + text + "--><script>" + text + "</script>");
        doc.outputSettings().prettyPrint(false);
        assertEquals(text, doc.expectFirst("p").wholeText());
        assertEquals(text, doc.expectFirst("p").attr("x"));
        assertEquals(text, doc.expectFirst("script").data());
        assertTrue(doc.outerHtml().contains("<!--" + text + "-->"));
        assertTrue(doc.expectFirst("p").outerHtml().contains(text));
    }

    @Test void xmlLineEndings() {
        String input = "<pre>\r\n&#13;A</pre>";
        Document xml = Jsoup.parse(input, "", Parser.xmlParser());
        xml.outputSettings().prettyPrint(false);
        assertEquals("\r\n\rA", xml.expectFirst("pre").wholeText());
        assertEquals("<pre>\r\n\rA</pre>", xml.outerHtml());
        assertEquals("\r\n", new CharacterReader("\r\n").consumeToEnd());
        Document html = Jsoup.parse("<pre>\n\nA</pre>");
        html.outputSettings().prettyPrint(false).syntax(Document.OutputSettings.Syntax.xml);
        assertEquals("<pre>\nA</pre>", html.expectFirst("pre").outerHtml());
    }

    @Test void tracksSourceRanges() {
        for (String newline : new String[]{"\r\n", "\r", "\n", "&#10;", "&NewLine;"}) {
            String source = "<pre>" + newline + "A\r\nB</pre>\r<p x='C\r\nD'>E</p>";
            Document doc = Jsoup.parse(source, Parser.htmlParser().setTrackPosition(true));
            Range text = doc.expectFirst("pre").childNode(0).sourceRange();
            assertEquals(5 + newline.length(), text.startPos());
            assertEquals(source.indexOf("</pre>"), text.endPos());
            Element p = doc.expectFirst("p");
            assertEquals(source.indexOf("<p "), p.sourceRange().startPos());
            Range value = p.attributes().sourceRange("x").valueRange();
            assertEquals("C\r\nD", source.substring(value.startPos(), value.endPos()));
            assertEquals("C\r\nD", p.attr("x"));
        }
    }

    @Test void cdataNullErrors() {
        for (String tag : new String[]{"g", "foreignObject"}) {
            String input = "<!doctype html><svg><" + tag + "><![CDATA[a]\0\n\0]]b]]></" + tag + "></svg>";
            for (int chunk : new int[]{1, 7, CharacterReader.BufferSize}) {
                Parser parser = Parser.htmlParser().setTrackErrors(10);
                Document doc = parser.parseInput(chunked(input, chunk), "");
                assertEquals(tag.equals("g") ? "a]�\n�]]b" : "a]\n]]b", doc.expectFirst(tag).wholeText());
                assertEquals(2, parser.getErrors().size());
                for (int i = 0, pos = input.indexOf('\0'); i < 2; i++, pos = input.indexOf('\0', pos + 1)) {
                    ParseError error = parser.getErrors().get(i);
                    assertEquals(pos, error.getPosition());
                    assertEquals(i == 0 ? "1:" + (pos + 1) : "2:1", error.getCursorPos());
                    assertTrue(error.getErrorMessage().contains("CdataSection"));
                }
            }
        }
    }

    @Test void xmlCdataNulls() {
        Parser parser = Parser.xmlParser().setTrackErrors(10);
        Document doc = parser.parseInput("<r><![CDATA[a]\0]]b]]></r>", "");
        assertEquals("a]\0]]b", doc.expectFirst("r").wholeText());
        assertTrue(parser.getErrors().isEmpty());
    }

    @Test void nullsByContext() {
        for (String input : new String[]{"<html>\0<frameset>", "<html> \0 <frameset>", "<svg>\0 </svg><frameset>"})
            assertNotNull(Jsoup.parse(input).selectFirst("frameset"), input);
        for (String input : new String[]{"<body>\0", "<select>\0"}) {
            Document doc = Jsoup.parse(input);
            Element target = input.startsWith("<select>") ? doc.expectFirst("select") : doc.body();
            assertEquals(0, target.childNodeSize());
        }
        assertEquals("ab", Jsoup.parse("<p>a\0b</p>").expectFirst("p").wholeText());
        assertEquals("�", Jsoup.parse("<p x='&#0;'>&#0;</p>").expectFirst("p").attr("x"));
        assertEquals("�", Jsoup.parse("<p>&#0;</p>").expectFirst("p").wholeText());
        assertEquals(2, Jsoup.parse("<p><b>A</p>\0<div>B</div>").body().childNodeSize());
        assertEquals("�a�", Jsoup.parse("<svg>\0a\0</svg>").expectFirst("svg").wholeText());
        assertEquals("ab", Jsoup.parse("<svg><foreignObject>a\0b</foreignObject></svg>").expectFirst("foreignObject").wholeText());
        assertTrue(Jsoup.parse("<!\0a\0>").outerHtml().contains("<!--�a�-->"));
        assertNull(Jsoup.parse("<svg>�</svg><frameset>").selectFirst("frameset"));
        assertNull(Jsoup.parse("<svg>\0a</svg><frameset>").selectFirst("frameset"));
    }

    private static StringReader chunked(String input, int chunk) {
        return new StringReader(input) {
            @Override public int read(char[] buf, int off, int len) throws IOException {
                return super.read(buf, off, Math.min(chunk, len));
            }
        };
    }
}
