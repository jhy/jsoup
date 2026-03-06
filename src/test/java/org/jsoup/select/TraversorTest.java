package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    @Test
    void replacementInHeadCanTailReplacementAndVisitChildren() {
        // if head() replaces the current node, the replacement should be tailed and its children still traversed
        Document doc = Jsoup.parseBodyFragment("<div><i>two</i></div>");
        Element div = doc.expectFirst("div");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if ("i".equals(node.nodeName())) {
                    Element replacement = new Element("u").insertChildren(0, node.childNodes());
                    node.replaceWith(replacement);
                }
            }

            @Override
            public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, div);

        assertEquals("div;i;two;", headOrder.toString());
        assertEquals("two;u;div;", tailOrder.toString());
        assertEquals("<div><u>two</u></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @ParameterizedTest
    @EnumSource(CurrentPositionMutation.class)
    void supportsCurrentNodeMutationDuringHead(CurrentPositionMutation mutation) {
        // supported head() mutations at the current cursor should terminate cleanly and continue traversal in order
        // https://github.com/jhy/jsoup/issues/2472
        Document doc = Jsoup.parse("<div><p>One</p>a<em>Two</em></div>");
        AtomicInteger visits = new AtomicInteger();
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                if (visits.incrementAndGet() > MaxHeadVisits)
                    fail(String.format("Likely loop when applying %s in head()", mutation));
                trackSeen(node, headOrder);
                if (node instanceof TextNode && ((TextNode) node).text().equals("a"))
                    mutation.apply(node);
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, doc);

        assertEquals("#root;html;head;body;div;p;One;a;em;Two;", headOrder.toString());
        assertEquals(mutation.expectedTailOrder, tailOrder.toString());
        assertEquals(mutation.expectedHtml, TextUtil.stripNewlines(doc.body().html()));
    }

    private static final int MaxHeadVisits = 20;

    private enum CurrentPositionMutation {
        Remove("head;One;p;Two;em;div;body;html;#root;", "<div><p>One</p><em>Two</em></div>") {
            @Override void apply(Node node) {
                node.remove();
            }
        },
        Replace("head;One;p;b;Two;em;div;body;html;#root;", "<div><p>One</p>b<em>Two</em></div>") {
            @Override void apply(Node node) {
                node.replaceWith(new TextNode("b"));
            }
        },
        BeforeRemove("head;One;p;b;Two;em;div;body;html;#root;", "<div><p>One</p>b<em>Two</em></div>") {
            @Override void apply(Node node) {
                node.before(new TextNode("b"));
                node.remove();
            }
        },
        AfterRemove("head;One;p;b;Two;em;div;body;html;#root;", "<div><p>One</p>b<em>Two</em></div>") {
            @Override void apply(Node node) {
                node.after(new TextNode("b"));
                node.remove();
            }
        };

        final String expectedTailOrder;
        final String expectedHtml;

        CurrentPositionMutation(String expectedTailOrder, String expectedHtml) {
            this.expectedTailOrder = expectedTailOrder;
            this.expectedHtml = expectedHtml;
        }

        abstract void apply(Node node);
    }

    @ParameterizedTest
    @EnumSource(SiblingInsertion.class)
    void siblingInsertionsOnlyVisitFutureNodesDuringHead(SiblingInsertion insertion) {
        // nodes inserted before the current cursor are not visited; nodes inserted after it are still ahead and are
        Document doc = Jsoup.parseBodyFragment("<div>a</div>");
        StringBuilder headTexts = new StringBuilder();
        StringBuilder tailTexts = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                if (node instanceof TextNode) {
                    trackSeen(node, headTexts);
                    insertion.apply((TextNode) node);
                }
            }

            @Override public void tail(Node node, int depth) {
                if (node instanceof TextNode)
                    trackSeen(node, tailTexts);
            }
        }, doc);

        assertEquals(insertion.expectedTexts, headTexts.toString());
        assertEquals(insertion.expectedTexts, tailTexts.toString());
        assertEquals(insertion.expectedHtml, TextUtil.stripNewlines(doc.body().html()));
    }

    private enum SiblingInsertion {
        Before("a;", "<div>ba</div>") {
            @Override void apply(TextNode node) {
                if (node.text().equals("a"))
                    node.before(new TextNode("b"));
            }
        },
        After("a;b;", "<div>ab</div>") {
            @Override void apply(TextNode node) {
                if (node.text().equals("a"))
                    node.after(new TextNode("b"));
            }
        };

        final String expectedTexts;
        final String expectedHtml;

        SiblingInsertion(String expectedTexts, String expectedHtml) {
            this.expectedTexts = expectedTexts;
            this.expectedHtml = expectedHtml;
        }

        abstract void apply(TextNode node);
    }

    @Test
    void visitsChildrenInsertedInHead() {
        // when the current node remains attached, children inserted in head() are visited in the same traversal
        Document doc = Jsoup.parseBodyFragment("<div><p></p></div>");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if (node instanceof Element && node.nameIs("p"))
                    ((Element) node).append("<span>child</span>");
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, doc.body());

        assertEquals("body;div;p;span;child;", headOrder.toString());
        assertEquals("child;span;p;div;body;", tailOrder.toString());
        assertEquals("<div><p><span>child</span></p></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    void removingTraversalRootInHeadDoesNotEscapeOriginalSubtree() {
        Document doc = Jsoup.parseBodyFragment("<div>a</div><p>b</p>");
        Element root = doc.expectFirst("div");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if (node == root)
                    node.remove();
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, root);

        assertEquals("div;", headOrder.toString());
        assertEquals("", tailOrder.toString());
        assertEquals("<p>b</p>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    void removingOnlyTraversalRootInHeadStopsWhenOriginalSlotIsEmpty() {
        Document doc = Jsoup.parseBodyFragment("<div>a</div>");
        Element root = doc.expectFirst("div");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if (node == root)
                    node.remove();
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, root);

        assertEquals("div;", headOrder.toString());
        assertEquals("", tailOrder.toString());
        assertEquals("", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    void replacingTraversalRootInHeadStaysWithinReplacementSubtree() {
        Document doc = Jsoup.parseBodyFragment("<div><span>x</span></div><p>y</p>");
        Element root = doc.expectFirst("div");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if (node == root) {
                    Element replacement = new Element("section").insertChildren(0, node.childNodes());
                    node.replaceWith(replacement);
                }
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, root);

        assertEquals("div;span;x;", headOrder.toString());
        assertEquals("x;span;section;", tailOrder.toString());
        assertEquals("<section><span>x</span></section><p>y</p>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    void unwrappingTraversalRootInHeadVisitsExposedTopLevelNodesUntilOriginalBoundary() {
        Document doc = Jsoup.parseBodyFragment("<div><b>x</b><i>y</i></div><p>z</p>");
        Element root = doc.expectFirst("div");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if (node == root)
                    node.unwrap();
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, root);

        assertEquals("div;x;i;y;", headOrder.toString());
        assertEquals("x;b;y;i;", tailOrder.toString());
        assertEquals("<b>x</b><i>y</i><p>z</p>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    void unwrapInHeadContinuesFromExposedChildren() {
        Document doc = Jsoup.parseBodyFragment("<div><span><b>x</b><i>y</i></span><u>z</u></div>");
        Element root = doc.expectFirst("div");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if (node instanceof Element && node.nameIs("span"))
                    node.unwrap();
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, root);

        assertEquals("div;span;x;i;y;u;z;", headOrder.toString());
        assertEquals("x;b;y;i;z;u;div;", tailOrder.toString());
        assertEquals("<div><b>x</b><i>y</i><u>z</u></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    void removingCurrentAndOriginalNextInHeadTailsParent() {
        Document doc = Jsoup.parseBodyFragment("<div>a<em>Two</em></div>");
        Element root = doc.expectFirst("div");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if (node instanceof TextNode && ((TextNode) node).text().equals("a")) {
                    node.nextSibling().remove();
                    node.remove();
                }
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, root);

        assertEquals("div;a;", headOrder.toString());
        assertEquals("div;", tailOrder.toString());
        assertEquals("<div></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    void beforeAfterRemoveInHeadTailsCurrentSlotAndHeadsFutureSiblings() {
        Document doc = Jsoup.parse("<div><p>One</p>a<em>Two</em></div>");
        StringBuilder headOrder = new StringBuilder();
        StringBuilder tailOrder = new StringBuilder();

        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                trackSeen(node, headOrder);
                if (node instanceof TextNode && ((TextNode) node).text().equals("a")) {
                    node.before(new TextNode("b"));
                    node.after(new TextNode("c"));
                    node.remove();
                }
            }

            @Override public void tail(Node node, int depth) {
                trackSeen(node, tailOrder);
            }
        }, doc);

        assertEquals("#root;html;head;body;div;p;One;a;c;em;Two;", headOrder.toString());
        assertEquals("head;One;p;b;c;Two;em;div;body;html;#root;", tailOrder.toString());
        assertEquals("<div><p>One</p>bc<em>Two</em></div>", TextUtil.stripNewlines(doc.body().html()));
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
