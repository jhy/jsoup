package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.internal.StringUtil;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import static org.jsoup.helper.DataUtil.UTF_8;

/**
 A utility class to normalize input URLs. jsoup internal; API subject to change.
 <p>Normalization includes puny-coding the host, and encoding non-ascii path components. The query-string
 is left mostly as-is, to avoid inadvertently/incorrectly decoding a desired '+' literal ('%2B') as a ' '.</p>
 */
final class UrlBuilder {
    URL u;
    @Nullable StringBuilder q;

    UrlBuilder(URL inputUrl) {
        this.u = inputUrl;
        if (u.getQuery() != null)
            q = StringUtil.borrowBuilder().append(u.getQuery());
    }

    URL build() {
        try {
            // use the URI class to encode non-ascii in path
            URI uri = new URI(
                u.getProtocol(),
                u.getUserInfo(),
                IDN.toASCII(decodePart(u.getHost())), // puny-code
                u.getPort(),
                decodePart(u.getPath()),
                null, null // query and fragment appended later so as not to encode
            );

            String normUrl = uri.toASCIIString();
            if (q != null || u.getRef() != null) {
                StringBuilder sb = StringUtil.borrowBuilder().append(normUrl);
                if (q != null) {
                    sb.append('?');
                    sb.append(normalizeQuery(StringUtil.releaseBuilder(q)));
                }
                if (u.getRef() != null) {
                    sb.append('#');
                    sb.append(normalizeRef(u.getRef()));
                }
                normUrl = StringUtil.releaseBuilder(sb);
            }
            u =  new URL(normUrl);
            return u;
        } catch (MalformedURLException | URISyntaxException e) {
            // we assert here so that any incomplete normalization issues can be caught in devel. but in practise,
            // the remote end will be able to handle it, so in prod we just pass the original URL
            assert Validate.assertFail(e.toString());
            return u;
        }
    }

    void appendKeyVal(Connection.KeyVal kv) throws UnsupportedEncodingException {
        if (q == null)
            q = StringUtil.borrowBuilder();
        else
            q.append('&');
        q
            .append(URLEncoder.encode(kv.key(), UTF_8.name()))
            .append('=')
            .append(URLEncoder.encode(kv.value(), UTF_8.name()));
    }

    private static String decodePart(String encoded) {
        try {
            return URLDecoder.decode(encoded, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // wtf!
        }
    }

    private static String normalizeQuery(String q) {
        // minimal space normal; other characters left as supplied - if generated from jsoup data, will be encoded
        return q.replace(' ', '+');
    }

    private static String normalizeRef(String r) {
        // minimal space normal; other characters left as supplied
        return r.replace(" ", "%20");
    }


}
