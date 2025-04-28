package org.jsoup.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Longer running Parser tests.
 */

public class ParserIT {
    @Test
    @Disabled // disabled by default now, as there more specific unconsume tests
    public void testIssue1251() {
        // https://github.com/jhy/jsoup/issues/1251
        StringBuilder str = new StringBuilder("<a href=\"\"ca");
        for (int countSpaces = 0; countSpaces < 100000; countSpaces++) {
            try {
                Parser.htmlParser().setTrackErrors(1).parseInput(str.toString(), "");
            } catch (Exception e) {
                throw new AssertionError("failed at length " + str.length(), e);
            }
            str.insert(countSpaces, ' ');
        }
    }

    @Test
    public void handlesDeepStack() {
        // inspired by http://sv.stargate.wikia.com/wiki/M2J and https://github.com/jhy/jsoup/issues/955
        // I didn't put it in the integration tests, because explorer and intellij kept dieing trying to preview/index it

        // Arrange
        StringBuilder longBody = new StringBuilder(500000);
        for (int i = 0; i < 25000; i++) {
            longBody.append(i).append("<dl><dd>");
        }
        for (int i = 0; i < 25000; i++) {
            longBody.append(i).append("</dd></dl>");
        }

        // Act
        long start = System.currentTimeMillis();
        Document doc = Parser.parseBodyFragment(longBody.toString(), "");

        // Assert
        assertEquals(2, doc.body().childNodeSize());
        assertEquals(25000, doc.select("dd").size());
        assertTrue(System.currentTimeMillis() - start < 20000); // I get ~ 1.5 seconds, but others have reported slower
        // was originally much longer, or stack overflow.
    }

    @Test void parserIsThreadSafe() throws InterruptedException {
        // tests that a single parser can be called by multiple threads and won't blow up
        // without the lock, will see many exceptions in parse, and non-equal docs
        String html = "<div id=1><div id=2><div id=3>Text.</div></div></div>";
        Parser parser = Parser.htmlParser();
        Document expectDoc = parser.parseInput(html, "");

        int numThreads = 10;
        int numLoops = 20;
        List<Thread> threads = new ArrayList<>(numThreads);
        List<Document> toCheck = new ArrayList<>(numThreads * numLoops);
        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < numLoops; j++) {
                    Document doc = parser.parseInput(html, "");
                    toCheck.add(doc);
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (Document doc : toCheck) {
            assertTrue(doc.hasSameValue(expectDoc));
        }
    }

    @Test void parserIsThreadSafeWithCloneAndAppend() throws InterruptedException {
        // tests that a single parser can be called by multiple threads via Element.clone().append()
        String html = "<div id=1><div id=2><div id=3></div></div></div>";
        String append = "<div id=4>Text.</div>";
        Parser parser = Parser.htmlParser();
        Document baseDoc = parser.parseInput(html, "");
        Element baseElement = baseDoc.expectFirst("#3");

        int numThreads = 10;
        int numLoops = 20;
        List<Thread> threads = new ArrayList<>(numThreads);
        List<Element> toCheck = new ArrayList<>(numThreads * numLoops);
        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < numLoops; j++) {
                    Element cloned = baseElement.clone();
                    cloned.append(append); // invokes the parser internally - parseFragment
                    toCheck.add(cloned);
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        baseElement.append(append);
        for (Element element : toCheck) {
            assertTrue(element.hasSameValue(baseElement));
        }
    }
}
