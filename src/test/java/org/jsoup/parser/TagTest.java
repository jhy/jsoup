package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.MultiLocaleExtension.MultiLocaleTest;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 Tag tests.
 @author Jonathan Hedley, jonathan@hedley.net */
public class TagTest {
    @Test public void isCaseSensitive() {
        Tag p1 = Tag.valueOf("P");
        Tag p2 = Tag.valueOf("p");
        assertNotEquals(p1, p2);
    }

    @MultiLocaleTest
    public void canBeInsensitive(Locale locale) {
        Locale.setDefault(locale);

        Tag script1 = Tag.valueOf("script", ParseSettings.htmlDefault);
        Tag script2 = Tag.valueOf("SCRIPT", ParseSettings.htmlDefault);
        assertSame(script1, script2);
    }

    @Test public void trims() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf(" p ");
        assertEquals(p1, p2);
    }

    @Test public void equality() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf("p");
        assertEquals(p1, p2);
        assertSame(p1, p2);
    }

    @Test public void divSemantics() {
        Tag div = Tag.valueOf("div");

        assertTrue(div.isBlock());
        assertTrue(div.formatAsBlock());
    }

    @Test public void pSemantics() {
        Tag p = Tag.valueOf("p");

        assertTrue(p.isBlock());
        assertFalse(p.formatAsBlock());
    }

    @Test public void imgSemantics() {
        Tag img = Tag.valueOf("img");
        assertTrue(img.isInline());
        assertTrue(img.isSelfClosing());
        assertFalse(img.isBlock());
    }

    @Test public void defaultSemantics() {
        Tag foo = Tag.valueOf("FOO"); // not defined
        Tag foo2 = Tag.valueOf("FOO");

        assertEquals(foo, foo2);
        assertTrue(foo.isInline());
        assertTrue(foo.formatAsBlock());
    }

    @Test public void valueOfChecksNotNull() {
        assertThrows(IllegalArgumentException.class, () -> Tag.valueOf(null));
    }

    @Test public void valueOfChecksNotEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Tag.valueOf(" "));
    }

    @Test public void knownTags() {
        assertTrue(Tag.isKnownTag("div"));
        assertFalse(Tag.isKnownTag("explain"));
    }

    @Test public void registerNewTag1() {
        String html = "<assessment-group id=\"2C93808974B5C5DA0174B73EB5712FBA\"> \n" +
                "            <label> 2. </label> \n" +
                "            <highlight show-answer=\"true\" id=\"2C93808974B5C5DA0174B73EB5712FBB\"> \n" +
                "                <question> \n" +
                "                    <p id=\"2C93808974B5C5DA0174B73EB5712FBC\">Underline the best word to complete each sentence below.</p> \n" +
                "                </question> \n" +
                "                <items> \n" +
                "                    <item id=\"2C93808974B5C5DA0174B73EB5712FBD\">He has <token type=\"underline\" color=\"any\">received</token> many gifts, <strong>but</strong> his wife <strong>has</strong> received <strong>no /</strong> <token type=\"underline\" color=\"any\">none</token></item> \n" +
                "                    <item id=\"2C93808974B5C5DA0174B73EB5712FBE\">The <strong>bird</strong> hasn't drunk <token type=\"circle\" color=\"any\"><strong>any</strong></token> <strong> / some</strong> water today.</item> \n" +
                "                </items> \n" +
                "            </highlight> \n" +
                "</assessment-group>";
        Document doc = Jsoup.parse(html);
        assertFalse(Tag.isKnownTag("item"));
    }

    @Test public void registerNewTag2(){
        String html =  "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<body>\n" +
                "\n" +
                "<p>plain-<blind>foo</blind>。</p>\n" +
                "\n" +
                "</body>\n" +
                "</html>";
        Tag.formatAndRegister("blind");
        assertTrue(Tag.isKnownTag("blind"));
        Document doc = Jsoup.parse(html);
        Elements elements = doc.getElementsByTag("blind");
        assertEquals("foo", elements.get(0).text());
    }
}
