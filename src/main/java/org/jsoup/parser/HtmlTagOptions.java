package org.jsoup.parser;

import org.jsoup.internal.StringUtil;

import static org.jsoup.parser.Parser.*;

/**
 Parser-only tag options used by the HTML tree builder.
 <p>These are cached on {@link Tag} so hot stack walks can check an option instead of searching
 the same sorted arrays repeatedly.</p>
 */
final class HtmlTagOptions {
    static final int Scope              = 1;
    static final int ListScope          = 1 << 1;
    static final int ButtonScope        = 1 << 2;
    static final int TableScope         = 1 << 3;
    static final int ImpliedEnd         = 1 << 4;
    static final int ThoroughImpliedEnd = 1 << 5;
    static final int Special            = 1 << 6;

    static final String[] ScopeTags = new String[]{ // a particular element in scope
            "applet", "caption", "html", "marquee", "object", "select", "table", "td", "template", "th"};
    static final String[] MathScopeTags = new String[]{"annotation-xml", "mi", "mn", "mo", "ms", "mtext"};
    static final String[] SvgScopeTags = new String[]{"desc", "foreignobject", "title"};
    static final String[] ListScopeTags = new String[]{"ol", "ul"};
    static final String[] ButtonScopeTags = new String[]{"button"};
    static final String[] TableScopeTags = new String[]{"html", "table", "template"};
    static final String[] ImpliedEndTags = new String[]{
            "dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc"};
    static final String[] ThoroughImpliedEndTags = new String[]{
            "caption", "colgroup", "dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc", "tbody", "td",
            "tfoot", "th", "thead", "tr"};
    static final String[] SpecialTags = new String[]{
            "address", "applet", "area", "article", "aside", "base", "basefont", "bgsound", "blockquote", "body", "br",
            "button", "caption", "center", "col", "colgroup", "dd", "details", "dir", "div", "dl", "dt", "embed",
            "fieldset", "figcaption", "figure", "footer", "form", "frame", "frameset", "h1", "h2", "h3", "h4", "h5",
            "h6", "head", "header", "hgroup", "hr", "html", "iframe", "img", "input", "keygen", "li", "link", "listing",
            "main", "marquee", "menu", "meta", "nav", "noembed", "noframes", "noscript", "object", "ol", "p", "param",
            "plaintext", "pre", "script", "search", "section", "select", "source", "style", "summary", "table", "tbody",
            "td", "template", "textarea", "tfoot", "th", "thead", "title", "tr", "track", "ul", "wbr", "xmp"};

    private HtmlTagOptions() {}

    /**
     Get parser options for a tag's normalized name and namespace.
     */
    static int optionsFor(String normalName, String namespace) {
        int options = 0;

        switch (namespace) {
            case NamespaceHtml:
                if (StringUtil.inSorted(normalName, ImpliedEndTags))         options |= ImpliedEnd;
                if (StringUtil.inSorted(normalName, ThoroughImpliedEndTags)) options |= ThoroughImpliedEnd;
                if (StringUtil.inSorted(normalName, ScopeTags))              options |= Scope;
                if (StringUtil.inSorted(normalName, ListScopeTags))          options |= ListScope;
                if (StringUtil.inSorted(normalName, ButtonScopeTags))        options |= ButtonScope;
                if (StringUtil.inSorted(normalName, TableScopeTags))         options |= TableScope;
                if (StringUtil.inSorted(normalName, SpecialTags))            options |= Special;
                break;
            case NamespaceMathml:
                if (StringUtil.inSorted(normalName, MathScopeTags))          options |= Scope | Special;
                break;
            case NamespaceSvg:
                if (StringUtil.inSorted(normalName, SvgScopeTags))           options |= Scope | Special;
                break;
        }

        return options;
    }
}
