package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 Test TextNodes

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextNodeTest {
    @Test public void testBlank() {
        TextNode one = new TextNode("", "");
        TextNode two = new TextNode("     ", "");
        TextNode three = new TextNode("  \n\n   ", "");
        TextNode four = new TextNode("Hello", "");
        TextNode five = new TextNode("  \nHello ", "");

        assertTrue(one.isBlank());
        assertTrue(two.isBlank());
        assertTrue(three.isBlank());
        assertFalse(four.isBlank());
        assertFalse(five.isBlank());
    }
    
    @Test public void testTextBean() {
        Document doc = Jsoup.parse("<p>One <span>two &amp;</span> three &amp;</p>");
        Element p = doc.select("p").first();

        Element span = doc.select("span").first();
        assertEquals("two &", span.text());
        TextNode spanText = (TextNode) span.childNode(0);
        assertEquals("two &", spanText.text());
        
        TextNode tn = (TextNode) p.childNode(2);
        assertEquals(" three &", tn.text());
        
        tn.text(" POW!");
        assertEquals("One <span>two &amp;</span> POW!", TextUtil.stripNewlines(p.html()));

        tn.attr("text", "kablam &");
        assertEquals("kablam &", tn.text());
        assertEquals("One <span>two &amp;</span>kablam &amp;", TextUtil.stripNewlines(p.html()));
    }
}
