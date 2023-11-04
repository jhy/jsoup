package org.jsoup.integration.servlets;

import org.jsoup.integration.TestServer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class CookieServlet extends BaseServlet {
    public static final String Url;
    public static final String TlsUrl;
    static {
        TestServer.ServletUrls urls = TestServer.map(CookieServlet.class);
        Url = urls.url;
        TlsUrl = urls.tlsUrl;
    }
    public static final String SetCookiesParam = "setCookies";
    public static final String LocationParam = "loc";

    @Override
    protected void doIt(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // Do we want to set cookies?
        if (req.getParameter(SetCookiesParam) != null)
            setCookies(res);

        // Do we want to redirect elsewhere?
        String loc = req.getParameter(LocationParam);
        if (loc != null) {
            res.sendRedirect(loc);
            return;
        }

        // print out the cookies that were received
        res.setContentType(TextHtml);
        res.setStatus(200);

        PrintWriter w = res.getWriter();
        w.println("<table>");
        final Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                EchoServlet.write(w, cookie.getName(), cookie.getValue());
            }
        }
        w.println("</table>");
    }

    private void setCookies(HttpServletResponse res) {
        Cookie one = new Cookie("One", "Root");
        one.setPath("/");
        res.addCookie(one);

        Cookie two = new Cookie("One", "CookieServlet");
        two.setPath("/CookieServlet");
        two.setHttpOnly(true);
        two.setComment("Quite nice");
        res.addCookie(two);

        Cookie three = new Cookie("One", "EchoServlet");
        three.setPath("/EchoServlet");
        res.addCookie(three);
    }

}
