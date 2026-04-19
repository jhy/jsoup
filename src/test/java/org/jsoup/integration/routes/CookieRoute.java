package org.jsoup.integration.routes;

import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;

import java.io.PrintWriter;

public final class CookieRoute {
    public static final String SetCookiesParam = "setCookies";
    public static final String LocationParam = "loc";
    private static final String TextHtml = "text/html; charset=UTF-8";

    private CookieRoute() {
    }

    /**
     Sets path-scoped cookies and echoes the received cookies for session tests
     */
    public static void handle(TestRequest req, TestResponse res) {
        // Do we want to set cookies?
        if (req.parameter(SetCookiesParam) != null)
            setCookies(res);

        // Do we want to redirect elsewhere?
        String loc = req.parameter(LocationParam);
        if (loc != null) {
            res.sendRedirect(loc);
            return;
        }

        // print out the cookies that were received
        res.setContentType(TextHtml);
        res.setStatus(200);

        PrintWriter w = res.writer();
        w.println("<table>");
        for (io.netty.handler.codec.http.cookie.Cookie cookie : req.cookies()) {
            EchoRoute.write(w, cookie.name(), cookie.value());
        }
        w.println("</table>");
    }

    /**
     Sends the cookie set used by the session path-scoping tests
     */
    private static void setCookies(TestResponse res) {
        res.addHeader("Set-Cookie", "One=Root; Path=/");
        res.addHeader("Set-Cookie", "One=Cookie; Path=/Cookie; HttpOnly; Comment=\"Quite nice\"");
        res.addHeader("Set-Cookie", "One=Echo; Path=/Echo");
        res.addHeader("Set-Cookie", "Two=NoSuchPath; Path=/bogus");
        res.addHeader("Set-Cookie", "Two=Override; Path=/bogus");
    }

}
