package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FragmentParseTest {
    @Test
    public void parseBodyFragment() {
        Document doc = Jsoup.parseBodyFragment("<div><p>Lorem</p><p>ipsum");
        assertEquals("<div>\n <p>Lorem</p>\n <p>ipsum</p>\n</div>", doc.body().html());
    }

    @Test
    public void issue764Senario1() {
        Document doc = Jsoup.parseBodyFragment("<mg per week or )");
        assertEquals("&lt;mg per week or )", doc.body().html());
    }

    @Test
    public void issue764Senario2() {
        Document doc = Jsoup.parseBodyFragment("<p><mg per week or )</p>");
        assertEquals("<p>&lt;mg per week or )</p>", doc.body().html());
    }
}
