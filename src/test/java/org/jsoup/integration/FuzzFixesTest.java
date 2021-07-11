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
}
