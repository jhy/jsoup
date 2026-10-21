package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.jsoup.TestQueryMode.mode;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
class HasEvaluatorTest {
    @Test
    void canonicalRelativeSelectorsRoundTrip() {
        Document doc = Jsoup.parse("<!doctype html><main><div><A></A></div><span class=foo></span></main>");
        for (boolean web : new boolean[]{false, true})
            mode(web, () -> {
                for (String query : new String[]{
                    ":has(> A, SPAN.foo)", ":has(+ span.foo)", ":has(div > A)", ":has(> div + span.foo)"}) {
                    Evaluator evaluator = Selector.evaluatorOf(query);
                    assertEquals(doc.select(evaluator), doc.select(evaluator.toString()), query);
                    assertEquals(evaluator.toString(), Selector.evaluatorOf(evaluator.toString()).toString());
                }
                return null;
            });
    }

    @Test
    void nodeAdjacencyUsesTheWholePrecedingPrefix() {
        Document doc = Jsoup.parse("<main><i></i><!--first-->text<!--second--><i></i>text<b></b></main>");
        Element main = doc.selectFirst("main");
        for (boolean web : new boolean[]{false, true})
            mode(web, () -> {
                assertTrue(main.is(":has(i + ::comment:contains(first))"));
                assertTrue(main.is(":has(i + ::comment:contains(second))"));
                assertFalse(main.is(":has(::comment:contains(first) + ::comment:contains(second))"));
                assertTrue(main.is(":has(::comment:contains(first) + ::text + ::comment:contains(second))"));
                assertFalse(main.is(":has(::comment:contains(first) ~ i + b)"));
                assertTrue(main.is(":has(i + b)")); // an element-only prefix skips intervening text
                return null;
            });
    }

    @Test
    void relativeMatchesSeparateContextsAndSubjects() {
        Document doc = Jsoup.parse("<!doctype html><main><div id=one><p><span></span></p></div><div id=two><p><span></span></p></div></main>");
        HasEvaluator has = mode(true, () -> (HasEvaluator) Selector.evaluatorOf(":has(:scope > span)"));
        Element one = doc.getElementById("one");
        Element two = doc.getElementById("two");
        assertTrue(has.matches(one.child(0), one));
        assertFalse(has.matches(two.child(0), one));
        assertFalse(has.matches(one.child(0), two));
        assertTrue(has.matches(two.child(0), two));
    }

    @Test
    void sharedTraversalMatchesSimpleAndComplexAlternatives() {
        Element main = new Element("main").append("<div><span></span></div>");
        for (boolean web : new boolean[]{false, true})
            mode(web, () -> {
                assertTrue(main.is(":has(missing, > div span)"));
                assertTrue(main.is(":has(span, > missing span)"));
                assertFalse(main.is(":has(missing, > missing span)"));
                return null;
            });
    }

    @Test
    void overlappingSiblingTraversalsKeepSeparateAnchors() {
        mode(true, () -> {
            Document doc = Jsoup.parse("<!doctype html><main><p id=p></p><div id=a></div><div id=b><span></span></div><div id=c></div></main>");
            // p and a search the same span, but only p has the required two intervening divs
            assertEquals(Collections.singletonList("p"), doc.select(":is(p, div):has(+ div + div span)").eachAttr("id"));
            return null;
        });
    }

    @Test
    void customPredicatesReceiveTheHasQueryContext() {
        Element context = new Element("section");
        Element subject = new Element("main").append("<p></p>");
        Evaluator predicate = new Evaluator() {
            @Override
            public boolean matches(Element root, Element element) {
                return root == context;
            }
        };
        for (boolean web : new boolean[]{false, true}) {
            HasEvaluator has = mode(web, () -> new HasEvaluator("p", Collections.singletonList(
                new HasEvaluator.Traversal(new ArrayList<>(Collections.singletonList(predicate)), false, true, false))));
            assertEquals(web, has.matches(context, subject));
        }
    }

    @Test
    void relativeCompoundsMatchProcessingInstructionAttributes() {
        Document xml = Jsoup.parse("<main><div><?go DATA='Mixed'?>text</div></main>", "", Parser.xmlParser());
        Element main = xml.child(0);
        for (boolean web : new boolean[]{false, true})
            mode(web, () -> {
                assertTrue(main.is(":has(div > ::pi(go)[DATA=Mixed])"));
                assertTrue(main.is(":has(> missing, div > ::pi(go)[DATA=Mixed])"));
                assertTrue(main.is(":has(div > ::pi(go)[DATA=Mixed]:not(.missing))"));
                assertFalse(main.is(":has(div > ::pi(go)[DATA=other])"));
                assertFalse(main.is(":has(div > [DATA=Mixed])")); // attributes alone do not admit processing instructions
                assertTrue(main.child(0).is(":has(> ::pi(go)[DATA=Mixed], > ::pi(other)[DATA=Mixed])"));
                return null;
            });
    }

