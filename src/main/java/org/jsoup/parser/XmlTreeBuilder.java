package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.jsoup.parser.Parser.NamespaceXml;

/**
 * Use the {@code XmlTreeBuilder} when you want to parse XML without any of the HTML DOM rules being applied to the
 * document.
 * <p>Usage example: {@code Document xmlDoc = Jsoup.parse(html, baseUrl, Parser.xmlParser());}</p>
 *
 * @author Jonathan Hedley
 */
public class XmlTreeBuilder extends TreeBuilder {
    @Override ParseSettings defaultSettings() {
        return ParseSettings.preserveCase;
    }

    @Override
    protected void initialiseParse(Reader input, String baseUri, Parser parser) {
        super.initialiseParse(input, baseUri, parser);
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .prettyPrint(false); // as XML, we don't understand what whitespace is significant or not
    }

    Document parse(Reader input, String baseUri) {
        return parse(input, baseUri, new Parser(this));
    }

    Document parse(String input, String baseUri) {
        return parse(new StringReader(input), baseUri, new Parser(this));
    }

    @Override List<Node> completeParseFragment() {
        return doc.childNodes();
    }

    @Override
    XmlTreeBuilder newInstance() {
        return new XmlTreeBuilder();
    }

    @Override public String defaultNamespace() {
        return NamespaceXml;
    }

    @Override
    protected boolean process(Token token) {
        currentToken = token;

        // start tag, end tag, doctype, comment, character, eof
        switch (token.type) {
            case StartTag:
                insertElementFor(token.asStartTag());
                break;
            case EndTag:
                popStackToClose(token.asEndTag());
                break;
            case Comment:
                insertCommentFor(token.asComment());
                break;
            case Character:
                insertCharacterFor(token.asCharacter());
                break;
            case Doctype:
                insertDoctypeFor(token.asDoctype());
                break;
            case EOF: // could put some normalisation here if desired
                break;
            default:
                Validate.fail("Unexpected token type: " + token.type);
        }
        return true;
    }

    void insertElementFor(Token.StartTag startTag) {
        Tag tag = tagFor(startTag.name(), settings);
        if (startTag.attributes != null)
            startTag.attributes.deduplicate(settings);

        Element el = new Element(tag, null, settings.normalizeAttributes(startTag.attributes));
        currentElement().appendChild(el);
        push(el);

        if (startTag.isSelfClosing()) {
            tag.setSelfClosing();
            pop(); // push & pop ensures onNodeInserted & onNodeClosed
        }
    }

    void insertLeafNode(LeafNode node) {
        currentElement().appendChild(node);
        onNodeInserted(node);
    }

    void insertCommentFor(Token.Comment commentToken) {
        Comment comment = new Comment(commentToken.getData());
        LeafNode insert = comment;
        if (commentToken.bogus && comment.isXmlDeclaration()) {
            // xml declarations are emitted as bogus comments (which is right for html, but not xml)
            // so we do a bit of a hack and parse the data as an element to pull the attributes out
            // todo - refactor this to parse more appropriately
            XmlDeclaration decl = comment.asXmlDeclaration(); // else, we couldn't parse it as a decl, so leave as a comment
            if (decl != null)
                insert = decl;
        }
        insertLeafNode(insert);
    }

    void insertCharacterFor(Token.Character token) {
        final String data = token.getData();
        insertLeafNode(token.isCData() ? new CDataNode(data) : new TextNode(data));
    }

    void insertDoctypeFor(Token.Doctype token) {
        DocumentType doctypeNode = new DocumentType(settings.normalizeTag(token.getName()), token.getPublicIdentifier(), token.getSystemIdentifier());
        doctypeNode.setPubSysKey(token.getPubSysKey());
        insertLeafNode(doctypeNode);
    }

    /**
     * If the stack contains an element with this tag's name, pop up the stack to remove the first occurrence. If not
     * found, skips.
     *
     * @param endTag tag to close
     */
    protected void popStackToClose(Token.EndTag endTag) {
        // like in HtmlTreeBuilder - don't scan up forever for very (artificially) deeply nested stacks
        String elName = settings.normalizeTag(endTag.tagName);
        Element firstFound = null;

        final int bottom = stack.size() - 1;
        final int upper = bottom >= maxQueueDepth ? bottom - maxQueueDepth : 0;

        for (int pos = stack.size() -1; pos >= upper; pos--) {
            Element next = stack.get(pos);
            if (next.nodeName().equals(elName)) {
                firstFound = next;
                break;
            }
        }
        if (firstFound == null)
            return; // not found, skip

        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element next = pop();
            if (next == firstFound) {
                break;
            }
        }
    }
    private static final int maxQueueDepth = 256; // an arbitrary tension point between real XML and crafted pain
}
