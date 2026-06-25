package org.jsoup.nodes;

import org.jsoup.internal.StringUtil;
import org.jsoup.parser.TokenQueue;
import org.jsoup.select.Elements;
import org.jspecify.annotations.Nullable;

import static org.jsoup.parser.TokenQueue.escapeCssIdentifier;

/**
 Package-private helper that handles CSS selector path generation for {@link Element}.
 Extracted from Element to reduce its size (God Class / Large Class smell).
 */
class ElementCssSelector {

    static String cssSelector(Element el) {
        Document ownerDoc = el.ownerDocument();
        String idSel = uniqueIdSelector(el, ownerDoc);
        if (!idSel.isEmpty()) return idSel;

        StringBuilder selector = StringUtil.borrowBuilder();
        Element current = el;
        while (current != null && !(current instanceof Document)) {
            idSel = uniqueIdSelector(current, ownerDoc);
            if (!idSel.isEmpty()) {
                selector.insert(0, idSel);
                break;
            }
            selector.insert(0, cssSelectorComponent(current));
            current = current.parent();
        }
        return StringUtil.releaseBuilder(selector);
    }

    private static String uniqueIdSelector(Element el, @Nullable Document ownerDoc) {
        String id = el.id();
        if (!id.isEmpty()) {
            String idSel = "#" + escapeCssIdentifier(id);
            if (ownerDoc != null) {
                Elements els = ownerDoc.select(idSel);
                if (els.size() == 1 && els.get(0) == el) return idSel;
            } else {
                return idSel;
            }
        }
        return Node.EmptyString;
    }

    private static String cssSelectorComponent(Element el) {
        String tagName = escapeCssIdentifier(el.tagName()).replace("\\:", "|");
        StringBuilder selector = StringUtil.borrowBuilder().append(tagName);
        String classes = el.classNames().stream().map(TokenQueue::escapeCssIdentifier)
                .collect(StringUtil.joining("."));
        if (!classes.isEmpty())
            selector.append('.').append(classes);

        if (el.parent() == null || el.parent() instanceof Document)
            return StringUtil.releaseBuilder(selector);

        selector.insert(0, " > ");
        if (el.parent().select(selector.toString()).size() > 1)
            selector.append(String.format(
                ":nth-child(%d)", el.elementSiblingIndex() + 1));

        return StringUtil.releaseBuilder(selector);
    }
}
