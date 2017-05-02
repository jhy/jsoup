package testoracle;

import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.Test;

public class TokeniserTest {

    final char[] whiteSpace = { '\t', '\n', '\r', '\f', ' ' };
    final char[] quote = { '\'', '"' };

    @Test
    public void testCharacterReferenceInRcdata() {
        String body = "<textarea>You&I</textarea>";
        Document doc = Jsoup.parse(body);
        Elements els = doc.select("textarea");
        assertEquals("You&I", els.text());
    }

    @Test
    public void testBeforeTagName() {
        for (char c : whiteSpace) {
            String body = String.format("<div%c>test</div>", c);
            Document doc = Jsoup.parse(body);
            Elements els = doc.select("div");
            assertEquals("test", els.text());
        }
    }

    @Test
    public void testEndTagOpen() {
        String body;
        Document doc;
        Elements els;

        body = "<div>hello world</";
        doc = Jsoup.parse(body);
        els = doc.select("div");
        assertEquals("hello world</", els.text());

        body = "<div>hello world</div>";
        doc = Jsoup.parse(body);
        els = doc.select("div");
        assertEquals("hello world", els.text());

        body = "<div>fake</></div>";
        doc = Jsoup.parse(body);
        els = doc.select("div");
        assertEquals("fake", els.text());

        body = "<div>fake</?</div>";
        doc = Jsoup.parse(body);
        els = doc.select("div");
        assertEquals("fake", els.text());
    }

    @Test
    public void testRcdataLessthanSign() {
        String body;
        Document doc;
        Elements els;

        body = "<textarea><fake></textarea>";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("<fake>", els.text());

        body = "<textarea><open";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("", els.text());

        body = "<textarea>hello world</?fake</textarea>";
        doc = Jsoup.parse(body);
        els = doc.select("textarea");
        assertEquals("hello world</?fake", els.text());
    }

    @Test
    public void testRCDATAEndTagName() {
        for (char c : whiteSpace) {
            String body = String.format("<textarea>data</textarea%c>", c);
            Document doc = Jsoup.parse(body);
            Elements els = doc.select("textarea");
            assertEquals("data", els.text());
        }
    }

    @Test
    public void testCommentEndCoverage() {
        String html = "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --! --- --><p>Hello</p></body></html>";
        Document doc = Jsoup.parse(html);

        Element body = doc.body();
        Comment comment = (Comment) body.childNode(1);
        assertEquals(" <table><tr><td></table> --! --- ", comment.getData());
        Element p = body.child(1);
        TextNode text = (TextNode) p.childNode(0);
        assertEquals("Hello", text.getWholeText());
    }

    @Test
    public void testCommentEndBangCoverage() {
        String html = "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --!---!>--><p>Hello</p></body></html>";
        Document doc = Jsoup.parse(html);

        Element body = doc.body();
        Comment comment = (Comment) body.childNode(1);
        assertEquals(" <table><tr><td></table> --!-", comment.getData());
        Element p = body.child(1);
        TextNode text = (TextNode) p.childNode(0);
        assertEquals("Hello", text.getWholeText());
    }

    @Test
    public void testPublicIdentifiersWithWhitespace() {
        for (char q : quote) {
            for (char ws : whiteSpace) {
                String[] htmls = { 
                        String.format("<!DOCTYPE html%cPUBLIC %c-//W3C//DTD HTML 4.0//EN%c>", ws, q, q),
                        String.format("<!DOCTYPE html %cPUBLIC %c-//W3C//DTD HTML 4.0//EN%c>", ws, q, q),
                        String.format("<!DOCTYPE html PUBLIC%c%c-//W3C//DTD HTML 4.0//EN%c>", ws, q, q),
                        String.format("<!DOCTYPE html PUBLIC %c%c-//W3C//DTD HTML 4.0//EN%c>", ws, q, q),
                        String.format("<!DOCTYPE html PUBLIC %c-//W3C//DTD HTML 4.0//EN%c%c>", q, q, ws),
                        String.format("<!DOCTYPE html PUBLIC%c-//W3C//DTD HTML 4.0//EN%c%c>", q, q, ws)
                    };
                String expectedOutput = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0//EN\">";
                for (String html : htmls) {
                    Document doc = Jsoup.parse(html);
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml());
                }
            }
        }
    }

    @Test
    public void testSystemIdentifiersWithWhitespace() {
        for (char q : quote) {
            for (char ws : whiteSpace) {
                String[] htmls = {
                        String.format("<!DOCTYPE html%cSYSTEM %chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", ws, q, q),
                        String.format("<!DOCTYPE html %cSYSTEM %chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", ws, q, q),
                        String.format("<!DOCTYPE html SYSTEM%c%chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", ws, q, q),
                        String.format("<!DOCTYPE html SYSTEM %c%chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", ws, q, q),
                        String.format("<!DOCTYPE html SYSTEM %chttp://www.w3.org/TR/REC-html40/strict.dtd%c%c>", q, q, ws),
                        String.format("<!DOCTYPE html SYSTEM%chttp://www.w3.org/TR/REC-html40/strict.dtd%c%c>", q, q, ws)
                    };
                String expectedOutput = "<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/REC-html40/strict.dtd\">";
                for (String html : htmls) {
                    Document doc = Jsoup.parse(html);
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml());
                }
            }
        }
    }

    @Test
    public void testPublicAndSystemIdentifiersWithWhitespace() {
        for (char q : quote) {
            for (char ws : whiteSpace) {
                String[] htmls = {
                        String.format("<!DOCTYPE html PUBLIC %c-//W3C//DTD HTML 4.0//EN%c"
                                + "%c%chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", q, q, ws, q, q),
                        String.format("<!DOCTYPE html PUBLIC %c-//W3C//DTD HTML 4.0//EN%c"
                                + "%chttp://www.w3.org/TR/REC-html40/strict.dtd%c>", q, q, q, q)
                    };
                String expectedOutput = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0//EN\""
                        + " \"http://www.w3.org/TR/REC-html40/strict.dtd\">";
                for (String html : htmls) {
                    System.out.println(html);
                    Document doc = Jsoup.parse(html);
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml());
                }
            }
        }
    }
}
