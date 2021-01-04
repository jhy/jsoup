package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import java.util.Map;

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
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><head><title>W3c</title></head><body><p class=\"one\" id=\"12\">Text</p><!-- comment --><invalid>What<script>alert('!')</script></invalid></body></html>";
        assertEquals(expected, TextUtil.stripNewlines(out));

        Document roundTrip = parseXml(out, true);
        assertEquals("Text", roundTrip.getElementsByTagName("p").item(0).getTextContent());

        // check we can set properties
        Map<String, String> properties = W3CDom.OutputXml();
        properties.put(OutputKeys.INDENT, "yes");
        String furtherOut = W3CDom.asString(wDoc, properties);
        assertTrue(furtherOut.length() > out.length()); // wanted to assert formatting, but actual indentation is platform specific so breaks in CI
        String furtherExpected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><head><title>W3c</title></head><body><p class=\"one\" id=\"12\">Text</p><!-- comment --><invalid>What<script>alert('!')</script></invalid></body></html>";
        assertEquals(furtherExpected, TextUtil.stripNewlines(furtherOut)); // on windows, DOM will write newlines as \r\n
    }

    @Test
    public void convertsGoogle() throws IOException {
        File in = ParseTest.getFile("/htmltests/google-ipod.html.gz");
        org.jsoup.nodes.Document doc = Jsoup.parse(in, "UTF8");

        W3CDom w3c = new W3CDom();
        Document wDoc = w3c.fromJsoup(doc);
        Node htmlEl = wDoc.getChildNodes().item(1);
        assertNull(htmlEl.getNamespaceURI());
        assertEquals("html", htmlEl.getLocalName());
        assertEquals("html", htmlEl.getNodeName());

        DocumentType doctype = wDoc.getDoctype();
        Node doctypeNode = wDoc.getChildNodes().item(0);
        assertSame(doctype, doctypeNode);
        assertEquals("html", doctype.getName());

        String xml = W3CDom.asString(wDoc, W3CDom.OutputXml());
        assertTrue(xml.contains("ipod"));

        Document roundTrip = parseXml(xml, true);
        assertEquals("Images", roundTrip.getElementsByTagName("a").item(0).getTextContent());
    }

    @Test
    public void convertsGoogleLocation() throws IOException {
        File in = ParseTest.getFile("/htmltests/google-ipod.html.gz");
        org.jsoup.nodes.Document doc = Jsoup.parse(in, "UTF8");

        W3CDom w3c = new W3CDom();
        Document wDoc = w3c.fromJsoup(doc);

        String out = w3c.asString(wDoc);
        assertEquals(doc.location(), wDoc.getDocumentURI());
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

        Document w3Doc = new W3CDom().fromJsoup(jsoupDoc);
    }

    @Test
    public void treatsUndeclaredNamespaceAsLocalName() {
        String html = "<fb:like>One</fb:like>";
        org.jsoup.nodes.Document doc = Jsoup.parse(html);

        Document w3Doc = new W3CDom().fromJsoup(doc);
        Node htmlEl = w3Doc.getFirstChild();

        assertNull(htmlEl.getNamespaceURI());
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
        NodeList nodeList = xpath(dom, "//body");// no ns, so needs no prefix
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

    private NodeList xpath(Document w3cDoc, String query) throws XPathExpressionException {
        XPathExpression xpath = XPathFactory.newInstance().newXPath().compile(query);
        return ((NodeList) xpath.evaluate(w3cDoc, XPathConstants.NODE));
    }

    @Test
    public void testRoundTripDoctype() {
        // TODO - not super happy with this output - but plain DOM doesn't let it out, and don't want to rebuild the writer
        String base = "<!DOCTYPE html><p>One</p>";
        assertEquals("<!DOCTYPE html SYSTEM \"about:legacy-compat\"><html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>One</p></body></html>",
            output(base, true));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html SYSTEM \"about:legacy-compat\"><html><head/><body><p>One</p></body></html>", output(base, false));

        String publicDoc = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
        assertEquals("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body></body></html>", output(publicDoc, true));
        // different impls will have different XML formatting. OpenJDK 13 default gives this: <body /> but others have <body/>, so just check start
        assertTrue(output(publicDoc, false).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC"));

        String systemDoc = "<!DOCTYPE html SYSTEM \"exampledtdfile.dtd\">";
        assertEquals("<!DOCTYPE html SYSTEM \"exampledtdfile.dtd\"><html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body></body></html>", output(systemDoc, true));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html SYSTEM \"exampledtdfile.dtd\"><html><head/><body/></html>", output(systemDoc, false));

        String legacyDoc = "<!DOCTYPE html SYSTEM \"about:legacy-compat\">";
        assertEquals("<!DOCTYPE html SYSTEM \"about:legacy-compat\"><html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body></body></html>", output(legacyDoc, true));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html SYSTEM \"about:legacy-compat\"><html><head/><body/></html>", output(legacyDoc, false));

        String noDoctype = "<p>One</p>";
        assertEquals("<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>One</p></body></html>", output(noDoctype, true));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><head/><body><p>One</p></body></html>", output(noDoctype, false));
    }

    private String output(String in, boolean modeHtml) {
        org.jsoup.nodes.Document jdoc = Jsoup.parse(in);
        Document w3c = W3CDom.convert(jdoc);

        Map<String, String> properties = modeHtml ? W3CDom.OutputHtml() : W3CDom.OutputXml();
        return TextUtil.stripNewlines(W3CDom.asString(w3c, properties));
    }


    @Test
    public void checkElementsAttributesCaseSensitivity() throws IOException {
        File in = ParseTest.getFile("/htmltests/attributes-case-sensitivity-test.html");
        org.jsoup.nodes.Document jsoupDoc;
        jsoupDoc = Jsoup.parse(in, "UTF-8");

        org.jsoup.helper.W3CDom jDom = new org.jsoup.helper.W3CDom();
        Document doc = jDom.fromJsoup(jsoupDoc);

        final org.w3c.dom.Element body = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("body").item(0);

        final NodeList imgs = body.getElementsByTagName("img");
        assertEquals(2, imgs.getLength());

        final org.w3c.dom.Element first = (org.w3c.dom.Element) imgs.item(0);
        assertEquals(first.getAttributes().getLength(), 2);
        final String img1 = first.getAttribute("src");
        assertEquals("firstImage.jpg", img1);
        final String alt1 = first.getAttribute("alt");
        assertEquals("Alt one", alt1);

        final org.w3c.dom.Element second = (org.w3c.dom.Element) imgs.item(1);
        assertEquals(second.getAttributes().getLength(), 2);
        final String img2 = second.getAttribute("src");
        assertEquals("secondImage.jpg", img2);
        final String alt2 = second.getAttribute("alt");
        assertEquals("Alt two", alt2);
    }
}
