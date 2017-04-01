package testoracle;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.junit.Ignore;
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

	@Test
	public void testDatasetPutStringString() {
		Attributes a = new Attributes();
		a.put("Tot", "a&p");
		a.put("Hello", "There");
		a.put("data-name", "Jsoup");
		Map<String, String> html5Custom = a.dataset();
		assertEquals(1, html5Custom.size());

		assertEquals("Jsoup", html5Custom.put("name", "Lsoup"));	// Case4 : dataset's put method with same key test
		assertEquals(1, html5Custom.size());
		assertEquals(3, a.size());
		assertEquals("Lsoup", a.get("data-name"));
	}
	
	@Test
	public void testPutStringString() {
		Attributes a = new Attributes();
		a.put("Tot", "a&p");
		assertEquals(1, a.size());
		assertTrue(a.hasKey("Tot"));
		
		a.put("Hello", "There");
		assertEquals(2, a.size());
		assertTrue(a.hasKey("Tot"));
		
		//assertSame("a&p", a.put("Tot", "wjdebug"));
		//assertEquals(2, a.size());
		//assertTrue(a.hasKey("Tot"));
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

	/*
	@Test
	public void testPutAttribute() {
		fail("Not yet implemented");
	}
	*/

	@Test
	public void testRemove() {
		Attributes a = new Attributes();
		a.remove("Tot");					// Case1 : attributes == null
		assertEquals(0, a.size());
		a.put("Tot", "a&p");
		a.put("Hello", "There");
		a.remove("Hello");					// Case2 : attributes != null, standard case
		assertEquals(1, a.size());
		assertFalse(a.hasKey("Hello"));
		a.remove("data-name");				// Case3 : attributes != null, not exist key remove test
		assertEquals(1, a.size());
	}

	@Test
	public void testRemoveIgnoreCase() {
		Attributes a = new Attributes();
		a.removeIgnoreCase("tot");			// Case1 : attributes == null
		assertEquals(0, a.size());
        a.put("Tot", "a&p");
        a.put("Hello", "There");
        a.put("data-name", "Jsoup");

        assertEquals(3, a.size());
        a.removeIgnoreCase("tot");			// Case2 : attributes != null, standard case
        assertEquals(2, a.size());
        assertFalse(a.hasKey("Tot"));
        
        a.removeIgnoreCase("unknown");		// Case3 : attributes != null, not exist key removeIgnore test
        assertEquals(2, a.size());
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
		Attributes tmp = new Attributes();
		Attributes a = new Attributes();
		
		a.addAll(tmp);							// Case1 : incoming == null
		assertEquals(0, a.size());
		
		tmp.put("Tot", "a&p");
		tmp.put("Hello", "There");
		a.addAll(tmp);							// Case2 : incomming is normal case
		assertEquals(2, a.size());
		assertTrue(a.hasKey("Tot"));
		assertTrue(a.hasKey("Hello"));
		
		Attributes aNotNull = new Attributes();
		aNotNull.put("data-name", "Jsoup");
		aNotNull.addAll(tmp);					// Case3 : attributes is not null
		assertEquals(3, aNotNull.size());
		assertTrue(aNotNull.hasKey("data-name"));
		assertTrue(aNotNull.hasKey("Tot"));
		assertTrue(aNotNull.hasKey("Hello"));
	}

	/*
	@Test
	public void testIterator() {
		fail("Not yet implemented");
	}
	*/

	@Test
	public void testAsList() {
		Attributes aNull = new Attributes();						
		List<Attribute> EmptyList = new ArrayList<Attribute>();
		assertEquals(EmptyList, aNull.asList());					// Case1 : attributes == null
		
		Attributes a = new Attributes();
		List<Attribute> list = new ArrayList<Attribute>();
		a.put("Tot", "a&p");
		a.put("Hello", "There");
		list = a.asList();											// Case2 : Normal case
		assertEquals("Tot", list.get(0).getKey());
		assertEquals("a&p", list.get(0).getValue());
		assertEquals("Hello", list.get(1).getKey());
		assertEquals("There", list.get(1).getValue());
	}

	@Test
	public void testDataset() {
		Attributes a = new Attributes();
		Map<String, String> html5CustomNull = a.dataset();			// Case1 : attributes == null
		assertEquals(0, html5CustomNull.size());
		a.put("Tot", "a&p");
		a.put("Hello", "There");
		a.put("data-name", "Jsoup");
		Map<String, String> html5Custom = a.dataset();
		assertEquals(1, html5Custom.size());						// Case2 : attributes != null
		
		assertEquals(null, html5Custom.put("asdf", "Ksoup"));		// Case3 : dataset's put method standard test
		assertEquals(2, html5Custom.size());
		assertEquals(4, a.size());
		assertEquals("Ksoup", a.get("data-asdf"));
		assertEquals("Jsoup", html5Custom.put("name", "Lsoup"));	// Case4 : dataset's put method with same key test
		assertEquals(2, html5Custom.size());
		assertEquals(4, a.size());
		assertEquals("Lsoup", a.get("data-name"));

		html5Custom.remove("asdf");									// Case5 : dataset's remove method test
		assertEquals(1, html5Custom.size());
		assertEquals(3, a.size());
		assertEquals("", a.get("data-asdf"));
	}

	/*
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
	*/

	@Test
	public void testEqualsObject() {
		Attributes aOne = new Attributes();
		Attributes aTwo = new Attributes();
		assertTrue(aOne.equals(aTwo));			// Case1 : attributes == null, compare with null
		aTwo.put("Tot", "a&p");
		aTwo.put("Hello", "There");
		assertFalse(aOne.equals(aTwo));			// Case2 : attributes == null, compare with another attr
		aOne.put("Tot", "a&p");
		aOne.put("Hello", "There");
		assertFalse(aOne.equals(null));			// Case2 : attributes != null, Compare with null
		assertTrue(aOne.equals(aOne));			// Case3 : attributes != null, Compare with itself
		assertTrue(aOne.equals(aTwo));			// Case4 : attributes != null, Compare with another attr(equal value)
		aTwo.remove("Hello");
		assertFalse(aOne.equals(aTwo));			// Case5 : attributes != null, Compare with another attr(not equal value)
		aTwo.remove("Tot");
		assertFalse(aOne.equals(aTwo));			// Case6 : attributes != null, Compare with empty attr
	}

	@Test
	public void testClone() {
		Attributes a = new Attributes();
		Attributes aComp = new Attributes();
		assertEquals(aComp, a.clone());			// Case1 : attributes == null
		a.put("Tot", "a&p");
		a.put("Hello", "There");
		aComp.put("Tot", "a&p");
		aComp.put("Hello", "There");
		assertEquals(aComp, a.clone());			// Case2 : attributes != null
	}

}
