package org.jsoup.safety;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class DetailedHtmlCleaner {
    private Whitelist whitelist;
    private ValidationErrors validationErrors;

    public DetailedHtmlCleaner(Whitelist whitelist) {
        Validate.notNull(whitelist);

        this.whitelist          = whitelist;
        this.validationErrors = new ValidationErrors();
    }

    public ValidationErrors validate(Document dirtyDocument) {
        Validate.notNull(dirtyDocument);

        Document clean = Document.createShell(dirtyDocument.baseUri());

        copySafeNodes(dirtyDocument.body(), clean.body());

        return validationErrors;
    }

    private final class CleaningVisitor implements NodeVisitor {
        private final Element root;
        private Element destination;

        private CleaningVisitor(Element root, Element destination) {
            this.root        = root;
            this.destination = destination;
        }

        public void head(Node source, int depth) {
            if (source instanceof Element) {
                Element sourceEl = (Element) source;

                if (whitelist.isSafeTag(sourceEl.tagName())) { // safe, clone and copy safe attrs
                    ElementMeta meta = createSafeElement(sourceEl);
                    Element destChild = meta.el;
                    destination.appendChild(destChild);

                    destination = destChild;
                } else if (source != root) { // not a safe tag, so don't add. don't count root against discarded.
                    validationErrors.addTag(sourceEl.tagName(), sourceEl.toString());
                }
            } else if (source instanceof TextNode) {
                TextNode sourceText = (TextNode) source;
                TextNode destText = new TextNode(sourceText.getWholeText(), source.baseUri());
                destination.appendChild(destText);
            } else if (source instanceof DataNode && whitelist.isSafeTag(source.parent().nodeName())) {
                DataNode sourceData = (DataNode) source;
                DataNode destData = new DataNode(sourceData.getWholeData(), source.baseUri());
                destination.appendChild(destData);
            } // else, we don't care about comments, xml proc instructions, etc
        }

        public void tail(Node source, int depth) {
            if (source instanceof Element && whitelist.isSafeTag(source.nodeName())) {
                destination = destination.parent(); // would have descended, so pop destination stack
            }
        }
    }

    private void copySafeNodes(Element source, Element dest) {
        CleaningVisitor cleaningVisitor = new CleaningVisitor(source, dest);
        NodeTraversor traversor         = new NodeTraversor(cleaningVisitor);

        traversor.traverse(source);
    }

    private ElementMeta createSafeElement(Element sourceEl) {
        String sourceTag = sourceEl.tagName();
        Attributes destAttrs = new Attributes();
        Element dest = new Element(Tag.valueOf(sourceTag), sourceEl.baseUri(), destAttrs);

        Attributes sourceAttrs = sourceEl.attributes();
        for (Attribute sourceAttr : sourceAttrs) {
            if (whitelist.isSafeAttribute(sourceTag, sourceEl, sourceAttr))
                destAttrs.put(sourceAttr);
            else
                validationErrors.addAttribute(sourceTag, sourceAttr.getKey(), sourceEl.toString());
        }
        Attributes enforcedAttrs = whitelist.getEnforcedAttributes(sourceTag);
        destAttrs.addAll(enforcedAttrs);

        return new ElementMeta(dest);
    }

    private static class ElementMeta {
        Element el;

        ElementMeta(Element el) {
            this.el = el;
        }
    }
}
