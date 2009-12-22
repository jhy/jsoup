package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 Test suite for attribute parser.

 @author Jonathan Hedley, jonathan@hedley.net */
public class AttributeParserTest {

    @Test public void parsesRoughAttributeString() {
        String a = "id=\"123\" class=\"baz = 'bar'\" style = 'border: 2px'qux zim foo = 12 mux.=18 ";
        // should be: <id=123>, <class=baz = 'bar'>, <qux=>, <zim=>, <foo=12>, <mux.=18>

        AttributeParser ap = new AttributeParser();
        Attributes attr = ap.parse(a);
        assertEquals(7, attr.size());
        assertEquals("123", attr.get("id"));
        assertEquals("baz = 'bar'", attr.get("class"));
        assertEquals("border: 2px", attr.get("style"));
        assertEquals("", attr.get("qux"));
        assertEquals("", attr.get("zim"));
        assertEquals("12", attr.get("foo"));
        assertEquals("18", attr.get("mux."));
    }

    @Test public void parsesEmptyString() {
        AttributeParser ap = new AttributeParser();
        Attributes attr = ap.parse("");
        assertEquals(0, attr.size());
    }

    @Test public void emptyOnNoKey() {
        AttributeParser ap = new AttributeParser();
        Attributes attr = ap.parse("=empty");
        assertEquals(0, attr.size());
    }

    @Test public void parserIsReusable() {
        AttributeParser ap = new AttributeParser();
        Attributes attr = ap.parse("id=bar");
        assertEquals(1, attr.size());
        assertEquals("bar", attr.get("id"));

        attr = ap.parse("id=qux");
        assertEquals(1, attr.size());
        assertEquals("qux", attr.get("id"));
    }
}
