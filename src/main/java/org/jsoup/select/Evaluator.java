package org.jsoup.select;

import org.jsoup.nodes.Element;

/**
 * Evaluates that an element matches the selector.
 *
 * @author Jonathan Hedley
 */
public abstract class Evaluator {
    abstract boolean matches(Element element);

    public static class Tag extends Evaluator {
        private String tagName;
        public Tag (String tagName) {
            this.tagName = tagName;
        }

        boolean matches(Element element) {
            return (element.tagName().equals(tagName));
        }
    }

    public static class Id extends Evaluator {
        private String id;
        public Id (String id) {
            this.id = id;
        }

        boolean matches(Element element) {
            return (id.equals(element.id()));
        }
    }

    public static class Class extends Evaluator {
        private String className;
        public Class(String className) {
            this.className = className;
        }

        boolean matches(Element element) {
            return (element.hasClass(className));
        }
    }

    public static class Attribute extends Evaluator {
        private String key;

        public Attribute (String key) {
            this.key = key;
        }

        boolean matches(Element element) {
            return (element.hasAttr(key));
        }
    }

    public static class AttributeWithValue extends Evaluator {
        private String key;
        private String value;

        public AttributeWithValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        boolean matches(Element element) {
            return (value.equals(element.attr(key)));
        }
    }

    public static class AllElements extends Evaluator {
        boolean matches(Element element) {
            return true;
        }
    }


}
