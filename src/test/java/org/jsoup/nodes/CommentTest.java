package org.jsoup.nodes;

import static org.junit.Assert.*;

import org.jsoup.nodes.Comment;
import org.junit.Test;

public class CommentTest {
	
	@Test
	public void testComment() {
		Comment c = new Comment("This is wjdebug comment", "https://jsoup.org");
		assertEquals("This is wjdebug comment", c.getData());
		assertEquals("#comment", c.nodeName());
		assertEquals("\n<!--This is wjdebug comment-->", c.toString());
	}


}
