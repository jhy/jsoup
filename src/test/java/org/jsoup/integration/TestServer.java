package org.jsoup.integration;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.jsoup.integration.servlets.BaseServlet;

import java.net.InetSocketAddress;

public class TestServer {
    private static final Server jetty = new Server(new InetSocketAddress("localhost", 0));
    private static final ServletHandler handler = new ServletHandler();

    static {
        jetty.setHandler(handler);
    }

    private TestServer() {
    }

    public static void start() {
        synchronized (jetty) {
            try {
                jetty.start(); // jetty will safely no-op a start on an already running instance
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
            int port = ((ServerConnector) jetty.getConnectors()[0]).getLocalPort();
            return "http://localhost:" + port + path;
        }
    }
}
