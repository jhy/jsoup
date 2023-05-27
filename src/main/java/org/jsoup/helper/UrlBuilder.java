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
 * A utility class to normalize input URLs. jsoup internal; API subject to change.
 * <p>Normalization includes puny-coding the host, and encoding non-ascii path components. Any non-ascii characters in
 * the query string (or the fragment/anchor) are escaped, but any existing escapes in those components are preserved.</p>
 */
final class UrlBuilder {

    URL u;

    @Nullable
    StringBuilder q;

    UrlBuilder(URL inputUrl) {
        this.u = inputUrl;
        if (u.getQuery() != null)
            q = StringUtil.borrowBuilder().append(u.getQuery());
    }

    URL build() {
        try {
            // use the URI class to encode non-ascii in path
            URI uri = new URI(u.getProtocol(), u.getUserInfo(), // puny-code
            IDN.toASCII(decodePart(u.getHost())), u.getPort(), decodePart(u.getPath()), // query and fragment appended later so as not to encode
            null, // query and fragment appended later so as not to encode
            null);
            String normUrl = uri.toASCIIString();
            if (q != null || u.getRef() != null) {
                StringBuilder sb = StringUtil.borrowBuilder().append(normUrl);
                if (q != null) {
                    sb.append('?');
                    appendToAscii(StringUtil.releaseBuilder(q), true, sb);
                }
                if (u.getRef() != null) {
                    sb.append('#');
                    appendToAscii(u.getRef(), false, sb);
                }
                normUrl = StringUtil.releaseBuilder(sb);
            }
            u = new URL(normUrl);
            return u;
        } catch (MalformedURLException | URISyntaxException | UnsupportedEncodingException e) {
            // we assert here so that any incomplete normalization issues can be caught in devel. but in practise,
            // the remote end will be able to handle it, so in prod we just pass the original URL.
            // The UnsupportedEncodingException would never happen as always UTF8
            assert Validate.assertFail(e.toString());
            return u;
        }
    }

    void appendKeyVal(Connection.KeyVal kv) throws UnsupportedEncodingException {
        if (q == null)
            q = StringUtil.borrowBuilder();
        else
            q.append('&');
        q.append(URLEncoder.encode(kv.key(), UTF_8.name())).append('=').append(URLEncoder.encode(kv.value(), UTF_8.name()));
    }

    private static String decodePart(String encoded) {
        try {
            return URLDecoder.decode(encoded, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // wtf!
            throw new RuntimeException(e);
        }
    }

    private static void appendToAscii(String s, boolean spaceAsPlus, StringBuilder sb) throws UnsupportedEncodingException {
        // minimal normalization of Unicode -> Ascii, and space normal. Existing escapes are left as-is.
        for (int i = 0; i < s.length(); i++) {
            int c = s.codePointAt(i);
            if (c == ' ') {
                sb.append(spaceAsPlus ? '+' : "%20");
            } else if (c > 127) {
                // out of ascii range
                sb.append(URLEncoder.encode(new String(Character.toChars(c)), UTF_8.name()));
                // ^^ is a bit heavy-handed - if perf critical, we could optimize
            } else {
                sb.append((char) c);
            }
        }
    }
}
