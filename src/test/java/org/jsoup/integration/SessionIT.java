package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.integration.servlets.FileServlet;
import org.jsoup.integration.servlets.SlowRider;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Integration tests to test longer running Connection */
public class SessionIT {
    @BeforeAll
    public static void setUp() {
        TestServer.start();
    }

    @Test
    public void multiThread() throws InterruptedException {
        int numThreads = 20;
        int numThreadLoops = 5;
        String[] urls = {
            FileServlet.urlTo("/htmltests/medium.html"),
            FileServlet.urlTo("/htmltests/upload-form.html"),
            FileServlet.urlTo("/htmltests/comments.html"),
            FileServlet.urlTo("/htmltests/large.html"),
        };
        String[] titles = {
            "Medium HTML",
            "Upload Form Test",
            "A Certain Kind of Test",
            "Large HTML"
        };
        ThreadCatcher catcher = new ThreadCatcher();

        Connection session = Jsoup.newSession();

        Thread[] threads = new Thread[numThreads];
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            Thread thread = new Thread(() -> {
                for (int loop = 0; loop < numThreadLoops; loop++) {
                    for (int i = 0; i < urls.length; i++) {
                        try {
                            Document doc = session.newRequest().url(urls[i]).get();
                            assertEquals(titles[i], doc.title());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }
            });
            thread.setName("Runner-" + threadNum);
            thread.start();
            thread.setUncaughtExceptionHandler(catcher);
            threads[threadNum] = thread;
        }

        // now join them all
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(0, catcher.exceptionCount.get());
    }

    // test that we throw a nice clear exception if you try to multi-thread by forget .newRequest()
    @Test
    public void multiThreadWithoutNewRequestBlowsUp() throws InterruptedException {
        int numThreads = 20;
        String url = SlowRider.Url + "?" + SlowRider.MaxTimeParam + "=10000"; // this makes sure that the first req is still executing whilst the others run
        String title = "Slow Rider";

        ThreadCatcher catcher = new ThreadCatcher();
        Connection session = Jsoup.newSession();

        Thread[] threads = new Thread[numThreads];
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            Thread thread = new Thread(() -> {
                try {
                    Document doc = session.url(url).get();
                    assertEquals(title, doc.title());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            thread.setName("Runner-" + threadNum);
            thread.start();
            thread.setUncaughtExceptionHandler(catcher);
            threads[threadNum] = thread;
        }

        // now join them all
        for (Thread thread : threads) {
            thread.join();
        }

        // only one should have passed, rest should have blown up (assuming the started whilst other was running)
        assertEquals(numThreads - 1, catcher.multiThreadExceptions.get());
        assertEquals(numThreads - 1, catcher.exceptionCount.get());
    }


    static class ThreadCatcher implements Thread.UncaughtExceptionHandler {
        AtomicInteger exceptionCount = new AtomicInteger();
        AtomicInteger multiThreadExceptions = new AtomicInteger();

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (e instanceof IllegalArgumentException && e.getMessage().contains("Multiple threads"))
                multiThreadExceptions.incrementAndGet();
            else
                e.printStackTrace();
            exceptionCount.incrementAndGet();
        }
    }

}
