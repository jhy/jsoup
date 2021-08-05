package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 Tests fixes for issues raised by the OSS Fuzz project @ https://oss-fuzz.com/testcases?project=jsoup
 */
public class FuzzFixesTest {

    @Test
    public void blankAbsAttr() {
        // https://github.com/jhy/jsoup/issues/1541
        String html = "b<bodY abs: abs:abs: abs:abs:abs>";
        Document doc = Jsoup.parse(html);
        assertNotNull(doc);
    }

    @Test
    public void resetInsertionMode() throws IOException {
        // https://github.com/jhy/jsoup/issues/1538
        File in = ParseTest.getFile("/fuzztests/1538.html.gz"); // lots of escape chars etc.
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);
    }

    @Test
    public void xmlDeclOverflow() throws IOException {
        // https://github.com/jhy/jsoup/issues/1539
        File in = ParseTest.getFile("/fuzztests/1539.html.gz"); // lots of escape chars etc.
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void xmlDeclOverflowOOM() throws IOException {
        // https://github.com/jhy/jsoup/issues/1569
        File in = ParseTest.getFile("/fuzztests/1569.html.gz");
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void stackOverflowState14() throws IOException {
        // https://github.com/jhy/jsoup/issues/1543
        File in = ParseTest.getFile("/fuzztests/1543.html.gz");
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);
    }

    @Test
    public void parseTimeout() throws IOException {
        // https://github.com/jhy/jsoup/issues/1544
        File in = ParseTest.getFile("/fuzztests/1544.html.gz");
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);
    }

    @Test
    public void parseTimeout1580() throws IOException {
        // https://github.com/jhy/jsoup/issues/1580
        // a shedload of NULLs in append tagname so was spinning in there. Fixed to eat and replace all the chars in one hit
        File in = ParseTest.getFile("/fuzztests/1580.html.gz");
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);
    }

    @Test
    public void bookmark() {
        // https://github.com/jhy/jsoup/issues/1576
        String html = "<?a<U<P<A ";
        Document doc = Jsoup.parse(html);
        assertNotNull(doc);

        Document xmlDoc = Parser.xmlParser().parseInput(html, "");
        assertNotNull(xmlDoc);
    }

    @Test
    public void scope1579() {
        // https://github.com/jhy/jsoup/issues/1579
        String html = "<table<html\u001D<ÛÛ<tr><body\u001D<b:<select<m<input></html> </html>";
        Document doc = Jsoup.parse(html);
        assertNotNull(doc);

        Document xmlDoc = Parser.xmlParser().parseInput(html, "");
        assertNotNull(xmlDoc);
    }

    @Test
    public void overflow1577() throws IOException {
        // https://github.com/jhy/jsoup/issues/1577
        File in = ParseTest.getFile("/fuzztests/1577.html.gz");
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void parseTimeout36150() throws IOException {
        File in = ParseTest.getFile("/fuzztests/1580-attrname.html.gz");
        // pretty much 1MB of null chars in text head
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void parseTimeout1593() throws IOException {
        // https://github.com/jhy/jsoup/issues/1593
        // had unbounded depth in the foster formatting element scan - now limited to <= 256
        // realworld HTML generally has only a few
        File in = ParseTest.getFile("/fuzztests/1593.html.gz");

        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void parseTimeout1595() throws IOException {
        // https://github.com/jhy/jsoup/issues/1595
        // Time was getting soaked when setting a form attribute by searching up the node.root for ownerdocuments
        File in = ParseTest.getFile("/fuzztests/1595.html.gz");

        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void parseTimeout1596() throws IOException {
        // https://github.com/jhy/jsoup/issues/1596
        // Timesink when the stack was thousands of items deep, and non-matching close tags sent
        File in = ParseTest.getFile("/fuzztests/1596.html.gz");

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void parseTimeout1605() throws IOException {
        // timesink with 600K of accumulating attribute name
        File in = ParseTest.getFile("/fuzztests/1605.html.gz");

        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void parseTimeout1606() throws IOException {
        // https://github.com/jhy/jsoup/issues/1606
        // Timesink when closing missing empty tag (in XML comment processed as HTML) when thousands deep
        File in = ParseTest.getFile("/fuzztests/1606.html.gz");

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test
    public void overflow1607() throws IOException {
        // https://github.com/jhy/jsoup/issues/1607
        File in = ParseTest.getFile("/fuzztests/1607.html.gz");

        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }
}
