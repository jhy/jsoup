package org.jsoup.parser;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.ArrayList;

import static org.jsoup.internal.StringUtil.inSorted;
import static org.jsoup.parser.HtmlTreeBuilderState.Constants.*;

/**
 * The Tree Builder's current state. Each state embodies the processing for the state, and transitions to other states.
 */
enum HtmlTreeBuilderState {
    Initial {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                return true; // ignore whitespace until we get the first content
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                // todo: parse error check on expected doctypes
                // todo: quirk state check on doctype ids
                Token.Doctype d = t.asDoctype();
                DocumentType doctype = new DocumentType(
                    tb.settings.normalizeTag(d.getName()), d.getPublicIdentifier(), d.getSystemIdentifier());
                doctype.setPubSysKey(d.getPubSysKey());
                tb.getDocument().appendChild(doctype);
                tb.onNodeInserted(doctype, t);
                if (d.isForceQuirks())
                    tb.getDocument().quirksMode(Document.QuirksMode.quirks);
                tb.transition(BeforeHtml);
            } else {
                // todo: check not iframe srcdoc
                tb.transition(BeforeHtml);
                return tb.process(t); // re-process token
            }
            return true;
        }
    },
    BeforeHtml {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (isWhitespace(t)) {
                tb.insert(t.asCharacter()); // out of spec - include whitespace
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                tb.insert(t.asStartTag());
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
            tb.insertStartTag("html");
            tb.transition(BeforeHead);
            return tb.process(t);
        }
    },
    BeforeHead {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter()); // out of spec - include whitespace
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return InBody.process(t, tb); // does not transition
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("head")) {
                Element head = tb.insert(t.asStartTag());
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter()); // out of spec - include whitespace
                return true;
            }
            switch (t.type) {
                case Comment:
                    tb.insert(t.asComment());
                    break;
                case Doctype:
                    tb.error(this);
                    return false;
                case StartTag:
                    Token.StartTag start = t.asStartTag();
                    String name = start.normalName();
                    if (name.equals("html")) {
                        return InBody.process(t, tb);
                    } else if (inSorted(name, InHeadEmpty)) {
                        Element el = tb.insertEmpty(start);
                        // jsoup special: update base the first time it is seen
                        if (name.equals("base") && el.hasAttr("href"))
                            tb.maybeSetBaseUri(el);
                    } else if (name.equals("meta")) {
                        tb.insertEmpty(start);
                        // todo: charset switches
                    } else if (name.equals("title")) {
                        handleRcData(start, tb);
                    } else if (inSorted(name, InHeadRaw)) {
                        handleRawtext(start, tb);
                    } else if (name.equals("noscript")) {
                        // else if noscript && scripting flag = true: rawtext (jsoup doesn't run script, to handle as noscript)
                        tb.insert(start);
                        tb.transition(InHeadNoscript);
                    } else if (name.equals("script")) {
                        // skips some script rules as won't execute them
                        tb.tokeniser.transition(TokeniserState.ScriptData);
                        tb.markInsertionMode();
                        tb.transition(Text);
                        tb.insert(start);
                    } else if (name.equals("head")) {
                        tb.error(this);
                        return false;
                    } else if (name.equals("template")) {
                        tb.insert(start);
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
                            if (!name.equals(tb.currentElement().normalName())) tb.error(this);
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
    InHeadNoscript {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isDoctype()) {
                tb.error(this);
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return tb.process(t, InBody);
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("noscript")) {
                tb.pop();
                tb.transition(InHead);
            } else if (isWhitespace(t) || t.isComment() || (t.isStartTag() && inSorted(t.asStartTag().normalName(),
                    InHeadNoScriptHead))) {
                return tb.process(t, InHead);
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("br")) {
                return anythingElse(t, tb);
            } else if ((t.isStartTag() && inSorted(t.asStartTag().normalName(), InHeadNoscriptIgnore)) || t.isEndTag()) {
                tb.error(this);
                return false;
            } else {
                return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            // note that this deviates from spec, which is to pop out of noscript and reprocess in head:
            // https://html.spec.whatwg.org/multipage/parsing.html#parsing-main-inheadnoscript
            // allows content to be inserted as data
            tb.error(this);
            tb.insert(new Token.Character().data(t.toString()));
            return true;
        }
    },
    AfterHead {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter());
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
            } else if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.normalName();
                if (name.equals("html")) {
                    return tb.process(t, InBody);
                } else if (name.equals("body")) {
                    tb.insert(startTag);
                    tb.framesetOk(false);
                    tb.transition(InBody);
                } else if (name.equals("frameset")) {
                    tb.insert(startTag);
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case Character: {
                    Token.Character c = t.asCharacter();
                    if (c.getData().equals(nullString)) {
                        // todo confirm that check
                        tb.error(this);
                        return false;
                    } else if (tb.framesetOk() && isWhitespace(c)) { // don't check if whitespace if frames already closed
                        tb.reconstructFormattingElements();
                        tb.insert(c);
                    } else {
                        tb.reconstructFormattingElements();
                        tb.insert(c);
                        tb.framesetOk(false);
                    }
                    break;
                }
                case Comment: {
                    tb.insert(t.asComment());
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
                    // todo: error if stack contains something not dd, dt, li, p, tbody, td, tfoot, th, thead, tr, body, html
                    // stop parsing
                    break;
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
                    el = tb.insert(startTag);
                    tb.pushActiveFormattingElements(el);
                    break;
                case "span":
                    // same as final else, but short circuits lots of checks
                    tb.reconstructFormattingElements();
                    tb.insert(startTag);
                    break;
                case "li":
                    tb.framesetOk(false);
                    stack = tb.getStack();
                    for (int i = stack.size() - 1; i > 0; i--) {
                        el = stack.get(i);
                        if (el.normalName().equals("li")) {
                            tb.processEndTag("li");
                            break;
                        }
                        if (tb.isSpecial(el) && !inSorted(el.normalName(), Constants.InBodyStartLiBreakers))
                            break;
                    }
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insert(startTag);
                    break;
                case "html":
                    tb.error(this);
                    if (tb.onStack("template")) return false; // ignore
                    // otherwise, merge attributes onto real html (if present)
                    stack = tb.getStack();
                    if (stack.size() > 0) {
                        Element html = tb.getStack().get(0);
                        if (startTag.hasAttributes()) {
                            for (Attribute attribute : startTag.attributes) {
                                if (!html.hasAttr(attribute.getKey()))
                                    html.attributes().put(attribute);
                            }
                        }
                    }
                    break;
                case "body":
                    tb.error(this);
                    stack = tb.getStack();
                    if (stack.size() == 1 || (stack.size() > 2 && !stack.get(1).normalName().equals("body")) || tb.onStack("template")) {
                        // only in fragment case
                        return false; // ignore
                    } else {
                        tb.framesetOk(false);
                        // will be on stack if this is a nested body. won't be if closed (which is a variance from spec, which leaves it on)
                        Element body;
                        if (startTag.hasAttributes() && (body = tb.getFromStack("body")) != null) { // we only ever put one body on stack
                            for (Attribute attribute : startTag.attributes) {
                                if (!body.hasAttr(attribute.getKey()))
                                    body.attributes().put(attribute);
                            }
                        }
                    }
                    break;
                case "frameset":
                    tb.error(this);
                    stack = tb.getStack();
                    if (stack.size() == 1 || (stack.size() > 2 && !stack.get(1).normalName().equals("body"))) {
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
                        tb.insert(startTag);
                        tb.transition(InFrameset);
                    }
                    break;
                case "form":
                    if (tb.getFormElement() != null && !tb.onStack("template")) {
                        tb.error(this);
                        return false;
                    }
                    if (tb.inButtonScope("p")) {
                        tb.closeElement("p");
                    }
                    tb.insertForm(startTag, true, true); // won't associate to any template
                    break;
                case "plaintext":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insert(startTag);
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
                        tb.insert(startTag);
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
                    el = tb.insert(startTag);
                    tb.pushActiveFormattingElements(el);
                    break;
                case "table":
                    if (tb.getDocument().quirksMode() != Document.QuirksMode.quirks && tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insert(startTag);
                    tb.framesetOk(false);
                    tb.transition(InTable);
                    break;
                case "input":
                    tb.reconstructFormattingElements();
                    el = tb.insertEmpty(startTag);
                    if (!el.attr("type").equalsIgnoreCase("hidden"))
                        tb.framesetOk(false);
                    break;
                case "hr":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insertEmpty(startTag);
                    tb.framesetOk(false);
                    break;
                case "image":
                    if (tb.getFromStack("svg") == null)
                        return tb.process(startTag.name("img")); // change <image> to <img>, unless in svg
                    else
                        tb.insert(startTag);
                    break;
                case "isindex":
                    // how much do we care about the early 90s?
                    tb.error(this);
                    if (tb.getFormElement() != null)
                        return false;

                    tb.processStartTag("form");
                    if (startTag.hasAttribute("action")) {
                        Element form = tb.getFormElement();
                        if (form != null && startTag.hasAttribute("action")) {
                            String action = startTag.attributes.get("action");
                            form.attributes().put("action", action); // always LC, so don't need to scan up for ownerdoc
                        }
                    }
                    tb.processStartTag("hr");
                    tb.processStartTag("label");
                    // hope you like english.
                    String prompt = startTag.hasAttribute("prompt") ?
                        startTag.attributes.get("prompt") :
                        "This is a searchable index. Enter search keywords: ";

                    tb.process(new Token.Character().data(prompt));

                    // input
                    Attributes inputAttribs = new Attributes();
                    if (startTag.hasAttributes()) {
                        for (Attribute attr : startTag.attributes) {
                            if (!inSorted(attr.getKey(), Constants.InBodyStartInputAttribs))
                                inputAttribs.put(attr);
                        }
                    }
                    inputAttribs.put("name", "isindex");
                    tb.processStartTag("input", inputAttribs);
                    tb.processEndTag("label");
                    tb.processStartTag("hr");
                    tb.processEndTag("form");
                    break;
                case "textarea":
                    tb.insert(startTag);
                    if (!startTag.isSelfClosing()) {
                        tb.tokeniser.transition(TokeniserState.Rcdata);
                        tb.markInsertionMode();
                        tb.framesetOk(false);
                        tb.transition(Text);
                    }
                    break;
                case "xmp":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.reconstructFormattingElements();
                    tb.framesetOk(false);
                    handleRawtext(startTag, tb);
                    break;
                case "iframe":
                    tb.framesetOk(false);
                    handleRawtext(startTag, tb);
                    break;
                case "noembed":
                    // also handle noscript if script enabled
                    handleRawtext(startTag, tb);
                    break;
                case "select":
                    tb.reconstructFormattingElements();
                    tb.insert(startTag);
                    tb.framesetOk(false);
                    if (startTag.selfClosing) break; // don't change states if not added to the stack

                    HtmlTreeBuilderState state = tb.state();
                    if (state.equals(InTable) || state.equals(InCaption) || state.equals(InTableBody) || state.equals(InRow) || state.equals(InCell))
                        tb.transition(InSelectInTable);
                    else
                        tb.transition(InSelect);
                    break;
                case "math":
                    tb.reconstructFormattingElements();
                    // todo: handle A start tag whose tag name is "math" (i.e. foreign, mathml)
                    tb.insert(startTag);
                    break;
                case "svg":
                    tb.reconstructFormattingElements();
                    // todo: handle A start tag whose tag name is "svg" (xlink, svg)
                    tb.insert(startTag);
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
                    if (inSorted(tb.currentElement().normalName(), Constants.Headings)) {
                        tb.error(this);
                        tb.pop();
                    }
                    tb.insert(startTag);
                    break;
                // static final String[] InBodyStartPreListing = new String[]{"listing", "pre"};
                case "pre":
                case "listing":
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insert(startTag);
                    tb.reader.matchConsume("\n"); // ignore LF if next token
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
                        if (tb.isSpecial(el) && !inSorted(el.normalName(), Constants.InBodyStartLiBreakers))
                            break;
                    }
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p");
                    }
                    tb.insert(startTag);
                    break;
                // static final String[] InBodyStartOptions = new String[]{"optgroup", "option"};
                case "optgroup":
                case "option":
                    if (tb.currentElementIs("option"))
                        tb.processEndTag("option");
                    tb.reconstructFormattingElements();
                    tb.insert(startTag);
                    break;
                // static final String[] InBodyStartRuby = new String[]{"rp", "rt"};
                case "rp":
                case "rt":
                    if (tb.inScope("ruby")) {
                        tb.generateImpliedEndTags();
                        if (!tb.currentElementIs("ruby")) {
                            tb.error(this);
                            tb.popStackToBefore("ruby"); // i.e. close up to but not include name
                        }
                        tb.insert(startTag);
                    }
                    // todo - is this right? drops rp, rt if ruby not in scope?
                    break;
                // InBodyStartEmptyFormatters:
                case "area":
                case "br":
                case "embed":
                case "img":
                case "keygen":
                case "wbr":
                    tb.reconstructFormattingElements();
                    tb.insertEmpty(startTag);
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
                    el = tb.insert(startTag);
                    tb.pushActiveFormattingElements(el);
                    break;
                default:
                    // todo - bring scan groups in if desired
                    if (!Tag.isKnownTag(name)) { // no special rules for custom tags
                        tb.insert(startTag);
                    } else if (inSorted(name, Constants.InBodyStartPClosers)) {
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insert(startTag);
                    } else if (inSorted(name, Constants.InBodyStartToHead)) {
                        return tb.process(t, InHead);
                    } else if (inSorted(name, Constants.InBodyStartApplets)) {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        tb.insertMarkerToFormattingElements();
                        tb.framesetOk(false);
                    } else if (inSorted(name, Constants.InBodyStartMedia)) {
                        tb.insertEmpty(startTag);
                    } else if (inSorted(name, Constants.InBodyStartDrop)) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
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
                        // todo: error if stack contains something not dd, dt, li, optgroup, option, p, rp, rt, tbody, td, tfoot, th, thead, tr, body, html
                        anyOtherEndTag(t, tb);
                        tb.transition(AfterBody);
                    }
                    break;
                case "html":
                    boolean notIgnored = tb.processEndTag("body");
                    if (notIgnored)
                        return tb.process(endTag);
                    break;
                case "form":
                    if (!tb.onStack("template")) {
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
                    if (!tb.inScope(Constants.Headings)) {
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
                if (node.normalName().equals(name)) {
                    tb.generateImpliedEndTags(name);
                    if (!tb.currentElementIs(name))
                        tb.error(this);
                    tb.popStackToClose(name);
                    break;
                } else {
                    if (tb.isSpecial(node)) {
                        tb.error(this);
                        return false;
                    }
                }
            }
            return true;
        }

        // Adoption Agency Algorithm.
        private boolean inBodyEndTagAdoption(Token t, HtmlTreeBuilder tb) {
            final Token.EndTag endTag = t.asEndTag();
            final String name = endTag.normalName();

            final ArrayList<Element> stack = tb.getStack();
            Element el;
            for (int i = 0; i < 8; i++) {
                Element formatEl = tb.getActiveFormattingElement(name);
                if (formatEl == null)
                    return anyOtherEndTag(t, tb);
                else if (!tb.onStack(formatEl)) {
                    tb.error(this);
                    tb.removeFromActiveFormattingElements(formatEl);
                    return true;
                } else if (!tb.inScope(formatEl.normalName())) {
                    tb.error(this);
                    return false;
                } else if (tb.currentElement() != formatEl)
                    tb.error(this);

                Element furthestBlock = null;
                Element commonAncestor = null;
                boolean seenFormattingElement = false;
                // the spec doesn't limit to < 64, but in degenerate cases (9000+ stack depth) this prevents run-aways
                final int stackSize = stack.size();
                int bookmark = -1;
                for (int si = 1; si < stackSize && si < 64; si++) {
                    // TODO: this no longer matches the current spec at https://html.spec.whatwg.org/#adoption-agency-algorithm and should be updated
                    el = stack.get(si);
                    if (el == formatEl) {
                        commonAncestor = stack.get(si - 1);
                        seenFormattingElement = true;
                        // Let a bookmark note the position of the formatting element in the list of active formatting elements relative to the elements on either side of it in the list.
                        bookmark = tb.positionOfElement(el);
                    } else if (seenFormattingElement && tb.isSpecial(el)) {
                        furthestBlock = el;
                        break;
                    }
                }
                if (furthestBlock == null) {
                    tb.popStackToClose(formatEl.normalName());
                    tb.removeFromActiveFormattingElements(formatEl);
                    return true;
                }

                Element node = furthestBlock;
                Element lastNode = furthestBlock;
                for (int j = 0; j < 3; j++) {
                    if (tb.onStack(node))
                        node = tb.aboveOnStack(node);
                    if (!tb.isInActiveFormattingElements(node)) { // note no bookmark check
                        tb.removeFromStack(node);
                        continue;
                    } else if (node == formatEl)
                        break;

                    Element replacement = new Element(tb.tagFor(node.nodeName(), ParseSettings.preserveCase), tb.getBaseUri());
                    // case will follow the original node (so honours ParseSettings)
                    tb.replaceActiveFormattingElement(node, replacement);
                    tb.replaceOnStack(node, replacement);
                    node = replacement;

                    if (lastNode == furthestBlock) {
                        // move the aforementioned bookmark to be immediately after the new node in the list of active formatting elements.
                        // not getting how this bookmark both straddles the element above, but is inbetween here...
                        bookmark = tb.positionOfElement(node) + 1;
                    }
                    if (lastNode.parent() != null)
                        lastNode.remove();
                    node.appendChild(lastNode);

                    lastNode = node;
                }

                if (commonAncestor != null) { // safety check, but would be an error if null
                    if (inSorted(commonAncestor.normalName(), Constants.InBodyEndTableFosters)) {
                        if (lastNode.parent() != null)
                            lastNode.remove();
                        tb.insertInFosterParent(lastNode);
                    } else {
                        if (lastNode.parent() != null)
                            lastNode.remove();
                        commonAncestor.appendChild(lastNode);
                    }
                }

                Element adopter = new Element(formatEl.tag(), tb.getBaseUri());
                adopter.attributes().addAll(formatEl.attributes());
                adopter.appendChildren(furthestBlock.childNodes());
                furthestBlock.appendChild(adopter);
                tb.removeFromActiveFormattingElements(formatEl);
                // insert the new element into the list of active formatting elements at the position of the aforementioned bookmark.
                tb.pushWithBookmark(adopter, bookmark);
                tb.removeFromStack(formatEl);
                tb.insertOnStackAfter(furthestBlock, adopter);
            }
            return true;
        }
    },
    Text {
        // in script, style etc. normally treated as data tags
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isCharacter()) {
                tb.insert(t.asCharacter());
            } else if (t.isEOF()) {
                tb.error(this);
                // if current node is script: already started
                tb.pop();
                tb.transition(tb.originalState());
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isCharacter() && inSorted(tb.currentElement().normalName(), InTableFoster)) {
                tb.newPendingTableCharacters();
                tb.markInsertionMode();
                tb.transition(InTableText);
                return tb.process(t);
            } else if (t.isComment()) {
                tb.insert(t.asComment());
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
                    tb.insert(startTag);
                    tb.transition(InCaption);
                } else if (name.equals("colgroup")) {
                    tb.clearStackToTableContext();
                    tb.insert(startTag);
                    tb.transition(InColumnGroup);
                } else if (name.equals("col")) {
                    tb.clearStackToTableContext();
                    tb.processStartTag("colgroup");
                    return tb.process(t);
                } else if (inSorted(name, InTableToBody)) {
                    tb.clearStackToTableContext();
                    tb.insert(startTag);
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
                            tb.insert(startTag);
                            return true;
                        }
                        return tb.process(t);
                    }
                } else if (inSorted(name, InTableToHead)) {
                    return tb.process(t, InHead);
                } else if (name.equals("input")) {
                    if (!(startTag.hasAttributes() && startTag.attributes.get("type").equalsIgnoreCase("hidden"))) {
                        return anythingElse(t, tb);
                    } else {
                        tb.insertEmpty(startTag);
                    }
                } else if (name.equals("form")) {
                    tb.error(this);
                    if (tb.getFormElement() != null || tb.onStack("template"))
                        return false;
                    else {
                        tb.insertForm(startTag, false, false); // not added to stack. can associate to template
                    }
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
                if (tb.currentElementIs("html"))
                    tb.error(this);
                return true; // stops parsing
            }
            return anythingElse(t, tb);
        }

        boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.error(this);
            tb.setFosterInserts(true);
            tb.process(t, InBody);
            tb.setFosterInserts(false);
            return true;
        }
    },
    InTableText {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.type == Token.TokenType.Character) {
                Token.Character c = t.asCharacter();
                if (c.getData().equals(nullString)) {
                    tb.error(this);
                    return false;
                } else {
                    tb.getPendingTableCharacters().add(c.getData());
                }
            } else {// todo - don't really like the way these table character data lists are built
                if (tb.getPendingTableCharacters().size() > 0) {
                    for (String character : tb.getPendingTableCharacters()) {
                        if (!isWhitespace(character)) {
                            // InTable anything else section:
                            tb.error(this);
                            if (inSorted(tb.currentElement().normalName(), InTableFoster)) {
                                tb.setFosterInserts(true);
                                tb.process(new Token.Character().data(character), InBody);
                                tb.setFosterInserts(false);
                            } else {
                                tb.process(new Token.Character().data(character), InBody);
                            }
                        } else
                            tb.insert(new Token.Character().data(character));
                    }
                    tb.newPendingTableCharacters();
                }
                tb.transition(tb.originalState());
                return tb.process(t);
            }
            return true;
        }
    },
    InCaption {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isEndTag() && t.asEndTag().normalName().equals("caption")) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.normalName();
                if (!tb.inTableScope(name)) {
                    tb.error(this);
                    return false;
                } else {
                    tb.generateImpliedEndTags();
                    if (!tb.currentElementIs("caption"))
                        tb.error(this);
                    tb.popStackToClose("caption");
                    tb.clearFormattingElementsToLastMarker();
                    tb.transition(InTable);
                }
            } else if ((
                    t.isStartTag() && inSorted(t.asStartTag().normalName(), InCellCol) ||
                            t.isEndTag() && t.asEndTag().normalName().equals("table"))
                    ) {
                tb.error(this);
                boolean processed = tb.processEndTag("caption");
                if (processed)
                    return tb.process(t);
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter());
                return true;
            }
            switch (t.type) {
                case Comment:
                    tb.insert(t.asComment());
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
                            tb.insertEmpty(startTag);
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
                        case "template":
                            tb.process(t, InHead);
                            break;
                        default:
                            return anythingElse(t, tb);
                    }
                    break;
                case EOF:
                    if (tb.currentElementIs("html"))
                        return true; // stop parsing; frag case
                    else
                        return anythingElse(t, tb);
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case StartTag:
                    Token.StartTag startTag = t.asStartTag();
                    String name = startTag.normalName();
                    if (name.equals("tr")) {
                        tb.clearStackToTableBodyContext();
                        tb.insert(startTag);
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
            if (!(tb.inTableScope("tbody") || tb.inTableScope("thead") || tb.inScope("tfoot"))) {
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.normalName();

                if (inSorted(name, InCellNames)) {
                    tb.clearStackToTableRowContext();
                    tb.insert(startTag);
                    tb.transition(InCell);
                    tb.insertMarkerToFormattingElements();
                } else if (inSorted(name, InRowMissing)) {
                    return handleMissingTr(t, tb);
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
                    return handleMissingTr(t, tb);
                } else if (inSorted(name, InTableToBody)) {
                    if (!tb.inTableScope(name) || !tb.inTableScope("tr")) {
                        tb.error(this);
                        return false;
                    }
                    tb.clearStackToTableRowContext();
                    tb.pop(); // tr
                    tb.transition(InTableBody);
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

        private boolean handleMissingTr(Token t, TreeBuilder tb) {
            boolean processed = tb.processEndTag("tr");
            if (processed)
                return tb.process(t);
            else
                return false;
        }
    },
    InCell {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.normalName();

                if (inSorted(name, Constants.InCellNames)) {
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
    InSelect {
        boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case Character:
                    Token.Character c = t.asCharacter();
                    if (c.getData().equals(nullString)) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.insert(c);
                    }
                    break;
                case Comment:
                    tb.insert(t.asComment());
                    break;
                case Doctype:
                    tb.error(this);
                    return false;
                case StartTag:
                    Token.StartTag start = t.asStartTag();
                    String name = start.normalName();
                    if (name.equals("html"))
                        return tb.process(start, InBody);
                    else if (name.equals("option")) {
                        if (tb.currentElementIs("option"))
                            tb.processEndTag("option");
                        tb.insert(start);
                    } else if (name.equals("optgroup")) {
                        if (tb.currentElementIs("option"))
                            tb.processEndTag("option"); // pop option and flow to pop optgroup
                        if (tb.currentElementIs("optgroup"))
                            tb.processEndTag("optgroup");
                        tb.insert(start);
                    } else if (name.equals("select")) {
                        tb.error(this);
                        return tb.processEndTag("select");
                    } else if (inSorted(name, InSelectEnd)) {
                        tb.error(this);
                        if (!tb.inSelectScope("select"))
                            return false; // frag
                        tb.processEndTag("select");
                        return tb.process(start);
                    } else if (name.equals("script") || name.equals("template")) {
                        return tb.process(t, InHead);
                    } else {
                        return anythingElse(t, tb);
                    }
                    break;
                case EndTag:
                    Token.EndTag end = t.asEndTag();
                    name = end.normalName();
                    switch (name) {
                        case "optgroup":
                            if (tb.currentElementIs("option") && tb.aboveOnStack(tb.currentElement()) != null && tb.aboveOnStack(tb.currentElement()).normalName().equals("optgroup"))
                                tb.processEndTag("option");
                            if (tb.currentElementIs("optgroup"))
                                tb.pop();
                            else
                                tb.error(this);
                            break;
                        case "option":
                            if (tb.currentElementIs("option"))
                                tb.pop();
                            else
                                tb.error(this);
                            break;
                        case "select":
                            if (!tb.inSelectScope(name)) {
                                tb.error(this);
                                return false;
                            } else {
                                tb.popStackToClose(name);
                                tb.resetInsertionMode();
                            }
                            break;
                        case "template":
                            return tb.process(t, InHead);
                        default:
                            return anythingElse(t, tb);
                    }
                    break;
                case EOF:
                    if (!tb.currentElementIs("html"))
                        tb.error(this);
                    break;
                default:
                    return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.error(this);
            return false;
        }
    },
    InSelectInTable {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isStartTag() && inSorted(t.asStartTag().normalName(), InSelectTableEnd)) {
                tb.error(this);
                tb.popStackToClose("select");
                tb.resetInsertionMode();
                return tb.process(t);
            } else if (t.isEndTag() && inSorted(t.asEndTag().normalName(), InSelectTableEnd)) {
                tb.error(this);
                if (tb.inTableScope(t.asEndTag().normalName())) {
                    tb.popStackToClose("select");
                    tb.resetInsertionMode();
                    return (tb.process(t));
                } else
                    return false;
            } else {
                return tb.process(t, InSelect);
            }
        }
    },
    InTemplate {
        boolean process(Token t, HtmlTreeBuilder tb) {
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
                    if (!tb.onStack("template")) {// stop parsing
                        return true;
                    }
                    tb.error(this);
                    tb.popStackToClose("template");
                    tb.clearFormattingElementsToLastMarker();
                    tb.popTemplateMode();
                    tb.resetInsertionMode();
                    // spec deviation - if we did not break out of Template, stop processing, and don't worry about cleaning up ultra-deep template stacks
                    // limited depth because this can recurse and will blow stack if too deep
                    if (tb.state() != InTemplate && tb.templateModeSize() < 12)
                        return tb.process(t);
                    else return true;
            }
            return true;
        }
    },
    AfterBody {
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter()); // out of spec - include whitespace. spec would move into body
            } else if (t.isComment()) {
                tb.insert(t.asComment()); // into html node
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
                    if (tb.onStack("html")) tb.popStackToClose("html");
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter());
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag()) {
                Token.StartTag start = t.asStartTag();
                switch (start.normalName()) {
                    case "html":
                        return tb.process(start, InBody);
                    case "frameset":
                        tb.insert(start);
                        break;
                    case "frame":
                        tb.insertEmpty(start);
                        break;
                    case "noframes":
                        return tb.process(start, InHead);
                    default:
                        tb.error(this);
                        return false;
                }
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("frameset")) {
                if (tb.currentElementIs("html")) { // frag
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter());
            } else if (t.isComment()) {
                tb.insert(t.asComment());
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype() || (t.isStartTag() && t.asStartTag().normalName().equals("html"))) {
                return tb.process(t, InBody);
            } else if (isWhitespace(t)) {
                tb.insert(t.asCharacter());
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isComment()) {
                tb.insert(t.asComment());
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
        boolean process(Token t, HtmlTreeBuilder tb) {
            return true;
            // todo: implement. Also; how do we get here?
        }
    };

    private static final String nullString = String.valueOf('\u0000');

    abstract boolean process(Token t, HtmlTreeBuilder tb);

    private static boolean isWhitespace(Token t) {
        if (t.isCharacter()) {
            String data = t.asCharacter().getData();
            return StringUtil.isBlank(data);
        }
        return false;
    }

    private static boolean isWhitespace(String data) {
        return StringUtil.isBlank(data);
    }

    private static void handleRcData(Token.StartTag startTag, HtmlTreeBuilder tb) {
        tb.tokeniser.transition(TokeniserState.Rcdata);
        tb.markInsertionMode();
        tb.transition(Text);
        tb.insert(startTag);
    }

    private static void handleRawtext(Token.StartTag startTag, HtmlTreeBuilder tb) {
        tb.tokeniser.transition(TokeniserState.Rawtext);
        tb.markInsertionMode();
        tb.transition(Text);
        tb.insert(startTag);
    }

    // lists of tags to search through
    static final class Constants {
        static final String[] InHeadEmpty = new String[]{"base", "basefont", "bgsound", "command", "link"};
        static final String[] InHeadRaw = new String[]{"noframes", "style"};
        static final String[] InHeadEnd = new String[]{"body", "br", "html"};
        static final String[] AfterHeadBody = new String[]{"body", "br", "html"};
        static final String[] BeforeHtmlToHead = new String[]{"body", "br", "head", "html", };
        static final String[] InHeadNoScriptHead = new String[]{"basefont", "bgsound", "link", "meta", "noframes", "style"};
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
            "nav", "ol", "pre", "section", "summary", "ul"};
        static final String[] InBodyEndAdoptionFormatters = new String[]{"a", "b", "big", "code", "em", "font", "i", "nobr", "s", "small", "strike", "strong", "tt", "u"};
        static final String[] InBodyEndTableFosters = new String[]{"table", "tbody", "tfoot", "thead", "tr"};
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
        static final String[] InSelectEnd = new String[]{"input", "keygen", "textarea"};
        static final String[] InSelectTableEnd = new String[]{"caption", "table", "tbody", "td", "tfoot", "th", "thead", "tr"};
        static final String[] InTableEndIgnore = new String[]{"tbody", "tfoot", "thead"};
        static final String[] InHeadNoscriptIgnore = new String[]{"head", "noscript"};
        static final String[] InCaptionIgnore = new String[]{"body", "col", "colgroup", "html", "tbody", "td", "tfoot", "th", "thead", "tr"};
        static final String[] InTemplateToHead = new String[] {"base", "basefont", "bgsound", "link", "meta", "noframes", "script", "style", "template", "title"};
        static final String[] InTemplateToTable = new String[] {"caption", "colgroup", "tbody", "tfoot", "thead"};
    }
}
