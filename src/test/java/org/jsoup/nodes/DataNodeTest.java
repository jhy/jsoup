package org.jsoup.nodes;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataNodeTest {

    @Test
    public void xmlOutputScriptWithCData() throws IOException {
        DataNode node = new DataNode("//<![CDATA[\nscript && <> data]]>");
        node.parentNode = new Element("script");
        StringBuilder accum = new StringBuilder();
        node.outerHtmlHead(accum, 0, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("//<![CDATA[\nscript && <> data]]>", accum.toString());
    }

    @Test
    public void xmlOutputScriptWithoutCData() throws IOException {
        DataNode node = new DataNode("script && <> data");
        node.parentNode = new Element("script");
        StringBuilder accum = new StringBuilder();
        node.outerHtmlHead(accum, 0, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("//<![CDATA[\nscript && <> data\n//]]>", accum.toString());
    }

    @Test
    public void xmlOutputStyleWithCData() throws IOException {
        DataNode node = new DataNode("/*<![CDATA[*/\nstyle && <> data]]>");
        node.parentNode = new Element("style");
        StringBuilder accum = new StringBuilder();
        node.outerHtmlHead(accum, 0, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("/*<![CDATA[*/\nstyle && <> data]]>", accum.toString());
    }

    @Test
    public void xmlOutputStyleWithoutCData() throws IOException {
        DataNode node = new DataNode("style && <> data");
        node.parentNode = new Element("style");
        StringBuilder accum = new StringBuilder();
        node.outerHtmlHead(accum, 0, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("/*<![CDATA[*/\nstyle && <> data\n/*]]>*/", accum.toString());
    }

    @Test
    public void xmlOutputOtherWithCData() throws IOException {
        DataNode node = new DataNode("<![CDATA[other && <> data]]>");
        node.parentNode = new Element("other");
        StringBuilder accum = new StringBuilder();
        node.outerHtmlHead(accum, 0, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("<![CDATA[other && <> data]]>", accum.toString());
    }

    @Test
    public void xmlOutputOtherWithoutCData() throws IOException {
        DataNode node = new DataNode("other && <> data");
        node.parentNode = new Element("other");
        StringBuilder accum = new StringBuilder();
        node.outerHtmlHead(accum, 0, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("<![CDATA[other && <> data]]>", accum.toString());
    }

    @Test
    public void xmlOutputOrphanWithoutCData() throws IOException {
        DataNode node = new DataNode("other && <> data");
        StringBuilder accum = new StringBuilder();
        node.outerHtmlHead(accum, 0, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("<![CDATA[other && <> data]]>", accum.toString());
    }

}
