package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jsoup.nodes.NodeIteratorTest.trackSeen;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("<div></div>\n<div>There be</div>", doc.select("body").html());
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
                    if (el.nameIs("i")) {
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

    @Test void elementFunctionalTraverse() {
        Document doc = Jsoup.parse("<div><p>1<p>2<p>3");
        Element body = doc.body();

        AtomicInteger seenCount = new AtomicInteger();
        AtomicInteger deepest = new AtomicInteger();
        body.traverse((node, depth) -> {
            seenCount.incrementAndGet();
            if (depth > deepest.get()) deepest.set(depth);
        });

        assertEquals(8, seenCount.get()); // body and contents
        assertEquals(3, deepest.get());
    }

    @Test void seesDocRoot() {
        Document doc = Jsoup.parse("<p>One");
        AtomicBoolean seen = new AtomicBoolean(false);
        doc.traverse((node, depth) -> {
            if (node.equals(doc))
                seen.set(true);
        });
        assertTrue(seen.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"em", "b"})
    void doesntVisitAgainAfterRemoving(String removeTag) {
        // https://github.com/jhy/jsoup/issues/2355
        Document doc = Jsoup.parse("<div id=1><div><em>first</em><b>last</b></div></div>");
        HashSet<Node> visited = new HashSet<>();
        NodeTraversor.traverse((node, depth) -> {
            if (!visited.add(node))
                fail(String.format("node '%s' is being visited for the second time", node));
            if (removeTag.equals(node.nodeName()))
                node.remove();
        }, doc);
    }


    @Test
    void traversesOnceInOrderAfterRemove() {
        Document doc = Jsoup.parse("<div><p>Text <em>emphasized</em> and <b>bold</b></p></div>");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if ("b".equals(node.nodeName())) // remove the b, causes a cascade of last childs, don't miss any
                    node.remove();
            }

            @Override
            public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, doc);

        // check
        assertEquals("#root;html;head;body;div;p;Text ;em;emphasized; and ;b;", headOrder.toString());
        assertEquals("head;Text ;emphasized;em; and ;p;div;body;html;#root;", tailOrder.toString());
    }
}
