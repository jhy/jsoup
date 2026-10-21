package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.internal.CssMatching;
import org.jsoup.internal.SharedConstants;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.ProcessingInstruction;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.jsoup.parser.StreamParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.jsoup.select.EvaluatorDebug.sexpr;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

/**
 Query contracts enabled by the 1.24 preview; these move into the ordinary tests when the default changes.
 */
@ResourceLock(SYSTEM_PROPERTIES)
class WebDomQueryTest {
    private String previousMode;

    @BeforeEach
    void enableWebDomQueries() {
        previousMode = System.getProperty(SharedConstants.QueryMode);
        System.setProperty(SharedConstants.QueryMode, "web");
    }

    @AfterEach
    void restoreQueryMode() {
        if (previousMode == null) System.clearProperty(SharedConstants.QueryMode);
        else System.setProperty(SharedConstants.QueryMode, previousMode);
    }

    @Test
    void findersExcludeTheReceiver() {
        Element outer = Jsoup.parse("<div id=x class=hit data-x=yes>word<div id=x class=hit data-x=yes>word</div></div>")
            .getElementById("x");
        List<Function<Element, Elements>> finders = Arrays.asList(
            el -> el.select("div"), el -> el.getElementsByTag("div"),
            el -> el.getElementsByClass("hit"), el -> el.getElementsByAttribute("data-x"),
            el -> el.getElementsByAttributeStarting("data-"), el -> el.getElementsByAttributeValue("data-x", "yes"),
            el -> el.getElementsByAttributeValueNot("missing", "no"),
            el -> el.getElementsByAttributeValueStarting("data-x", "y"),
            el -> el.getElementsByAttributeValueEnding("data-x", "s"),
            el -> el.getElementsByAttributeValueContaining("data-x", "e"),
            el -> el.getElementsByAttributeValueMatching("data-x", "yes"),
            el -> el.getElementsByIndexEquals(0), el -> el.getElementsByIndexGreaterThan(-1),
            el -> el.getElementsContainingText("word"), el -> el.getElementsContainingOwnText("word"),
            el -> el.getElementsMatchingText("word"), el -> el.getElementsMatchingOwnText("word"),
            Element::getAllElements);
        for (Function<Element, Elements> finder : finders) {
            Elements found = finder.apply(outer);
            assertEquals(1, found.size());
            assertFalse(found.contains(outer));
        }
        assertSame(outer.child(0), outer.getElementById("x"));
        assertSame(outer.child(0), outer.selectFirst("div"));
        assertSame(outer.child(0), outer.expectFirst("div"));
        assertEquals(1, outer.getElementsByIndexLessThan(1).size());
    }

    @Test
    void searchesExcludeSelfButMatchingIncludesSelf() {
        Element leaf = new Element("p").id("only");
        Evaluator evaluator = Selector.evaluatorOf("#only");
        assertTrue(leaf.select(evaluator).isEmpty());
        assertNull(leaf.selectFirst(evaluator));
        assertEquals(0, leaf.selectStream(evaluator).count());
        assertTrue(leaf.selectNodes(evaluator).isEmpty());
        assertNull(leaf.selectFirstNode(evaluator, Node.class));
        assertTrue(leaf.is(evaluator));
        assertSame(leaf, leaf.closest(evaluator));
        assertThrows(IllegalArgumentException.class, () -> leaf.expectFirst("#only"));
        assertThrows(IllegalArgumentException.class, () -> leaf.expectFirstNode("#only", Node.class));
        assertNull(leaf.getElementById("only"));
        assertTrue(leaf.getAllElements().isEmpty());
    }

    @Test
    void selectorsInspectTheSurroundingTreeWithoutReturningIt() {
        Document doc = Jsoup.parse("<body class=theme><aside></aside><section id=panel><div id=child><p id=p></p></div></section><p id=outside>");
        Element panel = doc.getElementById("panel");
        for (String query : Arrays.asList("body.theme p", "body > section > div > p", "aside + section p", "aside ~ section p", ":root body p")) {
            assertEquals("p", panel.selectFirst(query).id(), query);
        }
        assertEquals(2, panel.select("*").size());
        assertTrue(panel.select("body, aside, #outside, #panel").isEmpty());
        assertTrue(panel.is("aside + section"));
    }

    @Test
    void scopeRemainsAtTheReceiverForMatchingAndClosest() {
        Document doc = Jsoup.parse("<section id=panel><p id=p></p></section>");
        Element panel = doc.getElementById("panel");
        Evaluator scope = Selector.evaluatorOf(":scope");
        assertTrue(panel.select(scope).isEmpty());
        assertTrue(panel.is(scope));
        assertSame(panel, panel.closest(scope));
        assertNull(panel.closest(":scope > body"));
        assertEquals("p", panel.selectFirst("> p").id());
        assertSame(doc.body(), doc.selectFirst("> body"));
    }

