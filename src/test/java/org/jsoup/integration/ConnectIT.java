package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.FileServlet;
import org.jsoup.integration.servlets.SlowRider;
import org.jsoup.internal.ConstrainableInputStream;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Failsafe integration tests for Connect methods. These take a bit longer to run, so included as Integ, not Unit, tests.
 */
public class ConnectIT {
    // Slow Rider tests.
    @Test
    public void canInterruptBodyStringRead() throws InterruptedException {
        // todo - implement in interruptable channels, so it's immediate
        final String[] body = new String[1];
        Thread runner = new Thread(() -> {
            try {
                Connection.Response res = Jsoup.connect(SlowRider.Url)
                    .timeout(15 * 1000)
                    .execute();
                body[0] = res.body();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

        runner.start();
        Thread.sleep(1000 * 3);
        runner.interrupt();
        assertTrue(runner.isInterrupted());
        runner.join();

        assertTrue(body[0].length() > 0);
        assertTrue(body[0].contains("<p>Are you still there?"));
    }

    @Test
    public void canInterruptDocumentRead() throws InterruptedException {
        // todo - implement in interruptable channels, so it's immediate
        final String[] body = new String[1];
        Thread runner = new Thread(() -> {
            try {
                Connection.Response res = Jsoup.connect(SlowRider.Url)
                    .timeout(15 * 1000)
                    .execute();
                body[0] = res.parse().text();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

        runner.start();
        Thread.sleep(1000 * 3);
        runner.interrupt();
        assertTrue(runner.isInterrupted());
        runner.join();

        assertEquals(0, body[0].length()); // doesn't ready a failed doc
    }

    @Test
    public void totalTimeout() throws IOException {
        int timeout = 3 * 1000;
        long start = System.currentTimeMillis();
        boolean threw = false;
        try {
            Jsoup.connect(SlowRider.Url).timeout(timeout).get();
        } catch (SocketTimeoutException e) {
            long end = System.currentTimeMillis();
            long took = end - start;
            assertTrue(took > timeout, ("Time taken was " + took));
            assertTrue(took < timeout * 1.8, ("Time taken was " + took));
            threw = true;
        }

        assertTrue(threw);
    }

    @Test
    public void slowReadOk() throws IOException {
        // make sure that a slow read that is under the request timeout is still OK
        Document doc = Jsoup.connect(SlowRider.Url)
            .data(SlowRider.MaxTimeParam, "2000") // the request completes in 2 seconds
            .get();

        Element h1 = doc.selectFirst("h1");
        assertEquals("outatime", h1.text());
    }

    @Test
    public void infiniteReadSupported() throws IOException {
        Document doc = Jsoup.connect(SlowRider.Url)
            .timeout(0)
            .data(SlowRider.MaxTimeParam, "2000")
            .get();

        Element h1 = doc.selectFirst("h1");
        assertEquals("outatime", h1.text());
    }

    @Test
    public void remainingAfterFirstRead() throws IOException {
        int bufferSize = 5 * 1024;
        int capSize = 100 * 1024;

        String url = FileServlet.urlTo("/htmltests/large.html"); // 280 K
        ConstrainableInputStream stream;
        try (BufferedInputStream inputStream = Jsoup.connect(url).maxBodySize(capSize)
            .execute().bodyStream()) {

            assertTrue(inputStream instanceof ConstrainableInputStream);
            stream = (ConstrainableInputStream) inputStream;

            // simulates parse which does a limited read first
            stream.mark(bufferSize);
            ByteBuffer firstBytes = stream.readToByteBuffer(bufferSize);

            byte[] array = firstBytes.array();
            String firstText = new String(array, StandardCharsets.UTF_8);
            assertTrue(firstText.startsWith("<html><head><title>Large"));
            assertEquals(bufferSize, array.length);

            boolean fullyRead = stream.read() == -1;
            assertFalse(fullyRead);

            // reset and read again
            stream.reset();
            ByteBuffer fullRead = stream.readToByteBuffer(0);
            byte[] fullArray = fullRead.array();
            assertEquals(capSize, fullArray.length);
            String fullText = new String(fullArray, StandardCharsets.UTF_8);
            assertTrue(fullText.startsWith(firstText));
        }
    }

    @Test
    public void noLimitAfterFirstRead() throws IOException {
        int bufferSize = 5 * 1024;

        String url = FileServlet.urlTo("/htmltests/large.html"); // 280 K
        ConstrainableInputStream stream;
        try (BufferedInputStream inputStream = Jsoup.connect(url).execute().bodyStream()) {
            assertTrue(inputStream instanceof ConstrainableInputStream);
            stream = (ConstrainableInputStream) inputStream;

            // simulates parse which does a limited read first
            stream.mark(bufferSize);
            ByteBuffer firstBytes = stream.readToByteBuffer(bufferSize);
            byte[] array = firstBytes.array();
            String firstText = new String(array, StandardCharsets.UTF_8);
            assertTrue(firstText.startsWith("<html><head><title>Large"));
            assertEquals(bufferSize, array.length);

            // reset and read fully
            stream.reset();
            ByteBuffer fullRead = stream.readToByteBuffer(0);
            byte[] fullArray = fullRead.array();
            assertEquals(280735, fullArray.length);
            String fullText = new String(fullArray, StandardCharsets.UTF_8);
            assertTrue(fullText.startsWith(firstText));
        }
    }
}
