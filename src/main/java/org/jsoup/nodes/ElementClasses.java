package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.internal.StringUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 Package-private helper that handles class-attribute operations for {@link Element}.
 Extracted from Element to reduce its size (God Class / Large Class smell).
 */
class ElementClasses {

    static String className(Element el) {
        return el.attr("class").trim();
    }

    static Set<String> classNames(Element el) {
        Set<String> classNames = new LinkedHashSet<>(4);
        if (el.attributes == null) return classNames;

        String classAttr = el.attributes.getIgnoreCase("class");
        int len = classAttr.length();
        for (int i = 0; i < len; ) {
            int start = nextClassStart(classAttr, i, len);
            if (start == len) break;

            int end = nextClassEnd(classAttr, start, len);
            classNames.add(classToken(classAttr, start, end));
            i = end;
        }
        return classNames;
    }

    static List<String> classList(Element el) {
        if (el.attributes == null) return Collections.emptyList();

        String attr = el.attributes.getIgnoreCase("class");
        int len = attr.length();
        int start = nextClassStart(attr, 0, len);
        if (start == len) return Collections.emptyList();

        int end = nextClassEnd(attr, start, len);
        String first = classToken(attr, start, end);
        start = nextClassStart(attr, end, len);
        if (start == len) return Collections.singletonList(first);

        List<String> classes = new ArrayList<>(4);
        classes.add(first);
        do {
            end = nextClassEnd(attr, start, len);
            classes.add(classToken(attr, start, end));
            start = nextClassStart(attr, end, len);
        } while (start < len);
        return Collections.unmodifiableList(classes);
    }

    static void classNames(Element el, Set<String> classNames) {
        Validate.notNull(classNames);
        if (classNames.isEmpty()) {
            el.attributes().remove("class");
        } else {
            el.attributes().put("class", StringUtil.join(classNames, " "));
        }
    }

    // performance sensitive
    static boolean hasClass(Element el, String className) {
        if (el.attributes == null) return false;

        final String classAttr = el.attributes.getIgnoreCase("class");
        final int len = classAttr.length();
        final int wantLen = className.length();

        if (len == 0 || len < wantLen) return false;

        if (len == wantLen) return className.equalsIgnoreCase(classAttr);

        for (int i = 0; i < len; ) {
            int start = nextClassStart(classAttr, i, len);
            if (start == len) return false;

            int end = nextClassEnd(classAttr, start, len);
            if (end - start == wantLen && classAttr.regionMatches(true, start, className, 0, wantLen)) return true;
            i = end;
        }

        return false;
    }

    static void addClass(Element el, String className) {
        Validate.notNull(className);
        Set<String> classes = classNames(el);
        classes.add(className);
        classNames(el, classes);
    }

    static void removeClass(Element el, String className) {
        Validate.notNull(className);
        Set<String> classes = classNames(el);
        classes.remove(className);
        classNames(el, classes);
    }

    static void toggleClass(Element el, String className) {
        Validate.notNull(className);
        Set<String> classes = classNames(el);
        if (classes.contains(className))
            classes.remove(className);
        else
            classes.add(className);
        classNames(el, classes);
    }

    private static int nextClassStart(String classAttr, int offset, int len) {
        while (offset < len && StringUtil.isWhitespace(classAttr.charAt(offset))) offset++;
        return offset;
    }

    private static int nextClassEnd(String classAttr, int offset, int len) {
        while (offset < len && !StringUtil.isWhitespace(classAttr.charAt(offset))) offset++;
        return offset;
    }

    private static String classToken(String classAttr, int start, int end) {
        return start == 0 && end == classAttr.length() ? classAttr : classAttr.substring(start, end);
    }
}
