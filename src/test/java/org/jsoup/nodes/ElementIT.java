package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
