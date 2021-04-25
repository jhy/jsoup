package org.jsoup;
import org.jsoup.nodes.Attributes;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class orderTest {

    @Test
    public void testInput() throws IOException {

        final Attributes attributesBarFoo = Jsoup.parseBodyFragment("<a style=\"bar\" class=\"foo\">").select("a").first().attributes();
        final Attributes attributesFooBar = Jsoup.parseBodyFragment("<a class=\"foo\" style=\"bar\">").select("a").first().attributes();
        assertEquals(attributesBarFoo, attributesFooBar);
    }

    @Test
    public void anotherTest() throws IOException {

        final Attributes attributesBarFoo = Jsoup.parseBodyFragment("<base href=\"http://www.runoob.com/images/\" target=\"_blank\">").select("base").first().attributes();
        final Attributes attributesFooBar = Jsoup.parseBodyFragment("<base target=\"_blank\" href=\"http://www.runoob.com/images/\">").select("base").first().attributes();
        assertEquals(attributesBarFoo, attributesFooBar);
    }
}

