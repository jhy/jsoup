package org.jsoup.nodes;

import org.jsoup.parser.StartTag;
import org.jsoup.parser.Tag;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 Tests Nodes

 @author Jonathan Hedley, jonathan@hedley.net */
public class NodeTest {
    @Test public void handlesBaseUri() {
        Tag tag = Tag.valueOf("a");
        Attributes attribs = new Attributes();
        attribs.put("relHref", "/foo");
        attribs.put("absHref", "http://bar/qux");

        Element noBase = new Element(new StartTag(tag, "", attribs));
        assertEquals("/foo", noBase.absUrl("relHref")); // with no base, should fallback to href attrib, whatever it is

        Element withBase = new Element(new StartTag(tag, "http://foo/", attribs));
        assertEquals("http://foo/foo", withBase.absUrl("relHref")); // construct abs from base + rel
        assertEquals("http://bar/qux", withBase.absUrl("absHref")); // href is abs, so returns that
        assertEquals("http://foo/", withBase.absUrl("noval"));

        Element dodgyBase = new Element(new StartTag(tag, "wtf://no-such-protocol/", attribs));
        assertEquals("http://bar/qux", dodgyBase.absUrl("absHref")); // base fails, but href good, so get that
        assertEquals("", dodgyBase.absUrl("relHref")); // base fails, only rel href, so return nothing 
    }
}