    @Test
    void directIndexMatchingIncludesTheReceiver() {
        Document doc = Jsoup.parse("<p></p><p></p>");
        Element first = doc.body().child(0), second = doc.body().child(1);
        assertTrue(first.is("p:lt(1)"));
        assertTrue(first.is(":scope:lt(1)"));
        assertFalse(second.is("p:lt(1)"));
        assertSame(first, first.closest("p:lt(1)"));
        assertNull(second.closest("p:lt(1)"));
        assertEquals(new Elements(second), new Elements(first, second).not(":lt(1)"));
        assertEquals(new Elements(first), doc.body().select("p:lt(1)"));
        assertTrue(first.select(":lt(1)").isEmpty());
    }

    @Test
    void searchesResolveTheContextDocumentOnce() {
        LookupElement root = lookupRoot();
        for (String query : new String[]{"p", ".hit", "#hit", "[data-x=yes]", "p:not(.missing)", "p:has(> span)"}) {
            root.lookups = 0;
            assertEquals(2, root.select(query).size(), query);
            assertEquals(1, root.lookups, query);
        }
        root.lookups = 0;
        assertEquals(2, root.selectNodes("p", Element.class).size());
        assertEquals(1, root.lookups);
        root.lookups = 0;
        assertTrue(root.is(":scope.section[data-x=yes]"));
        assertEquals(1, root.lookups);
        root.lookups = 0;
        assertSame(root.parent(), root.closest("body"));
        assertEquals(1, root.lookups);
    }

    @Test
    void nestedSearchesRestoreTheEnclosingDocumentLookup() {
        LookupElement root = lookupRoot();
        Document xml = Jsoup.parse("<Root><Thing/><thing/></Root>", "", Parser.xmlParser());
        Evaluator nested = new Evaluator() {
            @Override
            public boolean matches(Element context, Element element) {
                if (!element.nameIs("p")) return false;
                assertTrue(CssMatching.isHtmlElement(context, element));
                assertEquals(1, xml.select("Thing").size());
                assertTrue(CssMatching.isHtmlElement(context, element));
                return true;
            }
        };
        root.lookups = 0;
        assertEquals(2, root.select(nested).size());
        assertEquals(1, root.lookups);
    }

    @Test
    void failedSearchesReleaseTheirDocumentLookup() {
        LookupElement root = lookupRoot();
        Evaluator failure = new Evaluator() {
            @Override
            public boolean matches(Element context, Element element) {
                throw new IllegalStateException("test");
            }
        };
        root.lookups = 0;
        assertThrows(IllegalStateException.class, () -> root.select(failure));
        assertEquals(1, root.lookups);
        assertTrue(CssMatching.isHtmlElement(root, root.child(0)));
        assertEquals(2, root.lookups); // outside the search, there is no cached document
    }

    @Test
    void lazySearchesReadLiveDocumentSettings() {
        LookupElement root = lookupRoot();
        Document doc = root.ownerDocument();
        root.lookups = 0;
        try (Stream<Element> results = root.selectStream("P")) {
            assertEquals(0, root.lookups);
            doc.parser(Parser.xmlParser());
            assertEquals(0, results.count());
            assertEquals(1, root.lookups);
        }
    }

    @Test
    void lazySearchesUseTheDocumentAtTraversalTime() {
        LookupElement root = lookupRoot();
        root.children().attr("class", "Mixed");
        root.ownerDocument().quirksMode(Document.QuirksMode.quirks);
        Document xml = Jsoup.parse("<Root/>", "", Parser.xmlParser());
        root.lookups = 0;
        try (Stream<Element> elements = root.selectStream(".mixed");
             Stream<Element> nodes = Collector.streamNodes(Selector.evaluatorOf(".mixed"), root, Element.class)) {
            assertEquals(0, root.lookups);
            xml.child(0).appendChild(root);
            assertEquals(0, elements.count());
            assertEquals(1, root.lookups);
            assertEquals(0, nodes.count());
            assertEquals(2, root.lookups);
            assertTrue(root.select(".mixed").isEmpty());
        }
    }

    /**
     Builds a context whose document walks can be counted independently of traversal.
     */
    private static LookupElement lookupRoot() {
        LookupElement root = new LookupElement();
        Jsoup.parse("<!doctype html>").body().appendChild(root);
        root.attr("class", "section").attr("data-x", "yes");
        root.append("<p class=hit id=hit data-x=yes><span></span></p><p class=hit id=hit data-x=yes><span></span></p>");
        return root;
    }

    private static class LookupElement extends Element {
        int lookups;

        /**
         Creates the query receiver used to count document lookups.
         */
        LookupElement() {
            super("section");
        }

        /**
         Counts ancestor walks without changing document ownership.
         */
        @Override
        public Document ownerDocument() {
            lookups++;
            return super.ownerDocument();
        }
    }

    @Test
    void relativeSelectorsKeepTheirAnchorAndOuterScope() {
        Element panel = Jsoup.parse("<body><section id=panel><div id=d><p></p></div></section>").getElementById("panel");
        for (String query : Arrays.asList("div:has(body p)", "div:has(:scope p)", "div:has(:scope > p)", "div:has(:is(body) p)"))
            assertTrue(panel.select(query).isEmpty(), query);
        assertEquals("d", panel.selectFirst("div:has(p)").id());
        assertEquals("d", panel.selectFirst("div:has(> p)").id());
        assertEquals("d", panel.selectFirst(":scope:has(p) > div").id());
        assertEquals("d", panel.selectFirst("div:is(:scope > div):not(:scope)").id());
        Element before = Jsoup.parse("<div id=a></div><section><p></p></section>").getElementById("a");
        assertTrue(before.is(":has(+ section p)"));
        assertTrue(before.is(":has(+ section > p)"));
        assertFalse(before.is(":has(+ section body p)"));
    }

