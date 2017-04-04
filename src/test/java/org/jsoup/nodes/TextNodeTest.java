package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 Test TextNodes

 @author Jonathan Hedley, jonathan@hedley.net 
 @Contributor : Heekyo Lee (leeheekyo)
 */
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

    @Test public void testSplitText() {
        Document doc = Jsoup.parse("<div>Hello there</div>");
        Element div = doc.select("div").first();
        TextNode tn = (TextNode) div.childNode(0);
        TextNode tail = tn.splitText(6);
        assertEquals("Hello ", tn.getWholeText());
        assertEquals("there", tail.getWholeText());
        tail.text("there!");
        assertEquals("Hello there!", div.text());
        assertTrue(tn.parent() == tail.parent());
    }

    @Test public void testSplitAnEmbolden() {
        Document doc = Jsoup.parse("<div>Hello there</div>");
        Element div = doc.select("div").first();
        TextNode tn = (TextNode) div.childNode(0);
        TextNode tail = tn.splitText(6);
        tail.wrap("<b></b>");

        assertEquals("Hello <b>there</b>", TextUtil.stripNewlines(div.html())); // not great that we get \n<b>there there... must correct
    }

    @Test public void testWithSupplementaryCharacter(){
        Document doc = Jsoup.parse(new String(Character.toChars(135361)));
        TextNode t = doc.body().textNodes().get(0);
        assertEquals(new String(Character.toChars(135361)), t.outerHtml().trim());
    }
    
    @Test public void testCreateFromEncoded(){
        Document doc = Jsoup.parse("<div>one</div>");
        Element div = doc.select("div").first();
        TextNode textnode = (TextNode) div.childNode(0);
        
        assertEquals("base64",textnode.createFromEncoded("base64","http://naver.com").toString());
    }
    
    @Test public void TestStripLeadingWhitespace(){
    	Document doc = Jsoup.parse("<div>one</div>");
    	Element div = doc.select("div").first();
    	TextNode textnode = (TextNode) div.childNode(0);
    	
    	assertEquals("text", textnode.stripLeadingWhitespace("text"));
    }
    
    @Test public void TestAttr(){
    	Document doc = Jsoup.parse("<div name=val>one</div>");
    	Element div = doc.select("div").first();
    	TextNode textnode = (TextNode) div.childNode(0);
    	
    	assertNotNull(textnode.attr("val"));
    }
    
    @Test public void TestAttributes(){
    	Document doc = Jsoup.parse("<div name=val>one</div>");
    	Element div = doc.select("div").first();
    	TextNode textnode = (TextNode) div.childNode(0);
    	
    	assertNotNull(textnode.attributes());
    }
    
    @Test public void TestHasAttr(){
    	Document doc = Jsoup.parse("<div name=val>one</div>");
    	Element div = doc.select("div").first();
    	TextNode textnode = (TextNode) div.childNode(0);
    	
    	assertTrue(textnode.hasAttr("text"));
    }
    
    @Test public void TestRemoveAttr(){
    	Document doc = Jsoup.parse("<div name=val>one</div>");
    	Element div = doc.select("div").first();
    	TextNode textnode = (TextNode) div.childNode(0);
    	
    	textnode.removeAttr("text");
    	assertNotNull(textnode.attr("text"));
    	assertFalse(textnode.hasAttr("text"));
    	assertEquals("\ntext",textnode.text("text").toString());
    }
    
    @Test public void TestAbsUrl(){
    	Document doc = Jsoup.parse("<div name=val>one</div>");
    	Element div = doc.select("div").first();
    	TextNode textnode = (TextNode) div.childNode(0);
    	
    	assertEquals("",textnode.absUrl("text").toString());
    }
}
