package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.NamespaceBindings;
import org.jsoup.internal.SharedConstants;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.select.Elements;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jsoup.parser.Parser.NamespaceXml;

/**
 * Use the {@code XmlTreeBuilder} when you want to parse XML without any of the HTML DOM rules being applied to the
 * document.
 * <p>Usage example: {@code Document xmlDoc = Jsoup.parse(html, baseUrl, Parser.xmlParser());}</p>
 *
 * @author Jonathan Hedley
 */
public class XmlTreeBuilder extends TreeBuilder {
    final NamespaceBindings namespaceBindings = new NamespaceBindings();

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

        namespaceBindings.clear();
        namespaceBindings.put("xml", NamespaceXml);
        namespaceBindings.put("", NamespaceXml);
    }

    @Override
    void initialiseParseFragment(@Nullable Element context) {
        super.initialiseParseFragment(context);
        if (context == null) return;

        // transition to the tag's text state if available
        TokeniserState textState = context.tag().textState();
        if (textState != null) tokeniser.transition(textState);

        // establish the fragment's base namespace scope from the context and its ancestors, top down
        Elements chain = context.parents();
        chain.add(0, context);
        for (int i = chain.size() - 1; i >= 0; i--) {
            Element el = chain.get(i);
            if (el.attributesSize() > 0) {
                namespaceBindings.applyDeclarations(el.attributes());
            }
        }
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
    TagSet defaultTagSet() {
        return new TagSet(); // an empty tagset
    }

    @Override
    int defaultMaxDepth() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean process(Token token) {
        currentToken = token;

        // start tag, end tag, doctype, xmldecl, comment, character, eof
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
            case XmlDecl:
                insertXmlDeclarationFor(token.asXmlDecl());
                break;
            case EOF: // could put some normalisation here if desired
                break;
            default:
                Validate.fail("Unexpected token type: " + token.type);
        }
        return true;
    }

    void insertElementFor(Token.StartTag startTag) {
        Attributes attributes = startTag.attributes;
        if (attributes != null) {
            settings.normalizeAttributes(attributes);
            attributes.deduplicate(settings);
        }

        enforceStackDepthLimit();
        namespaceBindings.pushScope();

        if (attributes != null) {
            namespaceBindings.applyDeclarations(attributes);
            applyNamespacesToAttributes(attributes);
            startTag.finaliseAttributeRanges(settings);
        }

        String tagName = startTag.tagName.value();
        String ns = resolveNamespace(tagName);
        Tag tag = tagFor(tagName, startTag.normalName, ns, settings);
        Element el = new Element(tag, null, attributes);
        currentElOrDoc().appendChild(el);
        push(el);

        if (startTag.isSelfClosing()) {
            tag.setSeenSelfClose();
            pop(); // push & pop ensures onNodeInserted & onNodeClosed
        } else if (tag.isEmpty()) {
            pop(); // custom defined void tag
        } else {
            TokeniserState textState = tag.textState();
            if (textState != null) tokeniser.transition(textState);
        }
    }

    /** Applies resolved namespace URIs to prefixed attributes. */
    private void applyNamespacesToAttributes(Attributes attributes) {
        // collect first, then add, as userData is stored as an attribute
        Map<String, String> attrPrefix = new HashMap<>();
        for (Attribute attr: attributes) {
            if (NamespaceBindings.isDeclaration(attr.getKey())) continue;
            String prefix = attr.prefix();
            if (!prefix.isEmpty()) {
                String ns = namespaceBindings.get(prefix);
                if (ns != null) attrPrefix.put(SharedConstants.XmlnsAttr + prefix, ns);
            }
        }
        for (Map.Entry<String, String> entry : attrPrefix.entrySet())
            attributes.userData(entry.getKey(), entry.getValue());
    }

    /** Resolves the namespace URI for a qualified tag name. */
    private String resolveNamespace(String tagName) {
        String ns = namespaceBindings.get("");
        int pos = tagName.indexOf(':');
        if (pos > 0) {
            String prefix = tagName.substring(0, pos);
            String boundNamespace = namespaceBindings.get(prefix);
            if (boundNamespace != null)
                ns = boundNamespace;
        }
        return ns;
    }

    @Override
    Element pop() {
        namespaceBindings.popScope();
        return super.pop();
    }

    void insertLeafNode(LeafNode node) {
        currentElOrDoc().appendChild(node);
        onNodeInserted(node);
    }

    void insertCommentFor(Token.Comment commentToken) {
        Comment comment = new Comment(commentToken.getData());
        insertLeafNode(comment);
    }

    void insertCharacterFor(Token.Character token) {
        final String data = token.getData();
        LeafNode node;
        if      (token.isCData()) node = new CDataNode(data);
        else if (currentElOrDoc().tag().is(Tag.Data))
            node = new DataNode(data);
        else node = new TextNode(data);
        insertLeafNode(node);
    }

    void insertDoctypeFor(Token.Doctype token) {
        DocumentType doctypeNode = new DocumentType(settings.normalizeTag(token.getName()), token.getPublicIdentifier(), token.getSystemIdentifier());
        doctypeNode.setPubSysKey(token.getPubSysKey());
        if (token.hasInternalSubset())
            doctypeNode.setInternalSubset(token.getInternalSubset());
        insertLeafNode(doctypeNode);
    }

    void insertXmlDeclarationFor(Token.XmlDecl token) {
        XmlDeclaration decl = new XmlDeclaration(token.name(), token.isDeclaration);
        if (token.attributes != null) decl.attributes().addAll(token.attributes);
        insertLeafNode(decl);
    }

    /**
     * If the stack contains an element with this tag's name, pop up the stack to remove the first occurrence. If not
     * found, skips.
     *
     * @param endTag tag to close
     */
    protected void popStackToClose(Token.EndTag endTag) {
        // like in HtmlTreeBuilder - don't scan up forever for very (artificially) deeply nested stacks
        String elName = settings.normalizeTag(endTag.name());
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