    @Test
    void nodeQueriesRetainLeafNodes() {
        Element root = new Element("div").append("<!--one--><p><!--two--></p>");
        assertEquals(2, root.selectNodes("::comment", Comment.class).size());
        assertEquals("one", root.selectFirstNode(":scope > ::comment", Comment.class).getData());
        assertEquals(1, root.selectNodes("*", Node.class).size());
        Element piRoot = Jsoup.parse("<div><?marker name=target?></div>").body();
        assertTrue(piRoot.select("div:has([name=target])").isEmpty());
        assertEquals(1, piRoot.selectNodes("::pi[name=target]", ProcessingInstruction.class).size());
        assertTrue(piRoot.selectNodes("[name=target]").isEmpty());
    }

    @Test
    void filteringAndOverlappingRootsKeepTheirContracts() {
        Document doc = Jsoup.parse("<div id=a class=logo><div id=b><p id=p></p></div></div>");
        Element a = doc.getElementById("a");
        Element b = doc.getElementById("b");
        Elements roots = new Elements(a, b);
        assertEquals(new Elements(b), roots.not(".logo"));
        assertTrue(roots.not(":scope").isEmpty());
        assertEquals(new Elements(b, b.child(0)), roots.select("*"));
        assertSame(b, roots.selectFirst("div"));
        assertTrue(roots.is(":scope"));
    }

