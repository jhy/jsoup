package org.jsoup.parser;

import org.jsoup.helper.Validate;
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
import java.util.ArrayDeque;
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
    static final String XmlnsKey = "xmlns";
    static final String XmlnsPrefix = "xmlns:";
    private final ArrayDeque<HashMap<String, String>> namespacesStack = new ArrayDeque<>(); // stack of namespaces, prefix => urn

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

        namespacesStack.clear();
        HashMap<String, String> ns = new HashMap<>();
        ns.put("xml", NamespaceXml);
        ns.put("", NamespaceXml);
        namespacesStack.push(ns);
    }

    @Override
    void initialiseParseFragment(@Nullable Element context) {
        super.initialiseParseFragment(context);
        if (context == null) return;

        // transition to the tag's text state if available
        TokeniserState textState = context.tag().textState();
        if (textState != null) tokeniser.transition(textState);

        // reconstitute the namespace stack by traversing the element and its parents (top down)
        Elements chain = context.parents();
        chain.add(0, context);
        for (int i = chain.size() - 1; i >= 0; i--) {
            Element el = chain.get(i);
            HashMap<String, String> namespaces = new HashMap<>(namespacesStack.peek());
            namespacesStack.push(namespaces);
            if (el.attributesSize() > 0) {
                processNamespaces(el.attributes(), namespaces);
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
        // handle namespace for tag
        HashMap<String, String> namespaces = new HashMap<>(namespacesStack.peek());
        namespacesStack.push(namespaces);

        Attributes attributes = startTag.attributes;
        if (attributes != null) {
            settings.normalizeAttributes(attributes);
            attributes.deduplicate(settings);
            processNamespaces(attributes, namespaces);
            applyNamespacesToAttributes(attributes, namespaces);
        }

        String tagName = startTag.tagName.value();
        String ns = resolveNamespace(tagName, namespaces);
        Tag tag = tagFor(tagName, startTag.normalName, ns, settings);
        Element el = new Element(tag, null, attributes);
        currentElement().appendChild(el);
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

    private static void processNamespaces(Attributes attributes, HashMap<String, String> namespaces) {
        // process attributes for namespaces (xmlns, xmlns:)
        for (Attribute attr : attributes) {
            String key = attr.getKey();
            String value = attr.getValue();
            if (key.equals(XmlnsKey)) {
                namespaces.put("", value); // new default for this level
            } else if (key.startsWith(XmlnsPrefix)) {
                String nsPrefix = key.substring(XmlnsPrefix.length());
                namespaces.put(nsPrefix, value);
            }
        }
    }

    private static void applyNamespacesToAttributes(Attributes attributes, HashMap<String, String> namespaces) {
        // second pass, apply namespace to attributes. Collects them first then adds (as userData is an attribute)
        Map<String, String> attrPrefix = new HashMap<>();
        for (Attribute attr: attributes) {
            String prefix = attr.prefix();
            if (!prefix.isEmpty()) {
                if (prefix.equals(XmlnsKey)) continue;
                String ns = namespaces.get(prefix);
                if (ns != null) attrPrefix.put(SharedConstants.XmlnsAttr + prefix, ns);
            }
        }
        for (Map.Entry<String, String> entry : attrPrefix.entrySet())
            attributes.userData(entry.getKey(), entry.getValue());
    }

    private static String resolveNamespace(String tagName, HashMap<String, String> namespaces) {
        String ns = namespaces.get("");
        int pos = tagName.indexOf(':');
        if (pos > 0) {
            String prefix = tagName.substring(0, pos);
            if (namespaces.containsKey(prefix))
                ns = namespaces.get(prefix);
        }
        return ns;
    }

    void insertLeafNode(LeafNode node) {
        currentElement().appendChild(node);
        onNodeInserted(node);
    }

    void insertCommentFor(Token.Comment commentToken) {
        Comment comment = new Comment(commentToken.getData());
        insertLeafNode(comment);
    }

    void insertCharacterFor(Token.Character token) {
        final String data = token.getData();
        LeafNode node;
        if      (token.isCData())                       node = new CDataNode(data);
        else if (currentElement().tag().is(Tag.Data))   node = new DataNode(data);
        else                                            node = new TextNode(data);
        insertLeafNode(node);
    }

    void insertDoctypeFor(Token.Doctype token) {
        DocumentType doctypeNode = new DocumentType(settings.normalizeTag(token.getName()), token.getPublicIdentifier(), token.getSystemIdentifier());
        doctypeNode.setPubSysKey(token.getPubSysKey());
        insertLeafNode(doctypeNode);
    }

    void insertXmlDeclarationFor(Token.XmlDecl token) {
        XmlDeclaration decl = new XmlDeclaration(token.name(), token.isDeclaration);
        if (token.attributes != null) decl.attributes().addAll(token.attributes);
        insertLeafNode(decl);
    }

    @Override
    Element pop() {
        namespacesStack.pop();
        return super.pop();
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
