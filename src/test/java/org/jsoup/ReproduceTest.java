package org.jsoup;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReproduceTest {
    @Test
    public void issue1341Test() throws IOException {
        File input = new File("src/test/java/org/jsoup/1341.html");
        Document doc = Jsoup.parse(input, "UTF-8", "");

        Element rv = doc.select("body").get(0).children().get(0);
        assertEquals("test:h1", rv.tagName());
    }
}
