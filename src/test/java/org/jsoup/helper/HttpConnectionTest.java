package org.jsoup.helper;

import static org.junit.Assert.*;
import org.junit.Test;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.Map;

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
}
