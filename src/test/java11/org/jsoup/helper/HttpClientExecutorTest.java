package org.jsoup.helper;

import org.jsoup.internal.SharedConstants;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jsoup.Connection.Method.POST;
import static org.junit.jupiter.api.Assertions.*;

public class HttpClientExecutorTest {
    @Test void loadsMultiReleaseHttpClientExecutor() {
        // sanity check that the test is resolving the packaged Java 11 override, not a copy on the test classpath
        String resource = HttpClientTestAccess.executorClassResource().toExternalForm();
        assertTrue(resource.contains("/META-INF/versions/11/"), resource);
    }

    @Test void getsHttpClient() {
        try {
            enableHttpClient();
            RequestExecutor executor = RequestDispatch.get(new HttpConnection.Request(), null);
            assertTrue(HttpClientTestAccess.isHttpClientExecutor(executor));
        } finally {
            disableHttpClient(); // reset to previous default for JDK8 compat tests
        }
    }

    @Test void getsHttpClientByDefault() {
        System.clearProperty(SharedConstants.UseHttpClient);
        RequestExecutor executor = RequestDispatch.get(new HttpConnection.Request(), null);
        assertTrue(HttpClientTestAccess.isHttpClientExecutor(executor));
    }

    @Test void requestBodyStreamIsNotBuffered() {
        AtomicInteger reads = new AtomicInteger();
        InputStream stream = new InputStream() {
            @Override public int read() {
                reads.incrementAndGet();
                return -1;
            }
        };
        HttpConnection.Request request = new HttpConnection.Request();
        request.method(POST);
        request.requestBodyStream(stream);

        // building the publisher should not consume the stream; HttpClient only reads when sending
        HttpRequest.BodyPublisher publisher = HttpClientTestAccess.requestBody(request);
        assertEquals(-1, publisher.contentLength());
        assertEquals(0, reads.get());
    }

    @Test void downgradesSocksProxyToUrlConnectionExecutor() {
        try {
            enableHttpClient();
            HttpConnection.Request request = new HttpConnection.Request();
            request.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 1080)));

            // SOCKS handling only matters on the Java 11+ path where HttpClient would otherwise be selected (and just bypasses)
            RequestExecutor executor = RequestDispatch.get(request, null);
            assertInstanceOf(UrlConnectionExecutor.class, executor);
        } finally {
            disableHttpClient(); // reset to previous default for JDK8 compat tests
        }
    }

    public static void enableHttpClient() {
        System.setProperty(SharedConstants.UseHttpClient, "true");
    }

    public static void disableHttpClient() {
        System.setProperty(SharedConstants.UseHttpClient, "false");
    }

    @Test void proxyWrapUsesSystemDefaultProxySelector() {
        ProxySelector originalSelector = ProxySelector.getDefault();
        InetSocketAddress defaultProxy = new InetSocketAddress("system.proxy", 8080);

        try {
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return Collections.singletonList(
                        new Proxy(Proxy.Type.HTTP, defaultProxy)
                    );
                }
                
                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {}
            });

            ProxySelector wrap = HttpClientTestAccess.newProxyWrap();
            List<Proxy> proxies = wrap.select(URI.create("http://example.com"));

            assertEquals(1, proxies.size());
            assertSame(defaultProxy, proxies.get(0).address());
        } finally {
            ProxySelector.setDefault(originalSelector);
        }
    }

    @Test void proxyWrapConnectFailedOnlyForSystemProxy() {
        try {
            ProxySelector wrap = HttpClientTestAccess.newProxyWrap();
            HttpClientTestAccess.setPerRequestProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("custom", 9090)));
            wrap.connectFailed(URI.create("http://example.com"),
                new InetSocketAddress("custom", 9090),
                new IOException("test"));
        } finally {
            HttpClientTestAccess.clearPerRequestProxy();
        }
    }

    @Test
    void perRequestProxyOverridesSystemDefault() {
        ProxySelector original = ProxySelector.getDefault();
        InetSocketAddress sysProxy = new InetSocketAddress("system.proxy", 8080);
        InetSocketAddress perReqProxy = new InetSocketAddress("per.request", 9999);
        try {
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return Collections.singletonList(
                        new Proxy(Proxy.Type.HTTP, sysProxy));
                }
                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {}
            });

            HttpClientTestAccess.setPerRequestProxy(
                new Proxy(Proxy.Type.HTTP, perReqProxy));

            ProxySelector wrap = HttpClientTestAccess.newProxyWrap();
            List<Proxy> proxies = wrap.select(URI.create("http://example.com"));
            assertSame(perReqProxy, proxies.get(0).address());
        } finally {
            HttpClientTestAccess.clearPerRequestProxy();
            ProxySelector.setDefault(original);
        }
    }

    @Test void connectFailedDelegatesToSystemDefault() {
        ProxySelector original = ProxySelector.getDefault();
        final boolean[] called = {false};
        try {
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) { return Collections.singletonList(Proxy.NO_PROXY); }
                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) { called[0] = true; }
            });
            HttpClientTestAccess.newProxyWrap()
                .connectFailed(URI.create("http://example.com"), new InetSocketAddress("x", 80), new IOException("x"));
            assertTrue(called[0]);
        } finally {
            ProxySelector.setDefault(original);
        }
    }
}
