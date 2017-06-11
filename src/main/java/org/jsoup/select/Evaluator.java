package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.XmlDeclaration;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jsoup.internal.Normalizer.lowerCase;
import static org.jsoup.internal.Normalizer.normalize;


/**
 * Evaluates that an element matches the selector.
 */
public abstract class Evaluator {
    protected Evaluator() {
    }

    /**
     * Test if the element meets the evaluator's requirements.
     *
     * @param root    Root of the matching subtree
     * @param element tested element
     * @return Returns <tt>true</tt> if the requirements are met or
     * <tt>false</tt> otherwise
     */
    public abstract boolean matches(Element root, Element element);

    /**
     * Evaluator for tag name
     */
    public static final class Tag extends Evaluator {
        private String tagName;

        public Tag(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return (element.tagName().equalsIgnoreCase(tagName));
        }

        @Override
        public String toString() {
            return String.format("%s", tagName);
        }
    }


    /**
     * Evaluator for tag name that ends with
     */
    public static final class TagEndsWith extends Evaluator {
        private String tagName;

        public TagEndsWith(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return (element.tagName().endsWith(tagName));
        }

        @Override
        public String toString() {
            return String.format("%s", tagName);
        }
    }

    /**
     * Evaluator for element id
     */
    public static final class Id extends Evaluator {
        private String id;

        public Id(String id) {
            this.id = id;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return (id.equals(element.id()));
        }

        @Override
        public String toString() {
            return String.format("#%s", id);
        }

    }

    /**
     * Evaluator for element class
     */
    public static final class Class extends Evaluator {
        private String className;

        public Class(String className) {
            this.className = className;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return (element.hasClass(className));
        }

        @Override
        public String toString() {
            return String.format(".%s", className);
        }

    }

    /**
     * Evaluator for attribute name matching
     */
    public static final class Attribute extends Evaluator {
        private String key;

        public Attribute(String key) {
            this.key = key;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.hasAttr(key);
        }

        @Override
        public String toString() {
            return String.format("[%s]", key);
        }

    }

    /**
     * Evaluator for attribute name prefix matching
     */
    public static final class AttributeStarting extends Evaluator {
        private String keyPrefix;

        public AttributeStarting(String keyPrefix) {
            Validate.notEmpty(keyPrefix);
            this.keyPrefix = lowerCase(keyPrefix);
        }

        @Override
        public boolean matches(Element root, Element element) {
            List<org.jsoup.nodes.Attribute> values = element.attributes().asList();
            for (org.jsoup.nodes.Attribute attribute : values) {
                if (lowerCase(attribute.getKey()).startsWith(keyPrefix))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("[^%s]", keyPrefix);
        }

    }

    /**
     * Evaluator for attribute name/value matching
     */
    public static final class AttributeWithValue extends AttributeKeyPair {
        public AttributeWithValue(String key, String value) {
            super(key, value);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.hasAttr(key) && value.equalsIgnoreCase(element.attr(key).trim());
        }

        @Override
        public String toString() {
            return String.format("[%s=%s]", key, value);
        }

    }

    /**
     * Evaluator for attribute name != value matching
     */
    public static final class AttributeWithValueNot extends AttributeKeyPair {
        public AttributeWithValueNot(String key, String value) {
            super(key, value);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return !value.equalsIgnoreCase(element.attr(key));
        }

        @Override
        public String toString() {
            return String.format("[%s!=%s]", key, value);
        }

    }

    /**
     * Evaluator for attribute name/value matching (value prefix)
     */
    public static final class AttributeWithValueStarting extends AttributeKeyPair {
        public AttributeWithValueStarting(String key, String value) {
            super(key, value);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.hasAttr(key) && lowerCase(element.attr(key)).startsWith(value); // value is lower case already
        }

        @Override
        public String toString() {
            return String.format("[%s^=%s]", key, value);
        }

    }

    /**
     * Evaluator for attribute name/value matching (value ending)
     */
    public static final class AttributeWithValueEnding extends AttributeKeyPair {
        public AttributeWithValueEnding(String key, String value) {
            super(key, value);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.hasAttr(key) && lowerCase(element.attr(key)).endsWith(value); // value is lower case
        }

        @Override
        public String toString() {
            return String.format("[%s$=%s]", key, value);
        }

    }

    /**
     * Evaluator for attribute name/value matching (value containing)
     */
    public static final class AttributeWithValueContaining extends AttributeKeyPair {
        public AttributeWithValueContaining(String key, String value) {
            super(key, value);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.hasAttr(key) && lowerCase(element.attr(key)).contains(value); // value is lower case
        }

        @Override
        public String toString() {
            return String.format("[%s*=%s]", key, value);
        }

    }

    /**
     * Evaluator for attribute name/value matching (value regex matching)
     */
    public static final class AttributeWithValueMatching extends Evaluator {
        String key;
        Pattern pattern;

