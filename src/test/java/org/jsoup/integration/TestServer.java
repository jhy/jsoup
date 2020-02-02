package org.jsoup.integration;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.jsoup.integration.servlets.BaseServlet;

import java.util.concurrent.atomic.AtomicInteger;

public class TestServer {
    private static final Server jetty = new Server(0);
    private static final ServletHandler handler = new ServletHandler();
    private static AtomicInteger latch = new AtomicInteger(0);

    static {
        jetty.setHandler(handler);
    }

    private TestServer() {
    }

    public static void start() {
        synchronized (jetty) {
            int count = latch.getAndIncrement();
            if (count == 0) {
                try {
                    jetty.start();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public static void stop() {
        synchronized (jetty) {
            int count = latch.decrementAndGet();
            if (count == 0) {
                try {
                    jetty.stop();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
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
