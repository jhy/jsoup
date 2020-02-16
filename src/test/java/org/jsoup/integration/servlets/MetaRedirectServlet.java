package org.jsoup.integration.servlets;

import org.jsoup.integration.TestServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MetaRedirectServlet extends BaseServlet {
    public static final String Url = TestServer.map(MetaRedirectServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType(TextHtml);
        res.setStatus(HttpServletResponse.SC_OK);

        String doc = "<meta http-equiv=\"refresh\" content=\"0;url=" + HelloServlet.Url + "\">";
        res.getWriter().write(doc);
    }
}