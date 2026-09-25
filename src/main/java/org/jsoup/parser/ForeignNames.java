package org.jsoup.parser;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;

import static org.jsoup.internal.Normalizer.asciiLowerCase;
import static org.jsoup.internal.Normalizer.equalsIgnoreAsciiCase;
import static org.jsoup.parser.Parser.NamespaceMathml;
import static org.jsoup.parser.Parser.NamespaceSvg;

/** Applies the SVG and MathML name casing rules used when parsing HTML. */
final class ForeignNames {
    // https://html.spec.whatwg.org/multipage/parsing.html#parsing-main-inforeign
    private static final String[] SvgTags = {
        "altGlyph", "altGlyphDef", "altGlyphItem", "animateColor", "animateMotion",
        "animateTransform", "clipPath", "feBlend", "feColorMatrix", "feComponentTransfer",
        "feComposite", "feConvolveMatrix", "feDiffuseLighting", "feDisplacementMap", "feDistantLight",
        "feDropShadow", "feFlood", "feFuncA", "feFuncB", "feFuncG", "feFuncR",
        "feGaussianBlur", "feImage", "feMerge", "feMergeNode", "feMorphology",
        "feOffset", "fePointLight", "feSpecularLighting", "feSpotLight", "feTile",
        "feTurbulence", "foreignObject", "glyphRef", "linearGradient", "radialGradient", "textPath"
    };
    // https://html.spec.whatwg.org/multipage/parsing.html#adjust-svg-attributes
    private static final String[] SvgAttributes = {
        "attributeName", "attributeType", "baseFrequency", "baseProfile", "calcMode",
        "clipPathUnits", "diffuseConstant", "edgeMode", "filterUnits", "glyphRef",
        "gradientTransform", "gradientUnits", "kernelMatrix", "kernelUnitLength", "keyPoints",
        "keySplines", "keyTimes", "lengthAdjust", "limitingConeAngle", "markerHeight",
        "markerUnits", "markerWidth", "maskContentUnits", "maskUnits", "numOctaves",
        "pathLength", "patternContentUnits", "patternTransform", "patternUnits", "pointsAtX",
        "pointsAtY", "pointsAtZ", "preserveAlpha", "preserveAspectRatio", "primitiveUnits",
        "refX", "refY", "repeatCount", "repeatDur", "requiredExtensions",
        "requiredFeatures", "specularConstant", "specularExponent", "spreadMethod", "startOffset",
        "stdDeviation", "stitchTiles", "surfaceScale", "systemLanguage", "tableValues",
        "targetX", "targetY", "textLength", "viewBox", "viewTarget",
        "xChannelSelector", "yChannelSelector", "zoomAndPan"
    };

    private ForeignNames() {}

    /** Use the defined case if there is one, otherwise lowercase. */
    static String tagName(String name, String namespace) {
        if (NamespaceSvg.equals(namespace)) {
            for (String svgName : SvgTags) {
                if (equalsIgnoreAsciiCase(svgName, name)) return svgName;
            }
        }
        return asciiLowerCase(name);
    }

    /** Use the defined case if there is one, otherwise lowercase. */
    static void normalizeAttributes(Attributes attributes, String namespace) {
        for (Attribute attribute : attributes) {
            String orig = attribute.getKey();
            String adjusted = null;
            if (NamespaceSvg.equals(namespace)) {
                for (String name : SvgAttributes) {
                    if (equalsIgnoreAsciiCase(name, orig)) {
                        adjusted = name;
                        break;
                    }
                }
            } else if (NamespaceMathml.equals(namespace) && equalsIgnoreAsciiCase(orig, "definitionURL")) {
                adjusted = "definitionURL";
            }
            if (adjusted == null) adjusted = asciiLowerCase(orig);
            if (!adjusted.equals(orig)) attribute.setKey(adjusted);
        }
    }
}
