package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 Tests for Document.

 @author Jonathan Hedley, jonathan@hedley.net */
public class DocumentTest {
    @Test public void setTextPreservesDocumentStructure() {
        Document doc = Jsoup.parse("<p>Hello</p>");
        doc.text("Replaced");
        assertEquals("Replaced", doc.text());
        assertEquals("Replaced", doc.body().text());
        assertEquals(1, doc.select("head").size());
    }
    
    @Test public void testTitles() {
        Document noTitle = Jsoup.parse("<p>Hello</p>");
        Document withTitle = Jsoup.parse("<title>First</title><title>Ignore</title><p>Hello</p>");
        
        assertEquals("", noTitle.title());
        noTitle.title("Hello");
        assertEquals("Hello", noTitle.title());
        assertEquals("Hello", noTitle.select("title").first().text());
        
        assertEquals("First", withTitle.title());
        withTitle.title("Hello");
        assertEquals("Hello", withTitle.title());
        assertEquals("Hello", withTitle.select("title").first().text());
    }

    @Test public void testOutputEncoding() {
        Document doc = Jsoup.parse("<p title=π>π & < > </p>");
        // default is utf-8
        assertEquals("<p title=\"π\">π &amp; &lt; &gt; </p>", doc.body().html());
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());

        doc.outputSettings().charset("ascii");
        assertEquals(Entities.EscapeMode.base, doc.outputSettings().escapeMode());
        assertEquals("<p title=\"&#960;\">&#960; &amp; &lt; &gt; </p>", doc.body().html());

        doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
        assertEquals("<p title=\"&pi;\">&pi; &amp; &lt; &gt; </p>", doc.body().html());
    }
}
