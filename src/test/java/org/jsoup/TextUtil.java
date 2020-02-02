package org.jsoup;

import java.util.regex.Pattern;

/**
 Text utils to ease testing

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextUtil {
    static Pattern stripper = Pattern.compile("\\r?\\n\\s*");

    public static String stripNewlines(String text) {
        return stripper.matcher(text).replaceAll("");
    }
}
