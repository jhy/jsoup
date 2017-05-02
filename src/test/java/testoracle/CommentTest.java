package testoracle;

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

	/*
	@Test
	public void testNodeName() {
		fail("Not yet implemented");
	}

	@Test
	public void testOuterHtmlHead() {
		fail("Not yet implemented");
	}

	@Test
	public void testOuterHtmlTail() {
		fail("Not yet implemented");
	}

	@Test
	public void testToString() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetData() {
		fail("Not yet implemented");
	}
	*/

}
