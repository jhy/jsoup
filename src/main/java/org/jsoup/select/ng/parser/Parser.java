package org.jsoup.select.ng.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Evaluator;
import org.jsoup.parser.TokenQueue;
import org.jsoup.select.ng.AndSelector;
import org.jsoup.select.ng.BasicSelector;
import org.jsoup.select.ng.ElementContainerSelector;
import org.jsoup.select.ng.ParentSelector;

public class Parser {
	TokenQueue tq;
    private final static String[] combinators = {",", ">", "+", "~", " "};
    String query;
    Deque<Evaluator> s = new ArrayDeque<Evaluator>();
    
    
    
    public Parser(String query) {
    	this.query = query;
    	this.tq = new TokenQueue(query);
    }
    
    public static Evaluator select(String query) {
    	Parser p = new Parser(query);
    	return p.select();
    }
    
    public Evaluator select() {
        tq.consumeWhitespace();
        
        if (tq.matchesAny(combinators)) { // if starts with a combinator, use root as elements
            //elements.add(root);
            combinator(tq.consume().toString());
        } else if (tq.matches(":has(")) {
            //elements.addAll(root.getAllElements());
        } else {
            //addElements(findElements()); // chomp first element matcher off queue 
        	findElements();
        }            
               
        while (!tq.isEmpty()) {
            // hierarchy and extras
            boolean seenWhite = tq.consumeWhitespace();
            
            if (tq.matchChomp(",")) { // group or

            	while (!tq.isEmpty()) {
                    String subQuery = tq.chompTo(",");
                    
                    
                    
                    //elements.addAll(select(subQuery, root));
                    //select(subQuery);
                }
            } else if (tq.matchesAny(combinators)) {
                combinator(tq.consume().toString());
            } else if (seenWhite) {
                combinator(" ");
            } else { // E.class, E#id, E[attr] etc. AND
                findElements(); // take next el, #. etc off queue
            }
        }
        
        if(s.size() == 1)
        	return s.getFirst();
        
        return new AndSelector(s);
    }
    
    private void combinator(String combinator) {
        tq.consumeWhitespace();
        String subQuery = tq.consumeToAny(combinators); // support multi > childs
        
        

        if (combinator.equals(">")) {
            //output = filterForChildren(elements, select(subQuery, elements));
        } else if (combinator.equals(" ")) {
        	AndSelector a = new AndSelector();
        	a.add(select(subQuery));
        	a.add(new ParentSelector(new AndSelector(s)));
        	s.clear();
        	s.push(a);
        	
        	
            //output = filterForDescendants(elements, select(subQuery, elements));
        } else if (combinator.equals("+")) {
            //output = filterForAdjacentSiblings(elements, select(subQuery, root));
        } else if (combinator.equals("~")) {
            //output = filterForGeneralSiblings(elements, select(subQuery, root));
        } else
            throw new IllegalStateException("Unknown combinator: " + combinator);
        

    }
    
    private void findElements() {
        if (tq.matchChomp("#")) {
            byId();
        } else if (tq.matchChomp(".")) {
            byClass();
        } else if (tq.matchesWord()) {
            byTag();
        } else if (tq.matches("[")) {
            byAttribute();
        } else if (tq.matchChomp("*")) {
            allElements();
        } else if (tq.matchChomp(":lt(")) {
            indexLessThan();
        } else if (tq.matchChomp(":gt(")) {
            indexGreaterThan();
        } else if (tq.matchChomp(":eq(")) {
            indexEquals();
        } else if (tq.matches(":has(")) {
            has();
        } else if (tq.matches(":contains(")) {
            contains(false);
        } else if (tq.matches(":containsOwn(")) {
            contains(true);
        } else if (tq.matches(":matches(")) {
            matches(false);
        } else if (tq.matches(":matchesOwn(")) {
            matches(true);
        } else if (tq.matches(":not(")) {
            not();
        } else { // unhandled
            throw new SelectorParseException("Could not parse query '%s': unexpected token at '%s'", query, tq.remainder());
        }
    }
    

    private void byId() {
        String id = tq.consumeCssIdentifier();
        Validate.notEmpty(id);
        ecPush(new Evaluator.Id(id));
    }

    private void byClass() {
        String className = tq.consumeCssIdentifier();
        Validate.notEmpty(className);
        ecPush(new Evaluator.Class(className));
    }

