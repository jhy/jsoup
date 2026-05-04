package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.internal.LineMap;

/**
 Internal hooks used by the parser and cleaner to attach source ranges to nodes and attributes.
 <p>This class is public only because jsoup's internal packages need to cross package boundaries; it is not a supported
 user API.</p>
 */
public final class NodeInternals {
    private NodeInternals() {}

    /**
     Sets the source range for a node's start.
     */
    public static void sourceRange(Node node, LineMap lineMap, int startPos, int endPos) {
        Validate.notNull(node);
        node.ensureSpans().sourceRange(lineMap, startPos, endPos);
    }

    /**
     Sets the source range for an element's end tag.
     */
    public static void endSourceRange(Element element, LineMap lineMap, int startPos, int endPos) {
        Validate.notNull(element);
        element.ensureSpans().endSourceRange(lineMap, startPos, endPos);
    }

    /**
     Sets parser-tracked source offsets for an attribute.
     */
    public static void attributeRange(
        Attributes attributes,
        String key,
        LineMap lineMap,
        int nameStart,
        int nameEnd,
        int valueStart,
        int valueEnd
    ) {
        Validate.notNull(attributes);
        Validate.notNull(key);
        int index = attributes.visibleIndexOfKey(key);
        if (index != Attributes.NotFound)
            attributes.ensureSpans().attributeRange(index, lineMap, nameStart, nameEnd, valueStart, valueEnd);
    }

    /**
     Copies an existing tracked attribute range onto another attribute set.
     */
    public static void attributeRange(Attributes attributes, String key, Range.AttributeRange range) {
        Validate.notNull(attributes);
        Validate.notNull(key);
        Validate.notNull(range);
        int index = attributes.visibleIndexOfKey(key);
        if (index != Attributes.NotFound && range.isTracked())
            attributes.ensureSpans().attributeRange(index, range);
    }
}
