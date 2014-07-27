package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Readers the input stream into tokens.
 */
class Tokeniser {
    static final char replacementChar = '\uFFFD'; // replaces null character

    private CharacterReader reader; // html input
    private ParseErrorList errors; // errors found while tokenising

    private TokeniserState state = TokeniserState.Data; // current tokenisation state
    private Token emitPending; // the token we are about to emit on next read
    private boolean isEmitPending = false;
    private StringBuilder charBuffer = new StringBuilder(); // buffers characters to output as one token
    StringBuilder dataBuffer; // buffers data looking for </script>

    Token.Tag tagPending; // tag we are building up
    Token.Doctype doctypePending; // doctype building up
    Token.Comment commentPending; // comment building up
    private Token.StartTag lastStartTag; // the last start tag emitted, to test appropriate end tag
    private boolean selfClosingFlagAcknowledged = true;

    Tokeniser(CharacterReader reader, ParseErrorList errors) {
        this.reader = reader;
        this.errors = errors;
    }

    Token read() {
        if (!selfClosingFlagAcknowledged) {
            error("Self closing flag not acknowledged");
            selfClosingFlagAcknowledged = true;
        }

        while (!isEmitPending)
            state.read(this, reader);

        // if emit is pending, a non-character token was found: return any chars in buffer, and leave token for next read:
        if (charBuffer.length() > 0) {
            String str = charBuffer.toString();
            charBuffer.delete(0, charBuffer.length());
            return new Token.Character(str);
        } else {
            isEmitPending = false;
            return emitPending;
        }
    }

    void emit(Token token) {
        Validate.isFalse(isEmitPending, "There is an unread token pending!");

        emitPending = token;
        isEmitPending = true;

        if (token.type == Token.TokenType.StartTag) {
            Token.StartTag startTag = (Token.StartTag) token;
            lastStartTag = startTag;
            if (startTag.selfClosing)
                selfClosingFlagAcknowledged = false;
        } else if (token.type == Token.TokenType.EndTag) {
            Token.EndTag endTag = (Token.EndTag) token;
            if (endTag.attributes != null)
                error("Attributes incorrectly present on end tag");
        }
    }

    void emit(String str) {
        // buffer strings up until last string token found, to emit only one token for a run of character refs etc.
        // does not set isEmitPending; read checks that
        charBuffer.append(str);
    }

    void emit(char[] chars) {
        charBuffer.append(chars);
    }

    void emit(char c) {
        charBuffer.append(c);
    }

    TokeniserState getState() {
        return state;
    }

    void transition(TokeniserState state) {
        this.state = state;
    }

    void advanceTransition(TokeniserState state) {
        reader.advance();
        this.state = state;
    }

    void acknowledgeSelfClosingFlag() {
        selfClosingFlagAcknowledged = true;
    }

    char[] consumeCharacterReference(Character additionalAllowedCharacter, boolean inAttribute) {
        if (reader.isEmpty())
            return null;
        if (additionalAllowedCharacter != null && additionalAllowedCharacter == reader.current())
            return null;
        if (reader.matchesAny('\t', '\n', '\r', '\f', ' ', '<', '&'))
            return null;

        reader.mark();
        if (reader.matchConsume("#")) { // numbered
            boolean isHexMode = reader.matchConsumeIgnoreCase("X");
            String numRef = isHexMode ? reader.consumeHexSequence() : reader.consumeDigitSequence();
            if (numRef.length() == 0) { // didn't match anything
                characterReferenceError("numeric reference with no numerals");
                reader.rewindToMark();
                return null;
            }
            if (!reader.matchConsume(";"))
                characterReferenceError("missing semicolon"); // missing semi
            int charval = -1;
            try {
                int base = isHexMode ? 16 : 10;
                charval = Integer.valueOf(numRef, base);
            } catch (NumberFormatException e) {
            } // skip
            if (charval == -1 || (charval >= 0xD800 && charval <= 0xDFFF) || charval > 0x10FFFF) {
                characterReferenceError("character outside of valid range");
                return new char[]{replacementChar};
            } else {
                // todo: implement number replacement table
                // todo: check for extra illegal unicode points as parse errors
                return Character.toChars(charval);
            }
        } else { // named
            // get as many letters as possible, and look for matching entities.
            String nameRef = reader.consumeLetterThenDigitSequence();
            boolean looksLegit = reader.matches(';');
            // found if a base named entity without a ;, or an extended entity with the ;.
            boolean found = (Entities.isBaseNamedEntity(nameRef) || (Entities.isNamedEntity(nameRef) && looksLegit));

            if (!found) {
                reader.rewindToMark();
                if (looksLegit) // named with semicolon
                    characterReferenceError(String.format("invalid named referenece '%s'", nameRef));
                return null;
            }
            if (inAttribute && (reader.matchesLetter() || reader.matchesDigit() || reader.matchesAny('=', '-', '_'))) {
                // don't want that to match
                reader.rewindToMark();
                return null;
            }
            if (!reader.matchConsume(";"))
                characterReferenceError("missing semicolon"); // missing semi
            return new char[]{Entities.getCharacterByName(nameRef)};
        }
    }

    Token.Tag createTagPending(boolean start) {
        tagPending = start ? new Token.StartTag() : new Token.EndTag();
        return tagPending;
    }

    void emitTagPending() {
        tagPending.finaliseTag();
        emit(tagPending);
    }

    void createCommentPending() {
        commentPending = new Token.Comment();
    }

    void emitCommentPending() {
        emit(commentPending);
    }

    void createDoctypePending() {
        doctypePending = new Token.Doctype();
    }

    void emitDoctypePending() {
        emit(doctypePending);
    }

    void createTempBuffer() {
        dataBuffer = new StringBuilder();
    }

    boolean isAppropriateEndTagToken() {
        if (lastStartTag == null)
            return false;
        return tagPending.tagName.equals(lastStartTag.tagName);
    }

    String appropriateEndTagName() {
        return lastStartTag.tagName;
    }

    void error(TokeniserState state) {
        if (errors.canAddError())
            errors.add(new ParseError(reader.pos(), "Unexpected character '%s' in input state [%s]", reader.current(), state));
    }

    void eofError(TokeniserState state) {
        if (errors.canAddError())
            errors.add(new ParseError(reader.pos(), "Unexpectedly reached end of file (EOF) in input state [%s]", state));
    }

    private void characterReferenceError(String message) {
        if (errors.canAddError())
            errors.add(new ParseError(reader.pos(), "Invalid character reference: %s", message));
    }

    private void error(String errorMsg) {
        if (errors.canAddError())
            errors.add(new ParseError(reader.pos(), errorMsg));
    }

    boolean currentNodeInHtmlNS() {
        // todo: implement namespaces correctly
        return true;
        // Element currentNode = currentNode();
        // return currentNode != null && currentNode.namespace().equals("HTML");
    }

    /**
     * Utility method to consume reader and unescape entities found within.
     * @param inAttribute
     * @return unescaped string from reader
     */
    String unescapeEntities(boolean inAttribute) {
        StringBuilder builder = new StringBuilder();
        while (!reader.isEmpty()) {
            builder.append(reader.consumeTo('&'));
            if (reader.matches('&')) {
                reader.consume();
                char[] c = consumeCharacterReference(null, inAttribute);
                if (c == null || c.length==0)
                    builder.append('&');
                else
                    builder.append(c);
            }
        }
        return builder.toString();
    }
}