package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;

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

    Document parse(Reader input, String baseUri) {
        return parse(input, baseUri, ParseErrorList.noTracking(), ParseSettings.preserveCase);
    }

    Document parse(String input, String baseUri) {
        return parse(new StringReader(input), baseUri, ParseErrorList.noTracking(), ParseSettings.preserveCase);
    }

    @Override
    protected void initialiseParse(Reader input, String baseUri, ParseErrorList errors, ParseSettings settings) {
        super.initialiseParse(input, baseUri, errors, settings);
        stack.add(doc); // place the document onto the stack. differs from HtmlTreeBuilder (not on stack)
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
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

    private void insertNode(Node node) {
        currentElement().appendChild(node);
    }

    Element insert(Token.StartTag startTag) {
        Tag tag = Tag.valueOf(startTag.name(), settings);
        // todo: wonder if for xml parsing, should treat all tags as unknown? because it's not html.
        Element el = new Element(tag, baseUri, settings.normalizeAttributes(startTag.attributes));
        insertNode(el);
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
        if (commentToken.bogus) { // xml declarations are emitted as bogus comments (which is right for html, but not xml)
            // so we do a bit of a hack and parse the data as an element to pull the attributes out
            String data = comment.getData();
            if (data.length() > 1 && (data.startsWith("!") || data.startsWith("?"))) {
                Document doc = Jsoup.parse("<" + data.substring(1, data.length() -1) + ">", baseUri, Parser.xmlParser());
                Element el = doc.child(0);
                insert = new XmlDeclaration(settings.normalizeTag(el.tagName()), data.startsWith("!"));
                insert.attributes().addAll(el.attributes());
            }
        }
        insertNode(insert);
    }

    void insert(Token.Character token) {
        final String data = token.getData();
        insertNode(token.isCData() ? new CDataNode(data) : new TextNode(data));
    }

    void insert(Token.Doctype d) {
        DocumentType doctypeNode = new DocumentType(settings.normalizeTag(d.getName()), d.getPublicIdentifier(), d.getSystemIdentifier());
        doctypeNode.setPubSysKey(d.getPubSysKey());
        insertNode(doctypeNode);
    }

    /**
     * If the stack contains an element with this tag's name, pop up the stack to remove the first occurrence. If not
     * found, skips.
     *
     * @param endTag tag to close
     */
    private void popStackToClose(Token.EndTag endTag) {
        String elName = settings.normalizeTag(endTag.tagName);
        Element firstFound = null;

        for (int pos = stack.size() -1; pos >= 0; pos--) {
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
            if (next == firstFound)
                break;
        }
    }

    List<Node> parseFragment(String inputFragment, String baseUri, ParseErrorList errors, ParseSettings settings) {
        initialiseParse(new StringReader(inputFragment), baseUri, errors, settings);
        runParser();
        return doc.childNodes();
    }
}
