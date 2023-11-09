package org.jsoup.integration.servlets;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jsoup.integration.TestServer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.jsoup.integration.servlets.AuthFilter.ProxyRealm;

public class ProxyServlet extends AsyncProxyServlet {
    public static TestServer.ProxySettings ProxySettings = TestServer.proxySettings();
    public static String Via = "1.1 jsoup test proxy";

    static {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        // removes Basic, which is otherwise excluded from auth for CONNECT tunnels
    }

    public static Handler createHandler(boolean alwaysAuth) {
        // ConnectHandler wraps this ProxyServlet and handles CONNECT, which sets up a tunnel for HTTPS requests and is
        // opaque to the proxy. The ProxyServlet handles simple HTTP requests.
        AuthFilter authFilter = new AuthFilter(alwaysAuth, true);
        ConnectHandler connectHandler = new ConnectProxy(authFilter);
        ServletHandler proxyHandler = new ServletHandler();
        proxyHandler.addFilterWithMapping(new FilterHolder(authFilter), "/*", FilterMapping.ALL); // auth for HTTP proxy
        ServletHolder proxyServletHolder = new ServletHolder(ProxyServlet.class); // Holder wraps as it requires maxThreads initialization
        proxyServletHolder.setAsyncSupported(true);
        proxyServletHolder.setInitParameter("maxThreads", "8");
        proxyHandler.addServletWithMapping(proxyServletHolder, "/*");
        connectHandler.setHandler(proxyHandler);

        return connectHandler;
    }

    @Override
    protected void onServerResponseHeaders(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onServerResponseHeaders(clientRequest, proxyResponse, serverResponse);
        proxyResponse.addHeader("Via", Via);
    }

    /** Supports CONNECT tunnels */
    static class ConnectProxy extends ConnectHandler {
        final AuthFilter authFilter;

        public ConnectProxy(AuthFilter authFilter) {
            this.authFilter = authFilter;
        }

        @Override
        protected boolean handleAuthentication(HttpServletRequest req, HttpServletResponse res, String address) {
            boolean accessGranted = authFilter.checkAuth(req);
            //System.err.println("CONNECT AUTH: " + accessGranted);

            // need to add the desired auth header if not granted. Returning false here will also send 407 header
            if (!accessGranted) {
                res.setHeader("Proxy-Authenticate", "Basic realm=\"" + ProxyRealm + "\"");
            }
            return accessGranted;
        }
    }
}
