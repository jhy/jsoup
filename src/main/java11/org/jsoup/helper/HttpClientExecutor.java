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
import java.util.List;
import java.util.Map;

import static org.jsoup.helper.HttpConnection.Response;
import static org.jsoup.helper.HttpConnection.Response.writePost;

/**
 Executes requests using the HttpClient, for http/2 support. Currently disabled by default; enable by setting system
 property {@code jsoup.useHttpClient} to {@code true}.
 */
class HttpClientExecutor extends RequestExecutor {
    @Nullable
    HttpResponse<InputStream> hRes;

    public HttpClientExecutor(HttpConnection.Request request, HttpConnection.@Nullable Response previousResponse) {
        super(request, previousResponse);
    }

    @Override
    HttpConnection.Response execute() throws IOException {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder();
            Proxy proxy = req.proxy();
            if (proxy != null) builder.proxy(new ProxyWrap(proxy));
            builder.followRedirects(HttpClient.Redirect.NEVER); // customized redirects
            //builder.connectTimeout(Duration.ofMillis(req.timeout()/2)); // jsoup timeout is total connect + all reads
            // todo - how to handle socketfactory? HttpClient wants SSLContext...
            if (req.authenticator != null) {
                AuthenticationHandler.AuthShim handler = new RequestAuthHandler();
                handler.enable(req.authenticator, builder);
            }
            HttpClient client = builder.build();

            HttpRequest.Builder reqBuilder =
                HttpRequest.newBuilder(req.url.toURI()).method(req.method.name(), requestBody(req));
            if (req.timeout() > 0) reqBuilder.timeout(
                Duration.ofMillis(req.timeout())); // infinite if unset (UrlConnection / jsoup uses 0 for same)
            CookieUtil.applyCookiesToRequest(req, reqBuilder::header);

            // headers:
            for (Map.Entry<String, List<String>> header : req.multiHeaders().entrySet()) {
                for (String value : header.getValue()) {
                    reqBuilder.header(header.getKey(), value);
                }
            }

            HttpRequest hReq = reqBuilder.build();
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
            throw new IOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL: " + req.url, e);
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
            // no real closer
            // todo - review
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
        final List<Proxy> proxies;

        public ProxyWrap(Proxy proxy) {
            this.proxies = new ArrayList<>(1);
            proxies.add(proxy);
        }

        @Override
        public List<Proxy> select(URI uri) {
            return proxies;
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            // no-op
        }
    }
}
