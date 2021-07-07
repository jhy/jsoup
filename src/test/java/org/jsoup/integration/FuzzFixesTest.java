package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.HtmlParserTest;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 Tests fixes for issues raised by the OSS Fuzz project @ https://oss-fuzz.com/testcases?project=jsoup
 */
public class FuzzFixesTest {

    @Test public void blankAbsAttr() {
        // https://github.com/jhy/jsoup/issues/1541
        String html = "b<bodY abs: abs:abs: abs:abs:abs>";
        Document doc = Jsoup.parse(html);
        assertNotNull(doc);
    }

    @Test public void resetInsertionMode() throws IOException {
        // https://github.com/jhy/jsoup/issues/1538
        File in = ParseTest.getFile("/fuzztests/1538.html"); // lots of escape chars etc.
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);
    }

    @Test public void xmlDeclOverflow() throws IOException {
        // https://github.com/jhy/jsoup/issues/1539
        File in = ParseTest.getFile("/fuzztests/1539.html"); // lots of escape chars etc.
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docXml = Jsoup.parse(new FileInputStream(in), "UTF-8", "https://example.com", Parser.xmlParser());
        assertNotNull(docXml);
    }

    @Test public void unconsume() throws IOException {
        // https://github.com/jhy/jsoup/issues/1542
        File in = ParseTest.getFile("/fuzztests/1542.html"); // lots of escape chars etc.
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);

        Document docFromString = Jsoup.parse(ParseTest.getFileAsString(in));
        assertNotNull(docFromString);

        // I haven't been able to replicate this - per code, content is handed as a plain string, as here. File encoding issue? Tried UTF8, ascii, windows-1252.
    }

    @Test public void stackOverflowState14() throws IOException {
        // https://github.com/jhy/jsoup/issues/1543
        File in = ParseTest.getFile("/fuzztests/1543.html");
        Document doc = Jsoup.parse(in, "UTF-8");
        assertNotNull(doc);
    }
}
