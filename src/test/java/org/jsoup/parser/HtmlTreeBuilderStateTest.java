package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
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
        assertEquals(38, constants.size());
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
            " <body><a href=\"#1\"> </a>\n" +
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
            " <body><a href=\"#1\"> </a>\n" +
            "  <div>\n" +
            "   <a href=\"#1\"> </a>\n" +
            "   <div>\n" +
            "    <a href=\"#1\"> </a><a href=\"#2\">child</a>\n" +
            "   </div>\n" +
            "  </div>\n" +
            " </body>\n" +
            "</html>", s);
    }

}