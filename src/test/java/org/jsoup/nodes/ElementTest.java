package org.jsoup.nodes;

import org.jsoup.JSoup;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;

/**
 * Tests for Element (DOM stuff mostly).
 *
 * @author Jonathan Hedley
 */
public class ElementTest {
    @Test
    public void getElementsByTagName() {
        Document doc = JSoup.parse("<div id=div1><p>Hello</p><p>Another</p><div id=div2><img src=foo.png></div></div>");
        List<Element> divs = doc.getElementsByTag("div");
        assertEquals(2, divs.size());
        assertEquals("div1", divs.get(0).id());
        assertEquals("div2", divs.get(1).id());

        List<Element> ps = doc.getElementsByTag("p");
        assertEquals(2, ps.size());
        assertEquals("Hello", ((TextNode) ps.get(0).childNode(0)).getWholeText());
        assertEquals("Another", ((TextNode) ps.get(1).childNode(0)).getWholeText());

        List<Element> imgs = doc.getElementsByTag("img");
        assertEquals("foo.png", imgs.get(0).attr("src"));
    }

}
