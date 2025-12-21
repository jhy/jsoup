package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.CookieServlet;
import org.jsoup.integration.servlets.EchoServlet;
import org.jsoup.integration.servlets.FileServlet;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.parser.TagSet;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
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
        assertEquals(0, doc1.select("table tr").size()); // none sent to servlet

        // set the cookies
        Connection con2 = session.newRequest().data(CookieServlet.SetCookiesParam, "1");
        Document doc2 = con2.get();
        assertEquals(0, doc2.select("table tr").size());  // none sent to servlet - we just got them!
        Map<String, String> cookies = con2.response().cookies(); // simple cookie response, all named "One", so should be last sent
        assertEquals(2, cookies.size());
        assertEquals("EchoServlet", cookies.get("One"));

        // test that all response cookies are present, even if would not be sent for this request path (i.e. cookies() res can be set on a req, without using sessions)
        assertEquals("Override", cookies.get("Two"));

        // todo - interrogate cookie-store

        // check that they are sent and filtered to the right path
        Connection con3 = session.newRequest();
        Document doc3 = con3.get();
        assertCookieServlet(doc3);

        Document echo = session.newRequest().url(EchoServlet.Url).get();
        assertEchoServlet(echo);
        assertEquals(userAgent, keyText("User-Agent", echo)); // check that customer user agent sent on session arrived

        // check that cookies aren't set out of the session
        Document doc4 = Jsoup.newSession().url(CookieServlet.Url).get();
        assertEquals(0, doc4.select("table tr").size()); // none sent to servlet

        // check can add local ones also
        Document doc5 = session.newRequest().cookie("Bar", "Qux").get();
        Elements doc5Bar = keyEls("Bar", doc5);
        assertEquals("Qux", doc5Bar.first().text());
    }

    // validate that only cookies set by cookie servlet get to the cookie servlet path
    private void assertCookieServlet(Document doc) {
        assertEquals(2, doc.select("table tr").size());  // two of three sent to servlet (/ and /CookieServlet)
        Elements doc3Els = keyEls("One", doc);
        assertEquals(2, doc3Els.size());
        assertEquals("CookieServlet", doc3Els.get(0).text()); // ordered by most specific path
        assertEquals("Root", doc3Els.get(1).text()); // ordered by most specific path
    }

    // validate that only for echo servlet
    private void assertEchoServlet(Document doc) {
        Elements echoEls = keyEls("Cookie: One", doc);  // two of three sent to servlet (/ and /EchoServlet)
        assertEquals(2, echoEls.size());
        assertEquals("EchoServlet", echoEls.get(0).text()); // ordered by most specific path - /Echo
        assertEquals("Root", echoEls.get(1).text()); // ordered by most specific path - /
    }

    @Test
    public void testPathScopedCookiesOnRedirect() throws IOException {
        Connection session = Jsoup.newSession();

        Document doc1 = session.newRequest()
            .url(CookieServlet.Url)
            .data(CookieServlet.LocationParam, EchoServlet.Url)
            .data(CookieServlet.SetCookiesParam, "1")
            .get();

        // we should be redirected to the echo servlet with cookies
        assertEquals(EchoServlet.Url, doc1.location());
        assertEchoServlet(doc1); // checks we only have /echo cookies

        Document doc2 = session.newRequest()
            .url(EchoServlet.Url)
            .get();
        assertEchoServlet(doc2); // test retained in session

        Document doc3 = session.newRequest()
            .url(CookieServlet.Url)
            .get();
        assertCookieServlet(doc3); // and so were the /cookie cookies
    }

    @Test
    public void testCanChangeParsers() throws IOException {
        Connection session = Jsoup.newSession().parser(Parser.xmlParser());

        String xmlUrl = FileServlet.urlTo("/htmltests/xml-test.xml");
        String xmlVal = "<doc><val>One<val>Two</val>Three</val></doc>\n";

        Document doc1 = session.newRequest().url(xmlUrl).get();
        assertEquals(xmlVal, doc1.html()); // not HTML normed, used XML parser

        Document doc2 = session.newRequest().parser(Parser.htmlParser()).url(xmlUrl).get();
        assertTrue(doc2.html().startsWith("<html>"));

        Document doc3 = session.newRequest().url(xmlUrl).get();
        assertEquals(xmlVal, doc3.html()); // did not blow away xml default
    }

    @Test
    public void sessionTagSetDoesNotMutateRoot() {
        Connection session = Jsoup.newSession();
        TagSet rootTags = session.request().parser().tagSet();

        int rootNamespacesBefore = tagSetNamespaceCount(rootTags);

        Connection request = session.newRequest();
        Parser parser = request.request().parser();
        parser.parseInput("<custom>One <b>Two</b></custom>", "http://example.com/");

        int rootNamespacesAfter = tagSetNamespaceCount(rootTags);
        assertEquals(rootNamespacesBefore, rootNamespacesAfter);
    }

    @Test
    public void sessionTagSetCustomizerDoesNotMutateRoot() {
        Connection session = Jsoup.newSession();
        TagSet rootTags = session.request().parser().tagSet();
        rootTags.onNewTag(tag -> {
            if (!tag.isKnownTag())
                tag.set(Tag.RcData);
        });

        int rootNamespacesBefore = tagSetNamespaceCount(rootTags);

        Connection request = session.newRequest();
        Parser parser = request.request().parser();
        Document doc = parser.parseInput("<custom>One <b>Two</b></custom>", "https://example.com/");
        assertEquals(0, doc.select("custom b").size());

        int rootNamespacesAfter = tagSetNamespaceCount(rootTags);
        assertEquals(rootNamespacesBefore, rootNamespacesAfter);
    }

    private static int tagSetNamespaceCount(TagSet tagSet) {
        try {
            Field tagsField = TagSet.class.getDeclaredField("tags");
            tagsField.setAccessible(true);
            Map<?, ?> tags = (Map<?, ?>) tagsField.get(tagSet);
            return tags.size();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
