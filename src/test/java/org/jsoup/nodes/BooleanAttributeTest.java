package org.jsoup.nodes;

import static org.junit.Assert.*;

import org.jsoup.nodes.BooleanAttribute;
import org.junit.Test;

public class BooleanAttributeTest {
	
	@Test
	public void testBooleanAttribute() {
		BooleanAttribute ba = new BooleanAttribute("Tot");
		assertEquals("Tot", ba.getKey());
		assertEquals("", ba.getValue());
	}

}
