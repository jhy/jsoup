package org.jsoup.nodes;

import org.jsoup.internal.QuietAppendable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataNodeTest {
    
    static QuietAppendable appendable() {
        return QuietAppendable.wrap(new StringBuilder());
    }

    @Test
    public void xmlOutputScriptWithCData() {
        DataNode node = new DataNode("//<![CDATA[\nscript && <> data]]>");
        node.parentNode = new Element("script");
        QuietAppendable accum = appendable();
        node.outerHtmlHead(accum, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("//<![CDATA[\nscript && <> data]]>", accum.toString());
    }

    @Test
    public void xmlOutputScriptWithoutCData() {
        DataNode node = new DataNode("script && <> data");
        node.parentNode = new Element("script");
        QuietAppendable accum = appendable();
        node.outerHtmlHead(accum, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("//<![CDATA[\nscript && <> data\n//]]>", accum.toString());
    }

    @Test
    public void xmlOutputStyleWithCData() {
        DataNode node = new DataNode("/*<![CDATA[*/\nstyle && <> data]]>");
        node.parentNode = new Element("style");
        QuietAppendable accum = appendable();
        node.outerHtmlHead(accum, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("/*<![CDATA[*/\nstyle && <> data]]>", accum.toString());
    }

    @Test
    public void xmlOutputStyleWithoutCData() {
        DataNode node = new DataNode("style && <> data");
        node.parentNode = new Element("style");
        QuietAppendable accum = appendable();
        node.outerHtmlHead(accum, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("/*<![CDATA[*/\nstyle && <> data\n/*]]>*/", accum.toString());
    }

    @Test
    public void xmlOutputOtherWithCData() {
        DataNode node = new DataNode("<![CDATA[other && <> data]]>");
        node.parentNode = new Element("other");
        QuietAppendable accum = appendable();
        node.outerHtmlHead(accum, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("<![CDATA[other && <> data]]>", accum.toString());
    }

    @Test
    public void xmlOutputOtherWithoutCData() {
        DataNode node = new DataNode("other && <> data");
        node.parentNode = new Element("other");
        QuietAppendable accum = appendable();
        node.outerHtmlHead(accum, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("<![CDATA[other && <> data]]>", accum.toString());
    }

    @Test
    public void xmlOutputOrphanWithoutCData() {
        DataNode node = new DataNode("other && <> data");
        QuietAppendable accum = appendable();
        node.outerHtmlHead(accum, new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml));
        assertEquals("<![CDATA[other && <> data]]>", accum.toString());
    }

}
