package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateTest {
    @Test
    public void testParseTemplateWithTrTd() {
        String html = "<template id=\"lorem-ipsum\">\n" +
                " <tr>\n" +
                "    <td>Lorem</td>\n" +
                "    <td>Ipsum</td>\n" +
                " </tr>\n" +
                "</template>";


        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);


        assertEquals("<html><head></head><body><template id=\"lorem-ipsum\">\n" +
                " <tr>\n" +
                "    <td>Lorem</td>\n" +
                "    <td>Ipsum</td>\n" +
                " </tr>\n" +
                "</template></body></html>", doc.outerHtml());
        System.out.println(doc.outerHtml());
    }

    @Test
    public void testParseTemplateWidthTd() {
        String html = "<template id=\"lorem-ipsum\">\n" +
                "    <td>Lorem</td>\n" +
                "    <td>Ipsum</td>\n" +
                "</template>";


        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);


        assertEquals("<html><head></head><body><template id=\"lorem-ipsum\">\n" +
                "    <td>Lorem</td>\n" +
                "    <td>Ipsum</td>\n" +
                "</template></body></html>", doc.outerHtml());
        System.out.println(doc.outerHtml());
    }
    
    @Test
    public void testParseTemplateWidthTr() {
        String html = "<template id=\"lorem-ipsum\">\n" +
                "    <tr>Lorem</tr>\n" +
                "</template>";


        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);


        assertEquals("<html><head></head><body><template id=\"lorem-ipsum\">\n" +
                "    <tr>Lorem</tr>\n" +
                "</template></body></html>", doc.outerHtml());
        System.out.println(doc.outerHtml());
    }
}