    @Test
    void streamingSearchesResolveDocumentScopeAndExcludeTheDocument() throws Exception {
        Evaluator all = Selector.evaluatorOf("*");
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse("<p>One</p>", "")) {
            Element next;
            boolean documentFound = false;
            while ((next = parser.selectNext(all)) != null) {
                documentFound |= next instanceof Document;
            }
            assertFalse(documentFound);
        }
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse("<p>One</p>", "")) {
            assertEquals("html", parser.selectNext(Selector.evaluatorOf(":scope")).tagName());
        }
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse("<p>One</p>", "")) {
            assertEquals("head", parser.selectNext(Selector.evaluatorOf(":scope > *")).tagName());
        }
    }

    @Test
    void tokenOperatorMatchesCompleteTokens() {
        Document doc = Jsoup.parse("<!doctype html><p class='card featured'></p><p class='cardinal'></p>");
        assertFalse(doc.selectFirst("p").hasClass("card featured"));
        assertEquals(1, doc.select("[class~=card]").size());
        assertEquals(1, doc.select("[class~='CARD' i]").size());
        assertEquals(0, doc.select("[class~='CARD' s]").size());
        assertEquals(0, doc.select("[class~='card featured']").size());
        assertEquals(0, doc.select("[class~='']").size());
        assertEquals(2, doc.select("p:matchesAttr(class, card)").size());
    }

    @Test
    void attributesUseHostDefaultsAndLiteralOperands() {
        Document doc = Jsoup.parse("<!doctype html><p class='Mixed' data-x='Mixed' lang='en-US' rel='NEXT' data-e='é' data-space=' a b '></p>");
        assertEquals(0, doc.select("[class=mixed], [data-x=mixed]").size());
        assertEquals(1, doc.select("[lang|=EN][rel=next]").size());
        assertEquals(1, doc.select("[data-e='\\e9'][data-space=' a b ']").size());
        assertEquals(1, doc.select("[data-space*=a\\ b]").size());
        assertEquals(0, doc.select("[data-space^= a]").size());
        assertEquals(0, doc.select("[data-space$=b ]").size());
        assertEquals(0, doc.select("[data-x^=''], [data-x$=''], [data-x*='']").size());
        assertTrue(doc.selectFirst("p").is("[missing!='']"));
        Evaluator eval = Selector.evaluatorOf("[data-e='\\e9' i]");
        assertEquals(doc.select(eval), doc.select(eval.toString()));
    }

    @Test
    void classAndIdRulesFollowDocumentModeButLookupIsExact() {
        for (Document.QuirksMode quirks : Document.QuirksMode.values()) {
            Document doc = Jsoup.parse("<!doctype html><p id='Mixed' class='Mixed'></p>").quirksMode(quirks);
            boolean insensitive = quirks == Document.QuirksMode.quirks;
            Element p = doc.getElementById("Mixed");
            assertEquals(insensitive, p.hasClass("mixed"));
            assertEquals(insensitive ? 1 : 0, doc.getElementsByClass("mixed").size());
            assertEquals(insensitive ? 1 : 0, doc.select(".mixed#mixed").size());
            assertNull(doc.getElementById("mixed"));
            assertEquals(0, doc.select("[class=mixed], [id=mixed]").size());
            assertEquals(1, doc.select("[class~=mixed i][id=mixed i]").size());
        }
        Document unicode = Jsoup.parse("<p id='K' class='K'></p>");
        assertEquals(0, unicode.select("#k, .k").size());
    }

    @Test
    void classChecksRespectAttributeNamesAndTokenBoundaries() {
        Document doc = Jsoup.parse("<!doctype html><p></p>");
        Element p = doc.selectFirst("p");
        p.attributes().put("CLASS", "Alpha\tbeta K");
        for (String name : new String[]{"Alpha", "beta", "K"}) {
            assertTrue(p.hasClass(name));
            assertTrue(p.is(new Evaluator.Class(name)));
        }
        for (String name : new String[]{"", "alpha", "k", "Alpha\tbeta K", "much_longer_than_the_attribute"}) {
            assertFalse(p.hasClass(name));
            assertFalse(p.is(new Evaluator.Class(name)));
        }
        doc.quirksMode(Document.QuirksMode.quirks);
        assertTrue(p.hasClass("alpha"));
        assertTrue(p.is(".alpha"));
        assertFalse(p.hasClass("k"));
        assertFalse(p.is(".k"));

        Document xml = Jsoup.parse("<Root><Child CLASS='alpha'/><Child class='alpha'/></Root>", "", Parser.xmlParser());
        assertFalse(xml.child(0).child(0).hasClass("alpha"));
        assertTrue(xml.child(0).child(1).hasClass("alpha"));
        assertEquals(new Elements(xml.child(0).child(1)), xml.select(".alpha"));
    }

    @Test
    void idLookupsRespectXmlAttributeNameCase() {
        Document xml = Jsoup.parse("<Root><Thing ID='x'/><Thing id='x'/></Root>", "", Parser.xmlParser());
        Element root = xml.child(0), upper = root.child(0), lower = root.child(1);
        assertFalse(upper.is("#x, [id]"));
        assertNull(upper.getElementById("x"));
        assertSame(lower, xml.getElementById("x"));
        assertSame(lower, root.getElementById("x"));
        assertEquals(new Elements(lower), xml.select("#x"));
        lower.attr("id", "Mixed");
        xml.quirksMode(Document.QuirksMode.quirks);
        assertSame(lower, xml.getElementById("Mixed"));
        assertNull(xml.getElementById("mixed"));
    }

    @Test
    void absoluteAttributesRespectAttributeNameCase() {
        String base = "https://example.com/base/";
        Document xml = Jsoup.parse("<Root><a HREF='/upper'/><a href='/lower'/></Root>", base, Parser.xmlParser());
        Element upper = xml.child(0).child(0), lower = xml.child(0).child(1);
        assertFalse(upper.is("[href], [abs:href]"));
        assertTrue(upper.is("[HREF][abs:HREF='https://example.com/upper']"));
        assertTrue(lower.is("[href][abs:href='https://example.com/lower']"));
        assertFalse(lower.is("[HREF], [abs:HREF]"));

        Document html = Jsoup.parse("<!doctype html><a href=/path></a>", base);
        Element link = html.expectFirst("a");
        link.removeAttr("href");
        link.attributes().put("HREF", "/path");
        assertTrue(link.is("[href][abs:href='https://example.com/path']"));
        assertFalse(new Element("a").is("[abs:href]"));
        assertFalse(new Element("a").attr("href", "/path").is("[abs:href]"));
        link.attr("HREF", "");
        assertTrue(link.is("[abs:href='https://example.com/base/']"));
        link.attributes().put("abs:href", "literal");
        assertTrue(link.is("[abs:href=literal]"));
        link.attributes().put("abs:href", "");
        assertTrue(link.is("[abs:href='https://example.com/base/']"));
    }

    @Test
    void xmlAndForeignNamesRemainExact() {
        Document xml = Jsoup.parse("<Root><Thing DATA='Mixed' class='Mixed'/><thing DATA='mixed'/></Root>", "", Parser.xmlParser());
        assertEquals(1, xml.select("Thing").size());
        assertEquals(1, xml.getElementsByTag("Thing").size());
        assertEquals(0, xml.select("[data]").size());
        assertEquals(2, xml.select("[DATA]").size());
        assertEquals(1, xml.select("[DATA=mixed]").size());
        assertEquals(2, xml.select("[DATA=mixed i]").size());
        assertEquals(0, xml.select(".mixed").size());
        assertEquals(2, xml.select("Thing:only-of-type, thing:only-of-type").size());
        Document xhtml = Jsoup.parse("<html xmlns='http://www.w3.org/1999/xhtml'><P TYPE='TEXT'/></html>", "", Parser.xmlParser());
        assertEquals(1, xhtml.select("P[TYPE=TEXT]").size());
        assertEquals(0, xhtml.select("p, [type], [TYPE=text]").size());
        Document svg = Jsoup.parse("<!doctype html><svg><linearGradient viewBox='Mixed'/></svg>");
        assertEquals(1, svg.select("linearGradient[viewBox=Mixed]").size());
        assertEquals(0, svg.select("lineargradient, [viewbox], [viewBox=mixed]").size());
        Element detached = new Element("p").attr("class", "Mixed");
        assertFalse(detached.hasClass("mixed"));
        assertTrue(detached.is("P[class~=mixed i]"));
    }

    @Test
    void processingInstructionsShareExplicitComparisons() {
        Document doc = Jsoup.parse("<?probe DATA='Mixed'?>", "", Parser.xmlParser());
        assertEquals(1, doc.selectNodes("::pi[DATA=Mixed]", ProcessingInstruction.class).size());
        assertEquals(0, doc.selectNodes("::pi[data], ::pi[DATA=mixed]", ProcessingInstruction.class).size());
        assertEquals(1, doc.selectNodes("::pi[DATA=mixed i]:matchesAttr(DATA, (?i)mixed)", ProcessingInstruction.class).size());
    }

    @Test
    void findersAndDirectEvaluatorsShareCaseRules() {
        Document doc = Jsoup.parse("<!doctype html><p class='Mixed' data-x='Mixed' type='TEXT'></p>");
        assertEquals(0, doc.getElementsByAttributeValue("data-x", "mixed").size());
        assertEquals(0, doc.getElementsByAttributeValueStarting("data-x", "mi").size());
        assertEquals(0, doc.getElementsByAttributeValueEnding("data-x", "ED").size());
        assertEquals(0, doc.getElementsByAttributeValueContaining("data-x", "XE").size());
        assertEquals(1, doc.getElementsByAttributeValue("type", "text").size());
        assertEquals(1, doc.select(new Evaluator.AttributeWithValue("data-x", "Mixed")).size());
        assertEquals(0, doc.select(new Evaluator.AttributeWithValueStarting("data-x", "mi")).size());
        assertEquals(1, doc.getElementsByAttributeValueMatching("data-x", Pattern.compile("mixed", Pattern.CASE_INSENSITIVE)).size());
    }

    @Test
    void htmlAttributeValueDefaultsIncludeLanguageLinkMediaAndMethod() {
        Element html = Jsoup.parse("<!doctype html><p></p>").expectFirst("p");
        Element xml = Jsoup.parse("<root><p/></root>", "", Parser.xmlParser()).expectFirst("p");
        Element svg = Jsoup.parse("<!doctype html><svg><g></g></svg>").expectFirst("g");
        String[][] attributes = {
            {"language", "JAVASCRIPT", "javascript"}, {"link", "RED", "red"},
            {"media", "SCREEN", "screen"}, {"method", "POST", "post"}
        };
        for (String[] attribute : attributes) {
            String key = attribute[0], query = "[" + key + "=" + attribute[2];
            for (Element element : new Element[]{html, xml, svg}) {
                element.attr(key, attribute[1]);
                assertEquals(element == html, element.is(query + "]"), key);
                assertTrue(element.is(query + " i]"), key);
                assertFalse(element.is(query + " s]"), key);
            }
        }
    }

    @Test
    void ofTypeUsesExpandedNamesInXml() {
        Document xml = Jsoup.parse("<root xmlns:p='urn:a' xmlns:q='urn:a' xmlns:r='urn:b'><p:X/><q:X/><r:X/><p:x/></root>", "", Parser.xmlParser());
        assertEquals(1, xml.select("q|X:nth-of-type(2)").size());
        assertEquals(0, xml.select("p|X:only-of-type").size());
        assertEquals(1, xml.select("r|X:only-of-type").size());
        assertEquals(1, xml.select("p|x:only-of-type").size());
    }

    @Test
    void prefixExtensionsFollowNameCaseRules() {
        Document html = Jsoup.parse("<!doctype html><ABC:DEF DATA-X='one'></ABC:DEF>", Parser.htmlParser().settings(ParseSettings.preserveCase));
        assertEquals(1, html.select("abc|*[^data-]").size());
        assertEquals(1, html.select("*|def[DATA-X]").size());
        Document xml = Jsoup.parse("<root><ABC:DEF DATA-X='one'/></root>", "", Parser.xmlParser());
        assertEquals(1, xml.select("ABC|*[^DATA-]").size());
        assertEquals(0, xml.select("abc|*, *|def, [^data-]").size());
    }

    @Test
    void escapedAttributeNamesAndQuotedValuesRoundTrip() {
        Element p = new Element("p").attr("data=name", "Mixed").attr("data,name", "a,b");
        assertTrue(p.is("[data\\=name='Mi\\78 ed']"));
        Evaluator regex = Selector.evaluatorOf(":matchesAttr(data\\,name, a,b)");
        assertTrue(p.is(regex));
        assertTrue(p.is(regex.toString()));
        assertTrue(p.is("[data\\=name='Mi\\\nxed']"));
    }

    @Test
    void documentRelativeChainsAnchorAtTheDocumentElement() {
        Document doc = Jsoup.parse("<!doctype html><p></p>");
        assertFalse(doc.is(":has(:scope p)"));
        assertTrue(doc.is(":has(> * + *)"));
        assertFalse(doc.is(":has(> html p)"));
    }

    @Test
    void documentRelativeTraversalsShareTheDocumentElementAnchor() {
        Document doc = Jsoup.parse("<!doctype html><p></p>");
        assertTrue(doc.is(":has(> head)"));
        assertTrue(doc.is(":has(> body)"));
        assertFalse(doc.is(":has(> html)"));
        assertTrue(doc.is(":has(> head + body)"));
        assertFalse(doc.is(":has(html)"));
        assertFalse(doc.is(":has(+ html)"));
        assertFalse(doc.is(":has(~ html)"));
        Document empty = new Document("");
        for (String query : new String[]{":has(*)", ":has(> *)", ":has(> * + *)", ":has(+ *)", ":has(~ *)"})
            assertFalse(empty.is(query), query);

        Document xml = Jsoup.parse("<Root><First/><Second/></Root>", "", Parser.xmlParser());
        assertTrue(xml.is(":has(> First)"));
        assertTrue(xml.is(":has(> First + Second)"));
        assertFalse(xml.is(":has(> Root)"));
        assertFalse(xml.is(":has(Root)"));
    }

    @Test
    void decodedQuotesAreNotStrippedAgain() {
        Document doc = Jsoup.parse("<!doctype html><p></p>");
        Element p = doc.selectFirst("p");
        p.attr("data-x", "'Mixed'");
        Evaluator literal = Selector.evaluatorOf("[data-x=\"'Mixed'\"]");
        assertTrue(p.is(literal));
        assertTrue(p.is(literal.toString()));
        assertFalse(p.is(new Evaluator.AttributeWithValue("data-x", "'Mixed'")));
        assertTrue(p.is(new Evaluator.AttributeWithValue("data-x", "\"'Mixed'\"")));
    }

    @Test
    void blankNodes() {
        Document doc = Jsoup.parse("<p> </p><p><!--  --><!----></p><p>\n</p><p>One</p><p><!-- two --></p>");

        Nodes<Node> nodes = doc.selectNodes("::node:blank", Node.class);
        assertEquals(11, nodes.size());
        assertEquals("html", nodes.get(0).nodeName());
        assertEquals("head", nodes.get(1).nodeName());
        assertEquals("body", nodes.get(2).nodeName());
        assertEquals("p", nodes.get(3).nodeName());
        assertEquals("#text", nodes.get(4).nodeName());
        assertEquals("p", nodes.get(5).nodeName());
        assertEquals("#comment", nodes.get(6).nodeName());
        assertEquals("#comment", nodes.get(7).nodeName());
        assertEquals("p", nodes.get(8).nodeName());
        assertEquals("#text", nodes.get(9).nodeName());
        assertEquals("p", nodes.get(10).nodeName());

        Nodes<Comment> comments = doc.selectNodes("::comment:blank", Comment.class);
        assertEquals(2, comments.size());
        assertEquals("  ", comments.get(0).getData());
        assertEquals("", comments.get(1).getData());

        String notBlank = "::comment:not(:blank)";
        Evaluator notBlankEval = QueryParser.parse(notBlank);
        assertEquals("(And (InstanceType '::comment')(Not (BlankValue ':blank')))", sexpr(notBlankEval));
        Nodes<Comment> commentsWithData = doc.selectNodes(notBlankEval, Comment.class);
        assertEquals(1, commentsWithData.size());
        assertEquals(" two ", commentsWithData.get(0).getData());
    }

    @Test
    void selectClassWithSpace() {
        final String html = "<div class=\"value\">class without space</div>\n"
            + "<div class=\"value \">class with space</div>";

        Document doc = Jsoup.parse(html);

        Elements found = doc.select("div[class=value ]");
        assertEquals(1, found.size());
        assertEquals("class without space", found.get(0).text());

        found = doc.select("div[class=\"value \"]");
        assertEquals(1, found.size());
        assertEquals("class with space", found.get(0).text());

        found = doc.select("div[class=\"value\\ \"]");
        assertEquals(1, found.size());
    }

    @Test
    void nameMatchingUsesAsciiButTextMatchingUsesUnicode() {
        Document doc = Jsoup.parse("<x-Ä data-Ä='VALUE'>Äpfel</x-Ä><x-ä data-ä='value'>Other</x-ä>");
        assertEquals("Äpfel", doc.select("X-Ä").text());
        assertEquals("Other", doc.select("x-ä").text());
        assertEquals("Äpfel", doc.select("[DATA-Ä]").text());
        assertEquals("", doc.select("[DATA-Ä=value]").text());
        assertEquals("Äpfel", doc.select("[^DATA-Ä]").text());
        assertEquals("Äpfel", doc.select("x-Ä:contains(äPFEL)").text());
        Element el = new Element(new org.jsoup.parser.Tag("x "), "");
        el.text("Space");
        assertEquals("", el.select("x\\ :not(p)").text());
    }

    @Test
    void namespacedXmlQueriesUseExactNames() {
        Document doc = Jsoup.parse("<One:Two>One</One:Two><three:four>Two</three:four>", "", Parser.xmlParser());
        assertEquals("One", doc.select("One|Two").text());
        assertTrue(doc.select("one|two, Three|Four, three|Four").isEmpty());
        assertEquals("Two", doc.select("three|four").text());
    }

    @Test
    void documentAndTextQueriesExcludeReceivers() throws Exception {
        Document doc = Jsoup.parse("<div><p>word<script>word</script></p></div>");
        assertEquals(6, doc.select("*").size());
        assertEquals(4, doc.select(":contains(word)").size());
        assertEquals(3, doc.body().select(":containsData(word)").size());
        assertEquals(new Elements(doc.selectFirst("div")), doc.body().select(":has(p)"));
        assertEquals(2, doc.body().select(":not(p)").size());
        assertTrue(doc.select("p").select(":contains(word)").isEmpty());
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse("<p>Full</p>", "")) {
            Element found = parser.expectFirst("*");
            assertSame(parser.document().child(0), found);
            assertEquals("Full", found.text());
        }
    }

    @Test
    void literalOperandsSupportUnquotedValues() {
        String[][] values = {
            {"123", "123"}, {"-1", "-1"}, {"-", "-"}, {"1,2", "1,2"}, {"a.b", "a.b"},
            {"/one", "/one"}, {"https://example.com/a?x=1&y=2", "https://example.com/a?x=1&y=2"},
            {"http://[::1]/path", "http://[::1]/path"}, {"a[b[c]]d", "a[b[c]]d"},
            {"two words", "two words"}, {"two\twords", "two\twords"}, {"two words z", "two words z"},
            {"  two words  ", "two words"}, {"", ""}, {"  ", ""},
            {"a\\]b", "a]b"}, {"a\\[b", "a[b"}, {"C:\\\\Query", "C:\\Query"},
            {"\\31 23", "123"}, {"foo\\20 ", "foo "}, {"\\ foo\\ ", " foo "},
            {"foo\\20  bar", "foo  bar"}
        };
        for (String[] value : values) {
            String query = "[data-x=" + value[0] + "]";
            Element p = new Element("p").attr("data-x", value[1]);
            Element parent = new Element("div").appendChild(p);
            Evaluator evaluator = Selector.evaluatorOf(query);
            assertTrue(p.is(evaluator), query);
            assertTrue(p.is(evaluator.toString()), query);
            assertTrue(p.is(":is(" + evaluator + ")"), query);
            assertTrue(parent.is(":has(> " + query + ")"), query);
            Evaluator has = Selector.evaluatorOf(":has(> " + query + ")");
            assertTrue(parent.is(has.toString()), query);
        }

        assertThrows(Selector.SelectorParseException.class, () -> Selector.evaluatorOf("[/jsoup.userdata=foo]"));
        Selector.SelectorParseException ex = assertThrows(Selector.SelectorParseException.class,
            () -> Selector.evaluatorOf("[\"=\"]"));
        assertEquals("Attribute name expected", ex.getMessage());
    }

    @Test
    void unquotedValuesUseOnlyTheFinalSeparateTokenAsAFlag() {
        String[][] values = {
            {"two words i", "TWO WORDS"}, {"two words S", "two words"}, {"123 s", "123"},
            {"/one I", "/ONE"}, {"http://[::1]/path i", "HTTP://[::1]/PATH"},
            {"i", "i"}, {" s ", "s"}, {"foo i s", "foo i"}, {"foo i z", "foo i z"},
            {"foo\\ i", "foo i"}, {"foo\\20 i", "foo i"}, {"foo\\20  i", "FOO "},
            {"foo\\20  s", "foo "}, {"foo \\69", "FOO"}, {"foo\\ i s", "foo i"},
            {"'foo i'", "foo i"}, {"\"foo s\"", "foo s"}, {"\"two words\"i", "TWO WORDS"}
        };
        for (String[] value : values) {
            String query = "[data-x=" + value[0] + "]";
            Element p = new Element("p").attr("data-x", value[1]);
            Evaluator evaluator = Selector.evaluatorOf(query);
            assertTrue(p.is(evaluator), query);
            assertTrue(p.is(evaluator.toString()), query);
            assertFalse(p.attr("data-x", "different").is(evaluator), query);
        }
        Element p = new Element("p").attr("data-x", "Two Words");
        assertTrue(p.is("[data-x=two words i]"));
        assertFalse(p.is("[data-x=two words s]"));
        assertFalse(p.is("[data-x=two words]"));
    }

    @Test
    void extendedValuesWorkWithEachAttributeOperator() {
        for (String operator : new String[]{"=", "!=", "^=", "$=", "*=", "|=", "~="}) {
            for (String value : new String[]{"123,abc", "two words", ""}) {
                Element p = new Element("p").attr("data-x", value);
                String query = "[data-x" + operator + value + "]";
                boolean expected = operator.equals("=") || operator.equals("|=") ||
                    !operator.equals("!=") && !value.isEmpty() && (!operator.equals("~=") || !value.contains(" "));
                Evaluator evaluator = Selector.evaluatorOf(query);
                assertEquals(expected, p.is(evaluator), query);
                assertEquals(expected, p.is(evaluator.toString()), query);

                if (!value.isEmpty()) {
                    p.attr("data-x", value.toUpperCase(Locale.ROOT));
                    Evaluator insensitive = Selector.evaluatorOf("[data-x" + operator + value + " i]");
                    assertEquals(expected, p.is(insensitive), query);
                    assertEquals(expected, p.is(insensitive.toString()), query);
                }
            }
        }
    }

    @Test
    void extendedValuesStillRejectMalformedQuotedSyntax() {
        for (String query : new String[]{
            "[data-x='two words' z]", "[data-x=\"two words\" i s]",
            "[data-x='two words]", "[data-x='two\nwords']", "[data-x=foo\\\nbar]", "[data-x=http://[::1]/"})
            assertThrows(Selector.SelectorParseException.class, () -> Selector.evaluatorOf(query), query);
    }

    @Test
    void attributeValueIdentifiersReplaceLeadingNull() {
        for (String value : new String[]{"\u0000", "-\u0000", "\u0000value", "-\u0000value"}) {
            Element p = new Element("p").attr("data-x", value.replace('\u0000', '\uFFFD'));
            assertTrue(p.is("[data-x=" + value + "]"));
            assertTrue(p.is("[data-x='" + value + "']"));
            Evaluator evaluator = Selector.evaluatorOf("[data-x=" + value + "]");
            assertTrue(p.is(evaluator.toString()));
        }
    }

    @Test
    void publicAttributeEvaluatorsSerializeCssOperands() {
        String key = "data=name";
        List<BiFunction<String, String, Evaluator>> constructors = Arrays.asList(
            Evaluator.AttributeWithValue::new, Evaluator.AttributeWithValueNot::new,
            Evaluator.AttributeWithValueStarting::new, Evaluator.AttributeWithValueEnding::new,
            Evaluator.AttributeWithValueContaining::new);
        String[][] values = {
            {"two words", "two words"}, {"123", "123"}, {"", ""}, {"-", "-"}, {"-1", "-1"},
            {"'quoted'", "quoted"}, {"\"quoted\"", "quoted"}, {"a\"quote", "a\"quote"},
            {"a'quote", "a'quote"}, {"a\\b", "a\\b"}, {"C:\\Query", "C:\\Query"}, {"C:\\End", "C:\\End"},
            {"a]b", "a]b"}, {"a\\", "a\\"}, {"line\nbreak", "line\nbreak"}, {"foo s", "foo s"}
        };
        for (String[] value : values) {
            for (BiFunction<String, String, Evaluator> constructor : constructors) {
                Evaluator evaluator = constructor.apply(key, value[0]);
                String query = evaluator.toString();
                Element p = new Element("p").attr(key, value[1]);
                Element parent = new Element("div").appendChild(p);
                boolean matches = !(evaluator instanceof Evaluator.AttributeWithValueNot) &&
                    (!value[1].isEmpty() || evaluator instanceof Evaluator.AttributeWithValue);
                assertEquals(matches, p.is(evaluator), query);
                for (String actual : new String[]{value[1], "prefix" + value[1] + "suffix", "different"}) {
                    p.attr(key, actual);
                    boolean expected = p.is(evaluator);
                    assertEquals(expected, p.is(query), query);
                    assertEquals(expected, p.is(":is(" + query + ")"), query);
                    assertEquals(expected, parent.is(":has(> " + query + ")"), query);
                }
                Element missing = new Element("p");
                Element missingParent = new Element("div").appendChild(missing);
                boolean expected = missing.is(evaluator);
                assertEquals(expected, missing.is(query), query);
                assertEquals(expected, missing.is(":is(" + query + ")"), query);
                assertEquals(expected, missingParent.is(":has(> " + query + ")"), query);
            }
        }
    }

    @Test
    void htmlAttributesFollowDocumentLanguageWhenCaseIsPreserved() {
        String html = "<SVG VIEWBOX=one><LINEARGRADIENT KEYPOINTS=two UNKNOWN=three /></SVG>";
        Document doc = Jsoup.parse(html, Parser.htmlParser().settings(ParseSettings.preserveCase));
        assertTrue(doc.select("svg").isEmpty());
        assertEquals("LINEARGRADIENT", doc.expectFirst("SVG").child(0).tagName());
        assertEquals(1, doc.select("SVG[VIEWBOX=one]").size());
        assertTrue(doc.select("SVG[viewbox]").isEmpty());
    }

    @Test
    void processingInstructionValuesAreCaseSensitive() {
        Document doc = Jsoup.parse("<?marker mode=Append?><?Marker mode=replace?><?other?>");
        assertEquals(3, doc.selectNodes("::pi[mode!=append]", ProcessingInstruction.class).size());
        assertEquals(1, doc.selectNodes("::pi[mode=append i]", ProcessingInstruction.class).size());
        assertEquals("::pi(marker)[mode=Append]", QueryParser.parse("::pi(marker)[mode=Append]").toString());
    }

    @Test
    void detachedRootsMatchWithoutBeingReturnedBySearch() {
        Element root = new Element("div").append("<p></p>");
        assertTrue(root.is(":root"));
        assertTrue(root.select(":root").isEmpty());
        assertSame(root, root.child(0).closest(":root"));
    }

}
