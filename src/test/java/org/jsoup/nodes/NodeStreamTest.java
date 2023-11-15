package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static org.jsoup.nodes.NodeIteratorTest.trackSeen;
import static org.jsoup.nodes.NodeIteratorTest.assertContents;
import static org.junit.jupiter.api.Assertions.*;

public class NodeStreamTest {

    String html = "<div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div>";


    @Test void canStream() {
        Document doc = Jsoup.parse(html);
        StringBuilder seen = new StringBuilder();
        Stream<Node> stream = doc.nodeStream();
        stream.forEachOrdered(node -> trackSeen(node, seen));
        assertEquals("#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;", seen.toString());
    }

    @Test void canStreamParallel() {
        Document doc = Jsoup.parse(html);
        long count = doc.nodeStream().parallel().count();
        assertEquals(14, count);
    }

    @Test void canFindFirst() {
        Document doc = Jsoup.parse(html);
        Optional<Node> first = doc.nodeStream().findFirst();
        assertTrue(first.isPresent());
        assertSame(doc, first.get());
    }

    @Test void canFilter() {
        Document doc = Jsoup.parse(html);
        StringBuilder seen = new StringBuilder();

        doc.nodeStream()
            .filter(node -> node instanceof TextNode)
            .forEach(node -> trackSeen(node, seen));

        assertEquals("One;Two;Three;Four;", seen.toString());
    }

    @Test void canRemove() {
        String html = "<div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div><div id=3><p>Five";
        Document doc = Jsoup.parse(html);

        doc.nodeStream()
            .filter(node -> node instanceof Element)
                .filter(node -> node.attr("id").equals("1") || node.attr("id").equals("2"))
                    .forEach(Node::remove);

        assertContents(doc, "#root;html;head;body;div#3;p;Five;");
    }

    @Test void elementStream() {
        Document doc = Jsoup.parse(html);
        StringBuilder seen = new StringBuilder();
        Stream<Element> stream = doc.stream();
        stream.forEachOrdered(node -> trackSeen(node, seen));
        assertEquals("#root;html;head;body;div#1;p;p;div#2;p;p;", seen.toString());
    }

}