    private void byTag() {
        String tagName = tq.consumeElementSelector();
        Validate.notEmpty(tagName);
        
        // namespaces: if element name is "abc:def", selector must be "abc|def", so flip:
        if (tagName.contains("|"))
            tagName = tagName.replace("|", ":");
        
        ecPush(new Evaluator.Tag(tagName));
    }

    private void byAttribute() {
        TokenQueue cq = new TokenQueue(tq.chompBalanced('[', ']')); // content queue
        String key = cq.consumeToAny("=", "!=", "^=", "$=", "*=", "~="); // eq, not, start, end, contain, match, (no val)
        Validate.notEmpty(key);
        cq.consumeWhitespace();

        if (cq.isEmpty()) {
            if(key.startsWith("^"))
            	ecPush(new Evaluator.AttributeStarting(key.substring(1)));
            else
            	ecPush(new Evaluator.Attribute(key));
        } else {
        	String value = cq.remainder();
            if (cq.matchChomp("="))
            	ecPush(new Evaluator.AttributeWithValue(key, value));

            else if (cq.matchChomp("!="))
                ecPush(new Evaluator.AttributeWithValueNot(key, value));

            else if (cq.matchChomp("^="))
            	ecPush(new Evaluator.AttributeWithValueStarting(key, value));

            else if (cq.matchChomp("$="))
            	ecPush(new Evaluator.AttributeWithValueEnding(key, value));

            else if (cq.matchChomp("*="))
            	ecPush(new Evaluator.AttributeWithValueContaining(key, value));
            
            else if (cq.matchChomp("~="))
            	ecPush(new Evaluator.AttributeWithValueMatching(key, Pattern.compile(value)));
            
            else
                throw new SelectorParseException("Could not parse attribute query '%s': unexpected token at '%s'", query, cq.remainder());
        }
    }

    private void allElements() {
        //return root.getAllElements();
    	// TODO: add all parsing
    }
    
    // pseudo selectors :lt, :gt, :eq
    private void indexLessThan() {
    	
        ecPush(new Evaluator.IndexLessThan(consumeIndex()));
    }
    
    private void indexGreaterThan() {
    	ecPush(new Evaluator.IndexGreaterThan(consumeIndex()));
    }
    
    private void indexEquals() {
    	ecPush(new Evaluator.IndexEquals(consumeIndex()));
    }

    private int consumeIndex() {
        String indexS = tq.chompTo(")").trim();
        Validate.isTrue(StringUtil.isNumeric(indexS), "Index must be numeric");
        return Integer.parseInt(indexS);
    }

    // pseudo selector :has(el)
    private void has() {
        tq.consume(":has");
        String subQuery = tq.chompBalanced('(',')');
        Validate.notEmpty(subQuery, ":has(el) subselect must not be empty");
        // TODO: add has parsing
    }
    
    // pseudo selector :contains(text), containsOwn(text)
    private void contains(boolean own) {
        tq.consume(own ? ":containsOwn" : ":contains");
        String searchText = TokenQueue.unescape(tq.chompBalanced('(',')'));
        Validate.notEmpty(searchText, ":contains(text) query must not be empty");
        
        // TODO: add :contains parsing
    }
    
    // :matches(regex), matchesOwn(regex)
    private void matches(boolean own) {
        tq.consume(own? ":matchesOwn" : ":matches");
        String regex = tq.chompBalanced('(', ')'); // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, ":matches(regex) query must not be empty");
        
        // TODO: add :matches parsing
        
    }

    // :not(selector)
    private void not() {
        tq.consume(":not");
        String subQuery = tq.chompBalanced('(', ')');
        Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty");

        // TODO: add :not parsing
    }


    public static class SelectorParseException extends IllegalStateException {
        public SelectorParseException(String msg, Object... params) {
            super(String.format(msg, params));
        }
    }
    
    void ecPush(Evaluator e) {
    	Evaluator p = s.peek();

    	if(p == null || !(p instanceof ElementContainerSelector)) {
    		s.push(new ElementContainerSelector().add(e));
    		return;
    	}
    	
    	ElementContainerSelector ec = (ElementContainerSelector) p;
    	ec.add(e);
    }

    
    public static void main(String[] args) {
    	Evaluator e = select("div p href");
	}
    


    
	

}
