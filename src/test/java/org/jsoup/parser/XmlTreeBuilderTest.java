package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.jsoup.nodes.Document.OutputSettings.Syntax;
import static org.jsoup.parser.Parser.NamespaceHtml;
import static org.jsoup.parser.Parser.NamespaceXml;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests XmlTreeBuilder.
 *
 * @author Jonathan Hedley
 */
public class XmlTreeBuilderTest {
    @Test
    public void testSimpleXmlParse() {
        String xml = "<doc id=2 href='/bar'>Foo <br /><link>One</link><link>Two</link></doc>";
        XmlTreeBuilder tb = new XmlTreeBuilder();
        Document doc = tb.parse(xml, "http://foo.com/");
        assertEquals("<doc id=\"2\" href=\"/bar\">Foo <br /><link>One</link><link>Two</link></doc>",
                TextUtil.stripNewlines(doc.html()));
        assertEquals(doc.getElementById("2").absUrl("href"), "http://foo.com/bar");
    }

    @Test
    public void testPopToClose() {
        // test: </val> closes Two, </bar> ignored
        String xml = "<doc><val>One<val>Two</val></bar>Three</doc>";
        XmlTreeBuilder tb = new XmlTreeBuilder();
        Document doc = tb.parse(xml, "http://foo.com/");
        assertEquals("<doc><val>One<val>Two</val>Three</val></doc>",
                TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void testCommentAndDocType() {
        String xml = "<!DOCTYPE HTML><!-- a comment -->One <qux />Two";
        XmlTreeBuilder tb = new XmlTreeBuilder();
        Document doc = tb.parse(xml, "http://foo.com/");
        assertEquals("<!DOCTYPE HTML><!-- a comment -->One <qux />Two",
                TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void testSupplyParserToJsoupClass() {
        String xml = "<doc><val>One<val>Two</val></bar>Three</doc>";
        Document doc = Jsoup.parse(xml, "http://foo.com/", Parser.xmlParser());
        assertEquals("<doc><val>One<val>Two</val>Three</val></doc>",
                TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void testSupplyParserToDataStream() throws IOException, URISyntaxException {
        File xmlFile = new File(XmlTreeBuilder.class.getResource("/htmltests/xml-test.xml").toURI());
        InputStream inStream = new FileInputStream(xmlFile);
        Document doc = Jsoup.parse(inStream, null, "http://foo.com", Parser.xmlParser());
        assertEquals("<doc><val>One<val>Two</val>Three</val></doc>",
                TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void testDoesNotForceSelfClosingKnownTags() {
        // html will force "<br>one</br>" to logically "<br />One<br />". XML should be stay "<br>one</br> -- don't recognise tag.
        Document htmlDoc = Jsoup.parse("<br>one</br>");
        assertEquals("<br>\none\n<br>", htmlDoc.body().html());

        Document xmlDoc = Jsoup.parse("<br>one</br>", "", Parser.xmlParser());
        assertEquals("<br>one</br>", xmlDoc.html());
    }

    @Test public void handlesXmlDeclarationAsDeclaration() {
        String html = "<?xml encoding='UTF-8' ?><body>One</body><!-- comment -->";
        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
        assertEquals("<?xml encoding=\"UTF-8\"?><body>One</body><!-- comment -->",doc.outerHtml());
        assertEquals("#declaration", doc.childNode(0).nodeName());
        assertEquals("#comment", doc.childNode(2).nodeName());
    }

    @Test public void xmlFragment() {
        String xml = "<one src='/foo/' />Two<three><four /></three>";
        List<Node> nodes = Parser.parseXmlFragment(xml, "http://example.com/");
        assertEquals(3, nodes.size());

        assertEquals("http://example.com/foo/", nodes.get(0).absUrl("src"));
        assertEquals("one", nodes.get(0).nodeName());
        assertEquals("Two", ((TextNode)nodes.get(1)).text());
    }

    @Test public void xmlParseDefaultsToHtmlOutputSyntax() {
        Document doc = Jsoup.parse("x", "", Parser.xmlParser());
        assertEquals(Syntax.xml, doc.outputSettings().syntax());
    }

    @Test
    public void testDoesHandleEOFInTag() {
        String html = "<img src=asdf onerror=\"alert(1)\" x=";
        Document xmlDoc = Jsoup.parse(html, "", Parser.xmlParser());
        assertEquals("<img src=\"asdf\" onerror=\"alert(1)\" x=\"\"></img>", xmlDoc.html());
    }

    @Test
    public void testDetectCharsetEncodingDeclaration() throws IOException, URISyntaxException {
        File xmlFile = new File(XmlTreeBuilder.class.getResource("/htmltests/xml-charset.xml").toURI());
        InputStream inStream = new FileInputStream(xmlFile);
        Document doc = Jsoup.parse(inStream, null, "http://example.com/", Parser.xmlParser());
        assertEquals("ISO-8859-1", doc.charset().name());
        assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><data>äöåéü</data>",
            TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void testParseDeclarationAttributes() {
        String xml = "<?xml version='1' encoding='UTF-8' something='else'?><val>One</val>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        XmlDeclaration decl = (XmlDeclaration) doc.childNode(0);
        assertEquals("1", decl.attr("version"));
        assertEquals("UTF-8", decl.attr("encoding"));
        assertEquals("else", decl.attr("something"));
        assertEquals("version=\"1\" encoding=\"UTF-8\" something=\"else\"", decl.getWholeDeclaration());
        assertEquals("<?xml version=\"1\" encoding=\"UTF-8\" something=\"else\"?>", decl.outerHtml());
    }

    @Test
    public void testParseDeclarationWithoutAttributes() {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<?myProcessingInstruction My Processing instruction.?>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        XmlDeclaration decl = (XmlDeclaration) doc.childNode(2);
        assertEquals("myProcessingInstruction", decl.name());
        assertTrue(decl.hasAttr("My"));
        assertEquals("<?myProcessingInstruction My Processing instruction.?>", decl.outerHtml());
    }

    @Test
    public void caseSensitiveDeclaration() {
        String xml = "<?XML version='1' encoding='UTF-8' something='else'?>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        assertEquals("<?XML version=\"1\" encoding=\"UTF-8\" something=\"else\"?>", doc.outerHtml());
    }

    @Test
    public void testCreatesValidProlog() {
        Document document = Document.createShell("");
        document.outputSettings().syntax(Syntax.xml);
        document.charset(StandardCharsets.UTF_8);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<html>\n" +
            " <head></head>\n" +
            " <body></body>\n" +
            "</html>", document.outerHtml());
    }

    @Test
    public void preservesCaseByDefault() {
        String xml = "<CHECK>One</CHECK><TEST ID=1>Check</TEST>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        assertEquals("<CHECK>One</CHECK><TEST ID=\"1\">Check</TEST>", TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void appendPreservesCaseByDefault() {
        String xml = "<One>One</One>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        Elements one = doc.select("One");
        one.append("<Two ID=2>Two</Two>");
        assertEquals("<One>One<Two ID=\"2\">Two</Two></One>", TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void disablesPrettyPrintingByDefault() {
        String xml = "\n\n<div><one>One</one><one>\n Two</one>\n</div>\n ";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        assertEquals(xml, doc.html());
    }

    @Test
    public void canNormalizeCase() {
        String xml = "<TEST ID=1>Check</TEST>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser().settings(ParseSettings.htmlDefault));
        assertEquals("<test id=\"1\">Check</test>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void normalizesDiscordantTags() {
        Parser parser = Parser.xmlParser().settings(ParseSettings.htmlDefault);
        Document document = Jsoup.parse("<div>test</DIV><p></p>", "", parser);
        assertEquals("<div>test</div><p></p>", document.html());
        // was failing -> toString() = "<div>\n test\n <p></p>\n</div>"
    }

    @Test public void roundTripsCdata() {
        String xml = "<div id=1><![CDATA[\n<html>\n <foo><&amp;]]></div>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());

        Element div = doc.getElementById("1");
        assertEquals("<html>\n <foo><&amp;", div.text());
        assertEquals(0, div.children().size());
        assertEquals(1, div.childNodeSize()); // no elements, one text node

        assertEquals("<div id=\"1\"><![CDATA[\n<html>\n <foo><&amp;]]></div>", div.outerHtml());

        CDataNode cdata = (CDataNode) div.textNodes().get(0);
        assertEquals("\n<html>\n <foo><&amp;", cdata.text());
    }

    @Test public void cdataPreservesWhiteSpace() {
        String xml = "<script type=\"text/javascript\">//<![CDATA[\n\n  foo();\n//]]></script>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        assertEquals(xml, doc.outerHtml());

        assertEquals("//\n\n  foo();\n//", doc.selectFirst("script").text());
    }

    @Test
    public void handlesDodgyXmlDecl() {
        String xml = "<?xml version='1.0'><val>One</val>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        assertEquals("One", doc.select("val").text());
    }

    @Test
    public void handlesLTinScript() {
        // https://github.com/jhy/jsoup/issues/1139
        String html = "<script> var a=\"<?\"; var b=\"?>\"; </script>";
        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
        assertEquals("<script> var a=\"<!--?\"; var b=\"?-->\"; </script>", doc.html()); // converted from pseudo xmldecl to comment
    }

    @Test public void dropsDuplicateAttributes() {
        // case sensitive, so should drop Four and Five
        String html = "<p One=One ONE=Two one=Three One=Four ONE=Five two=Six two=Seven Two=Eight>Text</p>";
        Parser parser = Parser.xmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(html, "");

        assertEquals("<p One=\"One\" ONE=\"Two\" one=\"Three\" two=\"Six\" Two=\"Eight\">Text</p>", doc.selectFirst("p").outerHtml());
    }

    @Test public void readerClosedAfterParse() {
        Document doc = Jsoup.parse("Hello", "", Parser.xmlParser());
        TreeBuilder treeBuilder = doc.parser().getTreeBuilder();
        assertNull(treeBuilder.reader);
        assertNull(treeBuilder.tokeniser);
    }

    @Test public void xmlParserEnablesXmlOutputAndEscapes() {
        // Test that when using the XML parser, the output mode and escape mode default to XHTML entities
        Document doc = Jsoup.parse("<root/>", "", Parser.xmlParser());
        assertEquals(doc.outputSettings().syntax(), Syntax.xml);
        assertEquals(doc.outputSettings().escapeMode(), Entities.EscapeMode.xhtml);
    }

    @Test public void xmlSyntaxAlwaysEscapesLtAndGtInAttributeValues() {
        // https://github.com/jhy/jsoup/issues/2337
        Document doc = Jsoup.parse("<p one='&lt;two&gt;'>Three</p>", "", Parser.xmlParser());
        doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
        assertEquals(doc.outputSettings().syntax(), Syntax.xml);
        assertEquals("<p one=\"&lt;two&gt;\">Three</p>", doc.html());
    }

    @Test void xmlOutputCorrectsInvalidAttributeNames() {
        String xml = "<body style=\"color: red\" \" name\"><div =\"\"></div></body>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());
        assertEquals(Syntax.xml, doc.outputSettings().syntax());

        String out = doc.html();
        assertEquals("<body style=\"color: red\" _=\"\" name_=\"\"><div _=\"\"></div></body>", out);
    }

    @Test void xmlValidAttributes() {
        String xml = "<a bB1-_:.=foo _9!=bar xmlns:p1=qux>One</a>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());
        assertEquals(Syntax.xml, doc.outputSettings().syntax());

        String out = doc.html();
        assertEquals("<a bB1-_:.=\"foo\" _9_=\"bar\" xmlns:p1=\"qux\">One</a>", out); // first is same, second coerced
    }

    @Test void customTagsAreFlyweights() {
        String xml = "<foo>Foo</foo><foo>Foo</foo><FOO>FOO</FOO><FOO>FOO</FOO>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());
        Elements els = doc.children();

        Tag t1 = els.get(0).tag();
        Tag t2 = els.get(1).tag();
        Tag t3 = els.get(2).tag();
        Tag t4 = els.get(3).tag();
        assertEquals("foo", t1.getName());
        assertEquals("FOO", t3.getName());
        assertSame(t1, t2);
        assertSame(t3, t4);
    }

    @Test void rootHasXmlSettings() {
        Document doc = Jsoup.parse("<foo>", Parser.xmlParser());
        ParseSettings settings = doc.parser().settings();
        assertTrue(settings.preserveTagCase());
        assertTrue(settings.preserveAttributeCase());
        assertEquals(NamespaceXml, doc.parser().defaultNamespace());
    }

    @Test void xmlNamespace() {
        String xml = "<foo><bar><div><svg><math>Qux</bar></foo>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());

        assertXmlNamespace(doc);
        Elements els = doc.select("*");
        for (Element el : els) {
            assertXmlNamespace(el);
        }

        Document clone = doc.clone();
        assertXmlNamespace(clone);
        assertXmlNamespace(clone.expectFirst("bar"));

        Document shallow = doc.shallowClone();
        assertXmlNamespace(shallow);
    }

    private static void assertXmlNamespace(Element el) {
        assertEquals(NamespaceXml, el.tag().namespace(), String.format("Element %s not in XML namespace", el.tagName()));
    }

    @Test void declarations() {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE html\n" +
            "  PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
            "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
            "<!ELEMENT footnote (#PCDATA|a)*>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());

        XmlDeclaration proc = (XmlDeclaration) doc.childNode(0);
        DocumentType doctype = (DocumentType) doc.childNode(1);
        XmlDeclaration decl = (XmlDeclaration) doc.childNode(2);

        assertEquals("xml", proc.name());
        assertEquals("1.0", proc.attr("version"));
        assertEquals("utf-8", proc.attr("encoding"));
        assertEquals("version=\"1.0\" encoding=\"utf-8\"", proc.getWholeDeclaration());
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>", proc.outerHtml());

        assertEquals("html", doctype.name());
        assertEquals("-//W3C//DTD XHTML 1.0 Transitional//EN", doctype.attr("publicId"));
        assertEquals("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", doctype.attr("systemId"));
        assertEquals("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">", doctype.outerHtml());

        assertEquals("ELEMENT", decl.name());
        assertEquals("footnote (#PCDATA|a)*", decl.getWholeDeclaration());
        assertTrue(decl.hasAttr("footNote"));
        assertFalse(decl.hasAttr("ELEMENT"));
        assertEquals("<!ELEMENT footnote (#PCDATA|a)*>", decl.outerHtml());

        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
            "<!ELEMENT footnote (#PCDATA|a)*>", doc.outerHtml());
    }

    @Test void declarationWithGt() {
        // https://github.com/jhy/jsoup/issues/1947
        String xml = "<x><?xmlDeclaration att1=\"value1\" att2=\"&lt;val2>\"?></x>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());
        XmlDeclaration decl = (XmlDeclaration) doc.expectFirst("x").childNode(0);
        assertEquals("<val2>", decl.attr("att2"));
    }

    @Test void xmlHeaderIsValid() {
        // https://github.com/jhy/jsoup/issues/2298
        String xml = "<?xml version=\"1.0\"?>\n<root></root>";
        String expect = xml;

        Document doc = Jsoup.parse(xml, Parser.xmlParser().setTrackErrors(10));
        assertEquals(0, doc.parser().getErrors().size());
        assertEquals(expect, doc.html());

        xml =  "<?xml version=\"1.0\" ?>\n<root></root>";
        doc = Jsoup.parse(xml, Parser.xmlParser().setTrackErrors(10));
        assertEquals(0, doc.parser().getErrors().size());
        assertEquals(expect, doc.html());
    }

    @Test void canSetCustomRcdataTag() {
        String inner = "Blah\nblah\n<foo></foo>&quot;";
        String innerText = "Blah\nblah\n<foo></foo>\"";

        String xml = "<x><y><z>" + inner + "</z></y></x><x><z id=2></z>";
        TagSet custom = new TagSet();
        Tag z = custom.valueOf("z", NamespaceXml, ParseSettings.preserveCase);
        z.set(Tag.RcData);

        Document doc = Jsoup.parse(xml, Parser.xmlParser().tagSet(custom));
        Element zEl = doc.expectFirst("z");
        assertNotSame(z, zEl.tag()); // not same because we copy the tagset
        assertEquals(z, zEl.tag());

        assertEquals(1, zEl.childNodeSize());
        Node child = zEl.childNode(0);
        assertTrue(child instanceof TextNode);
        assertEquals(innerText, ((TextNode) child).getWholeText());

        // test fragment context parse - should parse <foo> as text
        Element z2 = doc.expectFirst("#2");
        z2.html(inner);
        assertEquals(innerText, z2.wholeText());
    }

    @Test void canSetCustomDataTag() {
        String inner = "Blah\nblah\n<foo></foo>&quot;"; // no character refs, will be as-is

        String xml = "<x><y><z>" + inner + "</z></y></x><x><z id=2></z>";
        TagSet custom = new TagSet();
        Tag z = custom.valueOf("z", NamespaceXml, ParseSettings.preserveCase);
        z.set(Tag.Data);

        Document doc = Jsoup.parse(xml, Parser.xmlParser().tagSet(custom));
        Element zEl = doc.expectFirst("z");
        assertNotSame(z, zEl.tag()); // not same because we copy the tagset
        assertEquals(z, zEl.tag());

        assertEquals(1, zEl.childNodeSize());
        Node child = zEl.childNode(0);
        assertTrue(child instanceof DataNode);
        assertEquals(inner, ((DataNode) child).getWholeData());
        assertEquals(inner, zEl.data());

        // test fragment context parse - should parse <foo> as data
        Element z2 = doc.expectFirst("#2");
        z2.html(inner);
        assertEquals(inner, ((DataNode) child).getWholeData());
        assertEquals(inner, zEl.data());
    }

    @Test void canSetCustomVoid() {
        String ns = "custom";
        String xml = "<x xmlns=custom><foo><link><meta>";
        TagSet custom = new TagSet();
        custom.valueOf("link", ns).set(Tag.Void);
        custom.valueOf("meta", ns).set(Tag.Void);
        custom.valueOf("foo", "other").set(Tag.Void); // ns doesn't match, won't impact

        Document doc = Jsoup.parse(xml, Parser.xmlParser().tagSet(custom));
        String expect = "<x xmlns=\"custom\"><foo><link /><meta /></foo></x>";
        assertEquals(expect, doc.html());
    }

    @Test void canSupplyWithHtmlTagSet() {
        // use the properties of html tag set but without HtmlTreeBuilder rules
        String xml = "<html xmlns=" + NamespaceHtml + "><div><script>a<b</script><img><p>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser().tagSet(TagSet.Html()));
        doc.outputSettings().prettyPrint(true);
        String expect = "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            " <div>\n" +
            "  <script>//<![CDATA[\n" +
            "a<b\n" +
            "//]]></script>\n" +
            "  <img />\n" +
            "  <p></p>\n" +
            " </div>\n" +
            "</html>";
        assertEquals(expect, doc.html());

        doc.outputSettings().syntax(Syntax.html);
        expect = "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            " <div>\n" +
            "  <script>a<b</script>\n" +
            "  <img>\n" +
            "  <p></p>\n" +
            " </div>\n" +
            "</html>";
        assertEquals(expect, doc.html());
    }

    @Test void prettyFormatsTextInline() {
        // https://github.com/jhy/jsoup/issues/2141
        String xml = "<package><metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
            "<dc:identifier id=\"pub-id\">id</dc:identifier>\n" +
            "<dc:title>title</dc:title>\n" +
            "<dc:language>ja</dc:language>\n" +
            "<dc:description>desc</dc:description>\n" +
            "</metadata></package>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());
        doc.outputSettings().prettyPrint(true);
        assertEquals("<package>\n" +
            " <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
            "  <dc:identifier id=\"pub-id\">id</dc:identifier> <dc:title>title</dc:title> <dc:language>ja</dc:language> <dc:description>desc</dc:description>\n" +
            " </metadata>\n" +
            "</package>", doc.html());

        // can customize
        Element meta = doc.expectFirst("metadata");
        Tag metaTag = meta.tag();
        metaTag.set(Tag.Block);
        // set all the inner els of meta to be blocks
        for (Element inner : meta) inner.tag().set(Tag.Block);

        assertEquals("<package>\n" +
            " <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
            "  <dc:identifier id=\"pub-id\">id</dc:identifier>\n" +
            "  <dc:title>title</dc:title>\n" +
            "  <dc:language>ja</dc:language>\n" +
            "  <dc:description>desc</dc:description>\n" +
            " </metadata>\n" +
            "</package>", doc.html());
    }

    // namespace tests
    @Test void xmlns() {
        // example from the xml namespace spec https://www.w3.org/TR/xml-names/
        String xml = "<?xml version=\"1.0\"?>\n" +
            "<!-- both namespace prefixes are available throughout -->\n" +
            "<bk:book xmlns:bk=\"urn:loc.gov:books\" xmlns:isbn=\"urn:ISBN:0-395-36341-6\">\n" +
            "    <bk:title>Cheaper by the Dozen</bk:title>\n" +
            "    <isbn:number>1568491379</isbn:number>\n" +
            "</bk:book>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());

        Element book = doc.expectFirst("bk|book");
        assertEquals("bk:book", book.tag().name());
        assertEquals("bk", book.tag().prefix());
        assertEquals("book", book.tag().localName());
        assertEquals("urn:loc.gov:books", book.tag().namespace());

        Element title = doc.expectFirst("bk|title");
        assertEquals("bk:title", title.tag().name());
        assertEquals("urn:loc.gov:books", title.tag().namespace());

        Element number = doc.expectFirst("isbn|number");
        assertEquals("isbn:number", number.tag().name());
        assertEquals("urn:ISBN:0-395-36341-6", number.tag().namespace());

        // and we didn't modify the dom
        assertEquals(xml, doc.html());
    }

    @Test void unprefixedDefaults() {
        String xml = "<?xml version=\"1.0\"?>\n" +
            "<!-- elements are in the HTML namespace, in this case by default -->\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "  <head><title>Frobnostication</title></head>\n" +
            "  <body><p>Moved to \n" +
            "    <a href=\"http://frob.example.com\">here</a>.</p></body>\n" +
            "</html>";

        Document doc = Jsoup.parse(xml, Parser.xmlParser());
        Element html = doc.expectFirst("html");
        assertEquals(NamespaceHtml, html.tag().namespace());
        Element a = doc.expectFirst("a");
        assertEquals(NamespaceHtml, a.tag().namespace());
    }

    @Test void emptyDefault() {
        String xml = "<?xml version='1.0'?>\n" +
            "<Beers>\n" +
            "  <!-- the default namespace inside tables is that of HTML -->\n" +
            "  <table xmlns='http://www.w3.org/1999/xhtml'>\n" +
            "   <th><td>Name</td><td>Origin</td><td>Description</td></th>\n" +
            "   <tr> \n" +
            "     <!-- no default namespace inside table cells -->\n" +
            "     <td><brandName xmlns=\"\">Huntsman</brandName></td>\n" +
            "     <td><origin xmlns=\"\">Bath, UK</origin></td>\n" +
            "     <td>\n" +
            "       <details xmlns=\"\"><class>Bitter</class><hop>Fuggles</hop>\n" +
            "         <pro>Wonderful hop, light alcohol, good summer beer</pro>\n" +
            "         <con>Fragile; excessive variance pub to pub</con>\n" +
            "         </details>\n" +
            "        </td>\n" +
            "      </tr>\n" +
            "    </table>\n" +
            "  </Beers>";

        Document doc = Jsoup.parse(xml, Parser.xmlParser());
        Element beers = doc.expectFirst("Beers");
        assertEquals(NamespaceXml, beers.tag().namespace());
        Element td = doc.expectFirst("td");
        assertEquals(NamespaceHtml, td.tag().namespace());
        Element origin = doc.expectFirst("origin");
        assertEquals("", origin.tag().namespace());
        Element pro = doc.expectFirst("pro");
        assertEquals("", pro.tag().namespace());
    }

    @Test void namespacedAttribute() {
        String xml = "<x xmlns:edi='http://ecommerce.example.org/schema'>\n" +
            "  <!-- the 'taxClass' attribute's namespace is http://ecommerce.example.org/schema -->\n" +
            "  <lineItem edi:taxClass=\"exempt\" other=foo>Baby food</lineItem>\n" +
            "</x>";

        Document doc = Jsoup.parse(xml, Parser.xmlParser());
        Element lineItem = doc.expectFirst("lineItem");

        Attribute taxClass = lineItem.attribute("edi:taxClass");
        assertNotNull(taxClass);
        assertEquals("edi", taxClass.prefix());
        assertEquals("taxClass", taxClass.localName());
        assertEquals("http://ecommerce.example.org/schema", taxClass.namespace());

        Attribute other = lineItem.attribute("other");
        assertNotNull(other);
        assertEquals("foo", other.getValue());
        assertEquals("", other.prefix());
        assertEquals("other", other.localName());
        assertEquals("", other.namespace());
    }

    @Test void elementsViaAppendHtmlAreNamespaced() {
        // tests that when elements / attributes are added via a fragment parse, they inherit the namespace stack, and can still override
        String xml = "<out xmlns='/out'><bk:book xmlns:bk='/books' xmlns:edi='/edi'><bk:title>Test</bk:title><li edi:foo='bar'></bk:book></out>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());

        // insert some parsed xml, inherit bk and edi, and with an inner node override bk
        Element book = doc.expectFirst("bk|book");
        book.append("<bk:content edi:foo=qux>Content</bk:content>");

        Element out = doc.expectFirst("out");
        assertEquals("/out", out.tag().namespace());

        Element content = book.expectFirst("bk|content");
        assertEquals("bk:content", content.tag().name());
        assertEquals("/books", content.tag().namespace());
        assertEquals("/edi", content.attribute("edi:foo").namespace());

        content.append("<data>Data</data><html xmlns='/html' xmlns:bk='/update'><p>Foo</p><bk:news>News</bk:news></html>");
        // p should be in /html, news in /update
        Element p = content.expectFirst("p");
        assertEquals("/html", p.tag().namespace());
        Element news = content.expectFirst("bk|news");
        assertEquals("/update", news.tag().namespace());
        Element data = content.expectFirst("data");
        assertEquals("/out", data.tag().namespace());
    }

    @Test void selfClosingOK() {
        // In XML, all tags can be self-closing regardless of tag type
        Parser parser = Parser.xmlParser().setTrackErrors(10);
        String xml = "<div id='1'/><p/><div>Foo</div><div></div><foo></foo>";
        Document doc = Jsoup.parse(xml, "", parser);
        ParseErrorList errors = parser.getErrors();
        assertEquals(0, errors.size());
        assertEquals("<div id=\"1\" /><p /><div>Foo</div><div /><foo></foo>", TextUtil.stripNewlines(doc.outerHtml()));
        // we infer that empty els can be represented with self-closing if seen in parse
    }
}
