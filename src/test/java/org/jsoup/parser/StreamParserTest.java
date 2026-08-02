package org.jsoup.parser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.DataUtil;
import org.jsoup.integration.ParseTest;
import org.jsoup.integration.TestServer;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 Tests for the StreamParser. There are also some tests in {@link org.jsoup.integration.ConnectTest}.
 */
class StreamParserTest {

    @Test
    void canStream() {
        String html = "<title>Test</title></head><div id=1>D1</div><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            StringBuilder seen;
            seen = new StringBuilder();
            parser.stream().forEachOrdered(el -> trackSeen(el, seen));
            assertEquals("title[Test];head+;div#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];body;html;#root;", seen.toString());
            // checks expected order, and the + indicates that element had a next sibling at time of emission
        }
    }

    @Test
    void canStreamXml() {
        String html = "<outmost><DIV id=1>D1</DIV><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>";
        try (StreamParser parser = new StreamParser(Parser.xmlParser()).parse(html, "")) {
            StringBuilder seen;
            seen = new StringBuilder();
            parser.stream().forEachOrdered(el -> trackSeen(el, seen));
            assertEquals("DIV#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];outmost;#root;", seen.toString());
            // checks expected order, and the + indicates that element had a next sibling at time of emission
        }
    }

    @Test void canIterate() {
        // same as stream, just a different interface
        String html = "<title>Test</title></head><div id=1>D1</div><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");
        StringBuilder seen = new StringBuilder();

        Iterator<Element> it = parser.iterator();
        while (it.hasNext()) {
            trackSeen(it.next(), seen);
        }

        assertEquals("title[Test];head+;div#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];body;html;#root;", seen.toString());
        // checks expected order, and the + indicates that element had a next sibling at time of emission
    }

    @Test void canReuse() {
        StreamParser parser = new StreamParser(Parser.htmlParser());
        String html1 = "<p>One<p>Two";
        parser.parse(html1, "");

        StringBuilder seen = new StringBuilder();
        parser.stream().forEach(el -> trackSeen(el, seen));
        assertEquals("head+;p[One]+;p[Two];body;html;#root;", seen.toString());

        String html2 = "<div>Three<div>Four</div></div>";
        StringBuilder seen2 = new StringBuilder();
        parser.parse(html2, "");
        parser.stream().forEach(el -> trackSeen(el, seen2));
        assertEquals("head+;div[Four];div[Three];body;html;#root;", seen2.toString());

        // re-run without a new parse should be empty
        StringBuilder seen3 = new StringBuilder();
        parser.stream().forEach(el -> trackSeen(el, seen3));
        assertEquals("", seen3.toString());
    }

    @Test void canStopAndCompleteAndReuse() throws IOException {
        StreamParser parser = new StreamParser(Parser.htmlParser());
        String html1 = "<p id=one>One<p id=two>Two";
        parser.parse(html1, "");

        Element p = parser.expectFirst("p");
        assertEquals("One", p.text());
        parser.stop();

        Iterator<Element> it = parser.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);

        Element p2 = parser.selectNext("p");
        assertNull(p2);
        assertNull(parser.selectFirst("p + p")); // a stopped parse exposes only complete matches

        Document completed = parser.complete();
        Elements ps = completed.select("p");
        assertEquals(2, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Two", ps.get(1).text());

        // complete() makes the whole document selectable, even though stop() still suppresses iterator emission
        Element completedP2 = parser.selectFirst("#two");
        assertSame(ps.get(1), completedP2);

        // can reuse
        parser.parse("<div>DIV", "");
        Element div = parser.expectFirst("div");
        assertEquals("DIV", div.text());
    }

    @Test void closeKeepsOpenMatchesUnavailable() throws IOException {
        String html = "<title>One</title><p id=hit>Full</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            Element title = parser.expectFirst("title");
            parser.stop();
            parser.close();

            // completed matches remain selectable while an open match stays unavailable after close
            assertSame(title, parser.selectFirst("title"));
            assertNull(parser.selectFirst("#hit"));
        }
    }

    static void trackSeen(Element el, StringBuilder actual) {
        actual.append(el.tagName());
        if (el.hasAttr("id"))
            actual.append("#").append(el.id());
        if (!el.ownText().isEmpty())
            actual.append("[").append(el.ownText()).append("]");
        if (el.nextElementSibling() != null)
            actual.append("+");

        actual.append(";");
    }

    @Test void select() throws IOException {
        String html = "<title>One</title><p id=1>P One</p><p id=2>P Two</p>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");

        Element title = parser.expectFirst("title");
        assertEquals("One", title.text());

        Document partialDoc = title.ownerDocument();
        assertNotNull(partialDoc);
        // at this point, we should have one P with no text - as title was emitted on P head
        Elements ps = partialDoc.select("p");
        assertEquals(1, ps.size());
        assertEquals("", ps.get(0).text());
        assertSame(partialDoc, parser.document());

        Element title2 = parser.selectFirst("title");
        assertSame(title2, title);

        Element p1 = parser.expectNext("p");
        assertEquals("P One", p1.text());

        Element p2 = parser.expectNext("p");
        assertEquals("P Two", p2.text());

        Element p1Again = parser.selectFirst("#1");
        assertSame(p1, p1Again); // can reselect an earlier element after later elements were emitted

        Element pNone = parser.selectNext("p");
        assertNull(pNone);
    }

    @Test void selectFirstWaitsForLookaheadElement() throws IOException {
        String html = "<title>One</title><p id=hit>Full</p><p>After</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            parser.expectFirst("title");

            // the second selection advances the provisional paragraph before returning it
            Element hit = parser.expectFirst("#hit");
            assertEquals("Full", hit.text());
            Element next = hit.nextElementSibling();
            assertNotNull(next);
            assertEquals("p", next.normalName());
        }
    }

    @Test void selectFirstWaitsForOpenQueuedElement() throws IOException {
        String html = "<form><em id=hit></form>x</em>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            // form-stack correction must not expose the formatting element before its later text is parsed
            Element hit = parser.expectFirst("#hit");
            assertEquals("x", hit.text());
        }
    }

    @Test void selectFirstCompletesDocumentRoot() throws IOException {
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse("<p>Full</p>", "")) {
            // the document root becomes selectable with the same complete contents as its final iterator emission
            Element root = parser.expectFirst("*");
            assertSame(parser.document(), root);
            assertEquals("Full", root.text());
        }
    }

    @Test void selectFirstKeepsDocumentOrderForNestedMatches() throws IOException {
        String html = "<title>One</title><div id=outer class=hit><div id=inner class=hit>Inner</div>Tail</div><p>After</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            parser.expectFirst("title");

            Element hit = parser.expectFirst(".hit");
            assertEquals("outer", hit.id());
            assertEquals("Inner Tail", hit.text());
        }
    }

    @Test void selectFirstKeepsDocumentOrderWhenParsingToMatch() throws IOException {
        String html = "<div id=outer class=hit><div id=inner class=hit>Inner</div></div><p>After</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            // the document-first outer match wins over the child emitted first by the iterator
            Element hit = parser.expectFirst(".hit");
            assertEquals("outer", hit.id());
            assertEquals("Inner", hit.text());
        }
    }

    @Test void selectFirstKeepsDocumentOrderAfterTransientMatch() throws IOException {
        String html = "<title>One</title><p>Full</p><div id=outer class=hit><div id=inner class=hit>Inner</div></div>" +
            "<p>After</p><aside id=unread>Unread</aside>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            parser.expectFirst("title");

            // the replacement outer match wins after the provisional empty paragraph stops matching
            Element hit = parser.expectFirst("p:empty, .hit");
            assertEquals("outer", hit.id());
            assertEquals("Inner", hit.text());
            assertNull(parser.document().selectFirst("#unread")); // later input remains unparsed
        }
    }

    @Test void selectFirstRechecksLookaheadMatchAfterEmission() throws IOException {
        String html = "<title>One</title><p id=filled>Full</p><p id=empty></p><p>After</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            parser.expectFirst("title");

            Element hit = parser.expectFirst("p:empty");
            assertEquals("empty", hit.id());
        }
    }

    @Test void selectFirstDoesNotRescanTransientLookaheads() throws IOException {
        int paragraphCount = 200;
        StringBuilder html = new StringBuilder("<title>One</title>");
        for (int i = 0; i < paragraphCount; i++)
            html.append("<p>Filled</p>");
        html.append("<p id=empty></p><div>After</div>");

        Evaluator emptyParagraph = Selector.evaluatorOf("p:empty");
        AtomicInteger evaluations = new AtomicInteger();
        Evaluator countingEvaluator = new Evaluator() {
            @Override public boolean matches(Element root, Element element) {
                evaluations.incrementAndGet();
                return emptyParagraph.matches(root, element);
            }
        };

        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html.toString(), "")) {
            parser.expectFirst("title");

            Element hit = parser.selectFirst(countingEvaluator);
            assertNotNull(hit);
            assertEquals("empty", hit.id());
            // make sure transient p:empty matches are not rescanned quadratically
            assertTrue(evaluations.get() < paragraphCount * 3, "evaluations: " + evaluations.get());
        }
    }

    @Test void selectFirstFindsFosterParentedElementThatIsNotEmitted() throws IOException {
        String html = "<table><b id=hit>T<div>T";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            parser.expectNext("head");
            parser.expectNext("div");

            // the completed tree makes the foster-parented match selectable
            Element hit = parser.expectFirst("#hit");
            assertEquals("b", hit.normalName());
            assertEquals("T T", hit.text());
        }
    }

    @Test void selectFirstRechecksCompletedDocumentAfterIteratorFallback() throws IOException {
        String html = "<p id=pre>pre</p><table><b id=hit>T<div>T";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            parser.expectFirst("#pre");

            // the completed document supplies a foster-parented match that was not emitted by the iterator
            Element hit = parser.expectFirst("#hit");
            assertEquals("b", hit.normalName());
            assertEquals("T T", hit.text());
        }
    }

    @Test void selectFirstDoesNotConsumeBufferedMatch() throws IOException {
        String html = "<p>One</p><p>Two</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            Iterator<Element> iterator = parser.iterator();
            assertTrue(iterator.hasNext());

            // the complete head buffered by hasNext() remains the iterator's next element after selection
            Element head = parser.expectFirst("head");
            assertSame(head, iterator.next());
        }
    }

    @Test void selectFirstDoesNotConsumeQueuedMatch() throws IOException {
        String html = "<div><p><span>One</div><aside>After";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            parser.expectNext("head");
            Iterator<Element> iterator = parser.iterator();
            assertTrue(iterator.hasNext());
            Element span = parser.document().selectFirst("span");
            assertNotNull(span);
            Element paragraph = parser.document().selectFirst("p");
            assertNotNull(paragraph);

            // the ready span remains next in iterator order after selecting the queued paragraph
            Element selected = parser.expectFirst("p");
            assertSame(paragraph, selected);
            assertSame(span, iterator.next());
        }
    }

    @Test void selectFirstWaitsForVoidElementEmission() throws IOException {
        String html = "<title>One</title><img id=hit><p>After</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            parser.expectFirst("title");

            Element hit = parser.expectFirst("#hit");
            Element next = hit.nextElementSibling();
            assertNotNull(next);
            assertEquals("p", next.normalName());
        }
    }

    @Test void selectFirstWaitsForFragmentLookahead() throws IOException {
        String html = "<tr id=one><td>One</td><tr id=two><td>Two</td>";
        Element context = new Element("table");
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parseFragment(html, context, "")) {
            parser.expectFirst("td");

            Element row = parser.expectFirst("#two");
            assertEquals("Two", row.text());
        }
    }

    @Test void selectFirstUnwrapsReaderExceptionWhileCompletingLookahead() throws IOException {
        String prefix = "<title>One</title><p id=hit>";
        // selection preserves its checked IOException contract while advancing the pending match
        String initialBuffer = prefix + StringUtil.padding(CharacterReader.BufferSize - prefix.length(), -1);
        IOException expected = new IOException("read failed");
        Reader reader = new Reader() {
            boolean firstRead = true;

            @Override public int read(char[] buffer, int offset, int length) throws IOException {
                if (firstRead) {
                    firstRead = false;
                    initialBuffer.getChars(0, initialBuffer.length(), buffer, offset);
                    return initialBuffer.length();
                }
                throw expected;
            }

            @Override public void close() { }
        };

        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(reader, "")) {
            parser.expectFirst("title");

            IOException actual = assertThrows(IOException.class, () -> parser.selectFirst("#hit"));
            assertSame(expected, actual);
        }
    }

    @Test void canRemoveFromDom() {
        String html = "<div>One</div><div>DESTROY</div><div>Two</div>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");
        parser.parse(html, "");

        parser.stream().forEach(
            el -> {
                if (el.ownText().equals("DESTROY"))
                    el.remove();
            });

        Document doc = parser.document();
        Elements divs = doc.select("div");
        assertEquals(2, divs.size());
        assertEquals("One Two", divs.text());
    }

    @Test void canRemoveWithIterator() {
        String html = "<div>One</div><div>DESTROY</div><div>Two</div>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");
        parser.parse(html, "");

        Iterator<Element> it = parser.iterator();
        while (it.hasNext()) {
            Element el = it.next();
            if (el.ownText().equals("DESTROY"))
                it.remove(); // we know el.remove() works, from above test
        }

        Document doc = parser.document();
        Elements divs = doc.select("div");
        assertEquals(2, divs.size());
        assertEquals("One Two", divs.text());
    }

    @Test void canSelectWithHas() throws IOException {
        StreamParser parser = basic();

        Element el = parser.expectNext("div:has(p)");
        assertEquals("Two", el.text());
    }

    @Test void canSelectWithSibling() throws IOException {
        StreamParser parser = basic();

        Element el = parser.expectNext("div:first-of-type");
        assertEquals("One", el.text());

        Element el2 = parser.selectNext("div:first-of-type");
        assertNull(el2);
    }

    @Test void canLoopOnSelectNext() throws IOException {
        StreamParser streamer = new StreamParser(Parser.htmlParser()).parse("<div><p>One<p>Two<p>Thr</div>", "");

        int count = 0;
        Element e;
        while ((e = streamer.selectNext("p")) != null) {
            assertEquals(3, e.text().length()); // has a body
            e.remove();
            count++;
        }

        assertEquals(3, count);
        assertEquals(0, streamer.document().select("p").size()); // removed all during iter

        assertTrue(isClosed(streamer)); // read to the end
    }

    @Test void worksWithXmlParser() throws IOException {
        StreamParser streamer = new StreamParser(Parser.xmlParser()).parse("<div><p>One</p><p>Two</p><p>Thr</p></div>", "");

        int count = 0;
        Element e;
        while ((e = streamer.selectNext("p")) != null) {
            assertEquals(3, e.text().length()); // has a body
            e.remove();
            count++;
        }

        assertEquals(3, count);
        assertEquals(0, streamer.document().select("p").size()); // removed all during iter

        assertTrue(isClosed(streamer)); // read to the end
    }

    @Test void closedOnStreamDrained() {
        StreamParser streamer = basic();
        assertFalse(isClosed(streamer));
        long count = streamer.stream().count();
        assertEquals(7, count);

        assertTrue(isClosed(streamer));
    }

    @Test void closedOnIteratorDrained() {
        StreamParser streamer = basic();

        int count = 0;
        Iterator<Element> it = streamer.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(7, count);
        assertTrue(isClosed(streamer));
    }

    @Test void closedOnComplete() throws IOException {
        StreamParser streamer = basic();
        Document doc = streamer.complete();
        assertTrue(isClosed(streamer));
    }

    @Test void closedOnTryWithResources() {
        StreamParser copy;
        try(StreamParser streamer = basic()) {
            copy = streamer;
            assertFalse(isClosed(copy));
        }
        assertTrue(isClosed(copy));
    }

    static StreamParser basic() {
        String html = "<div>One</div><div><p>Two</div>";
        StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "");
        parser.parse(html, "");
        return parser;
    }

    static boolean isClosed(StreamParser streamer) {
        // a bit of a back door in!
        return getReader(streamer) == null;
    }

     private static CharacterReader getReader(StreamParser streamer) {
        return streamer.document().parser().getTreeBuilder().reader;
    }

    @Test void doesNotReadPastParse() throws IOException {
        StreamParser streamer = basic();
        Element div = streamer.expectFirst("div");

        // we should have read the sibling div, but not yet its children p
        Element sib = div.nextElementSibling();
        assertNotNull(sib);
        assertEquals("div", sib.tagName());
        assertEquals(0, sib.childNodeSize());

        // the Reader should be at "<p>" because we haven't consumed it
        assertTrue(getReader(streamer).matches("<p>Two"));
    }

    @Test void canParseFileReader() throws IOException {
        File file = ParseTest.getFile("/htmltests/large.html");

        // can't use FileReader from Java 11 here
        InputStreamReader input = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(input);
        StreamParser streamer = new StreamParser(Parser.htmlParser()).parse(reader, file.getAbsolutePath());

        Element last = null, e;
        while ((e = streamer.selectNext("p")) != null) {
            last = e;
        }
        assertTrue(last.text().startsWith("VESTIBULUM"));

        // the reader should be closed as streamer is closed on completion of read
        assertTrue(isClosed(streamer));

        assertThrows(IOException.class, reader::ready); // ready() checks isOpen and throws
    }

    @Test void canParseFile() throws IOException {
        File file = ParseTest.getFile("/htmltests/large.html");
        StreamParser streamer = DataUtil.streamParser(file.toPath(), StandardCharsets.UTF_8, "", Parser.htmlParser());

        Element last = null, e;
        while ((e = streamer.selectNext("p")) != null) {
            last = e;
        }
        assertTrue(last.text().startsWith("VESTIBULUM"));

        // the reader should be closed as streamer is closed on completion of read
        assertTrue(isClosed(streamer));
    }

    @Test void canCleanlyConsumePortionOfUrl() throws IOException {
        // test that we can get just the head section of large.html, and only read the minimum required from the URL
        String url = TestServer.origin().file.url("/htmltests/large.html"); // 280 K

        AtomicReference<Float> seenPercent = new AtomicReference<>(0.0f);
        StreamParser parserRef;

        Connection con = Jsoup.connect(url)
            .onResponseProgress((processed, total, percent, response) -> {
                //System.out.println("Processed: " + processed + " Total: " + total + " Percent: " + percent);
                seenPercent.set(percent);
            });

        Connection.Response response = con.execute();
        try (StreamParser parser = response.streamParser()) {
            parserRef = parser;
            // get the head section
            Element head = parser.selectFirst("head");
            Element title = head.expectFirst("title");
            assertEquals("Large HTML", title.text());
        }
        // now that we've left the try, the stream parser and the response bodystream should be closed
        assertTrue(isClosed(parserRef));

        // test that we didn't read all of the stream
        assertTrue(seenPercent.get() > 0.0f);
        assertTrue(seenPercent.get() < 100.0f);
        // not sure of a good way to assert the bufferedInputReader buf (as held by ConstrainableInputStream in Response.BodyStream) is null. But it is via StreamParser.close.
    }

    // Fragments

    @Test
    void canStreamFragment() {
        String html = "<tr id=1><td>One</td><tr id=2><td>Two</td></tr><tr id=3><td>Three</td></tr>";
        Element context = new Element("table");

        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parseFragment(html, context, "")) {
            StringBuilder seen = new StringBuilder();
            parser.stream().forEachOrdered(el -> trackSeen(el, seen));
            assertEquals("td[One];tr#1+;td[Two];tr#2+;td[Three];tr#3;tbody;table;#root;", seen.toString());
            // checks expected order, and the + indicates that element had a next sibling at time of emission
            // note that we don't get a full doc, just the fragment (and the context at the end of the stack)

            assertTrue(isClosed(parser)); // as read to completion
        }
    }

    @Test void canIterateFragment() {
        // same as stream, just a different interface
        String html = "<tr id=1><td>One</td><tr id=2><td>Two</td></tr><tr id=3><td>Three</td></tr>"; // missing </tr>, following <tr> infers it
        Element context = new Element("table");

        try(StreamParser parser = new StreamParser(Parser.htmlParser()).parseFragment(html, context, "")) {
            StringBuilder seen = new StringBuilder();

            Iterator<Element> it = parser.iterator();
            while (it.hasNext()) {
                trackSeen(it.next(), seen);
            }

            assertEquals("td[One];tr#1+;td[Two];tr#2+;td[Three];tr#3;tbody;table;#root;", seen.toString());
            // checks expected order, and the + indicates that element had a next sibling at time of emission
            // note that we don't get a full doc, just the fragment (and the context at the end of the stack)

            assertTrue(isClosed(parser)); // as read to completion
        }
    }

    @Test
    void canSelectAndCompleteFragment() throws IOException {
        String html = "<tr id=1><td>One</td><tr id=2><td>Two</td></tr><tr id=3><td>Three</td></tr>";
        Element context = new Element("table");

        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parseFragment(html, context, "")) {
            Element first = parser.expectNext("td");
            assertEquals("One", first.ownText());

            Element el = parser.expectNext("td");
            assertEquals("Two", el.ownText());

            el = parser.expectNext("td");
            assertEquals("Three", el.ownText());

            el = parser.selectNext("td");
            assertNull(el);

            List<Node> nodes = parser.completeFragment();
            assertEquals(1, nodes.size()); // should be the inferred tbody
            Node tbody = nodes.get(0);
            assertEquals("tbody", tbody.nodeName());
            List<Node> trs = tbody.childNodes();
            assertEquals(3, trs.size()); // should be the three TRs
            assertSame(trs.get(0).childNode(0), first); // tr -> td

            assertSame(parser.document(), first.ownerDocument()); // the shell document for this fragment
        }
    }

    @Test
    void canStreamFragmentXml() throws IOException {
        String html = "<tr id=1><td>One</td></tr><tr id=2><td>Two</td></tr><tr id=3><td>Three</td></tr>";
        Element context = new Element("Other");

        try (StreamParser parser = new StreamParser(Parser.xmlParser()).parseFragment(html, context, "")) {
            StringBuilder seen = new StringBuilder();
            parser.stream().forEachOrdered(el -> trackSeen(el, seen));
            assertEquals("td[One];tr#1+;td[Two];tr#2+;td[Three];tr#3;#root;", seen.toString());
            // checks expected order, and the + indicates that element had a next sibling at time of emission
            // note that we don't get a full doc, just the fragment

            assertTrue(isClosed(parser)); // as read to completion

            List<Node> nodes = parser.completeFragment();
            assertEquals(3, nodes.size());
            assertEquals("tr", nodes.get(0).nodeName());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<html><body><a>Link</a></body></html>",
        "<html><body><a>Link</a>",
        "<a>Link</a></body></html>",
        "<a>Link</a>",
        "<a>Link",
        "<a>Link</body>",
    })
    void emitsOnlyOnce(String html) {
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            // https://github.com/jhy/jsoup/issues/2295
            // When there was a /body or /html, those were being emitted twice, due to firing a fake onNodeClosed to track their source positions
            StringBuilder seen = new StringBuilder();
            parser.stream().forEach(el -> trackSeen(el, seen));
            assertEquals("head+;a[Link];body;html;#root;", seen.toString());
        }
    }

    @Test
    void emitsNoscriptFallbackElements() {
        String html = "<body><noscript><p>one</p><p>two</p></noscript><p>after</p>";
        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            StringBuilder seen = new StringBuilder();
            parser.stream().forEachOrdered(el -> {
                if (el.normalName().equals("p"))
                    trackSeen(el, seen);
            });
            assertEquals("p[one]+;p[two];p[after];", seen.toString());
        }
    }

}
