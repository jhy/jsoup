package org.jsoup.parser;

import org.apache.commons.lang.Validate;

import java.util.*;

/**
 HTML Tag specifications. This is a very simplistic model without the full expressiveness as the DTD,
 but it should capture most of what we need to know to intelligently parse a doc.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Tag {
    private static final Map<String, Tag> tags = new HashMap<String, Tag>();
    private static final Tag defaultAncestor;
    static {
        defaultAncestor = new Tag("BODY");
        tags.put(defaultAncestor.tagName, defaultAncestor);
    }

    private String tagName;
    private boolean isBlock = true; // block or inline
    private boolean canContainBlock = true; // Can this tag hold block level tags?
    private boolean canContainInline = true; // only pcdata if not
    private boolean optionalClosing = false; // If tag is open, and another seen, close previous tag
    private boolean empty = false; // can hold nothing; e.g. img
    private boolean preserveWhitespace = false; // for pre, textarea, script etc
    private List<Tag> ancestors;

    private Tag(String tagName) {
        this.tagName = tagName.toLowerCase();
    }

    public String getName() {
        return tagName;
    }

    /**
     Get a Tag by name. If not previously defined (unknown), returns a new generic tag, that can do anything.
     <p>
     Pre-defined tags (P, DIV etc) will be ==, but unknown tags are not registered and will only .equals().
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
                // not defined: create default; go anywhere, do anything! (incl be inside a <p>)
                tag = new Tag(tagName);
                tag.setAncestor(defaultAncestor.tagName);
                tag.isBlock = false;
                tag.canContainBlock = true;
            }
            return tag;
        }
    }

    /**
     Test if this tag, the prospective parent, can accept the proposed child.
     @param child potential child tag.
     @return true if this can contain child.
     */
    boolean canContain(Tag child) {
        Validate.notNull(child);

        if (child.isBlock && !this.canContainBlock)
            return false;

        if (!child.isBlock && !this.canContainInline) // not block == inline
            return false;

        if (this.optionalClosing && this.equals(child))
            return false;

        if (this.empty || this.isData())
            return false;

        // head can only contain a few. if more than head in here, modify to have a list of valids
        // TODO: (could solve this with walk for ancestor)
        if (this.tagName.equals("head")) {
            if (child.tagName.equals("base") || child.tagName.equals("script") || child.tagName.equals("noscript") || child.tagName.equals("link") ||
                    child.tagName.equals("meta") || child.tagName.equals("title") || child.tagName.equals("style") || child.tagName.equals("object")) {
                return true;
            }
            return false;
        }
        
        // dt and dd (in dl)
        if (this.tagName.equals("dt") && child.tagName.equals("dd"))
            return false;
        if (this.tagName.equals("dd") && child.tagName.equals("dt"))
            return false;

        return true;
    }

    /**
     Gets if this is a block tag.
     @return if block tag
     */
    public boolean isBlock() {
        return isBlock;
    }

    /**
     Gets if this tag can contain block tags.
     @return if tag can contain block tags
     */
    public boolean canContainBlock() {
        return canContainBlock;
    }

    /**
     Gets if this tag is an inline tag.
     @return if this tag is an inline tag.
     */
    public boolean isInline() {
        return !isBlock;
    }

    /**
     Gets if this tag is a data only tag.
     @return if this tag is a data only tag
     */
    public boolean isData() {
        return !canContainInline && !isEmpty();
    }

    /**
     Get if this is an empty tag
     @return if this is an emtpy tag
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     Get if this tag should preserve whitespace within child text nodes.
     @return if preserve whitepace
     */
    public boolean preserveWhitespace() {
        return preserveWhitespace;
    }

    Tag getImplicitParent() {
        return (!ancestors.isEmpty()) ? ancestors.get(0) : null;
    }

    boolean isValidParent(Tag child) {
        if (child.ancestors.isEmpty())
            return true; // HTML tag

        for (Tag tag : child.ancestors) {
            if (this.equals(tag))
                return true;
        }
        return false;
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

        // document
        createBlock("HTML").setAncestor(new String[0]); // specific includes not impl
        createBlock("HEAD").setAncestor("HTML"); // specific includes not impl: SCRIPT, STYLE, META, LINK, OBJECT
        createBlock("BODY").setAncestor("HTML"); // specific includes not impl
        createBlock("FRAMESET").setAncestor("HTML");

        // head
        // all ancestors set to (head, body): so implicitly create head, but allow in body
        createBlock("SCRIPT").setAncestor("HEAD", "BODY").setContainDataOnly();
        createBlock("NOSCRIPT").setAncestor("HEAD", "BODY");
        createBlock("STYLE").setAncestor("HEAD", "BODY").setContainDataOnly();
        createBlock("META").setAncestor("HEAD", "BODY").setEmpty();
        createBlock("LINK").setAncestor("HEAD", "BODY").setEmpty(); // only within head
        createInline("OBJECT").setAncestor("HEAD", "BODY"); // flow (block/inline) or param
        createBlock("TITLE").setAncestor("HEAD", "BODY").setContainDataOnly();
        createInline("BASE").setAncestor("HEAD", "BODY").setEmpty();

        createBlock("FRAME").setAncestor("FRAMESET").setEmpty();
        createBlock("NOFRAMES").setAncestor("FRAMESET").setContainDataOnly();



        // fontstyle
        createInline("FONT");
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
        createInline("BR").setEmpty();
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
        createBlock("PRE").setContainInlineOnly().setPreserveWhitespace();
        createBlock("DIV");
        createBlock("BLOCKQUOTE");
        createBlock("HR").setEmpty();
        createBlock("ADDRESS").setContainInlineOnly();


        // formctrl
        createBlock("FORM").setOptionalClosing(); // can't contian self
        createInline("INPUT").setAncestor("FORM").setEmpty();
        createInline("SELECT").setAncestor("FORM"); // just contain optgroup or option
        createInline("TEXTAREA").setAncestor("FORM").setContainDataOnly();
        createInline("LABEL").setAncestor("FORM").setOptionalClosing(); // not self
        createInline("BUTTON").setAncestor("FORM"); // bunch of excludes not defined
        createInline("OPTGROUP").setAncestor("SELECT"); //  only contain option
        createInline("OPTION").setAncestor("SELECT").setContainDataOnly();
        createBlock("FIELDSET").setAncestor("FORM");
        createInline("LEGEND").setAncestor("FIELDSET");

        // other
        createInline("AREA").setEmpty(); // not an inline per-se
        createInline("PARAM").setAncestor("OBJECT").setEmpty();
        createBlock("INS"); // only within body
        createBlock("DEL"); // only within body

        createBlock("DL");
        createInline("DT").setAncestor("DL").setOptionalClosing(); // only within DL.
        createInline("DD").setAncestor("DL").setOptionalClosing(); // only within DL.

        createBlock("LI").setAncestor("UL", "OL").setOptionalClosing(); // only within OL or UL.

        // tables
        createBlock("TABLE"); // specific list of only includes (tr, td, thead etc) not implemented
        createBlock("CAPTION").setAncestor("TABLE");
        createBlock("THEAD").setAncestor("TABLE").setOptionalClosing(); // just TR
        createBlock("TFOOT").setAncestor("TABLE").setOptionalClosing(); // just TR
        createBlock("TBODY").setAncestor("TABLE").setOptionalClosing(); // optional / implicit open too. just TR
        createBlock("COLGROUP").setAncestor("TABLE").setOptionalClosing(); // just COL
        createBlock("COL").setAncestor("COLGROUP").setEmpty();
        createBlock("TR").setAncestor("TABLE").setOptionalClosing(); // just TH, TD
        createBlock("TH").setAncestor("TR").setOptionalClosing();
        createBlock("TD").setAncestor("TR").setOptionalClosing();
    }

    private static Tag createBlock(String tagName) {
        return register(new Tag(tagName));
    }

    private static Tag createInline(String tagName) {
        Tag inline = new Tag(tagName);
        inline.isBlock = false;
        inline.canContainBlock = false;
        return register(inline);
    }

    private static Tag register(Tag tag) {
        tag.setAncestor(defaultAncestor.tagName);
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
        preserveWhitespace = true;
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

    private Tag setPreserveWhitespace() {
        preserveWhitespace = true;
        return this;
    }

    private Tag setAncestor(String... tagNames) {
        if (tagNames == null) {
            ancestors = Collections.emptyList();
        } else {
            ancestors = new ArrayList<Tag>(tagNames.length);
            for (String name : tagNames) {
                ancestors.add(Tag.valueOf(name));
            }
        }
        return this;
    }
}
