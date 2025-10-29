package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ElementsBeforeNodeTest {

    @Test
    public void beforeNode_onMultipleElements_clonesPerInsert() {
        Document doc = Jsoup.parse("<div><p>A</p><p>B</p></div>");
        Elements ps = doc.select("p");
        TextNode tn = new TextNode("#");

        ps.before(tn);

        Element div = doc.selectFirst("div");
        assertNotNull(div);
        List<Node> kids = div.childNodes();

        // Expected: TextNode("#"), <p>A</p>, TextNode("#"), <p>B</p>
        assertEquals(4, kids.size());
        assertTrue(kids.get(0) instanceof TextNode);
        assertEquals("#", ((TextNode) kids.get(0)).text());

        assertTrue(kids.get(1) instanceof Element);
        assertEquals("p", ((Element) kids.get(1)).tagName());
        assertEquals("A", ((Element) kids.get(1)).text());

        assertTrue(kids.get(2) instanceof TextNode);
        assertEquals("#", ((TextNode) kids.get(2)).text());

        assertTrue(kids.get(3) instanceof Element);
        assertEquals("p", ((Element) kids.get(3)).tagName());
        assertEquals("B", ((Element) kids.get(3)).text());

        // Original node should remain unattached (we inserted clones)
        assertNull(tn.parent());
    }

    @Test
    public void beforeNode_sourceHasParent_isCloned() {
        Document doc = Jsoup.parse("<div><p>A</p><p>B</p></div>");
        Elements ps = doc.select("p");

        TextNode tn = new TextNode("#");
        Element span = new Element(Tag.valueOf("span"), "");
        span.appendChild(tn); // give tn a parent

        ps.before(tn);

        Element div = doc.selectFirst("div");
        assertNotNull(div);
        List<Node> kids = div.childNodes();

        // Expected: TextNode("#"), <p>A</p>, TextNode("#"), <p>B</p>
        assertEquals(4, kids.size());
        assertTrue(kids.get(0) instanceof TextNode);
        assertEquals("#", ((TextNode) kids.get(0)).text());
        assertTrue(kids.get(1) instanceof Element);
        assertEquals("p", ((Element) kids.get(1)).tagName());
        assertEquals("A", ((Element) kids.get(1)).text());
        assertTrue(kids.get(2) instanceof TextNode);
        assertEquals("#", ((TextNode) kids.get(2)).text());
        assertTrue(kids.get(3) instanceof Element);
        assertEquals("p", ((Element) kids.get(3)).tagName());
        assertEquals("B", ((Element) kids.get(3)).text());

        // The original tn stays under <span>, proving we cloned
        assertNotNull(tn.parent());
        assertEquals("span", tn.parent().nodeName());
    }

    @Test
    public void beforeString_existingBehavior_unchanged() {
        Document doc = Jsoup.parse("<div><p>A</p><p>B</p></div>");
        doc.select("p").before("<hr>");

        Element div = doc.selectFirst("div");
        assertNotNull(div);
        List<Node> kids = div.childNodes();

        // Expected: <hr>, <p>A</p>, <hr>, <p>B</p>
        assertEquals(4, kids.size());
        assertTrue(kids.get(0) instanceof Element);
        assertEquals("hr", ((Element) kids.get(0)).tagName());

        assertTrue(kids.get(1) instanceof Element);
        assertEquals("p", ((Element) kids.get(1)).tagName());
        assertEquals("A", ((Element) kids.get(1)).text());

        assertTrue(kids.get(2) instanceof Element);
        assertEquals("hr", ((Element) kids.get(2)).tagName());

        assertTrue(kids.get(3) instanceof Element);
        assertEquals("p", ((Element) kids.get(3)).tagName());
        assertEquals("B", ((Element) kids.get(3)).text());
    }

    @Test
    public void beforeNode_null_throwsNpe() {
        Document doc = Jsoup.parse("<div><p>A</p></div>");
        Elements ps = doc.select("p");
        assertThrows(NullPointerException.class, () -> ps.before((Node) null));
    }
}
