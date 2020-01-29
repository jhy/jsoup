package org.jsoup.parser;

import org.junit.Test;

/**
 * Longer running Parser tests.
 */

public class ParserIT {
    @Test
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
}
