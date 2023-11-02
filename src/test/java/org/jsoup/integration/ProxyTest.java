package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.EchoServlet;
import org.jsoup.integration.servlets.FileServlet;
import org.jsoup.integration.servlets.HelloServlet;
import org.jsoup.integration.servlets.ProxyServlet;
import org.jsoup.integration.servlets.RedirectServlet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.jsoup.integration.ConnectTest.ihVal;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 Tests Jsoup.connect proxy support
 */
public class ProxyTest {
    private static String echoUrl;
    private static TestServer.ProxySettings proxy;

    @BeforeAll
    public static void setUp() {
        echoUrl = EchoServlet.Url;
        proxy = ProxyServlet.ProxySettings;
    }

    @Test void fetchViaProxy() throws IOException {
        Connection con = Jsoup.connect(HelloServlet.Url)
            .proxy(proxy.hostname, proxy.port);

        Connection.Response res = con.execute();
        assertVia(res);

        Document doc = res.parse();
        Element p = doc.expectFirst("p");
        assertEquals("Hello, World!", p.text());
    }

    private static void assertVia(Connection.Response res) {
        assertEquals(res.header("Via"), ProxyServlet.Via);
    }

    @Test void redirectViaProxy() throws IOException {
        Connection.Response res = Jsoup
            .connect(RedirectServlet.Url)
            .data(RedirectServlet.LocationParam, echoUrl)
            .header("Random-Header-name", "hello")
            .proxy(proxy.hostname, proxy.port)
            .execute();

        assertVia(res);
        Document doc = res.parse();
        assertEquals(echoUrl, doc.location());
        assertEquals("hello", ihVal("Random-Header-name", doc));
        assertVia(res);
    }

    @Test void proxyForSession() throws IOException {
        Connection session = Jsoup.newSession().proxy(proxy.hostname, proxy.port);

        Connection.Response medRes = session.newRequest().url(FileServlet.urlTo("/htmltests/medium.html")).execute();
        Connection.Response largeRes = session.newRequest().url(FileServlet.urlTo("/htmltests/large.html")).execute();

        assertVia(medRes);
        assertVia(largeRes);
        assertEquals("Medium HTML", medRes.parse().title());
        assertEquals("Large HTML", largeRes.parse().title());
    }
}
