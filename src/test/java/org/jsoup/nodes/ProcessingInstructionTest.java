package org.jsoup.nodes;

import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.CombiningEvaluator;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;
import org.junit.jupiter.api.Test;

import static org.jsoup.nodes.Document.OutputSettings.Syntax.xml;
import static org.junit.jupiter.api.Assertions.*;

class ProcessingInstructionTest {
    @Test void exposesTargetAndData() {
        ProcessingInstruction instruction = new ProcessingInstruction("target", "some data");

        assertEquals("#processing-instruction", instruction.nodeName());
        assertEquals("target", instruction.target());
        assertEquals("some data", instruction.data());
        assertEquals("some data", instruction.nodeValue());
    }

    @Test void serializesForHtmlAndXml() {
        ProcessingInstruction empty = new ProcessingInstruction("target", "");
        ProcessingInstruction data = new ProcessingInstruction("target", "some data");

        assertEquals("<?target ?>", empty.outerHtml());
        assertEquals("<?target some data?>", data.outerHtml());

        Document xmlDocument = new Document("");
        xmlDocument.outputSettings().syntax(xml).prettyPrint(false);
        xmlDocument.appendChild(empty).appendChild(data);
        assertEquals("<?target?><?target some data?>", xmlDocument.outerHtml());
    }

    @Test void parsesDataWhenAttributesAreAccessed() {
        ProcessingInstruction instruction = new ProcessingInstruction("target", "one='1' two = unquoted");

        assertEquals("one='1' two = unquoted", instruction.data());
        assertEquals("1", instruction.attr("one"));
        assertEquals("one=\"1\" two=\"unquoted\"", instruction.data());

        instruction.attr("three", "3").attr("empty", "");
        assertEquals("one=\"1\" two=\"unquoted\" three=\"3\" empty=\"\"", instruction.data());
    }

    @Test void stopsParsingAttributesAtCloser() {
        ProcessingInstruction instruction = new ProcessingInstruction("target", "foo=bar><a href>text");

        assertEquals("bar", instruction.attr("foo"));
        assertEquals("foo=\"bar\"", instruction.data());

        instruction.attr("added", "value");
        assertEquals("foo=\"bar\" added=\"value\"", instruction.data());
    }

    @Test void dataSetterUpdatesParsedAttributes() {
        ProcessingInstruction instruction = new ProcessingInstruction("target", "one='1'");
        Attributes attributes = instruction.attributes();

        instruction.data("Two='2'");

        assertSame(attributes, instruction.attributes());
        assertFalse(attributes.hasKey("one"));
        assertEquals("2", attributes.get("two"));
        assertEquals("two=\"2\"", instruction.data());

        attributes.put("three", "3");
        assertEquals("two=\"2\" three=\"3\"", instruction.data());
    }

    @Test void attributeNamedLikeNodePreservesInstructionData() {
        ProcessingInstruction instruction = new ProcessingInstruction("target", "one='1'");

        instruction.attr(instruction.nodeName(), "value");

        assertEquals("1", instruction.attr("one"));
        assertEquals("value", instruction.attr(instruction.nodeName()));
    }

    @Test void parsesAttributesWithDocumentParser() {
        ProcessingInstruction html = (ProcessingInstruction) Jsoup.parse("<body><?target Mixed='1'>")
            .body().childNode(0);
        ProcessingInstruction parsedXml = (ProcessingInstruction) Jsoup.parse(
            "<root><?target Mixed='1'?></root>",
            Parser.xmlParser()
        ).expectFirst("root").childNode(0);
        ProcessingInstruction detached = new ProcessingInstruction("target", "Mixed='1'");

        html.attributes();
        parsedXml.attributes();
        detached.attributes();

        assertEquals("mixed=\"1\"", html.data());
        assertEquals("Mixed=\"1\"", parsedXml.data());
        assertEquals("mixed=\"1\"", detached.data());
        assertEquals("1", html.attr("mixed"));
        assertEquals("1", parsedXml.attr("Mixed"));
        assertEquals("1", detached.attr("mixed"));
    }

    @Test void parsingAttributesDoesNotChangeDocumentParserErrors() {
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Document document = parser.parseInput("<body></span><?target one='1'>", "");
        int errors = parser.getErrors().size();
        ProcessingInstruction instruction = document.nodeStream(ProcessingInstruction.class).findFirst()
            .orElseThrow(() -> new AssertionError("processing instruction missing"));

        assertTrue(errors > 0);
        instruction.attributes();
        assertEquals(errors, parser.getErrors().size());
    }

