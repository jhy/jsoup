package org.jsoup.nodes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.integration.ParseTest;
import org.junit.Test;
import org.junit.Ignore;

import static org.jsoup.nodes.Document.OutputSettings.Syntax;
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
        assertEquals("&lt; &gt; &amp; \" ' ×", doc.body().html());
    }

    @Test public void testNormalisesStructure() {
        Document doc = Jsoup.parse("<html><head><script>one</script><noscript><p>two</p></noscript></head><body><p>three</p></body><p>four</p></html>");
        assertEquals("<html><head><script>one</script><noscript>&lt;p&gt;two</noscript></head><body><p>three</p><p>four</p></body></html>", TextUtil.stripNewlines(doc.html()));
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
        assertEquals("<!doctype html><html><head><title>Doctype test</title></head><body></body></html>",
                TextUtil.stripNewlines(clone.html()));
    }
    
    @Test public void testLocation() throws IOException {
    	File in = new ParseTest().getFile("/htmltests/yahoo-jp.html");
        Document doc = Jsoup.parse(in, "UTF-8", "http://www.yahoo.co.jp/index.html");
        String location = doc.location();
        String baseUri = doc.baseUri();
        assertEquals("http://www.yahoo.co.jp/index.html",location);
        assertEquals("http://www.yahoo.co.jp/_ylh=X3oDMTB0NWxnaGxsBF9TAzIwNzcyOTYyNjUEdGlkAzEyBHRtcGwDZ2Ex/",baseUri);
        in = new ParseTest().getFile("/htmltests/nyt-article-1.html");
        doc = Jsoup.parse(in, null, "http://www.nytimes.com/2010/07/26/business/global/26bp.html?hp");
        location = doc.location();
        baseUri = doc.baseUri();
        assertEquals("http://www.nytimes.com/2010/07/26/business/global/26bp.html?hp",location);
        assertEquals("http://www.nytimes.com/2010/07/26/business/global/26bp.html?hp",baseUri);
    }

    @Test public void testHtmlAndXmlSyntax() {
        String h = "<!DOCTYPE html><body><img async checked='checked' src='&<>\"'>&lt;&gt;&amp;&quot;<foo />bar";
        Document doc = Jsoup.parse(h);

        doc.outputSettings().syntax(Syntax.html);
        assertEquals("<!doctype html>\n" +
                "<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <img async checked src=\"&amp;<>&quot;\">&lt;&gt;&amp;\"\n" +
                "  <foo />bar\n" +
                " </body>\n" +
                "</html>", doc.html());

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        assertEquals("<!DOCTYPE html>\n" +
                "<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <img async=\"\" checked=\"checked\" src=\"&amp;<>&quot;\" />&lt;&gt;&amp;\"\n" +
                "  <foo />bar\n" +
                " </body>\n" +
                "</html>", doc.html());
    }

    @Test public void htmlParseDefaultsToHtmlOutputSyntax() {
        Document doc = Jsoup.parse("x");
        assertEquals(Syntax.html, doc.outputSettings().syntax());
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
    
    @Test
    public void testMetaCharsetUpdate() {
        // Existing meta charset tag
        final Document doc = Document.createShell("");
        doc.updateMetaCharset(true);
        doc.head().appendElement("meta").attr("charset", "changeThis");        
        
        final String charsetUtf8 = "UTF-8";
        doc.charset(Charset.forName(charsetUtf8));
        Element selectedElement = doc.select("meta[charset]").first();
        
        final String htmlCharsetUTF8 = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetUtf8 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        
        assertNotNull(selectedElement);
        assertEquals(charsetUtf8, doc.charset().displayName());
        assertEquals(charsetUtf8, selectedElement.attr("charset"));
        assertEquals(htmlCharsetUTF8, doc.toString());
        assertEquals(doc.charset(), doc.outputSettings().charset());
        
        final String charsetIso8859 = "ISO-8859-1";
        doc.charset(Charset.forName(charsetIso8859));
        selectedElement = doc.select("meta[charset]").first();
        
        final String htmlCharsetISO = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetIso8859 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        
        assertNotNull(selectedElement);
        assertEquals(charsetIso8859, doc.charset().displayName());
        assertEquals(charsetIso8859, selectedElement.attr("charset"));
        assertEquals(htmlCharsetISO, doc.toString());
        assertEquals(doc.charset(), doc.outputSettings().charset());
        
        
        // No meta charset tag
        final Document docNoCharset = Document.createShell("");
        docNoCharset.updateMetaCharset(true);
        docNoCharset.charset(Charset.forName(charsetUtf8));
        
        assertEquals(charsetUtf8, docNoCharset.select("meta[charset]").first().attr("charset"));
        assertEquals(htmlCharsetUTF8, docNoCharset.toString());
        
        
        // Disabled update of meta charset tag
        final Document docDisabled = Document.createShell("");
        assertFalse(docDisabled.updateMetaCharset());
        
        final String htmlNoCharset = "<html>\n" +
                                        " <head></head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        
        assertEquals(htmlNoCharset, docDisabled.toString());
        assertNull(docDisabled.select("meta[charset]").first());
        
        final String htmlCharset = "<html>\n" +
                                    " <head>\n" +
                                    "  <meta charset=\"dontTouch\">\n" +
                                    "  <meta name=\"charset\" content=\"dontTouch\">\n" +
                                    " </head>\n" +
                                    " <body></body>\n" +
                                    "</html>";
        
        docDisabled.head().appendElement("meta").attr("charset", "dontTouch");
        docDisabled.head().appendElement("meta").attr("name", "charset").attr("content", "dontTouch");
        
        assertEquals(htmlCharset, docDisabled.toString());
        
        selectedElement = docDisabled.select("meta[charset]").first();
        assertNotNull(selectedElement);
        assertEquals("dontTouch", selectedElement.attr("charset"));
        selectedElement = docDisabled.select("meta[name=charset]").first();
        assertNotNull(selectedElement);
        assertEquals("dontTouch", selectedElement.attr("content"));
        
        docDisabled.charset(Charset.forName(charsetUtf8));
        selectedElement = docDisabled.select("meta[charset]").first();
        assertNotNull(selectedElement);
        assertEquals("dontTouch", selectedElement.attr("charset"));
        selectedElement = docDisabled.select("meta[name=charset]").first();
        assertNotNull(selectedElement);
        assertEquals("dontTouch", selectedElement.attr("content"));
        
        
        // Remove obsolete charset definitions
        final Document docCleanup = Document.createShell("");
        docCleanup.updateMetaCharset(true);
        docCleanup.head().appendElement("meta").attr("charset", "dontTouch");
        docCleanup.head().appendElement("meta").attr("name", "charset").attr("content", "dontTouch");
        docCleanup.charset(Charset.forName(charsetUtf8));
        
        assertEquals(htmlCharsetUTF8, docCleanup.toString());
    }
    
    @Test
    public void testMetaCharsetUpdateXml() {
        // Existing encoding definition
        final Document doc = new Document("");
        doc.appendElement("root").text("node");
        doc.outputSettings().syntax(Syntax.xml);
        doc.updateMetaCharset(true);
        
        XmlDeclaration decl = new XmlDeclaration("xml", "", false);
        decl.attr("version", "1.0");
        decl.attr("encoding", "changeThis");
        doc.prependChild(decl);
        
        final String charsetUtf8 = "UTF-8";
        doc.charset(Charset.forName(charsetUtf8));
        
        Node declNode = doc.childNode(0);
        assertTrue(declNode instanceof XmlDeclaration);
        XmlDeclaration selectedNode = (XmlDeclaration) declNode;
        
        final String xmlCharsetUTF8 = "<?xml version=\"1.0\" encoding=\"" + charsetUtf8 + "\">\n" +
                                        "<root>\n" +
                                        " node\n" +
                                        "</root>";

        assertNotNull(declNode);
        assertEquals(charsetUtf8, doc.charset().displayName());
        assertEquals(charsetUtf8, selectedNode.attr("encoding"));
        assertEquals("1.0", selectedNode.attr("version"));
        assertEquals(xmlCharsetUTF8, doc.toString());
        assertEquals(doc.charset(), doc.outputSettings().charset());
        
        final String charsetIso8859 = "ISO-8859-1";
        doc.charset(Charset.forName(charsetIso8859));
        
        declNode = doc.childNode(0);
        assertTrue(declNode instanceof XmlDeclaration);
        selectedNode = (XmlDeclaration) declNode;
        
        final String xmlCharsetISO = "<?xml version=\"1.0\" encoding=\"" + charsetIso8859 + "\">\n" +
                                        "<root>\n" +
                                        " node\n" +
                                        "</root>";
        
        assertNotNull(declNode);
        assertEquals(charsetIso8859, doc.charset().displayName());
        assertEquals(charsetIso8859, selectedNode.attr("encoding"));
        assertEquals("1.0", selectedNode.attr("version"));
        assertEquals(xmlCharsetISO, doc.toString());
        assertEquals(doc.charset(), doc.outputSettings().charset());
        
        
        // No encoding definition
        final Document docNoCharset = new Document("");
        docNoCharset.appendElement("root").text("node");
        docNoCharset.outputSettings().syntax(Syntax.xml);
        docNoCharset.updateMetaCharset(true);
        docNoCharset.charset(Charset.forName(charsetUtf8));
        
        declNode = docNoCharset.childNode(0);
        assertTrue(declNode instanceof XmlDeclaration);
        selectedNode = (XmlDeclaration) declNode;
        
        assertEquals(charsetUtf8, selectedNode.attr("encoding"));
        assertEquals(xmlCharsetUTF8, docNoCharset.toString());
        
        
        // Disabled update of encoding definition
        final Document docDisabled = new Document("");
        docDisabled.appendElement("root").text("node");
        docDisabled.outputSettings().syntax(Syntax.xml);
        assertFalse(docDisabled.updateMetaCharset());
        
        final String xmlNoCharset = "<root>\n" +
                                    " node\n" +
                                    "</root>";
        
        assertEquals(xmlNoCharset, docDisabled.toString());
        
        decl = new XmlDeclaration("xml", "", false);
        decl.attr("version", "dontTouch");
        decl.attr("encoding", "dontTouch");
        docDisabled.prependChild(decl);
        
        final String xmlCharset = "<?xml version=\"dontTouch\" encoding=\"dontTouch\">\n" +
                                    "<root>\n" +
                                    " node\n" +
                                    "</root>";
        
        assertEquals(xmlCharset, docDisabled.toString());
        
        declNode = docDisabled.childNode(0);
        assertTrue(declNode instanceof XmlDeclaration);
        selectedNode = (XmlDeclaration) declNode;
        
        assertEquals("dontTouch", selectedNode.attr("encoding"));
        assertEquals("dontTouch", selectedNode.attr("version"));
    }
}
