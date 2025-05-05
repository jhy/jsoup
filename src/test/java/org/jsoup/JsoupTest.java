package org.jsoup;

import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsoupTest {
    // Tests for the Jsoup class. Mostly for code coverage for methods that haven't been covered elsewhere already.

    @Test
    void parseWithPath() throws IOException {
        // parse(Path path, @Nullable String charsetName, String baseUri)
        Path path = ParseTest.getPath("/htmltests/medium.html");
        Document doc = Jsoup.parse(path, "UTF-8", "https://example.com/");
        String title = "Medium HTML";
        assertEquals(title, doc.title());

        // parse(Path path)
        doc = Jsoup.parse(path);
        assertEquals(title, doc.title());

        // (Path path, @Nullable String charsetName, String baseUri, Parser parser)
        doc = Jsoup.parse(path, "UTF-8", "https://example.com/", Parser.htmlParser());
        assertEquals(title, doc.title());
    }
}
