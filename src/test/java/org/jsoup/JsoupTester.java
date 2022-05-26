package org.jsoup;

import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class JsoupTester {
    public static void main(String[] args) {

        String html = "<html><head><title>Sample Title</title></head>"
                + "<body><p>Sample Content</p></body></html>";
        Document document = Jsoup.parse(html);
        System.out.println(document.title());
        Element element = document.body();
        TextNode text = new TextNode("text");
        CDataNode cdata = new CDataNode("test");
        XmlDeclaration xml = new XmlDeclaration("xml",true);
        Comment comment = new Comment("data");
        DocumentType dt = new DocumentType("type","1","1");
        DataNode dataNode = new DataNode("data");

        System.out.printf("%d, " + element.nodeName() + "\n", element.nodeType());
        System.out.printf("%d, " + text.nodeName() + "\n", text.nodeType());
        System.out.printf("%d, " + cdata.nodeName() + "\n", cdata.nodeType());
        System.out.printf("%d, " + xml.nodeName() + "\n", xml.nodeType());
        System.out.printf("%d, " + comment.nodeName() + "\n", comment.nodeType());
        System.out.printf("%d, " + document.nodeName() + "\n", document.nodeType());
        System.out.printf("%d, " + dt.nodeName() + "\n", dt.nodeType());
        System.out.printf("%d, " + dataNode.nodeName() + "\n", dataNode.nodeType());

    }
}

