package org.jsoup.nodes;

import static org.junit.Assert.*;

import org.junit.Test;

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
    
    @Test
	public void testEqualsObject() {
		Attribute a = new Attribute("Tot", "a&p");
		Attribute aKeyValueEqual = new Attribute("Tot", "a&p");
		Attribute aOnlyKeyEqual = new Attribute("Tot", "wjdebug");
		Attribute aOnlyValueEqual = new Attribute("Hello", "a&p");
		Attribute aNotEqual = new Attribute("Hello", "There");
		Attributes as = new Attributes();
		as.put("Tot", "a&p");
		
		assertTrue(a.equals(a));				// Case1 : Compare with self
		assertTrue(a.equals(aKeyValueEqual));	// Case2 : Compare with Key,Value Equal Attribute
		assertFalse(a.equals(aOnlyKeyEqual));	// Case3 : Compare with only Key Equal Attribute
		assertFalse(a.equals(aOnlyValueEqual));	// Case4 : Compare with only Value Equal Attribute
		assertFalse(a.equals(aNotEqual));		// Case5 : Compare with not equal Attribute
		
		assertFalse(a.equals(as));				// Case6 : Compare with Attributes
	}
}
