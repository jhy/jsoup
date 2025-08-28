package org.jsoup.helper;
import org.jsoup.internal.SharedConstants;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HttpClientExecutorTest {
    @Test void getsHttpClient() {
        try {
            enableHttpClient();
            RequestExecutor executor = RequestDispatch.get(new HttpConnection.Request(), null);
            assertInstanceOf(HttpClientExecutor.class, executor);
        } finally {
            disableHttpClient(); // reset to previous default for JDK8 compat tests
        }
    }

    @Test void getsHttpUrlConnectionByDefault() {
        System.clearProperty(SharedConstants.UseHttpClient);
        RequestExecutor executor = RequestDispatch.get(new HttpConnection.Request(), null);
        assertInstanceOf(HttpClientExecutor.class, executor);
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

            HttpClientExecutor.ProxyWrap wrap = new HttpClientExecutor.ProxyWrap();
            List<Proxy> proxies = wrap.select(URI.create("http://example.com"));
            
            assertEquals(1, proxies.size());
            assertSame(defaultProxy, proxies.get(0).address());
        } finally {
            ProxySelector.setDefault(originalSelector);
        }
    }

    @Test void proxyWrapConnectFailedOnlyForSystemProxy() {
        HttpClientExecutor.ProxyWrap wrap = new HttpClientExecutor.ProxyWrap();
        HttpClientExecutor.perRequestProxy.set(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("custom", 9090)));
        wrap.connectFailed(URI.create("http://example.com"), 
            new InetSocketAddress("custom", 9090), 
            new IOException("test"));
        HttpClientExecutor.perRequestProxy.remove();
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

            HttpClientExecutor.perRequestProxy.set(
                new Proxy(Proxy.Type.HTTP, perReqProxy));

            HttpClientExecutor.ProxyWrap wrap = new HttpClientExecutor.ProxyWrap();
            List<Proxy> proxies = wrap.select(URI.create("http://example.com"));
            assertSame(perReqProxy, proxies.get(0).address());
        } finally {
            HttpClientExecutor.perRequestProxy.remove();
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
            new HttpClientExecutor.ProxyWrap()
                .connectFailed(URI.create("http://example.com"), new InetSocketAddress("x", 80), new IOException("x"));
            assertTrue(called[0]);
        } finally {
            ProxySelector.setDefault(original);
        }
    }
}
