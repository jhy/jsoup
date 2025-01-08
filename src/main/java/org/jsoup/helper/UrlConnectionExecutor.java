package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.internal.Functions;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jsoup.helper.HttpConnection.Response;

/**
 Execute HTTP requests using the HttpURLConnection implementation. Currently used by default; set system property
 {@code jsoup.useHttpClient} to {@code false} to explicitly set.
 */
class UrlConnectionExecutor extends RequestExecutor {
    @Nullable
    HttpURLConnection conn;

    UrlConnectionExecutor(HttpConnection.Request req, HttpConnection.@Nullable Response prevRes) {
        super(req, prevRes);
    }

    @Override
    HttpConnection.Response execute() throws IOException {
        try {
            conn = createConnection(req);
            conn.connect();
            if (conn.getDoOutput()) {
                try (OutputStream out = conn.getOutputStream()) {
                    Response.writePost(req, out);
                } catch (IOException e) {
                    conn.disconnect();
                    throw e;
                }
            }

            // set up url, method, header, cookies
            Response res = new Response(req);
            res.executor = this;
            res.method = Connection.Method.valueOf(conn.getRequestMethod());
            res.url = conn.getURL();
            res.statusCode = conn.getResponseCode();
            res.statusMessage = conn.getResponseMessage();
            res.contentType = conn.getContentType();
            res.contentLength = conn.getContentLength();
            Map<String, List<String>> resHeaders = createHeaderMap(conn);
            res.prepareResponse(resHeaders, prevRes);

            return res;
        } catch (IOException e) {
            safeClose();
            throw e;
        }
    }

    @Override
    InputStream responseBody() throws IOException {
        if (conn == null) throw new IllegalStateException("Not yet executed");
        return conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
    }

    @Override
    void safeClose() {
        if (conn != null) {
            conn.disconnect();
            conn = null;
        }
    }

    // set up connection defaults, and details from request
    private static HttpURLConnection createConnection(HttpConnection.Request req) throws IOException {
        Proxy proxy = req.proxy();
        final HttpURLConnection conn = (HttpURLConnection) (
            proxy == null ?
                req.url().openConnection() :
                req.url().openConnection(proxy)
        );

        conn.setRequestMethod(req.method().name());
        conn.setInstanceFollowRedirects(false); // don't rely on native redirection support
        conn.setConnectTimeout(req.timeout());
        conn.setReadTimeout(req.timeout() / 2); // gets reduced after connection is made and status is read

        if (req.sslSocketFactory() != null && conn instanceof HttpsURLConnection)
            ((HttpsURLConnection) conn).setSSLSocketFactory(req.sslSocketFactory());
        if (req.authenticator != null)
            AuthenticationHandler.handler.enable(req.authenticator, conn); // removed in finally
        if (req.method().hasBody())
            conn.setDoOutput(true);
        CookieUtil.applyCookiesToRequest(req, conn::addRequestProperty); // from the Request key/val cookies and the Cookie Store
        for (Map.Entry<String, List<String>> header : req.multiHeaders().entrySet()) {
            for (String value : header.getValue()) {
                conn.addRequestProperty(header.getKey(), value);
            }
        }
        return conn;
    }

    private static LinkedHashMap<String, List<String>> createHeaderMap(HttpURLConnection conn) {
        // the default sun impl of conn.getHeaderFields() returns header values out of order
        final LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        int i = 0;
        while (true) {
            final String key = conn.getHeaderFieldKey(i);
            final String val = conn.getHeaderField(i);
            if (key == null && val == null)
                break;
            i++;
            if (key == null || val == null)
                continue; // skip http1.1 line

            final List<String> vals = headers.computeIfAbsent(key, Functions.listFunction());
            vals.add(val);
        }
        return headers;
    }
}
