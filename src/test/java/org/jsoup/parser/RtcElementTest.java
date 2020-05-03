package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RtcElementTest {
    @Test
    public void testParseRtc() {
        String html = "<html><head></head><body><ruby><rtc><rt>Month</rt></rtc></ruby></body></html>";
        Document doc = Jsoup.parse(html);
        assertEquals("<ruby>\n" +
                " <rtc>\n" +
                "  <rt>Month</rt>\n" +
                " </rtc></ruby>", doc.body().html());
    }
}
