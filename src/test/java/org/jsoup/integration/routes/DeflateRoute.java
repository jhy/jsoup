package org.jsoup.integration.routes;

import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class DeflateRoute {
    private static final String TextHtml = "text/html; charset=UTF-8";

    private DeflateRoute() {
    }

    /**
     Serves a raw deflated body so the client can verify transparent decompression
     */
    public static void handle(TestRequest request, TestResponse response) throws IOException {
        response.setContentType(TextHtml);
        response.setStatus(200);
        response.setHeader("Content-Encoding", "deflate");

        String doc = "<p>Hello, World!<p>That should be enough, right?<p>Hello, World!<p>That should be enough, right?";

        OutputStream bodyStream = response.bodyStream();
        DeflaterOutputStream stream = new DeflaterOutputStream(bodyStream,
            new Deflater(Deflater.BEST_COMPRESSION, true)); // true = nowrap zlib headers

        stream.write(doc.getBytes(StandardCharsets.UTF_8));
        stream.close();
    }
}
