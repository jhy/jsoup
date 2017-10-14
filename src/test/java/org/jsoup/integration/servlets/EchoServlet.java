package org.jsoup.integration.servlets;

import org.jsoup.helper.DataUtil;
import org.jsoup.helper.StringUtil;
import org.jsoup.integration.TestServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Enumeration;

import static org.jsoup.nodes.Entities.escape;

public class EchoServlet extends BaseServlet {
    public static final String Url = TestServer.map(EchoServlet.class);

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

    private void doIt(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType(TextHtml);
        res.setStatus(HttpServletResponse.SC_OK);
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

        // the request params
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] values = req.getParameterValues(name);
            write(w, name, StringUtil.join(values, ", "));
        }

        // rest body
        ByteBuffer byteBuffer = DataUtil.readToByteBuffer(req.getInputStream(), 0);
        String postData = new String(byteBuffer.array(), "UTF-8");
        if (!StringUtil.isBlank(postData)) {
            write(w, "Post Data", postData);
        }

        w.println("</table>");
    }

    private static void write(PrintWriter w, String key, String val) {
        w.println("<tr><th>" + escape(key) + "</th><td>" + escape(val) + "</td></tr>");
    }

    // allow the servlet to run as a main program, for local test
    public static void main(String[] args) {
        TestServer.start();
        System.out.println(Url);
    }
}
