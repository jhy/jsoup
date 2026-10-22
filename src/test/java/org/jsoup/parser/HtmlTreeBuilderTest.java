package org.jsoup.parser;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.jsoup.parser.HtmlTreeBuilder.MaxFormattingElements;
import static org.jsoup.parser.Parser.NamespaceHtml;
import static org.junit.jupiter.api.Assertions.*;

public class HtmlTreeBuilderTest {
    @Nested class FormattingHistory {
        private final HtmlTreeBuilder tb = new HtmlTreeBuilder();

        @BeforeEach void initialise() {
            tb.initialiseParse(new StringReader(""), "", new Parser(tb));
            tb.initialiseParseFragment(new Element("div"));
        }

        @AfterEach void close() {
            tb.closeParse();
        }

        @ParameterizedTest
        @ValueSource(ints = {MaxFormattingElements - 1, MaxFormattingElements, MaxFormattingElements + 1, MaxFormattingElements * 4})
        void limitsFormattingHistoryWithoutRemovingDomNodes(int count) {
            Element parent = new Element("div");
            for (int i = 0; i < count; i++) {
                Element el = parent.appendElement("b").attr("x", Integer.toString(i));
                tb.pushActiveFormattingElements(el);
                assertEquals(Math.min(i + 1, MaxFormattingElements), tb.formattingElements.size());
            }

            int retained = Math.min(count, MaxFormattingElements);
            assertEquals(count, parent.childrenSize());
            for (int i = 0; i < retained; i++)
                assertSame(parent.child(count - retained + i), tb.formattingElements.get(i));
        }

        @Test void limitsFormattingMarkers() {
            for (int i = 0; i < MaxFormattingElements * 4; i++) {
                tb.insertMarkerToFormattingElements();
                assertEquals(Math.min(i + 1, MaxFormattingElements), tb.formattingElements.size());
                assertNull(tb.lastFormattingElement());
            }
            tb.clearFormattingElementsToLastMarker();
            assertEquals(MaxFormattingElements - 1, tb.formattingElements.size());
            Element recent = new Element("b");
            tb.pushActiveFormattingElements(recent);
            assertSame(recent, tb.getActiveFormattingElement("b"));
            tb.clearFormattingElementsToLastMarker();
            assertEquals(MaxFormattingElements - 2, tb.formattingElements.size());
        }

        @Test void formattingOverflowPreservesRetainedMarkerBoundaries() {
            tb.pushActiveFormattingElements(new Element("b"));
            tb.insertMarkerToFormattingElements();
            for (int i = 0; i < MaxFormattingElements - 3; i++)
                tb.pushActiveFormattingElements(new Element("i").attr("x", Integer.toString(i)));
            Element first = tb.formattingElements.get(2);
            Element last = tb.lastFormattingElement();
            tb.insertMarkerToFormattingElements();
            tb.pushActiveFormattingElements(new Element("u").attr("x", "1"));
            tb.pushActiveFormattingElements(new Element("u").attr("x", "2"));

            // the old b and its following marker have left the retained prefix
            assertEquals(MaxFormattingElements, tb.formattingElements.size());
            assertSame(first, tb.formattingElements.get(0));
            assertNull(tb.getActiveFormattingElement("b"));
            assertNull(tb.getActiveFormattingElement("i"));
            tb.clearFormattingElementsToLastMarker();
            assertEquals(MaxFormattingElements - 3, tb.formattingElements.size());
            assertSame(last, tb.getActiveFormattingElement("i"));
            tb.clearFormattingElementsToLastMarker();
            assertTrue(tb.formattingElements.isEmpty());
        }

