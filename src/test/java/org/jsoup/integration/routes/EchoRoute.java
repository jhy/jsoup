package org.jsoup.integration.routes;

import org.jsoup.integration.netty.TestPart;
import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;
import org.jsoup.internal.StringUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.jsoup.nodes.Entities.escape;

public final class EchoRoute {
    public static final String CodeParam = "code";
    private static final int DefaultCode = 200;
    static final String TextHtml = "text/html;charset=utf-8";

    private EchoRoute() {
    }

    /**
     Echoes the request environment so the client tests can assert exact request handling
     */
    public static void handle(TestRequest req, TestResponse res) throws IOException {
        int intCode = DefaultCode;
        String code = req.header(CodeParam);
        if (code != null)
            intCode = Integer.parseInt(code);

        boolean isMulti = req.contentType() != null && req.contentType().startsWith("multipart/form-data");
        List<TestPart> parts = req.parts();

        res.setContentType(TextHtml);
        res.setStatus(intCode);
        // no-cache headers for test
        res.addHeader("Cache-Control", "no-cache");
        res.addHeader("Cache-Control", "no-store");

        PrintWriter w = res.writer();

        w.write("<title>Webserver Environment Variables</title>\n" +
            "    <style type=\"text/css\">\n" +
            "      body, td, th {font: 10pt Verdana, Arial, sans-serif; text-align: left}\n" +
            "      th {font-weight: bold}        \n" +
            "    </style>\n" +
            "    <body>\n" +
            "    <table border=\"0\">");

        // some get items
        write(w, "Method", req.method());
        write(w, "Request URI", req.requestUri());
        write(w, "Path Info", req.pathInfo());
        write(w, "Query String", req.queryString());

        for (String header : req.headerNames()) {
            for (String value : req.headers(header)) {
                write(w, header, value);
            }
        }

        // cookies
        if (!req.cookies().isEmpty()) {
            for (io.netty.handler.codec.http.cookie.Cookie cookie : req.cookies()) {
                EchoRoute.write(w, "Cookie: " + cookie.name(), cookie.value());
            }
        }

        // the request params
        for (String name : req.parameterNames()) {
            write(w, name, StringUtil.join(req.parameters(name), ", "));
        }

        // post body
        String postData = req.body();
        if (!StringUtil.isBlank(postData)) {
            write(w, "Post Data", postData);
        }

        // file uploads
        if (isMulti) {
            write(w, "Parts", String.valueOf(parts.size()));

            for (TestPart part : parts) {
                String name = part.name();
                write(w, "Part " + name + " ContentType", part.contentType());
                write(w, "Part " + name + " Name", name);
                write(w, "Part " + name + " Filename", part.submittedFileName());
                write(w, "Part " + name + " Size", String.valueOf(part.size()));
            }
        }

        w.println("</table>");
    }

    /**
     Writes one table row to the echoed HTML response
     */
    static void write(PrintWriter w, String key, String val) {
        w.println("<tr><th>" + escape(key) + "</th><td>" + escape(val) + "</td></tr>");
    }
}
