package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WrapTest {
    @Test
    public void normalWrapTest() {
        String html = "<tag></tag>";
        Element el = Jsoup.parse(html).selectFirst("tag");
        el.wrap("<div></div>");
        assertEquals("<div>\n <tag></tag>\n</div>", el.parent().toString());
    }

    @Test
    public void standaloneWrapTest() {
        Element el = new Element("tag");
        el.wrap("<div></div>");
        assertEquals("<div>\n <tag></tag>\n</div>", el.parent().toString());
    }

    @Test
    public void wrapWithRemainder() {
        Element el = new Element("tag");
        el.wrap("<div></div><p></p>");
        assertEquals("<div>\n <tag></tag>\n <p></p>\n</div>", el.parent().toString());
    }

}
