package org.jsoup.integration;

import org.jsoup.integration.netty.NettyProxyServer;
import org.jsoup.integration.netty.NettyTestServer;
import org.jsoup.integration.netty.RouteHandler;
import org.jsoup.integration.routes.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestServer {
    private static final String Localhost = "localhost";
    private static final Object Lock = new Object();
    public static final String ProxyVia = "1.1 jsoup test proxy";
    private static final ProxySettings ProxySettings = new ProxySettings();
    private static final Origin Origin = new Origin();

    static {
        TestTls.setupDefaultTrust();
    }

    private TestServer() {
    }

    /**
     The public origin endpoint facade used by tests and helper wrappers
     */
    public static Origin origin() {
        return Origin;
    }

    /**
     Starts the Netty origin and proxy harnesses
     */
    public static void start() {
        synchronized (Lock) {
            if (NettyTestServer.isStarted() && NettyProxyServer.hasStarted()) return;

            try {
                NettyTestServer.start(origin().endpoints());
                NettyProxyServer.start();
                ProxySettings.port = NettyProxyServer.port();
                ProxySettings.authedPort = NettyProxyServer.authedPort();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     Close any current connections to the authed proxy. Tunneled connections only authenticate in their first
     CONNECT, and may be kept alive and reused. So when we want to test unauthed - authed flows, we need to disconnect
     them first.
     */
    static int closeAuthedProxyConnections() {
        return NettyProxyServer.closeAuthedConnections();
    }

    public static ProxySettings proxySettings() {
        synchronized (Lock) {
            if (!NettyTestServer.isStarted() || !NettyProxyServer.hasStarted())
                start();

            return ProxySettings;
        }
    }
    public static class ProxySettings {
        final String hostname = Localhost;
        int port;
        int authedPort;
    }

    /**
     Origin endpoints used by the integration tests
     */
    public static final class Origin {
        public final Endpoint hello = new Endpoint("/Hello", HelloRoute::handle);
        public final Endpoint echo = new Endpoint("/Echo", EchoRoute::handle);
        public final Endpoint file = new Endpoint("/File", FileRoute::handle);
        public final Endpoint redirect = new Endpoint("/Redirect", RedirectRoute::handle);
        public final Endpoint cookie = new Endpoint("/Cookie", CookieRoute::handle);
        public final Endpoint deflate = new Endpoint("/Deflate", DeflateRoute::handle);
        public final Endpoint interrupted = new Endpoint("/Interrupted", InterruptedRoute::handle);
        public final Endpoint slowRider = new Endpoint("/SlowRider", SlowRider::handle);
        private final List<Endpoint> endpoints = Collections.unmodifiableList(Arrays.asList(
            hello, echo, file, redirect, cookie, deflate, interrupted, slowRider));

        private Origin() {
        }

        Iterable<Endpoint> endpoints() {
            return endpoints;
        }
    }

    public static final class Endpoint {
        private final String path;
        private final RouteHandler handler;

        private Endpoint(String path, RouteHandler handler) {
            this.path = path;
            this.handler = handler;
        }

        public String path() {
            return path;
        }

        public RouteHandler handler() {
            return handler;
        }

        public String url() {
            start();
            return NettyTestServer.url(path);
        }

        public String tlsUrl() {
            start();
            return NettyTestServer.tlsUrl(path);
        }

        public String url(String suffix) {
            return url() + suffix;
        }

        public String tlsUrl(String suffix) {
            return tlsUrl() + suffix;
        }
    }
}
