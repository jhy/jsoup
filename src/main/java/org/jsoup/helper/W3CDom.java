package org.jsoup.helper;

import org.jsoup.internal.NamespaceBindings;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.Selector;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.jspecify.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.xml.transform.OutputKeys.METHOD;
import static org.jsoup.nodes.Document.OutputSettings.Syntax;

/**
 * Helper class to transform a {@link org.jsoup.nodes.Document} to a {@link org.w3c.dom.Document org.w3c.dom.Document},
 * for integration with toolsets that use the W3C DOM.
 */
public class W3CDom {
    /** For W3C Documents created by this class, this property is set on each node to link back to the original jsoup node. */
    public static final String SourceProperty = "jsoupSource";
    private static final String ContextProperty = "jsoupContextSource"; // tracks the jsoup context element on w3c doc
    private static final String ContextNodeProperty = "jsoupContextNode"; // the w3c node used as the creating context

    /**
     To get support for XPath versions &gt; 1, set this property to the classname of an alternate XPathFactory
     implementation. (For e.g. {@code net.sf.saxon.xpath.XPathFactoryImpl}).
     */
    public static final String XPathFactoryProperty = "javax.xml.xpath.XPathFactory:jsoup";

    protected DocumentBuilderFactory factory;
    private boolean namespaceAware = true; // false when using selectXpath, for user's query convenience

    public W3CDom() {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
    }

    /**
     Returns if this W3C DOM is namespace aware. By default, this will be {@code true}, but is disabled for simplicity
     when using XPath selectors in {@link org.jsoup.nodes.Element#selectXpath(String)}.
     @return the current namespace aware setting.
     */
    public boolean namespaceAware() {
        return namespaceAware;
    }

    /**
     Update the namespace aware setting. This impacts the factory that is used to create W3C nodes from jsoup nodes.
     <p>For HTML documents, controls if the document will be in the default {@code http://www.w3.org/1999/xhtml}
     namespace if otherwise unset.</p>.
     @param namespaceAware the updated setting
     @return this W3CDom, for chaining.
     */
    public W3CDom namespaceAware(boolean namespaceAware) {
        this.namespaceAware = namespaceAware;
        factory.setNamespaceAware(namespaceAware);
        return this;
    }

    /**
     * Converts a jsoup DOM to a W3C DOM.
     *
     * @param in jsoup Document
     * @return W3C Document
     */
    public static Document convert(org.jsoup.nodes.Document in) {
        return (new W3CDom().fromJsoup(in));
    }

    /**
     * Serialize a W3C document to a String. Provide Properties to define output settings including if HTML or XML. If
     * you don't provide the properties ({@code null}), the output will be auto-detected based on the content of the
     * document.
     *
     * @param doc Document
     * @param properties (optional/nullable) the output properties to use. See {@link
     *     Transformer#setOutputProperties(Properties)} and {@link OutputKeys}
     * @return Document as string
     * @see #OutputHtml
     * @see #OutputXml
     * @see OutputKeys#ENCODING
     * @see OutputKeys#OMIT_XML_DECLARATION
     * @see OutputKeys#STANDALONE
     * @see OutputKeys#DOCTYPE_PUBLIC
     * @see OutputKeys#CDATA_SECTION_ELEMENTS
     * @see OutputKeys#INDENT
     * @see OutputKeys#MEDIA_TYPE
     */
    public static String asString(Document doc, @Nullable Map<String, String> properties) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            if (properties != null)
                transformer.setOutputProperties(propertiesFromMap(properties));

