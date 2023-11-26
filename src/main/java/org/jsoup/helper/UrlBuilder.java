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
    URL inputUrl;
    @Nullable StringBuilder queryBuilder;

    UrlBuilder(URL inputUrl) {
        this.inputUrl = inputUrl;
        if (this.inputUrl.getQuery() != null)
            queryBuilder = StringUtil.borrowBuilder().append(this.inputUrl.getQuery());
    }

    URL build() {
        try {
            // use the URI class to encode non-ascii in path
            URI uri = new URI(
                inputUrl.getProtocol(),
                inputUrl.getUserInfo(),
                IDN.toASCII(decodePart(inputUrl.getHost())), // puny-code
                inputUrl.getPort(),
                null, null, null // path, query and fragment appended later so as not to encode
            );

            StringBuilder normUrl = StringUtil.borrowBuilder().append(uri.toASCIIString());
            appendToAscii(inputUrl.getPath(), false, normUrl);
            if (queryBuilder != null) {
                normUrl.append('?');
                appendToAscii(StringUtil.releaseBuilder(queryBuilder), true, normUrl);
            }
            if (inputUrl.getRef() != null) {
                normUrl.append('#');
                appendToAscii(inputUrl.getRef(), false, normUrl);
            }
            inputUrl = new URL(StringUtil.releaseBuilder(normUrl));
            return inputUrl;
        } catch (MalformedURLException | URISyntaxException | UnsupportedEncodingException e) {
            // we assert here so that any incomplete normalization issues can be caught in devel. but in practise,
            // the remote end will be able to handle it, so in prod we just pass the original URL.
            // The UnsupportedEncodingException would never happen as always UTF8
            assert Validate.assertFail(e.toString());
            return inputUrl;
        }
    }

    void appendKeyVal(Connection.KeyVal kv) throws UnsupportedEncodingException {
        if (queryBuilder == null)
            queryBuilder = StringUtil.borrowBuilder();
        else
            queryBuilder.append('&');
        queryBuilder
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

    private static void appendToAscii(String string, boolean spaceAsPlus, StringBuilder stringBuilder) throws UnsupportedEncodingException {
        // minimal normalization of Unicode -> Ascii, and space normal. Existing escapes are left as-is.
        for (int i = 0; i < string.length(); i++) {
            int codePoint = string.codePointAt(i);
            if (codePoint == ' ') {
                stringBuilder.append(spaceAsPlus ? '+' : "%20");
            } else if (codePoint > 127) { // out of ascii range
                stringBuilder.append(URLEncoder.encode(new String(Character.toChars(codePoint)), UTF_8.name()));
                // ^^ is a bit heavy-handed - if perf critical, we could optimize
                if (Character.charCount(codePoint) == 2) i++; // advance past supplemental
            } else {
                stringBuilder.append((char) codePoint);
            }
        }
    }


}
