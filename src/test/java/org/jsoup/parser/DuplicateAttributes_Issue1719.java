package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicateAttributes_Issue1719 {
    private static final String HEAD = "<html>\n" +
            " <head></head>\n" +
            " <body>\n" +
            "  ";
    private static final String TRAIL = "\n" +
            " </body>\n" +
            "</html>";
    public static final String DESIRED_XML_TAG = "<img src=\"file.png\" name=\"test\" value=\"foo\" type=\"hidden\" />";
    public static final String DESIRED_HTML_TAG = "<img src=\"file.png\" name=\"test\" value=\"foo\" type=\"hidden\">";

    public static final String INPUT = "<img src=\"file.png\" name=\"test\" value=\"foo\" type=\"hidden\" value=\"bar\" />";

    public static final String INPUT_NO_SLASH = "<img src=\"file.png\" name=\"test\" value=\"foo\" type=\"hidden\" value=\"bar\">";

    @Test
    void parserXML() {
        String doubleTag = INPUT;
        Parser parser = Parser.xmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");
        System.out.println(doc.html());

        assertThat(doc.selectFirst("img").outerHtml()).isNotBlank().isEqualTo(DESIRED_XML_TAG);
    }

    @Test
    void parserHTML() {
        String doubleTag = INPUT;
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");

        System.out.println(doc.html());
        assertThat(doc.selectFirst("img").outerHtml()).isNotBlank().isEqualTo(DESIRED_HTML_TAG);
    }

    @Test
    void parserXML_toXML() {
        String doubleTag = INPUT;
        Parser parser = Parser.xmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        assertThat(doc.selectFirst("img").outerHtml()).isNotBlank().isEqualTo(DESIRED_XML_TAG);
    }

    @Test
    void parserHTML_toXML() {
        String doubleTag = INPUT;
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        assertThat(doc.selectFirst("img").outerHtml()).isNotBlank().isEqualTo(DESIRED_XML_TAG);
    }

    @Test
    void jsoupParseToXML() {
        String doubleTag = INPUT;

        final Document document = Jsoup.parse(doubleTag);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        String outputXhtml = document.html()
                .replaceAll("&nbsp;", "&#160;");// nbsp does not exist in xhtml.

        assertThat(outputXhtml).isNotBlank().isEqualTo(HEAD + DESIRED_XML_TAG + TRAIL);
    }

    @Test
    void jsoupParseToXML_outerMethod() {
        String doubleTag = INPUT;

        final Document document = Jsoup.parse(doubleTag);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        String outputXhtml = document.outerHtml()
                .replaceAll("&nbsp;", "&#160;");// nbsp does not exist in xhtml.

        assertThat(outputXhtml).isNotBlank().isEqualTo(HEAD + DESIRED_XML_TAG + TRAIL);
    }
}