    @Test
    void relativeCompositesUseCustomMatchesOverrides() {
        Element context = new Element("section");
        Element subject = context.appendElement("article");
        Element p = subject.appendElement("p");
        for (boolean web : new boolean[]{false, true})
            mode(web, () -> {
                Evaluator custom = new Evaluator.AttributeKeyPair("missing", "value") {
                    @Override
                    public boolean matches(Element root, Element element) {
                        assertSame(web ? context : subject, root);
                        return element == p;
                    }
                };
                for (Evaluator composite : new Evaluator[]{
                    new CombiningEvaluator.And(custom, new Evaluator.Tag("p")),
                    new CombiningEvaluator.Or(new Evaluator.Tag("missing"), custom)}) {
                    HasEvaluator has = new HasEvaluator("p", Collections.singletonList(
                        new HasEvaluator.Traversal(new ArrayList<>(Collections.singletonList(composite)), false, true, false)));
                    assertTrue(has.matches(context, subject));
                }
                return null;
            });
    }

    @Test
    void earlierMatchesAreCachedOnlyWithinOneTraversal() {
        for (boolean web : new boolean[]{false, true})
            mode(web, () -> {
                Element main = new Element("main").append("<div><span></span><span></span><span></span></div>");
                Element div = main.child(0);
                int[] calls = {0};
                Evaluator prefix = new Evaluator() {
                    @Override
                    public boolean matches(Element root, Element element) {
                        if (element != div) return false;
                        calls[0]++;
                        return element.hasAttr("data-match");
                    }
                };
                Evaluator relative = QueryParser.combinator(
                    QueryParser.combinator(new StructuralEvaluator.RelativeAnchor(), '>', prefix),
                    ' ', new Evaluator.Tag("span"));
                HasEvaluator has = new HasEvaluator("> div span", Collections.singletonList(
                    new HasEvaluator.Traversal(new ArrayList<>(Collections.singletonList(relative)), false, true, false)));

                assertFalse(has.matches(main, main));
                assertEquals(1, calls[0]); // all three candidates share the same earlier match
                assertFalse(has.matches(main, main));
                assertEquals(2, calls[0]);
                div.attr("data-match", "");
                assertTrue(has.matches(main, main));
                assertEquals(3, calls[0]);
                div.removeAttr("data-match");
                assertFalse(has.matches(main, main));
                assertEquals(4, calls[0]);
                return null;
            });
    }

    @Test
    void relativeQueriesKeepNestedQueryRoots() {
        Element main = Jsoup.parse("<!doctype html><main><div><span></span></div></main>").selectFirst("main");
        HasEvaluator has = mode(true, () -> (HasEvaluator) Selector.evaluatorOf(":has(> div:is(> div))"));
        assertTrue(has.matches(main, main));
        assertFalse(has.matches(main.child(0), main));
        assertTrue(main.is(mode(true, () -> Selector.evaluatorOf(":has(> div:has(> span))"))));
    }

    @Test
    void relativeParentRunsRetainNodeEligibilityAndBoundaries() {
        Element main = Jsoup.parse("<!doctype html><main><div>text</div></main>").selectFirst("main");
        for (boolean web : new boolean[]{false, true})
            mode(web, () -> {
                assertTrue(main.is(":has(div > ::text:not(.missing))"));
                assertTrue(main.is(":has(:is(div, ::comment) > :not(.missing))"));
                assertFalse(main.is(":has(div > :not(.missing))"));
                assertFalse(main.is(":has(html > body > main > div)"));
                assertEquals(!web, main.is(":has(main > div)"));
                return null;
            });
    }

    @Test
    void traversalMemosAreClearedAfterAnException() {
        for (boolean web : new boolean[]{false, true})
            mode(web, () -> {
                Element main = new Element("main").append("<div><span></span></div>");
                boolean[] fail = {true};
                Evaluator prefix = new Evaluator() {
                    @Override
                    public boolean matches(Element root, Element element) {
                        if (fail[0]) throw new IllegalStateException("prefix failed");
                        return element.nameIs("div");
                    }
                };
                StructuralEvaluator.Ancestor ancestor = new StructuralEvaluator.Ancestor(prefix);
                Evaluator relative = new CombiningEvaluator.And(ancestor, new Evaluator.Tag("span"));
                HasEvaluator has = new HasEvaluator("div span", Collections.singletonList(
                    new HasEvaluator.Traversal(new ArrayList<>(Collections.singletonList(relative)), false, true, false)));
                assertThrows(IllegalStateException.class, () -> has.matches(main, main));
                assertTrue(ancestor.threadMemo.get().isEmpty());
                fail[0] = false;
                assertTrue(has.matches(main, main));
                assertTrue(ancestor.threadMemo.get().isEmpty());
                return null;
            });
    }

    @Test
    void relativeChainsAreReusableAcrossThreads() {
        Evaluator has = mode(true, () -> Selector.evaluatorOf("div:has(> p + span):not(:has(> b))"));
        CompletableFuture<?>[] tasks = new CompletableFuture<?>[4];
        for (int i = 0; i < tasks.length; i++)
            tasks[i] = CompletableFuture.runAsync(() -> {
                for (int repeat = 0; repeat < 20; repeat++) {
                    Element main = Jsoup.parse("<!doctype html><main><div><p></p><span></span></div><div><p></p></div></main>").selectFirst("main");
                    assertEquals(1, main.select(has).size());
                }
            });
        CompletableFuture.allOf(tasks).join();
    }
}
