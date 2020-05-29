package org.jsoup;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class ExampleTest {
    @Test
    public void testChineseTag() {
        String s = "<p><一></p>";
        Document doc =Jsoup.parse(s);
        assertEquals("<一>",doc.text());
    }
    @Test
    public void testAlphabetTag() {
        for(int i=0;i<26;i++){
            char c = (char)('a'+i);
            String s = "<p><"+c+"></p>";
            Document doc =Jsoup.parse(s);
            assertEquals("",doc.text());
            c = (char)('A'+i);
            s = "<p><"+c+"></p>";
            doc =Jsoup.parse(s);
            assertEquals("",doc.text());
        }


    }
}
