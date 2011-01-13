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
    protected Evaluator() {}
    
    /**
     * Test if the element meets the evaluator's requirements.
     */
    public abstract boolean matches(Element element);

    public static final class Tag extends Evaluator {
        private String tagName;
        public Tag (String tagName) {
            this.tagName = tagName;
        }

        public boolean matches(Element element) {
            return (element.tagName().equals(tagName));
        }
    }

    public static final class Id extends Evaluator {
        private String id;
        public Id (String id) {
            this.id = id;
        }

        public boolean matches(Element element) {
            return (id.equals(element.id()));
        }
    }

    public static final class Class extends Evaluator {
        private String className;
        public Class(String className) {
            this.className = className;
        }

        public boolean matches(Element element) {
            return (element.hasClass(className));
        }
    }

    public static final class Attribute extends Evaluator {
        private String key;

        public Attribute (String key) {
            this.key = key;
        }

        public boolean matches(Element element) {
            return element.hasAttr(key);
        }
    }

    public static final class AttributeStarting extends Evaluator {
        private String keyPrefix;

        public AttributeStarting (String keyPrefix) {
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

    public static final class AttributeWithValue extends AttributeKeyPair {
        public AttributeWithValue(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && value.equalsIgnoreCase(element.attr(key));
        }
    }

    public static final class AttributeWithValueNot extends AttributeKeyPair {
        public AttributeWithValueNot(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return !value.equalsIgnoreCase(element.attr(key));
        }
    }

    public static final class AttributeWithValueStarting extends AttributeKeyPair {
        public AttributeWithValueStarting(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && element.attr(key).toLowerCase().startsWith(value); // value is lower case already
        }
    }

    public static final class AttributeWithValueEnding extends AttributeKeyPair {
        public AttributeWithValueEnding(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && element.attr(key).toLowerCase().endsWith(value); // value is lower case
        }
    }

    public static final class AttributeWithValueContaining extends AttributeKeyPair {
        public AttributeWithValueContaining(String key, String value) {
            super(key, value);
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && element.attr(key).toLowerCase().contains(value); // value is lower case
        }
    }
    
    public static final class AttributeWithValueMatching extends Evaluator{
        protected String key;
        protected Pattern pattern;
        
        public AttributeWithValueMatching(String key, Pattern pattern) {
            this.key = key.trim().toLowerCase();
            this.pattern = pattern;
        }

        public boolean matches(Element element) {
            return element.hasAttr(key) && pattern.matcher(element.attr(key)).find();
        }
    }

    public abstract static class AttributeKeyPair extends Evaluator {
        protected String key;
        protected String value;

        public AttributeKeyPair(String key, String value) {
            Validate.notEmpty(key);
            Validate.notEmpty(value);
            
            this.key = key.trim().toLowerCase();
            this.value = value.trim().toLowerCase();
        }
    }

    public static final class AllElements extends Evaluator {
        public boolean matches(Element element) {
            return true;
        }
    }
    
    public static final class IndexLessThan extends IndexEvaluator {
        public IndexLessThan(int index) {
            super(index);
        }

        public boolean matches(Element element) {
            return element.elementSiblingIndex() < index;
        }
    }
    
    public static final class IndexGreaterThan extends IndexEvaluator {
        public IndexGreaterThan(int index) {
            super(index);
        }

        public boolean matches(Element element) {
            return element.elementSiblingIndex() > index;
        }
    }
    
    public static final class IndexEquals extends IndexEvaluator {
        public IndexEquals(int index) {
            super(index);
        }

        public boolean matches(Element element) {
            return element.elementSiblingIndex() == index;
        }
    }    
    
    public abstract static class IndexEvaluator extends Evaluator {
        protected int index;
        
        public IndexEvaluator(int index) {
            this.index = index;
        }
    }
    
    public static final class ContainsText extends Evaluator {
        private String searchText;
        public ContainsText(String searchText) {
            this.searchText = searchText.toLowerCase();
        }

        public boolean matches(Element element) {
            return (element.text().toLowerCase().contains(searchText));
        }
    }
    
    public static final class ContainsOwnText extends Evaluator {
        private String searchText;
        public ContainsOwnText(String searchText) {
            this.searchText = searchText.toLowerCase();
        }

        public boolean matches(Element element) {
            return (element.ownText().toLowerCase().contains(searchText));
        }
    }
    
    public static final class Matches extends Evaluator {
        private Pattern pattern;
        public Matches(Pattern pattern) {
            this.pattern = pattern;
        }

        public boolean matches(Element element) {
            Matcher m = pattern.matcher(element.text());
            return m.find();
        }
    }
    
    public static final class MatchesOwn extends Evaluator {
        private Pattern pattern;
        public MatchesOwn(Pattern pattern) {
            this.pattern = pattern;
        }

        public boolean matches(Element element) {
            Matcher m = pattern.matcher(element.ownText());
            return m.find();
        }
    }


}
