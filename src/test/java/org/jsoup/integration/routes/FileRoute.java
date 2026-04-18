package org.jsoup.integration.routes;

import org.jsoup.integration.ParseTest;
import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class FileRoute {
    public static final String ContentTypeParam = "contentType";
    public static final String HtmlType = "text/html";
    static final String XmlType = "text/xml";
    public static final String SuppressContentLength = "surpriseMe";

    private FileRoute() {
    }

    /**
     Serves test fixture files with controllable content-type and framing headers
     */
    public static void handle(TestRequest req, TestResponse res) throws IOException {
        String location = req.pathInfo();
        if (location == null) {
            res.sendError(404);
            return;
        }

        String contentType = req.parameter(ContentTypeParam);
        if (contentType == null) {
            contentType = HtmlType;
            if (location.contains(".xml")) contentType = XmlType;
        }

        File file = ParseTest.getFile(location);
        if (file.exists()) {
            res.setContentType(contentType);
            if (file.getName().endsWith("gz"))
                res.addHeader("Content-Encoding", "gzip");
            res.setStatus(200);

            if (req.parameter(SuppressContentLength) == null) {
                if ("HEAD".equals(req.method())) {
                    res.setContentLength(file.length());
                    return;
                }
                Files.copy(file.toPath(), res.bodyStream());
            } else {
                res.startChunked();
                if (!"HEAD".equals(req.method())) {
                    res.writeChunk(Files.readAllBytes(file.toPath()));
                }
                res.finish();
            }
        } else {
            res.sendError(404);
        }
    }
}
