package org.jsoup.safety;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.List;


/**
 The safelist based HTML cleaner. Use to ensure that end-user provided HTML contains only the elements and attributes
 that you are expecting; no junk, and no cross-site scripting attacks!
 <p>
 The HTML cleaner parses the input as HTML and then runs it through a safe-list, so the output HTML can only contain
 HTML that is allowed by the safelist.
 </p>
 <p>
 It is assumed that the input HTML is a body fragment; the clean methods only pull from the source's body, and the
 canned safe-lists only allow body contained tags.
 </p>
 <p>
 Rather than interacting directly with a Cleaner object, generally see the {@code clean} methods in {@link org.jsoup.Jsoup}.
 </p>
 */
public class Cleaner {
    private final Safelist safelist;
    private final Cleaner.CleanerSettings cleanerSettings;

    /**
     Create a new cleaner, that sanitizes documents using the supplied safelist.
     @param safelist safe-list to clean with
     */
    public Cleaner(Safelist safelist) {
        this(safelist, new CleanerSettings().cleanAttributeValues(false));
    }

    /**
     Create a new cleaner, that sanitizes documents using the supplied safelist.
     @param safelist safe-list to clean with
     @param cleanerSettings control how cleaner cleans
     */
    public Cleaner(Safelist safelist, Cleaner.CleanerSettings cleanerSettings) {
        Validate.notNull(safelist);
        this.safelist = safelist;
        this.cleanerSettings = cleanerSettings;
    }

    /**
     Creates a new, clean document, from the original dirty document, containing only elements allowed by the safelist.
     The original document is not modified. Only elements from the dirty document's <code>body</code> are used. The
     OutputSettings of the original document are cloned into the clean document.
     @param dirtyDocument Untrusted base document to clean.
     @return cleaned document.
     */
    public Document clean(Document dirtyDocument) {
        Validate.notNull(dirtyDocument);

        Document clean = Document.createShell(dirtyDocument.baseUri());
        copySafeNodes(dirtyDocument.body(), clean.body());
        clean.outputSettings(dirtyDocument.outputSettings().clone());

        return clean;
    }

    /**
     Determines if the input document's <b>body</b> is valid, against the safelist. It is considered valid if all the
     tags and attributes in the input HTML are allowed by the safelist, and that there is no content in the
     <code>head</code>.
     <p>
     This method is intended to be used in a user interface as a validator for user input. Note that regardless of the
     output of this method, the input document <b>must always</b> be normalized using a method such as
     {@link #clean(Document)}, and the result of that method used to store or serialize the document before later reuse
     such as presentation to end users. This ensures that enforced attributes are set correctly, and that any
     differences between how a given browser and how jsoup parses the input HTML are normalized.
     </p>
     <p>Example:
     <pre>{@code
     Document inputDoc = Jsoup.parse(inputHtml);
     Cleaner cleaner = new Cleaner(Safelist.relaxed());
     boolean isValid = cleaner.isValid(inputDoc);
     Document normalizedDoc = cleaner.clean(inputDoc);
     }</pre>
     </p>
     @param dirtyDocument document to test
     @return true if no tags or attributes need to be removed; false if they do
     */
    public boolean isValid(Document dirtyDocument) {
        Validate.notNull(dirtyDocument);

        Document clean = Document.createShell(dirtyDocument.baseUri());
        int numDiscarded = copySafeNodes(dirtyDocument.body(), clean.body());
        return numDiscarded == 0
            && dirtyDocument.head().childNodes().isEmpty(); // because we only look at the body, but we start from a shell, make sure there's nothing in the head
    }

    /**
     Determines if the input document's <b>body HTML</b> is valid, against the safelist. It is considered valid if all
     the tags and attributes in the input HTML are allowed by the safelist.
     <p>
     This method is intended to be used in a user interface as a validator for user input. Note that regardless of the
     output of this method, the input document <b>must always</b> be normalized using a method such as
     {@link #clean(Document)}, and the result of that method used to store or serialize the document before later reuse
     such as presentation to end users. This ensures that enforced attributes are set correctly, and that any
     differences between how a given browser and how jsoup parses the input HTML are normalized.
     </p>
     <p>Example:
     <pre>{@code
     Document inputDoc = Jsoup.parse(inputHtml);
     Cleaner cleaner = new Cleaner(Safelist.relaxed());
     boolean isValid = cleaner.isValidBodyHtml(inputHtml);
     Document normalizedDoc = cleaner.clean(inputDoc);
     }</pre>
     </p>
     @param bodyHtml HTML fragment to test
     @return true if no tags or attributes need to be removed; false if they do
     */
    public boolean isValidBodyHtml(String bodyHtml) {
        Document clean = Document.createShell("");
        Document dirty = Document.createShell("");
        ParseErrorList errorList = ParseErrorList.tracking(1);
        List<Node> nodes = Parser.parseFragment(bodyHtml, dirty.body(), "", errorList, null);
        dirty.body().insertChildren(0, nodes);
        int numDiscarded = copySafeNodes(dirty.body(), clean.body());
        return numDiscarded == 0 && errorList.isEmpty();
    }

    /**
     Iterates the input and copies trusted nodes (tags, attributes, text) into the destination.
     */
    private final class CleaningVisitor implements NodeVisitor {
        private int numDiscarded = 0;
        private final Element root;
        private Element destination; // current element to append nodes to

        private CleaningVisitor(Element root, Element destination) {
            this.root = root;
            this.destination = destination;
        }

