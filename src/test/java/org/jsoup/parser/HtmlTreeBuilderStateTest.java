package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.HtmlTreeBuilderState.Constants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jsoup.parser.HtmlTreeBuilderState.Constants.InBodyStartInputAttribs;
import static org.junit.jupiter.api.Assertions.*;

public class HtmlTreeBuilderStateTest {
    static List<Object[]> findConstantArrays(Class aClass) {
        ArrayList<Object[]> array = new ArrayList<>();
        Field[] fields = aClass.getDeclaredFields();

        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && field.getType().isArray()) {
                try {
                    array.add((Object[]) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        return array;
    }

    static void ensureSorted(List<Object[]> constants) {
        for (Object[] array : constants) {
            Object[] copy = Arrays.copyOf(array, array.length);
            Arrays.sort(array);
            assertArrayEquals(array, copy);
        }
    }

    @Test
    public void ensureArraysAreSorted() {
        List<Object[]> constants = findConstantArrays(Constants.class);
        ensureSorted(constants);
        assertEquals(35, constants.size());
    }

    @Test public void ensureTagSearchesAreKnownTags() {
        List<Object[]> constants = findConstantArrays(Constants.class);
        for (Object[] constant : constants) {
            String[] tagNames = (String[]) constant;
            for (String tagName : tagNames) {
                if (StringUtil.inSorted(tagName, InBodyStartInputAttribs))
                    continue; // odd one out in the constant
                assertTrue(Tag.isKnownTag(tagName), String.format("Unknown tag name: %s", tagName));
            }
        }
    }

    @Test public void mainStartAndEndTagsCloseParagraphs() {
        Document doc = Jsoup.parse("<p>foo<main><p>bar</main>baz");
        doc.outputSettings().prettyPrint(false);
        assertEquals("<p>foo</p><main><p>bar</p></main>baz", doc.body().html());
    }


    @Test
    public void nestedAnchorElements01() {
        String html = "<html>\n" +
            "  <body>\n" +
            "    <a href='#1'>\n" +
            "        <div>\n" +
            "          <a href='#2'>child</a>\n" +
            "        </div>\n" +
            "    </a>\n" +
            "  </body>\n" +
            "</html>";
        String s = Jsoup.parse(html).toString();
        assertEquals("<html>\n" +
            " <head></head>\n" +
            " <body>\n" +
            "  <a href=\"#1\"> </a>\n" +
            "  <div>\n" +
            "   <a href=\"#1\"> </a><a href=\"#2\">child</a>\n" +
            "  </div>\n" +
            " </body>\n" +
            "</html>", s);
    }

    @Test
    public void nestedAnchorElements02() {
        String html = "<html>\n" +
            "  <body>\n" +
            "    <a href='#1'>\n" +
            "      <div>\n" +
            "        <div>\n" +
            "          <a href='#2'>child</a>\n" +
            "        </div>\n" +
            "      </div>\n" +
            "    </a>\n" +
            "  </body>\n" +
            "</html>";
        String s = Jsoup.parse(html).toString();
        assertEquals("<html>\n" +
            " <head></head>\n" +
            " <body>\n" +
            "  <a href=\"#1\"> </a>\n" +
            "  <div>\n" +
            "   <a href=\"#1\"> </a>\n" +
            "   <div>\n" +
            "    <a href=\"#1\"> </a><a href=\"#2\">child</a>\n" +
            "   </div>\n" +
            "  </div>\n" +
            " </body>\n" +
            "</html>", s);
    }

    @Test public void scriptEndTagLeavesFollowingSpaceInHead() {
        Document doc = Jsoup.parse("<!DOCTYPE html><script> <!-- </script> --> </script> EOF");
        Element head = doc.head();
        assertEquals(" ", ((TextNode) head.childNode(1)).getWholeText());
        assertEquals("-->  EOF", doc.body().wholeText());
    }

    @Test public void whitespaceBeforeHtml() {
        Document doc = Jsoup.parse("<!doctype html> \tX");
        assertEquals(" \t", ((TextNode) doc.childNode(1)).getWholeText());
        assertEquals("X", doc.body().wholeText());
    }

    @Test public void whitespaceBeforeHead() {
        Document doc = Jsoup.parse("<!doctype html><html> \tX");
        assertEquals(" \t", ((TextNode) doc.expectFirst("html").childNode(0)).getWholeText());
        assertEquals("X", doc.body().wholeText());
    }

    @Test public void whitespaceInHead() {
        Document doc = Jsoup.parse("<!doctype html><head> \tX");
        assertEquals(" \t", ((TextNode) doc.head().childNode(0)).getWholeText());
        assertEquals("X", doc.body().wholeText());
    }

    @Test public void whitespaceAfterHead() {
        Document doc = Jsoup.parse("<!doctype html><head></head> \tX");
        assertEquals(" \t", ((TextNode) doc.expectFirst("html").childNode(1)).getWholeText());
        assertEquals("X", doc.body().wholeText());
    }

    @Test public void whitespaceInColumnGroup() {
        Document doc = Jsoup.parse("<!doctype html><table><colgroup> \tX");
        assertEquals(" \t", ((TextNode) doc.expectFirst("colgroup").childNode(0)).getWholeText());
        assertEquals("X", doc.body().ownText());
    }

    @Test public void whitespaceAfterBody() {
        Document doc = Jsoup.parse("<!doctype html><body>A</body> \tX");
        assertEquals(" \t", ((TextNode) doc.expectFirst("html").childNode(2)).getWholeText());
        assertEquals("AX", doc.body().wholeText());
    }

    @Test public void whitespaceAfterAfterBody() {
        Document doc = Jsoup.parse("<!doctype html><body>A</body></html> \tX");
        assertEquals(" \t", ((TextNode) doc.childNode(2)).getWholeText());
        assertEquals("AX", doc.body().wholeText());
    }

    @Test public void whitespaceAfterAfterFrameset() {
        Document doc = Jsoup.parse("<!doctype html><frameset></frameset></html> \tX");
        assertEquals(" \t", ((TextNode) doc.expectFirst("html").childNode(2)).getWholeText());
        assertEquals(" \t", doc.wholeText());
    }

    @Test public void initialWhitespaceIsIgnored() {
        Document initial = Jsoup.parse(" \tX");
        assertEquals(1, initial.childNodeSize());
        assertEquals("X", initial.body().wholeText());

        String html = "<html><head></head><body>X</body></html>";
        Document explicitHtml = Jsoup.parse(" \t" + html);
        explicitHtml.outputSettings().prettyPrint(false);
        assertEquals(html, explicitHtml.outerHtml());
    }

    @Test public void whitespaceAndNullBeforeFrameset() {
        Document doc = Jsoup.parse("<html> \u0000 <frameset></frameset>");
        Element html = doc.expectFirst("html");
        assertEquals("head", html.childNode(0).nodeName());
        assertEquals("frameset", html.childNode(1).nodeName());
    }

    @Test public void whitespaceAcrossInsertionModesRoundTrips() {
        // the decoded '&' closes the head; Y and Z cross the body and html end-tag boundaries
        String input = "<!doctype html> \n<html> \t<head> &#32;&amp;X</body> \tY</html> \tZ";
        String expected = "<!doctype html> \n<html> \t<head>  </head><body>&amp;XYZ</body> \t</html> \t";

        Document doc = Jsoup.parse(input);
        doc.outputSettings().prettyPrint(false);
        String serialized = doc.outerHtml();
        assertEquals(expected, serialized);

        Document reparsed = Jsoup.parse(serialized);
        reparsed.outputSettings().prettyPrint(false);
        assertEquals(expected, reparsed.outerHtml());
    }
}
