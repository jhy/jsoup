package org.jsoup;

import java.util.regex.Pattern;

/**
 Text utils to ease testing

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextUtil {
    static Pattern stripper = Pattern.compile("\\r?\\n\\s*");
    static Pattern stripLines = Pattern.compile("\\r?\\n?");
    static Pattern spaceCollapse = Pattern.compile("\\s{2,}");
    static Pattern tagSpaceCollapse = Pattern.compile(">\\s+<");
    static Pattern stripCRs = Pattern.compile("\\r*");

    public static String stripNewlines(String text) {
        return stripper.matcher(text).replaceAll("");
    }

    public static String normalizeSpaces(String text) {
        text = stripLines.matcher(text).replaceAll("");
        text = stripper.matcher(text).replaceAll("");
        text = spaceCollapse.matcher(text).replaceAll(" ");
        text = tagSpaceCollapse.matcher(text).replaceAll("><");
        return text;
    }

    public static String stripCRs(String text) {
        return stripCRs.matcher(text).replaceAll("");
    }
}
