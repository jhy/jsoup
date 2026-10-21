package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;
import org.jsoup.select.Selector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;
import static org.jsoup.TestQueryMode.mode;
import static org.junit.jupiter.api.Assertions.*;

@ResourceLock(SYSTEM_PROPERTIES)
public class RegexTest {

    private boolean originalUseRe2j; // track original setting

    @BeforeEach
    void setUp() {
        originalUseRe2j = Regex.wantsRe2j();
    }

    @AfterEach
    void tearDown() {
        Regex.wantsRe2j(originalUseRe2j); // restore original setting
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testRegexDelegates(boolean useRe2j) {
        Regex.wantsRe2j(useRe2j);
        assertEquals(Regex.usingRe2j(), useRe2j);
        String pattern = "(\\d+)";
        String input = "12345";

        Regex regex = Regex.compile(pattern);
        Regex.Matcher matcher = regex.matcher(input);
        assertTrue(matcher.find());
    }

    @Test
    void jdkSupportsBackreferenceMatches() {
        Regex.wantsRe2j(false);
        String pattern = "(\\w+)\\s+\\1"; // backreference to group 1
        String input = "hello hello";

        Regex regex = Regex.compile(pattern);
        Regex.Matcher matcher = regex.matcher(input);
        assertTrue(matcher.find());
    }

    @Test
    void re2jRejectsBackreferenceThrows() {
        Regex.wantsRe2j(true);
        String pattern = "(\\w+)\\s+\\1"; // backreference unsupported by RE2J

        assertThrows(ValidationException.class, () -> Regex.compile(pattern));
        // and not the rej2 PatternSyntaxException
    }

    @Test
    void re2jSelectorHandlesMatchComplexity() {
        Regex.wantsRe2j(true);
        assertTrue(Regex.usingRe2j());

        Document doc = Jsoup.parse("<p data-key>");
        Evaluator evaluator = Selector.evaluatorOf("[data-key~=((a|)){1000}]"); // max repeat and empty alternatives create deep recursion in RE2J
        try {
            assertEquals(1, doc.select(evaluator).size());
        } catch (ValidationException e) {
            // pom defines a lower stack, so this normalized ex is normally thrown
            assertEquals("Pattern complexity error", e.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void queryParserThrowsSelectorExceptionOnMalformedRegex(boolean useRe2j) {
        Regex.wantsRe2j(useRe2j);
        String query = "[attr~=(unclosed]";

        boolean threw = false;
        try {
            QueryParser.parse(query);
        } catch (Selector.SelectorParseException e) {
            threw = true;
            assertTrue(e.getMessage().contains("Pattern syntax error"));
        }
        assertTrue(threw);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void legacyAttributeRegexFallsBackAfterCssProbeFailure(boolean useRe2j) {
        Regex.wantsRe2j(useRe2j);
        mode(false, () -> {
            String pattern = "\\\\" + '"';
            String query = "[x~=" + pattern + "]";
            Element p = new Element("p").attr("x", "\\\"");
            Evaluator evaluator = Selector.evaluatorOf(query);
            assertTrue(p.is(evaluator));
            assertTrue(p.is(evaluator.toString()));
            assertFalse(p.attr("x", "\"").is(query));
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void attributeRegexRoundTripsLeadingWhitespace(boolean useRe2j) {
        Regex.wantsRe2j(useRe2j);
        for (boolean web : new boolean[]{false, true}) mode(web, () -> {
            for (String whitespace : new String[]{" ", "  ", "\t", "\n", "\r", "\f", " \t\n"}) {
                String pattern = whitespace + "(foo)$";
                Evaluator original = new Evaluator.AttributeWithValueMatching("x", Pattern.compile(pattern));
                String query = original.toString();
                assertEquals(web ? ":matchesAttr(x, (?:)" + pattern + ")" : "[x~=" + pattern + "]", query);
                Evaluator reparsed = Selector.evaluatorOf(query);
                Element p = new Element("p").attr("x", "foo");
                assertFalse(p.is(original));
                assertFalse(p.is(reparsed));
                p.attr("x", whitespace + "foo");
                assertTrue(p.is(original));
                assertTrue(p.is(reparsed));
                assertEquals(query, reparsed.toString());
                if (!web) {
                    Evaluator legacy = Selector.evaluatorOf("[x~=" + pattern + "]");
                    assertTrue(p.is(legacy.toString()));
                    assertFalse(p.attr("x", "foo").is(legacy.toString()));
                }
            }
            assertEquals(web ? ":matchesAttr(x, foo)" : "[x~=foo]",
                new Evaluator.AttributeWithValueMatching("x", Pattern.compile("foo")).toString());
            if (!useRe2j) {
                // the prefix must not change capture numbering or terminate a trailing regex comment
                for (String pattern : new String[]{" (foo)\\1$", " (?x)foofoo # trailing comment"}) {
                    Evaluator original = new Evaluator.AttributeWithValueMatching("x", Pattern.compile(pattern));
                    Element p = new Element("p").attr("x", " foofoo");
                    assertTrue(p.is(original));
                    assertTrue(p.is(original.toString()));
                    assertFalse(p.attr("x", "foofoo").is(original.toString()));
                }
            }
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void regexSelectorsUseBalancedArguments(boolean useRe2j) {
        Regex.wantsRe2j(useRe2j);
        String[][] patterns = {
            {"^[^\\)]+$", "abc("}, {"^[\\(]+$", "(("}, {"^[\\)]+$", "))"},
            {"^((ab)|(cd))$", "ab"}, {"^\\Qfoo(bar)[baz]\\E$", "foo(bar)[baz]"},
            {"\"foo\"", "\"foo\""}
        };
        for (boolean web : new boolean[]{false, true}) mode(web, () -> {
            assertTrue(new Element("p").attr("dataQ", "abc(").is(":matchesAttr(data\\Q, ^[^\\)]+$)"));
            for (String[] pattern : patterns) {
                Element p = new Element("p").attr("data-x", pattern[1]).text(pattern[1]);
                Element parent = new Element("div").appendChild(p);
                for (String selector : new String[]{":matchesAttr(data-x, ", ":matches(", ":matchesOwn(",
                    ":matchesWholeText(", ":matchesWholeOwnText("}) {
                    String query = selector + pattern[0] + ")";
                    Evaluator evaluator = Selector.evaluatorOf(query);
                    assertTrue(p.is(evaluator), query);
                    assertTrue(p.is(evaluator.toString()), query);
                    assertTrue(p.is(":is(" + query + ")"), query);
                    assertTrue(parent.is(":has(> p" + query + ")"), query);
                }
            }
            Element plain = new Element("p").attr("data-x", "foo").text("foo");
            assertFalse(plain.is(":matches(\"foo\")")); // quotes are regex characters, not an argument wrapper
            assertFalse(plain.is(":matchesAttr(data-x, \"foo\")"));
            for (String query : new String[]{":matchesAttr(data-x, [)", ":matches([)", ":matchesOwn((abc)",
                ":matchesWholeText(abc\\)", ":matchesWholeOwnText([abc)",
                ":matchesAttr(data-x, ^[^)]+$)", ":matches(^[^)]+$)"})
                assertThrows(Selector.SelectorParseException.class, () -> Selector.evaluatorOf(query), query);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void legacyAttributeRegexKeepsItsSerialization(boolean useRe2j) {
        Regex.wantsRe2j(useRe2j);
        mode(false, () -> {
            Element p = new Element("p").attr("data-x", "abc(");
            String query = "[data-x~=^[^)]+$]";
            Evaluator evaluator = Selector.evaluatorOf(query);
            assertEquals(query, evaluator.toString());
            assertTrue(p.is(evaluator));
            assertTrue(p.is(evaluator.toString()));
            for (String value : new String[]{"[", "]", "foo[", "[i] s"}) {
                query = "[data-x~=" + Pattern.quote(value) + "]";
                evaluator = Selector.evaluatorOf(query);
                assertEquals(query, evaluator.toString());
                assertTrue(p.attr("data-x", value).is(evaluator), query);
                assertTrue(p.is(evaluator.toString()), query);
            }
            return null;
        });
    }

    @Test
    void regexCommentsDoNotChangeArgumentBalancing() {
        Regex.wantsRe2j(false);
        for (boolean web : new boolean[]{false, true}) mode(web, () -> {
            Element p = new Element("p").attr("data-x", "foo").text("foo");
            Element parent = new Element("div").appendChild(p);
            for (String pattern : new String[]{"(?x)foo # [\n", "(?x:foo # [\n)"}) {
                for (String selector : new String[]{":matchesAttr(data-x, ", ":matches(", ":matchesOwn(",
                    ":matchesWholeText(", ":matchesWholeOwnText("}) {
                    String query = selector + pattern + ")";
                    assertTrue(p.is(query), query);
                    assertTrue(p.is(":is(" + query + ")"), query);
                    assertTrue(parent.is(":has(> p" + query + ")"), query);
                }
                assertEquals(1, p.selectNodes("::text:matches(" + pattern + ")").size());
            }
            return null;
        });
    }
}
