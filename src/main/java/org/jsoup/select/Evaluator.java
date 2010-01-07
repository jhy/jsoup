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

    public static class AttributeWithValue extends AttributeKeyPair {
        public AttributeWithValue(String key, String value) {
            super(key, value);
        }

        boolean matches(Element element) {
            return (value.equalsIgnoreCase(element.attr(key)));
        }
    }

    public static class AttributeWithValueNot extends AttributeKeyPair {
        public AttributeWithValueNot(String key, String value) {
            super(key, value);
        }

        boolean matches(Element element) {
            return (!value.equalsIgnoreCase(element.attr(key)));
        }
    }

    public static class AttributeWithValueStarting extends AttributeKeyPair {
        public AttributeWithValueStarting(String key, String value) {
            super(key, value);
        }

        boolean matches(Element element) {
            return element.attr(key).toLowerCase().startsWith(value); // value is lower case already
        }
    }

    public static class AttributeWithValueEnding extends AttributeKeyPair {
        public AttributeWithValueEnding(String key, String value) {
            super(key, value);
        }

        boolean matches(Element element) {
            return element.attr(key).toLowerCase().endsWith(value); // value is lower case
        }
    }

    public static class AttributeWithValueContaining extends AttributeKeyPair {
        public AttributeWithValueContaining(String key, String value) {
            super(key, value);
        }

        boolean matches(Element element) {
            return element.attr(key).toLowerCase().contains(value); // value is lower case
        }
    }

    public abstract static class AttributeKeyPair extends Evaluator {
        protected String key;
        protected String value;

        public AttributeKeyPair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class AllElements extends Evaluator {
        boolean matches(Element element) {
            return true;
        }
    }


}
