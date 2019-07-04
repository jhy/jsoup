package org.jsoup.integration.servlets;

import org.jsoup.integration.ParseTest;
import org.jsoup.integration.TestServer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class FileServlet extends BaseServlet {
    public static final String Url = TestServer.map(FileServlet.class);
    public static final String ContentTypeParam = "contentType";
    public static final String LocationParam = "loc";
    public static final String DefaultType = "text/html";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String contentType = req.getParameter(ContentTypeParam);
        if (contentType == null)
            contentType = DefaultType;
        String location = req.getPathInfo();

        File file = ParseTest.getFile(location);
        if (file.exists()) {
            res.setContentType(contentType);
            res.setStatus(HttpServletResponse.SC_OK);

            ServletOutputStream out = res.getOutputStream();
            Files.copy(file.toPath(), out);
            out.flush();
        } else {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    public static String urlTo(String path) {
        return Url + path;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }
}
