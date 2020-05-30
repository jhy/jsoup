package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * This is the test for the feature of > pseudo selector.
 */
public class issue1035Test {
    private final String HTML = "<div><span>span body</span></div>" +
            "<div><p>p</p></div>" +
            "<div><a>s</a></div>";

    @Test
    public void singleLimitionTest() {
        Document doc = Jsoup.parse(HTML);
        Elements hasSpan = doc.select("div:has(>span)");
        assertEquals(hasSpan.get(0).text(), "span body");
    }

    @Test
    public void multiLimitionTest() {
        Document doc = Jsoup.parse(HTML);
        Elements hasSpan = doc.select("div:has(>:not(a, p))");
        assertEquals(hasSpan.get(0).text(), "span body");
    }

}
