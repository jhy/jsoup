package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.internal.QueryMode;
import org.jsoup.internal.SharedConstants;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_ERR;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
@ResourceLock(SYSTEM_ERR)
class QueryCompatTest {
    private String previousMode;
    private PrintStream previousErr;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @BeforeEach void captureWarnings() throws Exception {
        previousMode = System.getProperty(SharedConstants.QueryMode);
        previousErr = System.err;
        System.setProperty(SharedConstants.QueryMode, "check");
        System.setErr(new PrintStream(output, true, "UTF-8"));
    }

    @AfterEach void restoreSettings() {
        System.setErr(previousErr);
        if (previousMode == null) System.clearProperty(SharedConstants.QueryMode);
        else System.setProperty(SharedConstants.QueryMode, previousMode);
    }

    @Test void modeSettings() {
        System.clearProperty(SharedConstants.QueryMode);
        assertEquals(QueryMode.Legacy, QueryMode.current());
        for (String mode : new String[]{"legacy", "WEB", "Check"}) {
            System.setProperty(SharedConstants.QueryMode, mode);
            assertEquals(mode.toLowerCase(), QueryMode.current().name().toLowerCase());
        }
        System.setProperty(SharedConstants.QueryMode, "true");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Jsoup.parse("<p>").select("p"));
        assertEquals("Invalid jsoup.queryMode: true; expected legacy, web, or check", error.getMessage());
    }

    @Test void unchangedAndNonCheckingQueriesAreSilent() {
        Element outer = new Element("div").appendElement("div").parent();
        outer.select("span");
        for (String mode : new String[]{"legacy", "web"}) {
            System.setProperty(SharedConstants.QueryMode, mode);
            assertEquals(mode.equals("legacy") ? 2 : 1, outer.select("div").size());
        }
        assertEquals("", output.toString());
    }

    @Test void differencesRetainResultsAndShowTwoCallers() {
        Element outer = Jsoup.parse("<div><div></div></div>").body().child(0);
        Elements result = selectFromHelper(outer);
        assertSame(outer, result.first());
        assertEquals(2, result.size());
        String warning = output.toString();
        assertTrue(warning.startsWith("jsoup query compat: select(\"div\"): legacy=2, web=1; results differ; at org.jsoup.select.QueryCompatTest.selectFromHelper(QueryCompatTest.java:"), warning);
        assertTrue(warning.contains("; at org.jsoup.select.QueryCompatTest.differencesRetainResultsAndShowTwoCallers(QueryCompatTest.java:"), warning);
        assertFalse(warning.contains("; at org.jsoup.select.Selector."), warning);
        assertEquals(1, warning.split("\\R").length);
    }

    private Elements selectFromHelper(Element root) {
        return root.select("div");
    }

    @Test void comparesCaseAndRegexChanges() {
        Document doc = Jsoup.parse("<!doctype html><p class='Mixed card'></p><p class='mixed cardinal'></p>");
        assertEquals(2, doc.select(".mixed").size());
        assertEquals(2, doc.select("[class~=card]").size());
        assertEquals(2, output.toString().split("\\R").length);
        assertTrue(output.toString().contains("legacy=2, web=1"));
    }

    @Test void comparesActualFirstAndBooleanResults() {
        Element outer = Jsoup.parse("<!doctype html><div class=Mixed><div></div></div>").body().child(0);
        assertSame(outer, outer.selectFirst("div"));
        assertSame(outer, outer.expectFirst("div"));
        assertTrue(outer.is(".mixed"));
        assertSame(outer, outer.closest(".mixed"));
        assertNull(outer.selectFirst("span"));
        assertThrows(IllegalArgumentException.class, () -> outer.expectFirst("span"));
        String warning = output.toString();
        assertEquals(4, warning.split("\\R").length);
        assertTrue(warning.contains("selectFirst(\"div\"): results differ"));
        assertTrue(warning.contains("is(\".mixed\"): legacy=true, web=false"));
        assertTrue(warning.contains("closest(\".mixed\"): results differ"));
    }

    @Test void firstMatchDoesNotWarnAboutLaterDifferences() {
        Document doc = Jsoup.parse("<!doctype html><p class=mixed></p><p class=Mixed></p>");
        assertSame(doc.body().child(0), doc.selectFirst(".mixed"));
        assertEquals("", output.toString());
    }

    @Test void collectionsCompareIdentityAndOrderNotJustCounts() {
        Element a = new Element("p"), b = new Element("p");
        QueryCompat.check("p", new Elements(a), eval -> new Elements(b));
        QueryCompat.check("p", new Elements(a, b), eval -> new Elements(b, a));
        QueryCompat.check("p", new Elements(a, b), eval -> new Elements(a, b));
        assertEquals(2, output.toString().split("\\R").length);
        assertTrue(output.toString().contains("legacy=1, web=1; results differ"));
        assertTrue(output.toString().contains("legacy=2, web=2; results differ"));
    }

    @Test void multiRootResultsAreComparedAfterDeduplication() {
        Element outer = Jsoup.parse("<div><div></div></div>").body().child(0);
        Elements roots = new Elements(outer, outer.child(0));
        assertEquals(2, roots.select("div").size());
        assertSame(outer, roots.selectFirst("div"));
        assertSame(outer, roots.expectFirst("div"));
        assertEquals(3, output.toString().split("\\R").length);
        assertTrue(output.toString().contains("legacy=2, web=1; results differ"));
    }

    @Test void excludedPathsKeepLegacyBehavior() {
        Element outer = Jsoup.parse("<div><div></div></div>").body().child(0);
        assertEquals(2, outer.select(Selector.evaluatorOf("div")).size());
        assertEquals(2, outer.selectStream("div").count());
        assertEquals(2, outer.getElementsByTag("div").size());
        Iterator<Element> iterator = Arrays.asList(outer).iterator();
        Iterable<Element> once = () -> iterator;
        assertEquals(2, Selector.select("div", once).size());
        assertEquals("", output.toString());
    }

    @Test void failedAlternateQueriesWarnWithoutThrowing() {
        Document doc = Jsoup.parse("<p data-x=value></p>");
        Elements legacy = doc.select("[data-x='value' z]");
        assertTrue(legacy.isEmpty());
        assertTrue(output.toString().contains("web parse failed:"));
        output.reset();
        Element p = doc.selectFirst("p").attr("x", "\\\"");
        assertSame(p, doc.select("[x~=" + "\\\\" + '"' + "]").first());
        assertTrue(output.toString().contains("web parse failed:"));
        output.reset();
        assertThrows(Selector.SelectorParseException.class, () -> doc.select("["));
        assertEquals("", output.toString());
        QueryCompat.check("p", new Elements(), eval -> { throw new IllegalStateException("line1\r\n\t line2"); });
        assertTrue(output.toString().contains("web evaluation failed: line1 line2"));
        assertEquals(1, output.toString().split("\\R").length);
    }

    @Test void queryTextIsReadableOnOneLine() {
        QueryCompat.check("[data-x=\"a\\\\b\"]\r\n\t", new Elements(new Element("p")), eval -> new Elements());
        String warning = output.toString();
        assertTrue(warning.contains("select(\"[data-x=\"a\\\\b\"] \")"), warning);
        assertEquals(1, warning.split("\\R").length);
    }

    @Test void explicitCompilationDoesNotDependOnDefault() {
        Document xml = Jsoup.parse("<Root><Thing CLASS='Mixed' DATA='Mixed'/><thing class='mixed' data='mixed'/></Root>", "", Parser.xmlParser());
        String[] queries = {"Thing", "*|Thing", "[DATA]", "[DATA=Mixed]", ":is(Thing)", ":not(Thing)",
            "Root:has(> Thing)", "Root > Thing", "Thing + thing", "Thing ~ thing", ":nth-of-type(1)",
            ":first-of-type", ":last-of-type", ":only-of-type", "*", ":root", ":scope", ":empty", ":lt(1)",
            ":matchesAttr(DATA, Mixed)", "[data~=mixed]", ":contains(Mixed)"};
        for (String query : queries) {
            for (boolean web : new boolean[]{false, true}) {
                System.setProperty(SharedConstants.QueryMode, web ? "web" : "legacy");
                Evaluator expected = Selector.evaluatorOf(query);
                System.setProperty(SharedConstants.QueryMode, web ? "legacy" : "web");
                Evaluator actual = QueryParser.parse(query, web);
                assertEquals(xml.select(expected), xml.select(actual), query + " web=" + web);
                assertEquals(expected.toString(), actual.toString(), query);
                assertEquals(web, actual.web);
            }
        }
        assertEquals("", output.toString());
    }
}
