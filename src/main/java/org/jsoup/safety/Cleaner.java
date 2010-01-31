package org.jsoup.safety;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;

import java.util.List;

/**
 The whitelist based HTML cleaner. Use to ensure that end-user provided HTML contains only the elements and attributes
 that you are expecting; no junk, and no cross-site scripting attacks!
 <p/>
 The HTML cleaner parses the input as HTML and then runs it through a white-list, so the output HTML can only contain
 HTML that is allowed by the whitelist.
 <p/>
 It is assumed that the input HTML is a body fragment; the clean methods only pull from the source's body, and the
 canned white-lists only allow body contained tags.
 <p/>
 Rather than interacting directly with a Cleaner object, generally see the {@code clean} methods in {@link org.jsoup.Jsoup}.
 */
public class Cleaner {
    private Whitelist whitelist;

    /**
     Create a new cleaner, that sanitizes documents using the supplied whitelist.
     @param whitelist white-list to clean with
     */
    public Cleaner(Whitelist whitelist) {
        Validate.notNull(whitelist);
        this.whitelist = whitelist;
    }

    /**
     Creates a new, clean document, from the original dirty document, containing only elements allowed by the whitelist.
     The original document is not modified. Only elements from the dirt document's <code>body</code> are used.
     @param dirtyDocument Untrusted base document to clean.
     @return cleaned document.
     */
    public Document clean(Document dirtyDocument) {
        Validate.notNull(dirtyDocument);

        Document clean = Document.createShell(dirtyDocument.baseUri());
        copySafeNodes(dirtyDocument.body(), clean.body());

        return clean;
    }

    private void copySafeNodes(Element source, Element dest) {
        List<Node> sourceChildren = source.childNodes();

        for (Node sourceChild : sourceChildren) {
            if (sourceChild instanceof Element) {
                Element sourceEl = (Element) sourceChild;

                if (whitelist.isSafeTag(sourceEl.tagName())) { // safe, clone and copy safe attrs
                    Element destChild = createSafeElement(sourceEl);
                    dest.appendChild(destChild);
                    copySafeNodes(sourceEl, destChild); // recurs
                } else { // not a safe tag, but it may have children (els or text) that are, so recurse
                    copySafeNodes(sourceEl, dest);
                }
            } else if (sourceChild instanceof TextNode) {
                TextNode sourceText = (TextNode) sourceChild;
                TextNode destText = new TextNode(sourceText.getWholeText(), sourceChild.baseUri());
                dest.appendChild(destText);
            } // else, we don't care about comments, xml proc instructions, etc
        }
    }

    private Element createSafeElement(Element sourceEl) {
        String sourceTag = sourceEl.tagName();
        Attributes destAttrs = new Attributes();
        Element dest = new Element(Tag.valueOf(sourceTag), sourceEl.baseUri(), destAttrs);

        Attributes sourceAttrs = sourceEl.attributes();
        for (Attribute sourceAttr : sourceAttrs) {
            if (whitelist.isSafeAttribute(sourceTag, sourceAttr))
                destAttrs.put(sourceAttr);
        }
        Attributes enforcedAttrs = whitelist.getEnforcedAttributes(sourceTag);
        destAttrs.addAll(enforcedAttrs);
        return dest;
    }

}
