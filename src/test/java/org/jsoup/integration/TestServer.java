package org.jsoup.integration;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.jsoup.integration.servlets.BaseServlet;
import org.jsoup.integration.servlets.ProxyServlet;

import java.net.InetSocketAddress;

public class TestServer {
    private static final String localhost = "localhost";
    private static final Server jetty = newServer();
    private static final ServletHandler handler = new ServletHandler();
    static int port;

    private static final Server proxy = newServer();
    private static final ServletHandler proxyHandler = new ServletHandler();
    private static final ProxySettings proxySettings = new ProxySettings();

    private static Server newServer() {
        return new Server(new InetSocketAddress(localhost, 0));
    }

    static {
        jetty.setHandler(handler);
        proxy.setHandler(proxyHandler);
        proxyHandler.addServletWithMapping(ProxyServlet.class, "/*");
    }

    private TestServer() {
    }

    public static void start() {
        synchronized (jetty) {
            if (jetty.isStarted()) return;

            try {
                jetty.start(); // jetty will safely no-op a start on an already running instance
                port = ((ServerConnector) jetty.getConnectors()[0]).getLocalPort();

                proxy.start();
                proxySettings.port = ((ServerConnector) proxy.getConnectors()[0]).getLocalPort();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static String map(Class<? extends BaseServlet> servletClass) {
        synchronized (jetty) {
            if (!jetty.isStarted())
                start(); // if running out of the test cases

            String path = "/" + servletClass.getSimpleName();
            handler.addServletWithMapping(servletClass, path + "/*");
            return "http://" + localhost + ":" + port + path;
        }
    }

    public static ProxySettings proxySettings(Class<? extends BaseServlet> servletClass) {
        synchronized (jetty) {
            if (!jetty.isStarted())
                start(); // if running out of the test cases

            return proxySettings;
        }
    }

    //public static String proxy
    public static class ProxySettings {
        final String hostname = localhost;
        int port;
    }
}
