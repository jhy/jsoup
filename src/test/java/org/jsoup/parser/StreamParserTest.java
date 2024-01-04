package org.jsoup.parser;

import org.jsoup.helper.DataUtil;
import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 Tests for the StreamParser. There are also some tests in {@link org.jsoup.integration.ConnectTest}.
 */
class StreamParserTest {

    @Test
    void canStream() {
        String html = "<title>Test</title></head><div id=1>D1</div><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            StringBuilder seen;
            seen = new StringBuilder();
            parser.stream().forEachOrdered(el -> trackSeen(el, seen));
            assertEquals("title[Test];head+;div#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];body;html;", seen.toString());
            // checks expected order, and the + indicates that element had a next sibling at time of emission
        }
    }

    @Test void canIterate() {
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

    @Test void canStopAndCompleteAndReuse() {
        StreamParser parser = new StreamParser(Parser.htmlParser());
        String html1 = "<p>One<p>Two";
        parser.parse(html1, "");

        Element p = parser.expectFirst("p");
        assertEquals("One", p.text());
        parser.stop();

        Iterator<Element> it = parser.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);

        Element p2 = parser.selectNext("p");
        assertNull(p2);

        Document completed = parser.complete();
        Elements ps = completed.select("p");
        assertEquals(2, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Two", ps.get(1).text());

        // can reuse
        parser.parse("<div>DIV", "");
        Element div = parser.expectFirst("div");
        assertEquals("DIV", div.text());
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

        Element title = parser.expectFirst("title");
        assertEquals("One", title.text());

        Document partialDoc = title.ownerDocument();
        assertNotNull(partialDoc);
        // at this point, we should have one P with no text - as title was emitted on P head
        Elements ps = partialDoc.select("p");
        assertEquals(1, ps.size());
        assertEquals("", ps.get(0).text());
        assertSame(partialDoc, parser.document());

        Element title2 = parser.selectFirst("title");
        assertSame(title2, title);

        Element p1 = parser.expectNext("p");
        assertEquals("P One", p1.text());

        Element p2 = parser.expectNext("p");
        assertEquals("P Two", p2.text());

        Element pNone = parser.selectNext("p");
        assertNull(pNone);
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

    @Test void canSelectWithHas() {
        StreamParser parser = basic();

        Element el = parser.expectNext("div:has(p)");
        assertEquals("Two", el.text());
    }

    @Test void canSelectWithSibling() {
        StreamParser parser = basic();

        Element el = parser.expectNext("div:first-of-type");
        assertEquals("One", el.text());

        Element el2 = parser.selectNext("div:first-of-type");
        assertNull(el2);
    }

    @Test void canLoopOnSelectNext() {
        StreamParser streamer = new StreamParser(Parser.htmlParser()).parse("<div><p>One<p>Two<p>Thr</div>", "");

        int count = 0;
        Element e;
        while ((e = streamer.selectNext("p")) != null) {
            assertEquals(3, e.text().length()); // has a body
            e.remove();
            count++;
        }

        assertEquals(3, count);
        assertEquals(0, streamer.document().select("p").size()); // removed all during iter

        assertTrue(isClosed(streamer)); // read to the end
    }

    @Test void worksWithXmlParser() {
        StreamParser streamer = new StreamParser(Parser.xmlParser()).parse("<div><p>One</p><p>Two</p><p>Thr</p></div>", "");

        int count = 0;
        Element e;
        while ((e = streamer.selectNext("p")) != null) {
            assertEquals(3, e.text().length()); // has a body
            e.remove();
            count++;
        }

        assertEquals(3, count);
        assertEquals(0, streamer.document().select("p").size()); // removed all during iter

        assertTrue(isClosed(streamer)); // read to the end
    }

    @Test void closedOnStreamDrained() {
        StreamParser streamer = basic();
        assertFalse(isClosed(streamer));
        long count = streamer.stream().count();
        assertEquals(6, count);

        assertTrue(isClosed(streamer));
    }

    @Test void closedOnIteratorDrained() {
        StreamParser streamer = basic();

        int count = 0;
        Iterator<Element> it = streamer.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(6, count);
        assertTrue(isClosed(streamer));
    }

    @Test void closedOnComplete() {
        StreamParser streamer = basic();
        Document doc = streamer.complete();
        assertTrue(isClosed(streamer));
    }

    @Test void closedOnTryWithResources() {
        StreamParser copy;
        try(StreamParser streamer = basic()) {
            copy = streamer;
            assertFalse(isClosed(copy));
        }
        assertTrue(isClosed(copy));
    }

    static StreamParser basic() {
        String html = "<div>One</div><div><p>Two</div>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");
        parser.parse(html, "");
        return parser;
    }

    static boolean isClosed(StreamParser streamer) {
        // a bit of a back door in!
        return getReader(streamer) == null;
    }

     private static CharacterReader getReader(StreamParser streamer) {
        return streamer.document().parser().getTreeBuilder().reader;
    }

    @Test void doesNotReadPastParse() {
        StreamParser streamer = basic();
        Element div = streamer.expectFirst("div");

        // we should have read the sibling div, but not yet its children p
        Element sib = div.nextElementSibling();
        assertNotNull(sib);
        assertEquals("div", sib.tagName());
        assertEquals(0, sib.childNodeSize());

        // the Reader should be at "<p>" because we haven't consumed it
        assertTrue(getReader(streamer).matches("<p>Two"));
    }

    @Test void canParseFileReader() throws IOException {
        File file = ParseTest.getFile("/htmltests/large.html");

        // can't use FileReader from Java 11 here
        InputStreamReader input = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(input);
        StreamParser streamer = new StreamParser(Parser.htmlParser()).parse(reader, file.getAbsolutePath());

        Element last = null, e;
        while ((e = streamer.selectNext("p")) != null) {
            last = e;
        }
        assertTrue(last.text().startsWith("VESTIBULUM"));

        // the reader should be closed as streamer is closed on completion of read
        assertTrue(isClosed(streamer));

        assertThrows(IOException.class, reader::ready); // ready() checks isOpen and throws
    }

    @Test void canParseFile() throws IOException {
        File file = ParseTest.getFile("/htmltests/large.html");
        StreamParser streamer = DataUtil.streamParser(file.toPath(), StandardCharsets.UTF_8, "", Parser.htmlParser());

        Element last = null, e;
        while ((e = streamer.selectNext("p")) != null) {
            last = e;
        }
        assertTrue(last.text().startsWith("VESTIBULUM"));

        // the reader should be closed as streamer is closed on completion of read
        assertTrue(isClosed(streamer));
    }
}
