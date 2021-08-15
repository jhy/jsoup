package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 Tests fixes for issues raised by the OSS Fuzz project @ https://oss-fuzz.com/testcases?project=jsoup. Contains inline
 string cases causing exceptions. Timeout tests are in FuzzFixesIT.
 */
public class FuzzFixesTest {

    private static Stream<File> testFiles() {
        File[] files = FuzzFixesIT.testDir.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 10);

        return Stream.of(files);
    }

    @Test
    public void blankAbsAttr() {
        // https://github.com/jhy/jsoup/issues/1541
        String html = "b<bodY abs: abs:abs: abs:abs:abs>";
        Document doc = Jsoup.parse(html);
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

    @ParameterizedTest
    @MethodSource("testFiles")
    void testHtmlParse(File file) throws IOException {
        Document doc = Jsoup.parse(file, "UTF-8", "https://example.com/");
        assertNotNull(doc);
    }

    @ParameterizedTest
    @MethodSource("testFiles")
    void testXmlParse(File file) throws IOException {
        Document doc = Jsoup.parse(file, "UTF-8", "https://example.com/", Parser.xmlParser());
        assertNotNull(doc);
    }
}
