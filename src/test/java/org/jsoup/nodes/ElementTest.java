package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.helper.ValidationException;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.QueryParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 Tests for Element (DOM stuff mostly).

 @author Jonathan Hedley */
public class ElementTest {
    private final String reference = "<div id=div1><p>Hello</p><p>Another <b>element</b></p><div id=div2><img src=foo.png></div></div>";

    private static void validateScriptContents(String src, Element el) {
        assertEquals("", el.text()); // it's not text
        assertEquals("", el.ownText());
        assertEquals("", el.wholeText());
        assertEquals(src, el.html());
        assertEquals(src, el.data());
    }

    private static void validateXmlScriptContents(Element el) {
        assertEquals("var foo = 5 < 2; var bar = 1 && 2;", el.text());
        assertEquals("var foo = 5 < 2; var bar = 1 && 2;", el.ownText());
        assertEquals("var foo = 5 < 2;\nvar bar = 1 && 2;", el.wholeText());
        assertEquals("var foo = 5 &lt; 2;\nvar bar = 1 &amp;&amp; 2;", el.html());
        assertEquals("", el.data());
    }

    @Test
    public void testId() {
        Document doc = Jsoup.parse("<div id=Foo>");
        Element el = doc.selectFirst("div");
        assertEquals("Foo", el.id());
    }

    @Test
    public void testSetId() {
        Document doc = Jsoup.parse("<div id=Boo>");
        Element el = doc.selectFirst("div");
        el.id("Foo");
        assertEquals("Foo", el.id());
    }

    @Test
    public void getElementsByTagName() {
        Document doc = Jsoup.parse(reference);
        List<Element> divs = doc.getElementsByTag("div");
        assertEquals(2, divs.size());
        assertEquals("div1", divs.get(0).id());
        assertEquals("div2", divs.get(1).id());

        List<Element> ps = doc.getElementsByTag("p");
        assertEquals(2, ps.size());
        assertEquals("Hello", ((TextNode) ps.get(0).childNode(0)).getWholeText());
        assertEquals("Another ", ((TextNode) ps.get(1).childNode(0)).getWholeText());
        List<Element> ps2 = doc.getElementsByTag("P");
        assertEquals(ps, ps2);

        List<Element> imgs = doc.getElementsByTag("img");
        assertEquals("foo.png", imgs.get(0).attr("src"));

        List<Element> empty = doc.getElementsByTag("wtf");
        assertEquals(0, empty.size());
    }

    @Test
    public void getNamespacedElementsByTag() {
        Document doc = Jsoup.parse("<div><abc:def id=1>Hello</abc:def></div>");
        Elements els = doc.getElementsByTag("abc:def");
        assertEquals(1, els.size());
        assertEquals("1", els.first().id());
        assertEquals("abc:def", els.first().tagName());
    }

    @Test
    public void testGetElementById() {
        Document doc = Jsoup.parse(reference);
        Element div = doc.getElementById("div1");
        assertEquals("div1", div.id());
        assertNull(doc.getElementById("none"));

        Document doc2 = Jsoup.parse("<div id=1><div id=2><p>Hello <span id=2>world!</span></p></div></div>");
        Element div2 = doc2.getElementById("2");
        assertEquals("div", div2.tagName()); // not the span
        Element span = div2.child(0).getElementById("2"); // called from <p> context should be span
        assertEquals("span", span.tagName());
    }

    @Test
    public void testGetText() {
        Document doc = Jsoup.parse(reference);
        assertEquals("Hello Another element", doc.text());
        assertEquals("Another element", doc.getElementsByTag("p").get(1).text());
    }

    @Test
    public void testGetChildText() {
        Document doc = Jsoup.parse("<p>Hello <b>there</b> now");
        Element p = doc.select("p").first();
        assertEquals("Hello there now", p.text());
        assertEquals("Hello now", p.ownText());
    }

    @Test
    public void testNormalisesText() {
        String h = "<p>Hello<p>There.</p> \n <p>Here <b>is</b> \n s<b>om</b>e text.";
        Document doc = Jsoup.parse(h);
        String text = doc.text();
        assertEquals("Hello There. Here is some text.", text);
    }

    @Test
    public void testKeepsPreText() {
        String h = "<p>Hello \n \n there.</p> <div><pre>  What's \n\n  that?</pre>";
        Document doc = Jsoup.parse(h);
        assertEquals("Hello there.   What's \n\n  that?", doc.text());
    }

    @Test
    public void testKeepsPreTextInCode() {
        String h = "<pre><code>code\n\ncode</code></pre>";
        Document doc = Jsoup.parse(h);
        assertEquals("code\n\ncode", doc.text());
        assertEquals("<pre><code>code\n\ncode</code></pre>", doc.body().html());
    }

    @Test
    public void testKeepsPreTextAtDepth() {
        String h = "<pre><code><span><b>code\n\ncode</b></span></code></pre>";
        Document doc = Jsoup.parse(h);
        assertEquals("code\n\ncode", doc.text());
        assertEquals("<pre><code><span><b>code\n\ncode</b></span></code></pre>", doc.body().html());
    }

    @Test
    public void testBrHasSpace() {
        Document doc = Jsoup.parse("<p>Hello<br>there</p>");
        assertEquals("Hello there", doc.text());
        assertEquals("Hello there", doc.select("p").first().ownText());

        doc = Jsoup.parse("<p>Hello <br> there</p>");
        assertEquals("Hello there", doc.text());
    }

    @Test
    public void testBrHasSpaceCaseSensitive() {
        Document doc = Jsoup.parse("<p>Hello<br>there<BR>now</p>", Parser.htmlParser().settings(ParseSettings.preserveCase));
        assertEquals("Hello there now", doc.text());
        assertEquals("Hello there now", doc.select("p").first().ownText());

        doc = Jsoup.parse("<p>Hello <br> there <BR> now</p>");
        assertEquals("Hello there now", doc.text());
    }

    @Test public void textHasSpacesAfterBlock() {
        Document doc = Jsoup.parse("<div>One</div><div>Two</div><span>Three</span><p>Fou<i>r</i></p>");
        String text = doc.text();
        String wholeText = doc.wholeText();

        assertEquals("One Two Three Four", text);
        assertEquals("OneTwoThreeFour",wholeText);

        assertEquals("OneTwo",Jsoup.parse("<span>One</span><span>Two</span>").text());
    }

    @Test
    public void testWholeText() {
        Document doc = Jsoup.parse("<p> Hello\nthere &nbsp;  </p>");
        assertEquals(" Hello\nthere    ", doc.wholeText());

        doc = Jsoup.parse("<p>Hello  \n  there</p>");
        assertEquals("Hello  \n  there", doc.wholeText());

        doc = Jsoup.parse("<p>Hello  <div>\n  there</div></p>");
        assertEquals("Hello  \n  there", doc.wholeText());
    }

    @Test
    public void testGetSiblings() {
        Document doc = Jsoup.parse("<div><p>Hello<p id=1>there<p>this<p>is<p>an<p id=last>element</div>");
        Element p = doc.getElementById("1");
        assertEquals("there", p.text());
        assertEquals("Hello", p.previousElementSibling().text());
        assertEquals("this", p.nextElementSibling().text());
        assertEquals("Hello", p.firstElementSibling().text());
        assertEquals("element", p.lastElementSibling().text());
    }

    @Test
    public void testGetSiblingsWithDuplicateContent() {
        Document doc = Jsoup.parse("<div><p>Hello<p id=1>there<p>this<p>this<p>is<p>an<p id=last>element</div>");
        Element p = doc.getElementById("1");
        assertEquals("there", p.text());
        assertEquals("Hello", p.previousElementSibling().text());
        assertEquals("this", p.nextElementSibling().text());
        assertEquals("this", p.nextElementSibling().nextElementSibling().text());
        assertEquals("is", p.nextElementSibling().nextElementSibling().nextElementSibling().text());
        assertEquals("Hello", p.firstElementSibling().text());
        assertEquals("element", p.lastElementSibling().text());
    }

    @Test
    public void testFirstElementSiblingOnOrphan() {
        Element p = new Element("p");
        assertSame(p, p.firstElementSibling());
        assertSame(p, p.lastElementSibling());
    }

    @Test
    public void testFirstAndLastSiblings() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three");
        Element div = doc.selectFirst("div");
        Element one = div.child(0);
        Element two = div.child(1);
        Element three = div.child(2);

