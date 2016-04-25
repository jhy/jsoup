package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Entities;

import java.util.Arrays;

/**
 * Readers the input stream into tokens.
 */
final class Tokeniser {
    static final char replacementChar = '\uFFFD'; // replaces null character
    private static final char[] notCharRefCharsSorted = new char[]{'\t', '\n', '\r', '\f', ' ', '<', '&'};

    static {
        Arrays.sort(notCharRefCharsSorted);
    }

    private CharacterReader reader; // html input
    private ParseErrorList errors; // errors found while tokenising

    private TokeniserState state = TokeniserState.Data; // current tokenisation state
    private Token emitPending; // the token we are about to emit on next read
    private boolean isEmitPending = false;
    private String charsString = null; // characters pending an emit. Will fall to charsBuilder if more than one
    private StringBuilder charsBuilder = new StringBuilder(1024); // buffers characters to output as one token, if more than one emit per read
    StringBuilder dataBuffer = new StringBuilder(1024); // buffers data looking for </script>

    Token.Tag tagPending; // tag we are building up
    Token.StartTag startPending = new Token.StartTag();
    Token.EndTag endPending = new Token.EndTag();
    Token.Character charPending = new Token.Character();
    Token.Doctype doctypePending = new Token.Doctype(); // doctype building up
    Token.Comment commentPending = new Token.Comment(); // comment building up
    private String lastStartTag; // the last start tag emitted, to test appropriate end tag
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
        if (charsBuilder.length() > 0) {
            String str = charsBuilder.toString();
            charsBuilder.delete(0, charsBuilder.length());
            charsString = null;
            return charPending.data(str);
        } else if (charsString != null) {
            Token token = charPending.data(charsString);
            charsString = null;
            return token;
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
            lastStartTag = startTag.tagName;
            if (startTag.selfClosing)
                selfClosingFlagAcknowledged = false;
        } else if (token.type == Token.TokenType.EndTag) {
            Token.EndTag endTag = (Token.EndTag) token;
            if (endTag.attributes != null)
                error("Attributes incorrectly present on end tag");
        }
    }

    void emit(final String str) {
        // buffer strings up until last string token found, to emit only one token for a run of character refs etc.
        // does not set isEmitPending; read checks that
        if (charsString == null) {
            charsString = str;
        }
        else {
            if (charsBuilder.length() == 0) { // switching to string builder as more than one emit before read
                charsBuilder.append(charsString);
            }
            charsBuilder.append(str);
        }
    }

    void emit(char[] chars) {
        emit(String.valueOf(chars));
    }

    void emit(char c) {
        emit(String.valueOf(c));
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

    final private char[] charRefHolder = new char[1]; // holder to not have to keep creating arrays
    char[] consumeCharacterReference(Character additionalAllowedCharacter, boolean inAttribute) {
        if (reader.isEmpty())
            return null;
        if (additionalAllowedCharacter != null && additionalAllowedCharacter == reader.current())
            return null;
        if (reader.matchesAnySorted(notCharRefCharsSorted))
            return null;

        final char[] charRef = charRefHolder;
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
                charRef[0] = replacementChar;
                return charRef;
            } else {
                // todo: implement number replacement table
                // todo: check for extra illegal unicode points as parse errors
                if (charval < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                    charRef[0] = (char) charval;
                    return charRef;
                } else
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
            charRef[0] = Entities.getCharacterByName(nameRef);
            return charRef;
        }
    }

    Token.Tag createTagPending(boolean start) {
        tagPending = start ? startPending.reset() : endPending.reset();
        return tagPending;
    }

    void emitTagPending() {
        tagPending.finaliseTag();
        emit(tagPending);
    }

    void createCommentPending() {
        commentPending.reset();
    }

    void emitCommentPending() {
        emit(commentPending);
    }

    void createDoctypePending() {
        doctypePending.reset();
    }

    void emitDoctypePending() {
        emit(doctypePending);
    }

    void createTempBuffer() {
        Token.reset(dataBuffer);
    }

    boolean isAppropriateEndTagToken() {
        return lastStartTag != null && tagPending.tagName.equals(lastStartTag);
    }

    String appropriateEndTagName() {
        if (lastStartTag == null)
            return null;
        return lastStartTag;
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
