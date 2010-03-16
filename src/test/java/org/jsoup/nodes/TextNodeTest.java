package org.jsoup.nodes;

import org.jsoup.Jsoup;
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
        Document doc = Jsoup.parse("<p>One <span>two</span> three</p>");
        Element p = doc.select("p").first();
        
        TextNode tn = (TextNode) p.childNode(2);
        assertEquals(" three", tn.text());
        
        tn.text(" POW!");
        assertEquals("One <span>two</span> POW!", p.html());
    }
}
