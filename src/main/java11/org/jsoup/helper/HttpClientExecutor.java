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
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import static org.jsoup.helper.HttpConnection.Response;
import static org.jsoup.helper.HttpConnection.Response.writePost;

/**
 Executes requests using the HttpClient, for http/2 support. Enabled by default when available. To disable, set
 property {@code jsoup.useHttpClient} to {@code false}.
 */
class HttpClientExecutor extends RequestExecutor {
    private static final Object SharedClientLock = new Object();
    private static volatile @Nullable ClientState sharedClientState;

    // HttpClient expects proxy settings per client; jsoup supports them per request, so resolve them from the calling
    // thread.
    static ThreadLocal<@Nullable Proxy> perRequestProxy = new ThreadLocal<>();

    @Nullable
    HttpResponse<InputStream> hRes;

    public HttpClientExecutor(HttpConnection.Request request, HttpConnection.@Nullable Response previousResponse) {
        super(request, previousResponse);
    }

    /**
     Returns the shared client for default requests, or the owning connection's configured client.
     */
    HttpClient client() {
        RequestAuthenticator auth = req.authenticator;
        // HttpClient captures the JVM default SSL context when built, and that default can change at runtime.
        SSLContext sslContext = req.sslContext != null ? req.sslContext : defaultSslContext();
        if (auth == null && req.sslContext == null) return sharedClient(sslContext);
        return configuredClient(auth, sslContext);
    }

    private HttpClient configuredClient(@Nullable RequestAuthenticator auth, SSLContext sslContext) {
        ClientState state = (ClientState) req.connection.clientState;
        if (state != null && state.matches(auth, sslContext)) return state.client;
        synchronized (req.connection) {
            // another request may have initialized this connection's client while we waited for the lock.
            state = (ClientState) req.connection.clientState;
            if (state == null || !state.matches(auth, sslContext)) {
                state = new ClientState(auth, sslContext);
                req.connection.clientState = state;
            }
            return state.client;
        }
    }

    private static HttpClient sharedClient(SSLContext sslContext) {
        ClientState state = sharedClientState;
        if (state != null && state.matches(null, sslContext)) return state.client;
        synchronized (SharedClientLock) {
            state = sharedClientState;
            if (state != null && state.matches(null, sslContext)) return state.client;
            state = new ClientState(null, sslContext);
            sharedClientState = state;
            return state.client;
        }
    }

    private static HttpClient newClient(@Nullable RequestAuthenticator authenticator, SSLContext sslContext) {
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.followRedirects(HttpClient.Redirect.NEVER); // customized redirects
        builder.proxy(new ProxyWrap()); // thread local impl for per request; called on executing thread
        if (authenticator != null) builder.authenticator(new AuthenticationHandler(authenticator));
        builder.sslContext(sslContext);
        return builder.build();
    }

    private static SSLContext defaultSslContext() {
        try {
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No default SSL context is available", e);
        }
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
            res.statusMessage = StatusMessage(res.statusCode);
            res.contentType = headers.firstValue("content-type").orElse(null);
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

    /**
     As HTTP/2 no longer provides a server-set status message, and HttpClient doesn't parse it for 1.1, just provide minimal stock ones, for loggers.
     */
    static String StatusMessage(int statusCode) {
        if (statusCode < 400) return "OK";
        if (statusCode == 404) return "Not Found";
        return "Error " + statusCode;
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
        if (!req.method.hasBody())
            return HttpRequest.BodyPublishers.noBody();

        InputStream bodyStream = req.requestBodyStream();
        if (bodyStream != null) {
            // stream the upload so that the HttpClient does not buffer it before sending
            // a caller supplied stream is one-shot, so a replay gets nothing via an AtomicReference
            AtomicReference<InputStream> stream = new AtomicReference<>(bodyStream);
            return HttpRequest.BodyPublishers.ofInputStream(() -> stream.getAndSet(null));
        }

        // other bodies are fields or text; need to serialize
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writePost(req, buf);
        return HttpRequest.BodyPublishers.ofByteArray(buf.toByteArray());
    }

    static class ProxyWrap extends ProxySelector {
        // empty list for no proxy:
        static final List<Proxy> NoProxy = new ArrayList<>(0);

        @Override
        public List<Proxy> select(URI uri) {
            Proxy proxy = perRequestProxy.get();
            if (proxy != null) {
                return Collections.singletonList(proxy);
            }
            ProxySelector defaultSelector = ProxySelector.getDefault();
            if (defaultSelector != null && defaultSelector != this) { // avoid recursion if we were set as default
                return defaultSelector.select(uri);
            }
            return NoProxy;
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            if (perRequestProxy.get() != null) {
                return;  // no-op
            }
            ProxySelector defaultSelector = ProxySelector.getDefault();
            if (defaultSelector != null && defaultSelector != this) {
                defaultSelector.connectFailed(uri, sa, ioe);
            }
        }
    }

    private static final class ClientState {
        final HttpClient client;
        final @Nullable RequestAuthenticator authenticator;
        final SSLContext sslContext;

        ClientState(@Nullable RequestAuthenticator authenticator, SSLContext sslContext) {
            this.client = newClient(authenticator, sslContext);
            this.authenticator = authenticator;
            this.sslContext = sslContext;
        }

        boolean matches(@Nullable RequestAuthenticator authenticator, SSLContext sslContext) {
            return this.authenticator == authenticator && this.sslContext == sslContext;
        }
    }
}
