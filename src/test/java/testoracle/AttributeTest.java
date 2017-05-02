package testoracle;

import static org.junit.Assert.*;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.junit.Test;

public class AttributeTest {

	@Test
	public void testAttribute() {
		Attribute a = new Attribute("Tot", "a&p");
		assertEquals("Tot", a.getKey());
		assertEquals("a&p", a.getValue());
	}
	
	@Test
	public void testSetKey() {
		Attribute a = new Attribute("Tot", "a&p");
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
	public void testHtml() {
		Attribute a = new Attribute("Tot", "a&p");
        assertEquals("Tot=\"a&amp;p\"", a.html());
        assertEquals(a.html(), a.toString());
	}
	
	@Test
	public void testCreateFromEncoded() {
		Attribute a = new Attribute("Tot", "a&p");
		Attribute aComp = new Attribute("äöü", "a&p");
		assertEquals(aComp, a.createFromEncoded("äöü", "a&p"));
	}
	
	/*
	@Test
	public void testIsDataAttribute() {
		Attribute a = new Attribute("data-name", "Jsoup");
		assertEquals(true, a.isDataAttribute());
	}
	*/
	
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
	
	@Test
	public void testHashCode() {
		Attribute a = new Attribute("Tot", "a&p");
		assertEquals(31 * "Tot".hashCode() + "a&p".hashCode(), a.hashCode());
	}
	
	@Test
	public void testClone() {
		Attribute a = new Attribute("Tot", "a&p");
		Attribute aComp = new Attribute("Tot", "a&p");
		
		assertEquals(aComp, a.clone());
	}

	/*
	@Test
	public void testGetKey() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetValue() {
		fail("Not yet implemented");
	}

	@Test
	public void testHtmlAppendableOutputSettings() {
		fail("Not yet implemented");
	}

	@Test
	public void testToString() {
		fail("Not yet implemented");
	}

	@Test
	public void testShouldCollapseAttribute() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsBooleanAttribute() {
		fail("Not yet implemented");
	}
	*/
}
