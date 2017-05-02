package testoracle;

import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryParserTest {

	/*
	 * Test select by tag
	 * Input : html code string, tag
	 * expected : information matched by tag
	 */
	
	@Test
	public void testFindElementAllElement() {
		Elements elementAll = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select("*p");
        assertEquals(3, elementAll.size());
        assertEquals("0", elementAll.get(0).id());
        assertEquals("1", elementAll.get(1).id());
	}
	
	/*
	 * Test keyword Less than
	 * Input : test string
	 * expected : information matched by index
	 */
	
	@Test
	public void testFindElementLtElement() {
		Elements elementLt = Jsoup.parse("<p><p><p><p><p></p></p></p></p></p>").select(":lt(3)");
        assertEquals(7, elementLt.size());
	}
	
	/*
	 * Test keyword Grater than
	 * Input : test string
	 * expected : information matched by index
	 */
	
	@Test
	public void testFindElementGtElement() {
		Elements elementGt = Jsoup.parse("<p><p><p><p><p></p></p></p></p></p>").select(":gt(7)");
		assertEquals(1, elementGt.size());
	}
	
	/*
	 * Test keyword equal
	 * Input : test string
	 * expected : information matched by index
	 */
	
	@Test
	public void testFindElementEqElement() {
		Elements elementEq = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select(":eq(2)");
		assertEquals(1, elementEq.size());
	}
	
	/*
	 * Test keyword has
	 * Input : html code string, test case
	 * expected : matched size
	 */
	
	@Test
	public void testFindElementHasElement() {
		Elements elementHas = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'><div></div>").select(":has(div > p)");
		assertEquals(0, elementHas.size());
	}
	
	/*
	 * Test keyword contain
	 * Input : html code string, test case
	 * expected : matched size
	 */
	
	@Test
	public void testFindElementContainElement() {
		Elements elementContain = Jsoup.parse("<div><p>1<p>2<p>3<p>4<p>5<p>6</div><div><p>7<p>8<p>9<p>10<p>11<p>12</div>").select("p:contains(5)");
		assertEquals(1, elementContain.size());
	}
	
	/*
	 * Test keyword containOwn
	 * Input : html code string, test case
	 * expected : matched size
	 */
	
	@Test
	public void testFindElementContainOwnElement() {
		Elements elementCO = Jsoup.parse("<p id=0 class='ONE two'>test<p id=1 class='one'><p id=2 class='two'>").select("p:containsOwn(test)");
		assertEquals(1, elementCO.size());
	}
	
	/*
	 * Test keyword containData
	 * Input : html code string, test case
	 * expected : matched size
	 */
	
	@Test
	public void testFindElementContainDataElement() {
		Elements elementCD = Jsoup.parse("<p id=0 class='ONE two'>hello</p><p id=1 class='one'><p id=2 class='two'>").select("p:containsData(bye)");
		assertEquals(0, elementCD.size());
	}
	
	/*
	 * Test keyword match
	 * Input : html code string, test case
	 * expected : matched size
	 */
	
	@Test
	public void testFindElementMatchElement() {
		Elements elementMatch = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select("p:matches(#3)");
		assertEquals(0, elementMatch.size());
		
	}
	
	/*
	 * Test keyword matchOwn
	 * Input : html code string, test case
	 * expected : matched size 
	 */
	
	@Test
	public void testFindElementMatchOwnElement() {
		Elements elementMO = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select("p:matchesOwn(#3)");
		assertEquals(0, elementMO.size());
		
	}
	
	/*
	 * Test keyword not
	 * Input : html code string, test case
	 * expected : matched size 
	 */
	
	@Test
	public void testFindElementNotElement() {
		Elements elementNot = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select(":not(one)");
		assertEquals(7, elementNot.size());
		
	}
	
	/*
	 * Test keyword nth-child
	 * Input : html code string, test case
	 * expected : matched size 
	 */
	
	@Test
	public void testFindElementNCElement() {
		Elements elementNC = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select(":nth-child(1)");
		assertEquals(2, elementNC.size());
	}
	
	/*
	 * Test keyword nth-last-child
	 * Input : html code string, test case
	 * expected : matched size 
	 */
	
	@Test
	public void testFindElementNLCElement() {
		Elements elementNLC = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select(":nth-last-child(1)");
		assertEquals(2, elementNLC.size());
		
	}
	
	/*
	 * Test keyword nth-of-type
	 * Input : html code string, test case
	 * expected : matched size 
	 */
	
	@Test
	public void testFindElementNoTElement() {
		Elements elementNoT = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select(":nth-of-type(3)");
		assertEquals(1, elementNoT.size());
		
	}
	
	/*
	 * Test keyword nth-last-of-type
	 * Input : html code string, test case
	 * expected : matched size 
	 */
	
	@Test
	public void testFindElementNLoTElement() {
		Elements elementNLoT = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select(":nth-last-of-type(1)");
		assertEquals(3, elementNLoT.size());
		
	}
	
}
