package org.jsoup.nodes;

import org.jsoup.helper.Validate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Evaluates that an element matches the selector.
 *
 * @author Jonathan Hedley
 */
public abstract class Evaluator {
    private Evaluator() {}
    
    /**
     * Test if the element meets the evaluator's requirements.
     */
    public abstract boolean matches(Element element);

    static final class Tag extends Evaluator {
        private String tagName;
        Tag (String tagName) {
            this.tagName = tagName;
        }

        public boolean matches(Element element) {
            return (element.tagName().equals(tagName));
        }
    }

    static final class Id extends Evaluator {
        private String id;
        Id (String id) {
            this.id = id;
        }

        public boolean matches(Element element) {
            return (id.equals(element.id()));
        }
    }

    static final class Class extends Evaluator {
        private String className;
        Class(String className) {
            this.className = className;
        }

        public boolean matches(Element element) {
            return (element.hasClass(className));
        }
    }

    static final class Attribute extends Evaluator {
        private String key;

        Attribute (String key) {
            this.key = key;
        }

        public boolean matches(Element element) {
            return element.hasAttr(key);
        }
    }

    static final class AttributeStarting extends Evaluator {
        private String keyPrefix;

        AttributeStarting (String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public boolean matches(Element element) {
            List<org.jsoup.nodes.Attribute> values = element.attributes.asList();
            for (org.jsoup.nodes.Attribute attribute : values) {
                if (attribute.getKey().startsWith(keyPrefix))
                    return true;
            }
            return false;
        }
    }

    static final class AttributeWithValue extends AttributeKeyPair {
        AttributeWithValue(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && value.equalsIgnoreCase(element.attr(key));
        }
    }

    static final class AttributeWithValueNot extends AttributeKeyPair {
        AttributeWithValueNot(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return !value.equalsIgnoreCase(element.attr(key));
        }
    }

    static final class AttributeWithValueStarting extends AttributeKeyPair {
        AttributeWithValueStarting(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && element.attr(key).toLowerCase().startsWith(value); // value is lower case already
        }
    }

    static final class AttributeWithValueEnding extends AttributeKeyPair {
        AttributeWithValueEnding(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && element.attr(key).toLowerCase().endsWith(value); // value is lower case
        }
    }

    static final class AttributeWithValueContaining extends AttributeKeyPair {
        AttributeWithValueContaining(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && element.attr(key).toLowerCase().contains(value); // value is lower case
        }
    }
    
    static final class AttributeWithValueMatching extends Evaluator{
        protected String key;
        protected Pattern pattern;
        
        AttributeWithValueMatching(String key, Pattern pattern) {
            this.key = key.trim().toLowerCase();
            this.pattern = pattern;
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && pattern.matcher(element.attr(key)).find();
        }
    }

    abstract static class AttributeKeyPair extends Evaluator {
        protected String key;
        protected String value;

        AttributeKeyPair(String key, String value) {
            Validate.notEmpty(key);
            Validate.notEmpty(value);
            
            this.key = key.trim().toLowerCase();
            this.value = value.trim().toLowerCase();
        }
    }

    static final class AllElements extends Evaluator {
        public boolean matches(Element element) {
            return true;
        }
    }
    
    static final class IndexLessThan extends IndexEvaluator {
        IndexLessThan(int index) {
            super(index);
        }

        public boolean matches(Element element) {
            return element.elementSiblingIndex() < index;
        }
    }
    
    static final class IndexGreaterThan extends IndexEvaluator {
        IndexGreaterThan(int index) {
            super(index);
        }

        public boolean matches(Element element) {
            return element.elementSiblingIndex() > index;
        }
    }
    
    static final class IndexEquals extends IndexEvaluator {
        IndexEquals(int index) {
            super(index);
        }

        public boolean matches(Element element) {
            return element.elementSiblingIndex() == index;
        }
    }    
    
    abstract static class IndexEvaluator extends Evaluator {
        protected int index;
        
        IndexEvaluator(int index) {
            this.index = index;
        }
    }
    
    static final class ContainsText extends Evaluator {
        private String searchText;
        ContainsText(String searchText) {
            this.searchText = searchText.toLowerCase();
        }

        public boolean matches(Element element) {
            return (element.text().toLowerCase().contains(searchText));
        }
    }
    
    static final class ContainsOwnText extends Evaluator {
        private String searchText;
        ContainsOwnText(String searchText) {
            this.searchText = searchText.toLowerCase();
        }

        public boolean matches(Element element) {
            return (element.ownText().toLowerCase().contains(searchText));
        }
    }
    
    static final class Matches extends Evaluator {
        private Pattern pattern;
        Matches(Pattern pattern) {
            this.pattern = pattern;
        }

        public boolean matches(Element element) {
            Matcher m = pattern.matcher(element.text());
            return m.find();
        }
    }
    
    static final class MatchesOwn extends Evaluator {
        private Pattern pattern;
        MatchesOwn(Pattern pattern) {
            this.pattern = pattern;
        }

        public boolean matches(Element element) {
            Matcher m = pattern.matcher(element.ownText());
            return m.find();
        }
    }


}
