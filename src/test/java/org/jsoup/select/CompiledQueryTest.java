package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.internal.QueryMode;
import org.jsoup.internal.SharedConstants;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.ProcessingInstruction;
import org.jsoup.parser.Parser;
import org.jsoup.parser.StreamParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.StringReader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.jsoup.TestQueryMode.mode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
class CompiledQueryTest {
    @Test void compilationModeRestoresEnclosingMode() {
        for (boolean web : new boolean[]{false, true}) mode(!web, () -> {
            Evaluator.withMode(web, () -> {
                assertEquals(web, new Evaluator.Tag("Thing").web);
                Evaluator.withMode(!web, () -> {
                    assertEquals(!web, new Evaluator.Tag("Thing").web);
                    return null;
                });
                assertEquals(web, new Evaluator.Tag("Thing").web);
                assertThrows(IllegalStateException.class, () -> Evaluator.withMode(!web, () -> {
                    throw new IllegalStateException();
                }));
                assertEquals(web, new Evaluator.Tag("Thing").web);
                assertThrows(Selector.SelectorParseException.class, () -> QueryParser.parse("[", !web));
                assertEquals(web, new Evaluator.Tag("Thing").web);
                assertEquals(!web, QueryMode.current() == QueryMode.Web); // the application mode stays independent
                return null;
            });
            assertEquals(!web, new Evaluator.Tag("Thing").web);
            assertThrows(Selector.SelectorParseException.class, () -> QueryParser.parse("[", web));
            assertEquals(!web, new Evaluator.Tag("Thing").web);
            return null;
        });
    }

    @Test void explicitCompilationCapturesTheWholeQuery() {
        Document xml = Jsoup.parse("<root><Thing DATA='Mixed' class='Mixed'><Child/></Thing>" +
            "<thing data='mixed' class='mixed'><child/></thing></root>", "", Parser.xmlParser());
        for (String query : new String[]{"Thing", ".Mixed", "[DATA=Mixed]", "[^DA]", "*|Thing",
            "Thing > Child", "Thing:has(> Child)", ":is(Thing):not(.missing)", "Thing:nth-of-type(1)",
            "[DATA=Mixed i]", "::node[DATA=Mixed]"}) {
            for (boolean web : new boolean[]{false, true}) {
                Elements expected = mode(web, () -> xml.select(query));
                mode(!web, () -> {
                    Evaluator compiled = QueryParser.parse(query, web);
                    assertEquals(web, compiled.web, query);
                    assertEquals(expected, xml.select(compiled), query);
                    assertEquals(!web, new Evaluator.Tag("Thing").web);
                    return null;
                });
            }
        }
    }

