package org.jsoup;

import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;


public class PrettyPrintTest {


	@Test
	public void test4emptyLineIssue(){
		String html="<span>a</span> \n\n<span>b</span><pre> \n  </pre>";
		final Document doc = Jsoup.parse(html);
		doc.outputSettings().prettyPrint(true);
		String prettyHtml = doc.body().html();
		System.out.println("prettyHtml:"+prettyHtml);
		Assert.assertTrue(!Pattern.compile("(?sm)</span>\\s{2,}<span>").matcher(prettyHtml).find());
		Assert.assertTrue(Pattern.compile("(?s)<pre>\\s{2,}</pre>").matcher(prettyHtml).find());
	}
	
	@Test
	public void test4unkownTag(){
		
		String html="<w:xtag1>Aa</w:xtag1>";
		final Document doc = Jsoup.parse(html);
		doc.outputSettings().prettyPrint(true);
		
		String prettyHtml = doc.body().html();
		System.out.println("prettyHtml:"+prettyHtml);
		Assert.assertEquals(html, prettyHtml);
	
	}
	
	
		@Test
	public void test4prettyPrint1(){
		
		String html="S:<select name='Secret'><option>A</option><option>A</option></select><span>A</span><input name='n1' type='text' />";
		final Document doc = Jsoup.parse(html);
		doc.outputSettings().prettyPrint(true).indentAmount(0);
		
		//formatter should not add new line before/after select/span/input tag, otherwise you'll you layout is different(you'll see one blank between elements)
		String prettyHtml = doc.body().html();
		System.out.println("prettyHtml:"+prettyHtml);
		Assert.assertTrue(!Pattern.compile("(?sm)(span|select|input)>\\s+<(span|select|input)").matcher(prettyHtml).find());
		
	//	Assert.assertEquals(html, prettyHtml);
	
	}
}
