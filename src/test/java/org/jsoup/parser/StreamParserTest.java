package org.jsoup.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StreamParserTest {

    @Test
    void stream() {
        String html = "<title>Test</title></head><div>D1</div><div>D2<p>P One</p><p>P Two</p></div><div>D3<p>P three</p>";

        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");

        // todo - actual tests
        parser.stream().forEach(element -> System.out.println(element.nodeName() + ": " + element.ownText() + ", " + element.nextElementSibling()));
    }

    @Test
    void select() {
        String html = "<title>One</title><p>P One</p><p>P Two</p>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");

        Element title = parser.selectFirst("title").get();
        assertEquals("One", title.text());

        Document partialDoc = title.ownerDocument();
        // at this point, we should have one P with no text
        Elements ps = partialDoc.select("p");
        assertEquals(1, ps.size());
        assertEquals("", ps.get(0).text());

        Element title2 = parser.selectFirst("title").get();
        assertSame(title2, title);

        Element p1 = parser.selectNext("p").get();
        assertEquals("P One", p1.text());

        Element p2 = parser.selectNext("p").get();
        assertEquals("P Two", p2.text());

        Optional<Element> pNone = parser.selectNext("p");
        assertFalse(pNone.isPresent());

    }
}