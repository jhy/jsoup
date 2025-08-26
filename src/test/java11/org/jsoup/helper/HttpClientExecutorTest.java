package org.jsoup.helper;
import org.jsoup.internal.SharedConstants;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        
        try {
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return Collections.singletonList(
                        new Proxy(Proxy.Type.HTTP, new InetSocketAddress("system.proxy", 8080))
                    );
                }
                
                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {}
            });

            HttpClientExecutor.ProxyWrap wrap = new HttpClientExecutor.ProxyWrap();
            List<Proxy> proxies = wrap.select(URI.create("http://example.com"));
            
            assertEquals(1, proxies.size());
            assertEquals("system.proxy", ((InetSocketAddress) proxies.get(0).address()).getHostName());
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
}
