package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ParserSettingsTest {
    @Test
    public void caseSupport() {
        ParseSettings bothOn = new ParseSettings(true, true);
        ParseSettings bothOff = new ParseSettings(false, false);
        ParseSettings tagOn = new ParseSettings(true, false);
        ParseSettings attrOn = new ParseSettings(false, true);

        assertEquals("FOO", bothOn.normalizeTag("FOO"));
        assertEquals("FOO", bothOn.normalizeAttribute("FOO"));

        assertEquals("foo", bothOff.normalizeTag("FOO"));
        assertEquals("foo", bothOff.normalizeAttribute("FOO"));

        assertEquals("FOO", tagOn.normalizeTag("FOO"));
        assertEquals("foo", tagOn.normalizeAttribute("FOO"));

        assertEquals("foo", attrOn.normalizeTag("FOO"));
        assertEquals("FOO", attrOn.normalizeAttribute("FOO"));

    }
}
