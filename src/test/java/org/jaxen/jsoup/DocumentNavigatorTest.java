package org.jaxen.jsoup;

import java.util.List;


import org.jaxen.Navigator;
import org.jaxen.XPath;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.junit.Test;

public class DocumentNavigatorTest {

    private String testUrl = "http://google.com";
    private String[] xpaths = new String[]{
        "/html/body",
        "//body",
        "/html/body/../head",
        "/html/head/title/text()",
        "//div[@class='footer']",
        "//div[@class]"
    };

    @Test
    public void parsingVisitor() throws Exception {
        Navigator navigator = DocumentNavigator.getInstance();
        Document doc = (Document) navigator.getDocument(testUrl);
        for (int i = 0; i < xpaths.length; i++) {
            String xpath = xpaths[i];
            System.out.println("*** Evaluating: " + xpath);
            XPath expr = new JsoupXPath(xpath, navigator);
            Object result = expr.evaluate(doc);
            if (result instanceof Element) {
                System.out.println("Element: " + ((Element) result).tagName());
            } else if (result instanceof List) {
                System.out.println("List: size=" + ((List) result).size());
                List elements = (List) result;
                for (int j = 0; j < elements.size(); j++) {
                    if (elements.get(j) instanceof Element) {
                        Element element = (Element) elements.get(j);
                        System.out.println("Element: " + ((Element) element).tagName());
                    } else if (elements.get(j) instanceof TextNode) {
                        TextNode element = (TextNode) elements.get(j);
                        System.out.println("TextNode: " + element.getWholeText());
                    }
                }
            } else if (result instanceof String) {
                System.out.println("String: " + ((String) result));
            } else if (result instanceof Number) {
                System.out.println("Number: " + ((Number) result));
            } else if (result instanceof Boolean) {
                System.out.println("Boolean: " + ((Boolean) result));
            } else {
                System.out.println("Unknown: " + result == null
                        ? "NULL" : result.getClass().getName());
            }
        }
    }
}
