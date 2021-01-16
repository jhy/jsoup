package org.jsoup.integration.servlets;

import org.eclipse.jetty.server.Request;
import org.jsoup.helper.DataUtil;
import org.jsoup.internal.StringUtil;
import org.jsoup.integration.TestServer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;

import static org.jsoup.nodes.Entities.escape;

public class EchoServlet extends BaseServlet {
    public static final String CodeParam = "code";
    public static final String Url = TestServer.map(EchoServlet.class);
    private static final int DefaultCode = HttpServletResponse.SC_OK;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doIt(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doIt(req, res);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doIt(req, res);
    }

    private void doIt(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        int intCode = DefaultCode;
        String code = req.getHeader(CodeParam);
        if (code != null)
            intCode = Integer.parseInt(code);

        boolean isMulti = maybeEnableMultipart(req);

        res.setContentType(TextHtml);
        res.setStatus(intCode);
        // no-cache headers for test
        res.addHeader("Cache-Control", "no-cache");
        res.addHeader("Cache-Control", "no-store");

        PrintWriter w = res.getWriter();

        w.write("<title>Webserver Environment Variables</title>\n" +
            "    <style type=\"text/css\">\n" +
            "      body, td, th {font: 10pt Verdana, Arial, sans-serif; text-align: left}\n" +
            "      th {font-weight: bold}        \n" +
            "    </style>\n" +
            "    <body>\n" +
            "    <table border=\"0\">");

        // some get items
        write(w, "Method", req.getMethod());
        write(w, "Request URI", req.getRequestURI());
        write(w, "Query String", req.getQueryString());

        // request headers (why is it an enumeration?)
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            Enumeration<String> headers = req.getHeaders(header);
            while (headers.hasMoreElements()) {
                write(w, header, headers.nextElement());
            }
        }

        // cookies
        final Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                EchoServlet.write(w, "Cookie: " + cookie.getName(), cookie.getValue());
            }
        }

        // the request params
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] values = req.getParameterValues(name);
            write(w, name, StringUtil.join(values, ", "));
        }

        // post body
        ByteBuffer byteBuffer = DataUtil.readToByteBuffer(req.getInputStream(), 0);
        String postData = new String(byteBuffer.array(), StandardCharsets.UTF_8);
        if (!StringUtil.isBlank(postData)) {
            write(w, "Post Data", postData);
        }

        // file uploads
        if (isMulti) {
            Collection<Part> parts = req.getParts();
            write(w, "Parts", String.valueOf(parts.size()));

            for (Part part : parts) {
                String name = part.getName();
                write(w, "Part " + name + " ContentType", part.getContentType());
                write(w, "Part " + name + " Name", name);
                write(w, "Part " + name + " Filename", part.getSubmittedFileName());
                write(w, "Part " + name + " Size", String.valueOf(part.getSize()));
                part.delete();
            }
        }

        w.println("</table>");
    }

    static void write(PrintWriter w, String key, String val) {
        w.println("<tr><th>" + escape(key) + "</th><td>" + escape(val) + "</td></tr>");
    }

    // allow the servlet to run as a main program, for local test
    public static void main(String[] args) {
        TestServer.start();
        System.out.println(Url);
    }

    private static boolean maybeEnableMultipart(HttpServletRequest req) {
        boolean isMulti = req.getContentType() != null
            && req.getContentType().startsWith("multipart/form-data");

        if (isMulti) {
            req.setAttribute(Request.MULTIPART_CONFIG_ELEMENT, new MultipartConfigElement(
                System.getProperty("java.io.tmpdir")));
        }
        return isMulti;
    }
}
