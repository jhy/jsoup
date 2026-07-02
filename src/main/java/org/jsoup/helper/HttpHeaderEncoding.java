package org.jsoup.helper;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.jsoup.helper.DataUtil.UTF_8;

/**
 Package-private helper that handles HTTP header encoding detection and correction for {@link HttpConnection}.
 Extracted from HttpConnection.Response to reduce its size (Large Class smell).
 */
class HttpHeaderEncoding {

    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    @Nullable
    static String fixHeaderEncoding(@Nullable String val) {
        if (val == null) return val;
        if (!StandardCharsets.ISO_8859_1.newEncoder().canEncode(val))
            return val;
        byte[] bytes = val.getBytes(ISO_8859_1);
        if (looksLikeUtf8(bytes))
            return new String(bytes, UTF_8);
        else
            return val;
    }

    private static boolean looksLikeUtf8(byte[] input) {
        int i = 0;
        // BOM:
        if (input.length >= 3
            && (input[0] & 0xFF) == 0xEF
            && (input[1] & 0xFF) == 0xBB
            && (input[2] & 0xFF) == 0xBF) {
            i = 3;
        }

        int end;
        boolean foundNonAscii = false;
        for (int j = input.length; i < j; ++i) {
            int o = input[i];
            if ((o & 0x80) == 0) {
                continue; // ASCII
            }
            foundNonAscii = true;

            // UTF-8 leading:
            if ((o & 0xE0) == 0xC0) {
                end = i + 1;
            } else if ((o & 0xF0) == 0xE0) {
                end = i + 2;
            } else if ((o & 0xF8) == 0xF0) {
                end = i + 3;
            } else {
                return false;
            }

            if (end >= input.length)
                return false;

            while (i < end) {
                i++;
                o = input[i];
                if ((o & 0xC0) != 0x80) {
                    return false;
                }
            }
        }
        return foundNonAscii;
    }
}
