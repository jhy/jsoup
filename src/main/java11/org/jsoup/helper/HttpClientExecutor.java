package org.jsoup.helper;

import org.jsoup.Connection;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jsoup.helper.HttpConnection.Response;
import static org.jsoup.helper.HttpConnection.Response.writePost;

/**
 Executes requests using the HttpClient, for http/2 support. Currently disabled by default; enable by setting system
 property {@code jsoup.useHttpClient} to {@code true}.
 */
class HttpClientExecutor extends RequestExecutor {
    // HttpClient expects proxy settings per client; we do per request, so held as a thread local. Can't do same for
    // auth because that callback is on a worker thread, so can only do auth per Connection. So we create a new client
    // if the authenticator is different between requests
    static ThreadLocal<Proxy> perRequestProxy = new ThreadLocal<>();

    @Nullable
    HttpResponse<InputStream> hRes;

    public HttpClientExecutor(HttpConnection.Request request, HttpConnection.@Nullable Response previousResponse) {
        super(request, previousResponse);
    }

    /**
     Retrieve the HttpClient from the Connection, or create a new one. Allows for connection pooling of requests in the
     same Connection (session).
     */
    HttpClient client() {
        // we try to reuse the same Client across requests in a given Connection; but if the request auth has changed, we need to create a new client
        RequestAuthenticator prevAuth = req.connection.lastAuth;
        req.connection.lastAuth = req.authenticator;
        if (req.connection.client != null && prevAuth == req.authenticator) { // might both be null
            return (HttpClient) req.connection.client;
        }

        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.followRedirects(HttpClient.Redirect.NEVER); // customized redirects
        builder.proxy(new ProxyWrap()); // thread local impl for per request; called on executing thread
        if (req.authenticator != null) builder.authenticator(new AuthenticationHandler(req.authenticator));

        HttpClient client = builder.build();
        req.connection.client = client;
        return client;
    }

    @Override
    HttpConnection.Response execute() throws IOException {
        try {
            HttpRequest.Builder reqBuilder =
                HttpRequest.newBuilder(req.url.toURI()).method(req.method.name(), requestBody(req));
            if (req.timeout() > 0) reqBuilder.timeout(
                Duration.ofMillis(req.timeout())); // infinite if unset (UrlConnection / jsoup uses 0 for same)
            CookieUtil.applyCookiesToRequest(req, reqBuilder::header);

            // headers:
            req.multiHeaders().forEach((key, values) -> {
                values.forEach(value -> reqBuilder.header(key, value));
            });

            if (req.proxy() != null) perRequestProxy.set(req.proxy()); // set up per request proxy
            HttpRequest hReq = reqBuilder.build();
            HttpClient client = client();
            hRes = client.send(hReq, HttpResponse.BodyHandlers.ofInputStream());
            HttpHeaders headers = hRes.headers();

            // set up the response
            Response res = new Response(req);
            res.executor = this;
            res.method = Connection.Method.valueOf(hRes.request().method());
            res.url = hRes.uri().toURL();
            res.statusCode = hRes.statusCode();
            res.contentType = headers.firstValue("content-type").orElse("");
            long length = headers.firstValueAsLong("content-length").orElse(-1);
            res.contentLength = length < Integer.MAX_VALUE ? (int) length : -1;
            res.prepareResponse(headers.map(), prevRes);

            return res;
        } catch (IOException e) {
            safeClose();
            throw e;
        } catch (InterruptedException e) {
            safeClose();
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL: " + req.url, e);
        } finally {
            // detach per request proxy
            perRequestProxy.remove();
        }
    }

    @Override
    InputStream responseBody() throws IOException {
        if (hRes == null) throw new IllegalStateException("Not yet executed");
        return hRes.body();
    }

    @Override
    void safeClose() {
        if (hRes != null) {
            InputStream body = hRes.body();
            if (body != null) {
                try {
                    body.close();
                } catch (IOException ignored) {}
            }
            hRes = null;
        }
    }

    static HttpRequest.BodyPublisher requestBody(final HttpConnection.Request req) throws IOException {
        if (req.method.hasBody()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            writePost(req, buf);
            return HttpRequest.BodyPublishers.ofByteArray(buf.toByteArray());
        } else {
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    static class ProxyWrap extends ProxySelector {
        // empty list for no proxy:
        static final List<Proxy> NoProxy = new ArrayList<>(0);

        @Override
        public List<Proxy> select(URI uri) {
            Proxy proxy = perRequestProxy.get();
            return proxy != null ? Collections.singletonList(proxy) : NoProxy;
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            // no-op
        }
    }
}
