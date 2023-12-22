package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectorIT {

    @Test
    public void multiThreadHas() throws InterruptedException {
        final String html = "<div id=1></div><div id=2><p>One</p><p>Two</p>";
        final Evaluator eval = QueryParser.parse("div:has(p)");

        int numThreads = 20;
        int numThreadLoops = 5;

        SelectorIT.ThreadCatcher catcher = new SelectorIT.ThreadCatcher();

        Thread[] threads = new Thread[numThreads];
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            Thread thread = new Thread(() -> {
                Document doc = Jsoup.parse(html);
                for (int loop = 0; loop < numThreadLoops; loop++) {
                    Elements els = doc.select(eval);
                    assertEquals(1, els.size());
                    assertEquals("2", els.get(0).id());
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

    static class ThreadCatcher implements Thread.UncaughtExceptionHandler {
        AtomicInteger exceptionCount = new AtomicInteger();

        @Override
        public void uncaughtException(Thread t, Throwable e) {

            e.printStackTrace();
            exceptionCount.incrementAndGet();
        }
    }

}
