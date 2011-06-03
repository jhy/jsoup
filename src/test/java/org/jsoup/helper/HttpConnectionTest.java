package org.jsoup.helper;

import static org.junit.Assert.*;
import org.junit.Test;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;

public class HttpConnectionTest {
    /* most actual network http connection tests are in integration */

    @Test(expected=IllegalArgumentException.class) public void throwsExceptionOnParseWithoutExecute() throws IOException {
        Connection con = HttpConnection.connect("http://example.com");
        con.response().parse();
    }

    @Test(expected=IllegalArgumentException.class) public void throwsExceptionOnBodyWithoutExecute() throws IOException {
        Connection con = HttpConnection.connect("http://example.com");
        con.response().body();
    }

    @Test(expected=IllegalArgumentException.class) public void throwsExceptionOnBodyAsBytesWithoutExecute() throws IOException {
        Connection con = HttpConnection.connect("http://example.com");
        con.response().bodyAsBytes();
    }

    @Test public void caseInsensitiveHeaders() {
        Connection.Response res = new HttpConnection.Response();
        Map<String, String> headers = res.headers();
        headers.put("Accept-Encoding", "gzip");
        headers.put("content-type", "text/html");
        headers.put("refErrer", "http://example.com");

        assertTrue(res.hasHeader("Accept-Encoding"));
        assertTrue(res.hasHeader("accept-encoding"));
        assertTrue(res.hasHeader("accept-Encoding"));

        assertEquals("gzip", res.header("accept-Encoding"));
        assertEquals("text/html", res.header("Content-Type"));
        assertEquals("http://example.com", res.header("Referrer"));

        res.removeHeader("Content-Type");
        assertFalse(res.hasHeader("content-type"));

        res.header("accept-encoding", "deflate");
        assertEquals("deflate", res.header("Accept-Encoding"));
        assertEquals("deflate", res.header("accept-Encoding"));
    }

    @Test public void ignoresEmptySetCookies() {
        // prep http response header map
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Set-Cookie", Collections.<String>emptyList());
        HttpConnection.Response res = new HttpConnection.Response();
        res.processResponseHeaders(headers);
        assertEquals(0, res.cookies().size());
    }

    @Test public void ignoresEmptyCookieNameAndVals() {
        // prep http response header map
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> cookieStrings = new ArrayList<String>();
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

    @Test(expected=IllegalArgumentException.class) public void throwsOnMalformedUrl() {
        Connection con = HttpConnection.connect("bzzt");
    }

    @Test public void userAgent() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.userAgent("Mozilla");
        assertEquals("Mozilla", con.request().header("User-Agent"));
    }

    @Test public void timeout() {
        Connection con = HttpConnection.connect("http://example.com/");
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

    @Test(expected=IllegalArgumentException.class) public void throwsOnOdddData() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.data("Name", "val", "what");
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
}