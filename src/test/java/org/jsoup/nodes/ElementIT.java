package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ElementIT {
    @Test
    public void testFastReparent() {
        StringBuilder htmlBuf = new StringBuilder();
        int rows = 300000;
        for (int i = 1; i <= rows; i++) {
            htmlBuf
                .append("<p>El-")
                .append(i)
                .append("</p>");
        }
        String html = htmlBuf.toString();
        Document doc = Jsoup.parse(html);
        long start = System.currentTimeMillis();

        Element wrapper = new Element("div");
        List<Node> childNodes = doc.body().childNodes();
        wrapper.insertChildren(0, childNodes);

        long runtime = System.currentTimeMillis() - start;
        assertEquals(rows, wrapper.childNodes.size());
        assertEquals(rows, childNodes.size()); // child nodes is a wrapper, so still there
        assertEquals(0, doc.body().childNodes().size()); // but on a fresh look, all gone

        doc.body().empty().appendChild(wrapper);
        Element wrapperAcutal = doc.body().children().get(0);
        assertEquals(wrapper, wrapperAcutal);
        assertEquals("El-1", wrapperAcutal.children().get(0).text());
        assertEquals("El-" + rows, wrapperAcutal.children().get(rows - 1).text());
        assertTrue(runtime <= 10000);
    }

    @Test
    public void testFastReparentExistingContent() {
        StringBuilder htmlBuf = new StringBuilder();
        int rows = 300000;
        for (int i = 1; i <= rows; i++) {
            htmlBuf
                .append("<p>El-")
                .append(i)
                .append("</p>");
        }
        String html = htmlBuf.toString();
        Document doc = Jsoup.parse(html);
        long start = System.currentTimeMillis();

        Element wrapper = new Element("div");
        wrapper.append("<p>Prior Content</p>");
        wrapper.append("<p>End Content</p>");
        assertEquals(2, wrapper.childNodes.size());

        List<Node> childNodes = doc.body().childNodes();
        wrapper.insertChildren(1, childNodes);

        long runtime = System.currentTimeMillis() - start;
        assertEquals(rows + 2, wrapper.childNodes.size());
        assertEquals(rows, childNodes.size()); // child nodes is a wrapper, so still there
        assertEquals(0, doc.body().childNodes().size()); // but on a fresh look, all gone

        doc.body().empty().appendChild(wrapper);
        Element wrapperAcutal = doc.body().children().get(0);
        assertEquals(wrapper, wrapperAcutal);
        assertEquals("Prior Content", wrapperAcutal.children().get(0).text());
        assertEquals("El-1", wrapperAcutal.children().get(1).text());

        assertEquals("El-" + rows, wrapperAcutal.children().get(rows).text());
        assertEquals("End Content", wrapperAcutal.children().get(rows + 1).text());

        assertTrue(runtime <= 10000);
    }

    // These overflow tests take a couple seconds to run, so are in the slow tests
    @Test void hasTextNoOverflow() {
        // hasText() was recursive, so could overflow
        Document doc = new Document("https://example.com/");
        Element el = doc.body();
        for (int i = 0; i <= 50000; i++) {
            el = el.appendElement("p");
        }
        assertFalse(doc.hasText());
        el.text("Hello");
        assertTrue(doc.hasText());
        assertEquals(el.text(), doc.text());
    }

    @Test void dataNoOverflow() {
        // data() was recursive, so could overflow
        Document doc = new Document("https://example.com/");
        Element el = doc.body();
        for (int i = 0; i <= 50000; i++) {
            el = el.appendElement("p");
        }
        Element script = el.appendElement("script");
        script.text("script"); // holds data nodes, so inserts as data, not text
        assertFalse(script.hasText());
        assertEquals("script", script.data());
        assertEquals(el.data(), doc.data());
    }

    @Test void parentsNoOverflow() {
        // parents() was recursive, so could overflow
        Document doc = new Document("https://example.com/");
        Element el = doc.body();
        int num = 50000;
        for (int i = 0; i <= num; i++) {
            el = el.appendElement("p");
        }
        Elements parents = el.parents();
        assertEquals(num+2, parents.size()); // +2 for html and body
        assertEquals(doc, el.ownerDocument());
    }

    @Test void wrapNoOverflow() {
        // deepChild was recursive, so could overflow if presented with a fairly insane wrap
        Document doc = new Document("https://example.com/");
        Element el = doc.body().appendElement("p");
        int num = 50000;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= num; i++) {
            sb.append("<div>");
        }
        el.wrap(sb.toString());
        String html = doc.body().html();
        assertTrue(html.startsWith("<div>"));
        assertEquals(num + 3, el.parents().size());
    }
}
