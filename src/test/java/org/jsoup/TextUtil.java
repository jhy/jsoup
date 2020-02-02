package org.jsoup;

import java.util.regex.Pattern;

/**
 Text utils to ease testing

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextUtil {
    static Pattern stripper = Pattern.compile("\\r?\\n\\s*");
    static Pattern stripCRs = Pattern.compile("\\r*");

    public static String stripNewlines(String text) {
        return stripper.matcher(text).replaceAll("");
    }

    public static String stripCRs(String text) {
        return stripCRs.matcher(text).replaceAll("");
    }
}
