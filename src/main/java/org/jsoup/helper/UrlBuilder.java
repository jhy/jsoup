package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.internal.StringUtil;
import org.jspecify.annotations.Nullable;

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
 <p>Normalization includes puny-coding the host, and encoding non-ascii path components. Any non-ascii characters in
 the query string (or the fragment/anchor) are escaped, but any existing escapes in those components are preserved.</p>
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
                null, null, null // path, query and fragment appended later so as not to encode
            );

            StringBuilder normUrl = StringUtil.borrowBuilder().append(uri.toASCIIString());
            appendToAscii(u.getPath(), false, normUrl);
            if (q != null) {
                normUrl.append('?');
                appendToAscii(StringUtil.releaseBuilder(q), true, normUrl);
            }
            if (u.getRef() != null) {
                normUrl.append('#');
                appendToAscii(u.getRef(), false, normUrl);
            }
            u = new URL(StringUtil.releaseBuilder(normUrl));
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

    private static final String unsafeCharacters = "<>\"{}|\\^[]`";

    private static void appendToAscii(String s, boolean spaceAsPlus, StringBuilder sb) throws UnsupportedEncodingException {
        for (int i = 0; i < s.length(); i++) {
            int c = s.codePointAt(i);
            if (c == ' ') {
                sb.append(spaceAsPlus ? '+' : "%20");
            } else if (c == '%') { // if already a valid escape, pass; otherwise, escape
                if (i < s.length() - 2 && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                    sb.append('%').append(s.charAt(i + 1)).append(s.charAt(i + 2));
                    i += 2; // skip the next two characters
                } else {
                    sb.append("%25");
                }
            } else if (c > 127 || unsafeCharacters.indexOf(c) != -1) { // past ascii, or otherwise unsafe
                sb.append(URLEncoder.encode(new String(Character.toChars(c)), UTF_8.name()));
                if (Character.charCount(c) == 2) i++; // advance past supplemental
            } else {
                sb.append((char) c);
            }
        }
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }


}
