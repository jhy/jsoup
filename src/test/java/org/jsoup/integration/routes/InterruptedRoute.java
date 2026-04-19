package org.jsoup.integration.routes;

import org.jsoup.integration.netty.RawResponseWriter;
import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;

import java.nio.charset.StandardCharsets;

public final class InterruptedRoute {
    public static final String Magnitude = "magnitude";
    public static final String Larger = "larger";
    private static final String TextHtml = "text/html; charset=UTF-8";

    private InterruptedRoute() {
    }

    /**
     Writes deliberately malformed content-length responses so clients can validate EOF handling
     */
    public static void handle(TestRequest req, TestResponse res) {
        String magnitude = req.parameter(Magnitude);
        magnitude = magnitude == null ? "" : magnitude;

        StringBuilder sb = new StringBuilder();
        sb.append("<title>Something</title>");
        while (sb.length() <= 32 * 1024) {
            sb.append("<div>A suitable amount of data.</div>\n");
        }
        byte[] partialData = sb.toString().getBytes(StandardCharsets.UTF_8);
        sb.append("<p>Finale.</p>");
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);

        if (magnitude.equals(Larger)) {
            res.setContentLength(data.length * 2L);
        } else {
            res.setContentLength(data.length / 2);
        }

        res.setContentType(TextHtml);
        res.setStatus(200);
        res.closeConnection();
        RawResponseWriter raw = res.raw();
        if (magnitude.equals(Larger)) {
            raw.write(partialData);
        } else {
            raw.write(data);
        }
        raw.close();
    }
}
