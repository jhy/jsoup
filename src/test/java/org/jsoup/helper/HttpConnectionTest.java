package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.MultiLocaleExtension.MultiLocaleTest;
import org.jsoup.integration.ParseTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HttpConnectionTest {
    /* most actual network http connection tests are in integration */

    @Test public void canCreateEmptyConnection() {
        HttpConnection con = new HttpConnection();
        assertEquals(Connection.Method.GET, con.request().method());
        assertThrows(IllegalArgumentException.class, () -> {
            URL url = con.request().url();
        });
    }

    @Test public void throwsExceptionOnResponseWithoutExecute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com");
            con.response();
        });
    }

    @Test public void throwsExceptionOnParseWithoutExecute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com");
            con.response().parse();
        });
    }

    @Test public void throwsExceptionOnBodyWithoutExecute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com");
            con.response().body();
        });
    }

    @Test public void throwsExceptionOnBodyAsBytesWithoutExecute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com");
            con.response().bodyAsBytes();
        });
    }

    @MultiLocaleTest
    public void caseInsensitiveHeaders(Locale locale) {
        Locale.setDefault(locale);

        Connection.Response res = new HttpConnection.Response();
        res.header("Accept-Encoding", "gzip");
        res.header("content-type", "text/html");
        res.header("refErrer", "http://example.com");

        assertTrue(res.hasHeader("Accept-Encoding"));
        assertTrue(res.hasHeader("accept-encoding"));
        assertTrue(res.hasHeader("accept-Encoding"));
        assertTrue(res.hasHeader("ACCEPT-ENCODING"));

        assertEquals("gzip", res.header("accept-Encoding"));
        assertEquals("gzip", res.header("ACCEPT-ENCODING"));
        assertEquals("text/html", res.header("Content-Type"));
        assertEquals("http://example.com", res.header("Referrer"));

        res.removeHeader("Content-Type");
        assertFalse(res.hasHeader("content-type"));

        res.removeHeader("ACCEPT-ENCODING");
        assertFalse(res.hasHeader("Accept-Encoding"));

        res.header("ACCEPT-ENCODING", "deflate");
        assertEquals("deflate", res.header("Accept-Encoding"));
        assertEquals("deflate", res.header("accept-Encoding"));
    }

    @Test public void headers() {
        Connection con = HttpConnection.connect("http://example.com");
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "text/html");
        headers.put("Connection", "keep-alive");
        headers.put("Host", "http://example.com");
        con.headers(headers);
        assertEquals("text/html", con.request().header("content-type"));
        assertEquals("keep-alive", con.request().header("Connection"));
        assertEquals("http://example.com", con.request().header("Host"));
    }

    @Test public void sameHeadersCombineWithComma() {
        Map<String, List<String>> headers = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("no-cache");
        values.add("no-store");
        headers.put("Cache-Control", values);
        HttpConnection.Response res = new HttpConnection.Response();
        res.processResponseHeaders(headers);
        assertEquals("no-cache, no-store", res.header("Cache-Control"));
    }

    @Test public void multipleHeaders() {
        Connection.Request req = new HttpConnection.Request();
        req.addHeader("Accept", "Something");
        req.addHeader("Accept", "Everything");
        req.addHeader("Foo", "Bar");

        assertTrue(req.hasHeader("Accept"));
        assertTrue(req.hasHeader("ACCEpt"));
        assertEquals("Something, Everything", req.header("accept"));
        assertTrue(req.hasHeader("fOO"));
        assertEquals("Bar", req.header("foo"));

        List<String> accept = req.headers("accept");
        assertEquals(2, accept.size());
        assertEquals("Something", accept.get(0));
        assertEquals("Everything", accept.get(1));

        Map<String, List<String>> headers = req.multiHeaders();
        assertEquals(accept, headers.get("Accept"));
        assertEquals("Bar", headers.get("Foo").get(0));

        assertTrue(req.hasHeader("Accept"));
        assertTrue(req.hasHeaderWithValue("accept", "Something"));
        assertTrue(req.hasHeaderWithValue("accept", "Everything"));
        assertFalse(req.hasHeaderWithValue("accept", "Something for nothing"));

        req.removeHeader("accept");
        headers = req.multiHeaders();
        assertEquals("Bar", headers.get("Foo").get(0));
        assertFalse(req.hasHeader("Accept"));
        assertNull(headers.get("Accept"));
    }

    @Test public void ignoresEmptySetCookies() {
        // prep http response header map
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Set-Cookie", Collections.emptyList());
        HttpConnection.Response res = new HttpConnection.Response();
        res.processResponseHeaders(headers);
        assertEquals(0, res.cookies().size());
    }

    @Test public void ignoresEmptyCookieNameAndVals() {
        // prep http response header map
        Map<String, List<String>> headers = new HashMap<>();
        List<String> cookieStrings = new ArrayList<>();
        cookieStrings.add(null);
        cookieStrings.add("");
        cookieStrings.add("one");
        cookieStrings.add("two=");
        cookieStrings.add("three=;");
        cookieStrings.add("four=data; Domain=.example.com; Path=/");

        headers.put("Set-Cookie", cookieStrings);
        HttpConnection.Response res = new HttpConnection.Response();
        res.processResponseHeaders(headers);
        assertEquals(4, res.cookies().size());
        assertEquals("", res.cookie("one"));
        assertEquals("", res.cookie("two"));
        assertEquals("", res.cookie("three"));
        assertEquals("data", res.cookie("four"));
    }

    @Test public void connectWithUrl() throws MalformedURLException {
        Connection con = HttpConnection.connect(new URL("http://example.com"));
        assertEquals("http://example.com", con.request().url().toExternalForm());
    }

    @Test public void throwsOnMalformedUrl() {
        assertThrows(IllegalArgumentException.class, () -> HttpConnection.connect("bzzt"));
    }

    @Test public void userAgent() {
        Connection con = HttpConnection.connect("http://example.com/");
        assertEquals(HttpConnection.DEFAULT_UA, con.request().header("User-Agent"));
        con.userAgent("Mozilla");
        assertEquals("Mozilla", con.request().header("User-Agent"));
    }

    @Test public void timeout() {
        Connection con = HttpConnection.connect("http://example.com/");
        assertEquals(30 * 1000, con.request().timeout());
        con.timeout(1000);
        assertEquals(1000, con.request().timeout());
    }

    @Test public void referrer() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.referrer("http://foo.com");
        assertEquals("http://foo.com", con.request().header("Referer"));
    }

    @Test public void method() {
        Connection con = HttpConnection.connect("http://example.com/");
        assertEquals(Connection.Method.GET, con.request().method());
        con.method(Connection.Method.POST);
        assertEquals(Connection.Method.POST, con.request().method());
    }

    @Test public void throwsOnOddData() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com/");
            con.data("Name", "val", "what");
        });
    }

    @Test public void data() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.data("Name", "Val", "Foo", "bar");
        Collection<Connection.KeyVal> values = con.request().data();
        Object[] data =  values.toArray();
        Connection.KeyVal one = (Connection.KeyVal) data[0];
        Connection.KeyVal two = (Connection.KeyVal) data[1];
        assertEquals("Name", one.key());
        assertEquals("Val", one.value());
        assertEquals("Foo", two.key());
        assertEquals("bar", two.value());
    }

    @Test public void cookie() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.cookie("Name", "Val");
        assertEquals("Val", con.request().cookie("Name"));
    }

    @Test public void inputStream() {
        Connection.KeyVal kv = HttpConnection.KeyVal.create("file", "thumb.jpg", ParseTest.inputStreamFrom("Check"));
        assertEquals("file", kv.key());
        assertEquals("thumb.jpg", kv.value());
        assertTrue(kv.hasInputStream());

        kv = HttpConnection.KeyVal.create("one", "two");
        assertEquals("one", kv.key());
        assertEquals("two", kv.value());
        assertFalse(kv.hasInputStream());
    }

    @Test public void requestBody() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.requestBody("foo");
        assertEquals("foo", con.request().requestBody());
    }

    @Test public void encodeUrl() throws MalformedURLException {
        URL url1 = new URL("https://test.com/foo bar/[One]?q=white space#frag");
        URL url2 = HttpConnection.encodeUrl(url1);
        assertEquals("https://test.com/foo%20bar/%5BOne%5D?q=white%20space#frag", url2.toExternalForm());
    }

    @Test public void noUrlThrowsValidationError() throws IOException {
        HttpConnection con = new HttpConnection();
        boolean threw = false;
        try {
            con.execute();
        } catch (IllegalArgumentException e) {
            threw = true;
            assertEquals("URL not set. Make sure to call #url(...) before executing the request.", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test public void handlesHeaderEncodingOnRequest() {
        Connection.Request req = new HttpConnection.Request();
        req.addHeader("xxx", "é");
    }

    @Test public void supportsInternationalDomainNames() throws MalformedURLException {
        String idn = "https://www.测试.测试/foo.html?bar";
        String puny = "https://www.xn--0zwm56d.xn--0zwm56d/foo.html?bar";

        Connection con = Jsoup.connect(idn);
        assertEquals(puny, con.request().url().toExternalForm());

        HttpConnection.Request req = new HttpConnection.Request();
        req.url(new URL(idn));
        assertEquals(puny, req.url().toExternalForm());
    }

    @Test public void validationErrorsOnExecute() throws IOException {
        Connection con = new HttpConnection();
        boolean urlThrew = false;
        try {
            con.execute();
        } catch (IllegalArgumentException e) {
            urlThrew = e.getMessage().contains("URL");
        }
        assertTrue(urlThrew);
    }

    @Test void testMalformedException() {
        boolean threw = false;
        try {
            Jsoup.connect("jsoup.org/test");
        } catch (IllegalArgumentException e) {
            threw = true;
            assertEquals("The supplied URL, 'jsoup.org/test', is malformed. Make sure it is an absolute URL, and starts with 'http://' or 'https://'. See https://jsoup.org/cookbook/extracting-data/working-with-urls", e.getMessage());
        }
        assertTrue(threw);
    }
}
