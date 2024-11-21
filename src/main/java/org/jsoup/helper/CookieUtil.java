package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.internal.StringUtil;
import org.jsoup.parser.TokenQueue;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 Helper functions to support the Cookie Manager / Cookie Storage in HttpConnection.

 @since 1.14.1 */
class CookieUtil {
    // cookie manager get() wants request headers but doesn't use them, so we just pass a dummy object here
    private static final Map<String, List<String>> EmptyRequestHeaders = Collections.unmodifiableMap(new HashMap<>());
    private static final String Sep = "; ";
    private static final String CookieName = "Cookie";
    private static final String Cookie2Name = "Cookie2";

    /**
     Pre-request, get any applicable headers out of the Request cookies and the Cookie Store, and add them to the request
     headers. If the Cookie Store duplicates any Request cookies (same name and value), they will be discarded.
     */
    static void applyCookiesToRequest(HttpConnection.Request req, HttpURLConnection con) throws IOException {
        // Request key/val cookies. LinkedHashSet used to preserve order, as cookie store will return most specific path first
        Set<String> cookieSet = requestCookieSet(req);
        Set<String> cookies2 = null;

        // stored:
        Map<String, List<String>> storedCookies = req.cookieManager().get(asUri(req.url), EmptyRequestHeaders);
        for (Map.Entry<String, List<String>> entry : storedCookies.entrySet()) {
            // might be Cookie: name=value; name=value\nCookie2: name=value; name=value
            List<String> cookies = entry.getValue(); // these will be name=val
            if (cookies == null || cookies.size() == 0) // the cookie store often returns just an empty "Cookie" key, no val
                continue;

            String key = entry.getKey(); // Cookie or Cookie2
            Set<String> set;
            if (CookieName.equals(key))
                set = cookieSet;
            else if (Cookie2Name.equals(key)) {
                set = new HashSet<>();
                cookies2 = set;
            } else {
                continue; // unexpected header key
            }
            set.addAll(cookies);
        }

        if (cookieSet.size() > 0)
            con.addRequestProperty(CookieName, StringUtil.join(cookieSet, Sep));
        if (cookies2 != null && cookies2.size() > 0)
            con.addRequestProperty(Cookie2Name, StringUtil.join(cookies2, Sep));
    }

    private static LinkedHashSet<String> requestCookieSet(Connection.Request req) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        // req cookies are the wildcard key/val cookies (no domain, path, etc)
        for (Map.Entry<String, String> cookie : req.cookies().entrySet()) {
            set.add(cookie.getKey() + "=" + cookie.getValue());
        }
        return set;
    }

    static URI asUri(URL url) throws IOException {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {  // this would be a WTF because we construct the URL
            MalformedURLException ue = new MalformedURLException(e.getMessage());
            ue.initCause(e);
            throw ue;
        }
    }

    /** Store the Result cookies into the cookie manager, and place relevant cookies into the Response object. */
    static void storeCookies(HttpConnection.Request req, HttpConnection.Response res, URL url, Map<String, List<String>> resHeaders) throws IOException {
        CookieManager manager = req.cookieManager();
        URI uri = CookieUtil.asUri(url);
        manager.put(uri, resHeaders); // stores cookies for session

        // set up the simple cookies() map
        // the response may include cookies that are not relevant to this request, but users may require them if they are not using the cookie manager (setting request cookies only from the simple cookies() response):
        for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (name.equalsIgnoreCase("Set-Cookie")) {
                for (String value : values) {
                    if (value == null)
                        continue;
                    TokenQueue cd = new TokenQueue(value);
                    String cookieName = cd.chompTo("=").trim();
                    String cookieVal = cd.consumeTo(";").trim();
                    // ignores path, date, domain, validateTLSCertificates et al. full details will be available in cookiestore if required
                    // name not blank, value not null
                    res.cookie(cookieName, cookieVal); // if duplicate names, last set will win
                }
            }
        }
    }
}
