package testoracle;

import static org.junit.Assert.*;

import org.jsoup.nodes.DataNode;
import org.junit.Test;

public class DataNodeTest {

	@Test
	public void testDataNode() {
		DataNode dn = new DataNode("wjdebug", "https://jsoup.org");
		assertEquals("wjdebug", dn.getWholeData());
		assertEquals("#data", dn.nodeName());
		assertEquals("wjdebug", dn.toString());
		
		dn.setWholeData("Jsoup");
		assertEquals("Jsoup", dn.getWholeData());
		assertEquals("#data", dn.nodeName());
		assertEquals("Jsoup", dn.toString());
		
		DataNode dnFromEncoded = dn.createFromEncoded("HTML encoded data", "http://holinder4s.tistory.com");
		assertEquals("HTML encoded data", dnFromEncoded.getWholeData());
		assertEquals("#data", dnFromEncoded.nodeName());
		assertEquals("HTML encoded data", dnFromEncoded.toString());
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
	public void testGetWholeData() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetWholeData() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateFromEncoded() {
		fail("Not yet implemented");
	}
	*/

}
