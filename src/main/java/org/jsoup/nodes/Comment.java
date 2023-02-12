package org.jsoup.nodes;

import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 A comment node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Comment extends LeafNode {
    /**
     Create a new comment node.
     @param data The contents of the comment
     */
    public Comment(String data) {
        value = data;
    }

    public String nodeName() {
        return "#comment";
    }

    /**
     Get the contents of the comment.
     @return comment content
     */
    public String getData() {
        return coreValue();
    }

    public Comment setData(String data) {
        coreValue(data);
        return this;
    }

    @Override
	void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
        if (out.prettyPrint() && ((siblingIndex() == 0 && parentNode instanceof Element && ((Element) parentNode).tag().formatAsBlock()) || (out.outline() )))
            indent(accum, depth, out);
        accum
                .append("<!--")
                .append(getData())
                .append("-->");
    }

    @Override
    void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) {}

    @Override
    public String toString() {
        return outerHtml();
    }

    @Override
    public Comment clone() {
        return (Comment) super.clone();
    }

    /**
     * Check if this comment looks like an XML Declaration.
     * @return true if it looks like, maybe, it's an XML Declaration.
     */
    public boolean isXmlDeclaration() {
        String data = getData();
        return isXmlDeclarationData(data);
    }

    private static boolean isXmlDeclarationData(String data) {
        return (data.length() > 1 && (data.startsWith("!") || data.startsWith("?")));
    }

    /**
     * Attempt to cast this comment to an XML Declaration node.
     * @return an XML declaration if it could be parsed as one, null otherwise.
     */
    public @Nullable XmlDeclaration asXmlDeclaration() {
        String data = getData();

        XmlDeclaration decl = null;
        String declContent = data.substring(1, data.length() - 1);
        // make sure this bogus comment is not immediately followed by another, treat as comment if so
        if (isXmlDeclarationData(declContent))
            return null;

        String fragment = "<" + declContent + ">";
        // use the HTML parser not XML, so we don't get into a recursive XML Declaration on contrived data
        Document doc = Parser.htmlParser().settings(ParseSettings.preserveCase).parseInput(fragment, baseUri());
        if (doc.body().children().size() > 0) {
            Element el = doc.body().child(0);
            decl = new XmlDeclaration(NodeUtils.parser(doc).settings().normalizeTag(el.tagName()), data.startsWith("!"));
            decl.attributes().addAll(el.attributes());
        }
        return decl;
    }
}
