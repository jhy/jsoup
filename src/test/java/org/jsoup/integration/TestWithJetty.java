package org.jsoup.integration;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.FailOnGetWContentTypeServlet;
import org.jsoup.integration.servlets.RedirOnPostToFailOnGetWContentTypeServlet;
import org.jsoup.nodes.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestWithJetty {
    private static Server jetty;
    private static int port;

    @BeforeClass
    public static void setUp() throws Exception {
        jetty = new Server(0);
        ServletHandler servletHandler = new ServletHandler();
        jetty.addHandler(servletHandler);

        servletHandler.addServletWithMapping(FailOnGetWContentTypeServlet.class, "/FailOnGetWContentTypeServlet");
        servletHandler.addServletWithMapping(RedirOnPostToFailOnGetWContentTypeServlet.class, "/RedirOnPostToFailOnGetWContentTypeServlet");

        jetty.start();
        port = jetty.getConnectors()[0].getLocalPort();
    }

    @Test
    public void failOnGetWContentType() throws IOException {
        try {
            Jsoup.connect("http://localhost:" + port + "/FailOnGetWContentTypeServlet")
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8")
                    .get();
        } catch (HttpStatusException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void redirectOnPostToFailOnGetWContentTypeServlet() throws IOException {
        Document doc = Jsoup.connect("http://localhost:" + port + "/RedirOnPostToFailOnGetWContentTypeServlet")
                .data("somekey", "somevalue")
                .post();
        assertTrue(doc.select("h1").text().equals("Hello from FailOnGetWContentTypeServlet"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        jetty.stop();
    }
}
