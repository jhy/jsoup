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
}
