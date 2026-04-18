package org.jsoup.integration.routes;

import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;

public final class RedirectRoute {
    public static final String LocationParam = "loc";
    public static final String CodeParam = "code";
    public static final String SetCookiesParam = "setCookies";
    private static final int DefaultCode = 302;

    private RedirectRoute() {
    }

    /**
     Returns the configured redirect response so client redirect handling can be exercised
     */
    public static void handle(TestRequest request, TestResponse response) {
        String location = request.parameter(LocationParam);
        if (location == null)
            location = "";

        int intCode = DefaultCode;
        String code = request.parameter(CodeParam);
        if (code != null)
            intCode = Integer.parseInt(code);

        if (request.parameter(SetCookiesParam) != null) {
            response.addCookie(new DefaultCookie("token", "asdfg123"));
            response.addCookie(new DefaultCookie("uid", "foobar"));
            response.addCookie(new DefaultCookie("uid", "jhy")); // dupe, should use latter
        }

        response.setHeader("Location", location);
        response.setStatus(intCode);
    }
}
