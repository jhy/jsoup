package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * Use the {@code XmlTreeBuilder} when you want to parse XML without any of the HTML DOM rules being applied to the
 * document.
 * <p>Usage example: {@code Document xmlDoc = Jsoup.parse(html, baseUrl, Parser.xmlParser());}</p>
 *
 * @author Jonathan Hedley
 */
public class XmlTreeBuilder extends TreeBuilder {
    ParseSettings defaultSettings() {
        return ParseSettings.preserveCase;
    }

    @Override @ParametersAreNonnullByDefault
    protected void initialiseParse(Reader input, String baseUri, Parser parser) {
        super.initialiseParse(input, baseUri, parser);
        stack.add(doc); // place the document onto the stack. differs from HtmlTreeBuilder (not on stack)
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

    @Override
    XmlTreeBuilder newInstance() {
        return new XmlTreeBuilder();
    }

    @Override
    protected boolean process(Token token) {
        // start tag, end tag, doctype, comment, character, eof
        switch (token.type) {
            case StartTag:
                insert(token.asStartTag());
                break;
            case EndTag:
                popStackToClose(token.asEndTag());
                break;
            case Comment:
                insert(token.asComment());
                break;
            case Character:
                insert(token.asCharacter());
                break;
            case Doctype:
                insert(token.asDoctype());
                break;
            case EOF: // could put some normalisation here if desired
                break;
            default:
                Validate.fail("Unexpected token type: " + token.type);
        }
        return true;
    }

    protected void insertNode(Node node) {
        currentElement().appendChild(node);
        onNodeInserted(node, null);
    }

    protected void insertNode(Node node, Token token) {
        currentElement().appendChild(node);
        onNodeInserted(node, token);
    }

    Element insert(Token.StartTag startTag) {
        Tag tag = tagFor(startTag.name(), settings);
        // todo: wonder if for xml parsing, should treat all tags as unknown? because it's not html.
        if (startTag.hasAttributes())
            startTag.attributes.deduplicate(settings);

        Element el = new Element(tag, null, settings.normalizeAttributes(startTag.attributes));
        insertNode(el, startTag);
        if (startTag.isSelfClosing()) {
            if (!tag.isKnownTag()) // unknown tag, remember this is self closing for output. see above.
                tag.setSelfClosing();
        } else {
            stack.add(el);
        }
        return el;
    }

    void insert(Token.Comment commentToken) {
        Comment comment = new Comment(commentToken.getData());
        Node insert = comment;
        if (commentToken.bogus && comment.isXmlDeclaration()) {
            // xml declarations are emitted as bogus comments (which is right for html, but not xml)
            // so we do a bit of a hack and parse the data as an element to pull the attributes out
            XmlDeclaration decl = comment.asXmlDeclaration(); // else, we couldn't parse it as a decl, so leave as a comment
            if (decl != null)
                insert = decl;
        }
        insertNode(insert, commentToken);
    }

    void insert(Token.Character token) {
        final String data = token.getData();
        insertNode(token.isCData() ? new CDataNode(data) : new TextNode(data), token);
    }

    void insert(Token.Doctype d) {
        DocumentType doctypeNode = new DocumentType(settings.normalizeTag(d.getName()), d.getPublicIdentifier(), d.getSystemIdentifier());
        doctypeNode.setPubSysKey(d.getPubSysKey());
        insertNode(doctypeNode, d);
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
            Element next = stack.get(pos);
            stack.remove(pos);
            if (next == firstFound) {
                onNodeClosed(next, endTag);
                break;
            }
        }
    }
    private static final int maxQueueDepth = 256; // an arbitrary tension point between real XML and crafted pain



    List<Node> parseFragment(String inputFragment, String baseUri, Parser parser) {
        initialiseParse(new StringReader(inputFragment), baseUri, parser);
        runParser();
        return doc.childNodes();
    }

    List<Node> parseFragment(String inputFragment, Element context, String baseUri, Parser parser) {
        return parseFragment(inputFragment, baseUri, parser);
    }
}
