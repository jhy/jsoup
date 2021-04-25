package org.jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class headTest {
    String html = "<div someattribute=\"somevalue\"=\"\"></div>";
    String url = "https://www2.deloitte.com/us/en/insights/industry/power-and-utilities/future-of-energy-us-energy-transition.html";

    @Test
    public void testXml() {
        Document xml = Jsoup.parse(html, "", Parser.xmlParser());
        String xmlResult = xml.toString();
        assertEquals(xmlResult, "<div someattribute=\"somevalue\" =\"\"></div>");
    }

    @Test
    public void testHtml() throws IOException {
        //Document doc = Jsoup.connect(url).get();
        Document doc1 = Jsoup.parse(url);
        Document doc2 = Jsoup.connect(url).get();
        assertEquals(doc2.head(), doc1.head());
    }
}

