package org.jsoup.integration.servlets;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jsoup.integration.TestServer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyServlet extends AsyncProxyServlet {
    public static TestServer.ProxySettings ProxySettings = TestServer.proxySettings();
    public static String Via = "1.1 jsoup test proxy";

    public static Handler createHandler() {
        // ConnectHandler wraps this ProxyServlet and handles CONNECT, which sets up a tunnel for HTTPS requests and is
        // opaque to the proxy. The ProxyServlet handles simple HTTP requests.
        ConnectHandler connectHandler = new ConnectHandler();
        ServletHandler proxyHandler = new ServletHandler();
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
}
