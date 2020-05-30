package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.exception.ItemChangedException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the exception of item change during the trversoring.
 * @author Chaozu Zhang 
 * @version 1.0
 */
public class issue1289Test {
    private static final String html =
            "<html>\n" +
                    "<head>\n" +
                    "    <link href=\"css/test.css\" rel=\"stylesheet\">\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "</body>\n" +
                    "</html>";

    @Test
    @Order(1)
    public void ItemChangedExceptionCatchTest() {
        Document document = Jsoup.parse(html);
        try {
            document.traverse(new NodeVisitor() {
                @Override
                public void head(Node node, int depth) {
                    if (node instanceof Element) {
                        Element element = (Element) node;
                        if ("link".equals(element.tagName())) {
                            Element style = element.ownerDocument().createElement("style");
                            element.replaceWith(style);
                        }
                    }
                }

                @Override
                public void tail(Node node, int depth) {
                }
            });
        } catch (ItemChangedException e) {
            assert (true);
        }
    }

    @Test
    @Order(2)
    public void DonotChangeMessageMessageTest() {
        Document document = Jsoup.parse(html);
        try {
            document.traverse(new NodeVisitor() {
                @Override
                public void head(Node node, int depth) {
                    if (node instanceof Element) {
                        Element element = (Element) node;
                        if ("link".equals(element.tagName())) {
                            Element style = element.ownerDocument().createElement("style");
                            element.replaceWith(style);
                        }
                    }
                }

                @Override
                public void tail(Node node, int depth) {
                }
            });
        } catch (ItemChangedException e) {
            assertEquals(e.getMessage(), "Dont change the elements when traversing.");
        }
    }

}
