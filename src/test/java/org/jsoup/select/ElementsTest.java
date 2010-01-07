package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 Tests for ElementList.

 @author Jonathan Hedley, jonathan@hedley.net */
public class ElementsTest {
    @Test public void chainedSelects() {
        String h = "<p>Excl</p><div class=headline><p>Hello</p><p>There</p></div><div class=headline><h1>Headline</h1></div>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select(".headline").select("p");
        assertEquals(2, els.size());
        assertEquals("Hello", els.get(0).text());
        assertEquals("There", els.get(1).text());
    }

    @Test public void attributes() {
        String h = "<p title=foo><p title=bar><p class=foo><p class=bar>";
        Document doc = Jsoup.parse(h);
        Elements withTitle = doc.select("p[title]");
        assertEquals(2, withTitle.size());
        assertTrue(withTitle.hasAttr("title"));
        assertFalse(withTitle.hasAttr("class"));
        assertEquals("foo", withTitle.attr("title"));

        withTitle.removeAttr("title");
        assertEquals(2, withTitle.size()); // existing Elements are not reevaluated
        assertEquals(0, doc.select("p[title]").size());

        Elements ps = doc.select("p").attr("style", "classy");
        assertEquals(4, ps.size());
        assertEquals("classy", ps.last().attr("style"));
        assertEquals("bar", ps.last().attr("class"));
    }
}
