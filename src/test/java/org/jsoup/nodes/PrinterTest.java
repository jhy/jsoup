package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.jsoup.integration.ParseTest.getFile;
import static org.jsoup.integration.ParseTest.getFileAsString;
import static org.junit.jupiter.api.Assertions.*;

/**
 Test for the HTML / XML serialization of elements, including the Pretty Printer, Outline mode, and the base.
 */
public class PrinterTest {
    @Test void pretty() throws IOException {
        // parse /printertests/input-1.html, check formatted same as pretty-1.html
        File in = getFile("/printertests/input-1.html");
        Document doc = Jsoup.parse(in);

        String expected = getFileAsString(getFile("/printertests/pretty-1.html"));
        String html = doc.html();
        assertEquals(expected, html);
        assertEquals(html, doc.outerHtml());
    }

    @Test void passthru() throws IOException {
        // disable pretty, should be almost 1:1 of input (other than a couple parse normalizations; doctype, pre)
        File in = getFile("/printertests/input-1.html");
        Document doc = Jsoup.parse(in);
        doc.outputSettings().prettyPrint(false);

        String expected = getFileAsString(getFile("/printertests/passthru-1.html"));
        String html = doc.html();
        assertEquals(expected, html);
        assertEquals(html, doc.outerHtml());
    }

    @Test void outline() throws IOException {
        // outline mode, most everything gets indented
        File in = getFile("/printertests/input-1.html");
        Document doc = Jsoup.parse(in);
        doc.outputSettings().outline(true);

        String expected = getFileAsString(getFile("/printertests/outline-1.html"));
        String html = doc.html();
        assertEquals(expected, html);
        assertEquals(html, doc.outerHtml());
    }

    @Test void sequentialTextNodesDontCollapse() {
        // tests that the pretty printer does not collapse (trim leading | trailing whitespace) when there are
        // sequential textnodes. That doesn't happen in a parse, but can when manipulated.
        Document doc = Jsoup.parse("<div><div></div>Hello</div>"); // needs to be text that would indent
        Element div = doc.expectFirst("div");
        TextNode hello = (TextNode) div.childNode(1);
        hello.after(" there.");
        assertEquals("Hello", hello.getWholeText());
        assertEquals(" there.", ((TextNode) hello.nextSibling()).getWholeText());

        assertEquals("Hello there.", div.text());
        assertEquals("<div></div>\nHello there.", div.html());
        assertEquals("<div>\n <div></div>\n Hello there.\n</div>", div.outerHtml());
    }

    @Test void dontCollapseTextAfterNonElements() {
        Document doc = Jsoup.parse("<div><div></div>Hello <!-- -_- --> there</div>");
        Element body = doc.body();
        assertEquals("Hello there", body.text());
        assertEquals("<div>\n <div></div>\n Hello <!-- -_- -->\n  there\n</div>", body.html());
    }

    @Test void spaceAfterSpanInBlock() {
        Document doc = Jsoup.parse("<div> <span>Span</span> \n Text  <span>Follow</span></div> <p> <span>Span</span>  Text <span>Follow</span> </p>");
        Element body = doc.body();
        assertEquals("Span Text Follow Span Text Follow", body.text());
        assertEquals("<div>\n <span>Span</span> Text <span>Follow</span>\n</div>\n<p><span>Span</span> Text <span>Follow</span></p>", body.html());
    }

}
