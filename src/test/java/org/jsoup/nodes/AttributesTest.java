package org.jsoup.nodes;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Attributes.
 *
 * @author Jonathan Hedley
 */
public class AttributesTest {

    @Test
    public void html() {
    	Attributes aNull = new Attributes();
    	assertEquals("", aNull.html());
    	assertEquals(aNull.html(), aNull.toString());
    	
        Attributes a = new Attributes();
        a.put("Tot", "a&p");
        a.put("Hello", "There");
        a.put("data-name", "Jsoup");

        assertEquals(3, a.size());
        assertTrue(a.hasKey("Tot"));
        assertTrue(a.hasKey("Hello"));
        assertTrue(a.hasKey("data-name"));
        assertFalse(a.hasKey("tot"));
        assertTrue(a.hasKeyIgnoreCase("tot"));
        assertEquals("There", a.getIgnoreCase("hEllo"));

        assertEquals(1, a.dataset().size());
        assertEquals("Jsoup", a.dataset().get("name"));
        assertEquals("", a.get("tot"));
        assertEquals("a&p", a.get("Tot"));
        assertEquals("a&p", a.getIgnoreCase("tot"));
        assertEquals("a&p", a.getIgnoreCase("Tot"));	// Add Case : correct case-sensitive attribute data to getIgnoreCase()'s arg

        assertEquals(" Tot=\"a&amp;p\" Hello=\"There\" data-name=\"Jsoup\"", a.html());
        assertEquals(a.html(), a.toString());
    }

    @Test
    public void testIteratorRemovable() {
        Attributes a = new Attributes();
        a.put("Tot", "a&p");
        a.put("Hello", "There");
        a.put("data-name", "Jsoup");

        Iterator<Attribute> iterator = a.iterator();
        iterator.next();
        iterator.remove();
        assertEquals(2, a.size());
    }

    @Test
    public void testIterator() {
    	Attributes a = new Attributes();						
        String[][] datas = {{"Tot", "raul"},
            {"Hello", "pismuth"},
            {"data-name", "Jsoup"}};
        for (String[] atts : datas) {
            a.put(atts[0], atts[1]);
        }

        Iterator<Attribute> iterator = a.iterator();			// Case1 : Attributes != null, !attributes.isEmpty()
        assertTrue(iterator.hasNext());
        int i = 0;
        for (Attribute attribute : a) {
            assertEquals(datas[i][0], attribute.getKey());
            assertEquals(datas[i][1], attribute.getValue());
            i++;
        }
        assertEquals(datas.length, i);
        
        Attributes aNull = new Attributes();					
        Iterator<Attribute> iteratorNull = aNull.iterator();	// Case 2 : Attributes == null
        assertFalse(iteratorNull.hasNext());
        
        Attributes aEmpty = new Attributes();
        aEmpty.put("Tot", "a&p");
        aEmpty.remove("Tot");
        Iterator<Attribute> iteratorEmpty = aEmpty.iterator();	// Case 3 : Attributes != null, attributes.isEmpty()
        assertFalse(iteratorEmpty.hasNext());
    }

    @Test
    public void testIteratorEmpty() {
        Attributes a = new Attributes();

        Iterator<Attribute> iterator = a.iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void removeCaseSensitive() {
        Attributes a = new Attributes();
        a.put("Tot", "a&p");
        a.put("tot", "one");
        a.put("Hello", "There");
        a.put("hello", "There");
        a.put("data-name", "Jsoup");

        assertEquals(5, a.size());
        a.remove("Tot");
        a.remove("Hello");
        assertEquals(3, a.size());
        assertTrue(a.hasKey("tot"));
        assertFalse(a.hasKey("Tot"));
    }
    
    @Test
	public void testHashCode() {
		Attributes a = new Attributes();
		assertEquals(0, a.hashCode());				// Case1 : attributes == null
		a.put("Tot", "a&p");
		a.put("Hello", "There");
		assertEquals(-2123275893, a.hashCode());	// Case2 : attributes != null
	}
    
    @Test
	public void testGet() {
		Attributes a = new Attributes();			
		assertEquals("", a.get("Tot"));				// Case1 : attributes == null
		a.put("Tot", "a&p");
		assertEquals("a&p", a.get("Tot"));			// Case2 : attributes != null
	}
    
    @Test
	public void testGetIgnoreCase() {
		Attributes a = new Attributes();
		assertEquals("", a.getIgnoreCase("tot"));			// Case1 : attributes == null
		a.put("Tot", "a&p");
		a.put("Hello", "There");
		assertEquals("There", a.getIgnoreCase("hello"));	// Case2 : attributes != null, stadard case
		assertEquals("", a.getIgnoreCase("data-name"));		// Case3 : attributes != null, not exist key get test
	}

}