        @Test void formattingDuplicateRemovalFreesCapacityBeforeEviction() {
            fillFormattingElements(tb, MaxFormattingElements - 3);
            Element oldest = tb.formattingElements.get(0);
            Element duplicate = new Element("i");
            tb.pushActiveFormattingElements(duplicate);
            tb.pushActiveFormattingElements(new Element("i"));
            tb.pushActiveFormattingElements(new Element("i"));
            tb.pushActiveFormattingElements(new Element("i"));

            assertEquals(MaxFormattingElements, tb.formattingElements.size());
            assertSame(oldest, tb.formattingElements.get(0));
            assertFalse(tb.isInActiveFormattingElements(duplicate));
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void adoptionInsertionAtCapacityPreservesBookmark(boolean originalPresent) {
            fillFormattingElements(tb, MaxFormattingElements);
            Element first = tb.formattingElements.get(originalPresent ? 0 : 1);
            Element original = originalPresent ? tb.formattingElements.get(MaxFormattingElements / 4) : new Element("b");
            Element bookmark = tb.formattingElements.get(MaxFormattingElements / 2);
            Element replacement = new Element("i");
            tb.replaceFormattingElement(original, replacement, bookmark);

            assertEquals(MaxFormattingElements, tb.formattingElements.size());
            assertSame(first, tb.formattingElements.get(0));
            assertFalse(tb.isInActiveFormattingElements(original));
            int pos = tb.formattingElements.indexOf(bookmark) + 1;
            assertSame(replacement, tb.formattingElements.get(pos));

            Element next = new Element("u");
            tb.replaceFormattingElement(replacement, next, replacement);
            assertEquals(MaxFormattingElements, tb.formattingElements.size());
            assertSame(first, tb.formattingElements.get(0));
            assertSame(next, tb.formattingElements.get(pos));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void adoptionFallbackAtCapacityRetainsRecentEntries(boolean originalPresent) throws IOException {
        assertAdoptionRecovery(new HtmlTreeBuilder(), tb -> {
            fillFormattingElements(tb, MaxFormattingElements);
            Element first = tb.formattingElements.get(originalPresent ? 0 : 1);
            Element original = originalPresent ? tb.formattingElements.get(MaxFormattingElements / 4) : new Element("b");
            Element replacement = new Element("i");
            tb.replaceFormattingElement(original, replacement, new Element("u"));

            assertEquals(MaxFormattingElements, tb.formattingElements.size());
            assertSame(first, tb.formattingElements.get(0));
            assertSame(replacement, tb.lastFormattingElement());
            assertFalse(tb.isInActiveFormattingElements(original));
        }, "Unable to restore formatting order for <b>");
    }

    private static void fillFormattingElements(HtmlTreeBuilder tb, int count) {
        for (int i = 0; i < count; i++)
            tb.pushActiveFormattingElements(new Element("b").attr("x", Integer.toString(i)));
    }

    @Test void adoptionMovesExistingChildNodes() {
        HtmlTreeBuilder tb = new HtmlTreeBuilder();
        tb.initialiseParse(new StringReader(""), "", new Parser(tb));
        tb.initialiseParseFragment(new Element("div"));
        try {
            tb.processStartTag("b");
            Element formatting = tb.currentElement();
            tb.processStartTag("p");
            Element block = tb.currentElement();
            block.appendChild(new TextNode("before"));
            block.appendChild(new Comment("marker"));
            block.appendElement("span").text("inside");
            block.appendChild(new TextNode("after"));
            List<Node> children = block.childNodes();

            tb.processEndTag("b");

            assertEquals(0, formatting.childNodeSize());
            assertEquals(1, block.childNodeSize());
            Element replacement = block.child(0);
            assertNotSame(formatting, replacement);
            assertEquals("b", replacement.normalName());
            assertSame(block, replacement.parent());
            assertEquals(children.size(), replacement.childNodeSize());
            for (int i = 0; i < children.size(); i++) {
                Node child = children.get(i);
                assertSame(child, replacement.childNode(i));
                assertSame(replacement, child.parent());
                assertEquals(i, child.siblingIndex());
            }
        } finally {
            tb.closeParse();
        }
    }

    @Test void missingStackEntryReportsErrorAndAppends() throws IOException {
        assertAdoptionRecovery(new HtmlTreeBuilder(), tb -> {
            Element replacement = tb.doc.child(0).appendElement("b");
            tb.insertOnStackAfter(new Element("p"), replacement);
            assertSame(replacement, tb.currentElement());
        }, "Unable to place <b> after <p> while recovering misnested formatting");
    }

    @Test void missingBookmarkReportsErrorAndAppends() throws IOException {
        assertAdoptionRecovery(new HtmlTreeBuilder(), tb -> {
            Element original = new Element("b");
            Element replacement = new Element("b");
            tb.formattingElements.add(original);
            tb.replaceFormattingElement(original, replacement, new Element("i"));
            assertEquals(1, tb.formattingElements.size());
            assertSame(replacement, tb.lastFormattingElement());
        }, "Unable to restore formatting order for <b>");
    }

    @Test void missingOriginalBookmarkReportsErrorAndAppends() throws IOException {
        assertAdoptionRecovery(new HtmlTreeBuilder(), tb -> {
            Element original = new Element("b");
            Element replacement = new Element("b");
            tb.replaceFormattingElement(original, replacement, original);
            assertSame(replacement, tb.lastFormattingElement());
        }, "Unable to restore formatting order for <b>");
    }

    @Test void missingCommonAncestorReportsErrorAndStopsAdoption() throws IOException {
        assertAdoptionRecovery(new HtmlTreeBuilder(), tb -> {
            Element root = tb.stack.get(0);
            Element formatting = tb.doc.child(0).appendElement("b");
            Element block = formatting.appendElement("p");
            tb.stack.clear(); // omit the root to exercise recovery from a missing common ancestor
            tb.stack.add(formatting);
            tb.stack.add(block);
            tb.formattingElements.add(formatting);
            tb.currentToken = new Token.EndTag(tb).name("b");
            tb.process(tb.currentToken);
            assertSame(formatting, block.parent()); // leave the block in place when adoption cannot continue
            tb.stack.add(0, root); // restore the root before continuing the parse
        }, "No open parent element for misnested <b>");
    }

    @Test void missingFormattingElementDuringTraversalReportsError() throws IOException {
        HtmlTreeBuilder tb = new HtmlTreeBuilder() {
            @Override boolean removeFromStack(Element el) {
                boolean removed = super.removeFromStack(el);
                if (el.normalName().equals("span"))
                    stack.clear(); // simulate losing the remaining stack during the inner loop
                return removed;
            }
        };
        assertAdoptionRecovery(tb, builder -> {
            Element root = builder.stack.get(0);
            Element formatting = builder.doc.child(0).appendElement("b");
            Element span = formatting.appendElement("span");
            Element block = span.appendElement("p");
            builder.stack.add(formatting);
            builder.stack.add(span);
            builder.stack.add(block);
            builder.formattingElements.add(formatting);
            builder.currentToken = new Token.EndTag(builder).name("b");
            builder.process(builder.currentToken);
            assertSame(span, block.parent());
            builder.stack.add(root); // restore the root before continuing the parse
        }, "Formatting element <b> is no longer open during recovery");
    }

    @Test void adoptionReportsFormattingElementMissingFromStack() throws IOException {
        assertAdoptionRecovery(new HtmlTreeBuilder(), tb -> {
            // 4.4: remove a formatting entry whose element is no longer on the stack
            Element formatting = new Element("b");
            tb.formattingElements.add(formatting);
            tb.currentToken = new Token.EndTag(tb).name("b");
            tb.process(tb.currentToken);
            assertFalse(tb.isInActiveFormattingElements(formatting));
        }, "Unexpected EndTag token [</b>] when in state [InBody]");
    }

    @Test void adoptionReportsFormattingElementOutsideScope() throws IOException {
        assertAdoptionRecovery(new HtmlTreeBuilder(), tb -> {
            // 4.5: an element outside scope stays on both lists
            Element formatting = tb.doc.child(0).appendElement("b");
            tb.stack.add(formatting);
            tb.stack.add(formatting.appendElement("table"));
            tb.formattingElements.add(formatting);
            tb.currentToken = new Token.EndTag(tb).name("b");
            tb.process(tb.currentToken);
            assertTrue(tb.onStack(formatting));
            assertTrue(tb.isInActiveFormattingElements(formatting));
        }, "Unexpected EndTag token [</b>] when in state [InBody]");
    }

    @Test void adoptionReportsNonCurrentFormattingElementAndContinues() throws IOException {
        assertAdoptionRecovery(new HtmlTreeBuilder(), tb -> {
            // 4.6: report the error but continue through 4.8, popping both elements
            Element formatting = new Element("b");
            Element span = new Element("span");
            tb.stack.add(formatting);
            tb.stack.add(span);
            tb.formattingElements.add(formatting);
            tb.currentToken = new Token.EndTag(tb).name("b");
            tb.process(tb.currentToken);
            assertFalse(tb.onStack(formatting));
            assertFalse(tb.onStack(span));
            assertFalse(tb.isInActiveFormattingElements(formatting));
        }, "Unexpected EndTag token [</b>] when in state [InBody]");
    }

    // exercise an adoption error, then verify that the parser can consume the remaining input
    private static void assertAdoptionRecovery(HtmlTreeBuilder tb, Consumer<HtmlTreeBuilder> exercise, String message) throws IOException {
        Parser parser = new Parser(tb).setTrackErrors(20);
        tb.initialiseParse(new StringReader("<p>After</p>"), "", parser);
        tb.initialiseParseFragment(new Element("div"));
        try {
            exercise.accept(tb);
            assertTrue(parser.getErrors().stream().anyMatch(error -> error.getErrorMessage().equals(message)),
                () -> "Expected error: " + message + "; got: " + parser.getErrors());
            tb.runParser();
            assertEquals("After", tb.doc.text());
        } finally {
            tb.closeParse();
        }
    }

    @Test void scopeChecksTheSpecificFormattingElement() {
        HtmlTreeBuilder tb = new HtmlTreeBuilder();
        Element outer = new Element("b");
        Element inner = new Element("b");
        tb.stack.add(outer);
        tb.stack.add(new Element("template"));
        tb.stack.add(inner);

        assertTrue(tb.inScope("b"));
        assertTrue(tb.inScope(inner));
        assertFalse(tb.inScope(outer)); // the same tag inside the template does not put outer in scope
    }

    @Test void adoptionDropsNodeThatWouldCreateCycle() {
        Element body = new Element("body");
        Element formatting = body.appendElement("b");
        Element block = formatting.appendElement("p");
        // adoption steps 4.15–4.16 detach the node, but do not insert an ancestor into its descendant
        HtmlTreeBuilder.insertAdopted(formatting, block, null);
        assertNull(formatting.parent());
        assertSame(formatting, block.parent());
        assertEquals("", body.html());
    }

    @Test void adoptionDropsNodeWhenItsReferenceIsRemoved() {
        Element body = new Element("body");
        Element table = body.appendElement("table");
        // step 4.15 removes lastNode; step 4.16 rejects the now-detached reference node
        HtmlTreeBuilder.insertAdopted(table, body, table);
        assertNull(table.parent());
        assertEquals("", body.html());
    }

    @Test void adoptionDoesNotAddSecondDocumentElement() {
        Document doc = Document.createShell("");
        Element formatting = doc.body().appendElement("b");
        // step 4.16 rejects insertion into a Document that already has an element child
        HtmlTreeBuilder.insertAdopted(formatting, doc, null);
        assertNull(formatting.parent());
        assertEquals(1, doc.childrenSize());
        assertEquals("html", doc.child(0).normalName());
    }

    @Test void fosterInsertionUsesStackParentForRemovedTable() {
        // a table removed while still open uses the element above it on the stack
        Parser parser = Parser.htmlParser();
        try (StreamParser stream = new StreamParser(parser).parseFragment("", new Element("div"), "")) {
            HtmlTreeBuilder tb = (HtmlTreeBuilder) stream.treeBuilder;
            Element container = stream.document().child(0);
            Element table = container.appendElement("table");
            tb.push(table);
            table.remove();
            Element paragraph = new Element("p");
            tb.insertInFosterParent(paragraph);
            assertSame(container, paragraph.parent());
            assertEquals(1, container.childrenSize());
        }
    }

    @Test void fosterInsertionUsesDocumentHtmlWithoutTableOnStack() throws IOException {
        HtmlTreeBuilder tb = new HtmlTreeBuilder();
        Parser parser = new Parser(tb);
        tb.initialiseParse(new StringReader(""), "", parser);
        try {
            tb.processStartTag("html");
            Element html = tb.currentElement();
            Element paragraph = new Element("p");
            assertEquals(1, tb.stack.size());
            assertSame(html, tb.stack.get(0));

            tb.insertInFosterParent(paragraph);

            assertSame(html, paragraph.parent());
        } finally {
            tb.closeParse();
        }
    }

    @Test void fosterInsertionUsesFragmentContextWithoutTableOnStack() throws IOException {
        HtmlTreeBuilder tb = new HtmlTreeBuilder();
        Parser parser = new Parser(tb);
        tb.initialiseParse(new StringReader(""), "", parser);
        tb.initialiseParseFragment(new Element("div"));
        try {
            Element fragmentRoot = tb.currentElement();
            Element context = tb.doc.child(0);
            Element paragraph = new Element("p");
            assertEquals(1, tb.stack.size());
            assertSame(fragmentRoot, tb.stack.get(0));

            tb.insertInFosterParent(paragraph);

            assertSame(context, paragraph.parent());
            assertNotSame(fragmentRoot, paragraph.parent());
        } finally {
            tb.closeParse();
        }
    }

    @Test
    public void ensureSearchArraysAreSorted() {
        List<Object[]> treeBuilderArrays = HtmlTreeBuilderStateTest.findConstantArrays(HtmlTreeBuilder.class);
        HtmlTreeBuilderStateTest.ensureSorted(treeBuilderArrays);
        assertEquals(3, treeBuilderArrays.size());

        List<Object[]> tagOptionArrays = HtmlTreeBuilderStateTest.findConstantArrays(HtmlTagOptions.class);
        HtmlTreeBuilderStateTest.ensureSorted(tagOptionArrays);
        assertEquals(9, tagOptionArrays.size());
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
        try (StreamParser streamParser = new StreamParser(parser).parse("<title>One</title><p id=hit>Full</p>", "")) {
            TreeBuilder treeBuilder = streamParser.treeBuilder;
            assertFalse(treeBuilder.isComplete());
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

        Tag svgOption = Tag.valueOf("option", Parser.NamespaceSvg, ParseSettings.htmlDefault);
        assertFalse(svgOption.hasParserOption(HtmlTagOptions.ImpliedEnd));
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

    @Test void scriptInBodyUsesScriptData() {
        Document unclosed = Jsoup.parse("FOO<script type=\"text/plain\">'<!-- <sCrIpt>'</script>BAR");
        Element script = unclosed.expectFirst("script");
        assertSame(unclosed.body(), script.parent());
        assertEquals("'<!-- <sCrIpt>'</script>BAR", script.data());
        assertEquals(2, unclosed.body().childNodeSize());

        Document closed = Jsoup.parse("FOO<script><!--<script>-></script>--></script>QUX");
        script = closed.expectFirst("script");
        assertSame(closed.body(), script.parent());
        assertEquals("<!--<script>-></script>-->", script.data());
        assertEquals("QUX", closed.body().childNode(2).outerHtml());
    }
}
