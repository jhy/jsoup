package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;
import java.util.List;
import java.util.stream.Stream;

import static org.jsoup.helper.W3CDom.XPathFactoryProperty;
import static org.junit.jupiter.api.Assertions.*;

public class XpathTest {

    @Test
    public void supportsXpath() {
        String html = "<body><div><p>One</div><div><p>Two</div><div>Three</div>";
        Document doc = Jsoup.parse(html);

        Elements els = doc.selectXpath("//div/p");
        assertEquals(2, els.size());
        assertEquals("One", els.get(0).text());
        assertEquals("Two", els.get(1).text());
    }

    @Test public void supportsXpathFromElement() {
        String html = "<body><div><p>One</div><div><p>Two</div><div>Three</div>";
        Document doc = Jsoup.parse(html);

        Element div = doc.selectFirst("div");
        assertNotNull(div);
        Element w3cDiv = div.selectXpath(".").first(); // self
        assertSame(div, w3cDiv);

        Elements els = div.selectXpath("p");
        assertEquals(1, els.size());
        assertEquals("One", els.get(0).text());
        assertEquals("p", els.get(0).tagName());

        assertEquals(1, div.selectXpath("//body").size()); // the whole document is visible on the div context
        assertEquals(1, doc.selectXpath("//body").size());
    }

    @Test public void emptyElementsIfNoResults() {
        Document doc = Jsoup.parse("<p>One<p>Two");
        assertEquals(0, doc.selectXpath("//div").size());
    }

    @Test
    public void throwsSelectException() {
        Document doc = Jsoup.parse("<p>One<p>Two");
        boolean threw = false;
        try {
            doc.selectXpath("//???");
        } catch (Selector.SelectorParseException e) {
            threw = true;
            // checks exception message within jsoup's control, rest may be JDK impl specific
            // was - Could not evaluate XPath query [//???]: javax.xml.transform.TransformerException: A location step was expected following the '/' or '//' token.
            assertTrue(e.getMessage().startsWith("Could not evaluate XPath query [//???]:"));
        }
        assertTrue(threw);
    }

    @Test
    public void supportsLocalname() {
        String xhtml = "<html xmlns='http://www.w3.org/1999/xhtml'><body id='One'><div>hello</div></body></html>";
        Document doc = Jsoup.parse(xhtml, Parser.xmlParser());
        Elements elements = doc.selectXpath("//*[local-name()='body']");
        assertEquals(1, elements.size());
        assertEquals("One", elements.first().id());
    }

    @Test
    public void canDitchNamespaces() {
        String xhtml = "<html xmlns='http://www.w3.org/1999/xhtml'><body id='One'><div>hello</div></body></html>";
        Document doc = Jsoup.parse(xhtml, Parser.xmlParser());
        doc.select("[xmlns]").removeAttr("xmlns");
        Elements elements = doc.selectXpath("//*[local-name()='body']");
        assertEquals(1, elements.size());

        elements = doc.selectXpath("//body");
        assertEquals(1, elements.size());
        assertEquals("One", elements.first().id());
    }

    @ParameterizedTest
    @MethodSource("provideEvaluators")
    void cssAndXpathEquivalents(Document doc, String css, String xpath) {
        Elements fromCss = doc.select(css);
        Elements fromXpath = doc.selectXpath(xpath);

        assertTrue(fromCss.size() >= 1);
        assertTrue(fromXpath.size() >= 1);
        // tests same size, order, and contents
        assertEquals(fromCss, fromXpath);
    }

    private static Stream<Arguments> provideEvaluators() {
        String html = "<div id=1><div id=2><p class=foo>Hello</p></div></div><DIV id=3>";
        Document doc = Jsoup.parse(html);

        return Stream.of(
           Arguments.of(doc, "DIV", "//div"),
           Arguments.of(doc, "div > p.foo", "//div/p[@class]"),
           Arguments.of(doc, "div + div", "//div/following-sibling::div[1]"),
           Arguments.of(doc, "p:containsOwn(Hello)", "//p[contains(text(),\"Hello\")]")
        );
    }

    @Test void canSelectTextNodes() {
        String html = "<div><p>One<p><a>Two</a><p>Three and some more";
        Document doc = Jsoup.parse(html);

        //  as text nodes:
        List<TextNode> text = doc.selectXpath("//body//p//text()", TextNode.class);
        assertEquals(3, text.size());
        assertEquals("One", text.get(0).text());
        assertEquals("Two", text.get(1).text());
        assertEquals("Three and some more", text.get(2).text());

        //  as just nodes:
        List<Node> nodes = doc.selectXpath("//body//p//text()", Node.class);
        assertEquals(3, nodes.size());
        assertEquals("One", nodes.get(0).outerHtml());
        assertEquals("Two", nodes.get(1).outerHtml());
        assertEquals("Three and some more", nodes.get(2).outerHtml());
    }

