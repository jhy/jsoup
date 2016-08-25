package org.jsoup.nodes;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

/**
 * Tests for Attributes.
 *
 * @author Jonathan Hedley
 */
public class AttributesTest {
    
    @Test public void html() {
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

}
