package org.jsoup.select.ng.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;
import org.jsoup.parser.TokenQueue;
import org.jsoup.select.ng.AndSelector;
import org.jsoup.select.ng.BasicSelector;
import org.jsoup.select.ng.ElementContainerSelector;
import org.jsoup.select.ng.HasSelector;
import org.jsoup.select.ng.ImmediateParentSelector;
import org.jsoup.select.ng.NotSelector;
import org.jsoup.select.ng.OrSelector;
import org.jsoup.select.ng.ParentSelector;
import org.jsoup.select.ng.PrevSiblingSelector;
import org.jsoup.select.ng.PreviousSequentSiblingSelector;
import org.jsoup.select.ng.SelectMatch;

public class Parser {
	TokenQueue tq;
    private final static String[] combinators = {",", ">", "+", "~", " "};
    String query;
    Deque<Evaluator> s = new ArrayDeque<Evaluator>();
    
    
    
    public Parser(String query) {
    	this.query = query;
    	this.tq = new TokenQueue(query);
    }
    
    public static Evaluator parse(String query) {
    	Parser p = new Parser(query);
    	return p.parse();
    }
    
    public Evaluator parse() {
        tq.consumeWhitespace();
        
        if (tq.matchesAny(combinators)) { // if starts with a combinator, use root as elements
            //elements.add(root);
            combinator(tq.consume());
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
            	OrSelector or = new OrSelector(s);
            	s.clear();
            	s.push(or);
            	while (!tq.isEmpty()) {
                    String subQuery = tq.chompTo(",");
                    or.add(parse(subQuery));
                }
            } else if (tq.matchesAny(combinators)) {
                combinator(tq.consume());
            } else if (seenWhite) {
                combinator(' ');
            } else { // E.class, E#id, E[attr] etc. AND
                findElements(); // take next el, #. etc off queue
            }
        }
        
        if(s.size() == 1)
        	return s.getFirst();
        
        return new AndSelector(s);
    }
    
    private void combinator(char combinator) {
        tq.consumeWhitespace();
        String subQuery = tq.consumeToAny(combinators); // support multi > childs
        
        
        Evaluator e = null;
        
        if(s.size() == 1)
        	e = s.pop();
        else {
        	e = new AndSelector(s);
        	s.clear();
        }
        Evaluator f = parse(subQuery);
        

        if (combinator == '>') {
        	s.push(BasicSelector.and(f, new ImmediateParentSelector(e)));
        } else if (combinator == ' ') {
        	s.push(BasicSelector.and(f, new ParentSelector(e)));
        } else if (combinator == '+') {
        	s.push(BasicSelector.and(f, new PrevSiblingSelector(e)));
        } else if (combinator == '~') {
        	s.push(BasicSelector.and(f, new PreviousSequentSiblingSelector(e)));
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
            if (cq.matchChomp("="))
            	ecPush(new Evaluator.AttributeWithValue(key, cq.remainder()));

            else if (cq.matchChomp("!="))
                ecPush(new Evaluator.AttributeWithValueNot(key, cq.remainder()));

            else if (cq.matchChomp("^="))
            	ecPush(new Evaluator.AttributeWithValueStarting(key, cq.remainder()));

            else if (cq.matchChomp("$="))
            	ecPush(new Evaluator.AttributeWithValueEnding(key, cq.remainder()));

            else if (cq.matchChomp("*="))
            	ecPush(new Evaluator.AttributeWithValueContaining(key, cq.remainder()));
            
            else if (cq.matchChomp("~="))
            	ecPush(new Evaluator.AttributeWithValueMatching(key, Pattern.compile(cq.remainder())));
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
        s.push(new HasSelector(parse(subQuery)));
        


    }
    
    // pseudo selector :contains(text), containsOwn(text)
    private void contains(boolean own) {
        tq.consume(own ? ":containsOwn" : ":contains");
        String searchText = TokenQueue.unescape(tq.chompBalanced('(',')'));
        Validate.notEmpty(searchText, ":contains(text) query must not be empty");
        if(own)
        	s.push(new Evaluator.ContainsOwnText(searchText));
        else
        	s.push(new Evaluator.ContainsText(searchText));
    }
    
    // :matches(regex), matchesOwn(regex)
    private void matches(boolean own) {
        tq.consume(own? ":matchesOwn" : ":matches");
        String regex = tq.chompBalanced('(', ')'); // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, ":matches(regex) query must not be empty");
        
        if(own)
        	s.push(new Evaluator.MatchesOwn(Pattern.compile(regex)));
        else
        	s.push(new Evaluator.Matches(Pattern.compile(regex)));

        
    }

    // :not(selector)
    private void not() {
        tq.consume(":not");
        String subQuery = tq.chompBalanced('(', ')');
        Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty");
        
        s.push(new NotSelector(parse(subQuery)));
    }


    public static class SelectorParseException extends IllegalStateException {
        public SelectorParseException(String msg, Object... params) {
            super(String.format(msg, params));
        }
    }
    
    void ecPush(Evaluator e) {
    	/*Evaluator p = s.peek();

    	if(p == null || !(p instanceof ElementContainerSelector)) {
    		s.push(new ElementContainerSelector().add(e));
    		return;
    	}
    	
    	ElementContainerSelector ec = (ElementContainerSelector) p;*/
    	//ec.add(e);
    	s.push(e);
    }

    
    public static void main(String[] args) {
        // make sure doesn't get nested
        Document doc = Jsoup.parse("<div id=1><div id=2><div id=3></div></div></div>");
        Element div = SelectMatch.match(SelectMatch.match(doc, Parser.parse("div")), Parser.parse(" > div")).first();
	}
    


    
	

}