    @Test void concurrentCompilationsKeepTheirOwnModes() {
        mode(false, () -> {
            Document xml = Jsoup.parse("<root><Thing DATA='Mixed'/><thing data='mixed'/></root>", "", Parser.xmlParser());
            CompletableFuture<?>[] tasks = new CompletableFuture<?>[16];
            for (int i = 0; i < tasks.length; i++) {
                boolean web = i % 2 == 0;
                tasks[i] = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < 40; j++) {
                        Evaluator compiled = QueryParser.parse("root:has(Thing) > :is(Thing)[DATA=Mixed]", web);
                        assertEquals(web ? 1 : 2, xml.select(compiled).size());
                        assertFalse(new Evaluator.Tag("Thing").web);
                    }
                });
            }
            CompletableFuture.allOf(tasks).join();
            return null;
        });
    }

    @Test void compilationOverrideDoesNotChangeCheckMode() {
        mode(false, () -> {
            System.setProperty(SharedConstants.QueryMode, "check");
            assertFalse(new Evaluator.Tag("Thing").web);
            assertTrue(QueryParser.parse("Thing", true).web);
            assertEquals(QueryMode.Check, QueryMode.current());
            assertFalse(new Evaluator.Tag("Thing").web);
            return null;
        });
    }

    @Test void childrenRetainTheirCapturedMatchingModes() {
        Document xml = Jsoup.parse("<root><Thing DATA='Mixed' class='Mixed'/><thing data='mixed' class='mixed'/></root>", "", Parser.xmlParser());
        for (String query : new String[]{"Thing", ".Mixed", "[DATA=Mixed]", "[DATA]", "[^DA]", "*|Thing"}) {
            Evaluator legacy = mode(false, () -> Selector.evaluatorOf(query));
            Evaluator web = mode(true, () -> Selector.evaluatorOf(query));
            assertEquals(2, xml.select(mode(true, () -> new CombiningEvaluator.And(legacy))).size(), query);
            assertEquals(new Elements(xml.child(0).child(0)),
                xml.select(mode(false, () -> new CombiningEvaluator.And(web))), query);
        }
    }

    @Test void siblingTypeChildrenRetainTheirCapturedModes() {
        Document xml = Jsoup.parse("<root><Thing/><thing/></root>", "", Parser.xmlParser());
        for (String query : new String[]{":only-of-type", ":first-of-type", ":last-of-type", ":nth-of-type(1)", ":nth-last-of-type(1)"}) {
            Evaluator legacy = mode(false, () -> Selector.evaluatorOf(query));
            Evaluator web = mode(true, () -> Selector.evaluatorOf(query));
            assertEquals(query.equals(":only-of-type") ? 0 : 1,
                xml.child(0).select(mode(true, () -> new CombiningEvaluator.And(legacy))).size(), query);
            assertEquals(2, xml.child(0).select(mode(false, () -> new CombiningEvaluator.And(web))).size(), query);
        }
    }

    @Test void compiledModeSurvivesPropertyChangesAndLazyTraversal() {
        Document doc = Jsoup.parse("<!doctype html><p class='Mixed' data-x='Mixed'></p><p class='mixed' data-x='mixed'></p>");
        Evaluator web = mode(true, () -> Selector.evaluatorOf("p.Mixed[data-x=Mixed]"));
        Stream<Element> results = mode(false, () -> Collector.stream(web, doc));
        assertEquals(1, mode(false, results::count));
        assertEquals(1, mode(false, () -> doc.select(web).size()));
        assertEquals(web.toString(), mode(false, web::toString));
    }

    @Test void processingInstructionChildrenRetainTheirModes() {
        Document xml = Jsoup.parse("<root><?go DATA='Mixed'?><?go DATA='mixed'?></root>", "", Parser.xmlParser());
        Evaluator legacy = mode(false, () -> Selector.evaluatorOf("::pi(go)[DATA=Mixed]"));
        Evaluator web = mode(true, () -> new CombiningEvaluator.And(legacy));
        assertEquals(2, xml.selectNodes(web, ProcessingInstruction.class).size());
        assertEquals(2, xml.selectNodes(mode(false, () -> new CombiningEvaluator.And(web)), ProcessingInstruction.class).size());
    }

    @Test void compiledRegexKeepsItsModeWhenComposed() {
        Document doc = Jsoup.parse("<!doctype html><p class='card featured'></p><p class='cardinal'></p>");
        Evaluator regex = mode(false, () -> Selector.evaluatorOf("[class~=card]"));
        Evaluator web = mode(true, () -> new CombiningEvaluator.And(regex));
        assertEquals(2, doc.select(web).size());
        assertEquals("[class~=card]", web.toString());
        assertEquals(2, mode(false, () -> doc.select(web.toString()).size()));
    }

    @Test void quirksStateIsReadWhenThePredicateRuns() {
        Document doc = Jsoup.parse("<!doctype html><p class='Mixed' id='Mixed'></p>");
        Evaluator web = mode(true, () -> Selector.evaluatorOf(".mixed#mixed"));
        Stream<Element> results = Collector.stream(web, doc);
        doc.quirksMode(Document.QuirksMode.quirks);
        assertEquals(1, results.count());
        doc.quirksMode(Document.QuirksMode.noQuirks);
        assertTrue(doc.select(web).isEmpty());
    }

    @Test void customAttributePredicatesRemainActiveInComposites() {
        Document doc = Jsoup.parse("<p></p>");
        Evaluator custom = new Evaluator.AttributeKeyPair("missing", "value") {
            @Override public boolean matches(Element root, Element element) { return element.nameIs("p"); }
        };
        for (boolean web : new boolean[]{false, true}) {
            assertTrue(doc.selectFirst("p").is(mode(web, () -> new CombiningEvaluator.And(custom))));
            assertEquals(1, doc.select(mode(web, () -> new CombiningEvaluator.And(custom))).size());
        }
    }

    @Test void directMatchingUsesMatchesRatherThanAnOverriddenPredicate() {
        for (boolean web : new boolean[]{false, true}) mode(web, () -> {
            Evaluator custom = new Evaluator() {
                @Override public boolean matches(Element root, Element element) { return true; }
                @Override public Predicate<Element> asPredicate(Element root) { return element -> false; }
            };
            Element root = new Element("div");
            root.appendElement("p");
            assertTrue(root.is(custom));
            assertSame(root, root.closest(custom));
            assertTrue(root.select(custom).isEmpty());
            return null;
        });
    }

    @Test void customNthHooksRemainActiveInComposites() {
        Document doc = Jsoup.parse("<p></p>");
        Evaluator position = new Evaluator.IsNthOfType(0, 7) {
            @Override protected int calculatePosition(Element root, Element element) { return 7; }
        };
        Evaluator predicate = new Evaluator.IsNthOfType(0, 1) {
            @Override public boolean matches(Element root, Element element) { return false; }
        };
        for (boolean web : new boolean[]{false, true}) {
            assertTrue(doc.selectFirst("p").is(mode(web, () -> new CombiningEvaluator.And(position))));
            assertFalse(doc.selectFirst("p").is(mode(web, () -> new CombiningEvaluator.And(predicate))));
        }
    }

    @Test void compiledContextsAreReusableAcrossDocumentsAndThreads() {
        Evaluator web = mode(true, () -> Selector.evaluatorOf("Thing[DATA=Mixed]:only-of-type"));
        CompletableFuture<?>[] tasks = new CompletableFuture<?>[4];
        for (int i = 0; i < tasks.length; i++) tasks[i] = CompletableFuture.runAsync(() -> {
            for (int repeat = 0; repeat < 20; repeat++) {
                Document xml = Jsoup.parse("<root><Thing DATA='Mixed'/><thing DATA='mixed'/></root>", "", Parser.xmlParser());
                assertEquals(1, xml.select(web).size());
                Document html = Jsoup.parse("<!doctype html><Thing DATA='Mixed'></Thing>");
                assertEquals(1, html.select(web).size());
            }
        });
        CompletableFuture.allOf(tasks).join();
    }

    @Test void compiledQueriesKeepTheirMode() {
        Element outer = Jsoup.parse("<span id=outer><span id=inner></span></span>").getElementById("outer");
        Evaluator legacy = mode(false, () -> Selector.evaluatorOf("span"));
        Evaluator web = mode(true, () -> Selector.evaluatorOf("span"));
        assertSame(outer, outer.selectFirst(legacy));
        assertEquals("inner", outer.selectFirst(web).id());
        assertEquals(2, outer.select(legacy).size());
        assertEquals(1, outer.select(web).size());
        assertEquals(2, Collector.collect(legacy, outer).size());
        assertEquals(1, Collector.collect(web, outer).size());
        Stream<Element> legacyStream = outer.selectStream(legacy);
        Stream<Element> webStream = outer.selectStream(web);
        assertSame(outer, mode(true, () -> legacyStream.findFirst().get()));
        assertEquals("inner", mode(false, () -> webStream.findFirst().get().id()));
        assertEquals(2, outer.stream().filter(web.asPredicate(outer)).count()); // intentional inclusive search
        assertTrue(outer.is(legacy));
        assertTrue(outer.is(web));
        assertSame(outer, outer.closest(web));
    }

    @Test void customEvaluatorsAndMixedCompositesKeepChildPredicates() {
        Evaluator custom = mode(true, () -> new Evaluator() {
            @Override public boolean matches(Element root, Element element) { return element.nameIs("p"); }
        });
        Element outer = new Element("p").addClass("Mixed").append("<p id=inner class=Mixed>");
        assertEquals(1, outer.select(custom).size());
        assertTrue(outer.is(custom));
        Evaluator legacyClass = mode(false, () -> Selector.evaluatorOf(".mixed"));
        Evaluator composite = mode(true, () -> new CombiningEvaluator.And(legacyClass));
        assertTrue(outer.child(0).is(composite));
        assertTrue(outer.is(composite));
    }

    @Test void compiledQueriesAreReusableAcrossScopesAndThreads() {
        Evaluator evaluator = mode(true, () -> Selector.evaluatorOf("body div:has(> p):not(.skip)"));
        CompletableFuture<?>[] futures = new CompletableFuture<?>[8];
        for (int i = 0; i < futures.length; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int repeat = 0; repeat < 20; repeat++) {
                    Document doc = Jsoup.parse("<div><p></p></div>");
                    assertEquals(1, doc.select(evaluator).size());
                    assertTrue(doc.child(0).select(evaluator).contains(doc.body().child(0)));
                    doc.body().child(0).addClass("skip");
                    assertTrue(doc.select(evaluator).isEmpty());
                }
            });
        }
        CompletableFuture.allOf(futures).join();
    }

    @Test void compiledModesAndFlagsSurvivePropertyChangesAndReuse() {
        Evaluator legacy = mode(false, () -> Selector.evaluatorOf(".mixed[data-x=mixed]"));
        Evaluator web = mode(true, () -> Selector.evaluatorOf(".mixed[data-x=mixed]"));
        Evaluator insensitive = mode(true, () -> Selector.evaluatorOf("[class~=mixed i][data-x=mixed i]"));
        mode(false, () -> {
            Document doc = Jsoup.parse("<!doctype html><p class='Mixed' data-x='Mixed'></p>");
            assertEquals(1, doc.select(legacy).size());
            assertEquals(0, doc.select(web).size());
            assertEquals(1, doc.select(insensitive).size());
            doc.selectFirst("p").attr("class", "mixed").attr("data-x", "mixed");
            assertEquals(1, doc.select(web).size());
            CompletableFuture<?>[] checks = new CompletableFuture<?>[4];
            for (int i = 0; i < checks.length; i++) checks[i] = CompletableFuture.runAsync(() -> {
                Document other = Jsoup.parse("<!doctype html><p class='Mixed' data-x='Mixed'></p>");
                assertEquals(0, other.select(web).size());
                assertEquals(1, other.select(insensitive).size());
            });
            CompletableFuture.allOf(checks).join();
            return null;
        });
    }

    @Test void documentLanguageAndQuirksAreIndependentOfCompilationAndSerialization() {
        Evaluator classes = mode(true, () -> Selector.evaluatorOf(".mixed"));
        mode(false, () -> {
            Document standard = Jsoup.parse("<!doctype html><p class='Mixed'></p>");
            Document quirky = Jsoup.parse("<p class='Mixed'></p>");
            assertEquals(0, standard.select(classes).size());
            assertEquals(1, quirky.select(classes).size());
            quirky.quirksMode(Document.QuirksMode.noQuirks);
            assertEquals(0, quirky.select(classes).size());
            return null;
        });
        mode(true, () -> {
            Document html = Jsoup.parse("<!doctype html><p></p>");
            html.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            assertEquals(1, html.select("P").size());
            Document xml = Jsoup.parse("<Root><P/></Root>", "", Parser.xmlParser());
            xml.outputSettings().syntax(Document.OutputSettings.Syntax.html);
            assertEquals(0, xml.select("p").size());
            assertEquals(1, xml.select("P").size());
            return null;
        });
    }

    @Test void streamingUsesTheCapturedAttributeMode() throws Exception {
        Evaluator web = mode(true, () -> Selector.evaluatorOf("[class~=card i]"));
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(new StringReader("<!doctype html><p class=Card>one</p><p class=cardinal>two</p>"), "")) {
            assertEquals("one", parser.selectNext(web).text());
            assertNull(parser.selectNext(web));
        }
    }

    @Test void regexSerializationRetainsAttributeCaseInBothModes() {
        for (boolean web : new boolean[]{false, true}) mode(web, () -> {
            Evaluator regex = Selector.evaluatorOf(":matchesAttr(DATA, Mixed)");
            assertEquals(web ? ":matchesAttr(DATA, Mixed)" : "[DATA~=Mixed]", regex.toString());
            Document xml = Jsoup.parse("<root DATA='Mixed'/>", "", Parser.xmlParser());
            assertEquals(1, xml.select(regex.toString()).size());
            return null;
        });
    }

    @Test void contextControlsDocumentLanguageAndQuirks() {
        Element candidate = Jsoup.parse("<Thing DATA=Mixed class=Mixed></Thing>").selectFirst("thing");
        Document html = Jsoup.parse("<!doctype html>");
        Document xml = Jsoup.parse("<root/>", "", Parser.xmlParser());
        Evaluator tag = mode(true, () -> Selector.evaluatorOf("THING[DATA=Mixed]"));
        assertTrue(tag.matches(html, candidate));
        assertFalse(tag.matches(xml, candidate));
        Evaluator clazz = mode(true, () -> Selector.evaluatorOf(".mixed"));
        assertFalse(clazz.matches(html, candidate));
        html.quirksMode(Document.QuirksMode.quirks);
        assertTrue(clazz.matches(html, candidate));
    }

    @Test void findersUseTheCurrentPropertyAfterCompilation() {
        Element outer = new Element("div").id("x").attr("data-x", "yes").append("<div id=x data-x=yes></div>");
        Evaluator captured = mode(false, () -> Selector.evaluatorOf("div"));
        mode(true, () -> {
            assertEquals(1, outer.getElementsByTag("div").size());
            assertEquals(1, outer.getElementsByAttributeValue("data-x", "yes").size());
            assertSame(outer.child(0), outer.getElementById("x"));
            assertEquals(2, outer.select(captured).size());
            return null;
        });
        mode(false, () -> {
            assertEquals(2, outer.getElementsByTag("div").size());
            assertEquals(2, outer.getElementsByAttributeValue("data-x", "yes").size());
            assertSame(outer, outer.getElementById("x"));
            return null;
        });
    }

}
