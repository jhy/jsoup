package org.jsoup.integration.servlets;

import org.jsoup.integration.TestServer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedirectServlet extends BaseServlet {
    public static final String Url = TestServer.map(RedirectServlet.class);
    public static final String LocationParam = "loc";
    public static final String CodeParam = "code";
    private static final int DefaultCode = HttpServletResponse.SC_MOVED_TEMPORARILY;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        String[] locations = req.getParameterValues(LocationParam);
        if (locations == null || locations.length == 0) {
            res.setHeader("Location", "");
        } else {
            for (final String location : locations) {
                res.addHeader("Location", location);
            }
        }

        int intCode = DefaultCode;
        String code = req.getParameter(CodeParam);
        if (code != null)
            intCode = Integer.parseInt(code);

        res.setStatus(intCode);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) {
        doGet(req, res);
    }
}
