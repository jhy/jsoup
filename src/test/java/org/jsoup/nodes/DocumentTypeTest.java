package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the DocumentType node
 *
 * @author Jonathan Hedley, http://jonathanhedley.com/
 */
public class DocumentTypeTest {

    @Test
    public void outerHtmlGeneration() {
        Document html5Doc = Jsoup.parse("<!DOCTYPE html>");
        DocumentType html5 = html5Doc.documentType();
        assertEquals("<!doctype html>", html5.outerHtml());

        Document publicDoc = Jsoup.parse("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML//\">");
        DocumentType publicDocType = publicDoc.documentType();
        assertEquals("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML//\">", publicDocType.outerHtml());

        Document systemDoc = Jsoup.parse("<!DOCTYPE html SYSTEM \"http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd\">");
        DocumentType systemDocType = systemDoc.documentType();
        assertEquals("<!DOCTYPE html SYSTEM \"http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd\">", systemDocType.outerHtml());

        Document comboDoc = Jsoup.parse("<!DOCTYPE notHtml PUBLIC \"--public\" \"--system\">");
        DocumentType combo = comboDoc.documentType();
        assertEquals("<!DOCTYPE notHtml PUBLIC \"--public\" \"--system\">", combo.outerHtml());
        assertEquals("nothtml", combo.name());
        assertEquals("--public", combo.publicId());
        assertEquals("--system", combo.systemId());
    }

    @Test
    public void testRoundTrip() {
        String base = "<!DOCTYPE html>";
        assertEquals("<!doctype html>", htmlOutput(base));
        assertEquals(base, xmlOutput(base));

        String publicDoc = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
        assertEquals(publicDoc, htmlOutput(publicDoc));
        assertEquals(publicDoc, xmlOutput(publicDoc));

        String systemDoc = "<!DOCTYPE html SYSTEM \"exampledtdfile.dtd\">";
        assertEquals(systemDoc, htmlOutput(systemDoc));
        assertEquals(systemDoc, xmlOutput(systemDoc));

        String legacyDoc = "<!DOCTYPE html SYSTEM \"about:legacy-compat\">";
        assertEquals(legacyDoc, htmlOutput(legacyDoc));
        assertEquals(legacyDoc, xmlOutput(legacyDoc));
    }

    @Test
    public void testPreservesRawDeclaration() {
        String rawDoctype = "<!DOCTYPE html PUBLIC \"-//TEST//DTD HTML 1.0//EN\" \"http://www.test.com/test.dtd\">";
        Document doc = Jsoup.parse(rawDoctype, "", Parser.xmlParser());
        DocumentType doctype = doc.documentType();
        assertEquals(rawDoctype, doctype.outerHtml());
    }

    @Test
    public void testPreservesUnusualDoctype() {
        String rawDoctype = "<!DOCTYPE root SYSTEM \"weird.dtd\">";
        Document doc = Jsoup.parse(rawDoctype, "", Parser.xmlParser());
        DocumentType doctype = doc.documentType();
        assertEquals(rawDoctype, doctype.outerHtml());
    }

    @Test
    public void attributes() {
        Document doc = Jsoup.parse("<!DOCTYPE html>");
        DocumentType doctype = doc.documentType();
        assertEquals("#doctype", doctype.nodeName());
        assertEquals("html", doctype.name());
        assertEquals("html", doctype.attr("name"));
        assertEquals("", doctype.publicId());
        assertEquals("", doctype.systemId());

        doc = Jsoup.parse("<!DOCTYPE notHtml PUBLIC \"--public\" \"--system\">");
        doctype = doc.documentType();
        assertEquals("#doctype", doctype.nodeName());
        assertEquals("nothtml", doctype.name());
        assertEquals("nothtml", doctype.attr("name"));
        assertEquals("--public", doctype.publicId());
        assertEquals("--system", doctype.systemId());
    }

    private String htmlOutput(String in) {
        Document doc = Jsoup.parse(in);
        DocumentType type = doc.documentType();
        return type.outerHtml();
    }

    private String xmlOutput(String in) {
        Document doc = Jsoup.parse(in, "", Parser.xmlParser());
        DocumentType type = doc.documentType();
        return type.outerHtml();
    }
}
