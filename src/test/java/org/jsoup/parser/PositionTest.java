package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.FileServlet;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Range;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 Functional tests for the Position tracking behavior (across nodes, treebuilder, etc.)
 */
class PositionTest {
    static Parser TrackingHtmlParser = Parser.htmlParser().setTrackPosition(true);
    static Parser TrackingXmlParser = Parser.xmlParser().setTrackPosition(true);

    @Test void parserTrackDefaults() {
        Parser htmlParser = Parser.htmlParser();
        assertFalse(htmlParser.isTrackPosition());
        htmlParser.setTrackPosition(true);
        assertTrue(htmlParser.isTrackPosition());

        Parser xmlParser = Parser.xmlParser();
        assertFalse(xmlParser.isTrackPosition());
        xmlParser.setTrackPosition(true);
        assertTrue(xmlParser.isTrackPosition());
    }

    @Test void tracksPosition() {
        String content = "<p id=1\n class=foo>\n<span>Hello\n &reg;\n there &copy.</span> now.\n <!-- comment --> ";
        Document doc = Jsoup.parse(content, TrackingHtmlParser);

        Element html = doc.expectFirst("html");
        Element body = doc.expectFirst("body");
        Element p = doc.expectFirst("p");
        Element span = doc.expectFirst("span");
        TextNode text = (TextNode) span.firstChild();
        assertNotNull(text);
        TextNode now = (TextNode) span.nextSibling();
        assertNotNull(now);
        Comment comment = (Comment) now.nextSibling();
        assertNotNull(comment);

        // implicit
        assertTrue(body.sourceRange().isTracked());
        assertTrue(body.endSourceRange().isTracked());
        assertTrue(body.sourceRange().isImplicit());
        assertTrue(body.endSourceRange().isImplicit());
        Range htmlRange = html.sourceRange();
        assertEquals("1,1:0-1,1:0", htmlRange.toString());
        assertEquals(htmlRange, body.sourceRange());
        assertEquals(html.endSourceRange(), body.endSourceRange());


        Range pRange = p.sourceRange();
        assertEquals("1,1:0-2,12:19", pRange.toString());
        assertFalse(pRange.isImplicit());
        assertTrue(p.endSourceRange().isImplicit());
        assertEquals("6,19:83-6,19:83", p.endSourceRange().toString());
        assertEquals(p.endSourceRange(), html.endSourceRange());

        // no explicit P closer
        Range pEndRange = p.endSourceRange();
        assertTrue(pEndRange.isTracked());
        assertTrue(pEndRange.isImplicit());

        Range.Position pStart = pRange.start();
        assertTrue(pStart.isTracked());
        assertEquals(0, pStart.pos());
        assertEquals(1, pStart.columnNumber());
        assertEquals(1, pStart.lineNumber());
        assertEquals("1,1:0", pStart.toString());

        Range.Position pEnd = pRange.end();
        assertTrue(pStart.isTracked());
        assertEquals(19, pEnd.pos());
        assertEquals(12, pEnd.columnNumber());
        assertEquals(2, pEnd.lineNumber());
        assertEquals("2,12:19", pEnd.toString());

        assertEquals("3,1:20", span.sourceRange().start().toString());
        assertEquals("3,7:26", span.sourceRange().end().toString());

        // span end tag
        Range spanEnd = span.endSourceRange();
        assertTrue(spanEnd.isTracked());
        assertEquals("5,14:52-5,21:59", spanEnd.toString());

        String wholeText = text.getWholeText();
        assertEquals("Hello\n ®\n there ©.", wholeText);
        String textOrig = "Hello\n &reg;\n there &copy.";
        Range textRange = text.sourceRange();
        assertEquals(textRange.end().pos() -  textRange.start().pos(), textOrig.length());
        assertEquals("3,7:26", textRange.start().toString());
        assertEquals("5,14:52", textRange.end().toString());

        assertEquals("6,2:66", comment.sourceRange().start().toString());
        assertEquals("6,18:82", comment.sourceRange().end().toString());
    }

    @Test void tracksExpectedPoppedElements() {
        // When TreeBuilder hits a direct .pop(), vs popToClose(..)
        String html = "<html><head><meta></head><body><img><p>One</p><p>Two</p></body></html>";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);

