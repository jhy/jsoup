package org.jsoup.parser;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
}
