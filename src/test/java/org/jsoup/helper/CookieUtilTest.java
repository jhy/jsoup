package org.jsoup.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CookieUtilTest {

    @Test void parseCookie() {
        HttpConnection.Response res = new HttpConnection.Response();

        CookieUtil.parseCookie("foo=bar qux; Domain=.example.com; Path=/; Secure", res);
        CookieUtil.parseCookie("bar=foo qux", res);
        CookieUtil.parseCookie("=bar; Domain=.example.com; Path=/; Secure", res);
        CookieUtil.parseCookie("; Domain=.example.com; Path=/", res);
        CookieUtil.parseCookie("", res);
        CookieUtil.parseCookie(null, res);

        assertEquals(3, res.cookies().size());
        assertEquals("bar qux", res.cookies.get("foo"));
        assertEquals("foo qux", res.cookies.get("bar"));
        assertEquals(".example.com", res.cookies.get("; Domain")); // no actual cookie name or val
    }
}
