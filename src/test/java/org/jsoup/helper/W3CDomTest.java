package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.integration.ParseTest;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;

import static org.jsoup.TextUtil.LE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class W3CDomTest {
    @Test
    public void simpleConversion() {
        String html = "<html><head><title>W3c</title></head><body><p class='one' id=12>Text</p><!-- comment --><invalid>What<script>alert('!')";
        org.jsoup.nodes.Document doc = Jsoup.parse(html);

        W3CDom w3c = new W3CDom();
        Document wDoc = w3c.fromJsoup(doc);
        String out = w3c.asString(wDoc);
        assertEquals(
                "<html>" + LE +
                        "<head>" + LE +
                        "<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" + LE +
                        "<title>W3c</title>" + LE +
                        "</head>" + LE +
                        "<body>" + LE +
                        "<p class=\"one\" id=\"12\">Text</p>" + LE +
                        "<!-- comment -->" + LE +
                        "<invalid>What<script>alert('!')</script>" + LE +
                        "</invalid>" + LE +
                        "</body>" + LE +
                        "</html>" + LE
                , out);
    }

    @Test
    public void convertsGoogle() throws IOException {
        File in = ParseTest.getFile("/htmltests/google-ipod.html");
        org.jsoup.nodes.Document doc = Jsoup.parse(in, "UTF8");

        W3CDom w3c = new W3CDom();
        Document wDoc = w3c.fromJsoup(doc);
        String out = w3c.asString(wDoc);
        assertTrue(out.contains("ipod"));
    }
}

