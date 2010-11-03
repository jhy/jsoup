package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 Tag tests.
 @author Jonathan Hedley, jonathan@hedley.net */
public class TagTest {

    @Test public void isCaseInsensitive() {
        Tag p1 = Tag.valueOf("P");
        Tag p2 = Tag.valueOf("p");
        assertEquals(p1, p2);
    }

    @Test public void trims() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf(" p ");
        assertEquals(p1, p2);
    }

    @Test public void equality() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf("p");
        assertTrue(p1.equals(p2));
        assertTrue(p1 == p2);
    }

    @Test public void divSemantics() {
        Tag div = Tag.valueOf("div");
        Tag p = Tag.valueOf("p");

        assertTrue(div.canContain(div));
        assertTrue(div.canContain(p));
    }

    @Test public void pSemantics() {
        Tag div = Tag.valueOf("div");
        Tag p = Tag.valueOf("p");
        Tag img = Tag.valueOf("img");
        Tag span = Tag.valueOf("span");

        assertTrue(p.canContain(img));
        assertTrue(p.canContain(span));
        assertFalse(p.canContain(div));
        assertFalse(p.canContain(p));
    }

    @Test public void spanSemantics() {
        Tag span = Tag.valueOf("span");
        Tag p = Tag.valueOf("p");
        Tag div = Tag.valueOf("div");

        assertTrue(span.canContain(span));
        assertTrue(span.canContain(p));
        assertTrue(span.canContain(div));
    }

    @Test public void imgSemantics() {
        Tag img = Tag.valueOf("img");
        Tag p = Tag.valueOf("p");

        assertFalse(img.canContain(img));
        assertFalse(img.canContain(p));
    }

    @Test public void defaultSemantics() {
        Tag foo = Tag.valueOf("foo"); // not defined
        Tag foo2 = Tag.valueOf("FOO");
        Tag div = Tag.valueOf("div");

        assertEquals(foo, foo2);
        assertTrue(foo.canContain(foo));
        assertTrue(foo.canContain(div));
        assertTrue(div.canContain(foo));
    }

    @Test(expected = IllegalArgumentException.class) public void valueOfChecksNotNull() {
        Tag.valueOf(null);
    }

    @Test(expected = IllegalArgumentException.class) public void valueOfChecksNotEmpty() {
        Tag.valueOf(" ");
    }
}
