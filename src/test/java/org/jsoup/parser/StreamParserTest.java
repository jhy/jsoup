package org.jsoup.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StreamParserTest {

    @Test
    void stream() {
        String html = "<title>Test</title></head><div id=1>D1</div><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");

        StringBuilder seen = new StringBuilder();
        parser.stream().forEachOrdered(el -> trackSeen(el, seen));
        assertEquals("title[Test];head+;div#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];body;html;", seen.toString());
        // checks expected order, and the + indicates that element had a next sibling at time of emission
    }

    @Test void iterator() {
        // same as stream, just a different interface
        String html = "<title>Test</title></head><div id=1>D1</div><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");
        StringBuilder seen = new StringBuilder();

        Iterator<Element> it = parser.iterator();
        while (it.hasNext()) {
            trackSeen(it.next(), seen);
        }

        assertEquals("title[Test];head+;div#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];body;html;", seen.toString());
        // checks expected order, and the + indicates that element had a next sibling at time of emission
    }

    @Test void canReuse() {
        StreamParser parser = new StreamParser(Parser.htmlParser());
        String html1 = "<p>One<p>Two";
        parser.parse(html1, "");

        StringBuilder seen = new StringBuilder();
        parser.stream().forEach(el -> trackSeen(el, seen));
        assertEquals("head+;p[One]+;p[Two];body;html;", seen.toString());

        String html2 = "<div>Three<div>Four</div></div>";
        StringBuilder seen2 = new StringBuilder();
        parser.parse(html2, "");
        parser.stream().forEach(el -> trackSeen(el, seen2));
        assertEquals("head+;div[Four];div[Three];body;html;", seen2.toString());

        // re-run without a new parse should be empty
        StringBuilder seen3 = new StringBuilder();
        parser.stream().forEach(el -> trackSeen(el, seen3));
        assertEquals("", seen3.toString());
    }

    @Test void canStop() {
        StreamParser parser = new StreamParser(Parser.htmlParser());
        String html1 = "<p>One<p>Two";
        parser.parse(html1, "");

        Optional<Element> p = parser.selectFirst("p");
        assertEquals("One", p.get().text());
        parser.stop();

        Iterator<Element> it = parser.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);

        Optional<Element> p2 = parser.selectNext("p");
        assertFalse(p2.isPresent());

        // can resume
        parser.parse("<div>DIV", "");
        Optional<Element> div = parser.selectFirst("div");
        assertEquals("DIV", div.get().text());
    }

    static void trackSeen(Element el, StringBuilder actual) {
        actual.append(el.tagName());
        if (el.hasAttr("id"))
            actual.append("#").append(el.id());
        if (!el.ownText().isEmpty())
            actual.append("[").append(el.ownText()).append("]");
        if (el.nextElementSibling() != null)
            actual.append("+");

        actual.append(";");
    }

    @Test void select() {
        String html = "<title>One</title><p id=1>P One</p><p id=2>P Two</p>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");

        Element title = parser.selectFirst("title").get();
        assertEquals("One", title.text());

        Document partialDoc = title.ownerDocument();
        // at this point, we should have one P with no text - as title was emitted on P head
        Elements ps = partialDoc.select("p");
        assertEquals(1, ps.size());
        assertEquals("", ps.get(0).text());
        assertSame(partialDoc, parser.document());

        Element title2 = parser.selectFirst("title").get();
        assertSame(title2, title);

        Element p1 = parser.selectNext("p").get();
        assertEquals("P One", p1.text());

        Element p2 = parser.selectNext("p").get();
        assertEquals("P Two", p2.text());

        Optional<Element> pNone = parser.selectNext("p");
        assertFalse(pNone.isPresent());
    }

    @Test void canRemoveFromDom() {
        String html = "<div>One</div><div>DESTROY</div><div>Two</div>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");
        parser.parse(html, "");

        parser.stream().forEach(
            el -> {
                if (el.ownText().equals("DESTROY"))
                    el.remove();
            });

        Document doc = parser.document();
        Elements divs = doc.select("div");
        assertEquals(2, divs.size());
        assertEquals("One Two", divs.text());
    }

    @Test void canRemoveWithIterator() {
        String html = "<div>One</div><div>DESTROY</div><div>Two</div>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");
        parser.parse(html, "");

        Iterator<Element> it = parser.iterator();
        while (it.hasNext()) {
            Element el = it.next();
            if (el.ownText().equals("DESTROY"))
                it.remove(); // we know el.remove() works, from above test
        }

        Document doc = parser.document();
        Elements divs = doc.select("div");
        assertEquals(2, divs.size());
        assertEquals("One Two", divs.text());
    }
}
