package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.HelloServlet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ConnectTest {

    @BeforeClass public static void setUp() throws Exception {
        TestServer.start();
    }

    @AfterClass public static void tearDown() throws Exception {
        TestServer.stop();
    }

    @Test public void canConnectToLocalServer() throws IOException {
        String url = HelloServlet.Url;
        Document doc = Jsoup.connect(url).get();
        Element p = doc.selectFirst("p");
        assertEquals("Hello, World!", p.text());
    }
}