        assertSame(one, one.firstElementSibling());
        assertSame(one, two.firstElementSibling());
        assertSame(three, three.lastElementSibling());
        assertSame(three, two.lastElementSibling());
    }

    @Test
    public void testGetParents() {
        Document doc = Jsoup.parse("<div><p>Hello <span>there</span></div>");
        Element span = doc.select("span").first();
        Elements parents = span.parents();

        assertEquals(4, parents.size());
        assertEquals("p", parents.get(0).tagName());
        assertEquals("div", parents.get(1).tagName());
        assertEquals("body", parents.get(2).tagName());
        assertEquals("html", parents.get(3).tagName());

        Element orphan = new Element("p");
        Elements none = orphan.parents();
        assertEquals(0, none.size());
    }

    @Test
    public void testElementSiblingIndex() {
        Document doc = Jsoup.parse("<div><p>One</p>...<p>Two</p>...<p>Three</p>");
        Elements ps = doc.select("p");
        assertEquals(0, ps.get(0).elementSiblingIndex());
        assertEquals(1, ps.get(1).elementSiblingIndex());
        assertEquals(2, ps.get(2).elementSiblingIndex());
    }

    @Test
    public void testElementSiblingIndexSameContent() {
        Document doc = Jsoup.parse("<div><p>One</p>...<p>One</p>...<p>One</p>");
        Elements ps = doc.select("p");
        assertEquals(0, ps.get(0).elementSiblingIndex());
        assertEquals(1, ps.get(1).elementSiblingIndex());
        assertEquals(2, ps.get(2).elementSiblingIndex());
    }

    @Test
    public void testGetElementsWithClass() {
        Document doc = Jsoup.parse("<div class='mellow yellow'><span class=mellow>Hello <b class='yellow'>Yellow!</b></span><p>Empty</p></div>");

        List<Element> els = doc.getElementsByClass("mellow");
        assertEquals(2, els.size());
        assertEquals("div", els.get(0).tagName());
        assertEquals("span", els.get(1).tagName());

        List<Element> els2 = doc.getElementsByClass("yellow");
        assertEquals(2, els2.size());
        assertEquals("div", els2.get(0).tagName());
        assertEquals("b", els2.get(1).tagName());

        List<Element> none = doc.getElementsByClass("solo");
        assertEquals(0, none.size());
    }

    @Test
    public void testGetElementsWithAttribute() {
        Document doc = Jsoup.parse("<div style='bold'><p title=qux><p><b style></b></p></div>");
        List<Element> els = doc.getElementsByAttribute("style");
        assertEquals(2, els.size());
        assertEquals("div", els.get(0).tagName());
        assertEquals("b", els.get(1).tagName());

        List<Element> none = doc.getElementsByAttribute("class");
        assertEquals(0, none.size());
    }

    @Test
    public void testGetElementsWithAttributeDash() {
        Document doc = Jsoup.parse("<meta http-equiv=content-type value=utf8 id=1> <meta name=foo content=bar id=2> <div http-equiv=content-type value=utf8 id=3>");
        Elements meta = doc.select("meta[http-equiv=content-type], meta[charset]");
        assertEquals(1, meta.size());
        assertEquals("1", meta.first().id());
    }

    @Test
    public void testGetElementsWithAttributeValue() {
        Document doc = Jsoup.parse("<div style='bold'><p><p><b style></b></p></div>");
        List<Element> els = doc.getElementsByAttributeValue("style", "bold");
        assertEquals(1, els.size());
        assertEquals("div", els.get(0).tagName());

        List<Element> none = doc.getElementsByAttributeValue("style", "none");
        assertEquals(0, none.size());
    }

    @Test
    public void testClassDomMethods() {
        Document doc = Jsoup.parse("<div><span class=' mellow yellow '>Hello <b>Yellow</b></span></div>");
        List<Element> els = doc.getElementsByAttribute("class");
        Element span = els.get(0);
        assertEquals("mellow yellow", span.className());
        assertTrue(span.hasClass("mellow"));
        assertTrue(span.hasClass("yellow"));
        Set<String> classes = span.classNames();
        assertEquals(2, classes.size());
        assertTrue(classes.contains("mellow"));
        assertTrue(classes.contains("yellow"));

        assertEquals("", doc.className());
        classes = doc.classNames();
        assertEquals(0, classes.size());
        assertFalse(doc.hasClass("mellow"));
    }

    @Test
    public void testHasClassDomMethods() {
        Tag tag = Tag.valueOf("a");
        Attributes attribs = new Attributes();
        Element el = new Element(tag, "", attribs);

        attribs.put("class", "toto");
        boolean hasClass = el.hasClass("toto");
        assertTrue(hasClass);

        attribs.put("class", " toto");
        hasClass = el.hasClass("toto");
        assertTrue(hasClass);

        attribs.put("class", "toto ");
        hasClass = el.hasClass("toto");
        assertTrue(hasClass);

        attribs.put("class", "\ttoto ");
        hasClass = el.hasClass("toto");
        assertTrue(hasClass);

        attribs.put("class", "  toto ");
        hasClass = el.hasClass("toto");
        assertTrue(hasClass);

        attribs.put("class", "ab");
        hasClass = el.hasClass("toto");
        assertFalse(hasClass);

        attribs.put("class", "     ");
        hasClass = el.hasClass("toto");
        assertFalse(hasClass);

        attribs.put("class", "tototo");
        hasClass = el.hasClass("toto");
        assertFalse(hasClass);

        attribs.put("class", "raulpismuth  ");
        hasClass = el.hasClass("raulpismuth");
        assertTrue(hasClass);

        attribs.put("class", " abcd  raulpismuth efgh ");
        hasClass = el.hasClass("raulpismuth");
        assertTrue(hasClass);

        attribs.put("class", " abcd efgh raulpismuth");
        hasClass = el.hasClass("raulpismuth");
        assertTrue(hasClass);

        attribs.put("class", " abcd efgh raulpismuth ");
        hasClass = el.hasClass("raulpismuth");
        assertTrue(hasClass);
    }

    @Test
    public void testClassUpdates() {
        Document doc = Jsoup.parse("<div class='mellow yellow'></div>");
        Element div = doc.select("div").first();

        div.addClass("green");
        assertEquals("mellow yellow green", div.className());
        div.removeClass("red"); // noop
        div.removeClass("yellow");
        assertEquals("mellow green", div.className());
        div.toggleClass("green").toggleClass("red");
        assertEquals("mellow red", div.className());
    }

    @Test
    public void testOuterHtml() {
        Document doc = Jsoup.parse("<div title='Tags &amp;c.'><img src=foo.png><p><!-- comment -->Hello<p>there");
        assertEquals("<html><head></head><body><div title=\"Tags &amp;c.\"><img src=\"foo.png\"><p><!-- comment -->Hello</p><p>there</p></div></body></html>",
            TextUtil.stripNewlines(doc.outerHtml()));
    }

    @Test
    public void testInnerHtml() {
        Document doc = Jsoup.parse("<div>\n <p>Hello</p> </div>");
        assertEquals("<p>Hello</p>", doc.getElementsByTag("div").get(0).html());
    }

    @Test
    public void testFormatHtml() {
        Document doc = Jsoup.parse("<title>Format test</title><div><p>Hello <span>jsoup <span>users</span></span></p><p>Good.</p></div>");
        assertEquals("<html>\n <head>\n  <title>Format test</title>\n </head>\n <body>\n  <div>\n   <p>Hello <span>jsoup <span>users</span></span></p>\n   <p>Good.</p>\n  </div>\n </body>\n</html>", doc.html());
    }

    @Test
    public void testFormatOutline() {
        Document doc = Jsoup.parse("<title>Format test</title><div><p>Hello <span>jsoup <span>users</span></span></p><p>Good.</p></div>");
        doc.outputSettings().outline(true);
        assertEquals("<html>\n <head>\n  <title>Format test</title>\n </head>\n <body>\n  <div>\n   <p>\n    Hello \n    <span>\n     jsoup \n     <span>users</span>\n    </span>\n   </p>\n   <p>Good.</p>\n  </div>\n </body>\n</html>", doc.html());
    }

    @Test
    public void testSetIndent() {
        Document doc = Jsoup.parse("<div><p>Hello\nthere</p></div>");
        doc.outputSettings().indentAmount(0);
        assertEquals("<html>\n<head></head>\n<body>\n<div>\n<p>Hello there</p>\n</div>\n</body>\n</html>", doc.html());
    }

    @Test void testIndentLevel() {
        // deep to test default and extended max
        StringBuilder divs = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            divs.append("<div>");
        }
        divs.append("Foo");
        Document doc = Jsoup.parse(divs.toString());
        Document.OutputSettings settings = doc.outputSettings();

        int defaultMax = 30;
        assertEquals(defaultMax, settings.maxPaddingWidth());
        String html = doc.html();
        assertTrue(html.contains("                              <div>\n" +
            "                              Foo\n" +
            "                              </div>"));

        settings.maxPaddingWidth(32);
        assertEquals(32, settings.maxPaddingWidth());
        html = doc.html();
        assertTrue(html.contains("                                <div>\n" +
            "                                Foo\n" +
            "                                </div>"));

        settings.maxPaddingWidth(-1);
        assertEquals(-1, settings.maxPaddingWidth());
        html = doc.html();
        assertTrue(html.contains("                                         <div>\n" +
            "                                          Foo\n" +
            "                                         </div>"));
    }

    @Test
    public void testNotPretty() {
        Document doc = Jsoup.parse("<div>   \n<p>Hello\n there\n</p></div>");
        doc.outputSettings().prettyPrint(false);
        assertEquals("<html><head></head><body><div>   \n<p>Hello\n there\n</p></div></body></html>", doc.html());

        Element div = doc.select("div").first();
        assertEquals("   \n<p>Hello\n there\n</p>", div.html());
    }

    @Test
    public void testNotPrettyWithEnDashBody() {
        String html = "<div><span>1:15</span>&ndash;<span>2:15</span>&nbsp;p.m.</div>";
        Document document = Jsoup.parse(html);
        document.outputSettings().prettyPrint(false);

        assertEquals("<div><span>1:15</span>–<span>2:15</span>&nbsp;p.m.</div>", document.body().html());
    }

    @Test
    public void testPrettyWithEnDashBody() {
        String html = "<div><span>1:15</span>&ndash;<span>2:15</span>&nbsp;p.m.</div>";
        Document document = Jsoup.parse(html);

        assertEquals("<div>\n <span>1:15</span>–<span>2:15</span>&nbsp;p.m.\n</div>", document.body().html());
    }

    @Test
    public void testPrettyAndOutlineWithEnDashBody() {
        String html = "<div><span>1:15</span>&ndash;<span>2:15</span>&nbsp;p.m.</div>";
        Document document = Jsoup.parse(html);
        document.outputSettings().outline(true);

        assertEquals("<div>\n <span>1:15</span>\n –\n <span>2:15</span>\n &nbsp;p.m.\n</div>", document.body().html());
    }

    @Test
    public void testBasicFormats() {
        String html = "<span>0</span>.<div><span>1</span>-<span>2</span><p><span>3</span>-<span>4</span><div>5</div>";
        Document doc = Jsoup.parse(html);
        assertEquals(
            "<span>0</span>.\n" +
                "<div>\n" +
                " <span>1</span>-<span>2</span>\n" +
                " <p><span>3</span>-<span>4</span></p>\n" +
                " <div>\n" +
                "  5\n" +
                " </div>\n" +
                "</div>", doc.body().html());
    }

    @Test
    public void testEmptyElementFormatHtml() {
        // don't put newlines into empty blocks
        Document doc = Jsoup.parse("<section><div></div></section>");
        assertEquals("<section>\n <div></div>\n</section>", doc.select("section").first().outerHtml());
    }

    @Test
    public void testNoIndentOnScriptAndStyle() {
        // don't newline+indent closing </script> and </style> tags
        Document doc = Jsoup.parse("<script>one\ntwo</script>\n<style>three\nfour</style>");
        assertEquals("<script>one\ntwo</script>\n<style>three\nfour</style>", doc.head().html());
    }

    @Test
    public void testContainerOutput() {
        Document doc = Jsoup.parse("<title>Hello there</title> <div><p>Hello</p><p>there</p></div> <div>Another</div>");
        assertEquals("<title>Hello there</title>", doc.select("title").first().outerHtml());
        assertEquals("<div>\n <p>Hello</p>\n <p>there</p>\n</div>", doc.select("div").first().outerHtml());
        assertEquals("<div>\n <p>Hello</p>\n <p>there</p>\n</div>\n<div>\n Another\n</div>", doc.select("body").first().html());
    }

    @Test
    public void testSetText() {
        String h = "<div id=1>Hello <p>there <b>now</b></p></div>";
        Document doc = Jsoup.parse(h);
        assertEquals("Hello there now", doc.text()); // need to sort out node whitespace
        assertEquals("there now", doc.select("p").get(0).text());

        Element div = doc.getElementById("1").text("Gone");
        assertEquals("Gone", div.text());
        assertEquals(0, doc.select("p").size());
    }

    @Test
    public void testAddNewElement() {
        Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
        Element div = doc.getElementById("1");
        div.appendElement("p").text("there");
        div.appendElement("P").attr("CLASS", "second").text("now");
        // manually specifying tag and attributes should maintain case based on parser settings
        assertEquals("<html><head></head><body><div id=\"1\"><p>Hello</p><p>there</p><p class=\"second\">now</p></div></body></html>",
            TextUtil.stripNewlines(doc.html()));

        // check sibling index (with short circuit on reindexChildren):
        Elements ps = doc.select("p");
        for (int i = 0; i < ps.size(); i++) {
            assertEquals(i, ps.get(i).siblingIndex);
        }
    }

    @Test
    public void testAddBooleanAttribute() {
        Element div = new Element(Tag.valueOf("div"), "");

        div.attr("true", true);

        div.attr("false", "value");
        div.attr("false", false);

        assertTrue(div.hasAttr("true"));
        assertEquals("", div.attr("true"));

        List<Attribute> attributes = div.attributes().asList();
        assertEquals(1, attributes.size(), "There should be one attribute");
        assertFalse(div.hasAttr("false"));

        assertEquals("<div true></div>", div.outerHtml());
    }

    @Test
    public void testAppendRowToTable() {
        Document doc = Jsoup.parse("<table><tr><td>1</td></tr></table>");
        Element table = doc.select("tbody").first();
        table.append("<tr><td>2</td></tr>");

        assertEquals("<table><tbody><tr><td>1</td></tr><tr><td>2</td></tr></tbody></table>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testPrependRowToTable() {
        Document doc = Jsoup.parse("<table><tr><td>1</td></tr></table>");
        Element table = doc.select("tbody").first();
        table.prepend("<tr><td>2</td></tr>");

        assertEquals("<table><tbody><tr><td>2</td></tr><tr><td>1</td></tr></tbody></table>", TextUtil.stripNewlines(doc.body().html()));

        // check sibling index (reindexChildren):
        Elements ps = doc.select("tr");
        for (int i = 0; i < ps.size(); i++) {
            assertEquals(i, ps.get(i).siblingIndex);
        }
    }

    @Test
    public void testPrependElement() {
        Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
        Element div = doc.getElementById("1");
        div.prependElement("p").text("Before");
        assertEquals("Before", div.child(0).text());
        assertEquals("Hello", div.child(1).text());
    }

    @Test
    public void testAddNewText() {
        Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
        Element div = doc.getElementById("1");
        div.appendText(" there & now >");
        assertEquals ("Hello there & now >", div.text());
        assertEquals("<p>Hello</p> there &amp; now &gt;", TextUtil.stripNewlines(div.html()));
    }

    @Test
    public void testPrependText() {
        Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
        Element div = doc.getElementById("1");
        div.prependText("there & now > ");
        assertEquals("there & now > Hello", div.text());
        assertEquals("there &amp; now &gt; <p>Hello</p>", TextUtil.stripNewlines(div.html()));
    }

    @Test
    public void testThrowsOnAddNullText() {
        assertThrows(IllegalArgumentException.class, () -> {
            Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
            Element div = doc.getElementById("1");
            div.appendText(null);
        });
    }

    @Test
    public void testThrowsOnPrependNullText() {
        assertThrows(IllegalArgumentException.class, () -> {
            Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
            Element div = doc.getElementById("1");
            div.prependText(null);
        });
    }

    @Test
    public void testAddNewHtml() {
        Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
        Element div = doc.getElementById("1");
        div.append("<p>there</p><p>now</p>");
        assertEquals("<p>Hello</p><p>there</p><p>now</p>", TextUtil.stripNewlines(div.html()));

        // check sibling index (no reindexChildren):
        Elements ps = doc.select("p");
        for (int i = 0; i < ps.size(); i++) {
            assertEquals(i, ps.get(i).siblingIndex);
        }
    }

    @Test
    public void testPrependNewHtml() {
        Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
        Element div = doc.getElementById("1");
        div.prepend("<p>there</p><p>now</p>");
        assertEquals("<p>there</p><p>now</p><p>Hello</p>", TextUtil.stripNewlines(div.html()));

        // check sibling index (reindexChildren):
        Elements ps = doc.select("p");
        for (int i = 0; i < ps.size(); i++) {
            assertEquals(i, ps.get(i).siblingIndex);
        }
    }

    @Test
    public void testSetHtml() {
        Document doc = Jsoup.parse("<div id=1><p>Hello</p></div>");
        Element div = doc.getElementById("1");
        div.html("<p>there</p><p>now</p>");
        assertEquals("<p>there</p><p>now</p>", TextUtil.stripNewlines(div.html()));
    }

    @Test
    public void testSetHtmlTitle() {
        Document doc = Jsoup.parse("<html><head id=2><title id=1></title></head></html>");

        Element title = doc.getElementById("1");
        title.html("good");
        assertEquals("good", title.html());
        title.html("<i>bad</i>");
        assertEquals("&lt;i&gt;bad&lt;/i&gt;", title.html());

        Element head = doc.getElementById("2");
        head.html("<title><i>bad</i></title>");
        assertEquals("<title>&lt;i&gt;bad&lt;/i&gt;</title>", head.html());
    }

    @Test
    public void testWrap() {
        Document doc = Jsoup.parse("<div><p>Hello</p><p>There</p></div>");
        Element p = doc.select("p").first();
        p.wrap("<div class='head'></div>");
        assertEquals("<div><div class=\"head\"><p>Hello</p></div><p>There</p></div>", TextUtil.stripNewlines(doc.body().html()));

        Element ret = p.wrap("<div><div class=foo></div><p>What?</p></div>");
        assertEquals("<div><div class=\"head\"><div><div class=\"foo\"><p>Hello</p></div><p>What?</p></div></div><p>There</p></div>",
            TextUtil.stripNewlines(doc.body().html()));

        assertEquals(ret, p);
    }

    @Test
    public void testWrapNoop() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div>");
        Node p = doc.select("p").first();
        Node wrapped = p.wrap("Some junk");
        assertSame(p, wrapped);
        assertEquals("<div><p>Hello</p></div>", TextUtil.stripNewlines(doc.body().html()));
        // should be a NOOP
    }

    @Test
    public void testWrapOnOrphan() {
        Element orphan = new Element("span").text("Hello!");
        assertFalse(orphan.hasParent());
        Element wrapped = orphan.wrap("<div></div> There!");
        assertSame(orphan, wrapped);
        assertTrue(orphan.hasParent()); // should now be in the DIV
        assertNotNull(orphan.parent());
        assertEquals("div", orphan.parent().tagName());
        assertEquals("<div>\n <span>Hello!</span>\n</div>", orphan.parent().outerHtml());
    }

    @Test
    public void testWrapArtificialStructure() {
        // div normally couldn't get into a p, but explicitly want to wrap
        Document doc = Jsoup.parse("<p>Hello <i>there</i> now.");
        Element i = doc.selectFirst("i");
        i.wrap("<div id=id1></div> quite");
        assertEquals("div", i.parent().tagName());
        assertEquals("<p>Hello <div id=\"id1\"><i>there</i></div> quite now.</p>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void before() {
        Document doc = Jsoup.parse("<div><p>Hello</p><p>There</p></div>");
        Element p1 = doc.select("p").first();
        p1.before("<div>one</div><div>two</div>");
        assertEquals("<div><div>one</div><div>two</div><p>Hello</p><p>There</p></div>", TextUtil.stripNewlines(doc.body().html()));

        doc.select("p").last().before("<p>Three</p><!-- four -->");
        assertEquals("<div><div>one</div><div>two</div><p>Hello</p><p>Three</p><!-- four --><p>There</p></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void after() {
        Document doc = Jsoup.parse("<div><p>Hello</p><p>There</p></div>");
        Element p1 = doc.select("p").first();
        p1.after("<div>one</div><div>two</div>");
        assertEquals("<div><p>Hello</p><div>one</div><div>two</div><p>There</p></div>", TextUtil.stripNewlines(doc.body().html()));

        doc.select("p").last().after("<p>Three</p><!-- four -->");
        assertEquals("<div><p>Hello</p><div>one</div><div>two</div><p>There</p><p>Three</p><!-- four --></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testWrapWithRemainder() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div>");
        Element p = doc.select("p").first();
        p.wrap("<div class='head'></div><p>There!</p>");
        assertEquals("<div><div class=\"head\"><p>Hello</p></div><p>There!</p></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testWrapWithSimpleRemainder() {
        Document doc = Jsoup.parse("<p>Hello");
        Element p = doc.selectFirst("p");
        Element body = p.parent();
        assertNotNull(body);
        assertEquals("body", body.tagName());

        p.wrap("<div></div> There");
        Element div = p.parent();
        assertNotNull(div);
        assertEquals("div", div.tagName());
        assertSame(div, p.parent());
        assertSame(body, div.parent());

        assertEquals("<div><p>Hello</p></div> There", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testHasText() {
        Document doc = Jsoup.parse("<div><p>Hello</p><p></p></div>");
        Element div = doc.select("div").first();
        Elements ps = doc.select("p");

        assertTrue(div.hasText());
        assertTrue(ps.first().hasText());
        assertFalse(ps.last().hasText());
    }

    @Test
    public void dataset() {
        Document doc = Jsoup.parse("<div id=1 data-name=jsoup class=new data-package=jar>Hello</div><p id=2>Hello</p>");
        Element div = doc.select("div").first();
        Map<String, String> dataset = div.dataset();
        Attributes attributes = div.attributes();

        // size, get, set, add, remove
        assertEquals(2, dataset.size());
        assertEquals("jsoup", dataset.get("name"));
        assertEquals("jar", dataset.get("package"));

        dataset.put("name", "jsoup updated");
        dataset.put("language", "java");
        dataset.remove("package");

        assertEquals(2, dataset.size());
        assertEquals(4, attributes.size());
        assertEquals("jsoup updated", attributes.get("data-name"));
        assertEquals("jsoup updated", dataset.get("name"));
        assertEquals("java", attributes.get("data-language"));
        assertEquals("java", dataset.get("language"));

        attributes.put("data-food", "bacon");
        assertEquals(3, dataset.size());
        assertEquals("bacon", dataset.get("food"));

        attributes.put("data-", "empty");
        assertNull(dataset.get("")); // data- is not a data attribute

        Element p = doc.select("p").first();
        assertEquals(0, p.dataset().size());

    }

    @Test
    public void parentlessToString() {
        Document doc = Jsoup.parse("<img src='foo'>");
        Element img = doc.select("img").first();
        assertEquals("<img src=\"foo\">", img.toString());

        img.remove(); // lost its parent
        assertEquals("<img src=\"foo\">", img.toString());
    }

    @Test
    public void orphanDivToString() {
        Element orphan = new Element("div").id("foo").text("Hello");
        assertEquals("<div id=\"foo\">\n Hello\n</div>", orphan.toString());
    }

    @Test
    public void testClone() {
        Document doc = Jsoup.parse("<div><p>One<p><span>Two</div>");

        Element p = doc.select("p").get(1);
        Element clone = p.clone();

        assertNotNull(clone.parentNode); // should be a cloned document just containing this clone
        assertEquals(1, clone.parentNode.childNodeSize());
        assertSame(clone.ownerDocument(), clone.parentNode);

        assertEquals(0, clone.siblingIndex);
        assertEquals(1, p.siblingIndex);
        assertNotNull(p.parent());

        clone.append("<span>Three");
        assertEquals("<p><span>Two</span><span>Three</span></p>", TextUtil.stripNewlines(clone.outerHtml()));
        assertEquals("<div><p>One</p><p><span>Two</span></p></div>", TextUtil.stripNewlines(doc.body().html())); // not modified

        doc.body().appendChild(clone); // adopt
        assertNotNull(clone.parent());
        assertEquals("<div><p>One</p><p><span>Two</span></p></div><p><span>Two</span><span>Three</span></p>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testClonesClassnames() {
        Document doc = Jsoup.parse("<div class='one two'></div>");
        Element div = doc.select("div").first();
        Set<String> classes = div.classNames();
        assertEquals(2, classes.size());
        assertTrue(classes.contains("one"));
        assertTrue(classes.contains("two"));

        Element copy = div.clone();
        Set<String> copyClasses = copy.classNames();
        assertEquals(2, copyClasses.size());
        assertTrue(copyClasses.contains("one"));
        assertTrue(copyClasses.contains("two"));
        copyClasses.add("three");
        copyClasses.remove("one");

        assertTrue(classes.contains("one"));
        assertFalse(classes.contains("three"));
        assertFalse(copyClasses.contains("one"));
        assertTrue(copyClasses.contains("three"));

        assertEquals("", div.html());
        assertEquals("", copy.html());
    }

    @Test
    public void testShallowClone() {
        String base = "http://example.com/";
        Document doc = Jsoup.parse("<div id=1 class=one><p id=2 class=two>One", base);
        Element d = doc.selectFirst("div");
        Element p = doc.selectFirst("p");
        TextNode t = p.textNodes().get(0);

        Element d2 = d.shallowClone();
        Element p2 = p.shallowClone();
        TextNode t2 = (TextNode) t.shallowClone();

        assertEquals(1, d.childNodeSize());
        assertEquals(0, d2.childNodeSize());

        assertEquals(1, p.childNodeSize());
        assertEquals(0, p2.childNodeSize());

        assertEquals("", p2.text());
        assertEquals("One", t2.text());

        assertEquals("two", p2.className());
        p2.removeClass("two");
        assertEquals("two", p.className());

        d2.append("<p id=3>Three");
        assertEquals(1, d2.childNodeSize());
        assertEquals("Three", d2.text());
        assertEquals("One", d.text());
        assertEquals(base, d2.baseUri());
    }

    @Test
    public void testTagNameSet() {
        Document doc = Jsoup.parse("<div><i>Hello</i>");
        doc.select("i").first().tagName("em");
        assertEquals(0, doc.select("i").size());
        assertEquals(1, doc.select("em").size());
        assertEquals("<em>Hello</em>", doc.select("div").first().html());
    }

    @Test
    public void testHtmlContainsOuter() {
        Document doc = Jsoup.parse("<title>Check</title> <div>Hello there</div>");
        doc.outputSettings().indentAmount(0);
        assertTrue(doc.html().contains(doc.select("title").outerHtml()));
        assertTrue(doc.html().contains(doc.select("div").outerHtml()));
    }

    @Test
    public void testGetTextNodes() {
        Document doc = Jsoup.parse("<p>One <span>Two</span> Three <br> Four</p>");
        List<TextNode> textNodes = doc.select("p").first().textNodes();

        assertEquals(3, textNodes.size());
        assertEquals("One ", textNodes.get(0).text());
        assertEquals(" Three ", textNodes.get(1).text());
        assertEquals(" Four", textNodes.get(2).text());

        assertEquals(0, doc.select("br").first().textNodes().size());
    }

    @Test
    public void testManipulateTextNodes() {
        Document doc = Jsoup.parse("<p>One <span>Two</span> Three <br> Four</p>");
        Element p = doc.select("p").first();
        List<TextNode> textNodes = p.textNodes();

        textNodes.get(1).text(" three-more ");
        textNodes.get(2).splitText(3).text("-ur");

        assertEquals("One Two three-more Fo-ur", p.text());
        assertEquals("One three-more Fo-ur", p.ownText());
        assertEquals(4, p.textNodes().size()); // grew because of split
    }

    @Test
    public void testGetDataNodes() {
        Document doc = Jsoup.parse("<script>One Two</script> <style>Three Four</style> <p>Fix Six</p>");
        Element script = doc.select("script").first();
        Element style = doc.select("style").first();
        Element p = doc.select("p").first();

        List<DataNode> scriptData = script.dataNodes();
        assertEquals(1, scriptData.size());
        assertEquals("One Two", scriptData.get(0).getWholeData());

        List<DataNode> styleData = style.dataNodes();
        assertEquals(1, styleData.size());
        assertEquals("Three Four", styleData.get(0).getWholeData());

        List<DataNode> pData = p.dataNodes();
        assertEquals(0, pData.size());
    }

    @Test
    public void elementIsNotASiblingOfItself() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three</div>");
        Element p2 = doc.select("p").get(1);

        assertEquals("Two", p2.text());
        Elements els = p2.siblingElements();
        assertEquals(2, els.size());
        assertEquals("<p>One</p>", els.get(0).outerHtml());
        assertEquals("<p>Three</p>", els.get(1).outerHtml());
    }

    @Test
    public void testChildThrowsIndexOutOfBoundsOnMissing() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p></div>");
        Element div = doc.select("div").first();

        assertEquals(2, div.children().size());
        assertEquals("One", div.child(0).text());

        try {
            div.child(3);
            fail("Should throw index out of bounds");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void moveByAppend() {
        // test for https://github.com/jhy/jsoup/issues/239
        // can empty an element and append its children to another element
        Document doc = Jsoup.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>");
        Element div1 = doc.select("div").get(0);
        Element div2 = doc.select("div").get(1);

        assertEquals(4, div1.childNodeSize());
        List<Node> children = div1.childNodes();
        assertEquals(4, children.size());

        div2.insertChildren(0, children);

        assertEquals(4, children.size()); // children is NOT backed by div1.childNodes but a wrapper, so should still be 4 (but re-parented)
        assertEquals(0, div1.childNodeSize());
        assertEquals(4, div2.childNodeSize());
        assertEquals("<div id=\"1\"></div>\n<div id=\"2\">\n Text \n <p>One</p> Text \n <p>Two</p>\n</div>",
            doc.body().html());
    }

    @Test
    public void insertChildrenArgumentValidation() {
        Document doc = Jsoup.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>");
        Element div1 = doc.select("div").get(0);
        Element div2 = doc.select("div").get(1);
        List<Node> children = div1.childNodes();

        try {
            div2.insertChildren(6, children);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            div2.insertChildren(-5, children);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            div2.insertChildren(0, (Collection<? extends Node>) null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void insertChildrenAtPosition() {
        Document doc = Jsoup.parse("<div id=1>Text1 <p>One</p> Text2 <p>Two</p></div><div id=2>Text3 <p>Three</p></div>");
        Element div1 = doc.select("div").get(0);
        Elements p1s = div1.select("p");
        Element div2 = doc.select("div").get(1);

        assertEquals(2, div2.childNodeSize());
        div2.insertChildren(-1, p1s);
        assertEquals(2, div1.childNodeSize()); // moved two out
        assertEquals(4, div2.childNodeSize());
        assertEquals(3, p1s.get(1).siblingIndex()); // should be last

        List<Node> els = new ArrayList<>();
        Element el1 = new Element(Tag.valueOf("span"), "").text("Span1");
        Element el2 = new Element(Tag.valueOf("span"), "").text("Span2");
        TextNode tn1 = new TextNode("Text4");
        els.add(el1);
        els.add(el2);
        els.add(tn1);

        assertNull(el1.parent());
        div2.insertChildren(-2, els);
        assertEquals(div2, el1.parent());
        assertEquals(7, div2.childNodeSize());
        assertEquals(3, el1.siblingIndex());
        assertEquals(4, el2.siblingIndex());
        assertEquals(5, tn1.siblingIndex());
    }

    @Test
    public void insertChildrenAsCopy() {
        Document doc = Jsoup.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>");
        Element div1 = doc.select("div").get(0);
        Element div2 = doc.select("div").get(1);
        Elements ps = doc.select("p").clone();
        ps.first().text("One cloned");
        div2.insertChildren(-1, ps);

        assertEquals(4, div1.childNodeSize()); // not moved -- cloned
        assertEquals(2, div2.childNodeSize());
        assertEquals("<div id=\"1\">Text <p>One</p> Text <p>Two</p></div><div id=\"2\"><p>One cloned</p><p>Two</p></div>",
            TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testCssPath() {
        Document doc = Jsoup.parse("<div id=\"id1\">A</div><div>B</div><div class=\"c1 c2\">C</div>");
        Element divA = doc.select("div").get(0);
        Element divB = doc.select("div").get(1);
        Element divC = doc.select("div").get(2);
        assertEquals(divA.cssSelector(), "#id1");
        assertEquals(divB.cssSelector(), "html > body > div:nth-child(2)");
        assertEquals(divC.cssSelector(), "html > body > div.c1.c2");

        assertSame(divA, doc.select(divA.cssSelector()).first());
        assertSame(divB, doc.select(divB.cssSelector()).first());
        assertSame(divC, doc.select(divC.cssSelector()).first());
    }

    @Test
    public void testCssPathDuplicateIds() {
        // https://github.com/jhy/jsoup/issues/1147 - multiple elements with same ID, use the non-ID form
        Document doc = Jsoup.parse("<article><div id=dupe>A</div><div id=dupe>B</div><div id=dupe class=c1>");
        Element divA = doc.select("div").get(0);
        Element divB = doc.select("div").get(1);
        Element divC = doc.select("div").get(2);

        assertEquals(divA.cssSelector(), "html > body > article > div:nth-child(1)");
        assertEquals(divB.cssSelector(), "html > body > article > div:nth-child(2)");
        assertEquals(divC.cssSelector(), "html > body > article > div.c1");

        assertSame(divA, doc.select(divA.cssSelector()).first());
        assertSame(divB, doc.select(divB.cssSelector()).first());
        assertSame(divC, doc.select(divC.cssSelector()).first());
    }

    @Test public void cssSelectorEscaped() {
        // https://github.com/jhy/jsoup/issues/1742
        Document doc = Jsoup.parse("<p\\p>One</p\\p> <p id='one.two'>Two</p> <p class='one.two:three/four'>Three</p>");
        Element one = doc.expectFirst("p\\\\p");
        Elements ps = doc.select("p");
        Element two = ps.get(0);
        Element three = ps.get(1);

        String oneSelect = one.cssSelector();
        assertEquals("html > body > p\\\\p", oneSelect);
        assertEquals(one, doc.expectFirst(oneSelect));

        String twoSelect = two.cssSelector();
        assertEquals("#one\\.two", twoSelect);
        assertEquals(two, doc.expectFirst(twoSelect));

        String threeSelect = three.cssSelector();
        assertEquals("html > body > p.one\\.two\\:three\\/four", threeSelect);
        assertEquals(three, doc.expectFirst(threeSelect));
    }

    @Test public void cssEscapedAmp() {
        Document doc = Jsoup.parse("<p class='\\&'>One</p>");
        Element one = doc.expectFirst(".\\\\\\&"); // tested matches js querySelector
        assertEquals("One", one.text());

        String q = one.cssSelector();
        assertEquals("html > body > p.\\\\\\&", q);
        assertEquals(one, doc.expectFirst(q));
    }

    @Test public void cssSelectorEscapedClass() {
        // example in https://github.com/jhy/jsoup/issues/838
        String html = "<div class='B\\&W\\?'><div class=test>Text</div></div>";
        Document parse = Jsoup.parse(html);
        Element el = parse.expectFirst(".test");
        assertEquals("Text", el.text());

        String q = el.cssSelector();
        assertEquals("html > body > div.B\\\\\\&W\\\\\\? > div.test", q);
        Element found = parse.expectFirst(q);
        assertEquals(found, el);
    }

    @Test
    public void testClassNames() {
        Document doc = Jsoup.parse("<div class=\"c1 c2\">C</div>");
        Element div = doc.select("div").get(0);

        assertEquals("c1 c2", div.className());

        final Set<String> set1 = div.classNames();
        final Object[] arr1 = set1.toArray();
        assertEquals(2, arr1.length);
        assertEquals("c1", arr1[0]);
        assertEquals("c2", arr1[1]);

        // Changes to the set should not be reflected in the Elements getters
        set1.add("c3");
        assertEquals(2, div.classNames().size());
        assertEquals("c1 c2", div.className());

        // Update the class names to a fresh set
        final Set<String> newSet = new LinkedHashSet<>(3);
        newSet.addAll(set1);
        newSet.add("c3");

        div.classNames(newSet);

        assertEquals("c1 c2 c3", div.className());

        final Set<String> set2 = div.classNames();
        final Object[] arr2 = set2.toArray();
        assertEquals(3, arr2.length);
        assertEquals("c1", arr2[0]);
        assertEquals("c2", arr2[1]);
        assertEquals("c3", arr2[2]);
    }

    @Test
    public void testHashAndEqualsAndValue() {
        // .equals and hashcode are identity. value is content.

        String doc1 = "<div id=1><p class=one>One</p><p class=one>One</p><p class=one>Two</p><p class=two>One</p></div>" +
            "<div id=2><p class=one>One</p><p class=one>One</p><p class=one>Two</p><p class=two>One</p></div>";

        Document doc = Jsoup.parse(doc1);
        Elements els = doc.select("p");

        /*
        for (Element el : els) {
            System.out.println(el.hashCode() + " - " + el.outerHtml());
        }

        0 1534787905 - <p class="one">One</p>
        1 1534787905 - <p class="one">One</p>
        2 1539683239 - <p class="one">Two</p>
        3 1535455211 - <p class="two">One</p>
        4 1534787905 - <p class="one">One</p>
        5 1534787905 - <p class="one">One</p>
        6 1539683239 - <p class="one">Two</p>
        7 1535455211 - <p class="two">One</p>
        */
        assertEquals(8, els.size());
        Element e0 = els.get(0);
        Element e1 = els.get(1);
        Element e2 = els.get(2);
        Element e3 = els.get(3);
        Element e4 = els.get(4);
        Element e5 = els.get(5);
        Element e6 = els.get(6);
        Element e7 = els.get(7);

        assertEquals(e0, e0);
        assertTrue(e0.hasSameValue(e1));
        assertTrue(e0.hasSameValue(e4));
        assertTrue(e0.hasSameValue(e5));
        assertNotEquals(e0, e2);
        assertFalse(e0.hasSameValue(e2));
        assertFalse(e0.hasSameValue(e3));
        assertFalse(e0.hasSameValue(e6));
        assertFalse(e0.hasSameValue(e7));

        assertEquals(e0.hashCode(), e0.hashCode());
        assertNotEquals(e0.hashCode(), (e2.hashCode()));
        assertNotEquals(e0.hashCode(), (e3).hashCode());
        assertNotEquals(e0.hashCode(), (e6).hashCode());
        assertNotEquals(e0.hashCode(), (e7).hashCode());
    }

    @Test
    public void testRelativeUrls() {
        String html = "<body><a href='./one.html'>One</a> <a href='two.html'>two</a> <a href='../three.html'>Three</a> <a href='//example2.com/four/'>Four</a> <a href='https://example2.com/five/'>Five</a> <a>Six</a> <a href=''>Seven</a>";
        Document doc = Jsoup.parse(html, "http://example.com/bar/");
        Elements els = doc.select("a");

        assertEquals("http://example.com/bar/one.html", els.get(0).absUrl("href"));
        assertEquals("http://example.com/bar/two.html", els.get(1).absUrl("href"));
        assertEquals("http://example.com/three.html", els.get(2).absUrl("href"));
        assertEquals("http://example2.com/four/", els.get(3).absUrl("href"));
        assertEquals("https://example2.com/five/", els.get(4).absUrl("href"));
        assertEquals("", els.get(5).absUrl("href"));
        assertEquals("http://example.com/bar/", els.get(6).absUrl("href"));
    }

    @Test
    public void testRelativeIdnUrls() {
        String idn = "https://www.测试.测试/";
        String idnFoo = idn + "foo.html?bar";

        Document doc = Jsoup.parse("<a href=''>One</a><a href='/bar.html?qux'>Two</a>", idnFoo);
        Elements els = doc.select("a");
        Element one = els.get(0);
        Element two = els.get(1);
        String hrefOne = one.absUrl("href");
        String hrefTwo = two.absUrl("href");
        assertEquals(idnFoo, hrefOne);
        assertEquals("https://www.测试.测试/bar.html?qux", hrefTwo);
    }

    @Test
    public void appendMustCorrectlyMoveChildrenInsideOneParentElement() {
        Document doc = new Document("");
        Element body = doc.appendElement("body");
        body.appendElement("div1");
        body.appendElement("div2");
        final Element div3 = body.appendElement("div3");
        div3.text("Check");
        final Element div4 = body.appendElement("div4");

        ArrayList<Element> toMove = new ArrayList<>();
        toMove.add(div3);
        toMove.add(div4);

        body.insertChildren(0, toMove);

        String result = doc.toString().replaceAll("\\s+", "");
        assertEquals("<body><div3>Check</div3><div4></div4><div1></div1><div2></div2></body>", result);
    }

    @Test
    public void testHashcodeIsStableWithContentChanges() {
        Element root = new Element(Tag.valueOf("root"), "");

        HashSet<Element> set = new HashSet<>();
        // Add root node:
        set.add(root);

        root.appendChild(new Element(Tag.valueOf("a"), ""));
        assertTrue(set.contains(root));
    }

    @Test
    public void testNamespacedElements() {
        // Namespaces with ns:tag in HTML must be translated to ns|tag in CSS.
        String html = "<html><body><fb:comments /></body></html>";
        Document doc = Jsoup.parse(html, "http://example.com/bar/");
        Elements els = doc.select("fb|comments");
        assertEquals(1, els.size());
        assertEquals("html > body > fb|comments", els.get(0).cssSelector());
    }

    @Test
    public void testChainedRemoveAttributes() {
        String html = "<a one two three four>Text</a>";
        Document doc = Jsoup.parse(html);
        Element a = doc.select("a").first();
        a
            .removeAttr("zero")
            .removeAttr("one")
            .removeAttr("two")
            .removeAttr("three")
            .removeAttr("four")
            .removeAttr("five");
        assertEquals("<a>Text</a>", a.outerHtml());
    }

    @Test
    public void testLoopedRemoveAttributes() {
        String html = "<a one two three four>Text</a><p foo>Two</p>";
        Document doc = Jsoup.parse(html);
        for (Element el : doc.getAllElements()) {
            el.clearAttributes();
        }

        assertEquals("<a>Text</a>\n<p>Two</p>", doc.body().html());
    }

    @Test
    public void testIs() {
        String html = "<div><p>One <a class=big>Two</a> Three</p><p>Another</p>";
        Document doc = Jsoup.parse(html);
        Element p = doc.select("p").first();

        assertTrue(p.is("p"));
        assertFalse(p.is("div"));
        assertTrue(p.is("p:has(a)"));
        assertFalse(p.is("a")); // does not descend
        assertTrue(p.is("p:first-child"));
        assertFalse(p.is("p:last-child"));
        assertTrue(p.is("*"));
        assertTrue(p.is("div p"));

        Element q = doc.select("p").last();
        assertTrue(q.is("p"));
        assertTrue(q.is("p ~ p"));
        assertTrue(q.is("p + p"));
        assertTrue(q.is("p:last-child"));
        assertFalse(q.is("p a"));
        assertFalse(q.is("a"));
    }

    @Test
    public void testEvalMethods() {
        Document doc = Jsoup.parse("<div><p>One <a class=big>Two</a> Three</p><p>Another</p>");
        Element p = doc.selectFirst(QueryParser.parse(("p")));
        assertEquals("One Three", p.ownText());

        assertTrue(p.is(QueryParser.parse("p")));
        Evaluator aEval = QueryParser.parse("a");
        assertFalse(p.is(aEval));

        Element a = p.selectFirst(aEval);
        assertEquals("div", a.closest(QueryParser.parse("div:has( > p)")).tagName());
        Element body = p.closest(QueryParser.parse("body"));
        assertEquals("body", body.nodeName());
    }

    @Test
    public void testClosest() {
        String html = "<article>\n" +
            "  <div id=div-01>Here is div-01\n" +
            "    <div id=div-02>Here is div-02\n" +
            "      <div id=div-03>Here is div-03</div>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "</article>";

        Document doc = Jsoup.parse(html);
        Element el = doc.selectFirst("#div-03");
        assertEquals("Here is div-03", el.text());
        assertEquals("div-03", el.id());

        assertEquals("div-02", el.closest("#div-02").id());
        assertEquals(el, el.closest("div div")); // closest div in a div is itself
        assertEquals("div-01", el.closest("article > div").id());
        assertEquals("article", el.closest(":not(div)").tagName());
        assertNull(el.closest("p"));
    }

    @Test
    public void elementByTagName() {
        Element a = new Element("P");
        assertEquals("P", a.tagName());
    }

    @Test
    public void testChildrenElements() {
        String html = "<div><p><a>One</a></p><p><a>Two</a></p>Three</div><span>Four</span><foo></foo><img>";
        Document doc = Jsoup.parse(html);
        Element div = doc.select("div").first();
        Element p = doc.select("p").first();
        Element span = doc.select("span").first();
        Element foo = doc.select("foo").first();
        Element img = doc.select("img").first();

        Elements docChildren = div.children();
        assertEquals(2, docChildren.size());
        assertEquals("<p><a>One</a></p>", docChildren.get(0).outerHtml());
        assertEquals("<p><a>Two</a></p>", docChildren.get(1).outerHtml());
        assertEquals(3, div.childNodes().size());
        assertEquals("Three", div.childNodes().get(2).outerHtml());

        assertEquals(1, p.children().size());
        assertEquals("One", p.children().text());

        assertEquals(0, span.children().size());
        assertEquals(1, span.childNodes().size());
        assertEquals("Four", span.childNodes().get(0).outerHtml());

        assertEquals(0, foo.children().size());
        assertEquals(0, foo.childNodes().size());
        assertEquals(0, img.children().size());
        assertEquals(0, img.childNodes().size());
    }

    @Test
    public void testShadowElementsAreUpdated() {
        String html = "<div><p><a>One</a></p><p><a>Two</a></p>Three</div><span>Four</span><foo></foo><img>";
        Document doc = Jsoup.parse(html);
        Element div = doc.select("div").first();
        Elements els = div.children();
        List<Node> nodes = div.childNodes();

        assertEquals(2, els.size()); // the two Ps
        assertEquals(3, nodes.size()); // the "Three" textnode

        Element p3 = new Element("p").text("P3");
        Element p4 = new Element("p").text("P4");
        div.insertChildren(1, p3);
        div.insertChildren(3, p4);
        Elements els2 = div.children();

        // first els should not have changed
        assertEquals(2, els.size());
        assertEquals(4, els2.size());

        assertEquals("<p><a>One</a></p>\n" +
            "<p>P3</p>\n" +
            "<p><a>Two</a></p>\n" +
            "<p>P4</p>Three", div.html());
        assertEquals("P3", els2.get(1).text());
        assertEquals("P4", els2.get(3).text());

        p3.after("<span>Another</span");

        Elements els3 = div.children();
        assertEquals(5, els3.size());
        assertEquals("span", els3.get(2).tagName());
        assertEquals("Another", els3.get(2).text());

        assertEquals("<p><a>One</a></p>\n" +
            "<p>P3</p><span>Another</span>\n" +
            "<p><a>Two</a></p>\n" +
            "<p>P4</p>Three", div.html());
    }

    @Test
    public void classNamesAndAttributeNameIsCaseInsensitive() {
        String html = "<p Class='SomeText AnotherText'>One</p>";
        Document doc = Jsoup.parse(html);
        Element p = doc.select("p").first();
        assertEquals("SomeText AnotherText", p.className());
        assertTrue(p.classNames().contains("SomeText"));
        assertTrue(p.classNames().contains("AnotherText"));
        assertTrue(p.hasClass("SomeText"));
        assertTrue(p.hasClass("sometext"));
        assertTrue(p.hasClass("AnotherText"));
        assertTrue(p.hasClass("anothertext"));

        Element p1 = doc.select(".SomeText").first();
        Element p2 = doc.select(".sometext").first();
        Element p3 = doc.select("[class=SomeText AnotherText]").first();
        Element p4 = doc.select("[Class=SomeText AnotherText]").first();
        Element p5 = doc.select("[class=sometext anothertext]").first();
        Element p6 = doc.select("[class=SomeText AnotherText]").first();
        Element p7 = doc.select("[class^=sometext]").first();
        Element p8 = doc.select("[class$=nothertext]").first();
        Element p9 = doc.select("[class^=sometext]").first();
        Element p10 = doc.select("[class$=AnotherText]").first();

        assertEquals("One", p1.text());
        assertEquals(p1, p2);
        assertEquals(p1, p3);
        assertEquals(p1, p4);
        assertEquals(p1, p5);
        assertEquals(p1, p6);
        assertEquals(p1, p7);
        assertEquals(p1, p8);
        assertEquals(p1, p9);
        assertEquals(p1, p10);
    }

    @Test
    public void testAppendTo() {
        String parentHtml = "<div class='a'></div>";
        String childHtml = "<div class='b'></div><p>Two</p>";

        Document parentDoc = Jsoup.parse(parentHtml);
        Element parent = parentDoc.body();
        Document childDoc = Jsoup.parse(childHtml);

        Element div = childDoc.select("div").first();
        Element p = childDoc.select("p").first();
        Element appendTo1 = div.appendTo(parent);
        assertEquals(div, appendTo1);

        Element appendTo2 = p.appendTo(div);
        assertEquals(p, appendTo2);

        assertEquals("<div class=\"a\"></div>\n<div class=\"b\">\n <p>Two</p>\n</div>", parentDoc.body().html());
        assertEquals("", childDoc.body().html()); // got moved out
    }

    @Test
    public void testNormalizesNbspInText() {
        String escaped = "You can't always get what you&nbsp;want.";
        String withNbsp = "You can't always get what you want."; // there is an nbsp char in there
        Document doc = Jsoup.parse("<p>" + escaped);
        Element p = doc.select("p").first();
        assertEquals("You can't always get what you want.", p.text()); // text is normalized

        assertEquals("<p>" + escaped + "</p>", p.outerHtml()); // html / whole text keeps &nbsp;
        assertEquals(withNbsp, p.textNodes().get(0).getWholeText());
        assertEquals(160, withNbsp.charAt(29));

        Element matched = doc.select("p:contains(get what you want)").first();
        assertEquals("p", matched.nodeName());
        assertTrue(matched.is(":containsOwn(get what you want)"));
    }

    @Test
    public void testNormalizesInvisiblesInText() {
        String escaped = "This&shy;is&#x200b;one&shy;long&shy;word";
        String decoded = "This\u00ADis\u200Bone\u00ADlong\u00ADword"; // browser would not display those soft hyphens / other chars, so we don't want them in the text

        Document doc = Jsoup.parse("<p>" + escaped);
        Element p = doc.select("p").first();
        doc.outputSettings().charset("ascii"); // so that the outer html is easier to see with escaped invisibles
        assertEquals("Thisisonelongword", p.text()); // text is normalized
        assertEquals("<p>" + escaped + "</p>", p.outerHtml()); // html / whole text keeps &shy etc;
        assertEquals(decoded, p.textNodes().get(0).getWholeText());

        Element matched = doc.select("p:contains(Thisisonelongword)").first(); // really just oneloneword, no invisibles
        assertEquals("p", matched.nodeName());
        assertTrue(matched.is(":containsOwn(Thisisonelongword)"));

    }

    @Test
    public void testRemoveBeforeIndex() {
        Document doc = Jsoup.parse(
            "<html><body><div><p>before1</p><p>before2</p><p>XXX</p><p>after1</p><p>after2</p></div></body></html>",
            "");
        Element body = doc.select("body").first();
        Elements elems = body.select("p:matchesOwn(XXX)");
        Element xElem = elems.first();
        Elements beforeX = xElem.parent().getElementsByIndexLessThan(xElem.elementSiblingIndex());

        for (Element p : beforeX) {
            p.remove();
        }

        assertEquals("<body><div><p>XXX</p><p>after1</p><p>after2</p></div></body>", TextUtil.stripNewlines(body.outerHtml()));
    }

    @Test
    public void testRemoveAfterIndex() {
        Document doc2 = Jsoup.parse(
            "<html><body><div><p>before1</p><p>before2</p><p>XXX</p><p>after1</p><p>after2</p></div></body></html>",
            "");
        Element body = doc2.select("body").first();
        Elements elems = body.select("p:matchesOwn(XXX)");
        Element xElem = elems.first();
        Elements afterX = xElem.parent().getElementsByIndexGreaterThan(xElem.elementSiblingIndex());

        for (Element p : afterX) {
            p.remove();
        }

        assertEquals("<body><div><p>before1</p><p>before2</p><p>XXX</p></div></body>", TextUtil.stripNewlines(body.outerHtml()));
    }

    @Test
    public void whiteSpaceClassElement() {
        Tag tag = Tag.valueOf("a");
        Attributes attribs = new Attributes();
        Element el = new Element(tag, "", attribs);

        attribs.put("class", "abc ");
        boolean hasClass = el.hasClass("ab");
        assertFalse(hasClass);
    }

    @Test
    public void testNextElementSiblingAfterClone() {
        // via https://github.com/jhy/jsoup/issues/951
        String html = "<!DOCTYPE html><html lang=\"en\"><head></head><body><div>Initial element</div></body></html>";
        String expectedText = "New element";
        String cloneExpect = "New element in clone";

        Document original = Jsoup.parse(html);
        Document clone = original.clone();

        Element originalElement = original.body().child(0);
        originalElement.after("<div>" + expectedText + "</div>");
        Element originalNextElementSibling = originalElement.nextElementSibling();
        Element originalNextSibling = (Element) originalElement.nextSibling();
        assertEquals(expectedText, originalNextElementSibling.text());
        assertEquals(expectedText, originalNextSibling.text());

        Element cloneElement = clone.body().child(0);
        cloneElement.after("<div>" + cloneExpect + "</div>");
        Element cloneNextElementSibling = cloneElement.nextElementSibling();
        Element cloneNextSibling = (Element) cloneElement.nextSibling();
        assertEquals(cloneExpect, cloneNextElementSibling.text());
        assertEquals(cloneExpect, cloneNextSibling.text());
    }

    @Test
    public void testRemovingEmptyClassAttributeWhenLastClassRemoved() {
        // https://github.com/jhy/jsoup/issues/947
        Document doc = Jsoup.parse("<img class=\"one two\" />");
        Element img = doc.select("img").first();
        img.removeClass("one");
        img.removeClass("two");
        assertFalse(doc.body().html().contains("class=\"\""));
    }

    @Test
    public void booleanAttributeOutput() {
        Document doc = Jsoup.parse("<img src=foo noshade='' nohref async=async autofocus=false>");
        Element img = doc.selectFirst("img");

        assertEquals("<img src=\"foo\" noshade nohref async autofocus=\"false\">", img.outerHtml());
    }

    @Test
    public void textHasSpaceAfterBlockTags() {
        Document doc = Jsoup.parse("<div>One</div>Two");
        assertEquals("One Two", doc.text());
    }

    @Test
    public void textHasSpaceBetweenDivAndCenterTags() {
        Document doc = Jsoup.parse("<div>One</div><div>Two</div><center>Three</center><center>Four</center>");
        assertEquals("One Two Three Four", doc.text());
    }

    @Test
    public void testNextElementSiblings() {
        Document doc = Jsoup.parse("<ul id='ul'>" +
            "<li id='a'>a</li>" +
            "<li id='b'>b</li>" +
            "<li id='c'>c</li>" +
            "</ul> Not An Element but a node" +
            "<div id='div'>" +
            "<li id='d'>d</li>" +
            "</div>");

        Element element = doc.getElementById("a");
        Elements elementSiblings = element.nextElementSiblings();
        assertNotNull(elementSiblings);
        assertEquals(2, elementSiblings.size());
        assertEquals("b", elementSiblings.get(0).id());
        assertEquals("c", elementSiblings.get(1).id());

        Element element1 = doc.getElementById("b");
        List<Element> elementSiblings1 = element1.nextElementSiblings();
        assertNotNull(elementSiblings1);
        assertEquals(1, elementSiblings1.size());
        assertEquals("c", elementSiblings1.get(0).id());

        Element element2 = doc.getElementById("c");
        List<Element> elementSiblings2 = element2.nextElementSiblings();
        assertEquals(0, elementSiblings2.size());

        Element ul = doc.getElementById("ul");
        List<Element> elementSiblings3 = ul.nextElementSiblings();
        assertNotNull(elementSiblings3);
        assertEquals(1, elementSiblings3.size());
        assertEquals("div", elementSiblings3.get(0).id());

        Element div = doc.getElementById("div");
        List<Element> elementSiblings4 = div.nextElementSiblings();
        assertEquals(0, elementSiblings4.size());
    }

    @Test
    public void testPreviousElementSiblings() {
        Document doc = Jsoup.parse("<ul id='ul'>" +
            "<li id='a'>a</li>" +
            "<li id='b'>b</li>" +
            "<li id='c'>c</li>" +
            "</ul>" +
            "<div id='div'>" +
            "<li id='d'>d</li>" +
            "</div>");

        Element element = doc.getElementById("b");
        Elements elementSiblings = element.previousElementSiblings();
        assertNotNull(elementSiblings);
        assertEquals(1, elementSiblings.size());
        assertEquals("a", elementSiblings.get(0).id());

        Element element1 = doc.getElementById("a");
        List<Element> elementSiblings1 = element1.previousElementSiblings();
        assertEquals(0, elementSiblings1.size());

        Element element2 = doc.getElementById("c");
        List<Element> elementSiblings2 = element2.previousElementSiblings();
        assertNotNull(elementSiblings2);
        assertEquals(2, elementSiblings2.size());
        assertEquals("b", elementSiblings2.get(0).id());
        assertEquals("a", elementSiblings2.get(1).id());

        Element ul = doc.getElementById("ul");
        List<Element> elementSiblings3 = ul.previousElementSiblings();
        assertEquals(0, elementSiblings3.size());
    }

    @Test
    public void testClearAttributes() {
        Element el = new Element("a").attr("href", "http://example.com").text("Hello");
        assertEquals("<a href=\"http://example.com\">Hello</a>", el.outerHtml());
        Element el2 = el.clearAttributes(); // really just force testing the return type is Element
        assertSame(el, el2);
        assertEquals("<a>Hello</a>", el2.outerHtml());
    }

    @Test
    public void testRemoveAttr() {
        Element el = new Element("a")
            .attr("href", "http://example.com")
            .attr("id", "1")
            .text("Hello");
        assertEquals("<a href=\"http://example.com\" id=\"1\">Hello</a>", el.outerHtml());
        Element el2 = el.removeAttr("href"); // really just force testing the return type is Element
        assertSame(el, el2);
        assertEquals("<a id=\"1\">Hello</a>", el2.outerHtml());
    }

    @Test
    public void testRoot() {
        Element el = new Element("a");
        el.append("<span>Hello</span>");
        assertEquals("<a><span>Hello</span></a>", el.outerHtml());
        Element span = el.selectFirst("span");
        assertNotNull(span);
        Element el2 = span.root();
        assertSame(el, el2);

        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three");
        Element div = doc.selectFirst("div");
        assertSame(doc, div.root());
        assertSame(doc, div.ownerDocument());
    }

    @Test
    public void testTraverse() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three");
        Element div = doc.selectFirst("div");
        assertNotNull(div);
        final AtomicInteger counter = new AtomicInteger(0);

        Element div2 = div.traverse(new NodeVisitor() {

            @Override
            public void head(Node node, int depth) {
                counter.incrementAndGet();
            }

            @Override
            public void tail(Node node, int depth) {

            }
        });

        assertEquals(7, counter.get());
        assertEquals(div2, div);
    }

    @Test void testTraverseLambda() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three");
        Element div = doc.selectFirst("div");
        assertNotNull(div);
        final AtomicInteger counter = new AtomicInteger(0);

        Element div2 = div.traverse((node, depth) -> counter.incrementAndGet());

        assertEquals(7, counter.get());
        assertEquals(div2, div);
    }

    @Test
    public void testFilterCallReturnsElement() {
        // doesn't actually test the filter so much as the return type for Element. See node.nodeFilter for an actual test
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three");
        Element div = doc.selectFirst("div");
        assertNotNull(div);
        Element div2 = div.filter(new NodeFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        });

        assertSame(div, div2);
    }

    @Test void testFilterAsLambda() {
        Document doc = Jsoup.parse("<div><p>One<p id=2>Two<p>Three");
        doc.filter((node, depth) -> node.attr("id").equals("2")
            ? NodeFilter.FilterResult.REMOVE
            : NodeFilter.FilterResult.CONTINUE);

        assertEquals("<div><p>One</p><p>Three</p></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test void testForEach() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div><div id=1>Gone<p></div>");
        doc.forEach(el -> {
            if (el.id().equals("1"))
                el.remove();
            else if (el.text().equals("There")) {
                el.text("There Now");
                el.append("<p>Another</p>");
            }
        });
        assertEquals("<div><p>Hello</p></div><div>There Now<p>Another</p></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void doesntDeleteZWJWhenNormalizingText() {
        String text = "\uD83D\uDC69\u200D\uD83D\uDCBB\uD83E\uDD26\uD83C\uDFFB\u200D\u2642\uFE0F";

        Document doc = Jsoup.parse("<p>" + text + "</p><div>One&zwj;Two</div>");
        Element p = doc.selectFirst("p");
        Element d = doc.selectFirst("div");

        assertEquals(12, p.text().length());
        assertEquals(text, p.text());
        assertEquals(7, d.text().length());
        assertEquals("One\u200DTwo", d.text());
        Element found = doc.selectFirst("div:contains(One\u200DTwo)");
        assertTrue(found.hasSameValue(d));
    }

    @Test
    public void testReparentSeperateNodes() {
        String html = "<div><p>One<p>Two";
        Document doc = Jsoup.parse(html);
        Element new1 = new Element("p").text("Three");
        Element new2 = new Element("p").text("Four");

        doc.body().insertChildren(-1, new1, new2);
        assertEquals("<div><p>One</p><p>Two</p></div><p>Three</p><p>Four</p>", TextUtil.stripNewlines(doc.body().html()));

        // note that these get moved from the above - as not copied
        doc.body().insertChildren(0, new1, new2);
        assertEquals("<p>Three</p><p>Four</p><div><p>One</p><p>Two</p></div>", TextUtil.stripNewlines(doc.body().html()));

        doc.body().insertChildren(0, new2.clone(), new1.clone());
        assertEquals("<p>Four</p><p>Three</p><p>Three</p><p>Four</p><div><p>One</p><p>Two</p></div>", TextUtil.stripNewlines(doc.body().html()));

        // shifted to end
        doc.body().appendChild(new1);
        assertEquals("<p>Four</p><p>Three</p><p>Four</p><div><p>One</p><p>Two</p></div><p>Three</p>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testNotActuallyAReparent() {
        // prep
        String html = "<div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.selectFirst("div");
        Element new1 = new Element("p").text("One");
        Element new2 = new Element("p").text("Two");
        div.addChildren(new1, new2);

        assertEquals("<div><p>One</p><p>Two</p></div>", TextUtil.stripNewlines(div.outerHtml()));

        // and the issue setup:
        Element new3 = new Element("p").text("Three");
        Element wrap = new Element("nav");
        wrap.addChildren(0, new1, new3);

        assertEquals("<nav><p>One</p><p>Three</p></nav>", TextUtil.stripNewlines(wrap.outerHtml()));
        div.addChildren(wrap);
        // now should be that One moved into wrap, leaving Two in div.

        assertEquals("<div><p>Two</p><nav><p>One</p><p>Three</p></nav></div>", TextUtil.stripNewlines(div.outerHtml()));
        assertEquals("<div><p>Two</p><nav><p>One</p><p>Three</p></nav></div>", TextUtil.stripNewlines(div.outerHtml()));
    }

    @Test
    public void testChildSizeWithMixedContent() {
        Document doc = Jsoup.parse("<table><tbody>\n<tr>\n<td>15:00</td>\n<td>sport</td>\n</tr>\n</tbody></table>");
        Element row = doc.selectFirst("table tbody tr");
        assertEquals(2, row.childrenSize());
        assertEquals(5, row.childNodeSize());
    }

    @Test
    public void isBlock() {
        String html = "<div><p><span>Hello</span>";
        Document doc = Jsoup.parse(html);
        assertTrue(doc.selectFirst("div").isBlock());
        assertTrue(doc.selectFirst("p").isBlock());
        assertFalse(doc.selectFirst("span").isBlock());
    }

    @Test
    public void testScriptTextHtmlSetAsData() {
        String src = "var foo = 5 < 2;\nvar bar = 1 && 2;";
        String html = "<script>" + src + "</script>";
        Document doc = Jsoup.parse(html);
        Element el = doc.selectFirst("script");
        assertNotNull(el);

        validateScriptContents(src, el);

        src = "var foo = 4 < 2;\nvar bar > 1 && 2;";
        el.html(src);
        validateScriptContents(src, el);

        // special case for .text (in HTML; in XML will just be regular text)
        el.text(src);
        validateScriptContents(src, el);

        // XML, no special treatment, get escaped correctly
        Document xml = Parser.xmlParser().parseInput(html, "");
        Element xEl = xml.selectFirst("script");
        assertNotNull(xEl);
        src = "var foo = 5 < 2;\nvar bar = 1 && 2;";
        String escaped = "var foo = 5 &lt; 2;\nvar bar = 1 &amp;&amp; 2;";
        validateXmlScriptContents(xEl);
        xEl.text(src);
        validateXmlScriptContents(xEl);
        xEl.html(src);
        validateXmlScriptContents(xEl);

        assertEquals("<script>var foo = 4 < 2;\nvar bar > 1 && 2;</script>", el.outerHtml());
        assertEquals("<script>" + escaped + "</script>", xEl.outerHtml()); // escaped in xml as no special treatment

    }

    @Test
    public void testShallowCloneToString() {
        // https://github.com/jhy/jsoup/issues/1410
        Document doc = Jsoup.parse("<p><i>Hello</i></p>");
        Element p = doc.selectFirst("p");
        Element i = doc.selectFirst("i");
        String pH = p.shallowClone().toString();
        String iH = i.shallowClone().toString();

        assertEquals("<p></p>", pH); // shallow, so no I
        assertEquals("<i></i>", iH);

        assertEquals(p.outerHtml(), p.toString());
        assertEquals(i.outerHtml(), i.toString());
    }

    @Test
    public void styleHtmlRoundTrips() {
        String styleContents = "foo < bar > qux {color:white;}";
        String html = "<head><style>" + styleContents + "</style></head>";
        Document doc = Jsoup.parse(html);

        Element head = doc.head();
        Element style = head.selectFirst("style");
        assertNotNull(style);
        assertEquals(styleContents, style.html());
        style.html(styleContents);
        assertEquals(styleContents, style.html());
        assertEquals("", style.text());
        style.text(styleContents); // pushes the HTML, not the Text
        assertEquals("", style.text());
        assertEquals(styleContents, style.html());
    }

    @Test
    public void moveChildren() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three</div><div></div>");
        Elements divs = doc.select("div");
        Element a = divs.get(0);
        Element b = divs.get(1);

        b.insertChildren(-1, a.childNodes());

        assertEquals("<div></div>\n<div>\n <p>One</p>\n <p>Two</p>\n <p>Three</p>\n</div>",
            doc.body().html());
    }

    @Test
    public void moveChildrenToOuter() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three</div><div></div>");
        Elements divs = doc.select("div");
        Element a = divs.get(0);
        Element b = doc.body();

        b.insertChildren(-1, a.childNodes());

        assertEquals("<div></div>\n<div></div>\n<p>One</p>\n<p>Two</p>\n<p>Three</p>",
            doc.body().html());
    }

    @Test
    public void appendChildren() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three</div><div><p>Four</div>");
        Elements divs = doc.select("div");
        Element a = divs.get(0);
        Element b = divs.get(1);

        b.appendChildren(a.childNodes());

        assertEquals("<div></div>\n<div>\n <p>Four</p>\n <p>One</p>\n <p>Two</p>\n <p>Three</p>\n</div>",
            doc.body().html());
    }

    @Test
    public void prependChildren() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three</div><div><p>Four</div>");
        Elements divs = doc.select("div");
        Element a = divs.get(0);
        Element b = divs.get(1);

        b.prependChildren(a.childNodes());

        assertEquals("<div></div>\n<div>\n <p>One</p>\n <p>Two</p>\n <p>Three</p>\n <p>Four</p>\n</div>",
            doc.body().html());
    }

    @Test
    public void loopMoveChildren() {
        Document doc = Jsoup.parse("<div><p>One<p>Two<p>Three</div><div><p>Four</div>");
        Elements divs = doc.select("div");
        Element a = divs.get(0);
        Element b = divs.get(1);

        Element outer = b.parent();
        assertNotNull(outer);
        for (Node node : a.childNodes()) {
            outer.appendChild(node);
        }

        assertEquals("<div></div>\n<div>\n <p>Four</p>\n</div>\n<p>One</p>\n<p>Two</p>\n<p>Three</p>",
            doc.body().html());
    }

    @Test
    public void accessorsDoNotVivifyAttributes() throws NoSuchFieldException, IllegalAccessException {
        // internally, we don't want to create empty Attribute objects unless actually used for something
        Document doc = Jsoup.parse("<div><p><a href=foo>One</a>");
        Element div = doc.selectFirst("div");
        Element p = doc.selectFirst("p");
        Element a = doc.selectFirst("a");

        // should not create attributes
        assertEquals("", div.attr("href"));
        p.removeAttr("href");

        Elements hrefs = doc.select("[href]");
        assertEquals(1, hrefs.size());

        assertFalse(div.hasAttributes());
        assertFalse(p.hasAttributes());
        assertTrue(a.hasAttributes());
    }

    @Test
    public void childNodesAccessorDoesNotVivify() {
        Document doc = Jsoup.parse("<p></p>");
        Element p = doc.selectFirst("p");
        assertFalse(p.hasChildNodes());

        assertEquals(0, p.childNodeSize());
        assertEquals(0, p.childrenSize());

        List<Node> childNodes = p.childNodes();
        assertEquals(0, childNodes.size());

        Elements children = p.children();
        assertEquals(0, children.size());

        assertFalse(p.hasChildNodes());
    }

    @Test void emptyChildrenElementsIsModifiable() {
        // using unmodifiable empty in childElementList as short circuit, but people may be modifying Elements.
        Element p = new Element("p");
        Elements els = p.children();
        assertEquals(0, els.size());
        els.add(new Element("a"));
        assertEquals(1, els.size());
    }

    @Test public void attributeSizeDoesNotAutoVivify() {
        Document doc = Jsoup.parse("<p></p>");
        Element p = doc.selectFirst("p");
        assertNotNull(p);
        assertFalse(p.hasAttributes());
        assertEquals(0, p.attributesSize());
        assertFalse(p.hasAttributes());

        p.attr("foo", "bar");
        assertEquals(1, p.attributesSize());
        assertTrue(p.hasAttributes());

        p.removeAttr("foo");
        assertEquals(0, p.attributesSize());
    }

    @Test void clonedElementsHaveOwnerDocsAndIndependentSettings() {
        // https://github.com/jhy/jsoup/issues/763
        Document doc = Jsoup.parse("<div>Text</div><div>Two</div>");
        doc.outputSettings().prettyPrint(false);
        Element div = doc.selectFirst("div");
        assertNotNull(div);
        Node text = div.childNode(0);
        assertNotNull(text);

        Element divClone = div.clone();
        Document docClone = divClone.ownerDocument();
        assertNotNull(docClone);
        assertFalse(docClone.outputSettings().prettyPrint());
        assertNotSame(doc, docClone);
        assertSame(docClone, divClone.childNode(0).ownerDocument());
        // the cloned text has same owner doc as the cloned div

        doc.outputSettings().prettyPrint(true);
        assertTrue(doc.outputSettings().prettyPrint());
        assertFalse(docClone.outputSettings().prettyPrint());
        assertEquals(1, docClone.children().size()); // check did not get the second div as the owner's children
        assertEquals(divClone, docClone.child(0)); // note not the head or the body -- not normalized
    }

    private static Stream<Document.OutputSettings> testOutputSettings() {
        return Stream.of(
            new Document.OutputSettings().prettyPrint(true).indentAmount(4),
            new Document.OutputSettings().prettyPrint(true).indentAmount(1),
            new Document.OutputSettings().prettyPrint(true).indentAmount(4).outline(true),
            new Document.OutputSettings().prettyPrint(false)
        );
    }

    @ParameterizedTest
    @MethodSource("testOutputSettings")
    void prettySerializationRoundTrips(Document.OutputSettings settings) {
        // https://github.com/jhy/jsoup/issues/1688
        // tests that repeated html() and parse() does not accumulate errant spaces / newlines
        Document doc = Jsoup.parse("<div>\nFoo\n<p>\nBar\nqux</p></div>\n<script>\n alert('Hello!');\n</script>");
        doc.outputSettings(settings);
        String html = doc.html();
        Document doc2 = Jsoup.parse(html);
        doc2.outputSettings(settings);
        String html2 = doc2.html();

        assertEquals(html, html2);
    }

    @Test void prettyPrintScriptsDoesNotGrowOnRepeat() {
        Document doc = Jsoup.parse("<div>\nFoo\n<p>\nBar\nqux</p></div>\n<script>\n alert('Hello!');\n</script>");
        Document.OutputSettings settings = doc.outputSettings();
        settings
            .prettyPrint(true)
            .outline(true)
            .indentAmount(4)
            ;

        String html = doc.html();
        Document doc2 = Jsoup.parse(html);
        doc2.outputSettings(settings);
        String html2 = doc2.html();
        assertEquals(html, html2);
    }

    @Test void elementBrText() {
        // testcase for https://github.com/jhy/jsoup/issues/1437
        String html = "<p>Hello<br>World</p>";
        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false); // otherwise html serializes as Hello<br>\n World.
        Element p = doc.select("p").first();
        assertNotNull(p);
        assertEquals(html, p.outerHtml());
        assertEquals("Hello World", p.text());
        assertEquals("Hello\nWorld", p.wholeText());
    }

    @Test void wrapTextAfterBr() {
        // https://github.com/jhy/jsoup/issues/1858
        String html = "<p>Hello<br>there<br>now.</p>";
        Document doc = Jsoup.parse(html);
        assertEquals("<p>Hello<br>\n there<br>\n now.</p>", doc.body().html());
    }

    @Test void prettyprintBrInBlock() {
        String html = "<div><br> </div>";
        Document doc = Jsoup.parse(html);
        assertEquals("<div>\n <br>\n</div>", doc.body().html()); // not div\n br\n \n/div
    }

    @Test void preformatFlowsToChildTextNodes() {
        // https://github.com/jhy/jsoup/issues/1776
        String html = "<div><pre>One\n<span>\nTwo</span>\n <span>  \nThree</span>\n <span>Four <span>Five</span>\n  Six\n</pre>";
        Document doc = Jsoup.parse(html);
        doc.outputSettings().indentAmount(2).prettyPrint(true);

        Element div = doc.selectFirst("div");
        assertNotNull(div);
        String actual = div.outerHtml();
        String expect = "<div>\n" +
            "  <pre>One\n" +
            "<span>\n" +
            "Two</span>\n" +
            " <span>  \n" +
            "Three</span>\n" +
            " <span>Four <span>Five</span>\n" +
            "  Six\n" +
            "</span></pre>\n" +
            "</div>";
        assertEquals(expect, actual);

        String expectText = "One\n" +
            "\n" +
            "Two\n" +
            "   \n" +
            "Three\n" +
            " Four Five\n" +
            "  Six\n";
        assertEquals(expectText, div.wholeText());

        String expectOwn = "One\n" +
            "\n" +
            " \n" +
            " ";
        assertEquals(expectOwn, div.child(0).wholeOwnText());
    }

    @Test void testExpectFirst() {
        Document doc = Jsoup.parse("<p>One</p><p>Two <span>Three</span> <span>Four</span>");

        Element span = doc.expectFirst("span");
        assertEquals("Three", span.text());

        assertNull(doc.selectFirst("div"));
        boolean threw = false;
        try {
            Element div = doc.expectFirst("div");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test void testExpectFirstMessage() {
        Document doc = Jsoup.parse("<p>One</p><p>Two <span>Three</span> <span>Four</span>");
        boolean threw = false;
        Element p = doc.expectFirst("P");
        try {
            Element span = p.expectFirst("span.doesNotExist");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("No elements matched the query 'span.doesNotExist' on element 'p'.", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test void testExpectFirstMessageDoc() {
        Document doc = Jsoup.parse("<p>One</p><p>Two <span>Three</span> <span>Four</span>");
        boolean threw = false;
        Element p = doc.expectFirst("P");
        try {
            Element span = doc.expectFirst("span.doesNotExist");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("No elements matched the query 'span.doesNotExist' in the document.", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test void spanRunsMaintainSpace() {
        // https://github.com/jhy/jsoup/issues/1787
        Document doc = Jsoup.parse("<p><span>One</span>\n<span>Two</span>\n<span>Three</span></p>");
        String text = "One Two Three";
        Element body = doc.body();
        assertEquals(text, body.text());

        Element p = doc.expectFirst("p");
        String html = p.html();
        p.html(html);
        assertEquals(text, body.text());

        assertEquals("<p><span>One</span> <span>Two</span> <span>Three</span></p>", body.html());
    }

    @Test void doctypeIsPrettyPrinted() {
        // resolves underlying issue raised in https://github.com/jhy/jsoup/pull/1664
        Document doc1 = Jsoup.parse("<!--\nlicense\n-->\n \n<!doctype html>\n<html>");
        Document doc2 = Jsoup.parse("\n  <!doctype html><html>");
        Document doc3 = Jsoup.parse("<!doctype html>\n<html>");
        Document doc4 = Jsoup.parse("\n<!doctype html>\n<html>");
        Document doc5 = Jsoup.parse("\n<!--\n comment \n -->  <!doctype html>\n<html>");
        Document doc6 = Jsoup.parse("<!--\n comment \n -->  <!doctype html>\n<html>");

        assertEquals("<!--\nlicense\n-->\n<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>", doc1.html());
        doc1.outputSettings().prettyPrint(false);
        assertEquals("<!--\nlicense\n--><!doctype html>\n<html><head></head><body></body></html>", doc1.html());
        // note that the whitespace between the comment and the doctype is not retained, in Initial state

        assertEquals("<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>", doc2.html());
        assertEquals("<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>", doc3.html());
        assertEquals("<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>", doc4.html());
        assertEquals("<!--\n comment \n -->\n<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>", doc5.html());
        assertEquals("<!--\n comment \n -->\n<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>", doc6.html());
    }

    @Test void textnodeInBlockIndent() {
        String html ="<div>\n{{ msg }} \n </div>\n<div>\n{{ msg }} \n </div>";
        Document doc = Jsoup.parse(html);
        assertEquals("<div>\n {{ msg }}\n</div>\n<div>\n {{ msg }}\n</div>", doc.body().html());
    }

    @Test void stripTrailing() {
        String html = "<p> This <span>is </span>fine. </p>";
        Document doc = Jsoup.parse(html);
        assertEquals("<p>This <span>is </span>fine.</p>", doc.body().html());
    }

    @Test void elementIndentAndSpaceTrims() {
        String html = "<body><div> <p> One Two </p> <a>  Hello </a><p>\nSome text \n</p>\n </div>";
        Document doc = Jsoup.parse(html);
        assertEquals("<div>\n" +
            " <p>One Two</p><a> Hello </a>\n" +
            " <p>Some text</p>\n" +
            "</div>", doc.body().html());
    }

    @Test void divAInlineable() {
        String html = "<body><div> <a>Text</a>";
        Document doc = Jsoup.parse(html);
        assertEquals("<div><a>Text</a>\n</div>", doc.body().html());
    }

    @Test void noDanglingSpaceAfterCustomElement() {
        // https://github.com/jhy/jsoup/issues/1852
        String html = "<bar><p/>\n</bar>";
        Document doc = Jsoup.parse(html);
        assertEquals("<bar>\n <p></p>\n</bar>", doc.body().html());

        html = "<foo>\n  <bar />\n</foo>";
        doc = Jsoup.parse(html);
        assertEquals("<foo>\n <bar />\n</foo>", doc.body().html());
    }

    @Test void spanInBlockTrims() {
        String html = "<p>Lorem ipsum</p>\n<span>Thanks</span>";
        Document doc = Jsoup.parse(html);
        String outHtml = doc.body().html();
        assertEquals("<p>Lorem ipsum</p><span>Thanks</span>", outHtml);
    }

    @Test void replaceWithSelf() {
        // https://github.com/jhy/jsoup/issues/1843
        Document doc = Jsoup.parse("<p>One<p>Two");
        Elements ps = doc.select("p");
        Element first = ps.first();

        assertNotNull(first);
        first.replaceWith(first);
        assertEquals(ps.get(1), first.nextSibling());
        assertEquals("<p>One</p>\n<p>Two</p>", first.parent().html());
    }

    @Test void select() {
        Evaluator eval = QueryParser.parse("div");
        Document doc = Jsoup.parse(reference);
        Elements els = doc.select("div");
        Elements els2 = doc.select(eval);
        assertEquals(els, els2);
    }

    @Test void insertChildrenValidation() {
        Document doc = Jsoup.parse(reference);
        Element div = doc.expectFirst("div");
        Throwable ex = assertThrows(ValidationException.class, () -> div.insertChildren(20, new Element("div")));
        assertEquals("Insert position out of bounds.", ex.getMessage());
    }

    @Test void cssSelectorNoDoc() {
        Element el = new Element("div");
        el.id("one");
        assertEquals("#one", el.cssSelector());
    }

    @Test void cssSelectorNoParent() {
        Element el = new Element("div");
        assertEquals("div", el.cssSelector());
    }

    @Test void orphanSiblings() {
        Element el = new Element("div");
        assertEquals(0, el.siblingElements().size());
        assertEquals(0, el.nextElementSiblings().size());
        assertEquals(0, el.previousElementSiblings().size());
        assertNull(el.nextElementSibling());
        assertNull(el.previousElementSibling());
    }

    @Test void getElementsByAttributeStarting() {
        Document doc = Jsoup.parse("<div data-one=1 data-two=2 id=1><p data-one=3 id=2>Text</div><div>");
        Elements els = doc.getElementsByAttributeStarting(" data- ");
        assertEquals(2, els.size());
        assertEquals("1", els.get(0).id());
        assertEquals("2", els.get(1).id());
        assertEquals(0, doc.getElementsByAttributeStarting("not-data").size());
    }

    @Test void getElementsByAttributeValueNot() {
        Document doc = Jsoup.parse("<div data-one=1 data-two=2 id=1><p data-one=3 id=2>Text</div><div id=3>");
        Elements els = doc.body().getElementsByAttributeValueNot("data-one", "1");
        assertEquals(3, els.size()); // the body, p, and last div
        assertEquals("body", els.get(0).normalName());
        assertEquals("2", els.get(1).id());
        assertEquals("3", els.get(2).id());
    }

    @Test void getElementsByAttributeValueStarting() {
        Document doc = Jsoup.parse("<a href=one1></a><a href=one2></a><a href=else</a>");
        Elements els = doc.getElementsByAttributeValueStarting("href", "one");
        assertEquals(2, els.size());
        assertEquals("one1", els.get(0).attr("href"));
        assertEquals("one2", els.get(1).attr("href"));
    }

    @Test void getElementsByAttributeValueEnding() {
        Document doc = Jsoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>");
        Elements els = doc.getElementsByAttributeValueEnding("href", "one");
        assertEquals(2, els.size());
        assertEquals("1one", els.get(0).attr("href"));
        assertEquals("2one", els.get(1).attr("href"));
    }

    @Test void getElementsByAttributeValueContaining() {
        Document doc = Jsoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>");
        Elements els = doc.getElementsByAttributeValueContaining("href", "on");
        assertEquals(2, els.size());
        assertEquals("1one", els.get(0).attr("href"));
        assertEquals("2one", els.get(1).attr("href"));
    }

    @Test void getElementsByAttributeValueMatchingPattern() {
        Document doc = Jsoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>");
        Elements els = doc.getElementsByAttributeValueMatching("href", Pattern.compile("^\\d\\w+"));
        assertEquals(2, els.size());
        assertEquals("1one", els.get(0).attr("href"));
        assertEquals("2one", els.get(1).attr("href"));
    }

    @Test void getElementsByAttributeValueMatching() {
        Document doc = Jsoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>");
        Elements els = doc.getElementsByAttributeValueMatching("href", "^\\d\\w+");
        assertEquals(2, els.size());
        assertEquals("1one", els.get(0).attr("href"));
        assertEquals("2one", els.get(1).attr("href"));
    }

    @Test void getElementsByAttributeValueMatchingValidation() {
        Document doc = Jsoup.parse(reference);
        Throwable ex = assertThrows(IllegalArgumentException.class,
            () -> doc.getElementsByAttributeValueMatching("key", "\\x"));
        assertEquals("Pattern syntax error: \\x", ex.getMessage());
    }

    @Test void getElementsByIndexEquals() {
        Document doc = Jsoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>");
        Elements els = doc.body().getElementsByIndexEquals(1);
        assertEquals(2, els.size());
        assertEquals("body", els.get(0).normalName());
        assertEquals("2one", els.get(1).attr("href"));
    }

    @Test void getElementsContainingText() {
        Document doc = Jsoup.parse("<div id=1>One</div><div>Two</div>");
        Elements els = doc.body().getElementsContainingText("one");
        assertEquals(2, els.size());
        assertEquals("body", els.get(0).normalName());
        assertEquals("1", els.get(1).id());
    }

    @Test void getElementsContainingOwnText() {
        Document doc = Jsoup.parse("<div id=1>One</div><div>Two</div>");
        Elements els = doc.body().getElementsContainingOwnText("one");
        assertEquals(1, els.size());
        assertEquals("1", els.get(0).id());
    }

    @Test void getElementsMatchingTextValidation() {
        Document doc = Jsoup.parse(reference);
        Throwable ex = assertThrows(IllegalArgumentException.class,
            () -> doc.getElementsMatchingText("\\x"));
        assertEquals("Pattern syntax error: \\x", ex.getMessage());
    }

    @Test void getElementsMatchingText() {
        Document doc = Jsoup.parse("<div id=1>One</div><div>Two</div>");
        Elements els = doc.body().getElementsMatchingText("O\\w+");
        assertEquals(2, els.size());
        assertEquals("body", els.get(0).normalName());
        assertEquals("1", els.get(1).id());
    }

    @Test void getElementsMatchingOwnText() {
        Document doc = Jsoup.parse("<div id=1>One</div><div>Two</div>");
        Elements els = doc.body().getElementsMatchingOwnText("O\\w+");
        assertEquals(1, els.size());
        assertEquals("1", els.get(0).id());
    }

    @Test void getElementsMatchingOwnTextValidation() {
        Document doc = Jsoup.parse(reference);
        Throwable ex = assertThrows(IllegalArgumentException.class,
            () -> doc.getElementsMatchingOwnText("\\x"));
        assertEquals("Pattern syntax error: \\x", ex.getMessage());
    }

    @Test void hasText() {
        Document doc = Jsoup.parse("<div id=1><p><i>One</i></p></div><div id=2>Two</div><div id=3><script>data</script> </div>");
        assertTrue(doc.getElementById("1").hasText());
        assertTrue(doc.getElementById("2").hasText());
        assertFalse(doc.getElementById("3").hasText());
    }

    @Test void dataInCdataNode() {
        Element el = new Element("div");
        CDataNode cdata = new CDataNode("Some CData");
        el.appendChild(cdata);
        assertEquals("Some CData", el.data());

        Document parse = Jsoup.parse("One <![CDATA[Hello]]>");
        assertEquals("Hello", parse.data());
    }

    @Test void outerHtmlAppendable() {
        // tests not string builder flow
        Document doc = Jsoup.parse("<div>One</div>");
        StringBuffer buffer = new StringBuffer();
        doc.body().outerHtml(buffer);
        assertEquals("\n<body>\n <div>\n  One\n </div>\n</body>", buffer.toString());
        StringBuilder builder = new StringBuilder();
        doc.body().outerHtml(builder);
        assertEquals("<body>\n <div>\n  One\n </div>\n</body>", builder.toString());
    }
}