        public void head(Node source, int depth) {
            if (source instanceof Element) {
                Element sourceEl = (Element) source;

                if (safelist.isSafeTag(sourceEl.normalName())) { // safe, clone and copy safe attrs
                    ElementMeta meta = createSafeElement(sourceEl);
                    Element destChild = meta.el;
                    destination.appendChild(destChild);

                    numDiscarded += meta.numAttribsDiscarded;
                    destination = destChild;
                } else if (source != root) { // not a safe tag, so don't add. don't count root against discarded.
                    numDiscarded++;
                }
            } else if (source instanceof TextNode) {
                TextNode sourceText = (TextNode) source;
                TextNode destText = new TextNode(sourceText.getWholeText());
                destination.appendChild(destText);
            } else if (source instanceof DataNode && safelist.isSafeTag(source.parent().nodeName())) {
                DataNode sourceData = (DataNode) source;
                DataNode destData = new DataNode(sourceData.getWholeData());
                destination.appendChild(destData);
            } else { // else, we don't care about comments, xml proc instructions, etc
                numDiscarded++;
            }
        }

        public void tail(Node source, int depth) {
            if (source instanceof Element && safelist.isSafeTag(source.nodeName())) {
                destination = destination.parent(); // would have descended, so pop destination stack
            }
        }
    }

    private int copySafeNodes(Element source, Element dest) {
        CleaningVisitor cleaningVisitor = new CleaningVisitor(source, dest);
        NodeTraversor.traverse(cleaningVisitor, source);
        return cleaningVisitor.numDiscarded;
    }

    private ElementMeta createSafeElement(Element sourceEl) {
        String sourceTag = sourceEl.tagName();
        Attributes destAttrs = new Attributes();
        Element dest = new Element(Tag.valueOf(sourceTag), sourceEl.baseUri(), destAttrs);
        int numDiscarded = 0;

        Attributes sourceAttrs = sourceEl.attributes();
        for (Attribute sourceAttr : sourceAttrs) {
            if (safelist.isSafeAttribute(sourceTag, sourceEl, sourceAttr)) {
                sourceAttr.setValue(cleanAttributeValue(sourceAttr));
                destAttrs.put(sourceAttr);
            }
            else {
                numDiscarded++;
            }
        }
        Attributes enforcedAttrs = safelist.getEnforcedAttributes(sourceTag);
        destAttrs.addAll(enforcedAttrs);

        // Copy the original start and end range, if set
        // TODO - might be good to make a generic Element#userData set type interface, and copy those all over
        if (sourceEl.sourceRange().isTracked())
            sourceEl.sourceRange().track(dest, true);
        if (sourceEl.endSourceRange().isTracked())
            sourceEl.endSourceRange().track(dest, false);

        return new ElementMeta(dest, numDiscarded);
    }

    private String cleanAttributeValue(Attribute attr) {
        if (!cleanerSettings.cleanAttributeValues()) {
            return attr.getValue();
        }
        return getCleanedAttributeValue(attr);
    }

    private String getCleanedAttributeValue(Attribute attr) {
        Document dirty = Parser.htmlParser().parseInput(attr.getValue(), cleanerSettings.baseUri());
        Elements headChildren = dirty.head().children();
        Elements bodyChildren = dirty.body().children();
        if (headChildren.size() == 0 && bodyChildren.size() == 0) {
            return attr.getValue();
        }

        Document cleaned = this.clean(dirty);
        Elements cleanedHeadChildren = cleaned.head().children();
        Elements cleanedBodyChildren = cleaned.body().children();
        if (headChildren.size() > 0 && cleanedHeadChildren.size() == 0) {
            return attr.getValue().replace(headChildren.outerHtml(), "");
        }
        if (bodyChildren.size() > 0 && cleanedBodyChildren.size() == 0) {
            return attr.getValue().replace(bodyChildren.outerHtml(), "");
        }
        return headChildren.size() > 0
                ? headChildren.outerHtml()
                : bodyChildren.outerHtml();
    }

    private static class ElementMeta {
        Element el;
        int numAttribsDiscarded;

        ElementMeta(Element el, int numAttribsDiscarded) {
            this.el = el;
            this.numAttribsDiscarded = numAttribsDiscarded;
        }
    }

    /**
     * A Cleaner's settings control how it cleans.
     */
    public static class CleanerSettings implements Cloneable {

        private boolean cleanAttributeValues = false;
        private String baseUri = "";

        public CleanerSettings() {}

        /**
         * Get if clean attribute values is enabled. Default is false.
         * @return if clean attribute values is enabled.
         */
        public boolean cleanAttributeValues() {
            return cleanAttributeValues;
        }

        /**
         * Enable or disable clean attribute values.
         * @param clean new clean attribute values setting
         * @return this, for chaining
         */
        public Cleaner.CleanerSettings cleanAttributeValues(boolean clean) {
            this.cleanAttributeValues = clean;
            return this;
        }

        /**
         * Get base Uri for the cleaner. Default is empty string.
         * @return the base Uri
         */
        public String baseUri() {
            return this.baseUri == null || this.baseUri.trim().length() == 0 ?  "" : this.baseUri;
        }

        /**
         * Set a base Uri for the cleaner.
         * @param baseUri the new base Uri
         * @return this, for chaining
         */
        public Cleaner.CleanerSettings baseUri(String baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        @Override
        public Cleaner.CleanerSettings clone() {
            Cleaner.CleanerSettings clone;
            try {
                clone = (Cleaner.CleanerSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            return clone;
        }
    }
}
