package org.jsoup.integration.servlets;

import org.jsoup.integration.TestServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectServlet extends BaseServlet {
    public static final String Url = TestServer.map(RedirectServlet.class);
    public static final String LocationParam = "loc";
    public static final String CodeParam = "code";
    private static final int DefaultCode = HttpServletResponse.SC_MOVED_TEMPORARILY;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String location = req.getParameter(LocationParam);
        if (location == null)
            location = "";

        int intCode = DefaultCode;
        String code = req.getParameter(CodeParam);
        if (code != null)
            intCode = Integer.parseInt(code);

        res.setHeader("Location", location);
        res.setStatus(intCode);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }
}
