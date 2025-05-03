package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.FileServlet;
import org.jsoup.integration.servlets.SlowRider;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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
        int numThreads = 5;
        String url = SlowRider.Url + "?" + SlowRider.MaxTimeParam + "=20000"; // this makes sure that the first req is still executing whilst the others run
        String title = "Slow Rider";

        ThreadCatcher catcher = new ThreadCatcher();
        Connection session = Jsoup.newSession();

        // run first slow request
        AtomicInteger successful = new AtomicInteger();
        Thread slow = new Thread(() -> {
            try {
                Document doc = session.url(url).get();
                assertNotNull(doc);
            } catch (IOException e) {
                if (!isInterruptedException(e))
                    throw new UncheckedIOException(e);
            }
        });
        slow.start();
        Thread.sleep(100); // yield so that thread can start before the next do

        // spawn others, should fail
        Thread[] threads = new Thread[numThreads];
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            Thread thread = new Thread(() -> {
                try {
                    Document doc = session.url(url).get();
                    assertEquals(title, doc.title());
                    successful.getAndIncrement();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            thread.setName("Runner-" + threadNum);
            thread.setUncaughtExceptionHandler(catcher);
            thread.start();
            threads[threadNum] = thread;
        }

        // now join them all
        for (Thread thread : threads) {
            thread.join();
        }
        // cancel the slow runner so we can wrap up the test quicker
        slow.interrupt();

        // only one should have passed, rest should have blown up (assuming the started whilst other was running)
        //assertEquals(numThreads - 1, catcher.multiThreadExceptions.get());
        //assertEquals(numThreads - 1, catcher.exceptionCount.get());

        /* The checks above work when all 20 threads are executed within 10 seconds. However, depending on how cloudy it
         is when the CI jobs are run, they may not all complete in time. As of writing that appears most commonly on the
         macOS runners, which appear overtaxed. That makes this test flaky. So we relax the test conditions, and make
         sure at least just one passed and one failed. That's OK in prod as well, because we are only concerned about
         concurrent execution, which the impl does detect correctly. */
        assertEquals(0, successful.get());
        assertTrue(catcher.multiThreadExceptions.get() > 0);
        assertEquals(catcher.multiThreadExceptions.get(), catcher.exceptionCount.get()); // all exceptions are multi-threaded
    }

    @Test
    public void multiThreadWithProgressListener() throws InterruptedException {
        // tests that we can use one progress listener for multiple URLs and threads.
        int numThreads = 10;
        String[] urls = {
            FileServlet.urlTo("/htmltests/medium.html"),
            FileServlet.urlTo("/htmltests/upload-form.html"),
            FileServlet.urlTo("/htmltests/comments.html"),
            FileServlet.urlTo("/htmltests/large.html"),
        };
        Set<String> seenUrls = ConcurrentHashMap.newKeySet();
        AtomicInteger completedCount = new AtomicInteger(0);
        ThreadCatcher catcher = new ThreadCatcher();

        Connection session = Jsoup.newSession()
            .onResponseProgress((processed, total, percent, response) -> {
                if (percent == 100.0f) {
                    //System.out.println("Completed " + Thread.currentThread().getName() + "- " + response.url());
                    seenUrls.add(response.url().toExternalForm());
                    completedCount.incrementAndGet();
                }
            });

        Thread[] threads = new Thread[numThreads];
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            Thread thread = new Thread(() -> {
                for (String url : urls) {
                    try {
                        Connection con = session.newRequest().url(url);
                        con.get();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
            thread.setName("Runner-" + threadNum);
            thread.setUncaughtExceptionHandler(catcher);
            thread.start();
            threads[threadNum] = thread;
        }

        // now join them all
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(0, catcher.exceptionCount.get());
        assertEquals(urls.length, seenUrls.size());
        assertEquals(urls.length * numThreads, completedCount.get());
    }


    static class ThreadCatcher implements Thread.UncaughtExceptionHandler {
        AtomicInteger exceptionCount = new AtomicInteger();
        AtomicInteger multiThreadExceptions = new AtomicInteger();

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (e.getMessage() != null && e.getMessage().contains("Multiple threads")) {
                multiThreadExceptions.incrementAndGet();
            } else if (!isInterruptedException(e)) {
                e.printStackTrace();
            }
            exceptionCount.incrementAndGet();
        }
    }

    private static boolean isInterruptedException(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof InterruptedException) return true;
            cause = cause.getCause();
        }
        return false;
    }

}
