package org.jsoup.parser;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static org.jsoup.parser.Parser.NamespaceHtml;
import static org.junit.jupiter.api.Assertions.*;

public class HtmlTreeBuilderTest {
    @Test
    public void ensureSearchArraysAreSorted() {
        List<Object[]> treeBuilderArrays = HtmlTreeBuilderStateTest.findConstantArrays(HtmlTreeBuilder.class);
        HtmlTreeBuilderStateTest.ensureSorted(treeBuilderArrays);
        assertEquals(3, treeBuilderArrays.size());

        List<Object[]> tagOptionArrays = HtmlTreeBuilderStateTest.findConstantArrays(HtmlTagOptions.class);
        HtmlTreeBuilderStateTest.ensureSorted(tagOptionArrays);
        assertEquals(10, tagOptionArrays.size());
    }

    @Test
    public void scopeSearchesMatchSpecBoundaries() {
        ParseSettings settings = ParseSettings.htmlDefault;
        assertTrue(Tag.valueOf("select", NamespaceHtml, settings).hasParserOption(HtmlTagOptions.Scope));
        assertTrue(Tag.valueOf("template", NamespaceHtml, settings).hasParserOption(HtmlTagOptions.TableScope));
    }

    @Test
    public void nonnull() {
        assertThrows(IllegalArgumentException.class, () -> {
                HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
                treeBuilder.parse(null, null, null); // not sure how to test that these visual warnings actually appear! - test below checks for method annotation
            }
        ); // I'm not convinced that this lambda is easier to read than the old Junit 4 @Test(expected=IEA.class)...
    }

    @Test public void nonnullAssertions() throws NoSuchMethodException {
        Annotation[] declaredAnnotations = TreeBuilder.class.getPackage().getDeclaredAnnotations();
        boolean seen = false;
        for (Annotation annotation : declaredAnnotations) {
            if (annotation.annotationType().isAssignableFrom(NullMarked.class))
                seen = true;
        }

        // would need to rework this if/when that annotation moves from the method to the class / package.
        assertTrue(seen);
    }

    @Test void tracksParseLifecycle() throws IOException {
        Parser parser = Parser.htmlParser();
        TreeBuilder treeBuilder = parser.getTreeBuilder();
        assertFalse(treeBuilder.isComplete());

        try (StreamParser streamParser = new StreamParser(parser).parse("<title>One</title><p id=hit>Full</p>", "")) {
            streamParser.expectFirst("title");
            Element open = streamParser.document().expectFirst("#hit");
            assertTrue(treeBuilder.isOpen(open));
            assertFalse(treeBuilder.isOpen(treeBuilder.doc));

            List<Element> openElements = new ArrayList<>();
            treeBuilder.copyOpenElementsTo(openElements);
            assertTrue(openElements.contains(open));
            assertFalse(openElements.contains(treeBuilder.doc));

            // closing before EOF releases the open stack without marking the document complete
            streamParser.close();
            assertTrue(treeBuilder.stack.isEmpty());
            assertFalse(treeBuilder.isComplete());

            streamParser.parse("<p>Complete</p>", "");
            assertFalse(treeBuilder.isComplete());
            streamParser.complete();
            assertTrue(treeBuilder.stack.isEmpty());
            assertTrue(treeBuilder.isComplete());
        }
    }

    @Test void isSpecial() {
        ParseSettings settings = ParseSettings.htmlDefault;
        Element htmlEl = new Element(Tag.valueOf("div", NamespaceHtml, settings), "");
        assertTrue(HtmlTreeBuilder.isSpecial(htmlEl));

        Element notHtml = new Element(Tag.valueOf("not-html", NamespaceHtml, settings), "");
        assertFalse(HtmlTreeBuilder.isSpecial(notHtml));

        Element mathEl = new Element(Tag.valueOf("mi", Parser.NamespaceMathml, settings), "");
        assertTrue(HtmlTreeBuilder.isSpecial(mathEl));

        Element notMathEl = new Element(Tag.valueOf("not-math", Parser.NamespaceMathml, settings), "");
        assertFalse(HtmlTreeBuilder.isSpecial(notMathEl));

        Element svgEl = new Element(Tag.valueOf("title", Parser.NamespaceSvg, settings), "");
        assertTrue(HtmlTreeBuilder.isSpecial(svgEl));

        Element svgForeignObject = Jsoup.parse("<svg><foreignObject></foreignObject></svg>").expectFirst("foreignObject");
        assertTrue(HtmlTreeBuilder.isSpecial(svgForeignObject));

        Element notSvgEl = new Element(Tag.valueOf("not-svg", Parser.NamespaceSvg, settings), "");
        assertFalse(HtmlTreeBuilder.isSpecial(notSvgEl));
    }

