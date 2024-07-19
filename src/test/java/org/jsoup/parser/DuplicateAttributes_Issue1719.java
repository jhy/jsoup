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
    public static final String DESIRED_XML_IMG_TAG = "<img src=\"file.png\" name=\"test\" value=\"foo\" type=\"hidden\" />";
    public static final String DESIRED_HTML_IMG_TAG = "<img src=\"file.png\" name=\"test\" value=\"foo\" type=\"hidden\">";

    public static final String IMG_INPUT = "<img src=\"file.png\" name=\"test\" value=\"foo\" type=\"hidden\" value=\"bar\" />";

    public static final String IMG_INPUT_NO_SLASH = "<img src=\"file.png\" name=\"test\" value=\"foo\" type=\"hidden\" value=\"bar\">";


    public static final String AREA_INPUT = "<html>\n" +
            "  <body>\n" +
            "    <a href='#1'>\n" +
            "        <div>\n" +
//                "          <a href='#2' class = 'hello' class = world'>\nh2<\a>" +
            "        </div>\n" +
            "    </a>\n" +
            "<area href='#2' class = 'hello' class = world'>\n"+
            "  </body>\n" +
            "</html>";

    public static final String DESIRED_HTML_AREA_TAG = "<area href=\"#2\" class=\"hello\">";

    @Test
    void parserXML() {
        String doubleTag = IMG_INPUT;
        Parser parser = Parser.xmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");
        System.out.println(doc.html());

        assertThat(doc.selectFirst("img").outerHtml()).isNotBlank().isEqualTo(DESIRED_XML_IMG_TAG);
    }

    @Test
    void parserHTML_IMAGE() {
        String doubleTag = IMG_INPUT;
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");

//        System.out.println(doc.html());
        assertThat(doc.selectFirst("img").outerHtml()).isNotBlank().isEqualTo(DESIRED_HTML_IMG_TAG);
    }

    @Test
    void parserHTML_AREA() {
        String doubleTag = AREA_INPUT;
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");

//        System.out.println(doc.html());
        assertThat(doc.selectFirst("area").outerHtml()).isNotBlank().isEqualTo(DESIRED_HTML_AREA_TAG);
    }

    @Test
    void parserXML_toXML() {
        String doubleTag = IMG_INPUT;
        Parser parser = Parser.xmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        assertThat(doc.selectFirst("img").outerHtml()).isNotBlank().isEqualTo(DESIRED_XML_IMG_TAG);
    }

    @Test
    void parserHTML_toXML() {
        String doubleTag = IMG_INPUT;
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(doubleTag, "");
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        assertThat(doc.selectFirst("img").outerHtml()).isNotBlank().isEqualTo(DESIRED_XML_IMG_TAG);
    }

    @Test
    void jsoupParseToXML() {
        String doubleTag = IMG_INPUT;

        final Document document = Jsoup.parse(doubleTag);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        String outputXhtml = document.html()
                .replaceAll("&nbsp;", "&#160;");// nbsp does not exist in xhtml.

        assertThat(outputXhtml).isNotBlank().isEqualTo(HEAD + DESIRED_XML_IMG_TAG + TRAIL);
    }

    @Test
    void jsoupParseToXML_outerMethod() {
        String doubleTag = IMG_INPUT;

        final Document document = Jsoup.parse(doubleTag);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        String outputXhtml = document.outerHtml()
                .replaceAll("&nbsp;", "&#160;");// nbsp does not exist in xhtml.

        assertThat(outputXhtml).isNotBlank().isEqualTo(HEAD + DESIRED_XML_IMG_TAG + TRAIL);
    }
}