    @Test void selectByAttribute() {
        Document doc = Jsoup.parse("<p><a href='/foo'>Foo</a><a href='/bar'>Bar</a><a>None</a>");
        List<String> hrefs = doc.selectXpath("//a[@href]").eachAttr("href");
        assertEquals(2, hrefs.size());
        assertEquals("/foo", hrefs.get(0));
        assertEquals("/bar", hrefs.get(1));
    }

    @Test void selectOutsideOfElementTree() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three");
        Elements ps = doc.selectXpath("//p");
        assertEquals(3, ps.size());

        Element p1 = ps.get(0);
        assertEquals("One", p1.text());

        Elements sibs = p1.selectXpath("following-sibling::p");
        assertEquals(2, sibs.size());
        assertEquals("Two", sibs.get(0).text());
        assertEquals("Three", sibs.get(1).text());
    }

    @Test void selectAncestorsOnContextElement() {
        // https://github.com/jhy/jsoup/issues/1652
        Document doc = Jsoup.parse("<div><p>Hello");
        Element p = doc.selectFirst("p");
        assertNotNull(p);
        Elements chain = p.selectXpath("ancestor-or-self::*");
        assertEquals(4, chain.size());
        assertEquals("html", chain.get(0).tagName());
        assertEquals("p", chain.get(3).tagName());
    }

    @Test
    public void canSupplyAlternateFactoryImpl() {
        // previously we had a test to load Saxon and do an XPath 2.0 query. But we know Saxon works and so that's
        // redundant - really just need to test that an alternate XPath factory can be used

        System.setProperty(XPathFactoryProperty, AlternateXpathFactory.class.getName());

        String xhtml = "<html xmlns='http://www.w3.org/1999/xhtml'><body id='One'><div>hello</div></body></html>";
        boolean threw = false;
        try {
            Document doc = Jsoup.parse(xhtml, Parser.xmlParser());
            Elements elements = doc.selectXpath("//*:body");

        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Sorry, no can do!"));
            threw = true;
        }
        assertTrue(threw);
        System.clearProperty(XPathFactoryProperty);
    }

    @Test
    public void notNamespaceAware() {
        String xhtml = "<html xmlns='http://www.w3.org/1999/xhtml'><body id='One'><div>hello</div></body></html>";
        Document doc = Jsoup.parse(xhtml, Parser.xmlParser());
        Elements elements = doc.selectXpath("//body");
        assertEquals(1, elements.size());
        assertEquals("One", elements.first().id());
    }

    @Test
    public void supportsPrefixes() {
        // example from https://www.w3.org/TR/xml-names/
        String xml = "<?xml version=\"1.0\"?>\n" +
            "<bk:book xmlns:bk='urn:loc.gov:books'\n" +
            "         xmlns:isbn='urn:ISBN:0-395-36341-6'>\n" +
            "    <bk:title>Cheaper by the Dozen</bk:title>\n" +
            "    <isbn:number>1568491379</isbn:number>\n" +
            "</bk:book>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());

        //Elements elements = doc.selectXpath("//bk:book/bk:title");
        Elements elements = doc.selectXpath("//book/title");
        assertEquals(1, elements.size());
        assertEquals("Cheaper by the Dozen", elements.first().text());

        // with prefix
        Elements byPrefix = doc.selectXpath("//*[name()='bk:book']/*[name()='bk:title']");
        assertEquals(1, byPrefix.size());
        assertEquals("Cheaper by the Dozen", byPrefix.first().text());

        Elements byLocalName = doc.selectXpath("//*[local-name()='book']/*[local-name()='title']");
        assertEquals(1, byLocalName.size());
        assertEquals("Cheaper by the Dozen", byLocalName.first().text());

        Elements isbn = doc.selectXpath("//book/number");
        assertEquals(1, isbn.size());
        assertEquals("1568491379", isbn.first().text());
    }

    // minimal, no-op implementation class to verify users can load a factory to support XPath 2.0 etc
    public static class AlternateXpathFactory extends XPathFactory {
        public AlternateXpathFactory() {
            super();
        }

        @Override
        public boolean isObjectModelSupported(String objectModel) {
            return true;
        }

        @Override
        public void setFeature(String name, boolean value) throws XPathFactoryConfigurationException {

        }

        @Override
        public boolean getFeature(String name) throws XPathFactoryConfigurationException {
            return true;
        }

        @Override
        public void setXPathVariableResolver(XPathVariableResolver resolver) {

        }

        @Override
        public void setXPathFunctionResolver(XPathFunctionResolver resolver) {

        }

        @Override
        public XPath newXPath() {
            throw new IllegalArgumentException("Sorry, no can do!");
        }
    }
}
