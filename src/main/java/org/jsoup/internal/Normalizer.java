package org.jsoup.internal;

import java.util.Locale;

public class Normalizer {
    public static String lowerCase(String input) {
        return input.toLowerCase(Locale.ENGLISH);
    }

    public static String normalize(String input) {
        return lowerCase(input).trim();
    }
}
