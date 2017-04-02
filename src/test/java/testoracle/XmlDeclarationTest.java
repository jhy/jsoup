package testoracle;

import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;
import org.junit.Test;

public class XmlDeclarationTest {

	@Test
	public void TestXmlDeclarationTrue() {
		XmlDeclaration xml = new XmlDeclaration("name","http://jsoup.co.kr",true);
		
		assertEquals("<!name!>", xml.toString());
		assertEquals("name", xml.name().toString());
		assertEquals("",xml.getWholeDeclaration().toString());
	}
	
	@Test
	public void TestXmlDeclarationFalse(){
		XmlDeclaration xml = new XmlDeclaration("name","http://jsoup.co.kr",false);
		
		assertEquals("<?name?>", xml.toString());
		assertEquals("name", xml.name().toString());
		assertEquals("",xml.getWholeDeclaration().toString());
	}

}
