package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.junit.Test;
import org.junit.Ignore;

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

        Document normaliseTitle = Jsoup.parse("<title>   Hello\nthere   \n   now   \n");
        assertEquals("Hello there now", normaliseTitle.title());
    }

    @Test public void testOutputEncoding() {
        Document doc = Jsoup.parse("<p title=π>π & < > </p>");
        // default is utf-8
        assertEquals("<p title=\"π\">π &amp; &lt; &gt; </p>", doc.body().html());
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());

        doc.outputSettings().charset("ascii");
        assertEquals(Entities.EscapeMode.base, doc.outputSettings().escapeMode());
        assertEquals("<p title=\"&#x3c0;\">&#x3c0; &amp; &lt; &gt; </p>", doc.body().html());

        doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
        assertEquals("<p title=\"&pi;\">&pi; &amp; &lt; &gt; </p>", doc.body().html());
    }

    @Test public void testXhtmlReferences() {
        Document doc = Jsoup.parse("&lt; &gt; &amp; &quot; &apos; &times;");
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        assertEquals("&lt; &gt; &amp; &quot; &apos; ×", doc.body().html());
    }

    @Test public void testNormalisesStructure() {
        Document doc = Jsoup.parse("<html><head><script>one</script><noscript><p>two</p></noscript></head><body><p>three</p></body><p>four</p></html>");
        assertEquals("<html><head><script>one</script><noscript></noscript></head><body><p>two</p><p>three</p><p>four</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void testClone() {
        Document doc = Jsoup.parse("<title>Hello</title> <p>One<p>Two");
        Document clone = doc.clone();

        assertEquals("<html><head><title>Hello</title> </head><body><p>One</p><p>Two</p></body></html>", TextUtil.stripNewlines(clone.html()));
        clone.title("Hello there");
        clone.select("p").first().text("One more").attr("id", "1");
        assertEquals("<html><head><title>Hello there</title> </head><body><p id=\"1\">One more</p><p>Two</p></body></html>", TextUtil.stripNewlines(clone.html()));
        assertEquals("<html><head><title>Hello</title> </head><body><p>One</p><p>Two</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void testClonesDeclarations() {
        Document doc = Jsoup.parse("<!DOCTYPE html><html><head><title>Doctype test");
        Document clone = doc.clone();

        assertEquals(doc.html(), clone.html());
        assertEquals("<!DOCTYPE html><html><head><title>Doctype test</title></head><body></body></html>",
                TextUtil.stripNewlines(clone.html()));
    }

    // Ignored since this test can take awhile to run.
    @Ignore
    @Test public void testOverflowClone() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            builder.insert(0, "<i>");
            builder.append("</i>");
        }

        Document doc = Jsoup.parse(builder.toString());
        doc.clone();
    }
}
