package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 Needs more tests! Just a POC so far.
 */
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

}
