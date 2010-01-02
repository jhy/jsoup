package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 Tests that the selector selects correctly.

 @author Jonathan Hedley, jonathan@hedley.net */
public class SelectorTest {
    @Test public void testByTag() {
        ElementList els = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>").select("div");
        assertEquals(3, els.size());
        assertEquals("1", els.get(0).id());
        assertEquals("2", els.get(1).id());
        assertEquals("3", els.get(2).id());

        ElementList none = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>").select("span");
        assertEquals(0, none.size());
    }

    @Test public void testById() {
        ElementList els = Jsoup.parse("<div><p id=foo>Hello</p><p id=foo>Foo two!</p></div>").select("#foo");
        assertEquals(1, els.size());
        assertEquals("Hello", els.get(0).text());

        ElementList none = Jsoup.parse("<div id=1></div>").select("#foo");
        assertEquals(0, none.size());
    }

    @Test public void testByClass() {
        ElementList els = Jsoup.parse("<p id=0 class='one two'><p id=1 class='one'><p id=2 class='two'>").select(".one");
        assertEquals(2, els.size());
        assertEquals("0", els.get(0).id());
        assertEquals("1", els.get(1).id());

        ElementList none = Jsoup.parse("<div class='one'></div>").select(".foo");
        assertEquals(0, none.size());

        ElementList els2 = Jsoup.parse("<div class='one-two'></div>").select(".one-two");
        assertEquals(1, els2.size());
    }

    @Test public void testByAttribute() {
        String h = "<div title=foo /><div title=bar /><div />";
        Document doc = Jsoup.parse(h);
        ElementList withTitle = doc.select("[title]");
        ElementList foo = doc.select("[title=foo]");

        assertEquals(2, withTitle.size());
        assertEquals(1, foo.size());
    }

    @Test public void testGroupOr() {
        String h = "<div title=foo /><div title=bar /><div /><p></p><img /><span title=qux>";
        ElementList els = Jsoup.parse(h).select("p,div,[title]");

        assertEquals(5, els.size());
        assertEquals("p", els.get(0).tagName());
        assertEquals("div", els.get(1).tagName());
        assertEquals("foo", els.get(1).attr("title"));
        assertEquals("div", els.get(2).tagName());
        assertEquals("bar", els.get(2).attr("title"));
        assertEquals("div", els.get(3).tagName());
        assertNull(els.get(3).attr("title"));
        assertEquals("span", els.get(4).tagName());

    }
}
