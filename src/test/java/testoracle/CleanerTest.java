package testoracle;

import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CleanerTest {
 
	/*
	 * Test clean() 
	 * Input : simpleText() tag with other tag
	 * expected : remain simpleText() tag string
	 */
	
	@Test
	public void testCleanSimpleTextWithOthers() {
		Cleaner cleaner = new Cleaner(Whitelist.simpleText());
		String dirtyDocument = "<div><p></p></div><a href='http://naver.com'>Test<b id=test>String</b></a>";
        Document dirtydoc = Jsoup.parse(dirtyDocument);
		Document cleanDocument = cleaner.clean(dirtydoc);
		assertEquals("Test\n<b>String</b>", cleanDocument.body().html());
	}
	
	/*
	 * Test clean()
	 * Input : simpleText() tag without other tag
	 * expected : remain simpleText() tag string
	 */
	
	@Test
	public void testCleanSimpleTextNoOthers() {
		Cleaner cleaner = new Cleaner(Whitelist.simpleText());
		String dirtyDocument = "<b id=test>String</b>Test";
        Document dirtydoc = Jsoup.parse(dirtyDocument);
		Document cleanDocument = cleaner.clean(dirtydoc);
		assertEquals("<b>String</b>Test", cleanDocument.body().html());
	}
	
	/*
	 * Test iaValid()
	 * Input : simple valid case
	 * expected : true
	 */
	
	@Test
	public void testValidTrue() {
		Cleaner cleaner = new Cleaner(Whitelist.simpleText());
		String dirtyDocument = "<b>String</b>";
        Document dirtydoc = Jsoup.parse(dirtyDocument);
        assertEquals(true, cleaner.isValid(dirtydoc));
	}

	/*
	 * Test isValid()
	 * Input : simple invalid case
	 * expected : false
	 */
	
	@Test
	public void testValidFalse() {
		Cleaner cleaner = new Cleaner(Whitelist.simpleText());
		String dirtyDocument = "<div><b>String</b>";
        Document dirtydoc = Jsoup.parse(dirtyDocument);
        assertEquals(false, cleaner.isValid(dirtydoc));
	}
	
	/*
	 * Test isValidHtml()
	 * Input : simple valid case
	 * expected : true
	 */
	
	@Test
	public void testValidHtmlTrue() {
		Cleaner cleaner = new Cleaner(Whitelist.simpleText());
		String dirtyDocument = "<b>Test String</b>";
		assertEquals(true, cleaner.isValidBodyHtml(dirtyDocument));
	}
	
	/*
	 * Test isValidHtml()
	 * Input : simple invalid case
	 * expected : false
	 */
	
	@Test
	public void testValidHtmlFalse() {

		Cleaner cleaner = new Cleaner(Whitelist.simpleText());
		String dirtyDocument = "<div><b>Test String</b>";
		assertEquals(false, cleaner.isValidBodyHtml(dirtyDocument));
	}
	

}
