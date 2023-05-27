package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.CookieServlet;
import org.jsoup.integration.servlets.EchoServlet;
import org.jsoup.integration.servlets.FileServlet;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SessionTest {

    @BeforeAll
    public static void setUp() {
        TestServer.start();
    }

    private static Elements keyEls(String key, Document doc) {
        return doc.select("th:contains(" + key + ") + td");
    }

    private static String keyText(String key, Document doc) {
        return doc.selectFirst("th:contains(" + key + ") + td").text();
    }

    @Test
    public void testPathScopedCookies() throws IOException {
        final Connection session = Jsoup.newSession();
        final String userAgent = "Jsoup Testalot v0.1";
        session.userAgent(userAgent);
        session.url(CookieServlet.Url);
        // should have no cookies:
        Connection con1 = session.newRequest();
        Document doc1 = con1.get();
        // none sent to servlet
        assertEquals(0, doc1.select("table tr").size());
        // set the cookies
        Connection con2 = session.newRequest().data(CookieServlet.SetCookiesParam, "1");
        Document doc2 = con2.get();
        // none sent to servlet - we just got them!
        assertEquals(0, doc2.select("table tr").size());
        // simple cookie response, all named "One", so should be first sent
        Map<String, String> cookies = con2.response().cookies();
        assertEquals(1, cookies.size());
        assertEquals("Root", cookies.get("One"));
        // todo - interrogate cookie-store
        // check that they are sent and filtered to the right path
        Connection con3 = session.newRequest();
        Document doc3 = con3.get();
        assertCookieServlet(doc3);
        Document echo = session.newRequest().url(EchoServlet.Url).get();
        assertEchoServlet(echo);
        // check that customer user agent sent on session arrived
        assertEquals(userAgent, keyText("User-Agent", echo));
        // check that cookies aren't set out of the session
        Document doc4 = Jsoup.newSession().url(CookieServlet.Url).get();
        // none sent to servlet
        assertEquals(0, doc4.select("table tr").size());
        // check can add local ones also
        Document doc5 = session.newRequest().cookie("Bar", "Qux").get();
        Elements doc5Bar = keyEls("Bar", doc5);
        assertEquals("Qux", doc5Bar.first().text());
    }

    // validate that only cookies set by cookie servlet get to the cookie servlet path
    private void assertCookieServlet(Document doc) {
        // two of three sent to servlet (/ and /CookieServlet)
        assertEquals(2, doc.select("table tr").size());
        Elements doc3Els = keyEls("One", doc);
        assertEquals(2, doc3Els.size());
        // ordered by most specific path
        assertEquals("CookieServlet", doc3Els.get(0).text());
        // ordered by most specific path
        assertEquals("Root", doc3Els.get(1).text());
    }

    // validate that only for echo servlet
    private void assertEchoServlet(Document doc) {
        // two of three sent to servlet (/ and /EchoServlet)
        Elements echoEls = keyEls("Cookie: One", doc);
        assertEquals(2, echoEls.size());
        // ordered by most specific path - /Echo
        assertEquals("EchoServlet", echoEls.get(0).text());
        // ordered by most specific path - /
        assertEquals("Root", echoEls.get(1).text());
    }

    @Test
    public void testPathScopedCookiesOnRedirect() throws IOException {
        Connection session = Jsoup.newSession();
        Document doc1 = session.newRequest().url(CookieServlet.Url).data(CookieServlet.LocationParam, EchoServlet.Url).data(CookieServlet.SetCookiesParam, "1").get();
        // we should be redirected to the echo servlet with cookies
        assertEquals(EchoServlet.Url, doc1.location());
        // checks we only have /echo cookies
        assertEchoServlet(doc1);
        Document doc2 = session.newRequest().url(EchoServlet.Url).get();
        // test retained in session
        assertEchoServlet(doc2);
        Document doc3 = session.newRequest().url(CookieServlet.Url).get();
        // and so were the /cookie cookies
        assertCookieServlet(doc3);
    }

    @Test
    public void testCanChangeParsers() throws IOException {
        Connection session = Jsoup.newSession().parser(Parser.xmlParser());
        String xmlUrl = FileServlet.urlTo("/htmltests/xml-test.xml");
        String xmlVal = "<doc><val>One<val>Two</val>Three</val></doc>\n";
        Document doc1 = session.newRequest().url(xmlUrl).get();
        // not HTML normed, used XML parser
        assertEquals(xmlVal, doc1.html());
        Document doc2 = session.newRequest().parser(Parser.htmlParser()).url(xmlUrl).get();
        assertTrue(doc2.html().startsWith("<html>"));
        Document doc3 = session.newRequest().url(xmlUrl).get();
        // did not blow away xml default
        assertEquals(xmlVal, doc3.html());
    }
}
