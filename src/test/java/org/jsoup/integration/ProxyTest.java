package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.AuthFilter;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.jsoup.helper.AuthenticationHandlerTest.MaxAttempts;
import static org.jsoup.integration.ConnectTest.ihVal;
import static org.junit.jupiter.api.Assertions.*;

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

    private static Stream<String> echoUrls() {
        return Stream.of(EchoServlet.Url, EchoServlet.TlsUrl);
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

        Connection.Response medRes = session.newRequest(FileServlet.urlTo("/htmltests/medium.html")).execute();
        Connection.Response largeRes = session.newRequest(FileServlet.urlTo("/htmltests/large.html")).execute();

        assertVia(medRes);
        assertVia(largeRes);
        assertEquals("Medium HTML", medRes.parse().title());
        assertEquals("Large HTML", largeRes.parse().title());

        Connection.Response smedRes = session.newRequest(FileServlet.tlsUrlTo("/htmltests/medium.html")).execute();
        Connection.Response slargeRes = session.newRequest(FileServlet.tlsUrlTo("/htmltests/large.html")).execute();

        assertEquals("Medium HTML", smedRes.parse().title());
        assertEquals("Large HTML", slargeRes.parse().title());
    }

    @ParameterizedTest @MethodSource("echoUrls")
    void canAuthenticateToProxy(String url) throws IOException {
        int closed = TestServer.closeAuthedProxyConnections(); // reset any existing authed connections from previous tests, so we can test the auth flow

        // the proxy wants auth, but not the server. HTTP and HTTPS, so tests direct proxy and CONNECT
        Connection session = Jsoup.newSession()
            .proxy(proxy.hostname, proxy.authedPort).ignoreHttpErrors(true);
        String password = AuthFilter.newProxyPassword();

        // fail first
        try {
            Connection.Response execute = session.newRequest(url)
                .execute();
            int code = execute.statusCode(); // no auth sent
            assertEquals(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED, code);
        } catch (IOException e) {
            // in CONNECT (for the HTTPS url), URLConnection will throw the proxy connect as a Stringly typed IO exception - "Unable to tunnel through proxy. Proxy returns "HTTP/1.1 407 Proxy Authentication Required"". (Not a response code)
            assertTrue(e.getMessage().contains("407"));
        }

        try {
            AtomicInteger count = new AtomicInteger(0);
            Connection.Response res = session.newRequest(url)
                .auth(ctx -> {
                    count.incrementAndGet();
                    return ctx.credentials(AuthFilter.ProxyUser, password + "wrong"); // incorrect
                })
                .execute();
            assertEquals(MaxAttempts, count.get());
            assertEquals(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED, res.statusCode());
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("407"));
        }

        AtomicInteger successCount = new AtomicInteger(0);
        Connection.Response successRes = session.newRequest(url)
            .auth(ctx -> {
                successCount.incrementAndGet();
                return ctx.credentials(AuthFilter.ProxyUser, password); // correct
            })
            .execute();
        assertEquals(1, successCount.get());
        assertEquals(HttpServletResponse.SC_OK, successRes.statusCode());
    }

    @ParameterizedTest @MethodSource("echoUrls")
    void canAuthToProxyAndServer(String url) throws IOException {
        String serverPassword = AuthFilter.newServerPassword();
        String proxyPassword = AuthFilter.newProxyPassword();
        AtomicInteger count = new AtomicInteger(0);

        Connection session = Jsoup.newSession() // both proxy and server will want auth
            .proxy(proxy.hostname, proxy.authedPort)
            .header(AuthFilter.WantsServerAuthentication, "1")
            .auth(auth -> {
                count.incrementAndGet();

                if (auth.isServer()) {
                    assertEquals(url, auth.url().toString());
                    assertEquals(AuthFilter.ServerRealm, auth.realm());
                    return auth.credentials(AuthFilter.ServerUser, serverPassword);
                } else {
                    assertTrue(auth.isProxy());
                    return auth.credentials(AuthFilter.ProxyUser, proxyPassword);
                }
            });


        Connection.Response res = session.newRequest(url).execute();
        assertEquals(200, res.statusCode());
        assertEquals(2, count.get()); // hit server and proxy auth stages
        assertEquals("Webserver Environment Variables", res.parse().title());
    }
}