            if (doc.getDoctype() != null) {
                DocumentType doctype = doc.getDoctype();
                if (!StringUtil.isBlank(doctype.getPublicId()))
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
                if (!StringUtil.isBlank(doctype.getSystemId()))
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
                    // handle <!doctype html> for legacy dom.
                else if (doctype.getName().equalsIgnoreCase("html")
                    && StringUtil.isBlank(doctype.getPublicId())
                    && StringUtil.isBlank(doctype.getSystemId()))
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "about:legacy-compat");
            }

            transformer.transform(domSource, result);
            return writer.toString();

        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }

    static Properties propertiesFromMap(Map<String, String> map) {
        Properties props = new Properties();
        props.putAll(map);
        return props;
    }

    /** Canned default for HTML output. */
    public static HashMap<String, String> OutputHtml() {
        return methodMap("html");
    }

    /** Canned default for XML output. */
    public static HashMap<String, String> OutputXml() {
        return methodMap("xml");
    }

    private static HashMap<String, String> methodMap(String method) {
        HashMap<String, String> map = new HashMap<>();
        map.put(METHOD, method);
        return map;
    }

    /**
     * Convert a jsoup Document to a W3C Document. The created nodes will link back to the original
     * jsoup nodes in the user property {@link #SourceProperty} (but after conversion, changes on one side will not
     * flow to the other).
     *
     * @param in jsoup doc
     * @return a W3C DOM Document representing the jsoup Document or Element contents.
     */
    public Document fromJsoup(org.jsoup.nodes.Document in) {
        // just method API backcompat
        return fromJsoup((org.jsoup.nodes.Element) in);
    }

    /**
     * Convert a jsoup DOM to a W3C Document. The created nodes will link back to the original
     * jsoup nodes in the user property {@link #SourceProperty} (but after conversion, changes on one side will not
     * flow to the other). The input Element is used as a context node, but the whole surrounding jsoup Document is
     * converted. (If you just want a subtree converted, use {@link #convert(org.jsoup.nodes.Element, Document)}.)
     *
     * @param in jsoup element or doc
     * @return a W3C DOM Document representing the jsoup Document or Element contents.
     * @see #sourceNodes(NodeList, Class)
     * @see #contextNode(Document)
     */
    public Document fromJsoup(org.jsoup.nodes.Element in) {
        Validate.notNull(in);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document out = builder.newDocument();
            org.jsoup.nodes.Document inDoc = in.ownerDocument();
            out.setXmlStandalone(true);
            // if in is Document, use the root element, not the wrapping document, as the context:
            org.jsoup.nodes.Element context = (in instanceof org.jsoup.nodes.Document) ? in.firstElementChild() : in;
            out.setUserData(ContextProperty, context, null);
            convert(inDoc != null ? inDoc : in, out);
            return out;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts a jsoup document into the provided W3C Document. If required, you can set options on the output
     * document before converting.
     *
     * @param in jsoup doc
     * @param out w3c doc
     * @see org.jsoup.helper.W3CDom#fromJsoup(org.jsoup.nodes.Element)
     */
    public void convert(org.jsoup.nodes.Document in, Document out) {
        // just provides method API backcompat
        convert((org.jsoup.nodes.Element) in, out);
    }

    /**
     * Converts a jsoup element into the provided W3C Document. If required, you can set options on the output
     * document before converting.
     *
     * @param in jsoup element
     * @param out w3c doc
     * @see org.jsoup.helper.W3CDom#fromJsoup(org.jsoup.nodes.Element)
     */
    public void convert(org.jsoup.nodes.Element in, Document out) {
        W3CBuilder builder = new W3CBuilder(out);
        builder.namespaceAware = namespaceAware;
        org.jsoup.nodes.Document inDoc = in.ownerDocument();
        if (inDoc != null) {
            if (!StringUtil.isBlank(inDoc.location())) {
                out.setDocumentURI(inDoc.location());
            }
            builder.syntax = inDoc.outputSettings().syntax();
        }
        if (in instanceof org.jsoup.nodes.Document)
            builder.traverseDocument((org.jsoup.nodes.Document) in);
        else
            builder.traverse(in);
    }

    /**
     Evaluate an XPath query against the supplied document, and return the results.
     @param xpath an XPath query
     @param doc the document to evaluate against
     @return the matches nodes
     */
    public NodeList selectXpath(String xpath, Document doc) {
        return selectXpath(xpath, (Node) doc);
    }

    /**
     Evaluate an XPath query against the supplied context node, and return the results.
     @param xpath an XPath query
     @param contextNode the context node to evaluate against
     @return the matches nodes
     */
    public NodeList selectXpath(String xpath, Node contextNode) {
        Validate.notEmptyParam(xpath, "xpath");
        Validate.notNullParam(contextNode, "contextNode");

        NodeList nodeList;
        try {
            // if there is a configured XPath factory, use that instead of the Java base impl:
            String property = System.getProperty(XPathFactoryProperty);
            final XPathFactory xPathFactory = property != null ?
                XPathFactory.newInstance("jsoup") :
                XPathFactory.newInstance();

            XPathExpression expression = xPathFactory.newXPath().compile(xpath);
            nodeList = (NodeList) expression.evaluate(contextNode, XPathConstants.NODESET); // love the strong typing here /s
            Validate.notNull(nodeList);
        } catch (XPathExpressionException | XPathFactoryConfigurationException e) {
            throw new Selector.SelectorParseException(
                e, "Could not evaluate XPath query [%s]: %s", xpath, e.getMessage());
        }
        return nodeList;
    }

    /**
     Retrieves the original jsoup DOM nodes from a nodelist created by this convertor.
     @param nodeList the W3C nodes to get the original jsoup nodes from
     @param nodeType the jsoup node type to retrieve (e.g. Element, DataNode, etc)
     @param <T> node type
     @return a list of the original nodes
     */
    public <T extends org.jsoup.nodes.Node> List<T> sourceNodes(NodeList nodeList, Class<T> nodeType) {
        Validate.notNull(nodeList);
        Validate.notNull(nodeType);
        List<T> nodes = new ArrayList<>(nodeList.getLength());

        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Node node = nodeList.item(i);
            Object source = node.getUserData(W3CDom.SourceProperty);
            if (nodeType.isInstance(source))
                nodes.add(nodeType.cast(source));
        }

        return nodes;
    }

    /**
     For a Document created by {@link #fromJsoup(org.jsoup.nodes.Element)}, retrieves the W3C context node.
     @param wDoc Document created by this class
     @return the corresponding W3C Node to the jsoup Element that was used as the creating context.
     */
    public Node contextNode(Document wDoc) {
        return (Node) wDoc.getUserData(ContextNodeProperty);
    }

    /**
     * Serialize a W3C document that was created by {@link #fromJsoup(org.jsoup.nodes.Element)} to a String.
     * The output format will be XML or HTML depending on the content of the doc.
     *
     * @param doc Document
     * @return Document as string
     * @see W3CDom#asString(Document, Map)
     */
    public String asString(Document doc) {
        return asString(doc, null);
    }

    /**
     * Implements the conversion by walking the input.
     */
    protected static class W3CBuilder implements NodeVisitor {
        private final Document doc;
        // source bindings include omitted ancestors; output bindings track declarations emitted to the W3C tree
        private final NamespaceBindings sourceNamespaces = new NamespaceBindings();
        private final NamespaceBindings outputNamespaces = new NamespaceBindings();
        private boolean namespaceAware = true;
        private Node dest;
        private Syntax syntax = Syntax.xml; // the syntax (to coerce attributes to). From the input doc if available.
        /*@Nullable*/ private final org.jsoup.nodes.Element contextElement; // todo - unsure why this can't be marked nullable?

        public W3CBuilder(Document doc) {
            this.doc = doc;
            dest = doc;
            sourceNamespaces.put("xml", Parser.NamespaceXml);
            outputNamespaces.put("xml", Parser.NamespaceXml);
            contextElement = (org.jsoup.nodes.Element) doc.getUserData(ContextProperty); // Track the context jsoup Element, so we can save the corresponding w3c element
        }

        // Traverse only nodes supported as W3C document children, and keep the first element as the root.
        private void traverseDocument(org.jsoup.nodes.Document source) {
            org.jsoup.nodes.Element root = source.firstElementChild();
            for (org.jsoup.nodes.Node child : source.childNodes()) {
                if (child == root || child instanceof org.jsoup.nodes.DocumentType ||
                    child instanceof org.jsoup.nodes.Comment || child instanceof org.jsoup.nodes.XmlDeclaration)
                    traverse(child);
            }
        }

        @Override
        public void head(org.jsoup.nodes.Node source, int depth) {
            if (source instanceof org.jsoup.nodes.Element) {
                org.jsoup.nodes.Element sourceEl = (org.jsoup.nodes.Element) source;
                if (depth == 0)
                    seedSourceNamespaces(sourceEl);
                sourceNamespaces.pushScope();
                outputNamespaces.pushScope();
                sourceNamespaces.applyDeclarations(sourceEl.attributes());
                @Nullable String namespace = namespaceAware ? w3cNamespace(sourceEl) : null;
                String tagName = w3cSafeName(sourceEl.tagName(), Syntax.xml);
                Element el;
                try {
                    // use an empty namespace if none is present but the tag name has a prefix
                    String imputedNamespace = namespace == null && tagName.contains(":") ? "" : namespace;
                    el = doc.createElementNS(imputedNamespace, tagName);
                } catch (DOMException ignored) {
                    // If the Normalize didn't get it XML / W3C safe, inserts as plain text
                    append(doc.createTextNode("<" + tagName + ">"), sourceEl);
                    return;
                }
                copyAttributes(sourceEl, el);
                append(el, sourceEl);
                if (sourceEl == contextElement)
                    doc.setUserData(ContextNodeProperty, el, null);
                dest = el; // descend
            } else if (source instanceof org.jsoup.nodes.DocumentType) {
                appendDocumentType((org.jsoup.nodes.DocumentType) source);
            } else if (source instanceof org.jsoup.nodes.CDataNode) {
                appendCdata((org.jsoup.nodes.CDataNode) source);
            } else if (source instanceof org.jsoup.nodes.TextNode) {
                org.jsoup.nodes.TextNode sourceText = (org.jsoup.nodes.TextNode) source;
                Text text = doc.createTextNode(sourceText.getWholeText());
                append(text, sourceText);
            } else if (source instanceof org.jsoup.nodes.Comment) {
                org.jsoup.nodes.Comment sourceComment = (org.jsoup.nodes.Comment) source;
                Comment comment = doc.createComment(sourceComment.getData());
                append(comment, sourceComment);
            } else if (source instanceof org.jsoup.nodes.DataNode) {
                org.jsoup.nodes.DataNode sourceData = (org.jsoup.nodes.DataNode) source;
                Text node = doc.createTextNode(sourceData.getWholeData());
                append(node, sourceData);
            } else if (source instanceof org.jsoup.nodes.XmlDeclaration) {
                appendProcessingInstruction((org.jsoup.nodes.XmlDeclaration) source);
            } else {
                // unhandled
            }
        }

        // Keep the doctype in document order; invalid doctypes cannot be represented.
        private void appendDocumentType(org.jsoup.nodes.DocumentType source) {
            try {
                DocumentType type = doc.getImplementation().createDocumentType(source.name(), source.publicId(), source.systemId());
                append(type, source);
            } catch (DOMException ignored) {
                // invalid / empty doctype dropped
            }
        }

        // Preserve CDATA where possible; programmatic content may be invalid for a W3C CDATA node.
        private void appendCdata(org.jsoup.nodes.CDataNode source) {
            try {
                append(doc.createCDATASection(source.getWholeText()), source);
            } catch (DOMException ignored) {
                append(doc.createTextNode(source.getWholeText()), source);
            }
        }

        // XmlDeclaration also represents <!name ...> nodes; XML declarations are reserved by the W3C DOM.
        private void appendProcessingInstruction(org.jsoup.nodes.XmlDeclaration source) {
            if (!source.outerHtml().startsWith("<?") || source.name().equalsIgnoreCase("xml")) return;
            try {
                append(doc.createProcessingInstruction(source.name(), source.getWholeDeclaration()), source);
            } catch (DOMException ignored) {
                // invalid programmatic processing instruction dropped
            }
        }

        private static @Nullable String w3cNamespace(org.jsoup.nodes.Element sourceEl) {
            // In W3C DOM, plain XML elements have no namespace; XML namespace is reserved for the {@code xml} prefix
            String namespace = sourceEl.tag().namespace();
            if (Parser.NamespaceXml.equals(namespace) && sourceEl.tag().prefix().isEmpty())
                return null;
            return namespace;
        }

        /** Applies declarations inherited from ancestors outside a subtree conversion. */
        private void seedSourceNamespaces(org.jsoup.nodes.Element sourceEl) {
            org.jsoup.select.Elements parents = sourceEl.parents();
            for (int i = parents.size() - 1; i >= 0; i--) {
                org.jsoup.nodes.Element parent = parents.get(i);
                if (parent.attributesSize() > 0)
                    sourceNamespaces.applyDeclarations(parent.attributes());
            }
        }

        private void append(Node append, org.jsoup.nodes.Node source) {
            append.setUserData(SourceProperty, source, null);
            dest.appendChild(append);
        }

        @Override
        public void tail(org.jsoup.nodes.Node source, int depth) {
            // head may emit an unrepresentable element as text without descending, so only ascend from its matching output element
            if (source instanceof org.jsoup.nodes.Element && dest.getUserData(SourceProperty) == source &&
                dest.getParentNode() != null) {
                dest = dest.getParentNode(); // undescend
            }
            if (source instanceof org.jsoup.nodes.Element) {
                sourceNamespaces.popScope();
                outputNamespaces.popScope();
            }
        }

        /** Copies namespace declarations first so source attribute order does not affect binding resolution. */
        private void copyAttributes(org.jsoup.nodes.Element jEl, Element wEl) {
            Attributes attributes = jEl.attributes();
            for (Attribute attribute : attributes) {
                if (NamespaceBindings.isDeclaration(attribute.getKey()))
                    copyAttribute(wEl, attribute);
            }
            for (Attribute attribute : attributes) {
                if (!NamespaceBindings.isDeclaration(attribute.getKey()))
                    copyAttribute(wEl, attribute);
            }
        }

        /** Copies an attribute using the closest W3C representation. */
        private void copyAttribute(Element wEl, Attribute attribute) {
            // preserve DOM-compatible HTML names; otherwise normalize as XML, and skip if still unrepresentable
            if (!trySetAttribute(wEl, attribute, syntax) && syntax != Syntax.xml)
                trySetAttribute(wEl, attribute, Syntax.xml);
        }

        /** Tries to copy an attribute, allowing the DOM to validate its name and namespace. */
        private boolean trySetAttribute(Element wEl, Attribute attribute, Syntax syntax) {
            try {
                setAttribute(wEl, attribute, syntax);
                return true;
            } catch (DOMException ignored) {
                return false;
            }
        }

        /** Copies an attribute with its resolved namespace and W3C-safe name. */
        private void setAttribute(Element wEl, Attribute attribute, Syntax syntax) throws DOMException {
            String key = w3cSafeName(attribute.getKey(), syntax);

            @Nullable String declarationPrefix = NamespaceBindings.declarationPrefix(key);
            if (declarationPrefix != null) {
                setNamespaceDeclaration(wEl, key, attribute.getValue());
                outputNamespaces.put(declarationPrefix, attribute.getValue());
                return;
            }

            int pos = key.indexOf(':');
            if (pos == -1) { // default namespaces do not apply to unprefixed attributes
                wEl.setAttribute(key, attribute.getValue());
                return;
            }

            String prefix = key.substring(0, pos);
            String sourcePrefix = attribute.prefix();
            @Nullable String namespace;
            if (namespaceAware) {
                String attributeNamespace = attribute.namespace();
                namespace = !attributeNamespace.isEmpty() ? attributeNamespace : sourceNamespaces.get(sourcePrefix);
            } else {
                namespace = sourceNamespaces.get(sourcePrefix);
            }
            if (namespace == null || namespace.isEmpty())
                namespace = undefinedNs;

            if (namespaceAware)
                wEl.setAttributeNS(namespace, key, attribute.getValue());
            else
                wEl.setAttribute(key, attribute.getValue());
            ensureOutputBinding(wEl, prefix, namespace);
        }

        /** Normalizes a name to a W3C-compatible QName, converting {@code 1:a:b} to {@code _1:a_b}. */
        private String w3cSafeName(String name, Syntax syntax) {
            @Nullable String normalized = Attribute.getValidKey(name, syntax);
            if (normalized == null) {
                normalized = Attribute.getValidKey("_" + name, syntax);
                if (normalized == null) return "_";
            }
            if (normalized.indexOf(':') == -1) return normalized;

            Matcher parts = QNameParts.matcher(normalized);
            if (!parts.matches()) return w3cSafeNcName(normalized.replace(':', '_'));
            return w3cSafeNcName(parts.group(1)) + ':' + w3cSafeNcName(parts.group(2));
        }

        /** Normalizes one QName component while preserving valid XML name characters. */
        private String w3cSafeNcName(String name) {
            if (isValidNcName(name)) return name;
            String safeStart = "_" + name;
            if (isValidNcName(safeStart)) return safeStart;

            String colonSafe = name.replace(':', '_');
            @Nullable String normalized = Attribute.getValidKey(colonSafe, Syntax.xml);
            if (normalized != null) return normalized;
            normalized = Attribute.getValidKey("_" + colonSafe, Syntax.xml);
            return normalized != null ? normalized : "_";
        }

        /** Tests a component against the XML NCName rules used by the output DOM. */
        private boolean isValidNcName(String name) {
            try {
                // validate as a local name so reserved words such as xmlns are treated as ordinary NCName text
                doc.createAttributeNS(undefinedNs, "p:" + name);
                return true;
            } catch (DOMException ignored) {
                return false;
            }
        }

        /** Declares a prefix when its output binding is not active. */
        private void ensureOutputBinding(Element wEl, String prefix, String namespace) {
            if (!namespace.equals(outputNamespaces.get(prefix))) {
                setNamespaceDeclaration(wEl, "xmlns:" + prefix, namespace);
                outputNamespaces.put(prefix, namespace);
            }
        }

        /** Writes a namespace declaration with namespace awareness when enabled. */
        private void setNamespaceDeclaration(Element wEl, String key, String namespace) {
            if (namespaceAware)
                wEl.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, key, namespace);
            else
                wEl.setAttribute(key, namespace);
        }

        private static final Pattern QNameParts = Pattern.compile("^([^:]+):(.+)$");
        private static final String undefinedNs = "undefined";
    }

}
