package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.parser.StreamParser;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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

    @Test public void streamParserSelect() throws Exception {
        // https://github.com/jhy/jsoup/issues/2277
        // The memo in the StructuralEvaluator was not getting reset correctly, and so would run out of memory
        // Test tracks memory consumption. Will be interesting to see how it behaves on the CI workers.

        String xml = "<A><B><C>1";
        Evaluator query = QueryParser.parse("A B C");
        Runtime runtime = Runtime.getRuntime();

        System.gc();
        Thread.sleep(100);
        long initialUsed = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < 50_000; i++) { // Before fix, would exceed 10MB in ~ 9000 iters
            try (StreamParser parser = new StreamParser(Parser.xmlParser())) {
                parser.parse(xml, "");
                parser.selectFirst(query);
                parser.stop();
            }

            if (i % 1000 == 0) {
                System.gc();
                Thread.sleep(100);
                long currentUsed = runtime.totalMemory() - runtime.freeMemory();
                long delta = currentUsed - initialUsed;

                // Fail if we grow + 10MB
                if (delta > 10_000_000) {
                    fail(String.format("Memo leak detected. Memory increased by %,d bytes after %,d iterations",
                        delta, i));
                }
            }
        }
    }

}
