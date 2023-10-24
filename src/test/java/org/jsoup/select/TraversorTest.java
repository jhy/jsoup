package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TraversorTest {
    // Note: NodeTraversor.traverse(new NodeVisitor) is tested in
    // ElementsTest#traverse()

    @Test
    public void filterVisit() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>");
        final StringBuilder accum = new StringBuilder();
        NodeTraversor.filter(new NodeFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                accum.append("<").append(node.nodeName()).append(">");
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                accum.append("</").append(node.nodeName()).append(">");
                return FilterResult.CONTINUE;
            }
        }, doc.select("div"));
        assertEquals("<div><p><#text></#text></p></div><div><#text></#text></div>", accum.toString());
    }

    @Test
    public void filterSkipChildren() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>");
        final StringBuilder accum = new StringBuilder();
        NodeTraversor.filter(new NodeFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                accum.append("<").append(node.nodeName()).append(">");
                // OMIT contents of p:
                return ("p".equals(node.nodeName())) ? FilterResult.SKIP_CHILDREN : FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                accum.append("</").append(node.nodeName()).append(">");
                return FilterResult.CONTINUE;
            }
        }, doc.select("div"));
        assertEquals("<div><p></p></div><div><#text></#text></div>", accum.toString());
    }

    @Test
    public void filterSkipEntirely() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>");
        final StringBuilder accum = new StringBuilder();
        NodeTraversor.filter(new NodeFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                // OMIT p:
                if ("p".equals(node.nodeName()))
                    return FilterResult.SKIP_ENTIRELY;
                accum.append("<").append(node.nodeName()).append(">");
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                accum.append("</").append(node.nodeName()).append(">");
                return FilterResult.CONTINUE;
            }
        }, doc.select("div"));
        assertEquals("<div></div><div><#text></#text></div>", accum.toString());
    }

    @Test
    public void filterRemove() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div>There be <b>bold</b></div>");
        NodeTraversor.filter(new NodeFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                // Delete "p" in head:
                return ("p".equals(node.nodeName())) ? FilterResult.REMOVE : FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                // Delete "b" in tail:
                return ("b".equals(node.nodeName())) ? FilterResult.REMOVE : FilterResult.CONTINUE;
            }
        }, doc.select("div"));
        assertEquals("<div></div>\n<div>\n There be\n</div>", doc.select("body").html());
    }

    @Test
    public void filterStop() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>");
        final StringBuilder accum = new StringBuilder();
        NodeTraversor.filter(new NodeFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                accum.append("<").append(node.nodeName()).append(">");
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                accum.append("</").append(node.nodeName()).append(">");
                // Stop after p.
                return ("p".equals(node.nodeName())) ? FilterResult.STOP : FilterResult.CONTINUE;
            }
        }, doc.select("div"));
        assertEquals("<div><p><#text></#text></p>", accum.toString());
    }

    @Test public void replaceElement() {
        // https://github.com/jhy/jsoup/issues/1289
        // test we can replace an element during traversal
        String html = "<div><p>One <i>two</i> <i>three</i> four.</p></div>";
        Document doc = Jsoup.parse(html);

        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof Element) {
                    Element el = (Element) node;
                    if (el.normalName().equals("i")) {
                        Element u = new Element("u").insertChildren(0, el.childNodes());
                        el.replaceWith(u);
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {}
        }, doc);

        Element p = doc.selectFirst("p");
        assertNotNull(p);
        assertEquals("<p>One <u>two</u> <u>three</u> four.</p>", p.outerHtml());
    }

    @Test public void canAddChildren() {
        Document doc = Jsoup.parse("<div><p></p><p></p></div>");

        NodeTraversor.traverse(new NodeVisitor() {
            int i = 0;
            @Override
            public void head(Node node, int depth) {
                if (node.nodeName().equals("p")) {
                    Element p = (Element) node;
                    p.append("<span>" + i++ + "</span>");
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (node.nodeName().equals("p")) {
                    Element p = (Element) node;
                    p.append("<span>" + i++ + "</span>");
                }
            }
        }, doc);

        assertEquals("<div>\n" +
            " <p><span>0</span><span>1</span></p>\n" +
            " <p><span>2</span><span>3</span></p>\n" +
            "</div>", doc.body().html());
    }

    @Test public void canSpecifyOnlyHead() {
        // really, a compilation test - works as a lambda if just head
        Document doc = Jsoup.parse("<div><p>One</p></div>");
        final int[] count = {0};
        NodeTraversor.traverse((node, depth) -> count[0]++, doc);
        assertEquals(7, count[0]);
    }

    @Test public void canRemoveDuringHead() {
        Document doc = Jsoup.parse("<div><p id=1>Zero<p id=1>One<p id=2>Two<p>Three</div>");
        NodeTraversor.traverse((node, depth) -> {
            if (node.attr("id").equals("1"))
                node.remove();
            else if (node instanceof TextNode && ((TextNode) node).text().equals("Three"))
                node.remove();
        }, doc);

        assertEquals("<div><p id=\"2\">Two</p><p></p></div>", TextUtil.stripNewlines(doc.body().html()));
    }
}
