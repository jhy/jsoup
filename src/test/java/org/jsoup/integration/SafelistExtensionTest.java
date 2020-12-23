package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 Check that we can extend Safelist methods
 */
public class SafelistExtensionTest {
    @Test public void canCustomizeSafeTests() {
        OpenSafelist openSafelist = new OpenSafelist(Safelist.relaxed());
        Safelist safelist = Safelist.relaxed();

        String html = "<p><opentag openattr>Hello</opentag></p>";

        String openClean = Jsoup.clean(html, openSafelist);
        String clean = Jsoup.clean(html, safelist);

        assertEquals("<p><opentag openattr=\"\">Hello</opentag></p>", TextUtil.stripNewlines(openClean));
        assertEquals("<p>Hello</p>", clean);
    }

    // passes tags and attributes starting with "open"
    private static class OpenSafelist extends Safelist {
        public OpenSafelist(Safelist safelist) {
            super(safelist);
        }

        @Override
        protected boolean isSafeAttribute(String tagName, Element el, Attribute attr) {
            if (attr.getKey().startsWith("open"))
                return true;
            return super.isSafeAttribute(tagName, el, attr);
        }

        @Override
        protected boolean isSafeTag(String tag) {
            if (tag.startsWith("open"))
                return true;
            return super.isSafeTag(tag);
        }
    }
}
