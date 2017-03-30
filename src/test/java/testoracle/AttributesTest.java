package testoracle;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.junit.Test;

/**
 * Tests for Attributes.
 *
 * @author Jonathan Hedley
 * Contributor : Woojin Lee (holinder4s)
 */
public class AttributesTest {
	
    @Test
    public void html() {
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

        Iterator<Attribute> iterator = a.iterator();
        assertTrue(iterator.hasNext());
        int i = 0;
        for (Attribute attribute : a) {
            assertEquals(datas[i][0], attribute.getKey());
            assertEquals(datas[i][1], attribute.getValue());
            i++;
        }
        assertEquals(datas.length, i);
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
		fail("Not yet implemented");
	}

	@Test
	public void testGet() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetIgnoreCase() {
		fail("Not yet implemented");
	}

	@Test
	public void testPutStringString() {
		fail("Not yet implemented");
	}

	@Test
	public void testPutStringBoolean() {
		Attributes a = new Attributes();
		a.put("Tot", true);
		assertEquals(1, a.size());
		assertTrue(a.hasKey("Tot"));
		
		a.put("Hello", false);
		assertEquals(1, a.size());
		assertFalse(a.hasKey("Hello"));
	}

	@Test
	public void testPutAttribute() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemove() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveIgnoreCase() {
		Attributes a = new Attributes();
        a.put("Tot", "a&p");
        a.put("Hello", "There");
        a.put("data-name", "Jsoup");

        assertEquals(3, a.size());
        a.removeIgnoreCase("tot");
        assertEquals(2, a.size());
        assertFalse(a.hasKey("Tot"));
	}
	
	@Test
	public void testHasKey() {
		Attributes aTT = new Attributes();
        aTT.put("Tot", "a&p");
        assertTrue(aTT.hasKey("Tot"));
        
        Attributes aTF = new Attributes();
        aTF.put("Tot", "");
        assertTrue(aTF.hasKey("Tot"));
        
        Attributes aFF = new Attributes();
        assertFalse(aFF.hasKey("Tot"));
	}

	@Test
	public void testHasKeyIgnoreCase() {
		Attributes a = new Attributes();
		assertFalse(a.hasKeyIgnoreCase("tot"));			// Case1 : attributes = null
		a.put("Tot", "a&p");					
		assertTrue(a.hasKeyIgnoreCase("tot"));			// Case2 : find attributes (standard case)
		assertFalse(a.hasKeyIgnoreCase("Hello"));		// Case3 : not found attributes
	}

	@Test
	public void testSize() {
		Attributes a = new Attributes();
		assertEquals(0, a.size());						// Case 1 : Attributes = null
		
		a.put("Tot", "a&p");
		assertEquals(1, a.size());						// Case 2 : Attributes standard
	}

	@Test
	public void testAddAll() {
		fail("Not yet implemented");
	}

	/*
	@Test
	public void testIterator() {
		fail("Not yet implemented");
	}
	*/

	@Test
	public void testAsList() {
		fail("Not yet implemented");
	}

	@Test
	public void testDataset() {
		fail("Not yet implemented");
	}

	@Test
	public void testHtml() {
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
	public void testEqualsObject() {
		fail("Not yet implemented");
	}

	@Test
	public void testClone() {
		fail("Not yet implemented");
	}

}
