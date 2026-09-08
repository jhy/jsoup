package org.jsoup.integration.routes;

import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 Serves a small gzip representation that expands beyond {@link Integer#MAX_VALUE}.
 */
public final class LargeGzipRoute {
    public static final long DecodedBodySize = (long) Integer.MAX_VALUE + 1;
    private static final int DecodedMemberSize = 1024 * 1024;
    private static final int GzipMemberCount = (int) (DecodedBodySize / DecodedMemberSize);
    private static final byte[] EncodedBody = encodeBody();

    private LargeGzipRoute() {
    }

    /**
     Serves a virtual large response without storing or transferring its decoded representation.
     */
    public static void handle(TestRequest request, TestResponse response) {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Encoding", "gzip");
        response.write(EncodedBody);
    }

    private static byte[] encodeBody() {
        try {
            byte[] member = encodeZeroFilledMember();
            ByteArrayOutputStream encoded = new ByteArrayOutputStream(member.length * GzipMemberCount);
            for (int i = 0; i < GzipMemberCount; i++)
                encoded.write(member, 0, member.length);
            return encoded.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] encodeZeroFilledMember() throws IOException {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(encoded)) {
            gzip.write(new byte[DecodedMemberSize]);
        }
        return encoded.toByteArray();
    }
}
