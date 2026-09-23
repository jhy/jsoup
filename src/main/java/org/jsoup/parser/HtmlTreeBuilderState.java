package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.NodeInternals;
import org.jsoup.nodes.Range;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

import static org.jsoup.internal.Normalizer.asciiLowerCase;
import static org.jsoup.internal.Normalizer.equalsIgnoreAsciiCase;
import static org.jsoup.internal.StringUtil.inSorted;
import static org.jsoup.parser.HtmlTreeBuilder.isHtmlEl;
import static org.jsoup.parser.HtmlTreeBuilder.isSpecial;
import static org.jsoup.parser.HtmlTreeBuilderState.Constants.*;

/**
 * The Tree Builder's current state. Each state embodies the processing for the state, and transitions to other states.
 */
enum HtmlTreeBuilderState {
    Initial {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                return true; // ignore whitespace until we get the first content
            } else if (t.isComment()) {
                tb.insertCommentNode(t.asComment());
            } else if (t.isDoctype()) {
                // todo: parse error check on expected doctypes
                Token.Doctype d = t.asDoctype();
                String name = tb.settings.preserveTagCase() ? d.getName() : asciiLowerCase(d.getName());
                DocumentType doctype = new DocumentType(name, d.getPublicIdentifier(), d.getSystemIdentifier());
                doctype.setPubSysKey(d.getPubSysKey());
                tb.getDocument().appendChild(doctype);
                tb.onNodeInserted(doctype);
                // todo: quirk state check on more doctype ids, if deemed useful (most are ancient legacy and presumably irrelevant)
                if (d.isForceQuirks() || !doctype.name().equals("html") || equalsIgnoreAsciiCase(doctype.publicId(), "HTML"))
                    tb.getDocument().quirksMode(Document.QuirksMode.quirks);
                tb.transition(BeforeHtml);
            } else {
                // todo: check not iframe srcdoc
                tb.getDocument().quirksMode(Document.QuirksMode.quirks); // missing doctype
                tb.transition(BeforeHtml);
                return tb.process(t); // re-process token
            }
            return true;
        }
    },
    BeforeHtml {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isComment()) {
                tb.insertCommentNode(t.asComment());
            } else if (isWhitespace(t)) {
                tb.insertCharacterNode(t.asCharacter()); // out of spec - include whitespace
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                tb.insertElementFor(t.asStartTag());
                tb.transition(BeforeHead);
            } else if (t.isEndTag() && (inSorted(t.asEndTag().normalName(), BeforeHtmlToHead))) {
                return anythingElse(t, tb);
            } else if (t.isEndTag()) {
                tb.error(this);
                return false;
            } else {
                return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.processStartTag("html");
            tb.transition(BeforeHead);
            return tb.process(t);
        }
    },
    BeforeHead {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insertCharacterNode(t.asCharacter()); // out of spec - include whitespace
            } else if (t.isComment()) {
                tb.insertCommentNode(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return InBody.process(t, tb); // does not transition
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("head")) {
                Element head = tb.insertElementFor(t.asStartTag());
                tb.setHeadElement(head);
                tb.transition(InHead);
            } else if (t.isEndTag() && (inSorted(t.asEndTag().normalName(), BeforeHtmlToHead))) {
                tb.processStartTag("head");
                return tb.process(t);
            } else if (t.isEndTag()) {
                tb.error(this);
                return false;
            } else {
                tb.processStartTag("head");
                return tb.process(t);
            }
            return true;
        }
    },
    InHead {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insertCharacterNode(t.asCharacter()); // out of spec - include whitespace
                return true;
            }
            final String name;
            switch (t.type) {
                case Comment:
                    tb.insertCommentNode(t.asComment());
                    break;
                case Doctype:
                    tb.error(this);
                    return false;
                case StartTag:
                    Token.StartTag start = t.asStartTag();
                    name = start.normalName();
                    if (name.equals("html")) {
                        return InBody.process(t, tb);
                    } else if (inSorted(name, InHeadEmpty)) {
                        Element el = tb.insertEmptyElementFor(start);
                        // jsoup special: update base the first time it is seen
                        if (name.equals("base") && el.hasAttr("href"))
                            tb.maybeSetBaseUri(el);
                    } else if (name.equals("meta")) {
                        tb.insertEmptyElementFor(start);
                    } else if (name.equals("title")) {
                        HandleTextState(start, tb, tb.tagFor(start).textState());
                    } else if (inSorted(name, InHeadRaw)) {
                        HandleTextState(start, tb, tb.tagFor(start).textState());
                    } else if (name.equals("noscript")) {
                        tb.startNoscript(start);
                    } else if (name.equals("script")) {
                        // skips some script rules as won't execute them
                        HandleTextState(start, tb, TokeniserState.ScriptData);
                    } else if (name.equals("head")) {
                        tb.error(this);
                        return false;
                    } else if (name.equals("template")) {
                        tb.insertElementFor(start);
                        tb.insertMarkerToFormattingElements();
                        tb.framesetOk(false);
                        tb.transition(InTemplate);
                        tb.pushTemplateMode(InTemplate);
                    } else {
                        return anythingElse(t, tb);
                    }
                    break;
                case EndTag:
                    Token.EndTag end = t.asEndTag();
                    name = end.normalName();
                    if (name.equals("head")) {
                        tb.pop();
                        tb.transition(AfterHead);
                    } else if (inSorted(name, Constants.InHeadEnd)) {
                        return anythingElse(t, tb);
                    } else if (name.equals("template")) {
                        if (!tb.onStack(name)) {
                            tb.error(this);
                        } else {
                            tb.generateImpliedEndTags(true);
                            if (!tb.currentElementIs(name)) tb.error(this);
                            tb.popStackToClose(name);
                            tb.clearFormattingElementsToLastMarker();
                            tb.popTemplateMode();
                            tb.resetInsertionMode();
                        }
                    }
                    else {
                        tb.error(this);
                        return false;
                    }
                    break;
                default:
                    return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            tb.processEndTag("head");
            return tb.process(t);
        }
    },
    AfterHead {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insertCharacterNode(t.asCharacter());
            } else if (t.isComment()) {
                tb.insertCommentNode(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
            } else if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.normalName();
                if (name.equals("html")) {
                    return tb.process(t, InBody);
                } else if (name.equals("body")) {
                    tb.insertElementFor(startTag);
                    tb.framesetOk(false);
                    tb.transition(InBody);
                } else if (name.equals("frameset")) {
                    tb.insertElementFor(startTag);
                    tb.transition(InFrameset);
                } else if (inSorted(name, InBodyStartToHead)) {
                    tb.error(this);
                    Element head = tb.getHeadElement();
                    tb.push(head);
                    tb.process(t, InHead);
                    tb.removeFromStack(head);
                } else if (name.equals("head")) {
                    tb.error(this);
                    return false;
                } else {
                    anythingElse(t, tb);
                }
            } else if (t.isEndTag()) {
                String name = t.asEndTag().normalName();
                if (inSorted(name, AfterHeadBody)) {
                    anythingElse(t, tb);
                } else if (name.equals("template")) {
                    tb.process(t, InHead);
                }
                else {
                    tb.error(this);
                    return false;
                }
            } else {
                anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.processStartTag("body");
            tb.framesetOk(true);
            return tb.process(t);
        }
    },
    InBody {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case Character: {
                    Token.Character c = t.asCharacter();
                    c.normalizeNulls(false);
                    if (c.getData().isEmpty()) break;
                    boolean disableFrameset = tb.framesetOk() && !isWhitespace(c);
                    tb.reconstructFormattingElements();
                    tb.insertCharacterNode(c);
                    if (disableFrameset) tb.framesetOk(false);
                    break;
                }
                case Comment: {
                    tb.insertCommentNode(t.asComment());
                    break;
                }
                case Doctype: {
                    tb.error(this);
                    return false;
                }
                case StartTag:
                    return inBodyStartTag(t, tb);
                case EndTag:
                    return inBodyEndTag(t, tb);
                case EOF:
                    if (tb.templateModeSize() > 0)
                        return tb.process(t, InTemplate);
                    if (tb.onStackNot(InBodyEndOtherErrors))
                        tb.error(this);
                    // stop parsing
                    break;
                default:
                    Validate.wtf("Unexpected state: " + t.type); // XmlDecl only in XmlTreeBuilder
            }
            return true;
        }

        private boolean inBodyStartTag(Token t, HtmlTreeBuilder tb) {
            final Token.StartTag startTag = t.asStartTag();
            final String name = startTag.normalName();
            final ArrayList<Element> stack;
            Element el;

            switch (name) {
                case "a":
                    if (tb.getActiveFormattingElement("a") != null) {
                        tb.error(this);
                        tb.processEndTag("a");

                        // still on stack?
                        Element remainingA = tb.getFromStack("a");
                        if (remainingA != null) {
                            tb.removeFromActiveFormattingElements(remainingA);
                            tb.removeFromStack(remainingA);
                        }
                    }
                    tb.reconstructFormattingElements();
                    el = tb.insertElementFor(startTag);
                    tb.pushActiveFormattingElements(el);
                    break;
                case "span":
                    // same as final else, but short circuits lots of checks
                    tb.reconstructFormattingElements();
                    tb.insertElementFor(startTag);
                    break;
                case "li":
                    tb.framesetOk(false);
                    stack = tb.getStack();
                    for (int i = stack.size() - 1; i > 0; i--) {
                        el = stack.get(i);
                        if (el.nameIs("li")) {
                            tb.processEndTag("li");
                            break;
                        }
                        if (isSpecial(el) && !inSorted(el.normalName(), Constants.InBodyStartLiBreakers))
                            break;
                    }
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insertElementFor(startTag);
                    break;
                case "html":
                    tb.error(this);
                    if (tb.onStack("template")) return false; // ignore
                    // otherwise, merge attributes onto real html (if present)
                    stack = tb.getStack();
                    if (stack.size() > 0) {
                        Element html = tb.getStack().get(0);
                        mergeAttributes(startTag, html);
                    }
                    break;
                case "body":
                    tb.error(this);
                    stack = tb.getStack();
                    if (stack.size() < 2 || (stack.size() > 2 && !stack.get(1).nameIs("body")) || tb.onStack("template")) {
                        // only in fragment case
                        return false; // ignore
                    } else {
                        tb.framesetOk(false);
                        // will be on stack if this is a nested body. won't be if closed (which is a variance from spec, which leaves it on)
                        Element body = tb.getFromStack("body");
                        if (body != null) mergeAttributes(startTag, body);
                    }
                    break;
                case "frameset":
                    tb.error(this);
                    stack = tb.getStack();
                    if (stack.size() < 2|| (stack.size() > 2 && !stack.get(1).nameIs("body"))) {
                        // only in fragment case
                        return false; // ignore
                    } else if (!tb.framesetOk()) {
                        return false; // ignore frameset
                    } else {
                        Element second = stack.get(1);
                        if (second.parent() != null)
                            second.remove();
                        // pop up to html element
                        while (stack.size() > 1)
                            stack.remove(stack.size() - 1);
                        tb.insertElementFor(startTag);
                        tb.transition(InFrameset);
                    }
                    break;
                case "form":
                    if (tb.getFormElement() != null && !tb.isParsingTemplateContents()) {
                        tb.error(this);
                        return false;
                    }
                    if (tb.inButtonScope("p")) {
                        tb.closeElement("p");
                    }
                    tb.insertFormElement(startTag, true);
                    break;
                case "plaintext":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insertElementFor(startTag);
                    tb.tokeniser.transition(TokeniserState.PLAINTEXT); // once in, never gets out
                    break;
                case "button":
                    if (tb.inButtonScope("button")) {
                        // close and reprocess
                        tb.error(this);
                        tb.processEndTag("button");
                        tb.process(startTag);
                    } else {
                        tb.reconstructFormattingElements();
                        tb.insertElementFor(startTag);
                        tb.framesetOk(false);
                    }
                    break;
                case "nobr":
                    tb.reconstructFormattingElements();
                    if (tb.inScope("nobr")) {
                        tb.error(this);
                        tb.processEndTag("nobr");
                        tb.reconstructFormattingElements();
                    }
                    el = tb.insertElementFor(startTag);
                    tb.pushActiveFormattingElements(el);
                    break;
                case "table":
                    if (tb.getDocument().quirksMode() != Document.QuirksMode.quirks && tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insertElementFor(startTag);
                    tb.framesetOk(false);
                    tb.transition(InTable);
                    break;
                case "input":
                    if (tb.fragmentContextIs("select")) {
                        tb.error(this);
                        return false;
                    }
                    if (tb.inScope("select")) {
                        tb.error(this);
                        tb.popStackToClose("select");
                    }
                    tb.reconstructFormattingElements();
                    el = tb.insertEmptyElementFor(startTag);
                    if (!equalsIgnoreAsciiCase(el.attr("type"), "hidden"))
                        tb.framesetOk(false);
                    break;
                case "hr":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    if (tb.inScope("select")) {
                        tb.generateImpliedEndTags();
                        if (tb.inScope("option") || tb.inScope("optgroup"))
                            tb.error(this);
                    }
                    tb.insertEmptyElementFor(startTag);
                    tb.framesetOk(false);
                    break;
                case "image":
                    if (tb.getFromStack("svg") == null)
                        return tb.process(startTag.name("img")); // change <image> to <img>, unless in svg
                    else
                        tb.insertElementFor(startTag);
                    break;
                case "textarea":
                    tb.tokeniser.ignoreLeadingLf();
                    tb.framesetOk(false);
                    HandleTextState(startTag, tb, tb.tagFor(startTag).textState());
                    break;
                case "xmp":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.reconstructFormattingElements();
                    tb.framesetOk(false);
                    HandleTextState(startTag, tb, tb.tagFor(startTag).textState());
                    break;
                case "iframe":
                    tb.framesetOk(false);
                    HandleTextState(startTag, tb, tb.tagFor(startTag).textState());
                    break;
                case "noembed":
                    HandleTextState(startTag, tb, tb.tagFor(startTag).textState());
                    break;
                case "noscript":
                    tb.reconstructFormattingElements();
                    tb.startNoscript(startTag);
                    break;
                case "select":
                    if (tb.fragmentContextIs("select")) {
                        tb.error(this);
                        return false;
                    }
                    if (tb.inScope("select")) {
                        tb.error(this);
                        tb.popStackToClose("select");
                        break;
                    }
                    tb.reconstructFormattingElements();
                    tb.insertElementFor(startTag);
                    tb.framesetOk(false);
                    break;
                case "math":
                    tb.reconstructFormattingElements();
                    tb.insertForeignElementFor(startTag, Parser.NamespaceMathml);
                    break;
                case "svg":
                    tb.reconstructFormattingElements();
                    tb.insertForeignElementFor(startTag, Parser.NamespaceSvg);
                    break;
                // static final String[] Headings = new String[]{"h1", "h2", "h3", "h4", "h5", "h6"};
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    if (tb.hasCurrentElement() && inSorted(tb.currentElement().normalName(), Constants.Headings)) {
                        tb.error(this);
                        tb.pop();
                    }
                    tb.insertElementFor(startTag);
                    break;
                // static final String[] InBodyStartPreListing = new String[]{"listing", "pre"};
                case "pre":
                case "listing":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insertElementFor(startTag);
                    tb.tokeniser.ignoreLeadingLf();
                    tb.framesetOk(false);
                    break;
                // static final String[] DdDt = new String[]{"dd", "dt"};
                case "dd":
                case "dt":
                    tb.framesetOk(false);
                    stack = tb.getStack();
                    final int bottom = stack.size() - 1;
                    final int upper = bottom >= MaxStackScan ? bottom - MaxStackScan : 0;
                    for (int i = bottom; i >= upper; i--) {
                        el = stack.get(i);
                        if (inSorted(el.normalName(), Constants.DdDt)) {
                            tb.processEndTag(el.normalName());
                            break;
                        }
                        if (isSpecial(el) && !inSorted(el.normalName(), Constants.InBodyStartLiBreakers))
                            break;
                    }
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insertElementFor(startTag);
                    break;

                case "option":
                    if (tb.inScope("select")) {
                        tb.generateImpliedEndTags("optgroup");
                        if (tb.inScope("option"))
                            tb.error(this);
                    } else if (tb.currentElementIs("option")) {
                        tb.processEndTag("option");
                    }
                    tb.reconstructFormattingElements();
                    tb.insertElementFor(startTag);
                    break;

                case "optgroup":
                    if (tb.inScope("select")) {
                        tb.generateImpliedEndTags();
                        if (tb.inScope("option") || tb.inScope("optgroup"))
                            tb.error(this);
                    } else if (tb.currentElementIs("option")) {
                        tb.processEndTag("option");
                    }
                    tb.reconstructFormattingElements();
                    tb.insertElementFor(startTag);
                    break;

                case "rb":
                case "rtc":
                    if (tb.inScope("ruby")) {
                        tb.generateImpliedEndTags();
                        if (!tb.currentElementIs("ruby"))
                            tb.error(this);
                    }
                    tb.insertElementFor(startTag);
                    break;

                case "rp":
                case "rt":
                    if (tb.inScope("ruby")) {
                        tb.generateImpliedEndTags("rtc");
                        if (!tb.currentElementIs("rtc") && !tb.currentElementIs("ruby"))
                            tb.error(this);
                    }
                    tb.insertElementFor(startTag);
                    break;

                // InBodyStartEmptyFormatters:
                case "area":
                case "br":
                case "embed":
                case "img":
                case "keygen":
                case "wbr":
                    tb.reconstructFormattingElements();
                    tb.insertEmptyElementFor(startTag);
                    tb.framesetOk(false);
                    break;
                // Formatters:
                case "b":
                case "big":
                case "code":
                case "em":
                case "font":
                case "i":
                case "s":
                case "small":
                case "strike":
                case "strong":
                case "tt":
                case "u":
                    tb.reconstructFormattingElements();
                    el = tb.insertElementFor(startTag);
                    tb.pushActiveFormattingElements(el);
                    break;
                default:
                    Tag tag = tb.tagFor(startTag);
                    TokeniserState textState = tag.textState();
                    if (textState != null) { // custom rcdata or rawtext (if we were in head, will have auto-transitioned here)
                        HandleTextState(startTag, tb, textState);
                    } else if (!tag.isKnownTag()) { // no other special rules for custom tags
                        tb.insertElementFor(startTag);
                    } else if (inSorted(name, Constants.InBodyStartPClosers)) {
                        if (tb.inButtonScope("p")) tb.processEndTag("p");
                        tb.insertElementFor(startTag);
                    } else if (inSorted(name, Constants.InBodyStartToHead)) {
                        return tb.process(t, InHead);
                    } else if (inSorted(name, Constants.InBodyStartApplets)) {
                        tb.reconstructFormattingElements();
                        tb.insertElementFor(startTag);
                        tb.insertMarkerToFormattingElements();
                        tb.framesetOk(false);
                    } else if (inSorted(name, Constants.InBodyStartMedia)) {
                        tb.insertEmptyElementFor(startTag);
                    } else if (inSorted(name, Constants.InBodyStartDrop)) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.reconstructFormattingElements();
                        tb.insertElementFor(startTag);
                    }
            }
            return true;
        }
        private static final int MaxStackScan = 24; // used for DD / DT scan, prevents runaway

        private boolean inBodyEndTag(Token t, HtmlTreeBuilder tb) {
            final Token.EndTag endTag = t.asEndTag();
            final String name = endTag.normalName();

            switch (name) {
                case "template":
                    tb.process(t, InHead);
                    break;
                case "sarcasm": // *sigh*
                case "span":
                    // same as final fall through, but saves short circuit
                    return anyOtherEndTag(t, tb);
                case "li":
                    if (!tb.inListItemScope(name)) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.generateImpliedEndTags(name);
                        if (!tb.currentElementIs(name))
                            tb.error(this);
                        tb.popStackToClose(name);
                    }
                    break;
                case "body":
                    if (!tb.inScope("body")) {
                        tb.error(this);
                        return false;
                    } else {
                        if (tb.onStackNot(InBodyEndOtherErrors))
                            tb.error(this);
                        tb.trackNodePosition(tb.getFromStack("body"), false); // track source position of close; body is left on stack, in case of trailers
                        tb.transition(AfterBody);
                    }
                    break;
                case "html":
                    if (!tb.onStack("body")) {
                        tb.error(this);
                        return false; // ignore
                    } else {
                        if (tb.onStackNot(InBodyEndOtherErrors))
                            tb.error(this);
                        tb.transition(AfterBody);
                        return tb.process(t); // re-process
                    }

                case "form":
                    if (!tb.isParsingTemplateContents()) {
                        Element currentForm = tb.getFormElement();
                        tb.setFormElement(null);
                        if (currentForm == null || !tb.inScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.generateImpliedEndTags();
                        if (!tb.currentElementIs(name))
                            tb.error(this);
                        // remove currentForm from stack. will shift anything under up.
                        tb.removeFromStack(currentForm);
                    } else { // template on stack
                        if (!tb.inScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.generateImpliedEndTags();
                        if (!tb.currentElementIs(name)) tb.error(this);
                        tb.popStackToClose(name);
                    }
                    break;
                case "p":
                    if (!tb.inButtonScope(name)) {
                        tb.error(this);
                        tb.processStartTag(name); // if no p to close, creates an empty <p></p>
                        return tb.process(endTag);
                    } else {
                        tb.generateImpliedEndTags(name);
                        if (!tb.currentElementIs(name))
                            tb.error(this);
                        tb.popStackToClose(name);
                    }
                    break;
                case "dd":
                case "dt":
                    if (!tb.inScope(name)) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.generateImpliedEndTags(name);
                        if (!tb.currentElementIs(name))
                            tb.error(this);
                        tb.popStackToClose(name);
                    }
                    break;
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    if (!tb.hasHeadingInScope()) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.generateImpliedEndTags(name);
                        if (!tb.currentElementIs(name))
                            tb.error(this);
                        tb.popStackToClose(Constants.Headings);
                    }
                    break;
                case "br":
                    tb.error(this);
                    tb.processStartTag("br");
                    return false;
                default:
                    // todo - move rest to switch if desired
                    if (inSorted(name, Constants.InBodyEndAdoptionFormatters)) {
                        return inBodyEndTagAdoption(t, tb);
                    } else if (inSorted(name, Constants.InBodyEndClosers)) {
                        if (!tb.inScope(name)) {
                            // nothing to close
                            tb.error(this);
                            return false;
                        } else {
                            tb.generateImpliedEndTags();
                            if (!tb.currentElementIs(name))
                                tb.error(this);
                            tb.popStackToClose(name);
                        }
                    } else if (inSorted(name, Constants.InBodyStartApplets)) {
                        if (!tb.inScope("name")) {
                            if (!tb.inScope(name)) {
                                tb.error(this);
                                return false;
                            }
                            tb.generateImpliedEndTags();
                            if (!tb.currentElementIs(name))
                                tb.error(this);
                            tb.popStackToClose(name);
                            tb.clearFormattingElementsToLastMarker();
                        }
                    } else {
                        return anyOtherEndTag(t, tb);
                    }
            }
            return true;
        }

        boolean anyOtherEndTag(Token t, HtmlTreeBuilder tb) {
            final String name = t.asEndTag().normalName; // case insensitive search - goal is to preserve output case, not for the parse to be case sensitive
            final ArrayList<Element> stack = tb.getStack();

            // deviate from spec slightly to speed when super deeply nested
            Element elFromStack = tb.getFromStack(name);
            if (elFromStack == null) {
                tb.error(this);
                return false;
            }

            for (int pos = stack.size() - 1; pos >= 0; pos--) {
                Element node = stack.get(pos);
                if (node.nameIs(name)) {
                    tb.generateImpliedEndTags(name);
                    if (!tb.currentElementIs(name))
                        tb.error(this);
                    tb.popStackToClose(name);
                    break;
                } else {
                    if (isSpecial(node)) {
                        tb.error(this);
                        return false;
                    }
                }
            }
            return true;
        }

        /** Repairs misnested formatting elements using the adoption agency algorithm. */
        private boolean inBodyEndTagAdoption(Token t, HtmlTreeBuilder tb) {
            // https://html.spec.whatwg.org/multipage/parsing.html#adoption-agency-algorithm
            // 1: Let subject be token's tag name.
            String subject = t.asEndTag().normalName;

            // 2: Pop a matching current node if it is not in the list of active formatting elements.
            if (tb.currentElementIs(subject) && !tb.isInActiveFormattingElements(tb.currentElement())) {
                tb.pop();
                return true;
            }

            // 3–4.2: Repeat up to eight times.
            for (int outer = 0; outer < 8; outer++) {
                // 4.3: Find the last matching formattingElement after the last marker; otherwise handle any other end tag.
                Element formatEl = tb.getActiveFormattingElement(subject);
                if (formatEl == null)
                    return anyOtherEndTag(t, tb);

                // 4.4: If formattingElement is not on the stack, remove it from the formatting list and return.
                ArrayList<Element> stack = tb.getStack();
                int formatPos = stack.lastIndexOf(formatEl);
                if (formatPos == -1) {
                    tb.error(this);
                    tb.removeFromActiveFormattingElements(formatEl);
                    return true;
                }
                // 4.5: If formattingElement is not in scope, report a parse error and return.
                if (!tb.inScope(formatEl)) {
                    tb.error(this);
                    return false;
                }
                // 4.6: If formattingElement is not the current node, report a parse error but continue.
                if (tb.currentElement() != formatEl)
                    tb.error(this);

                // 4.7: Let furthestBlock be the topmost special node below formattingElement.
                Element furthestBlock = null;
                int blockPos = formatPos + 1;
                for (; blockPos < stack.size(); blockPos++) {
                    Element el = stack.get(blockPos);
                    if (isSpecial(el)) {
                        furthestBlock = el;
                        break;
                    }
                }

                // 4.8: Without a furthestBlock, pop through formattingElement, remove its formatting entry, and return.
                if (furthestBlock == null) {
                    while (tb.currentElement() != formatEl)
                        tb.pop();
                    tb.pop();
                    tb.removeFromActiveFormattingElements(formatEl);
                    return true;
                }

                // 4.9: Let commonAncestor be the element immediately above formattingElement on the stack.
                if (formatPos == 0) {
                    tb.error("No open parent element for misnested <%s>", formatEl.tagName());
                    return true;
                }
                Element commonAncestor = stack.get(formatPos - 1);

                // 4.10: Let a bookmark note the position of formattingElement in the formatting list.
                // initially it marks formatEl's slot; if moved in 4.13.7, it marks the gap after the new node, retaining the element keeps that position stable when earlier entries are removed
                Element bookmark = formatEl;
                Element lastNode = furthestBlock;
                int inner = 0;

                // 4.13: While true:
                // walking backwards by index preserves the predecessor when the current entry is removed
                for (int nodePos = blockPos - 1; ; nodePos--) {
                    // 4.13.1: Increment innerLoopCounter by 1.
                    inner++;
                    // 4.13.2: Let node be the element immediately above node in the stack.
                    if (nodePos < 0 || nodePos >= stack.size()) {
                        tb.error("Formatting element <%s> is no longer open during recovery", formatEl.tagName());
                        return true;
                    }
                    Element node = stack.get(nodePos);
                    // 4.13.3: If node is formattingElement, then break.
                    if (node == formatEl) break;

                    // 4.13.4: After three iterations, remove node from the formatting list if present.
                    if (inner > 3)
                        tb.removeFromActiveFormattingElements(node);
                    // 4.13.5: If node is not in the formatting list, remove it from the stack and continue.
                    if (!tb.isInActiveFormattingElements(node)) {
                        tb.removeFromStack(node);
                        continue;
                    }

                    // 4.13.6: Create a replacement for node and replace its entries in both lists.
                    Element replacement = tb.recreateElement(node);
                    tb.replaceActiveFormattingElement(node, replacement);
                    stack.set(nodePos, replacement);
                    node = replacement;

                    // 4.13.7: If lastNode is furthestBlock, move the bookmark to immediately after the new node.
                    if (lastNode == furthestBlock)
                        bookmark = node;
                    // 4.13.8: Append lastNode to node.
                    node.appendChild(lastNode);
                    // 4.13.9: Set lastNode to node.
                    lastNode = node;
                }

                // 4.14: Let (target, refNode) be the adjusted insertion location given (commonAncestor, null).
                // steps 4.15–4.16 (remove lastNode, then insert if valid) are in insertAdopted
                tb.insertAdopted(lastNode, commonAncestor);
                // 4.17: Create a replacement for formattingElement, with furthestBlock as the intended parent.
                Element replacement = tb.recreateElement(formatEl);
                // 4.18: Append all children of furthestBlock to the new element.
                for (Node child : furthestBlock.childNodes())
                    replacement.appendChild(child);
                // 4.19: Append that new element to furthestBlock.
                furthestBlock.appendChild(replacement);
                // 4.20: Replace formattingElement at the bookmark.
                tb.replaceFormattingElement(formatEl, replacement, bookmark);
                // 4.21: Remove formattingElement from the stack and insert the replacement immediately below furthestBlock.
                tb.removeFromStack(formatEl);
                tb.insertOnStackAfter(furthestBlock, replacement);
            }
            return true;
        }
    },
    Text {
        // in script, style etc. normally treated as data tags
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isCharacter()) {
                tb.insertCharacterNode(t.asCharacter());
            } else if (t.isEOF()) {
                tb.error(this);
                // if current node is script: already started
                tb.pop();
                tb.transition(tb.originalState());
                if (tb.state() == Text) // stack is such that we couldn't transition out; just close
                    tb.transition(InBody);
                return tb.process(t);
            } else if (t.isEndTag()) {
                // if: An end tag whose tag name is "script" -- scripting nesting level, if evaluating scripts
                tb.pop();
                tb.transition(tb.originalState());
            }
            return true;
        }
    },
    InTable {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isCharacter() && tb.hasCurrentElement() && inSorted(tb.currentElement().normalName(), InTableFoster)) {
                tb.resetPendingTableCharacters();
                tb.markInsertionMode();
                tb.transition(InTableText);
                return tb.process(t);
            } else if (t.isComment()) {
                tb.insertCommentNode(t.asComment());
                return true;
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.normalName();
                if (name.equals("caption")) {
                    tb.clearStackToTableContext();
                    tb.insertMarkerToFormattingElements();
                    tb.insertElementFor(startTag);
                    tb.transition(InCaption);
                } else if (name.equals("colgroup")) {
                    tb.clearStackToTableContext();
                    tb.insertElementFor(startTag);
                    tb.transition(InColumnGroup);
                } else if (name.equals("col")) {
                    tb.clearStackToTableContext();
                    tb.processStartTag("colgroup");
                    return tb.process(t);
                } else if (inSorted(name, InTableToBody)) {
                    tb.clearStackToTableContext();
                    tb.insertElementFor(startTag);
                    tb.transition(InTableBody);
                } else if (inSorted(name, InTableAddBody)) {
                    tb.clearStackToTableContext();
                    tb.processStartTag("tbody");
                    return tb.process(t);
                } else if (name.equals("table")) {
                    tb.error(this);
                    if (!tb.inTableScope(name)) { // ignore it
                        return false;
                    } else {
                        tb.popStackToClose(name);
                        if (!tb.resetInsertionMode()) {
                            // not per spec - but haven't transitioned out of table. so try something else
                            tb.insertElementFor(startTag);
                            return true;
                        }
                        return tb.process(t);
                    }
                } else if (inSorted(name, InTableToHead)) {
                    return tb.process(t, InHead);
                } else if (name.equals("noscript")) {
                    tb.startNoscript(startTag);
                } else if (name.equals("input")) {
                    if (!(startTag.hasAttributes() && equalsIgnoreAsciiCase(startTag.attributes.get("type"), "hidden"))) {
                        return anythingElse(t, tb);
                    } else {
                        tb.insertEmptyElementFor(startTag);
                    }
                } else if (name.equals("form")) {
                    tb.error(this);
                    if (tb.getFormElement() != null && !tb.isParsingTemplateContents())
                        return false;
                    tb.insertFormElement(startTag, false);
                } else {
                    return anythingElse(t, tb);
                }
                return true; // todo: check if should return processed http://www.whatwg.org/specs/web-apps/current-work/multipage/tree-construction.html#parsing-main-intable
            } else if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.normalName();

                if (name.equals("table")) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.popStackToClose("table");
                        tb.resetInsertionMode();
                    }
                } else if (inSorted(name, InTableEndErr)) {
                    tb.error(this);
                    return false;
                } else if (name.equals("template")) {
                    tb.process(t, InHead);
                } else {
                    return anythingElse(t, tb);
                }
                return true; // todo: as above todo
            } else if (t.isEOF()) {
                return tb.process(t, InBody);
            }
            return anythingElse(t, tb);
        }

        boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.error(this);
            tb.processWithFosterInserts(t);
            return true;
        }
    },
    InTableText {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.type == Token.TokenType.Character) {
                Token.Character c = t.asCharacter();
                c.normalizeNulls(false);
                if (!c.getData().isEmpty()) tb.addPendingTableCharacters(c);
            } else {
                // insert gathered table text into the correct element:
                if (tb.getPendingTableCharacters().size() > 0) {
                    final Token og = tb.currentToken; // update current token, so we can track cursor pos correctly
                    for (Token.Character c : tb.getPendingTableCharacters()) {
                        tb.currentToken = c;
                        if (!isWhitespace(c)) {
                            // InTable anything else section:
                            tb.error(this);
                            tb.processWithFosterInserts(c);
                        } else
                            tb.insertCharacterNode(c);
                    }
                    tb.currentToken = og;
                    tb.resetPendingTableCharacters();
                }
                tb.transition(tb.originalState());
                return tb.process(t);
            }
            return true;
        }
    },
    InCaption {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isEndTag() && t.asEndTag().normalName().equals("caption")) {
                if (!tb.inTableScope("caption")) { // fragment case
                    tb.error(this);
                    return false;
                } else {
                    tb.generateImpliedEndTags();
                    if (!tb.currentElementIs("caption")) tb.error(this);
                    tb.popStackToClose("caption");
                    tb.clearFormattingElementsToLastMarker();
                    tb.transition(InTable);
                }
            } else if ((
                    t.isStartTag() && inSorted(t.asStartTag().normalName(), InCellCol) ||
                            t.isEndTag() && t.asEndTag().normalName().equals("table"))
                    ) {
                // same as above but processes after transition
                if (!tb.inTableScope("caption")) { // fragment case
                    tb.error(this);
                    return false;
                }
                tb.generateImpliedEndTags(false);
                if (!tb.currentElementIs("caption")) tb.error(this);
                tb.popStackToClose("caption");
                tb.clearFormattingElementsToLastMarker();
                tb.transition(InTable);
                InTable.process(t, tb); // doesn't check foreign context
            } else if (t.isEndTag() && inSorted(t.asEndTag().normalName(), InCaptionIgnore)) {
                tb.error(this);
                return false;
            } else {
                return tb.process(t, InBody);
            }
            return true;
        }
    },
    InColumnGroup {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insertCharacterNode(t.asCharacter());
                return true;
            }
            switch (t.type) {
                case Comment:
                    tb.insertCommentNode(t.asComment());
                    break;
                case Doctype:
                    tb.error(this);
                    break;
                case StartTag:
                    Token.StartTag startTag = t.asStartTag();
                    switch (startTag.normalName()) {
                        case "html":
                            return tb.process(t, InBody);
                        case "col":
                            tb.insertEmptyElementFor(startTag);
                            break;
                        case "template":
                            tb.process(t, InHead);
                            break;
                        default:
                            return anythingElse(t, tb);
                    }
                    break;
                case EndTag:
                    Token.EndTag endTag = t.asEndTag();
                    String name = endTag.normalName();
                    switch (name) {
                        case "colgroup":
                            if (!tb.currentElementIs(name)) {
                                tb.error(this);
                                return false;
                            } else {
                                tb.pop();
                                tb.transition(InTable);
                            }
                            break;
                        case "col":
                            tb.error(this);
                            return false;
                        case "template":
                            tb.process(t, InHead);
                            break;
                        default:
                            return anythingElse(t, tb);
                    }
                    break;
                case EOF:
                    return tb.process(t, InBody);
                default:
                    return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            if (!tb.currentElementIs("colgroup")) {
                tb.error(this);
                return false;
            }
            tb.pop();
            tb.transition(InTable);
            tb.process(t);
            return true;
        }
    },
    InTableBody {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            final String name;

            switch (t.type) {
                case StartTag:
                    Token.StartTag startTag = t.asStartTag();
                    name = startTag.normalName();
                    if (name.equals("tr")) {
                        tb.clearStackToTableBodyContext();
                        tb.insertElementFor(startTag);
                        tb.transition(InRow);
                    } else if (inSorted(name, InCellNames)) {
                        tb.error(this);
                        tb.processStartTag("tr");
                        return tb.process(startTag);
                    } else if (inSorted(name, InTableBodyExit)) {
                        return exitTableBody(t, tb);
                    } else
                        return anythingElse(t, tb);
                    break;
                case EndTag:
                    Token.EndTag endTag = t.asEndTag();
                    name = endTag.normalName();
                    if (inSorted(name, InTableEndIgnore)) {
                        if (!tb.inTableScope(name)) {
                            tb.error(this);
                            return false;
                        } else {
                            tb.clearStackToTableBodyContext();
                            tb.pop();
                            tb.transition(InTable);
                        }
                    } else if (name.equals("table")) {
                        return exitTableBody(t, tb);
                    } else if (inSorted(name, InTableBodyEndIgnore)) {
                        tb.error(this);
                        return false;
                    } else
                        return anythingElse(t, tb);
                    break;
                default:
                    return anythingElse(t, tb);
            }
            return true;
        }

        private boolean exitTableBody(Token t, HtmlTreeBuilder tb) {
            if (!(tb.inTableScope("tbody") || tb.inTableScope("thead") || tb.inTableScope("tfoot"))) {
                // frag case
                tb.error(this);
                return false;
            }
            tb.clearStackToTableBodyContext();
            tb.processEndTag(tb.currentElement().normalName()); // tbody, tfoot, thead
            return tb.process(t);
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            return tb.process(t, InTable);
        }
    },
    InRow {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.normalName();

                if (inSorted(name, InCellNames)) { // td, th
                    tb.clearStackToTableRowContext();
                    tb.insertElementFor(startTag);
                    tb.transition(InCell);
                    tb.insertMarkerToFormattingElements();
                } else if (inSorted(name, InRowMissing)) { // "caption", "col", "colgroup", "tbody", "tfoot", "thead", "tr"
                    if (!tb.inTableScope("tr")) {
                        tb.error(this);
                        return false;
                    }
                    tb.clearStackToTableRowContext();
                    tb.pop(); // tr
                    tb.transition(InTableBody);
                    return tb.process(t);
                } else {
                    return anythingElse(t, tb);
                }
            } else if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.normalName();

                if (name.equals("tr")) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this); // frag
                        return false;
                    }
                    tb.clearStackToTableRowContext();
                    tb.pop(); // tr
                    tb.transition(InTableBody);
                } else if (name.equals("table")) {
                    if (!tb.inTableScope("tr")) {
                        tb.error(this);
                        return false;
                    }
                    tb.clearStackToTableRowContext();
                    tb.pop(); // tr
                    tb.transition(InTableBody);
                    return tb.process(t);
                } else if (inSorted(name, InTableToBody)) { // "tbody", "tfoot", "thead"
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        return false;
                    }
                    if (!tb.inTableScope("tr")) {
                        // not an error per spec?
                        return false;
                    }
                    tb.clearStackToTableRowContext();
                    tb.pop(); // tr
                    tb.transition(InTableBody);
                    return tb.process(t);
                } else if (inSorted(name, InRowIgnore)) {
                    tb.error(this);
                    return false;
                } else {
                    return anythingElse(t, tb);
                }
            } else {
                return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            return tb.process(t, InTable);
        }
    },
    InCell {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.normalName();

                if (inSorted(name, Constants.InCellNames)) { // td, th
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        tb.transition(InRow); // might not be in scope if empty: <td /> and processing fake end tag
                        return false;
                    }
                    tb.generateImpliedEndTags();
                    if (!tb.currentElementIs(name))
                        tb.error(this);
                    tb.popStackToClose(name);
                    tb.clearFormattingElementsToLastMarker();
                    tb.transition(InRow);
                } else if (inSorted(name, Constants.InCellBody)) {
                    tb.error(this);
                    return false;
                } else if (inSorted(name, Constants.InCellTable)) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        return false;
                    }
                    closeCell(tb);
                    return tb.process(t);
                } else {
                    return anythingElse(t, tb);
                }
            } else if (t.isStartTag() &&
                    inSorted(t.asStartTag().normalName(), Constants.InCellCol)) {
                if (!(tb.inTableScope("td") || tb.inTableScope("th"))) {
                    tb.error(this);
                    return false;
                }
                closeCell(tb);
                return tb.process(t);
            } else {
                return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            return tb.process(t, InBody);
        }

        private void closeCell(HtmlTreeBuilder tb) {
            if (tb.inTableScope("td"))
                tb.processEndTag("td");
            else
                tb.processEndTag("th"); // only here if th or td in scope
        }
    },
    InTemplate {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            final String name;
            switch (t.type) {
                case Character:
                case Comment:
                case Doctype:
                    tb.process(t, InBody);
                    break;
                case StartTag:
                    name = t.asStartTag().normalName();
                    if (inSorted(name, InTemplateToHead))
                        tb.process(t, InHead);
                    else if (inSorted(name, InTemplateToTable)) {
                        tb.popTemplateMode();
                        tb.pushTemplateMode(InTable);
                        tb.transition(InTable);
                        return tb.process(t);
                    }
                    else if (name.equals("col")) {
                        tb.popTemplateMode();
                        tb.pushTemplateMode(InColumnGroup);
                        tb.transition(InColumnGroup);
                        return tb.process(t);
                    } else if (name.equals("tr")) {
                        tb.popTemplateMode();
                        tb.pushTemplateMode(InTableBody);
                        tb.transition(InTableBody);
                        return tb.process(t);
                    } else if (name.equals("td") || name.equals("th")) {
                        tb.popTemplateMode();
                        tb.pushTemplateMode(InRow);
                        tb.transition(InRow);
                        return tb.process(t);
                    } else {
                        tb.popTemplateMode();
                        tb.pushTemplateMode(InBody);
                        tb.transition(InBody);
                        return tb.process(t);
                    }

                    break;
                case EndTag:
                    name = t.asEndTag().normalName();
                    if (name.equals("template"))
                        tb.process(t, InHead);
                    else {
                        tb.error(this);
                        return false;
                    }
                    break;
                case EOF:
                    if (!tb.onStack("template")) { // stop parsing
                        return true;
                    }
                    while (tb.onStack("template")) {
                        tb.error(this);
                        tb.popStackToClose("template");
                        tb.clearFormattingElementsToLastMarker();
                        tb.popTemplateMode();
                        tb.resetInsertionMode();
                    }
                    return tb.process(t);
                default:
                    Validate.wtf("Unexpected state: " + t.type); // XmlDecl only in XmlTreeBuilder
            }
            return true;
        }
    },
    AfterBody {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            Element html = tb.getFromStack("html");
            if (isWhitespace(t)) {
                // spec deviation - currently body is still on stack, but we want this to go to the html node
                if (html != null)
                    tb.insertCharacterToElement(t.asCharacter(), html);
                else
                    tb.process(t, InBody); // will get into body
            } else if (t.isComment()) {
                if (html != null)
                    tb.insertCommentNode(t.asComment(), html);
                else
                    tb.insertCommentNode(t.asComment()); // fragment fallback
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return tb.process(t, InBody);
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("html")) {
                if (tb.isFragmentParsing()) {
                    tb.error(this);
                    return false;
                } else {
                    if (html != null) tb.trackNodePosition(html, false); // track source position of close; html is left on stack, in case of trailers
                    tb.transition(AfterAfterBody);
                }
            } else if (t.isEOF()) {
                // chillax! we're done
            } else {
                tb.error(this);
                tb.resetBody();
                return tb.process(t);
            }
            return true;
        }
    },
    InFrameset {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isCharacter()) {
                processFramesetCharacters(t.asCharacter(), tb, this);
            } else if (t.isComment()) {
                tb.insertCommentNode(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag()) {
                Token.StartTag start = t.asStartTag();
                switch (start.normalName()) {
                    case "html":
                        return tb.process(start, InBody);
                    case "frameset":
                        tb.insertElementFor(start);
                        break;
                    case "frame":
                        tb.insertEmptyElementFor(start);
                        break;
                    case "noframes":
                        return tb.process(start, InHead);
                    default:
                        tb.error(this);
                        return false;
                }
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("frameset")) {
                if (!tb.currentElementIs("frameset")) { // spec checks if el is html; deviate to confirm we are about to pop the frameset el
                    tb.error(this);
                    return false;
                } else {
                    tb.pop();
                    if (!tb.isFragmentParsing() && !tb.currentElementIs("frameset")) {
                        tb.transition(AfterFrameset);
                    }
                }
            } else if (t.isEOF()) {
                if (!tb.currentElementIs("html")) {
                    tb.error(this);
                    return true;
                }
            } else {
                tb.error(this);
                return false;
            }
            return true;
        }
    },
    AfterFrameset {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isCharacter()) {
                processFramesetCharacters(t.asCharacter(), tb, this);
            } else if (t.isComment()) {
                tb.insertCommentNode(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return tb.process(t, InBody);
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("html")) {
                tb.transition(AfterAfterFrameset);
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("noframes")) {
                return tb.process(t, InHead);
            } else if (t.isEOF()) {
                // cool your heels, we're complete
            } else {
                tb.error(this);
                return false;
            }
            return true;
        }
    },
    AfterAfterBody {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isComment()) {
                tb.insertCommentNode(t.asComment(), tb.getDocument());
            } else if (t.isDoctype() || (t.isStartTag() && t.asStartTag().normalName().equals("html"))) {
                return tb.process(t, InBody);
            } else if (isWhitespace(t)) {
                // spec deviation - body and html still on stack, but want this space to go after </html>
                Element doc = tb.getDocument();
                tb.insertCharacterToElement(t.asCharacter(), doc);
            }else if (t.isEOF()) {
                // nice work chuck
            } else {
                tb.error(this);
                tb.resetBody();
                return tb.process(t);
            }
            return true;
        }
    },
    AfterAfterFrameset {
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isComment()) {
                tb.insertCommentNode(t.asComment(), tb.getDocument());
            } else if (t.isDoctype() || isWhitespace(t) || (t.isStartTag() && t.asStartTag().normalName().equals("html"))) {
                return tb.process(t, InBody);
            } else if (t.isEOF()) {
                // nice work chuck
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("noframes")) {
                return tb.process(t, InHead);
            } else {
                tb.error(this);
                return false;
            }
            return true;
        }
    },
    ForeignContent {
        // https://html.spec.whatwg.org/multipage/parsing.html#parsing-main-inforeign
        @Override boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case Character:
                    Token.Character c = t.asCharacter();
                    // nulls produce replacement text but do not disable a later frameset
                    boolean disableFrameset = tb.framesetOk() && !StringUtil.isBlank(
                        c.hasNull ? c.getData().replace(nullString, "") : c.getData());
                    tb.insertCharacterNode(c, true);
                    if (disableFrameset) tb.framesetOk(false);
                    break;
                case Comment:
                    tb.insertCommentNode(t.asComment());
                    break;
                case Doctype:
                    tb.error(this);
                    break;
                case StartTag:
                    Token.StartTag start = t.asStartTag();
                    if (StringUtil.in(start.normalName, InForeignToHtml))
                        return breakoutToHtml(t, tb);
                    if (start.normalName.equals("font") && (
                        start.hasAttributeIgnoreCase("color")
                            || start.hasAttributeIgnoreCase("face")
                            || start.hasAttributeIgnoreCase("size")))
                        return breakoutToHtml(t, tb);

                    // Any other start:
                    // (whatwg says to fix up tag name and attribute case per a table - we will preserve original case instead)
                    String namespace = tb.currentElNs();
                    tb.insertForeignElementFor(start, namespace);
                    // (self-closing handled in insert)
                    // if self-closing svg script -- level and execution elided

                    // seemingly not in spec, but as browser behavior, get into ScriptData state for svg script; and allow custom data tags
                    TokeniserState textState = tb.tagFor(start.tagName.value(), start.normalName, namespace, tb.settings).textState();
                    if (textState != null) {
                        if (start.normalName.equals("script"))
                            tb.tokeniser.transition(TokeniserState.ScriptData);
                        else
                            tb.tokeniser.transition(textState);
                    }

                    break;

                case EndTag:
                    Token.EndTag end = t.asEndTag();
                    if (end.normalName.equals("br") || end.normalName.equals("p"))
                        return breakoutToHtml(t, tb);
                    if (end.normalName.equals("script") && tb.currentElementIs("script", Parser.NamespaceSvg)) {
                        // script level and execution elided.
                        tb.pop();
                        return true;
                    }

                    // Any other end tag
                    ArrayList<Element> stack = tb.getStack();
                    if (stack.isEmpty())
                        Validate.wtf("Stack unexpectedly empty");
                    int i = stack.size() - 1;
                    Element el = stack.get(i);
                    if (!el.nameIs(end.normalName))
                        tb.error(this);
                    while (i != 0) {
                        if (el.nameIs(end.normalName)) {
                            tb.popStackToCloseAnyNamespace(el.normalName());
                            return true;
                        }
                        i--;
                        el = stack.get(i);
                        if (isHtmlEl(el)) {
                            return processAsHtml(t, tb);
                        }
                    }
                    break;

                case EOF:
                    // won't come through here, but for completion:
                    break;
                default:
                    Validate.wtf("Unexpected state: " + t.type); // XmlDecl only in XmlTreeBuilder
            }
            return true;
        }

        /** Reprocesses a token as HTML, retaining the open-element stack. */
        boolean processAsHtml(Token t, HtmlTreeBuilder tb) {
            return tb.state().process(t, tb);
        }

        /** Pops foreign elements, then reprocesses the breakout token as HTML. */
        boolean breakoutToHtml(Token t, HtmlTreeBuilder tb) {
            // https://html.spec.whatwg.org/multipage/parsing.html#parsing-main-inforeign
            while (!tb.getStack().isEmpty()) {
                Element current = tb.currentElement();
                if (isHtmlEl(current)
                    || HtmlTreeBuilder.isMathmlTextIntegration(current)
                    || HtmlTreeBuilder.isHtmlIntegration(current))
                    break;
                tb.pop();
            }
            return processAsHtml(t, tb);
        }
    };

    private static void mergeAttributes(Token.StartTag source, Element dest) {
        if (!source.hasAttributes()) return;
        source.finaliseAttributeRanges(source.treeBuilder.settings);
        for (Attribute attr : source.attributes) { // only iterates public attributes
            Attributes destAttrs = dest.attributes();
            if (!destAttrs.hasKey(attr.getKey())) {
                Range.AttributeRange range = attr.sourceRange(); // grab before its parent changes
                destAttrs.put(attr);
                NodeInternals.attributeRange(destAttrs, attr.getKey(), range);
            }
        }
    }

    private static final String nullString = String.valueOf('\u0000');

    abstract boolean process(Token t, HtmlTreeBuilder tb);

    private static boolean isWhitespace(Token t) {
        if (t.isCharacter()) {
            String data = t.asCharacter().getData();
            return StringUtil.isBlank(data);
        }
        return false;
    }

    /** Applies the frameset insertion rules to each character in the token. */
    private static void processFramesetCharacters(Token.Character character, HtmlTreeBuilder tb, HtmlTreeBuilderState state) {
        String data = character.getData();
        if (StringUtil.isBlank(data)) {
            tb.insertCharacterNode(character); // keep the token and its source range; no filtering needed
            return;
        }

        tb.error(state);
        String whitespace = data.replaceAll("[^\\t\\n\\f\\r ]", ""); // ignore text, but keep whitespace in the same token
        if (!whitespace.isEmpty()) {
            character.data(whitespace); // reuse the token to keep its source range
            tb.insertCharacterNode(character);
        }
    }

    /** Inserts a text element and switches the tokenizer and tree builder into their text states. */
    private static void HandleTextState(Token.StartTag startTag, HtmlTreeBuilder tb, @Nullable TokeniserState state) {
        if (state != null)
            tb.tokeniser.transition(state);
        tb.markInsertionMode();
        tb.transition(Text);
        tb.insertElementFor(startTag);
    }

    // lists of tags to search through
    static final class Constants {
        static final String[] InHeadEmpty = new String[]{"base", "basefont", "bgsound", "command", "link"};
        static final String[] InHeadRaw = new String[]{"noframes", "style"};
        static final String[] InHeadEnd = new String[]{"body", "br", "html"};
        static final String[] AfterHeadBody = new String[]{"body", "br", "html"};
        static final String[] BeforeHtmlToHead = new String[]{"body", "br", "head", "html", };
        static final String[] InBodyStartToHead = new String[]{"base", "basefont", "bgsound", "command", "link", "meta", "noframes", "script", "style", "template", "title"};
        static final String[] InBodyStartPClosers = new String[]{"address", "article", "aside", "blockquote", "center", "details", "dir", "div", "dl",
            "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "menu", "nav", "ol",
            "p", "section", "summary", "ul"};
        static final String[] Headings = new String[]{"h1", "h2", "h3", "h4", "h5", "h6"};
        static final String[] InBodyStartLiBreakers = new String[]{"address", "div", "p"};
        static final String[] DdDt = new String[]{"dd", "dt"};
        static final String[] InBodyStartApplets = new String[]{"applet", "marquee", "object"};
        static final String[] InBodyStartMedia = new String[]{"param", "source", "track"};
        static final String[] InBodyStartInputAttribs = new String[]{"action", "name", "prompt"};
        static final String[] InBodyStartDrop = new String[]{"caption", "col", "colgroup", "frame", "head", "tbody", "td", "tfoot", "th", "thead", "tr"};
        static final String[] InBodyEndClosers = new String[]{"address", "article", "aside", "blockquote", "button", "center", "details", "dir", "div",
            "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "listing", "menu",
            "nav", "ol", "pre", "section", "select", "summary", "ul"};
        static final String[] InBodyEndOtherErrors = new String[] {"body", "dd", "dt", "html", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc", "tbody", "td", "tfoot", "th", "thead", "tr"};
        static final String[] InBodyEndAdoptionFormatters = new String[]{"a", "b", "big", "code", "em", "font", "i", "nobr", "s", "small", "strike", "strong", "tt", "u"};
        static final String[] InTableToBody = new String[]{"tbody", "tfoot", "thead"};
        static final String[] InTableAddBody = new String[]{"td", "th", "tr"};
        static final String[] InTableToHead = new String[]{"script", "style", "template"};
        static final String[] InCellNames = new String[]{"td", "th"};
        static final String[] InCellBody = new String[]{"body", "caption", "col", "colgroup", "html"};
        static final String[] InCellTable = new String[]{ "table", "tbody", "tfoot", "thead", "tr"};
        static final String[] InCellCol = new String[]{"caption", "col", "colgroup", "tbody", "td", "tfoot", "th", "thead", "tr"};
        static final String[] InTableEndErr = new String[]{"body", "caption", "col", "colgroup", "html", "tbody", "td", "tfoot", "th", "thead", "tr"};
        static final String[] InTableFoster = new String[]{"table", "tbody", "tfoot", "thead", "tr"};
        static final String[] InTableBodyExit = new String[]{"caption", "col", "colgroup", "tbody", "tfoot", "thead"};
        static final String[] InTableBodyEndIgnore = new String[]{"body", "caption", "col", "colgroup", "html", "td", "th", "tr"};
        static final String[] InRowMissing = new String[]{"caption", "col", "colgroup", "tbody", "tfoot", "thead", "tr"};
        static final String[] InRowIgnore = new String[]{"body", "caption", "col", "colgroup", "html", "td", "th"};
        static final String[] InTableEndIgnore = new String[]{"tbody", "tfoot", "thead"};
        static final String[] InCaptionIgnore = new String[]{"body", "col", "colgroup", "html", "tbody", "td", "tfoot", "th", "thead", "tr"};
        static final String[] InTemplateToHead = new String[] {"base", "basefont", "bgsound", "link", "meta", "noframes", "script", "style", "template", "title"};
        static final String[] InTemplateToTable = new String[] {"caption", "colgroup", "tbody", "tfoot", "thead"};
        static final String[] InForeignToHtml = new String[] {"b", "big", "blockquote", "body", "br", "center", "code", "dd", "div", "dl", "dt", "em", "embed", "h1", "h2", "h3", "h4", "h5", "h6", "head", "hr", "i", "img", "li", "listing", "menu", "meta", "nobr", "ol", "p", "pre", "ruby", "s", "small", "span", "strike", "strong", "sub", "sup", "table", "tt", "u", "ul", "var"};
    }
}
