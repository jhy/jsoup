package org.jsoup.helper;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.Selector;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.annotation.Nullable;
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
import java.util.Stack;

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
                    // handle <!doctype html> for legacy dom. TODO: nicer if <!doctype html>
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
            DOMImplementation impl = builder.getDOMImplementation();
            Document out = builder.newDocument();
            org.jsoup.nodes.Document inDoc = in.ownerDocument();
            org.jsoup.nodes.DocumentType doctype = inDoc != null ? inDoc.documentType() : null;
            if (doctype != null) {
                org.w3c.dom.DocumentType documentType = impl.createDocumentType(doctype.name(), doctype.publicId(), doctype.systemId());
                out.appendChild(documentType);
            }
            out.setXmlStandalone(true);
            // if in is Document, use the root element, not the wrapping document, as the context:
            org.jsoup.nodes.Element context = (in instanceof org.jsoup.nodes.Document) ? in.child(0) : in;
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
        org.jsoup.nodes.Element rootEl = in instanceof org.jsoup.nodes.Document ? in.child(0) : in; // skip the #root node if a Document
        NodeTraversor.traverse(builder, rootEl);
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
            throw new Selector.SelectorParseException("Could not evaluate XPath query [%s]: %s", xpath, e.getMessage());
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
     * Serialize a W3C document to a String. The output format will be XML or HTML depending on the content of the doc.
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
        private static final String xmlnsKey = "xmlns";
        private static final String xmlnsPrefix = "xmlns:";

        private final Document doc;
        private boolean namespaceAware = true;
        private final Stack<HashMap<String, String>> namespacesStack = new Stack<>(); // stack of namespaces, prefix => urn
        private Node dest;
        private Syntax syntax = Syntax.xml; // the syntax (to coerce attributes to). From the input doc if available.
        @Nullable private final org.jsoup.nodes.Element contextElement;

        public W3CBuilder(Document doc) {
            this.doc = doc;
            namespacesStack.push(new HashMap<>());
            dest = doc;
            contextElement = (org.jsoup.nodes.Element) doc.getUserData(ContextProperty); // Track the context jsoup Element, so we can save the corresponding w3c element
        }

        public void head(org.jsoup.nodes.Node source, int depth) {
            namespacesStack.push(new HashMap<>(namespacesStack.peek())); // inherit from above on the stack
            if (source instanceof org.jsoup.nodes.Element) {
                org.jsoup.nodes.Element sourceEl = (org.jsoup.nodes.Element) source;

                String prefix = updateNamespaces(sourceEl);
                String namespace = namespaceAware ? namespacesStack.peek().get(prefix) : null;
                String tagName = sourceEl.tagName();

                /* Tag names in XML are quite permissive, but less permissive than HTML. Rather than reimplement the validation,
                we just try to use it as-is. If it fails, insert as a text node instead. We don't try to normalize the
                tagname to something safe, because that isn't going to be meaningful downstream. This seems(?) to be
                how browsers handle the situation, also. https://github.com/jhy/jsoup/issues/1093 */
                try {
                    Element el = namespace == null && tagName.contains(":") ?
                        doc.createElementNS("", tagName) : // doesn't have a real namespace defined
                        doc.createElementNS(namespace, tagName);
                    copyAttributes(sourceEl, el);
                    append(el, sourceEl);
                    if (sourceEl == contextElement)
                        doc.setUserData(ContextNodeProperty, el, null);
                    dest = el; // descend
                } catch (DOMException e) {
                    append(doc.createTextNode("<" + tagName + ">"), sourceEl);
                }
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
            } else {
                // unhandled. note that doctype is not handled here - rather it is used in the initial doc creation
            }
        }

        private void append(Node append, org.jsoup.nodes.Node source) {
            append.setUserData(SourceProperty, source, null);
            dest.appendChild(append);
        }

        public void tail(org.jsoup.nodes.Node source, int depth) {
            if (source instanceof org.jsoup.nodes.Element && dest.getParentNode() instanceof Element) {
                dest = dest.getParentNode(); // undescend
            }
            namespacesStack.pop();
        }

        private void copyAttributes(org.jsoup.nodes.Node source, Element el) {
            for (Attribute attribute : source.attributes()) {
                String key = Attribute.getValidKey(attribute.getKey(), syntax);
                if (key != null) { // null if couldn't be coerced to validity
                    el.setAttribute(key, attribute.getValue());
                }
            }
        }

        /**
         * Finds any namespaces defined in this element. Returns any tag prefix.
         */
        private String updateNamespaces(org.jsoup.nodes.Element el) {
            // scan the element for namespace declarations
            // like: xmlns="blah" or xmlns:prefix="blah"
            Attributes attributes = el.attributes();
            for (Attribute attr : attributes) {
                String key = attr.getKey();
                String prefix;
                if (key.equals(xmlnsKey)) {
                    prefix = "";
                } else if (key.startsWith(xmlnsPrefix)) {
                    prefix = key.substring(xmlnsPrefix.length());
                } else {
                    continue;
                }
                namespacesStack.peek().put(prefix, attr.getValue());
            }

            // get the element prefix if any
            int pos = el.tagName().indexOf(':');
            return pos > 0 ? el.tagName().substring(0, pos) : "";
        }

    }
}
