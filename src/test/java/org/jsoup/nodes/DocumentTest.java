package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.select.Elements;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 Tests for Document.

 @author Jonathan Hedley, jonathan@hedley.net */
public class DocumentTest {
    private static final String charsetUtf8 = "UTF-8";
    private static final String charsetIso8859 = "ISO-8859-1";
    
    
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
        assertEquals("UTF-8", doc.outputSettings().charset().name());

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
    
    @Test public void testHtmlAppendable() {
    	String htmlContent = "<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>";
    	Document document = Jsoup.parse(htmlContent);
    	OutputSettings outputSettings = new OutputSettings();
    	
    	outputSettings.prettyPrint(false);
    	document.outputSettings(outputSettings);
    	assertEquals(htmlContent, document.html(new StringWriter()).toString());
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

    @Test public void DocumentsWithSameContentAreEqual() throws Exception {
        Document docA = Jsoup.parse("<div/>One");
        Document docB = Jsoup.parse("<div/>One");
        Document docC = Jsoup.parse("<div/>Two");

        assertFalse(docA.equals(docB));
        assertTrue(docA.equals(docA));
        assertEquals(docA.hashCode(), docA.hashCode());
        assertFalse(docA.hashCode() == docC.hashCode());
    }

    @Test public void DocumentsWithSameContentAreVerifialbe() throws Exception {
        Document docA = Jsoup.parse("<div/>One");
        Document docB = Jsoup.parse("<div/>One");
        Document docC = Jsoup.parse("<div/>Two");

        assertTrue(docA.hasSameValue(docB));
        assertFalse(docA.hasSameValue(docC));
    }
    
    @Test
    public void testMetaCharsetUpdateUtf8() {
        final Document doc = createHtmlDocument("changeThis");
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName(charsetUtf8));
        
        final String htmlCharsetUTF8 = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetUtf8 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        assertEquals(htmlCharsetUTF8, doc.toString());
        
        Element selectedElement = doc.select("meta[charset]").first();
        assertEquals(charsetUtf8, doc.charset().name());
        assertEquals(charsetUtf8, selectedElement.attr("charset"));
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }
    
    @Test
    public void testMetaCharsetUpdateIso8859() {
        final Document doc = createHtmlDocument("changeThis");
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName(charsetIso8859));
        
        final String htmlCharsetISO = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetIso8859 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        assertEquals(htmlCharsetISO, doc.toString());
        
