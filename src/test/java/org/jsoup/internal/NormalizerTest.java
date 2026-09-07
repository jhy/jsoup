package org.jsoup.internal;

import org.jsoup.MultiLocaleExtension.MultiLocaleTest;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class NormalizerTest {
    @MultiLocaleTest void separatesAsciiAndUnicodeCase(Locale locale) {
        Locale.setDefault(locale);
        String input = "AZ ÄKİıſ😀\u0001 ";
        assertEquals("az ÄKİıſ😀\u0001 ", Normalizer.asciiLowerCase(input));
        assertEquals("az äki\u0307ıſ😀\u0001 ", Normalizer.lowerCase(input));
        assertEquals("", Normalizer.asciiLowerCase(null));
        assertEquals("", Normalizer.lowerCase(null));
        assertTrue(Normalizer.equalsIgnoreAsciiCase("TeXT/HTML", "text/html"));
        for (String[] pair : new String[][]{{"K", "k"}, {"İ", "i"}, {"ı", "i"}, {"ſ", "s"}, {"Ä", "ä"}})
            assertFalse(Normalizer.equalsIgnoreAsciiCase(pair[0], pair[1]));
        assertTrue(Normalizer.equalsIgnoreAsciiCase("Ä😀", "Ä😀"));
        assertFalse(Normalizer.equalsIgnoreAsciiCase("Ä", "ä"));
        assertFalse(Normalizer.equalsIgnoreAsciiCase("ſ", "s"));
        assertFalse(Normalizer.equalsIgnoreAsciiCase("x", null));
        assertFalse(Normalizer.equalsIgnoreAsciiCase("x", " x"));
    }

    @Test void trimsOnlyAsciiWhitespace() {
        String name = "\u0000\u0001\u000b\u00a0name\u0001";
        assertEquals(name, StringUtil.trimAsciiWhitespace(" \t\n\f\r" + name + "\r\f\n\t "));
        assertEquals("", StringUtil.trimAsciiWhitespace(" \t\n\f\r"));
        assertEquals("", StringUtil.trimAsciiWhitespace(""));
    }
}
