package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.internal.SharedConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.jsoup.Connection.Method.POST;
import static org.junit.jupiter.api.Assertions.*;

public class HttpClientExecutorTest {
    @AfterEach void resetHttpClient() {
        HttpClientTestAccess.resetSharedClient();
        disableHttpClient();
    }

    @Test void loadsMultiReleaseHttpClientExecutor() {
        // sanity check that the test is resolving the packaged Java 11 override, not a copy on the test classpath
        String resource = HttpClientTestAccess.executorClassResource().toExternalForm();
        assertTrue(resource.contains("/META-INF/versions/11/"), resource);
    }

    @Test void getsHttpClient() {
        enableHttpClient();
        RequestExecutor executor = RequestDispatch.get(new HttpConnection.Request(), null);
        assertTrue(HttpClientTestAccess.isHttpClientExecutor(executor));
    }

    @Test void getsHttpClientByDefault() {
        System.clearProperty(SharedConstants.UseHttpClient);
        RequestExecutor executor = RequestDispatch.get(new HttpConnection.Request(), null);
        assertTrue(HttpClientTestAccess.isHttpClientExecutor(executor));
    }

    @Test void sharesDefaultClientAcrossConnections() {
        enableHttpClient();
        Object first = HttpClientTestAccess.client(Jsoup.connect("https://example.com/one"));
        Object second = HttpClientTestAccess.client(Jsoup.connect("https://example.com/two"));
        assertSame(first, second);
    }

    @Test void sharesDefaultClientAcrossConcurrentConnections() throws Exception {
        enableHttpClient();
        HttpClientTestAccess.resetSharedClient();
        Set<Object> clients = concurrentClients(i -> Jsoup.connect("https://example.com/" + i));
        assertEquals(1, clients.size());
    }

    @Test void refreshesDefaultClientWhenSslContextChanges() throws Exception {
        SSLContext original = SSLContext.getDefault();
        SSLContext replacement = SSLContext.getInstance("TLS");
        replacement.init(null, null, null);
        try {
            enableHttpClient();
            HttpClientTestAccess.resetSharedClient();
            Object first = HttpClientTestAccess.client(Jsoup.connect("https://example.com/one"));
            SSLContext.setDefault(replacement);
            Object second = HttpClientTestAccess.client(Jsoup.connect("https://example.com/two"));
            assertNotSame(first, second);
            assertSame(replacement, ((HttpClient) second).sslContext());
        } finally {
            SSLContext.setDefault(original);
        }
    }

    @Test void reusesConfiguredClientWithinSession() {
        enableHttpClient();
        Connection session = Jsoup.newSession().auth(context -> null);
        Object first = HttpClientTestAccess.client(session.newRequest("https://example.com/one"));
        Object second = HttpClientTestAccess.client(session.newRequest("https://example.com/two"));
        assertSame(first, second);
    }

    @Test void rebuildsClientWhenAuthenticatorChanges() {
        enableHttpClient();
        RequestAuthenticator firstAuth = context -> null;
        RequestAuthenticator secondAuth = context -> null;
        assertNotSame(firstAuth, secondAuth);

        Connection session = Jsoup.newSession();
        Object first = HttpClientTestAccess.client(
            session.newRequest("https://example.com/one").auth(firstAuth));
        Object second = HttpClientTestAccess.client(
            session.newRequest("https://example.com/two").auth(secondAuth));
        assertNotSame(first, second);
    }

    @Test void rebuildsClientWhenSslContextChanges() throws Exception {
        SSLContext firstContext = SSLContext.getInstance("TLS");
        SSLContext secondContext = SSLContext.getInstance("TLS");
        firstContext.init(null, null, null);
        secondContext.init(null, null, null);
        enableHttpClient();

        Connection session = Jsoup.newSession();
        Object first = HttpClientTestAccess.client(
            session.newRequest("https://example.com/one").sslContext(firstContext));
        Object second = HttpClientTestAccess.client(
            session.newRequest("https://example.com/two").sslContext(secondContext));
        assertNotSame(first, second);
    }

    @Test void createsOneClientForConcurrentSessionRequests() throws Exception {
        enableHttpClient();
        Connection session = Jsoup.newSession().auth(context -> null);
        Set<Object> clients = concurrentClients(i -> session.newRequest("https://example.com/" + i));
        assertEquals(1, clients.size());
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
        enableHttpClient();
        HttpConnection.Request request = new HttpConnection.Request();
        request.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 1080)));

        // SOCKS handling only matters on the Java 11+ path where HttpClient would otherwise be selected (and just bypasses)
        RequestExecutor executor = RequestDispatch.get(request, null);
        assertInstanceOf(UrlConnectionExecutor.class, executor);
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

    private static Set<Object> concurrentClients(IntFunction<Connection> connectionFactory) throws Exception {
        int count = 40;
        ExecutorService executor = Executors.newFixedThreadPool(count);
        // hold every worker at the first lookup so lazy initialization is concurrent
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        Set<Object> clients = ConcurrentHashMap.newKeySet();
        List<Future<?>> futures = new ArrayList<>(count);
        try {
            for (int i = 0; i < count; i++) {
                int request = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    clients.add(HttpClientTestAccess.client(connectionFactory.apply(request)));
                    return null;
                }));
            }
            ready.await();
            start.countDown();
            for (Future<?> future : futures) future.get();
            return clients;
        } finally {
            executor.shutdownNow();
        }
    }
}
