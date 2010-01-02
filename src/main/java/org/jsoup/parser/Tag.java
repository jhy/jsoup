package org.jsoup.parser;

import org.apache.commons.lang.Validate;

import java.util.HashMap;
import java.util.Map;

/**
 HTML Tag specifications. This is a very simplistic model without the full expressiveness as the DTD,
 but it should capture most of what we need to know to intelligently parse a doc.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Tag {
    private static final Map<String, Tag> tags = new HashMap<String, Tag>();

    private String tagName;
    private boolean isBlock; // block or inline
    private boolean canContainBlock; // Can this tag hold block level tags?
    private boolean canContainInline; // only pcdata if not
    private boolean optionalClosing; // If tag is open, and another seen, close previous tag
    private boolean empty; // can hold nothing; e.g. img

    private Tag(String tagName, boolean block, boolean canContainBlock, boolean canContainInline, boolean optionalClosing, boolean empty) {
        this.tagName = tagName.toLowerCase();
        isBlock = block;
        this.canContainBlock = canContainBlock;
        this.canContainInline = canContainInline;
        this.optionalClosing = optionalClosing;
        this.empty = empty;
    }

    public String getName() {
        return tagName;
    }

    /**
     Get a Tag by name. If not previously defined (unknown), registers and returns a new generic tag, that can do anything.
     <p>
     Two unknown tags with the same name will compare ==.
     @param tagName Name of tag, e.g. "p". Case insensitive.
     @return The tag, either defined or new generic.
     */
    public static Tag valueOf(String tagName) {
        Validate.notNull(tagName);
        tagName = tagName.trim().toLowerCase();
        Validate.notEmpty(tagName);

        synchronized (tags) {
            Tag tag = tags.get(tagName);
            if (tag == null) {
                // not defined: create default
                // TODO: log, generate warning?
                tag = new Tag(tagName, true, true, true, false, false);
                register(tag);
            }
            return tag;
        }
    }

    /**
     Test if this tag, the prospective parent, can accept the proposed child.
     @param child potential child tag.
     @return true if this can contain child.
     */
    public boolean canContain(Tag child) {
        Validate.notNull(child);

        if (child.isBlock && !this.canContainBlock)
            return false;

        if (!child.isBlock && !this.canContainInline) // not block == inline
            return false;

        if (this.optionalClosing && this.equals(child))
            return false;
        // TODO: the optional closing may need more context to decide?

        if (this.empty)
            return false;

        // head can only contain a few. if more than head in here, modify to have a list of valids
        // TODO[must] - lookup what head can contain
        // from memory: base, script, link, meta, title
        if (this.tagName.equals("head")) {
            if (child.tagName.equals("base") || child.tagName.equals("script") || child.tagName.equals("link") ||
                    child.tagName.equals("meta") || child.tagName.equals("title")) {
                return true;
            }
            return false;
        }

        return true;
    }

    public boolean isBlock() {
        return isBlock;
    }

    public boolean isInline() {
        return !isBlock;
    }

    public boolean isEmpty() {
        return empty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;

        if (canContainBlock != tag.canContainBlock) return false;
        if (canContainInline != tag.canContainInline) return false;
        if (empty != tag.empty) return false;
        if (isBlock != tag.isBlock) return false;
        if (optionalClosing != tag.optionalClosing) return false;
        if (tagName != null ? !tagName.equals(tag.tagName) : tag.tagName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = tagName != null ? tagName.hashCode() : 0;
        result = 31 * result + (isBlock ? 1 : 0);
        result = 31 * result + (canContainBlock ? 1 : 0);
        result = 31 * result + (canContainInline ? 1 : 0);
        result = 31 * result + (optionalClosing ? 1 : 0);
        result = 31 * result + (empty ? 1 : 0);
        return result;
    }

    public String toString() {
        return tagName;
    }

    // internal static initialisers:

    static {
        // prepped from http://www.w3.org/TR/REC-html40/sgml/dtd.html#inline
        // tags are set here in uppercase for legibility, but internally held as lowercase.
        // TODO[must]: incorporate html 5 as appropriate

        // fontstyle
        createInline("TT");
        createInline("I");
        createInline("B");
        createInline("BIG");
        createInline("SMALL");

        // phrase
        createInline("EM");
        createInline("STRONG");
        createInline("DFN");
        createInline("CODE");
        createInline("SAMP");
        createInline("KBD");
        createInline("VAR");
        createInline("CITE");
        createInline("ABBR");
        createInline("ACRONYM");

        // special
        createInline("A").setOptionalClosing(); // cannot contain self
        createInline("IMG").setEmpty();
        createInline("OBJECT"); // flow (block/inline) or param
        createInline("BR").setEmpty();
        createInline("SCRIPT").setContainDataOnly();
        createInline("MAP"); // map is defined as inline, but can hold block (what?) or area. Seldom used so NBD.
        createInline("Q");
        createInline("SUB");
        createInline("SUP");
        createInline("SPAN");
        createInline("BDO");

        // things past this point aren't really blocks or inline. I'm using them because they can hold block or inline,
        // but per the spec, only specific elements can hold this. if this becomes a real-world parsing problem,
        // will need to have another non block/inline type, and explicit include & exclude rules. should be right though

        // block
        createBlock("P").setContainInlineOnly(); // emasculated block?
        createBlock("H1").setContainInlineOnly();
        createBlock("H2").setContainInlineOnly();
        createBlock("H3").setContainInlineOnly();
        createBlock("H4").setContainInlineOnly();
        createBlock("H5").setContainInlineOnly();
        createBlock("H6").setContainInlineOnly();
        createBlock("UL");
        createBlock("OL");
        createBlock("PRE").setContainInlineOnly();
        createBlock("DL");
        createBlock("DIV");
        createBlock("NOSCRIPT");
        createBlock("BLOCKQUOTE");
        createBlock("FORM").setOptionalClosing(); // can't contian self
        createBlock("HR").setEmpty();
        createBlock("TABLE"); // specific list of only includes (tr, td, thead etc) not implemented
        createBlock("FIELDSET");
        createBlock("ADDRESS").setContainInlineOnly();


        // formctrl
        createInline("INPUT").setEmpty();
        createInline("SELECT"); // just optgroup or option
        createInline("TEXTAREA");
        createInline("LABEL").setOptionalClosing(); // not self
        createInline("BUTTON"); // bunch of excludes not defined

        // document
        createBlock("HTML"); // specific includes not impl
        createBlock("HEAD"); // specific includes not impl: SCRIPT, STYLE, META, LINK, OBJECT
        createBlock("BODY"); // specific includes not impl

        // other
        createInline("AREA").setEmpty(); // not an inline per-se
        createBlock("LINK").setEmpty(); // only within head
        createInline("PARAM").setEmpty(); // only within object
        createBlock("INS"); // only within body
        createBlock("DEL"); // only within body

        createInline("DT").setOptionalClosing(); // only within DL. Prolly should create implicit DL?
        createInline("DD").setOptionalClosing(); // only within DL. Prolly should create implicit DL?

        createBlock("LI").setOptionalClosing(); // only within OL or UL. Implicit?

        createInline("OPTGROUP"); // only in select, only contain option
        createInline("OPTION").setContainDataOnly();
        createInline("TEXTAREA").setContainDataOnly();
        createInline("LEGEND"); // only within fieldset (implicit?)

        // tables
        createInline("CAPTION");
        createInline("THEAD").setOptionalClosing(); // just TR
        createInline("TFOOT").setOptionalClosing(); // just TR
        createInline("TBODY").setOptionalClosing(); // optional / implicit open too. just TR
        createInline("COLGROUP").setOptionalClosing(); // just COL
        createInline("COL").setEmpty();
        createInline("TR").setOptionalClosing(); // just TH, TD
        createBlock("TD").setOptionalClosing();
        
        // head
        createInline("TITLE").setContainDataOnly();
        createInline("BASE").setEmpty();
        createInline("META").setEmpty();
        createInline("STYLE").setContainDataOnly();
    }

    private static Tag createBlock(String tagName) {
        return register(new Tag(tagName, true, true, true, false, false));
    }

    private static Tag createInline(String tagName) {
        return register(new Tag(tagName, false, false, true, false, false));
    }

    private static Tag register(Tag tag) {
        synchronized (tags) {
            tags.put(tag.tagName, tag);
        }
        return tag;
    }

    private Tag setContainInlineOnly() {
        canContainBlock = false;
        canContainInline = true;
        return this;
    }

    private Tag setContainDataOnly() {
        canContainBlock = false;
        canContainInline = false;
        return this;
    }

    private Tag setEmpty() {
        canContainBlock = false;
        canContainInline = false;
        empty = true;
        return this;
    }

    private Tag setOptionalClosing() {
        optionalClosing = true;
        return this;
    }
}
