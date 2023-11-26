package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class NodeIteratorTest {
    String html = "<div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div>";

    @Test void canIterateNodes() {
        Document doc = Jsoup.parse(html);
        NodeIterator<Node> it = NodeIterator.from(doc);
        assertIterates(it, "#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;");
        // todo - need to review that the Document object #root holds the html element as child. Why not have document root == html element?
        assertFalse(it.hasNext());

        boolean threw = false;
        try {
            it.next();
        } catch (NoSuchElementException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test void hasNextIsPure() {
        Document doc = Jsoup.parse(html);
        NodeIterator<Node> it = NodeIterator.from(doc);
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertIterates(it, "#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;");
        assertFalse(it.hasNext());
    }

    @Test void iterateSubTree() {
        Document doc = Jsoup.parse(html);

        Element div1 = doc.expectFirst("div#1");
        NodeIterator<Node> it = NodeIterator.from(div1);
        assertIterates(it, "div#1;p;One;p;Two;");
        assertFalse(it.hasNext());

        Element div2 = doc.expectFirst("div#2");
        NodeIterator<Node> it2 = NodeIterator.from(div2);
        assertIterates(it2, "div#2;p;Three;p;Four;");
        assertFalse(it2.hasNext());
    }

    @Test void canRestart() {
        Document doc = Jsoup.parse(html);

        NodeIterator<Node> it = NodeIterator.from(doc);
        assertIterates(it, "#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;");

        it.restart(doc.expectFirst("div#2"));
        assertIterates(it, "div#2;p;Three;p;Four;");
    }

    @Test void canIterateJustOneSibling() {
        Document doc = Jsoup.parse(html);
        Element p2 = doc.expectFirst("p:contains(Two)");
        assertEquals("Two", p2.text());

        NodeIterator<Node> it = NodeIterator.from(p2);
        assertIterates(it, "p;Two;");

        NodeIterator<Element> elIt = new NodeIterator<>(p2, Element.class);
        Element found = elIt.next();
        assertSame(p2, found);
        assertFalse(elIt.hasNext());
    }

    @Test void canIterateFirstEmptySibling() {
        Document doc = Jsoup.parse("<div><p id=1></p><p id=2>.</p><p id=3>..</p>");
        Element p1 = doc.expectFirst("p#1");
        assertEquals("", p1.ownText());

        NodeIterator<Node> it = NodeIterator.from(p1);
        assertTrue(it.hasNext());
        Node node = it.next();
        assertSame(p1, node);
        assertFalse(it.hasNext());
    }

    @Test void canRemoveViaIterator() {
        String html = "<div id=out1><div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div></div><div id=out2>Out2";
        Document doc = Jsoup.parse(html);

        NodeIterator<Node> it = NodeIterator.from(doc);
        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.attr("id").equals("1"))
                it.remove();
            trackSeen(node, seen);
        }
        assertEquals("#root;html;head;body;div#out1;div#1;div#2;p;Three;p;Four;div#out2;Out2;", seen.toString());
        assertContents(doc, "#root;html;head;body;div#out1;div#2;p;Three;p;Four;div#out2;Out2;");

        it = NodeIterator.from(doc);
        seen = new StringBuilder();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.attr("id").equals("2"))
                it.remove();
            trackSeen(node, seen);
        }
        assertEquals("#root;html;head;body;div#out1;div#2;div#out2;Out2;", seen.toString());
        assertContents(doc, "#root;html;head;body;div#out1;div#out2;Out2;");
    }

    @Test void canRemoveViaNode() {
        String html = "<div id=out1><div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div></div><div id=out2>Out2";
        Document doc = Jsoup.parse(html);

        NodeIterator<Node> it = NodeIterator.from(doc);
        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.attr("id").equals("1"))
                node.remove();
            trackSeen(node, seen);
        }
        assertEquals("#root;html;head;body;div#out1;div#1;div#2;p;Three;p;Four;div#out2;Out2;", seen.toString());
        assertContents(doc, "#root;html;head;body;div#out1;div#2;p;Three;p;Four;div#out2;Out2;");

        it = NodeIterator.from(doc);
        seen = new StringBuilder();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.attr("id").equals("2"))
                node.remove();
            trackSeen(node, seen);
        }
        assertEquals("#root;html;head;body;div#out1;div#2;div#out2;Out2;", seen.toString());
        assertContents(doc, "#root;html;head;body;div#out1;div#out2;Out2;");
    }

    @Test void canReplace() {
        String html = "<div id=out1><div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div></div><div id=out2>Out2";
        Document doc = Jsoup.parse(html);

        NodeIterator<Node> it = NodeIterator.from(doc);
        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            Node node = it.next();
            trackSeen(node, seen);
            if (node.attr("id").equals("1")) {
                node.replaceWith(new Element("span").text("Foo"));
            }
        }
        assertEquals("#root;html;head;body;div#out1;div#1;span;Foo;div#2;p;Three;p;Four;div#out2;Out2;", seen.toString());
        // ^^ we don't see <p>One, do see the replaced in <span>, and the subsequent nodes
        assertContents(doc, "#root;html;head;body;div#out1;span;Foo;div#2;p;Three;p;Four;div#out2;Out2;");

        it = NodeIterator.from(doc);
        seen = new StringBuilder();
        while (it.hasNext()) {
            Node node = it.next();
            trackSeen(node, seen);
            if (node.attr("id").equals("2")) {
                node.replaceWith(new Element("span").text("Bar"));
            }
        }
        assertEquals("#root;html;head;body;div#out1;span;Foo;div#2;span;Bar;div#out2;Out2;", seen.toString());
        assertContents(doc, "#root;html;head;body;div#out1;span;Foo;span;Bar;div#out2;Out2;");
    }

    @Test void canWrap() {
        Document doc = Jsoup.parse(html);
        NodeIterator<Node> it = NodeIterator.from(doc);
        boolean sawInner = false;
        while (it.hasNext()) {
            Node node = it.next();
            if (node.attr("id").equals("1")) {
                node.wrap("<div id=outer>");
            }
            if (node instanceof TextNode && ((TextNode) node).text().equals("One"))
                sawInner = true;
        }
        assertContents(doc, "#root;html;head;body;div#outer;div#1;p;One;p;Two;div#2;p;Three;p;Four;");
        assertTrue(sawInner);
    }

    @Test void canFilterForElements() {
        Document doc = Jsoup.parse(html);
        NodeIterator<Element> it = new NodeIterator<>(doc, Element.class);

        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            Element el = it.next();
            assertNotNull(el);
            trackSeen(el, seen);
        }

        assertEquals("#root;html;head;body;div#1;p;p;div#2;p;p;", seen.toString());
    }

    @Test void canFilterForTextNodes() {
        Document doc = Jsoup.parse(html);
        NodeIterator<TextNode> it = new NodeIterator<>(doc, TextNode.class);

        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            TextNode text = it.next();
            assertNotNull(text);
            trackSeen(text, seen);
        }

        assertEquals("One;Two;Three;Four;", seen.toString());
        assertContents(doc, "#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;");
    }

    @Test void canModifyFilteredElements() {
        Document doc = Jsoup.parse(html);
        NodeIterator<Element> it = new NodeIterator<>(doc, Element.class);

        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            Element el = it.next();
            if (!el.ownText().isEmpty())
                el.text(el.ownText() + "++");
            trackSeen(el, seen);
        }

        assertEquals("#root;html;head;body;div#1;p;p;div#2;p;p;", seen.toString());
        assertContents(doc, "#root;html;head;body;div#1;p;One++;p;Two++;div#2;p;Three++;p;Four++;");
    }

    static <T extends Node> void assertIterates(NodeIterator<T> it, String expected) {
        Node previous = null;
        StringBuilder actual = new StringBuilder();
        while (it.hasNext()) {
            Node node = it.next();
            assertNotNull(node);
            assertNotSame(previous, node);

            trackSeen(node, actual);
            previous = node;
        }
        assertEquals(expected, actual.toString());
    }

    static void assertContents(Element el, String expected) {
        NodeIterator<Node> it = NodeIterator.from(el);
        assertIterates(it, expected);
    }

    static void trackSeen(Node node, StringBuilder actual) {
        if (node instanceof Element) {
            Element el = (Element) node;
            actual.append(el.tagName());
            if (el.hasAttr("id"))
                actual.append("#").append(el.id());
        }
        else if (node instanceof TextNode)
            actual.append(((TextNode) node).text());
        else
            actual.append(node.nodeName());
        actual.append(";");
    }

}