package org.jsoup.parser;

import org.jsoup.helper.DescendableLinkedList;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.*;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * The Tree Builder's current state. Each state embodies the processing for the state, and transitions to other states.
 */
enum TreeBuilderState {
    Initial {
        boolean process(Token t, TreeBuilder tb) {
            if (isWhitespace(t)) {
                return true; // ignore whitespace
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                // todo: parse error check on expected doctypes
                // todo: quirk state check on doctype ids
                Token.Doctype d = t.asDoctype();
                DocumentType doctype = new DocumentType(d.getName(), d.getPublicIdentifier(), d.getSystemIdentifier(), tb.getBaseUri());
                tb.getDocument().appendChild(doctype);
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
        boolean process(Token t, TreeBuilder tb) {
            if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (isWhitespace(t)) {
                return true; // ignore whitespace
            } else if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                tb.insert(t.asStartTag());
                tb.transition(BeforeHead);
            } else if (t.isEndTag() && (StringUtil.in(t.asEndTag().name(), "head", "body", "html", "br"))) {
                return anythingElse(t, tb);
            } else if (t.isEndTag()) {
                tb.error(this);
                return false;
            } else {
                return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            tb.insert("html");
            tb.transition(BeforeHead);
            return tb.process(t);
        }
    },
    BeforeHead {
        boolean process(Token t, TreeBuilder tb) {
            if (isWhitespace(t)) {
                return true;
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                return InBody.process(t, tb); // does not transition
            } else if (t.isStartTag() && t.asStartTag().name().equals("head")) {
                Element head = tb.insert(t.asStartTag());
                tb.setHeadElement(head);
                tb.transition(InHead);
            } else if (t.isEndTag() && (StringUtil.in(t.asEndTag().name(), "head", "body", "html", "br"))) {
                tb.process(new Token.StartTag("head"));
                return tb.process(t);
            } else if (t.isEndTag()) {
                tb.error(this);
                return false;
            } else {
                tb.process(new Token.StartTag("head"));
                return tb.process(t);
            }
            return true;
        }
    },
    InHead {
        boolean process(Token t, TreeBuilder tb) {
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
                    return false;
                case StartTag:
                    Token.StartTag start = t.asStartTag();
                    String name = start.name();
                    if (name.equals("html")) {
                        return InBody.process(t, tb);
                    } else if (StringUtil.in(name, "base", "basefont", "bgsound", "command", "link")) {
                        Element el = tb.insertEmpty(start);
                        // jsoup special: update base as it is seen. todo: flip to current browser behaviour of one shot
                        if (name.equals("base") && el.hasAttr("href"))
                            tb.setBaseUri(el);
                    } else if (name.equals("meta")) {
                        Element meta = tb.insertEmpty(start);
                        // todo: charset switches
                    } else if (name.equals("title")) {
                        handleRcData(start, tb);
                    } else if (StringUtil.in(name, "noframes", "style")) {
                        handleRawtext(start, tb);
                    } else if (name.equals("noscript")) {
                        // else if noscript && scripting flag = true: rawtext (jsoup doesn't run script, to handle as noscript)
                        tb.insert(start);
                        tb.transition(InHeadNoscript);
                    } else if (name.equals("script")) {
                        // skips some script rules as won't execute them
                        tb.insert(start);
                        tb.tokeniser.transition(TokeniserState.ScriptData);
                        tb.markInsertionMode();
                        tb.transition(Text);
                    } else if (name.equals("head")) {
                        tb.error(this);
                        return false;
                    } else {
                        return anythingElse(t, tb);
                    }
                    break;
                case EndTag:
                    Token.EndTag end = t.asEndTag();
                    name = end.name();
                    if (name.equals("head")) {
                        tb.pop();
                        tb.transition(AfterHead);
                    } else if (StringUtil.in(name, "body", "html", "br")) {
                        return anythingElse(t, tb);
                    } else {
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
            tb.process(new Token.EndTag("head"));
            return tb.process(t);
        }
    },
    InHeadNoscript {
        boolean process(Token t, TreeBuilder tb) {
            if (t.isDoctype()) {
                tb.error(this);
            } else if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                return tb.process(t, InBody);
            } else if (t.isEndTag() && t.asEndTag().name().equals("noscript")) {
                tb.pop();
                tb.transition(InHead);
            } else if (isWhitespace(t) || t.isComment() || (t.isStartTag() && StringUtil.in(t.asStartTag().name(),
                    "basefont", "bgsound", "link", "meta", "noframes", "style"))) {
                return tb.process(t, InHead);
            } else if (t.isEndTag() && t.asEndTag().name().equals("br")) {
                return anythingElse(t, tb);
            } else if ((t.isStartTag() && StringUtil.in(t.asStartTag().name(), "head", "noscript")) || t.isEndTag()) {
                tb.error(this);
                return false;
            } else {
                return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            tb.error(this);
            tb.process(new Token.EndTag("noscript"));
            return tb.process(t);
        }
    },
    AfterHead {
        boolean process(Token t, TreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter());
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
            } else if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.name();
                if (name.equals("html")) {
                    return tb.process(t, InBody);
                } else if (name.equals("body")) {
                    tb.insert(startTag);
                    tb.framesetOk(false);
                    tb.transition(InBody);
                } else if (name.equals("frameset")) {
                    tb.insert(startTag);
                    tb.transition(InFrameset);
                } else if (StringUtil.in(name, "base", "basefont", "bgsound", "link", "meta", "noframes", "script", "style", "title")) {
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
                if (StringUtil.in(t.asEndTag().name(), "body", "html")) {
                    anythingElse(t, tb);
                } else {
                    tb.error(this);
                    return false;
                }
            } else {
                anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            tb.process(new Token.StartTag("body"));
            tb.framesetOk(true);
            return tb.process(t);
        }
    },
    InBody {
        boolean process(Token t, TreeBuilder tb) {
            switch (t.type) {
                case Character: {
                    Token.Character c = t.asCharacter();
                    if (c.getData().equals(nullString)) {
                        // todo confirm that check
                        tb.error(this);
                        return false;
                    } else if (isWhitespace(c)) {
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
                    Token.StartTag startTag = t.asStartTag();
                    String name = startTag.name();
                    if (name.equals("html")) {
                        tb.error(this);
                        // merge attributes onto real html
                        Element html = tb.getStack().getFirst();
                        for (Attribute attribute : startTag.getAttributes()) {
                            if (!html.hasAttr(attribute.getKey()))
                                html.attributes().put(attribute);
                        }
                    } else if (StringUtil.in(name, "base", "basefont", "bgsound", "command", "link", "meta", "noframes", "script", "style", "title")) {
                        return tb.process(t, InHead);
                    } else if (name.equals("body")) {
                        tb.error(this);
                        LinkedList<Element> stack = tb.getStack();
                        if (stack.size() == 1 || (stack.size() > 2 && !stack.get(1).nodeName().equals("body"))) {
                            // only in fragment case
                            return false; // ignore
                        } else {
                            tb.framesetOk(false);
                            Element body = stack.get(1);
                            for (Attribute attribute : startTag.getAttributes()) {
                                if (!body.hasAttr(attribute.getKey()))
                                    body.attributes().put(attribute);
                            }
                        }
                    } else if (name.equals("frameset")) {
                        tb.error(this);
                        LinkedList<Element> stack = tb.getStack();
                        if (stack.size() == 1 || (stack.size() > 2 && !stack.get(1).nodeName().equals("body"))) {
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
                                stack.removeLast();
                            tb.insert(startTag);
                            tb.transition(InFrameset);
                        }
                    } else if (StringUtil.in(name,
                            "address", "article", "aside", "blockquote", "center", "details", "dir", "div", "dl",
                            "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "menu", "nav", "ol",
                            "p", "section", "summary", "ul")) {
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        tb.insert(startTag);
                    } else if (StringUtil.in(name, "h1", "h2", "h3", "h4", "h5", "h6")) {
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        if (StringUtil.in(tb.currentElement().nodeName(), "h1", "h2", "h3", "h4", "h5", "h6")) {
                            tb.error(this);
                            tb.pop();
                        }
                        tb.insert(startTag);
                    } else if (StringUtil.in(name, "pre", "listing")) {
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        tb.insert(startTag);
                        // todo: ignore LF if next token
                        tb.framesetOk(false);
                    } else if (name.equals("form")) {
                        if (tb.getFormElement() != null) {
                            tb.error(this);
                            return false;
                        }
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        Element form = tb.insert(startTag);
                        tb.setFormElement(form);
                    } else if (name.equals("li")) {
                        tb.framesetOk(false);
                        LinkedList<Element> stack = tb.getStack();
                        for (int i = stack.size() - 1; i > 0; i--) {
                            Element el = stack.get(i);
                            if (el.nodeName().equals("li")) {
                                tb.process(new Token.EndTag("li"));
                                break;
                            }
                            if (tb.isSpecial(el) && !StringUtil.in(el.nodeName(), "address", "div", "p"))
                                break;
                        }
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        tb.insert(startTag);
                    } else if (StringUtil.in(name, "dd", "dt")) {
                        tb.framesetOk(false);
                        LinkedList<Element> stack = tb.getStack();
                        for (int i = stack.size() - 1; i > 0; i--) {
                            Element el = stack.get(i);
                            if (StringUtil.in(el.nodeName(), "dd", "dt")) {
                                tb.process(new Token.EndTag(el.nodeName()));
                                break;
                            }
                            if (tb.isSpecial(el) && !StringUtil.in(el.nodeName(), "address", "div", "p"))
                                break;
                        }
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        tb.insert(startTag);
                    } else if (name.equals("plaintext")) {
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        tb.insert(startTag);
                        tb.tokeniser.transition(TokeniserState.PLAINTEXT); // once in, never gets out
                    } else if (name.equals("button")) {
                        if (tb.inButtonScope("button")) {
                            // close and reprocess
                            tb.error(this);
                            tb.process(new Token.EndTag("button"));
                            tb.process(startTag);
                        } else {
                            tb.reconstructFormattingElements();
                            tb.insert(startTag);
                            tb.framesetOk(false);
                        }
                    } else if (name.equals("a")) {
                        if (tb.getActiveFormattingElement("a") != null) {
                            tb.error(this);
                            tb.process(new Token.EndTag("a"));

                            // still on stack?
                            Element remainingA = tb.getFromStack("a");
                            if (remainingA != null) {
                                tb.removeFromActiveFormattingElements(remainingA);
                                tb.removeFromStack(remainingA);
                            }
                        }
                        tb.reconstructFormattingElements();
                        Element a = tb.insert(startTag);
                        tb.pushActiveFormattingElements(a);
                    } else if (StringUtil.in(name,
                            "b", "big", "code", "em", "font", "i", "s", "small", "strike", "strong", "tt", "u")) {
                        tb.reconstructFormattingElements();
                        Element el = tb.insert(startTag);
                        tb.pushActiveFormattingElements(el);
                    } else if (name.equals("nobr")) {
                        tb.reconstructFormattingElements();
                        if (tb.inScope("nobr")) {
                            tb.error(this);
                            tb.process(new Token.EndTag("nobr"));
                            tb.reconstructFormattingElements();
                        }
                        Element el = tb.insert(startTag);
                        tb.pushActiveFormattingElements(el);
                    } else if (StringUtil.in(name, "applet", "marquee", "object")) {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        tb.insertMarkerToFormattingElements();
                        tb.framesetOk(false);
                    } else if (name.equals("table")) {
                        if (tb.getDocument().quirksMode() != Document.QuirksMode.quirks && tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        tb.insert(startTag);
                        tb.framesetOk(false);
                        tb.transition(InTable);
                    } else if (StringUtil.in(name, "area", "br", "embed", "img", "keygen", "wbr")) {
                        tb.reconstructFormattingElements();
                        tb.insertEmpty(startTag);
                        tb.framesetOk(false);
                    } else if (name.equals("input")) {
                        tb.reconstructFormattingElements();
                        Element el = tb.insertEmpty(startTag);
                        if (!el.attr("type").equalsIgnoreCase("hidden"))
                            tb.framesetOk(false);
                    } else if (StringUtil.in(name, "param", "source", "track")) {
                        tb.insertEmpty(startTag);
                    } else if (name.equals("hr")) {
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        tb.insertEmpty(startTag);
                        tb.framesetOk(false);
                    } else if (name.equals("image")) {
                        // we're not supposed to ask.
                        startTag.name("img");
                        return tb.process(startTag);
                    } else if (name.equals("isindex")) {
                        // how much do we care about the early 90s?
                        tb.error(this);
                        if (tb.getFormElement() != null)
                            return false;

                        tb.tokeniser.acknowledgeSelfClosingFlag();
                        tb.process(new Token.StartTag("form"));
                        if (startTag.attributes.hasKey("action")) {
                            Element form = tb.getFormElement();
                            form.attr("action", startTag.attributes.get("action"));
                        }
                        tb.process(new Token.StartTag("hr"));
                        tb.process(new Token.StartTag("label"));
                        // hope you like english.
                        String prompt = startTag.attributes.hasKey("prompt") ?
                                startTag.attributes.get("prompt") :
                                "This is a searchable index. Enter search keywords: ";

                        tb.process(new Token.Character(prompt));

                        // input
                        Attributes inputAttribs = new Attributes();
                        for (Attribute attr : startTag.attributes) {
                            if (!StringUtil.in(attr.getKey(), "name", "action", "prompt"))
                                inputAttribs.put(attr);
                        }
                        inputAttribs.put("name", "isindex");
                        tb.process(new Token.StartTag("input", inputAttribs));
                        tb.process(new Token.EndTag("label"));
                        tb.process(new Token.StartTag("hr"));
                        tb.process(new Token.EndTag("form"));
                    } else if (name.equals("textarea")) {
                        tb.insert(startTag);
                        // todo: If the next token is a U+000A LINE FEED (LF) character token, then ignore that token and move on to the next one. (Newlines at the start of textarea elements are ignored as an authoring convenience.)
                        tb.tokeniser.transition(TokeniserState.Rcdata);
                        tb.markInsertionMode();
                        tb.framesetOk(false);
                        tb.transition(Text);
                    } else if (name.equals("xmp")) {
                        if (tb.inButtonScope("p")) {
                            tb.process(new Token.EndTag("p"));
                        }
                        tb.reconstructFormattingElements();
                        tb.framesetOk(false);
                        handleRawtext(startTag, tb);
                    } else if (name.equals("iframe")) {
                        tb.framesetOk(false);
                        handleRawtext(startTag, tb);
                    } else if (name.equals("noembed")) {
                        // also handle noscript if script enabled
                        handleRawtext(startTag, tb);
                    } else if (name.equals("select")) {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        tb.framesetOk(false);

                        TreeBuilderState state = tb.state();
                        if (state.equals(InTable) || state.equals(InCaption) || state.equals(InTableBody) || state.equals(InRow) || state.equals(InCell))
                            tb.transition(InSelectInTable);
                        else
                            tb.transition(InSelect);
                    } else if (StringUtil.in("optgroup", "option")) {
                        if (tb.currentElement().nodeName().equals("option"))
                            tb.process(new Token.EndTag("option"));
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                    } else if (StringUtil.in("rp", "rt")) {
                        if (tb.inScope("ruby")) {
                            tb.generateImpliedEndTags();
                            if (!tb.currentElement().nodeName().equals("ruby")) {
                                tb.error(this);
                                tb.popStackToBefore("ruby"); // i.e. close up to but not include name
                            }
                            tb.insert(startTag);
                        }
                    } else if (name.equals("math")) {
                        tb.reconstructFormattingElements();
                        // todo: handle A start tag whose tag name is "math" (i.e. foreign, mathml)
                        tb.insert(startTag);
                        tb.tokeniser.acknowledgeSelfClosingFlag();
                    } else if (name.equals("svg")) {
                        tb.reconstructFormattingElements();
                        // todo: handle A start tag whose tag name is "svg" (xlink, svg)
                        tb.insert(startTag);
                        tb.tokeniser.acknowledgeSelfClosingFlag();
                    } else if (StringUtil.in(name,
                            "caption", "col", "colgroup", "frame", "head", "tbody", "td", "tfoot", "th", "thead", "tr")) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                    }
                    break;

                case EndTag:
                    Token.EndTag endTag = t.asEndTag();
                    name = endTag.name();
                    if (name.equals("body")) {
                        if (!tb.inScope("body")) {
                            tb.error(this);
                            return false;
                        } else {
                            // todo: error if stack contains something not dd, dt, li, optgroup, option, p, rp, rt, tbody, td, tfoot, th, thead, tr, body, html
                            tb.transition(AfterBody);
                        }
                    } else if (name.equals("html")) {
                        boolean notIgnored = tb.process(new Token.EndTag("body"));
                        if (notIgnored)
                            return tb.process(endTag);
                    } else if (StringUtil.in(name,
                            "address", "article", "aside", "blockquote", "button", "center", "details", "dir", "div",
                            "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "listing", "menu",
                            "nav", "ol", "pre", "section", "summary", "ul")) {
                        // todo: refactor these lookups
                        if (!tb.inScope(name)) {
                            // nothing to close
                            tb.error(this);
                            return false;
                        } else {
                            tb.generateImpliedEndTags();
                            if (!tb.currentElement().nodeName().equals(name))
                                tb.error(this);
                            tb.popStackToClose(name);
                        }
                    } else if (name.equals("form")) {
                        Element currentForm = tb.getFormElement();
                        tb.setFormElement(null);
                        if (currentForm == null || !tb.inScope(name)) {
                            tb.error(this);
                            return false;
                        } else {
                            tb.generateImpliedEndTags();
                            if (!tb.currentElement().nodeName().equals(name))
                                tb.error(this);
                            // remove currentForm from stack. will shift anything under up.
                            tb.removeFromStack(currentForm);
                        }
                    } else if (name.equals("p")) {
                        if (!tb.inButtonScope(name)) {
                            tb.error(this);
                            tb.process(new Token.StartTag(name)); // if no p to close, creates an empty <p></p>
                            return tb.process(endTag);
                        } else {
                            tb.generateImpliedEndTags(name);
                            if (!tb.currentElement().nodeName().equals(name))
                                tb.error(this);
                            tb.popStackToClose(name);
                        }
                    } else if (name.equals("li")) {
                        if (!tb.inListItemScope(name)) {
                            tb.error(this);
                            return false;
                        } else {
                            tb.generateImpliedEndTags(name);
                            if (!tb.currentElement().nodeName().equals(name))
                                tb.error(this);
                            tb.popStackToClose(name);
                        }
                    } else if (StringUtil.in(name, "dd", "dt")) {
                        if (!tb.inScope(name)) {
                            tb.error(this);
                            return false;
                        } else {
                            tb.generateImpliedEndTags(name);
                            if (!tb.currentElement().nodeName().equals(name))
                                tb.error(this);
                            tb.popStackToClose(name);
                        }
                    } else if (StringUtil.in(name, "h1", "h2", "h3", "h4", "h5", "h6")) {
                        if (!tb.inScope(new String[]{"h1", "h2", "h3", "h4", "h5", "h6"})) {
                            tb.error(this);
                            return false;
                        } else {
                            tb.generateImpliedEndTags(name);
                            if (!tb.currentElement().nodeName().equals(name))
                                tb.error(this);
                            tb.popStackToClose("h1", "h2", "h3", "h4", "h5", "h6");
                        }
                    } else if (name.equals("sarcasm")) {
                        // *sigh*
                        return anyOtherEndTag(t, tb);
                    } else if (StringUtil.in(name,
                            "a", "b", "big", "code", "em", "font", "i", "nobr", "s", "small", "strike", "strong", "tt", "u")) {
                        // Adoption Agency Algorithm.
                        OUTER:
                        for (int i = 0; i < 8; i++) {
                            Element formatEl = tb.getActiveFormattingElement(name);
                            if (formatEl == null)
                                return anyOtherEndTag(t, tb);
                            else if (!tb.onStack(formatEl)) {
                                tb.error(this);
                                tb.removeFromActiveFormattingElements(formatEl);
                                return true;
                            } else if (!tb.inScope(formatEl.nodeName())) {
                                tb.error(this);
                                return false;
                            } else if (tb.currentElement() != formatEl)
                                tb.error(this);

                            Element furthestBlock = null;
                            Element commonAncestor = null;
                            boolean seenFormattingElement = false;
                            LinkedList<Element> stack = tb.getStack();
                            for (int si = 0; si < stack.size(); si++) {
                                Element el = stack.get(si);
                                if (el == formatEl) {
                                    commonAncestor = stack.get(si - 1);
                                    seenFormattingElement = true;
                                } else if (seenFormattingElement && tb.isSpecial(el)) {
                                    furthestBlock = el;
                                    break;
                                }
                            }
                            if (furthestBlock == null) {
                                tb.popStackToClose(formatEl.nodeName());
                                tb.removeFromActiveFormattingElements(formatEl);
                                return true;
                            }

                            // todo: Let a bookmark note the position of the formatting element in the list of active formatting elements relative to the elements on either side of it in the list.
                            // does that mean: int pos of format el in list?
                            Element node = furthestBlock;
                            Element lastNode = furthestBlock;
                            INNER:
                            for (int j = 0; j < 3; j++) {
                                if (tb.onStack(node))
                                    node = tb.aboveOnStack(node);
                                if (!tb.isInActiveFormattingElements(node)) { // note no bookmark check
                                    tb.removeFromStack(node);
                                    continue INNER;
                                } else if (node == formatEl)
                                    break INNER;

                                Element replacement = new Element(Tag.valueOf(node.nodeName()), tb.getBaseUri());
                                tb.replaceActiveFormattingElement(node, replacement);
                                tb.replaceOnStack(node, replacement);
                                node = replacement;

                                if (lastNode == furthestBlock) {
                                    // todo: move the aforementioned bookmark to be immediately after the new node in the list of active formatting elements.
                                    // not getting how this bookmark both straddles the element above, but is inbetween here...
                                }
                                if (lastNode.parent() != null)
                                    lastNode.remove();
                                node.appendChild(lastNode);

                                lastNode = node;
                            }

                            if (StringUtil.in(commonAncestor.nodeName(), "table", "tbody", "tfoot", "thead", "tr")) {
                                if (lastNode.parent() != null)
                                    lastNode.remove();
                                tb.insertInFosterParent(lastNode);
                            } else {
                                if (lastNode.parent() != null)
                                    lastNode.remove();
                                commonAncestor.appendChild(lastNode);
                            }

                            Element adopter = new Element(Tag.valueOf(name), tb.getBaseUri());
                            Node[] childNodes = furthestBlock.childNodes().toArray(new Node[furthestBlock.childNodes().size()]);
                            for (Node childNode : childNodes) {
                                adopter.appendChild(childNode); // append will reparent. thus the clone to avvoid concurrent mod.
                            }
                            furthestBlock.appendChild(adopter);
                            tb.removeFromActiveFormattingElements(formatEl);
                            // todo: insert the new element into the list of active formatting elements at the position of the aforementioned bookmark.
                            tb.removeFromStack(formatEl);
                            tb.insertOnStackAfter(furthestBlock, adopter);
                        }
                    } else if (StringUtil.in(name, "applet", "marquee", "object")) {
                        if (!tb.inScope("name")) {
                            if (!tb.inScope(name)) {
                                tb.error(this);
                                return false;
                            }
                            tb.generateImpliedEndTags();
                            if (!tb.currentElement().nodeName().equals(name))
                                tb.error(this);
                            tb.popStackToClose(name);
                            tb.clearFormattingElementsToLastMarker();
                        }
                    } else if (name.equals("br")) {
                        tb.error(this);
                        tb.process(new Token.StartTag("br"));
                        return false;
                    } else {
                        return anyOtherEndTag(t, tb);
                    }

                    break;
                case EOF:
                    // todo: error if stack contains something not dd, dt, li, p, tbody, td, tfoot, th, thead, tr, body, html
                    // stop parsing
                    break;
            }
            return true;
        }

        boolean anyOtherEndTag(Token t, TreeBuilder tb) {
            String name = t.asEndTag().name();
            DescendableLinkedList<Element> stack = tb.getStack();
            Iterator<Element> it = stack.descendingIterator();
            while (it.hasNext()) {
                Element node = it.next();
                if (node.nodeName().equals(name)) {
                    tb.generateImpliedEndTags(name);
                    if (!name.equals(tb.currentElement().nodeName()))
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
    },
    Text {
        // in script, style etc. normally treated as data tags
        boolean process(Token t, TreeBuilder tb) {
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
        boolean process(Token t, TreeBuilder tb) {
            if (t.isCharacter()) {
                tb.newPendingTableCharacters();
                tb.markInsertionMode();
                tb.transition(InTableText);
                return tb.process(t);
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.name();
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
                    tb.process(new Token.StartTag("colgroup"));
                    return tb.process(t);
                } else if (StringUtil.in(name, "tbody", "tfoot", "thead")) {
                    tb.clearStackToTableContext();
                    tb.insert(startTag);
                    tb.transition(InTableBody);
                } else if (StringUtil.in(name, "td", "th", "tr")) {
                    tb.process(new Token.StartTag("tbody"));
                    return tb.process(t);
                } else if (name.equals("table")) {
                    tb.error(this);
                    boolean processed = tb.process(new Token.EndTag("table"));
                    if (processed) // only ignored if in fragment
                        return tb.process(t);
                } else if (StringUtil.in(name, "style", "script")) {
                    return tb.process(t, InHead);
                } else if (name.equals("input")) {
                    if (!startTag.attributes.get("type").equalsIgnoreCase("hidden")) {
                        return anythingElse(t, tb);
                    } else {
                        tb.insertEmpty(startTag);
                    }
                } else if (name.equals("form")) {
                    tb.error(this);
                    if (tb.getFormElement() != null)
                        return false;
                    else {
                        Element form = tb.insertEmpty(startTag);
                        tb.setFormElement(form);
                    }
                } else {
                    return anythingElse(t, tb);
                }
            } else if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.name();

                if (name.equals("table")) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.popStackToClose("table");
                    }
                    tb.resetInsertionMode();
                } else if (StringUtil.in(name,
                        "body", "caption", "col", "colgroup", "html", "tbody", "td", "tfoot", "th", "thead", "tr")) {
                    tb.error(this);
                    return false;
                } else {
                    return anythingElse(t, tb);
                }
            } else if (t.isEOF()) {
                if (tb.currentElement().nodeName().equals("html"))
                    tb.error(this);
                return true; // stops parsing
            }
            return anythingElse(t, tb);
        }

        boolean anythingElse(Token t, TreeBuilder tb) {
            tb.error(this);
            boolean processed = true;
            if (StringUtil.in(tb.currentElement().nodeName(), "table", "tbody", "tfoot", "thead", "tr")) {
                tb.setFosterInserts(true);
                processed = tb.process(t, InBody);
                tb.setFosterInserts(false);
            } else {
                processed = tb.process(t, InBody);
            }
            return processed;
        }
    },
    InTableText {
        boolean process(Token t, TreeBuilder tb) {
            switch (t.type) {
                case Character:
                    Token.Character c = t.asCharacter();
                    if (c.getData().equals(nullString)) {
                        tb.error(this);
                        return false;
                    } else {
                        tb.getPendingTableCharacters().add(c);
                    }
                    break;
                default:
                    if (tb.getPendingTableCharacters().size() > 0) {
                        for (Token.Character character : tb.getPendingTableCharacters()) {
                            if (!isWhitespace(character)) {
                                // InTable anything else section:
                                tb.error(this);
                                if (StringUtil.in(tb.currentElement().nodeName(), "table", "tbody", "tfoot", "thead", "tr")) {
                                    tb.setFosterInserts(true);
                                    tb.process(character, InBody);
                                    tb.setFosterInserts(false);
                                } else {
                                    tb.process(character, InBody);
                                }
                            } else
                                tb.insert(character);
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
        boolean process(Token t, TreeBuilder tb) {
            if (t.isEndTag() && t.asEndTag().name().equals("caption")) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.name();
                if (!tb.inTableScope(name)) {
                    tb.error(this);
                    return false;
                } else {
                    tb.generateImpliedEndTags();
                    if (!tb.currentElement().nodeName().equals("caption"))
                        tb.error(this);
                    tb.popStackToClose("caption");
                    tb.clearFormattingElementsToLastMarker();
                    tb.transition(InTable);
                }
            } else if ((
                    t.isStartTag() && StringUtil.in(t.asStartTag().name(),
                            "caption", "col", "colgroup", "tbody", "td", "tfoot", "th", "thead", "tr") ||
                            t.isEndTag() && t.asEndTag().name().equals("table"))
                    ) {
                tb.error(this);
                boolean processed = tb.process(new Token.EndTag("caption"));
                if (processed)
                    return tb.process(t);
            } else if (t.isEndTag() && StringUtil.in(t.asEndTag().name(),
                    "body", "col", "colgroup", "html", "tbody", "td", "tfoot", "th", "thead", "tr")) {
                tb.error(this);
                return false;
            } else {
                return tb.process(t, InBody);
            }
            return true;
        }
    },
    InColumnGroup {
        boolean process(Token t, TreeBuilder tb) {
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
                    String name = startTag.name();
                    if (name.equals("html"))
                        return tb.process(t, InBody);
                    else if (name.equals("col"))
                        tb.insertEmpty(startTag);
                    else
                        return anythingElse(t, tb);
                    break;
                case EndTag:
                    Token.EndTag endTag = t.asEndTag();
                    name = endTag.name();
                    if (name.equals("colgroup")) {
                        if (tb.currentElement().nodeName().equals("html")) { // frag case
                            tb.error(this);
                            return false;
                        } else {
                            tb.pop();
                            tb.transition(InTable);
                        }
                    } else
                        return anythingElse(t, tb);
                    break;
                case EOF:
                    if (tb.currentElement().nodeName().equals("html"))
                        return true; // stop parsing; frag case
                    else
                        return anythingElse(t, tb);
                default:
                    return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            boolean processed = tb.process(new Token.EndTag("colgroup"));
            if (processed) // only ignored in frag case
                return tb.process(t);
            return true;
        }
    },
    InTableBody {
        boolean process(Token t, TreeBuilder tb) {
            switch (t.type) {
                case StartTag:
                    Token.StartTag startTag = t.asStartTag();
                    String name = startTag.name();
                    if (name.equals("tr")) {
                        tb.clearStackToTableBodyContext();
                        tb.insert(startTag);
                        tb.transition(InRow);
                    } else if (StringUtil.in(name, "th", "td")) {
                        tb.error(this);
                        tb.process(new Token.StartTag("tr"));
                        return tb.process(startTag);
                    } else if (StringUtil.in(name, "caption", "col", "colgroup", "tbody", "tfoot", "thead")) {
                        return exitTableBody(t, tb);
                    } else
                        return anythingElse(t, tb);
                    break;
                case EndTag:
                    Token.EndTag endTag = t.asEndTag();
                    name = endTag.name();
                    if (StringUtil.in(name, "tbody", "tfoot", "thead")) {
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
                    } else if (StringUtil.in(name, "body", "caption", "col", "colgroup", "html", "td", "th", "tr")) {
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

        private boolean exitTableBody(Token t, TreeBuilder tb) {
            if (!(tb.inTableScope("tbody") || tb.inTableScope("thead") || tb.inScope("tfoot"))) {
                // frag case
                tb.error(this);
                return false;
            }
            tb.clearStackToTableBodyContext();
            tb.process(new Token.EndTag(tb.currentElement().nodeName())); // tbody, tfoot, thead
            return tb.process(t);
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            return tb.process(t, InTable);
        }
    },
    InRow {
        boolean process(Token t, TreeBuilder tb) {
            if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.name();

                if (StringUtil.in(name, "th", "td")) {
                    tb.clearStackToTableRowContext();
                    tb.insert(startTag);
                    tb.transition(InCell);
                    tb.insertMarkerToFormattingElements();
                } else if (StringUtil.in(name, "caption", "col", "colgroup", "tbody", "tfoot", "thead", "tr")) {
                    return handleMissingTr(t, tb);
                } else {
                    return anythingElse(t, tb);
                }
            } else if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.name();

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
                } else if (StringUtil.in(name, "tbody", "tfoot", "thead")) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        return false;
                    }
                    tb.process(new Token.EndTag("tr"));
                    return tb.process(t);
                } else if (StringUtil.in(name, "body", "caption", "col", "colgroup", "html", "td", "th")) {
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

        private boolean anythingElse(Token t, TreeBuilder tb) {
            return tb.process(t, InTable);
        }

        private boolean handleMissingTr(Token t, TreeBuilder tb) {
            boolean processed = tb.process(new Token.EndTag("tr"));
            if (processed)
                return tb.process(t);
            else
                return false;
        }
    },
    InCell {
        boolean process(Token t, TreeBuilder tb) {
            if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.name();

                if (StringUtil.in(name, "td", "th")) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        tb.transition(InRow); // might not be in scope if empty: <td /> and processing fake end tag
                        return false;
                    }
                    tb.generateImpliedEndTags();
                    if (!tb.currentElement().nodeName().equals(name))
                        tb.error(this);
                    tb.popStackToClose(name);
                    tb.clearFormattingElementsToLastMarker();
                    tb.transition(InRow);
                } else if (StringUtil.in(name, "body", "caption", "col", "colgroup", "html")) {
                    tb.error(this);
                    return false;
                } else if (StringUtil.in(name, "table", "tbody", "tfoot", "thead", "tr")) {
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
                    StringUtil.in(t.asStartTag().name(),
                            "caption", "col", "colgroup", "tbody", "td", "tfoot", "th", "thead", "tr")) {
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

        private boolean anythingElse(Token t, TreeBuilder tb) {
            return tb.process(t, InBody);
        }

        private void closeCell(TreeBuilder tb) {
            if (tb.inTableScope("td"))
                tb.process(new Token.EndTag("td"));
            else
                tb.process(new Token.EndTag("th")); // only here if th or td in scope
        }
    },
    InSelect {
        boolean process(Token t, TreeBuilder tb) {
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
                    String name = start.name();
                    if (name.equals("html"))
                        return tb.process(start, InBody);
                    else if (name.equals("option")) {
                        tb.process(new Token.EndTag("option"));
                        tb.insert(start);
                    } else if (name.equals("optgroup")) {
                        if (tb.currentElement().nodeName().equals("option"))
                            tb.process(new Token.EndTag("option"));
                        else if (tb.currentElement().nodeName().equals("optgroup"))
                            tb.process(new Token.EndTag("optgroup"));
                        tb.insert(start);
                    } else if (name.equals("select")) {
                        tb.error(this);
                        return tb.process(new Token.EndTag("select"));
                    } else if (StringUtil.in(name, "input", "keygen", "textarea")) {
                        tb.error(this);
                        if (!tb.inSelectScope("select"))
                            return false; // frag
                        tb.process(new Token.EndTag("select"));
                        return tb.process(start);
                    } else if (name.equals("script")) {
                        return tb.process(t, InHead);
                    } else {
                        return anythingElse(t, tb);
                    }
                    break;
                case EndTag:
                    Token.EndTag end = t.asEndTag();
                    name = end.name();
                    if (name.equals("optgroup")) {
                        if (tb.currentElement().nodeName().equals("option") && tb.aboveOnStack(tb.currentElement()) != null && tb.aboveOnStack(tb.currentElement()).nodeName().equals("optgroup"))
                            tb.process(new Token.EndTag("option"));
                        if (tb.currentElement().nodeName().equals("optgroup"))
                            tb.pop();
                        else
                            tb.error(this);
                    } else if (name.equals("option")) {
                        if (tb.currentElement().nodeName().equals("option"))
                            tb.pop();
                        else
                            tb.error(this);
                    } else if (name.equals("select")) {
                        if (!tb.inSelectScope(name)) {
                            tb.error(this);
                            return false;
                        } else {
                            tb.popStackToClose(name);
                            tb.resetInsertionMode();
                        }
                    } else
                        return anythingElse(t, tb);
                    break;
                case EOF:
                    if (!tb.currentElement().nodeName().equals("html"))
                        tb.error(this);
                    break;
                default:
                    return anythingElse(t, tb);
            }
            return true;
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            tb.error(this);
            return false;
        }
    },
    InSelectInTable {
        boolean process(Token t, TreeBuilder tb) {
            if (t.isStartTag() && StringUtil.in(t.asStartTag().name(), "caption", "table", "tbody", "tfoot", "thead", "tr", "td", "th")) {
                tb.error(this);
                tb.process(new Token.EndTag("select"));
                return tb.process(t);
            } else if (t.isEndTag() && StringUtil.in(t.asEndTag().name(), "caption", "table", "tbody", "tfoot", "thead", "tr", "td", "th")) {
                tb.error(this);
                if (tb.inTableScope(t.asEndTag().name())) {
                    tb.process(new Token.EndTag("select"));
                    return (tb.process(t));
                } else
                    return false;
            } else {
                return tb.process(t, InSelect);
            }
        }
    },
    AfterBody {
        boolean process(Token t, TreeBuilder tb) {
            if (isWhitespace(t)) {
                return tb.process(t, InBody);
            } else if (t.isComment()) {
                tb.insert(t.asComment()); // into html node
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                return tb.process(t, InBody);
            } else if (t.isEndTag() && t.asEndTag().name().equals("html")) {
                if (tb.isFragmentParsing()) {
                    tb.error(this);
                    return false;
                } else {
                    tb.transition(AfterAfterBody);
                }
            } else if (t.isEOF()) {
                // chillax! we're done
            } else {
                tb.error(this);
                tb.transition(InBody);
                return tb.process(t);
            }
            return true;
        }
    },
    InFrameset {
        boolean process(Token t, TreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter());
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag()) {
                Token.StartTag start = t.asStartTag();
                String name = start.name();
                if (name.equals("html")) {
                    return tb.process(start, InBody);
                } else if (name.equals("frameset")) {
                    tb.insert(start);
                } else if (name.equals("frame")) {
                    tb.insertEmpty(start);
                } else if (name.equals("noframes")) {
                    return tb.process(start, InHead);
                } else {
                    tb.error(this);
                    return false;
                }
            } else if (t.isEndTag() && t.asEndTag().name().equals("frameset")) {
                if (tb.currentElement().nodeName().equals("html")) { // frag
                    tb.error(this);
                    return false;
                } else {
                    tb.pop();
                    if (!tb.isFragmentParsing() && !tb.currentElement().nodeName().equals("frameset")) {
                        tb.transition(AfterFrameset);
                    }
                }
            } else if (t.isEOF()) {
                if (!tb.currentElement().nodeName().equals("html")) {
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
        boolean process(Token t, TreeBuilder tb) {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter());
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                tb.error(this);
                return false;
            } else if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                return tb.process(t, InBody);
            } else if (t.isEndTag() && t.asEndTag().name().equals("html")) {
                tb.transition(AfterAfterFrameset);
            } else if (t.isStartTag() && t.asStartTag().name().equals("noframes")) {
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
        boolean process(Token t, TreeBuilder tb) {
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype() || isWhitespace(t) || (t.isStartTag() && t.asStartTag().name().equals("html"))) {
                return tb.process(t, InBody);
            } else if (t.isEOF()) {
                // nice work chuck
            } else {
                tb.error(this);
                tb.transition(InBody);
                return tb.process(t);
            }
            return true;
        }
    },
    AfterAfterFrameset {
        boolean process(Token t, TreeBuilder tb) {
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype() || isWhitespace(t) || (t.isStartTag() && t.asStartTag().name().equals("html"))) {
                return tb.process(t, InBody);
            } else if (t.isEOF()) {
                // nice work chuck
            } else if (t.isStartTag() && t.asStartTag().name().equals("nofrmes")) {
                return tb.process(t, InHead);
            } else {
                tb.error(this);
                tb.transition(InBody);
                return tb.process(t);
            }
            return true;
        }
    },
    ForeignContent {
        boolean process(Token t, TreeBuilder tb) {
            return true;
            // todo: implement. Also; how do we get here?
        }
    };

    private static String nullString = String.valueOf('\u0000');

    abstract boolean process(Token t, TreeBuilder tb);

    private static boolean isWhitespace(Token t) {
        if (t.isCharacter()) {
            String data = t.asCharacter().getData();
            // todo: this checks more than spec - "\t", "\n", "\f", "\r", " "
            for (int i = 0; i < data.length(); i++) {
                char c = data.charAt(i);
                if (!Character.isWhitespace(c))
                    return false;
            }
            return true;
        }
        return false;
    }

    private static void handleRcData(Token.StartTag startTag, TreeBuilder tb) {
        tb.insert(startTag);
        tb.tokeniser.transition(TokeniserState.Rcdata);
        tb.markInsertionMode();
        tb.transition(Text);
    }

    private static void handleRawtext(Token.StartTag startTag, TreeBuilder tb) {
        tb.insert(startTag);
        tb.tokeniser.transition(TokeniserState.Rawtext);
        tb.markInsertionMode();
        tb.transition(Text);
    }
}
