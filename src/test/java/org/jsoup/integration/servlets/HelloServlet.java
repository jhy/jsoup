package org.jsoup.integration.servlets;

import org.jsoup.integration.TestServer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloServlet extends BaseServlet {
    public static final String Url;
    public static final String TlsUrl;
    static {
        TestServer.ServletUrls urls = TestServer.map(HelloServlet.class);
        Url = urls.url;
        TlsUrl = urls.tlsUrl;
    }

    @Override
    protected void doIt(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType(TextHtml);
        res.setStatus(HttpServletResponse.SC_OK);

        String doc = "<p>Hello, World!";
        res.getWriter().write(doc);
    }
}