    @Test void serializesAttributesWithDocumentSettings() {
        Document html = Jsoup.parse("<body></body>");
        ProcessingInstruction htmlInstruction = new ProcessingInstruction("target", "checked=''");
        html.body().appendChild(htmlInstruction);
        htmlInstruction.attributes().put("added", "1");

        assertEquals("checked added=\"1\"", htmlInstruction.data());
        assertEquals("<?target checked added=\"1\"?>", htmlInstruction.outerHtml());

        Document xmlDoc = Jsoup.parse("<root/>", Parser.xmlParser());
        ProcessingInstruction xmlInstruction = new ProcessingInstruction("target", "checked=''");
        xmlDoc.expectFirst("root").appendChild(xmlInstruction);
        xmlInstruction.attributes().put("added", "1");

        assertEquals("checked=\"\" added=\"1\"", xmlInstruction.data());
        assertEquals("<?target checked=\"\" added=\"1\"?>", xmlInstruction.outerHtml());
    }

    @Test void serializesAttributesWithUniqueRepairedNames() {
        ProcessingInstruction instruction = new ProcessingInstruction("target", "");
        instruction.attributes()
            .put("a b", "1")
            .put("a_b", "2");

        assertEquals("_a_b=\"1\" a_b=\"2\"", instruction.data());
    }

    @Test void cloneIsIndependent() {
        ProcessingInstruction original = new ProcessingInstruction("target", "one='1'");
        original.attr("metadata", "original");
        ProcessingInstruction clone = original.clone();

        clone.data("two='2'").attr("metadata", "clone");

        assertEquals("one=\"1\" metadata=\"original\"", original.data());
        assertEquals("original", original.attr("metadata"));
        assertEquals("two=\"2\" metadata=\"clone\"", clone.data());
        assertEquals("clone", clone.attr("metadata"));
    }

    @Test void htmlAndXmlParsersCreateProcessingInstructions() {
        ProcessingInstruction html = (ProcessingInstruction) Jsoup.parse("<body><?target one='1'>")
            .body().childNode(0);
        ProcessingInstruction parsedXml = (ProcessingInstruction) Jsoup.parse(
            "<root><?target one='1'?></root>",
            Parser.xmlParser()
        ).expectFirst("root").childNode(0);

        assertEquals("one='1'", html.data());
        assertEquals("one='1'", parsedXml.data());
    }

    @Test void movesTemplateContentToEarlierMarker() {
        String html = "<select id=countries><?marker name=country-options?></select>\n" +
            "<template for=country-options><option>Antigua</option><option>Barbuda</option></template>";
        Document document = Jsoup.parse(html);
        Element select = document.expectFirst("select");
        Element template = document.expectFirst("template[for]");
        ProcessingInstruction marker = document.expectFirstNode(
            "::pi(marker)[name=country-options]", ProcessingInstruction.class);

        assertEquals(template.attr("for"), marker.attr("name"));

        marker.replaceWith(template);
        template.unwrap();

        document.outputSettings().prettyPrint(false);
        assertEquals("<select id=\"countries\"><option>Antigua</option><option>Barbuda</option></select>", select.outerHtml());
    }

    @Test void replacesAllTemplateMarkers() {
        String html = "<main>" +
            "<select id=countries><?marker name=country-options?></select>" +
            "<ul id=animals><?marker name=animal-items?></ul>" +
            "</main>" +
            "<template for=animal-items><li>Alpaca</li><li>Bear</li></template>" +
            "<template for=country-options><option>Antigua</option><option>Barbuda</option></template>";
        Document document = Jsoup.parse(html);
        Evaluator markerTarget = Selector.evaluatorOf("::pi(marker)");

        for (Element template : document.select("template[for]")) {
            // Keep the dynamic name out of the selector string, so it does not need CSS escaping.
            Evaluator markerEvaluator = new CombiningEvaluator.And(Arrays.asList(
                markerTarget,
                new Evaluator.AttributeWithValue("name", template.attr("for"))
            ));
            ProcessingInstruction marker = document.selectFirstNode(markerEvaluator, ProcessingInstruction.class);
            assertNotNull(marker);

            marker.replaceWith(template);
            template.unwrap();
        }

        document.outputSettings().prettyPrint(false);
        assertEquals("<main><select id=\"countries\"><option>Antigua</option><option>Barbuda</option></select>" +
            "<ul id=\"animals\"><li>Alpaca</li><li>Bear</li></ul></main>", document.body().html());
    }

    @Test void processingInstructionDoesNotMakeAnElementNonEmpty() {
        Document document = Jsoup.parse("<div><?target data></div><div>text</div>");

        assertEquals(1, document.select("div:empty").size());
        assertTrue(document.select("div:empty").first().childNode(0) instanceof ProcessingInstruction);
    }
}
