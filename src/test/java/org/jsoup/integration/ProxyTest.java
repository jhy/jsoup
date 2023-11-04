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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.jsoup.integration.ConnectTest.ihVal;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 Tests Jsoup.connect proxy support */
public class ProxyTest {
    private static String echoUrl;
    private static TestServer.ProxySettings proxy;

    @BeforeAll
    public static void setUp() {
        echoUrl = EchoServlet.Url;
        proxy = ProxyServlet.ProxySettings;
    }

    @ParameterizedTest @MethodSource("helloUrls")
    void fetchViaProxy(String url) throws IOException {
        Connection con = Jsoup.connect(url)
            .proxy(proxy.hostname, proxy.port);

        Connection.Response res = con.execute();
        if (url.startsWith("http:/")) assertVia(res); // HTTPS CONNECT won't have Via

        Document doc = res.parse();
        Element p = doc.expectFirst("p");
        assertEquals("Hello, World!", p.text());
    }

    private static Stream<String> helloUrls() {
        return Stream.of(HelloServlet.Url, HelloServlet.TlsUrl);
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

        Connection.Response smedRes = session.newRequest().url(FileServlet.tlsUrlTo("/htmltests/medium.html")).execute();
        Connection.Response slargeRes = session.newRequest().url(FileServlet.tlsUrlTo("/htmltests/large.html")).execute();

        assertEquals("Medium HTML", smedRes.parse().title());
        assertEquals("Large HTML", slargeRes.parse().title());
    }
}
