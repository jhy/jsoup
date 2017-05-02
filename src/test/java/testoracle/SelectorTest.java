package testoracle;

import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SelectorTest {

	/*
	 * Test if select tag exist
	 * Input : with string id, integer id and other id input
	 * expected : each id
	 */
	
	@Test
	public void testInputTagResultExist1() {
		Elements elementDIV = Jsoup.parse("<div id=1><div id=testString><p>Hello</p></div></div><DIV id=!#@!$%^&*>").select("DIV");
        assertEquals(3, elementDIV.size());
        assertEquals("1", elementDIV.get(0).id());
        assertEquals("testString", elementDIV.get(1).id());
        assertEquals("!#@!$%^&*", elementDIV.get(2).id());
        
	}
	
	/*
	 * Test if select tag exist
	 * Input : string with tag
	 * expected : string in <p> tag "Hello"
	 */
	
	@Test
	public void testInputTagResultExist2() {
        Elements elementP = Jsoup.parse("<div id=1><div id=testString><p>Hello</p></div></div><DIV id=#@!$%^&*>").select("p");
        assertEquals(1, elementP.size());
        assertEquals("Hello", elementP.get(0).text());
	}

	/*
	 * Test if select tag not exist
	 * Input : string without select tag
	 * expected : no result
	 */
	
	@Test
	public void testInputTagResultNotExist() {
		Elements noSame = Jsoup.parse("<div id=1><div id=testString></div></div><DIV id=#@!$%^&*>").select("p");
        assertEquals(0, noSame.size());
	}
	
	/*
	 * Test select id is exist
	 * Input : string that have each id
	 * expected : size and string selected id
	 */
	
	@Test
	public void testInputIDResultExist1() {
		
		Elements elementIDNum1 = Jsoup.parse("<div id=testString><p id=1>Hello</p></div><DIV id=#@!$%^&*>").select("#1");
        assertEquals(1, elementIDNum1.size());
        assertEquals("Hello", elementIDNum1.get(0).text());

	}

	/*
	 * Test select id is exist
	 * Input : string that have each id
	 * expected : size and string selected id
	 */
	
	@Test
	public void testInputIDResultExise2() {

        Elements elementIDString = Jsoup.parse("<div id=testString><p id=call>Hello</p></div><DIV id=#@!$%^&*>").select("#call");
        assertEquals(1, elementIDString.size());
        assertEquals("Hello", elementIDString.get(0).text());
        
	}
	
	/*
	 * Test exception
	 * Input : exception creator
	 * expected : call exception
	 */
	
	@Test(expected = Exception.class)  
	public void SelectorException1() {  

		Elements elementID = Jsoup.parse("<div id=testString><p id=!@#$%>Hello</p></div><DIV id=#@!$%^&*>").select("!@#$%");
        //call exception
	}

	/*
	 * Test exception
	 * Input : exception creator
	 * expected : call exception
	 */
	
	@Test(expected = Exception.class)  
	public void SelectorException2() {  

        Elements elementIDNum2 = Jsoup.parse("<div id=testString><p id=#1>Hello</p></div><DIV id=#@!$%^&*>").select("##1");
        //call exception
	}

	/*
	 * Test exception
	 * Input : exception creator
	 * expected : call exception
	 */
	
	@Test(expected = Exception.class)  
	public void SelectorException3() {  

        Elements callException = Jsoup.parse("<div class=!exception>One</div> <div>Two</div>").select(".!exception");
        //call exception
	}
    
	/*
	 * Test no match id
	 * Input : string without matching id
	 * expected : no result
	 */
	
	@Test
	public void testInputIDResultNotExist() {
		Elements noSame = Jsoup.parse("<div id=testString><p id=call>Bye</p></div><DIV id=#@!$%^&*>").select("nocall");
        assertEquals(0, noSame.size());
	}

	/*
	 * Test select class
	 * Input : string with selected class
	 * expected : string matched class
	 */
	
	@Test
	public void testInputClassResultExist1() {
		Elements elementClassString = Jsoup.parse("<div class=logo>One</div> <div>Two</div>").select(".logo");
        assertEquals(1, elementClassString.size());
        assertEquals("One", elementClassString.get(0).text());
	}
	
	/*
	 * Test select class
	 * Input : string with selected class
	 * expected : string matched class
	 */
	
	@Test
	public void testInputClassResultExist2() {
        Elements elementClassNum = Jsoup.parse("<div class=123>One</div> <div>Two</div>").select(".123");
        assertEquals(1, elementClassNum.size());
        assertEquals("One", elementClassNum.get(0).text());
        
	}
	
	/*
	 * Test tag and class
	 * Input : htmlcode string
	 * expected : matched string
	 */
	
	@Test
	public void testInputTagClassResultExist1() {
		String htmlCode = "<div class=logo>One</div> <div>Two</div><p class=logo>Two</p>";
		Elements elementDiv = Jsoup.parse(htmlCode).select("div.logo");
		
		assertEquals(1, elementDiv.size());
		assertEquals("One", elementDiv.get(0).text());
	}

	/*
	 * Test tag and class
	 * Input : htmlcode string
	 * expected : matched string
	 */
	
	@Test
	public void testInputTagClassResultExist2() {
		String htmlCode = "<div class=logo>One</div> <div>Two</div><p class=logo>Two</p>";
		Elements elementP = Jsoup.parse(htmlCode).select("p.logo");
		
		assertEquals(1, elementP.size());
		assertEquals("Two", elementP.get(0).text());
	}
	

	/*
	 * Test not exist matched class
	 * Input : htmlcode string
	 * expected : no result
	 */
	
	@Test
	public void testInputClassResultNotExist() {
		Elements noSame = Jsoup.parse("<div class=logo>One</div> <div>Two</div>").select(".noselect");
        assertEquals(0, noSame.size());
	}
	
	/*
	 * Test not() instruction
	 * Input : htmlcode string
	 * expected : matched size and string
	 */
	
	
	@Test
	public void testNotInstruction() {
		Elements elementNot = Jsoup.parse("<div class=logo>One</div> <div>Two</div>").select("div").not(".logo");
        assertEquals(1, elementNot.size());
        assertEquals("Two", elementNot.get(0).text());
	}
	
	/*
	 * Test id not exist
	 * Input : htmlcode string
	 * expected : no result
	 */
	
	
	@Test
	public void testInputIDResultNotExis() {
		Elements noSame = Jsoup.parse("<div id=testString><p id=call>Bye</p></div><DIV id=#@!$%^&*>").select("nocall");
        assertEquals(0, noSame.size());
	}
	
	
}
