package org.jsoup.integration.routes;

import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;

public final class HelloRoute {
    private static final String TextHtml = "text/html; charset=UTF-8";

    private HelloRoute() {
    }

    /**
     Returns a tiny HTML page so connect tests can validate basic fetch behavior
     */
    public static void handle(TestRequest request, TestResponse response) {
        response.setContentType(TextHtml);
        response.setStatus(200);
        response.write("<p>Hello, World!");
    }
}
