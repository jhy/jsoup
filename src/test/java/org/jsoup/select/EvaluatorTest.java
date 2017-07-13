package org.jsoup.select;

import static org.junit.Assert.assertEquals;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

public class EvaluatorTest {

	@Test
	public void testRemoveBeforeIndex() {
		Document doc = Jsoup.parse(
	            "<html><body><div><p>before1</p><p>before2</p><p>XXX</p><p>after1</p><p>after2</p></div></body></html>",
	            "");
	    Element body = doc.select("body").first();
	    Elements elems = body.select("p:matchesOwn(XXX)");
	    Element xElem = elems.first();
	    Elements beforeX = xElem.parent().getElementsByIndexLessThan(xElem.elementSiblingIndex());

	    for(Element p : beforeX) {
	        p.remove();
	    }

	    assertEquals("<body><div><p>XXX</p><p>after1</p><p>after2</p></div></body>", TextUtil.stripNewlines(body.outerHtml()));
	}
	
	@Test
	public void testRemoveAfterIndex() {
		 Document doc2 = Jsoup.parse(
		            "<html><body><div><p>before1</p><p>before2</p><p>XXX</p><p>after1</p><p>after2</p></div></body></html>",
		            "");
	    Element body = doc2.select("body").first();
	    Elements elems = body.select("p:matchesOwn(XXX)");
	    Element xElem = elems.first();
	    Elements afterX = xElem.parent().getElementsByIndexGreaterThan(xElem.elementSiblingIndex());

	    for(Element p : afterX) {
	        p.remove();
	    }

	    assertEquals("<body><div><p>before1</p><p>before2</p><p>XXX</p></div></body>", TextUtil.stripNewlines(body.outerHtml()));
	}

}
