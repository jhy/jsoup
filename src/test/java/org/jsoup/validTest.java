package org.jsoup;


import org.jsoup.safety.Whitelist;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

/**
 Tests for the cleaner.

 @author Jonathan Hedley, jonathan@hedley.net */
public class validTest {
    String baseurl="http://www.baidu.com/";
    @Test public void simpleBaseUrlTest1() {
        String HTML="<a href='http://www.baidu.com/xxx'> baidu</a>";

        boolean isValid = Jsoup.isValid(HTML, baseurl,Whitelist.basic());

        assertTrue(isValid);
    }

    @Test public void simpleBaseUrlTest2() {

        String HTML="<a href='/xxx'> baidu</a>";
        boolean isValid = Jsoup.isValid(HTML, baseurl,Whitelist.basic());

        assertTrue(isValid);
    }
    @Test public void simpleBaseUrlTest3() {
        String HTML="<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <a href=\"/xxx\"> baidu </a>\n" +
                " </body>\n" +
                "</html>";

        boolean isValid = Jsoup.isValid(HTML, baseurl,Whitelist.basic());

        assertTrue(isValid);
    }

    @Test public void mutilpRelativateAddressTest() {
        String HTML="<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <a href=\"/xxx\"> baidu </a>\n" +
                "  <a href=\"/yyy\"> baidu </a>\n" +
                " </body>\n" +
                "</html>";

        boolean isValid = Jsoup.isValid(HTML, baseurl,Whitelist.basic());

        assertTrue(isValid);
    }


}
