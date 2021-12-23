package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Attributes.
 *
 * @author Jonathan Hedley
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

        Map<String, String> dataset = a.dataset();
        assertEquals(1, dataset.size());
        assertEquals("Jsoup", dataset.get("name"));
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
        assertTrue(a.hasKey("Tot"));

        Iterator<Attribute> iterator = a.iterator();
        Attribute attr = iterator.next();
        assertEquals("Tot", attr.getKey());
        iterator.remove();
        assertEquals(2, a.size());
        attr = iterator.next();
        assertEquals("Hello", attr.getKey());
        assertEquals("There", attr.getValue());

        // make sure that's flowing to the underlying attributes object
        assertEquals(2, a.size());
        assertEquals("There", a.get("Hello"));
        assertFalse(a.hasKey("Tot"));
    }

    @Test
    public void testIteratorUpdateable() {
        Attributes a = new Attributes();
        a.put("Tot", "a&p");
        a.put("Hello", "There");

        assertFalse(a.hasKey("Foo"));
        Iterator<Attribute> iterator = a.iterator();
        Attribute attr = iterator.next();
        attr.setKey("Foo");
        attr = iterator.next();
        attr.setKey("Bar");
        attr.setValue("Qux");

        assertEquals("a&p", a.get("Foo"));
        assertEquals("Qux", a.get("Bar"));
        assertFalse(a.hasKey("Tot"));
        assertFalse(a.hasKey("Hello"));
    }

    @Test public void testIteratorHasNext() {
        Attributes a = new Attributes();
        a.put("Tot", "1");
        a.put("Hello", "2");
        a.put("data-name", "3");

        int seen = 0;
        for (Attribute attribute : a) {
            seen++;
            assertEquals(String.valueOf(seen), attribute.getValue());
        }
        assertEquals(3, seen);
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
    public void testIteratorSkipsInternal() {
        Attributes a = new Attributes();
        a.put("One", "One");
        a.put(Attributes.internalKey("baseUri"), "example.com");
        a.put("Two", "Two");
        a.put(Attributes.internalKey("another"), "example.com");

        Iterator<Attribute> it = a.iterator();
        assertTrue(it.hasNext());
        assertEquals("One", it.next().getKey());
        assertTrue(it.hasNext());
        assertEquals("Two", it.next().getKey());
        assertFalse(it.hasNext());

        int seen = 0;
        for (Attribute attribute : a) {
            seen++;
        }
        assertEquals(2, seen);
    }

    @Test
    public void testListSkipsInternal() {
        Attributes a = new Attributes();
        a.put("One", "One");
        a.put(Attributes.internalKey("baseUri"), "example.com");
        a.put("Two", "Two");
        a.put(Attributes.internalKey("another"), "example.com");

        List<Attribute> attributes = a.asList();
        assertEquals(2, attributes.size());
        assertEquals("One", attributes.get(0).getKey());
        assertEquals("Two", attributes.get(1). getKey());
    }

    @Test public void htmlSkipsInternals() {
        Attributes a = new Attributes();
        a.put("One", "One");
        a.put(Attributes.internalKey("baseUri"), "example.com");
        a.put("Two", "Two");
        a.put(Attributes.internalKey("another"), "example.com");

        assertEquals(" One=\"One\" Two=\"Two\"", a.html());
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
    public void testSetKeyConsistency() {
        Attributes a = new Attributes();
        a.put("a", "a");
        for(Attribute at : a) {
            at.setKey("b");
        }
        assertFalse(a.hasKey("a"), "Attribute 'a' not correctly removed");
        assertTrue(a.hasKey("b"), "Attribute 'b' not present after renaming");
    }

    @Test
    public void testBoolean() {
        Attributes ats = new Attributes();
        ats.put("a", "a");
        ats.put("B", "b");
        ats.put("c", null);

        assertTrue(ats.hasDeclaredValueForKey("a"));
        assertFalse(ats.hasDeclaredValueForKey("A"));
        assertTrue(ats.hasDeclaredValueForKeyIgnoreCase("A"));

        assertFalse(ats.hasDeclaredValueForKey("c"));
        assertFalse(ats.hasDeclaredValueForKey("C"));
        assertFalse(ats.hasDeclaredValueForKeyIgnoreCase("C"));
    }

    @Test public void testSizeWhenHasInternal() {
        Attributes a = new Attributes();
        a.put("One", "One");
        a.put("Two", "Two");
        assertEquals(2, a.size());

        a.put(Attributes.internalKey("baseUri"), "example.com");
        a.put(Attributes.internalKey("another"), "example.com");
        a.put(Attributes.internalKey("last"), "example.com");
        a.remove(Attributes.internalKey("last"));

        assertEquals(4, a.size());
        assertEquals(2, a.asList().size()); // excluded from lists
    }

    @Test public void testBooleans() {
        // want unknown=null, and known like async=null, async="", and async=async to collapse
        String html = "<a foo bar=\"\" async=async qux=qux defer=deferring ismap inert=\"\">";
        Element el = Jsoup.parse(html).selectFirst("a");
        assertEquals(" foo bar=\"\" async qux=\"qux\" defer=\"deferring\" ismap inert", el.attributes().html());

    }

    @Test public void booleanNullAttributesConsistent() {
        Attributes attributes = new Attributes();
        attributes.put("key", null);
        Attribute attribute = attributes.iterator().next();

        assertEquals("key", attribute.html());
        assertEquals(" key", attributes.html());
    }

    @Test public void booleanEmptyString() {
        Attributes attributes = new Attributes();
        attributes.put("checked", "");
        Attribute attribute = attributes.iterator().next();

        assertEquals("checked", attribute.html());
        assertEquals(" checked", attributes.html());
    }

    @Test public void booleanCaseInsensitive() {
        Attributes attributes = new Attributes();
        attributes.put("checked", "CHECKED");
        Attribute attribute = attributes.iterator().next();

        assertEquals("checked", attribute.html());
        assertEquals(" checked", attributes.html());
    }

    @Test public void equalsIsOrderInsensitive() {
        Attributes one = new Attributes()
            .add("Key1", "Val1")
            .add("Key2", "Val2")
            .add("Key3", null);

        Attributes two = new Attributes()
            .add("Key1", "Val1")
            .add("Key2", "Val2")
            .add("Key3", null);

        Attributes three = new Attributes()
            .add("Key2", "Val2")
            .add("Key3", null)
            .add("Key1", "Val1");

        Attributes four = new Attributes()
            .add("Key1", "Val1")
            .add("Key2", "Val2")
            .add("Key3", null)
            .add("Key4", "Val4");

        assertEquals(one, one.clone());
        assertEquals(one, two);
        assertEquals(two, two);
        assertEquals(one, three);
        assertEquals(two, three);
        assertEquals(three, three);
        assertEquals(three, three.clone());
        assertEquals(four, four);
        assertEquals(four, four.clone());
        assertNotEquals(one, four);
    }

    @Test void cloneAttributes() {
        Attributes one = new Attributes()
            .add("Key1", "Val1")
            .add("Key2", "Val2")
            .add("Key3", null);
        Attributes two = one.clone();
        assertEquals(3, two.size());
        assertEquals("Val2", two.get("Key2"));
        assertEquals(one, two);

        two.add("Key4", "Val4");
        assertEquals(4, two.size());
        assertEquals(3, one.size());
        assertNotEquals(one, two);

    }
}