    @Test void parserOptionsTrackTagMutation() {
        Tag tag = new Tag("not-html", NamespaceHtml);
        Element el = new Element(tag, "");
        assertFalse(HtmlTreeBuilder.isSpecial(el));

        Tag direct = new Tag("div", NamespaceHtml);
        assertTrue(HtmlTreeBuilder.isSpecial(new Element(direct, "")));

        tag.name("div");
        assertTrue(HtmlTreeBuilder.isSpecial(el));

        tag.namespace(Parser.NamespaceMathml);
        assertFalse(HtmlTreeBuilder.isSpecial(el));

        tag.name("mi");
        assertTrue(HtmlTreeBuilder.isSpecial(el));
    }

    @Test void parserOptionsAreNamespaceAware() {
        Tag htmlOption = Tag.valueOf("option", NamespaceHtml, ParseSettings.htmlDefault);
        assertTrue(htmlOption.hasParserOption(HtmlTagOptions.ImpliedEnd));
        assertTrue(htmlOption.hasParserOption(HtmlTagOptions.SelectScopeMember));

        Tag svgOption = Tag.valueOf("option", Parser.NamespaceSvg, ParseSettings.htmlDefault);
        assertFalse(svgOption.hasParserOption(HtmlTagOptions.ImpliedEnd));
        assertFalse(svgOption.hasParserOption(HtmlTagOptions.SelectScopeMember));
    }

    @Test void impliedEndTagsOnlyPopHtmlElements() {
        // same-named foreign elements do not participate in HTML's implied end tag rules
        HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
        for (String namespace : new String[]{Parser.NamespaceSvg, Parser.NamespaceMathml}) {
            Element foreignOption = new Element(new Tag("option", namespace), "");
            Element htmlOption = new Element(new Tag("option", NamespaceHtml), "");
            treeBuilder.stack.add(foreignOption);
            treeBuilder.stack.add(htmlOption);

            treeBuilder.generateImpliedEndTags("p");

            assertEquals(1, treeBuilder.stack.size(), namespace);
            assertSame(foreignOption, treeBuilder.currentElement(), namespace);
            treeBuilder.stack.clear();
        }
    }

    @Test void customRcdataTag() {
        String inner = "Blah\nblah\n<foo>Foo</foo>\n&quot;";
        String innerText = "Blah\nblah\n<foo>Foo</foo>\n\"";
        String html = "<div><x>" + inner + "</x></div><div><x id=2></x></div>";
        TagSet custom = TagSet.Html();
        Tag x = custom.valueOf("x", NamespaceHtml);
        x.set(Tag.RcData);

        Document doc = Jsoup.parse(html, Parser.htmlParser().tagSet(custom));
        Element xEl = doc.expectFirst("x");
        assertEquals(x, xEl.tag());
        assertEquals(innerText, xEl.wholeText()); // <foo> is text no el

        // fragment parse context
        Element x2 = doc.expectFirst("#2");
        x2.html(inner); // <foo> will be text not el, via custom fragment context element
        assertEquals(innerText, x2.wholeText());
    }

    @Test void customDataTag() {
        String inner = "Blah\nblah\n<foo>Foo</foo>\n&quot;"; // no character refs, will be as-is
        String html = "<div><x>" + inner + "</x></div><div><x id=2></x></div>";
        TagSet custom = TagSet.Html();
        Tag x = custom.valueOf("x", NamespaceHtml);
        x.set(Tag.Data);

        Document doc = Jsoup.parse(html, Parser.htmlParser().tagSet(custom));
        Element xEl = doc.expectFirst("x");
        assertEquals(x, xEl.tag());
        assertEquals(inner, xEl.data());

        // fragment parse context
        Element x2 = doc.expectFirst("#2");
        x2.html(inner); // <foo> will be text not el, via custom fragment context element
        assertEquals(inner, xEl.data());
    }
}