        Element selectedElement = doc.select("meta[charset]").first();
        assertEquals(charsetIso8859, doc.charset().name());
        assertEquals(charsetIso8859, selectedElement.attr("charset"));
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }
    
    @Test
    public void testMetaCharsetUpdateNoCharset() {
        final Document docNoCharset = Document.createShell("");
        docNoCharset.updateMetaCharsetElement(true);
        docNoCharset.charset(Charset.forName(charsetUtf8));
        
        assertEquals(charsetUtf8, docNoCharset.select("meta[charset]").first().attr("charset"));
        
        final String htmlCharsetUTF8 = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetUtf8 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        assertEquals(htmlCharsetUTF8, docNoCharset.toString()); 
    }
    
    @Test
    public void testMetaCharsetUpdateDisabled() {
        final Document docDisabled = Document.createShell("");
        
        final String htmlNoCharset = "<html>\n" +
                                        " <head></head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        assertEquals(htmlNoCharset, docDisabled.toString());
        assertNull(docDisabled.select("meta[charset]").first());
    }
    
    @Test
    public void testMetaCharsetUpdateDisabledNoChanges() {
        final Document doc = createHtmlDocument("dontTouch");
        
        final String htmlCharset = "<html>\n" +
                                    " <head>\n" +
                                    "  <meta charset=\"dontTouch\">\n" +
                                    "  <meta name=\"charset\" content=\"dontTouch\">\n" +
                                    " </head>\n" +
                                    " <body></body>\n" +
                                    "</html>";
        assertEquals(htmlCharset, doc.toString());
        
        Element selectedElement = doc.select("meta[charset]").first();
        assertNotNull(selectedElement);
        assertEquals("dontTouch", selectedElement.attr("charset"));
        
        selectedElement = doc.select("meta[name=charset]").first();
        assertNotNull(selectedElement);
        assertEquals("dontTouch", selectedElement.attr("content"));
    }
    
    @Test
    public void testMetaCharsetUpdateEnabledAfterCharsetChange() {
        final Document doc = createHtmlDocument("dontTouch");
        doc.charset(Charset.forName(charsetUtf8));
        
        Element selectedElement = doc.select("meta[charset]").first();
        assertEquals(charsetUtf8, selectedElement.attr("charset"));
        assertTrue(doc.select("meta[name=charset]").isEmpty());
    }
            
    @Test
    public void testMetaCharsetUpdateCleanup() {
        final Document doc = createHtmlDocument("dontTouch");
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName(charsetUtf8));
        
        final String htmlCharsetUTF8 = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetUtf8 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        
        assertEquals(htmlCharsetUTF8, doc.toString());
    }
    
    @Test
    public void testMetaCharsetUpdateXmlUtf8() {
        final Document doc = createXmlDocument("1.0", "changeThis", true);
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName(charsetUtf8));
        
        final String xmlCharsetUTF8 = "<?xml version=\"1.0\" encoding=\"" + charsetUtf8 + "\"?>\n" +
                                        "<root>\n" +
                                        " node\n" +
                                        "</root>";
        assertEquals(xmlCharsetUTF8, doc.toString());

        XmlDeclaration selectedNode = (XmlDeclaration) doc.childNode(0);
        assertEquals(charsetUtf8, doc.charset().name());
        assertEquals(charsetUtf8, selectedNode.attr("encoding"));
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }
    
    @Test
    public void testMetaCharsetUpdateXmlIso8859() {
        final Document doc = createXmlDocument("1.0", "changeThis", true);
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName(charsetIso8859));
        
        final String xmlCharsetISO = "<?xml version=\"1.0\" encoding=\"" + charsetIso8859 + "\"?>\n" +
                                        "<root>\n" +
                                        " node\n" +
                                        "</root>";
        assertEquals(xmlCharsetISO, doc.toString());
        
        XmlDeclaration selectedNode = (XmlDeclaration) doc.childNode(0);
        assertEquals(charsetIso8859, doc.charset().name());
        assertEquals(charsetIso8859, selectedNode.attr("encoding"));
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }
    
    @Test
    public void testMetaCharsetUpdateXmlNoCharset() {
        final Document doc = createXmlDocument("1.0", "none", false);
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName(charsetUtf8));
        
        final String xmlCharsetUTF8 = "<?xml version=\"1.0\" encoding=\"" + charsetUtf8 + "\"?>\n" +
                                        "<root>\n" +
                                        " node\n" +
                                        "</root>";
        assertEquals(xmlCharsetUTF8, doc.toString());
        
        XmlDeclaration selectedNode = (XmlDeclaration) doc.childNode(0);
        assertEquals(charsetUtf8, selectedNode.attr("encoding"));
    }
    
    @Test
    public void testMetaCharsetUpdateXmlDisabled() {
        final Document doc = createXmlDocument("none", "none", false);
        
        final String xmlNoCharset = "<root>\n" +
                                    " node\n" +
                                    "</root>";
        assertEquals(xmlNoCharset, doc.toString());
    }

    @Test
    public void testMetaCharsetUpdateXmlDisabledNoChanges() {
        final Document doc = createXmlDocument("dontTouch", "dontTouch", true);
        
        final String xmlCharset = "<?xml version=\"dontTouch\" encoding=\"dontTouch\"?>\n" +
                                    "<root>\n" +
                                    " node\n" +
                                    "</root>";
        assertEquals(xmlCharset, doc.toString());
        
        XmlDeclaration selectedNode = (XmlDeclaration) doc.childNode(0);
        assertEquals("dontTouch", selectedNode.attr("encoding"));
        assertEquals("dontTouch", selectedNode.attr("version"));
    }
    
    @Test
    public void testMetaCharsetUpdatedDisabledPerDefault() {
        final Document doc = createHtmlDocument("none");
        assertFalse(doc.updateMetaCharsetElement());
    }
    
    private Document createHtmlDocument(String charset) {
        final Document doc = Document.createShell("");
        doc.head().appendElement("meta").attr("charset", charset);
        doc.head().appendElement("meta").attr("name", "charset").attr("content", charset);
        
        return doc;
    }
    
    private Document createXmlDocument(String version, String charset, boolean addDecl) {
        final Document doc = new Document("");
        doc.appendElement("root").text("node");
        doc.outputSettings().syntax(Syntax.xml);
        
        if( addDecl == true ) {
            XmlDeclaration decl = new XmlDeclaration("xml", false);
            decl.attr("version", version);
            decl.attr("encoding", charset);
            doc.prependChild(decl);
        }
        
        return doc;
    }

    @Test
    public void testShiftJisRoundtrip() throws Exception {
        String input =
                "<html>"
                        +   "<head>"
                        +     "<meta http-equiv=\"content-type\" content=\"text/html; charset=Shift_JIS\" />"
                        +   "</head>"
                        +   "<body>"
                        +     "before&nbsp;after"
                        +   "</body>"
                        + "</html>";
        InputStream is = new ByteArrayInputStream(input.getBytes(Charset.forName("ASCII")));

        Document doc = Jsoup.parse(is, null, "http://example.com");
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

        String output = new String(doc.html().getBytes(doc.outputSettings().charset()), doc.outputSettings().charset());

        assertFalse("Should not have contained a '?'.", output.contains("?"));
        assertTrue("Should have contained a '&#xa0;' or a '&nbsp;'.",
                output.contains("&#xa0;") || output.contains("&nbsp;"));
    }

    @Test public void parseAndHtmlOnDifferentThreads() throws InterruptedException {
        String html = "<p>Alrighty then it's not \uD83D\uDCA9. <span>Next</span></p>"; // 💩
        String asci = "<p>Alrighty then it's not &#x1f4a9;. <span>Next</span></p>";

        final Document doc = Jsoup.parse(html);
        final String[] out = new String[1];
        final Elements p = doc.select("p");
        assertEquals(html, p.outerHtml());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                out[0] = p.outerHtml();
                doc.outputSettings().charset(StandardCharsets.US_ASCII);
            }
        });
        thread.start();
        thread.join();

        assertEquals(html, out[0]);
        assertEquals(StandardCharsets.US_ASCII, doc.outputSettings().charset());
        assertEquals(asci, p.outerHtml());
    }
}
