package org.jsoup.nodes;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the DocumentType node
 *
 * @author Jonathan Hedley, http://jonathanhedley.com/
 */
public class DocumentTypeTest {
    @Test(expected = IllegalArgumentException.class)
    public void constructorValidationThrowsExceptionOnBlankName() {
        DocumentType fail = new DocumentType("","", "", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorValidationThrowsExceptionOnNulls() {
        DocumentType fail = new DocumentType("html", null, null, "");
    }

    @Test
    public void constructorValidationOkWithBlankPublicAndSystemIds() {
        DocumentType fail = new DocumentType("html","", "","");
    }

    @Test public void outerHtmlGeneration() {
        DocumentType html5 = new DocumentType("html", "", "", "");
        assertEquals("<!DOCTYPE html>", html5.outerHtml());

        DocumentType publicDocType = new DocumentType("html", "-//IETF//DTD HTML//", "", "");
        assertEquals("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML//\">", publicDocType.outerHtml());

        DocumentType systemDocType = new DocumentType("html", "", "http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd", "");
        assertEquals("<!DOCTYPE html \"http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd\">", systemDocType.outerHtml());

        DocumentType combo = new DocumentType("notHtml", "--public", "--system", "");
        assertEquals("<!DOCTYPE notHtml PUBLIC \"--public\" \"--system\">", combo.outerHtml());
    }
}