        StringBuilder track = new StringBuilder();
        doc.expectFirst("html").stream().forEach(el -> {
            accumulatePositions(el, track);
            assertTrue(el.sourceRange().isTracked(), el.tagName());
            assertTrue(el.endSourceRange().isTracked(), el.tagName());
            assertFalse(el.sourceRange().isImplicit(), el.tagName());
            assertFalse(el.endSourceRange().isImplicit(), el.tagName());
        });
        assertEquals("html:0-6~63-70; head:6-12~18-25; meta:12-18~12-18; body:25-31~56-63; img:31-36~31-36; p:36-39~42-46; p:46-49~52-56; ", track.toString());

        StringBuilder textTrack = new StringBuilder();
        doc.nodeStream(TextNode.class).forEach(text -> accumulatePositions(text, textTrack));
        assertEquals("#text:39-42; #text:49-52; ", textTrack.toString());
    }

    static void accumulatePositions(Node node, StringBuilder sb) {
        sb
            .append(node.nodeName())
            .append(':')
            .append(node.sourceRange().startPos())
            .append('-')
            .append(node.sourceRange().endPos());

        if (node instanceof Element) {
            Element el = (Element) node;
            sb
                .append("~")
                .append(el.endSourceRange().startPos())
                .append('-')
                .append(el.endSourceRange().endPos());
        }
        sb.append("; ");
    }

    @Test void tracksImplicitPoppedElements() {
        // When TreeBuilder hits a direct .pop(), vs popToClose(..)
        String html = "<meta><img><p>One<p>Two<p>Three";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);

        StringBuilder track = new StringBuilder();
        doc.expectFirst("html").stream().forEach(el -> {
            assertTrue(el.sourceRange().isTracked());
            assertTrue(el.endSourceRange().isTracked());
            accumulatePositions(el, track);
        });

        assertTrue(doc.expectFirst("p").endSourceRange().isImplicit());
        assertFalse(doc.expectFirst("meta").endSourceRange().isImplicit());
        assertEquals("html:0-0~31-31; head:0-0~6-6; meta:0-6~0-6; body:6-6~31-31; img:6-11~6-11; p:11-14~17-17; p:17-20~23-23; p:23-26~31-31; ", track.toString());
    }
    private void printRange(Node node) {
        if (node instanceof Element) {
            Element el = (Element) node;
            System.out.println(el.tagName() + "\t"
                + el.sourceRange().start().pos() + "-" + el.sourceRange().end().pos()
                + "\t... "
                + el.endSourceRange().start().pos() + "-" + el.endSourceRange().end().pos()
            );
        } else {
            System.out.println(node.nodeName() + "\t"
                + node.sourceRange().start().pos() + "-" + node.sourceRange().end().pos()
            );
        }
    }

    @Test void tracksMarkup() {
        String html = "<!doctype\nhtml>\n<title>jsoup &copy;\n2022</title><body>\n<![CDATA[\n<jsoup>\n]]>";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);

        DocumentType doctype = doc.documentType();
        assertNotNull(doctype);
        assertEquals("html", doctype.name());
        assertEquals("1,1:0-2,6:15", doctype.sourceRange().toString());

        Element title = doc.expectFirst("title");
        TextNode titleText = (TextNode) title.firstChild();
        assertNotNull(titleText);
        assertEquals("jsoup ©\n2022", title.text());
        assertEquals(titleText.getWholeText(), title.text());
        assertEquals("3,1:16-3,8:23", title.sourceRange().toString());
        assertEquals("3,8:23-4,5:40", titleText.sourceRange().toString());

        CDataNode cdata = (CDataNode) doc.body().childNode(1);
        assertEquals("\n<jsoup>\n", cdata.text());
        assertEquals("5,1:55-7,4:76", cdata.sourceRange().toString());
    }

    @Test void tracksDataNodes() {
        String html = "<head>\n<script>foo;\nbar()\n5 <= 4;</script>";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);

        Element script = doc.expectFirst("script");
        assertNotNull(script);
        assertEquals("2,1:7-2,9:15", script.sourceRange().toString());
        DataNode data = (DataNode) script.firstChild();
        assertNotNull(data);
        assertEquals("2,9:15-4,8:33", data.sourceRange().toString());
    }

    @Test void tracksXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!doctype html>\n<rss url=foo>\nXML\n</rss>\n<!-- comment -->";
        Document doc = Jsoup.parse(xml, TrackingXmlParser);

        XmlDeclaration decl = (XmlDeclaration) doc.childNode(0);
        assertEquals("1,1:0-1,39:38", decl.sourceRange().toString());

        DocumentType doctype = (DocumentType) doc.childNode(2);
        assertEquals("2,1:39-2,16:54", doctype.sourceRange().toString());

        Element rss = doc.firstElementChild();
        assertNotNull(rss);
        assertEquals("3,1:55-3,14:68", rss.sourceRange().toString());
        assertEquals("5,1:73-5,7:79", rss.endSourceRange().toString());

        TextNode text = (TextNode) rss.firstChild();
        assertNotNull(text);
        assertEquals("3,14:68-5,1:73", text.sourceRange().toString());

        Comment comment = (Comment) rss.nextSibling().nextSibling();
        assertEquals("6,1:80-6,17:96", comment.sourceRange().toString());
    }

    @Test void tracksFromFetch() throws IOException {
        String url = FileServlet.urlTo("/htmltests/large.html"); // 280 K
        Document doc = Jsoup.connect(url).parser(TrackingHtmlParser).get();

        Element firstP = doc.expectFirst("p");
        assertNotNull(firstP);
        assertEquals("4,1:53-4,4:56", firstP.sourceRange().toString());

        Element p = doc.expectFirst("#xy");
        assertNotNull(p);
        assertEquals("1000,1:279646-1000,10:279655", p.sourceRange().toString());
        assertEquals("1000,567:280212-1000,571:280216", p.endSourceRange().toString());

        TextNode text = (TextNode) p.firstChild();
        assertNotNull(text);
        assertEquals("1000,10:279655-1000,357:280002", text.sourceRange().toString());
    }

    @Test void tracksFromXmlFetch() throws IOException {
        String url = FileServlet.urlTo("/htmltests/test-rss.xml");
        Document doc = Jsoup.connect(url).parser(TrackingXmlParser).get();

        Element item = doc.expectFirst("item + item");
        assertNotNull(item);
        assertEquals("13,5:496-13,11:502", item.sourceRange().toString());
        assertEquals("17,5:779-17,12:786", item.endSourceRange().toString());
    }

    @Test void tracksTableMovedText() {
        String html = "<table>foo<tr>bar<td>baz</td>qux</tr>coo</table>";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);

        StringBuilder track = new StringBuilder();
        List<TextNode> textNodes = doc.nodeStream(TextNode.class)
            .peek(node -> accumulatePositions(node, track))
            .collect(Collectors.toList());

        assertEquals(5, textNodes.size());
        assertEquals("foo", textNodes.get(0).text());
        assertEquals("bar", textNodes.get(1).text());
        assertEquals("baz", textNodes.get(2).text());
        assertEquals("qux", textNodes.get(3).text());
        assertEquals("coo", textNodes.get(4).text());

        assertEquals("#text:7-10; #text:14-17; #text:21-24; #text:29-32; #text:37-40; ", track.toString());
    }

    @Test void tracksClosingHtmlTagsInXml() {
        // verifies https://github.com/jhy/jsoup/issues/1935
        String xml = "<p>One</p><title>Two</title><data>Three</data>";
        Document doc = Jsoup.parse(xml, TrackingXmlParser);
        Elements els = doc.children();
        for (Element el : els) {
            assertTrue(el.sourceRange().isTracked());
            assertTrue(el.endSourceRange().isTracked());
        }
    }

    @Test void tracksClosingHeadingTags() {
        // https://github.com/jhy/jsoup/issues/1987
        String html = "<h1>One</h1><h2>Two</h2><h10>Ten</h10>";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);

        Elements els = doc.body().children();
        for (Element el : els) {
            assertTrue(el.sourceRange().isTracked());
            assertTrue(el.endSourceRange().isTracked());
        }

        Element h2 = doc.expectFirst("h2");
        assertEquals("1,13:12-1,17:16", h2.sourceRange().toString());
        assertEquals("1,20:19-1,25:24", h2.endSourceRange().toString());
    }

    @Test void tracksAttributes() {
        String html = "<div one=\"Hello there\" id=1 class=foo attr1 = \"bar &amp; qux\" attr2='val &gt x' attr3=\"\" attr4 attr5>Text";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);

        Element div = doc.expectFirst("div");

        StringBuilder track = new StringBuilder();
        for (Attribute attr : div.attributes()) {

            Range.AttributeRange attrRange = attr.sourceRange();
            assertTrue(attrRange.nameRange().isTracked());
            assertTrue(attrRange.valueRange().isTracked());
            assertSame(attrRange, div.attributes().sourceRange(attr.getKey()));

            assertFalse(attrRange.nameRange().isImplicit());
            if (attr.getValue().isEmpty())
                assertTrue(attrRange.valueRange().isImplicit());
            else
                assertFalse(attrRange.valueRange().isImplicit());

            accumulatePositions(attr, track);
        }

        assertEquals("one:5-8=10-21; id:23-25=26-27; class:28-33=34-37; attr1:38-43=47-60; attr2:62-67=69-78; attr3:80-85=85-85; attr4:89-94=94-94; attr5:95-100=100-100; ", track.toString());
    }

    @Test void tracksAttributesAcrossLines() {
        String html = "<div one=\"Hello\nthere\" \nid=1 \nclass=\nfoo\nattr5>Text";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);

        Element div = doc.expectFirst("div");

        StringBuilder track = new StringBuilder();
        for (Attribute attr : div.attributes()) {
            Range.AttributeRange attrRange = attr.sourceRange();
            assertTrue(attrRange.nameRange().isTracked());
            assertTrue(attrRange.valueRange().isTracked());
            assertSame(attrRange, div.attributes().sourceRange(attr.getKey()));
            assertFalse(attrRange.nameRange().isImplicit());
            if (attr.getValue().isEmpty())
                assertTrue(attrRange.valueRange().isImplicit());
            else
                assertFalse(attrRange.valueRange().isImplicit());
            accumulatePositions(attr, track);
        }

        String value = div.attributes().get("class");
        assertEquals("foo", value);
        Range.AttributeRange foo = div.attributes().sourceRange("class");
        assertEquals("4,1:30-4,6:35=5,1:37-5,4:40", foo.toString());

        assertEquals("one:5-8=10-21; id:24-26=27-28; class:30-35=37-40; attr5:41-46=46-46; ", track.toString());
    }

    @Test void trackAttributePositionInFirstElement() {
        String html = "<html lang=en class=dark><p hidden></p></html>";

        Document htmlDoc = Jsoup.parse(html, TrackingHtmlParser);
        StringBuilder htmlPos = new StringBuilder();
        htmlDoc.expectFirst("html").nodeStream().forEach(node -> {
            accumulatePositions(node, htmlPos);
            accumulateAttributePositions(node, htmlPos);
        });

        assertEquals("html:0-25~39-46; lang:6-10=11-13; class:14-19=20-24; head:25-25~25-25; body:25-25~46-46; p:25-35~35-39; hidden:28-34=34-34; ", htmlPos.toString());

        Document xmlDoc = Jsoup.parse(html, TrackingXmlParser);
        StringBuilder xmlPos = new StringBuilder();
        xmlDoc.expectFirst("html").nodeStream().forEach(node -> {
            accumulatePositions(node, xmlPos);
            accumulateAttributePositions(node, xmlPos);
        });

        assertEquals("html:0-25~39-46; lang:6-10=11-13; class:14-19=20-24; p:25-35~35-39; hidden:28-34=34-34; ", xmlPos.toString());
    }

    @Test void trackAttributePositionWithCase() {
        String pomXml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>";

        Document htmlDoc = Jsoup.parse(pomXml, TrackingHtmlParser);
        StringBuilder htmlPos = new StringBuilder();
        htmlDoc.expectFirst("html").nodeStream().forEach(node -> {
            accumulatePositions(node, htmlPos);
            accumulateAttributePositions(node, htmlPos);
        });

        assertEquals("html:0-0~243-243; head:0-0~0-0; body:0-0~243-243; project:0-204~243-243; xmlns:9-14=16-49; xmlns:xsi:51-60=62-103; xsi:schemalocation:105-123=125-202; #text:204-209; modelversion:209-223~228-243; #text:223-228; ", htmlPos.toString());

        Document xmlDoc = Jsoup.parse(pomXml, TrackingXmlParser);
        StringBuilder xmlPos = new StringBuilder();
        xmlDoc.expectFirst("project").nodeStream().forEach(node -> {
            accumulatePositions(node, xmlPos);
            accumulateAttributePositions(node, xmlPos);
        });

        assertEquals("project:0-204~243-243; xmlns:9-14=16-49; xmlns:xsi:51-60=62-103; xsi:schemaLocation:105-123=125-202; #text:204-209; modelVersion:209-223~228-243; #text:223-228; ", xmlPos.toString());

        Document xmlDocLc = Jsoup.parse(pomXml, Parser.xmlParser().setTrackPosition(true).settings(new ParseSettings(false, false)));
        StringBuilder xmlPosLc = new StringBuilder();
        xmlDocLc.expectFirst("project").nodeStream().forEach(node -> {
            accumulatePositions(node, xmlPosLc);
            accumulateAttributePositions(node, xmlPosLc);
        });

        assertEquals("project:0-204~243-243; xmlns:9-14=16-49; xmlns:xsi:51-60=62-103; xsi:schemalocation:105-123=125-202; #text:204-209; modelversion:209-223~228-243; #text:223-228; ", xmlPosLc.toString());
    }


    @Test void trackAttributesPositionsDedupes() {
        String html = "<p id=1 id=2 Id=3 Id=4 id=5 Id=6>";
        Document      htmlDoc   = Jsoup.parse(html, TrackingHtmlParser);
        Document      htmlDocUc = Jsoup.parse(html, Parser.htmlParser().setTrackPosition(true).settings(new ParseSettings(true, true)));
        Document      xmlDoc    = Jsoup.parse(html, TrackingXmlParser);
        Document      xmlDocLc  = Jsoup.parse(html, Parser.xmlParser().setTrackPosition(true).settings(new ParseSettings(false, false)));

        StringBuilder htmlPos   = new StringBuilder();
        StringBuilder htmlUcPos = new StringBuilder();
        StringBuilder xmlPos    = new StringBuilder();
        StringBuilder xmlLcPos  = new StringBuilder();

        accumulateAttributePositions(htmlDoc   .expectFirst("p"), htmlPos);
        accumulateAttributePositions(htmlDocUc .expectFirst("p"), htmlUcPos);
        accumulateAttributePositions(xmlDoc    .expectFirst("p"), xmlPos);
        accumulateAttributePositions(xmlDocLc  .expectFirst("p"), xmlLcPos);

        assertEquals("id:3-5=6-7; ", htmlPos   .toString());
        assertEquals("id:3-5=6-7; Id:13-15=16-17; ", htmlUcPos .toString());
        assertEquals("id:3-5=6-7; Id:13-15=16-17; ", xmlPos    .toString());
        assertEquals("id:3-5=6-7; ", xmlLcPos .toString());
    }

    @Test void trackAttributesPositionsDirectionalDedupes() {
        String html = "<p Id=1 id=2 Id=3 Id=4 id=5 Id=6>";
        Document      htmlDoc   = Jsoup.parse(html, TrackingHtmlParser);
        Document      htmlDocUc = Jsoup.parse(html, Parser.htmlParser().setTrackPosition(true).settings(new ParseSettings(true, true)));
        Document      xmlDoc    = Jsoup.parse(html, TrackingXmlParser);
        Document      xmlDocLc  = Jsoup.parse(html, Parser.xmlParser().setTrackPosition(true).settings(new ParseSettings(false, false)));

        StringBuilder htmlPos   = new StringBuilder();
        StringBuilder htmlUcPos = new StringBuilder();
        StringBuilder xmlPos    = new StringBuilder();
        StringBuilder xmlLcPos  = new StringBuilder();

        accumulateAttributePositions(htmlDoc   .expectFirst("p"), htmlPos);
        accumulateAttributePositions(htmlDocUc .expectFirst("p"), htmlUcPos);
        accumulateAttributePositions(xmlDoc    .expectFirst("p"), xmlPos);
        accumulateAttributePositions(xmlDocLc  .expectFirst("p"), xmlLcPos);

        assertEquals("id:3-5=6-7; ", htmlPos   .toString());
        assertEquals("Id:3-5=6-7; id:8-10=11-12; ", htmlUcPos .toString());
        assertEquals("Id:3-5=6-7; id:8-10=11-12; ", xmlPos    .toString());
        assertEquals("id:3-5=6-7; ", xmlLcPos .toString());
    }

    @Test void tracksFrag() {
        // https://github.com/jhy/jsoup/issues/2068
        String html = "<h1 id=1>One</h1>\n<h2 id=2>Two</h2><h10>Ten</h10>";
        Document shellDoc = Document.createShell("");

        List<Node> nodes = TrackingHtmlParser.parseFragmentInput(html, shellDoc.body(), shellDoc.baseUri());
        StringBuilder track = new StringBuilder();

        // nodes is the top level nodes - want to descend to check all tracked OK
        nodes.forEach(node -> node.nodeStream().forEach(descend -> {
            accumulatePositions(descend, track);
            accumulateAttributePositions(descend, track);
        }));

        assertEquals("h1:0-9~12-17; id:4-6=7-8; #text:9-12; #text:17-18; h2:18-27~30-35; id:22-24=25-26; #text:27-30; h10:35-40~43-49; #text:40-43; ", track.toString());
    }

    @Test void updateKeyMaintainsRangeLc() {
        String html = "<p xsi:CLASS=On>One</p>";
        Document doc = Jsoup.parse(html, TrackingHtmlParser);
        Element p = doc.expectFirst("p");
        Attribute attr = p.attribute("xsi:class");
        assertNotNull(attr);

        String expectedRange = "1,4:3-1,13:12=1,14:13-1,16:15";
        assertEquals(expectedRange, attr.sourceRange().toString());
        attr.setKey("class");
        assertEquals(expectedRange, attr.sourceRange().toString());
        assertEquals("class=\"On\"", attr.html());
    }

    @Test void updateKeyMaintainsRangeUc() {
        String html = "<p xsi:CLASS=On>One</p>";
        Document doc = Jsoup.parse(html, TrackingXmlParser);
        Element p = doc.expectFirst("p");
        Attribute attr = p.attribute("xsi:CLASS");
        assertNotNull(attr);

        String expectedRange = "1,4:3-1,13:12=1,14:13-1,16:15";
        assertEquals(expectedRange, attr.sourceRange().toString());
        attr.setKey("class");
        assertEquals(expectedRange, attr.sourceRange().toString());
        assertEquals("class=\"On\"", attr.html());

        attr.setKey("CLASSY");
        assertEquals(expectedRange, attr.sourceRange().toString());
        assertEquals("CLASSY=\"On\"", attr.html());

        attr.setValue("To");
        assertEquals(expectedRange, attr.sourceRange().toString());
        assertEquals("CLASSY=\"To\"", attr.html());

        assertEquals("<p CLASSY=\"To\">One</p>", p.outerHtml());

        p.attr("CLASSY", "Tree");
        assertEquals(expectedRange, attr.sourceRange().toString());
        assertEquals("CLASSY=\"To\"", attr.html()); // changes in this direction do not get to the attribute as it's not connected that way

        Attribute attr2 = p.attribute("CLASSY");
        assertEquals("CLASSY=\"Tree\"", attr2.html());
        assertEquals(expectedRange, attr2.sourceRange().toString());
    }

    static void accumulateAttributePositions(Node node, StringBuilder sb) {
        if (node instanceof LeafNode) return; // leafnode pseudo attributes are not tracked
        for (Attribute attribute : node.attributes()) {
            accumulatePositions(attribute, sb);
        }
    }

    static void accumulatePositions(Attribute attr, StringBuilder sb) {
        Range.AttributeRange range = attr.sourceRange();

        sb
            .append(attr.getKey())
            .append(':')
            .append(range.nameRange().startPos())
            .append('-')
            .append(range.nameRange().endPos())

            .append('=')
            .append(range.valueRange().startPos())
            .append('-')
            .append(range.valueRange().endPos());

        sb.append("; ");
    }
}