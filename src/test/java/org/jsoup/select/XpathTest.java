package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

        Elements els = div.selectXpath("/div/p");
        assertEquals(1, els.size());
        assertEquals("One", els.get(0).text());
        assertEquals("p", els.get(0).tagName());

        assertEquals(0, div.selectXpath("//body").size());
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
    public void supportsNamespaces() {
        String xhtml = "<html xmlns='http://www.w3.org/1999/xhtml'><body id='One'><div>hello</div></body></html>";;
        Document doc = Jsoup.parse(xhtml, Parser.xmlParser());
        Elements elements = doc.selectXpath("//*[local-name()='body']");
        assertEquals(1, elements.size());
        assertEquals("One", elements.first().id());
    }

    @Test
    public void canDitchNamespaces() {
        String xhtml = "<html xmlns='http://www.w3.org/1999/xhtml'><body id='One'><div>hello</div></body></html>";;
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
