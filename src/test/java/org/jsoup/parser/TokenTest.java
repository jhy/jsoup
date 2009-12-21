package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 Tests Tokens.

 @author Jonathan Hedley, jonathan@hedley.net */
public class TokenTest {
    private static Position startPos = new Position();

    @Test public void matchesStartTag() {
        Token t = token("<p>");

        assertTrue(t.isStartTag());
        assertFalse(t.isEndTag());
        assertEquals("p", t.getTagName());
        assertNull(t.getAttributeString());
    }

    @Test public void matchesTagWithAttributes() {
        Token t = token("<div id=\"foo\" width=500>");

        assertTrue(t.isStartTag());
        assertFalse(t.isEndTag());
        assertEquals("div", t.getTagName());
        assertEquals("id=\"foo\" width=500", t.getAttributeString());
    }

    @Test public void matchesEndTag() {
        Token t = token("</span>");

        assertFalse(t.isStartTag());
        assertTrue(t.isEndTag());
        assertEquals("span", t.getTagName());
        assertNull(t.getAttributeString());
    }

    @Test public void matchesEmptyTag() {
        Token t = token("<br />");

        assertTrue(t.isStartTag());
        assertTrue(t.isEndTag());
        assertEquals("br", t.getTagName());
        assertNull(t.getAttributeString());

        // repeat without space
        t = token("<br/>");
        assertTrue(t.isStartTag());
        assertTrue(t.isEndTag());
        assertEquals("br", t.getTagName());
        assertNull(t.getAttributeString());
    }

    @Test public void matchesEmptyTagWithAttributes() {
        Token t = token("<img src=foo.png />");

        assertTrue(t.isStartTag());
        assertTrue(t.isEndTag());
        assertEquals("img", t.getTagName());
        assertEquals("src=foo.png", t.getAttributeString());
    }

    @Test public void matchesText() {
        Token t = token("Hello, world!");

        assertTrue(t.isTextNode());
        assertFalse(t.isStartTag());
        assertFalse(t.isEndTag());
        assertNull(t.getAttributeString());
        assertEquals("Hello, world!", t.getData());
    }

    private Token token(String data) {
        return new Token(data, startPos);
    }
}
