package org.jsoup.nodes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttributeTest {
    @Test public void html() {
        Attribute attr = new Attribute("key", "value &");
        assertEquals("key=\"value &amp;\"", attr.html());
        assertEquals(attr.html(), attr.toString());
    }

    @Test public void testWithSupplementaryCharacterInAttributeKeyAndValue() {
        String s = new String(Character.toChars(135361));
        Attribute attr = new Attribute(s, "A" + s + "B");
        assertEquals(s + "=\"A" + s + "B\"", attr.html());
        assertEquals(attr.html(), attr.toString());
    }
    
    @Test
	public void testAttribute() {
		Attribute a = new Attribute("Tot", "a&p");
		assertEquals("Tot", a.getKey());
		assertEquals("a&p", a.getValue());
	}
    
    @Test
	public void testSetKey() {
		Attribute a = new Attribute("Tot", "a&p");
		//assertEquals("Tot", a.setKey("Hello"));			// old key return test
		a.setKey("Hello");
		assertEquals("Hello", a.getKey());
		assertEquals("a&p", a.getValue());
	}
    
    @Test
	public void testSetValue() {
		Attribute a = new Attribute("Tot", "a&p");
		assertEquals("a&p", a.setValue("wjdebug"));			// old value return test
		assertEquals("Tot", a.getKey());
		assertEquals("wjdebug", a.getValue());
	}
    
    @Test
	public void testCreateFromEncoded() {
		Attribute a = new Attribute("Tot", "a&p");
		Attribute aComp = new Attribute("우진디벅", "a&p");
		assertEquals(aComp, a.createFromEncoded("우진디벅", "a&p"));
	}
}