        public AttributeWithValueMatching(String key, Pattern pattern) {
            this.key = normalize(key);
            this.pattern = pattern;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.hasAttr(key) && pattern.matcher(element.attr(key)).find();
        }

        @Override
        public String toString() {
            return String.format("[%s~=%s]", key, pattern.toString());
        }

    }

    /**
     * Abstract evaluator for attribute name/value matching
     */
    public abstract static class AttributeKeyPair extends Evaluator {
        String key;
        String value;

        public AttributeKeyPair(String key, String value) {
            Validate.notEmpty(key);
            Validate.notEmpty(value);

            this.key = normalize(key);
            if (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length()-1);
            }
            this.value = normalize(value);
        }
    }

    /**
     * Evaluator for any / all element matching
     */
    public static final class AllElements extends Evaluator {

        @Override
        public boolean matches(Element root, Element element) {
            return true;
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    /**
     * Evaluator for matching by sibling index number (e {@literal <} idx)
     */
    public static final class IndexLessThan extends IndexEvaluator {
        public IndexLessThan(int index) {
            super(index);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.elementSiblingIndex() < index;
        }

        @Override
        public String toString() {
            return String.format(":lt(%d)", index);
        }

    }

    /**
     * Evaluator for matching by sibling index number (e {@literal >} idx)
     */
    public static final class IndexGreaterThan extends IndexEvaluator {
        public IndexGreaterThan(int index) {
            super(index);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.elementSiblingIndex() > index;
        }

        @Override
        public String toString() {
            return String.format(":gt(%d)", index);
        }

    }

    /**
     * Evaluator for matching by sibling index number (e = idx)
     */
    public static final class IndexEquals extends IndexEvaluator {
        public IndexEquals(int index) {
            super(index);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.elementSiblingIndex() == index;
        }

        @Override
        public String toString() {
            return String.format(":eq(%d)", index);
        }

    }
    
    /**
     * Evaluator for matching the last sibling (css :last-child)
     */
    public static final class IsLastChild extends Evaluator {
		@Override
		public boolean matches(Element root, Element element) {
			final Element p = element.parent();
			return p != null && !(p instanceof Document) && element.elementSiblingIndex() == p.children().size()-1;
		}
    	
		@Override
		public String toString() {
			return ":last-child";
		}
    }
    
    public static final class IsFirstOfType extends IsNthOfType {
		public IsFirstOfType() {
			super(0,1);
		}
		@Override
		public String toString() {
			return ":first-of-type";
		}
    }
    
    public static final class IsLastOfType extends IsNthLastOfType {
		public IsLastOfType() {
			super(0,1);
		}
		@Override
		public String toString() {
			return ":last-of-type";
		}
    }

    
    public static abstract class CssNthEvaluator extends Evaluator {
    	protected final int a, b;
    	
    	public CssNthEvaluator(int a, int b) {
    		this.a = a;
    		this.b = b;
    	}
    	public CssNthEvaluator(int b) {
    		this(0,b);
    	}
    	
    	@Override
    	public boolean matches(Element root, Element element) {
    		final Element p = element.parent();
    		if (p == null || (p instanceof Document)) return false;
    		
    		final int pos = calculatePosition(root, element);
    		if (a == 0) return pos == b;
    		
    		return (pos-b)*a >= 0 && (pos-b)%a==0;
    	}
    	
		@Override
		public String toString() {
			if (a == 0)
				return String.format(":%s(%d)",getPseudoClass(), b);
			if (b == 0)
				return String.format(":%s(%dn)",getPseudoClass(), a);
			return String.format(":%s(%dn%+d)", getPseudoClass(),a, b);
		}
    	
		protected abstract String getPseudoClass();
		protected abstract int calculatePosition(Element root, Element element);
    }
    
    
    /**
     * css-compatible Evaluator for :eq (css :nth-child)
     * 
     * @see IndexEquals
     */
    public static final class IsNthChild extends CssNthEvaluator {

    	public IsNthChild(int a, int b) {
    		super(a,b);
		}

		protected int calculatePosition(Element root, Element element) {
			return element.elementSiblingIndex()+1;
		}

		
		protected String getPseudoClass() {
			return "nth-child";
		}
    }
    
    /**
     * css pseudo class :nth-last-child)
     * 
     * @see IndexEquals
     */
    public static final class IsNthLastChild extends CssNthEvaluator {
    	public IsNthLastChild(int a, int b) {
    		super(a,b);
    	}

        @Override
        protected int calculatePosition(Element root, Element element) {
        	return element.parent().children().size() - element.elementSiblingIndex();
        }
        
		@Override
		protected String getPseudoClass() {
			return "nth-last-child";
		}
    }
    
    /**
     * css pseudo class nth-of-type
     * 
     */
    public static class IsNthOfType extends CssNthEvaluator {
    	public IsNthOfType(int a, int b) {
    		super(a,b);
    	}

		protected int calculatePosition(Element root, Element element) {
			int pos = 0;
        	Elements family = element.parent().children();
            for (Element el : family) {
                if (el.tag().equals(element.tag())) pos++;
                if (el == element) break;
            }
			return pos;
		}

		@Override
		protected String getPseudoClass() {
			return "nth-of-type";
		}
    }
    
    public static class IsNthLastOfType extends CssNthEvaluator {

		public IsNthLastOfType(int a, int b) {
			super(a, b);
		}
		
		@Override
		protected int calculatePosition(Element root, Element element) {
			int pos = 0;
        	Elements family = element.parent().children();
        	for (int i = element.elementSiblingIndex(); i < family.size(); i++) {
        		if (family.get(i).tag().equals(element.tag())) pos++;
        	}
			return pos;
		}

		@Override
		protected String getPseudoClass() {
			return "nth-last-of-type";
		}
    }

    /**
     * Evaluator for matching the first sibling (css :first-child)
     */
    public static final class IsFirstChild extends Evaluator {
    	@Override
    	public boolean matches(Element root, Element element) {
    		final Element p = element.parent();
    		return p != null && !(p instanceof Document) && element.elementSiblingIndex() == 0;
    	}
    	
    	@Override
    	public String toString() {
    		return ":first-child";
    	}
    }
    
    /**
     * css3 pseudo-class :root
     * @see <a href="http://www.w3.org/TR/selectors/#root-pseudo">:root selector</a>
     *
     */
    public static final class IsRoot extends Evaluator {
    	@Override
    	public boolean matches(Element root, Element element) {
    		final Element r = root instanceof Document?root.child(0):root;
    		return element == r;
    	}
    	@Override
    	public String toString() {
    		return ":root";
    	}
    }

    public static final class IsOnlyChild extends Evaluator {
		@Override
		public boolean matches(Element root, Element element) {
			final Element p = element.parent();
			return p!=null && !(p instanceof Document) && element.siblingElements().size() == 0;
		}
    	@Override
    	public String toString() {
    		return ":only-child";
    	}
    }

    public static final class IsOnlyOfType extends Evaluator {
		@Override
		public boolean matches(Element root, Element element) {
			final Element p = element.parent();
			if (p==null || p instanceof Document) return false;
			
			int pos = 0;
        	Elements family = p.children();
            for (Element el : family) {
                if (el.tag().equals(element.tag())) pos++;
            }
        	return pos == 1;
		}
    	@Override
    	public String toString() {
    		return ":only-of-type";
    	}
    }

    public static final class IsEmpty extends Evaluator {
		@Override
		public boolean matches(Element root, Element element) {
        	List<Node> family = element.childNodes();
            for (Node n : family) {
                if (!(n instanceof Comment || n instanceof XmlDeclaration || n instanceof DocumentType)) return false;
            }
        	return true;
		}
    	@Override
    	public String toString() {
    		return ":empty";
    	}
    }

    /**
     * Abstract evaluator for sibling index matching
     *
     * @author ant
     */
    public abstract static class IndexEvaluator extends Evaluator {
        int index;

        public IndexEvaluator(int index) {
            this.index = index;
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) text
     */
    public static final class ContainsText extends Evaluator {
        private String searchText;

        public ContainsText(String searchText) {
            this.searchText = lowerCase(searchText);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return lowerCase(element.text()).contains(searchText);
        }

        @Override
        public String toString() {
            return String.format(":contains(%s)", searchText);
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) data
     */
    public static final class ContainsData extends Evaluator {
        private String searchText;

        public ContainsData(String searchText) {
            this.searchText = lowerCase(searchText);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return lowerCase(element.data()).contains(searchText);
        }

        @Override
        public String toString() {
            return String.format(":containsData(%s)", searchText);
        }
    }

    /**
     * Evaluator for matching Element's own text
     */
    public static final class ContainsOwnText extends Evaluator {
        private String searchText;

        public ContainsOwnText(String searchText) {
            this.searchText = lowerCase(searchText);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return lowerCase(element.ownText()).contains(searchText);
        }

        @Override
        public String toString() {
            return String.format(":containsOwn(%s)", searchText);
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) text with regex
     */
    public static final class Matches extends Evaluator {
        private Pattern pattern;

        public Matches(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(Element root, Element element) {
            Matcher m = pattern.matcher(element.text());
            return m.find();
        }

        @Override
        public String toString() {
            return String.format(":matches(%s)", pattern);
        }
    }

    /**
     * Evaluator for matching Element's own text with regex
     */
    public static final class MatchesOwn extends Evaluator {
        private Pattern pattern;

        public MatchesOwn(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(Element root, Element element) {
            Matcher m = pattern.matcher(element.ownText());
            return m.find();
        }

        @Override
        public String toString() {
            return String.format(":matchesOwn(%s)", pattern);
        }
    }
}
