package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests for HtmlShrinker.
 *
 */
public class ShrinkTest {
    @Test
    public void shrinkTest(){
        String A = "AB\\nCD\nADBCAB\n\n\n";
        String B = "\n";
        assertEquals("AB\\nCDADBCAB",HtmlShrinker.htmlShrink(A,B));
    }
    @Test
    public void withoutShrinkTest(){
        String html = "<html>\n" +
                "<head>\n" +
                "<title>Example - Text Formating Tags</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "test\n" +
                "</body>\n" +
                "</html>";
        final StringBuilder accum = new StringBuilder();
        final Document doc = Jsoup.parse(html);
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                accum.append("<").append(node.nodeName()).append(">");
                return;
            }

            @Override
            public void tail(Node node, int depth) {
                accum.append("</").append(node.nodeName()).append(">");
                return;
            }
        }, doc.root());

        assertEquals("<#document><html><#text></#text><head><#text></#text><title>" +
                "<#text></#text></title><#text></#text></head><#text></#text><body><#text>" +
                "</#text><#text></#text></body></html></#document>",accum.toString());
    }
    @Test
    public void shrinkHtmlTest() {
        String html = "<html>\n" +
                "<head>\n" +
                "<title>Example - Text Formating Tags</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "test\n" +
                "</body>\n" +
                "</html>";
        html = HtmlShrinker.htmlShrink(html,"\n");
        assertEquals("<html><head><title>Example - Text Formating Tags</title></head><body>test</body>" +
                "</html>",html);
        final StringBuilder accum = new StringBuilder();
        final Document doc = Jsoup.parse(html);
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                accum.append("<").append(node.nodeName()).append(">");
                return;
            }

            @Override
            public void tail(Node node, int depth) {
                accum.append("</").append(node.nodeName()).append(">");
                return;
            }
        }, doc.root());
        assertEquals("<#document><html><head><title><#text></#text>" +
                "</title></head><body><#text></#text></body></html></#document>",accum.toString());
    }

}
