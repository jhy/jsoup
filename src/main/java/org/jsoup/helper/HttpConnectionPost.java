package org.jsoup.helper;

import org.jsoup.Connection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;

/**
 Package-private helper that handles POST serialization and request URL setup for {@link HttpConnection}.
 Extracted from HttpConnection to reduce its size (Large Class smell).
 */
class HttpConnectionPost {

    static String encodeMimeName(String val) {
        return val.replace("\"", "%22");
    }

    static void setOutputContentType(final HttpConnection.Request req) {
        final String contentType = req.header(HttpConnection.CONTENT_TYPE);
        String bound = null;
        if (contentType != null) {
            if (contentType.contains(HttpConnection.MULTIPART_FORM_DATA) && !contentType.contains("boundary")) {
                bound = DataUtil.mimeBoundary();
                req.header(HttpConnection.CONTENT_TYPE, HttpConnection.MULTIPART_FORM_DATA + "; boundary=" + bound);
            }
        } else if (needsMultipart(req)) {
            bound = DataUtil.mimeBoundary();
            req.header(HttpConnection.CONTENT_TYPE, HttpConnection.MULTIPART_FORM_DATA + "; boundary=" + bound);
        } else {
            req.header(HttpConnection.CONTENT_TYPE, HttpConnection.FORM_URL_ENCODED + "; charset=" + req.postDataCharset());
        }
        req.mimeBoundary = bound;
    }

    static void writePost(final HttpConnection.Request req, final OutputStream outputStream) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(outputStream, req.postDataCharset());
             BufferedWriter w = new BufferedWriter(osw)) {
            implWritePost(req, w, outputStream);
        }
    }

    private static void implWritePost(final HttpConnection.Request req, final BufferedWriter w, final OutputStream outputStream) throws IOException {
        final java.util.Collection<Connection.KeyVal> data = req.data();
        final String boundary = req.mimeBoundary;

        if (boundary != null) { // a multipart post
            for (Connection.KeyVal keyVal : data) {
                w.write("--");
                w.write(boundary);
                w.write("\r\n");
                w.write("Content-Disposition: form-data; name=\"");
                w.write(encodeMimeName(keyVal.key())); // encodes " to %22
                w.write("\"");
                final InputStream input = keyVal.inputStream();
                if (input != null) {
                    w.write("; filename=\"");
                    w.write(encodeMimeName(keyVal.value()));
                    w.write("\"\r\nContent-Type: ");
                    String ct = keyVal.contentType();
                    w.write(ct != null ? ct : HttpConnection.DefaultUploadType);
                    w.write("\r\n\r\n");
                    w.flush();
                    DataUtil.crossStreams(input, outputStream);
                    outputStream.flush();
                } else {
                    w.write("\r\n\r\n");
                    w.write(keyVal.value());
                }
                w.write("\r\n");
            }
            w.write("--");
            w.write(boundary);
            w.write("--");
        } else if (req.body != null) { // a single body (bytes or plain text); data will be in query string
            if (req.body instanceof String) {
                w.write((String) req.body);
            } else if (req.body instanceof InputStream) {
                DataUtil.crossStreams((InputStream) req.body, outputStream);
                outputStream.flush();
            } else {
                throw new IllegalStateException();
            }
        } else { // regular form data (application/x-www-form-urlencoded)
            boolean first = true;
            for (Connection.KeyVal keyVal : data) {
                if (!first) w.append('&');
                else first = false;

                w.write(URLEncoder.encode(keyVal.key(), req.postDataCharset()));
                w.write('=');
                w.write(URLEncoder.encode(keyVal.value(), req.postDataCharset()));
            }
        }
    }

    static void serialiseRequestUrl(Connection.Request req) throws IOException {
        UrlBuilder in = new UrlBuilder(req.url());

        for (Connection.KeyVal keyVal : req.data()) {
            Validate.isFalse(keyVal.hasInputStream(), "InputStream data not supported in URL query string.");
            in.appendKeyVal(keyVal);
        }
        req.url(in.build());
        req.data().clear(); // moved into url as get params
    }

    private static boolean needsMultipart(Connection.Request req) {
        for (Connection.KeyVal keyVal : req.data()) {
            if (keyVal.hasInputStream())
                return true;
        }
        return false;
    }
}
