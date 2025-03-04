package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.jsoup.TextUtil.normalizeSpaces;
import static org.jsoup.nodes.Document.OutputSettings.Syntax.xml;
import static org.junit.jupiter.api.Assertions.*;

public class W3CDomTest {

    private static Document parseXml(String xml, boolean nameSpaceAware) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(nameSpaceAware);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> {
                if (systemId.contains("about:legacy-compat")) { // <!doctype html>
                    return new InputSource(new StringReader(""));
                } else {
                    return null;
                }
            });
            Document dom = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            dom.normalizeDocument();
            return dom;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void simpleConversion() {
        String html = "<html><head><title>W3c</title></head><body><p class='one' id=12>Text</p><!-- comment --><invalid>What<script>alert('!')";
        org.jsoup.nodes.Document doc = Jsoup.parse(html);

        W3CDom w3c = new W3CDom();
        Document wDoc = w3c.fromJsoup(doc);
        NodeList meta = wDoc.getElementsByTagName("META");
        assertEquals(0, meta.getLength());

        String out = W3CDom.asString(wDoc, W3CDom.OutputXml());
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>W3c</title></head><body><p class=\"one\" id=\"12\">Text</p><!-- comment --><invalid>What<script>alert('!')</script></invalid></body></html>";
        assertEquals(expected, TextUtil.stripNewlines(out));

        Document roundTrip = parseXml(out, true);
        assertEquals("Text", roundTrip.getElementsByTagName("p").item(0).getTextContent());

        // check we can set properties
        Map<String, String> properties = W3CDom.OutputXml();
        properties.put(OutputKeys.INDENT, "yes");
        String furtherOut = W3CDom.asString(wDoc, properties);
        assertTrue(furtherOut.length() > out.length()); // wanted to assert formatting, but actual indentation is platform specific so breaks in CI
        String furtherExpected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>W3c</title></head><body><p class=\"one\" id=\"12\">Text</p><!-- comment --><invalid>What<script>alert('!')</script></invalid></body></html>";
        assertEquals(furtherExpected, TextUtil.stripNewlines(furtherOut)); // on windows, DOM will write newlines as \r\n
    }

    @Test
    public void namespacePreservation() throws IOException {
        File in = ParseTest.getFile("/htmltests/namespaces.xhtml");
        org.jsoup.nodes.Document jsoupDoc;
        jsoupDoc = Jsoup.parse(in, "UTF-8");

        Document doc;
        org.jsoup.helper.W3CDom jDom = new org.jsoup.helper.W3CDom();
        doc = jDom.fromJsoup(jsoupDoc);

        Node htmlEl = doc.getChildNodes().item(0);
        assertEquals("http://www.w3.org/1999/xhtml", htmlEl.getNamespaceURI());
        assertEquals("html", htmlEl.getLocalName());
        assertEquals("html", htmlEl.getNodeName());

        // inherits default namespace
        Node head = htmlEl.getFirstChild().getNextSibling();
        assertEquals("http://www.w3.org/1999/xhtml", head.getNamespaceURI());
        assertEquals("head", head.getLocalName());
        assertEquals("head", head.getNodeName());

        Node epubTitle = htmlEl.getChildNodes().item(3).getChildNodes().item(3);
        assertEquals("Check", epubTitle.getTextContent());
        assertEquals("http://www.idpf.org/2007/ops", epubTitle.getNamespaceURI());
        assertEquals("title", epubTitle.getLocalName());
        assertEquals("epub:title", epubTitle.getNodeName());

        Node xSection = epubTitle.getNextSibling().getNextSibling();
        assertEquals("urn:test", xSection.getNamespaceURI());
        assertEquals("section", xSection.getLocalName());
        assertEquals("x:section", xSection.getNodeName());

        // https://github.com/jhy/jsoup/issues/977
        // does not keep last set namespace
        Node svg = xSection.getNextSibling().getNextSibling();
        assertEquals("http://www.w3.org/2000/svg", svg.getNamespaceURI());
        assertEquals("svg", svg.getLocalName());
        assertEquals("svg", svg.getNodeName());

        Node path = svg.getChildNodes().item(1);
        assertEquals("http://www.w3.org/2000/svg", path.getNamespaceURI());
        assertEquals("path", path.getLocalName());
        assertEquals("path", path.getNodeName());

        Node clip = path.getChildNodes().item(1);
        assertEquals("http://example.com/clip", clip.getNamespaceURI());
        assertEquals("clip", clip.getLocalName());
        assertEquals("clip", clip.getNodeName());
        assertEquals("456", clip.getTextContent());

        Node picture = svg.getNextSibling().getNextSibling();
        assertEquals("http://www.w3.org/1999/xhtml", picture.getNamespaceURI());
        assertEquals("picture", picture.getLocalName());
        assertEquals("picture", picture.getNodeName());

        Node img = picture.getFirstChild();
        assertEquals("http://www.w3.org/1999/xhtml", img.getNamespaceURI());
        assertEquals("img", img.getLocalName());
        assertEquals("img", img.getNodeName());
    }

    @Test
    public void handlesInvalidAttributeNames() {
        String html = "<html><head></head><body style=\"color: red\" \" name\"></body></html>";
        org.jsoup.nodes.Document jsoupDoc;
        jsoupDoc = Jsoup.parse(html);
        Element body = jsoupDoc.select("body").first();
        assertTrue(body.hasAttr("\"")); // actually an attribute with key '"'. Correct per HTML5 spec, but w3c xml dom doesn't dig it
        assertTrue(body.hasAttr("name\""));

        Document w3Doc = W3CDom.convert(jsoupDoc);
        String xml = W3CDom.asString(w3Doc, W3CDom.OutputXml());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head/><body _=\"\" name_=\"\" style=\"color: red\"/></html>", xml);
    }

    @Test
    public void htmlInputDocMaintainsHtmlAttributeNames() {
        String html = "<!DOCTYPE html><html><head></head><body><p hành=\"1\" hình=\"2\">unicode attr names</p></body></html>";
        org.jsoup.nodes.Document jsoupDoc;
        jsoupDoc = Jsoup.parse(html);

        Document w3Doc = W3CDom.convert(jsoupDoc);
        String out = W3CDom.asString(w3Doc, W3CDom.OutputHtml());
        String expected = "<!DOCTYPE html SYSTEM \"about:legacy-compat\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p hành=\"1\" hình=\"2\">unicode attr names</p></body></html>";
        assertEquals(expected, TextUtil.stripNewlines(out));
    }

    @Test
    public void xmlInputDocMaintainsHtmlAttributeNames() {
        String html = "<!DOCTYPE html><html><head></head><body><p hành=\"1\" hình=\"2\">unicode attr names coerced</p></body></html>";
        org.jsoup.nodes.Document jsoupDoc;
        jsoupDoc = Jsoup.parse(html);
        jsoupDoc.outputSettings().syntax(xml);

        Document w3Doc = W3CDom.convert(jsoupDoc);
        String out = W3CDom.asString(w3Doc, W3CDom.OutputHtml());
        String expected = "<!DOCTYPE html SYSTEM \"about:legacy-compat\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p h_nh=\"2\">unicode attr names coerced</p></body></html>";
        assertEquals(expected, TextUtil.stripNewlines(out));
    }

    @Test
    public void handlesInvalidTagAsText() {
        org.jsoup.nodes.Document jsoup = Jsoup.parse("<インセンティブで高収入！>Text <p>More</p>");

        Document w3Doc = W3CDom.convert(jsoup);
        String xml = W3CDom.asString(w3Doc, W3CDom.OutputXml());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head/><body>&lt;インセンティブで高収入！&gt;Text <p>More</p></body></html>", xml);
    }

    @Test void handlesHtmlElsWithLt() {
        // In HTML, elements can be named "foo<bar" (<foo<bar>). Test that we can convert to W3C, that we can HTML parse our HTML serial, XML parse our XML serial, and W3C XML parse the XML serial and the W3C serial
        // And similarly attributes may have "<" in their name
        // https://github.com/jhy/jsoup/issues/2259
        String input = "<foo<bar attr<name=\"123\"><b>Text</b></foo<bar>";
        String xmlExpect = "<foo_bar attr_name=\"123\"><b>Text</b></foo_bar>"; // rewrites < to _ in el and attr

        // html round trips
        org.jsoup.nodes.Document htmlDoc = Jsoup.parse(input);
        String htmlSerial = htmlDoc.body().html();
        assertEquals(input, normalizeSpaces(htmlSerial)); // same as input
        Element htmlRound = Jsoup.parse(htmlSerial).body();
        assertTrue(htmlDoc.body().hasSameValue(htmlRound));

        // xml round trips
        htmlDoc.outputSettings().syntax(xml);
        String asXml = htmlDoc.body().html();
        assertEquals(xmlExpect, normalizeSpaces(asXml)); // <foo<bar> -> <foo_bar>
        org.jsoup.nodes.Document xmlDoc = Jsoup.parse(asXml);
        String xmlSerial = xmlDoc.body().html();
        assertEquals(xmlExpect, normalizeSpaces(xmlSerial)); // same as xmlExpect
        Element xmlRound = Jsoup.parse(xmlSerial).body();
        assertTrue(xmlDoc.body().hasSameValue(xmlRound));

        // Can W3C parse that XML
        Document w3cXml = parseXml(asXml, true);
        NodeList w3cXmlNodes = w3cXml.getElementsByTagName("foo_bar");
        assertEquals(1, w3cXmlNodes.getLength());
        assertEquals("123", w3cXmlNodes.item(0).getAttributes().getNamedItem("attr_name").getTextContent());

        // Can convert to W3C
        Document w3cDoc = W3CDom.convert(htmlDoc);
        NodeList w3cNodes = w3cDoc.getElementsByTagName("foo_bar");
        assertEquals(1, w3cNodes.getLength());
        assertEquals("123", w3cNodes.item(0).getAttributes().getNamedItem("attr_name").getTextContent());
    }

    @Test
    public void canConvertToCustomDocument() throws ParserConfigurationException {
        org.jsoup.nodes.Document document = Jsoup.parse("<html><div></div></html>");

        DocumentBuilderFactory localDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        Document customDocumentResult = localDocumentBuilderFactory.newDocumentBuilder().newDocument();

        W3CDom w3cDom = new W3CDom();
        w3cDom.convert(document, customDocumentResult);

        String html = W3CDom.asString(customDocumentResult, W3CDom.OutputHtml());
        assertEquals("<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><div></div></body></html>", html);
    }

    @Test
    public void treatsUndeclaredNamespaceAsLocalName() {
        String html = "<fb:like>One</fb:like>";
        org.jsoup.nodes.Document doc = Jsoup.parse(html);

        Document w3Doc = new W3CDom().fromJsoup(doc);
        Node htmlEl = w3Doc.getFirstChild();

        assertEquals("http://www.w3.org/1999/xhtml", htmlEl.getNamespaceURI());
        assertEquals("html", htmlEl.getLocalName());
        assertEquals("html", htmlEl.getNodeName());

        Node fb = htmlEl.getFirstChild().getNextSibling().getFirstChild();
        assertNull(fb.getNamespaceURI());
        assertEquals("like", fb.getLocalName());
        assertEquals("fb:like", fb.getNodeName());
    }

    @Test
    public void xmlnsXpathTest() throws XPathExpressionException {
        W3CDom w3c = new W3CDom();
        String html = "<html><body><div>hello</div></body></html>";
        Document dom = w3c.fromJsoup(Jsoup.parse(html));
        NodeList nodeList = xpath(dom, "//*[local-name()=\"body\"]");// namespace aware; HTML namespace is default
        assertEquals("div", nodeList.item(0).getLocalName());

        // default output is namespace aware, so query needs to be as well
        html = "<html xmlns='http://www.w3.org/1999/xhtml'><body id='One'><div>hello</div></body></html>";
        dom = w3c.fromJsoup(Jsoup.parse(html));
        nodeList = xpath(dom, "//body");
        assertNull(nodeList); // no matches

        dom = w3c.fromJsoup(Jsoup.parse(html));
        nodeList = xpath(dom, "//*[local-name()=\"body\"]");
        assertNotNull(nodeList);
        assertEquals(1, nodeList.getLength());
        assertEquals("div", nodeList.item(0).getLocalName());
        assertEquals("http://www.w3.org/1999/xhtml", nodeList.item(0).getNamespaceURI());
        assertNull(nodeList.item(0).getPrefix());

        // get rid of the name space awareness
        String xml = w3c.asString(dom);
        dom = parseXml(xml, false);
        Node item = (Node) xpath(dom, "//body");
        assertEquals("body", item.getNodeName());
        assertNull(item.getNamespaceURI());
        assertNull(item.getPrefix());

        // put back, will get zero
        dom = parseXml(xml, true);
        nodeList = xpath(dom, "//body");
        assertNull(nodeList);
    }

    @Test
    public void xhtmlNoNamespace() throws XPathExpressionException {
        W3CDom w3c = new W3CDom();
        String html = "<html><body><div>hello</div></body></html>";
        w3c.namespaceAware(false);
        Document dom = w3c.fromJsoup(Jsoup.parse(html));
        NodeList nodeList = xpath(dom, "//body");// no namespace
        assertEquals(1, nodeList.getLength());
        assertEquals("div", nodeList.item(0).getLocalName());
    }

    @Test
    void canDisableNamespaces() throws XPathExpressionException {
        W3CDom w3c = new W3CDom();
        assertTrue(w3c.namespaceAware());

        w3c.namespaceAware(false);
        assertFalse(w3c.namespaceAware());

        String html = "<html xmlns='http://www.w3.org/1999/xhtml'><body id='One'><div>hello</div></body></html>";
        Document dom = w3c.fromJsoup(Jsoup.parse(html));
        NodeList nodeList = xpath(dom, "//body");// no ns, so needs no prefix
        assertEquals("div", nodeList.item(0).getLocalName());
    }

    private NodeList xpath(Document w3cDoc, String query) throws XPathExpressionException {
        XPathExpression xpath = XPathFactory.newInstance().newXPath().compile(query);
        return ((NodeList) xpath.evaluate(w3cDoc, XPathConstants.NODE));
    }

    @Test
    public void testRoundTripDoctype() {
        // TODO - not super happy with this output - but plain DOM doesn't let it out, and don't want to rebuild the writer
        // because we have Saxon on the test classpath, the transformer will change to that, and so case may change (e.g. Java base in META, Saxon is meta for HTML)
        String base = "<!DOCTYPE html><p>One</p>";
        assertEqualsIgnoreCase("<!DOCTYPE html SYSTEM \"about:legacy-compat\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>One</p></body></html>", output(base, true));
        assertEqualsIgnoreCase("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html SYSTEM \"about:legacy-compat\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head/><body><p>One</p></body></html>", output(base, false));

        String publicDoc = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
        assertEqualsIgnoreCase("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body></body></html>", output(publicDoc, true));
        // different impls will have different XML formatting. OpenJDK 13 default gives this: <body /> but others have <body/>, so just check start
        assertTrue(output(publicDoc, false).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC"));

        String systemDoc = "<!DOCTYPE html SYSTEM \"exampledtdfile.dtd\">";
        assertEqualsIgnoreCase("<!DOCTYPE html SYSTEM \"exampledtdfile.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body></body></html>", output(systemDoc, true));
        assertEqualsIgnoreCase("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html SYSTEM \"exampledtdfile.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head/><body/></html>", output(systemDoc, false));

        String legacyDoc = "<!DOCTYPE html SYSTEM \"about:legacy-compat\">";
        assertEqualsIgnoreCase("<!DOCTYPE html SYSTEM \"about:legacy-compat\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body></body></html>", output(legacyDoc, true));
        assertEqualsIgnoreCase("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html SYSTEM \"about:legacy-compat\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head/><body/></html>", output(legacyDoc, false));

        String noDoctype = "<p>One</p>";
        assertEqualsIgnoreCase("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>One</p></body></html>", output(noDoctype, true));
        assertEqualsIgnoreCase("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head/><body><p>One</p></body></html>", output(noDoctype, false));
    }

    private String output(String in, boolean modeHtml) {
        org.jsoup.nodes.Document jdoc = Jsoup.parse(in);
        Document w3c = W3CDom.convert(jdoc);

        Map<String, String> properties = modeHtml ? W3CDom.OutputHtml() : W3CDom.OutputXml();
        return normalizeSpaces(W3CDom.asString(w3c, properties));
    }

    private void assertEqualsIgnoreCase(String want, String have) {
        assertEquals(want.toLowerCase(Locale.ROOT), have.toLowerCase(Locale.ROOT));
    }


    @Test
    public void canOutputHtmlWithoutNamespace() {
        String html = "<p>One</p>";
        org.jsoup.nodes.Document jdoc = Jsoup.parse(html);
        W3CDom w3c = new W3CDom();
        w3c.namespaceAware(false);

        String asHtml = W3CDom.asString(w3c.fromJsoup(jdoc), W3CDom.OutputHtml());
        String asXtml = W3CDom.asString(w3c.fromJsoup(jdoc), W3CDom.OutputXml());
        assertEqualsIgnoreCase(
            "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"></head><body><p>one</p></body></html>",
            asHtml);
        assertEqualsIgnoreCase(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><head/><body><p>One</p></body></html>",
            asXtml);
    }

    @Test public void convertsElementsAndMaintainsSource() {
        org.jsoup.nodes.Document jdoc = Jsoup.parse("<body><div><p>One</div><div><p>Two");
        W3CDom w3CDom = new W3CDom();
        Element jDiv = jdoc.selectFirst("div");
        assertNotNull(jDiv);
        Document doc = w3CDom.fromJsoup(jDiv);
        Node div = w3CDom.contextNode(doc);

        assertEquals("div", div.getLocalName());
        assertEquals(jDiv, div.getUserData(W3CDom.SourceProperty));

        Node textNode = div.getFirstChild().getFirstChild();
        assertEquals("One", textNode.getTextContent());
        assertEquals(Node.TEXT_NODE, textNode.getNodeType());

        org.jsoup.nodes.TextNode jText = (TextNode) jDiv.childNode(0).childNode(0);
        assertEquals(jText, textNode.getUserData(W3CDom.SourceProperty));
    }
    
    @Test public void canXmlParseCdataNodes() throws XPathExpressionException {
        String html = "<p><script>1 && 2</script><style>3 && 4</style> 5 &amp;&amp; 6</p>";
        org.jsoup.nodes.Document jdoc = Jsoup.parse(html);
        jdoc.outputSettings().syntax(xml);
        String xml = jdoc.body().html();
        assertTrue(xml.contains("<script>//<![CDATA[\n1 && 2\n//]]></script>")); // as asserted in ElementTest
        Document doc = parseXml(xml, false);
        NodeList list = xpath(doc, "//script");
        assertEquals(2, list.getLength());
        Node scriptComment = list.item(0); // will be the cdata node
        assertEquals("//", scriptComment.getTextContent());
        Node script = list.item(1);
        assertEquals("\n" +
            "1 && 2\n" +
            "//", script.getTextContent());

    }

    @Test public void handlesEmptyDoctype() {
        String html = "<!doctype>Foo";
        org.jsoup.nodes.Document jdoc = Jsoup.parse(html);
        Document doc = (new W3CDom()).fromJsoup(jdoc);
        assertNull(doc.getDoctype());
        assertEquals("Foo", doc.getFirstChild().getTextContent());
    }

    @Test void testHtmlParseAttributesAreCaseInsensitive() throws IOException {
        // https://github.com/jhy/jsoup/issues/981
        String html = "<html lang=en>\n" +
            "<body>\n" +
            "<img src=\"firstImage.jpg\" alt=\"Alt one\" />\n" +
            "<IMG SRC=\"secondImage.jpg\" AlT=\"Alt two\" />\n" +
            "</body>\n" +
            "</html>";
        org.jsoup.nodes.Document jsoupDoc;
        jsoupDoc = Jsoup.parse(html);
        org.jsoup.helper.W3CDom jDom = new org.jsoup.helper.W3CDom();
        Document doc = jDom.fromJsoup(jsoupDoc);
        org.w3c.dom.Element body = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("body").item(0);
        NodeList imgs = body.getElementsByTagName("img");
        assertEquals(2, imgs.getLength());
        org.w3c.dom.Element first = (org.w3c.dom.Element) imgs.item(0);
        assertEquals(first.getAttributes().getLength(), 2);
        String img1 = first.getAttribute("src");
        assertEquals("firstImage.jpg", img1);
        String alt1 = first.getAttribute("alt");
        assertEquals("Alt one", alt1);
        org.w3c.dom.Element second = (org.w3c.dom.Element) imgs.item(1);
        assertEquals(second.getAttributes().getLength(), 2);
        String img2 = second.getAttribute("src");
        assertEquals("secondImage.jpg", img2);
        String alt2 = second.getAttribute("alt");
        assertEquals("Alt two", alt2);
    }

    @ParameterizedTest
    @MethodSource("parserProvider")
    void doesNotExpandEntities(Parser parser) {
        // Tests that the billion laughs attack doesn't expand entities; also for XXE
        // Not impacted because jsoup doesn't parse the entities within the doctype, and so won't get to the w3c.
        // Added to confirm, and catch if that ever changes
        String billionLaughs = "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE lolz [\n" +
            " <!ENTITY lol \"lol\">\n" +
            " <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
            "]>\n" +
            "<html><body><p>&lol1;</p></body></html>";

        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(billionLaughs, parser);
        W3CDom w3cDom = new W3CDom();

        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        assertNotNull(w3cDoc);
        // select the p and make sure it's unexpanded
        NodeList p = w3cDoc.getElementsByTagName("p");
        assertEquals(1, p.getLength());
        assertEquals("&lol1;", p.item(0).getTextContent());

        // Check the string
        String string = W3CDom.asString(w3cDoc, W3CDom.OutputXml());
        assertFalse(string.contains("lololol"));
        assertTrue(string.contains("&amp;lol1;"));
    }

    private static Stream<Arguments> parserProvider() {
        return Stream.of(
            Arguments.of(Parser.htmlParser()),
            Arguments.of(Parser.xmlParser())
        );
    }

}
