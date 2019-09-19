/*
 * Copyright (C) 2010 Google Inc.
 * Copyright (C) 2019 Andrej Fink.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Powerful JSON (<a href="http://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a>, 7158,
 * <a href="http://www.ietf.org/rfc/rfc7159.txt">RFC 7159</a>) parser.
 * It can consume almost every text as JSON and convert it internally to an XML-tree.
 * <br><br>
 * Heavily inspired by Google Gson (c) 2010 Google Inc.
 * <br><br>
 * [example.json]
 * <pre>
 * {projects = [
 * {
 * 'project_name' => "Google Gson";
 * "url": "https://github.com/google/gson",
 * "rating": 4.956,
 * "contributors": [{
 * first_name: Jesse, "last_name": "Wilson",
 * "home_page": "https://medium.com/@swankjesse"
 * }]
 * },{
 * "project_name": "jsoup",
 * "url": "https://jsoup.org",
 * "rating": 5e10,
 * "contributors": [
 * {
 * "first_name": "Jonathan", "last_name": "Hedley",
 * "home_page": "https://jhy.io"
 * },{
 * "first_name": "Andrej", "last_name": "Fink",
 * "home_page": "https://github.com/magicprinc"
 * }
 * ]
 * }]}
 * </pre>
 * <pre>
 * Document doc = Jsoup.parse(exampleJson, "UTF-8", "", JsonTreeBuilder.jsonParser());
 * </pre>
 * [internal xml tree]
 * <pre>
 * &lt;obj>&lt;arr id="projects">
 * &lt;obj>
 * &lt;val id="project_name" class="quot quoted str">Google Gson&lt;/val>
 * &lt;val id="url" class="quot quoted str">https://github.com/google/gson&lt;/val>
 * &lt;val id="rating" class="unquoted num">4.956&lt;/val>
 * &lt;arr id="contributors">
 * &lt;obj>
 * &lt;val id="first_name" class="quot quoted str">Jesse&lt;/val>
 * &lt;val id="last_name" class="quot quoted str">Wilson&lt;/val>
 * &lt;val id="home_page" class="quot quoted str">https://medium.com/@swankjesse&lt;/val>
 * &lt;/obj>
 * &lt;/arr>
 * &lt;/obj>
 * &lt;obj>
 * &lt;val id="project_name" class="quot quoted str">jsoup&lt;/val>
 * &lt;val id="url" class="quot quoted str">https://jsoup.org&lt;/val>
 * &lt;val id="rating" class="unquoted num">5e10&lt;/val>
 * &lt;arr id="contributors">
 * &lt;obj>
 * &lt;val id="first_name" class="quot quoted str">Jonathan&lt;/val>
 * &lt;val id="last_name" class="quot quoted str">Hedley&lt;/val>
 * &lt;val id="home_page" class="quot quoted str">https://jhy.io&lt;/val>
 * &lt;/obj>
 * &lt;obj>
 * &lt;val id="first_name" class="quot quoted str">Andrej&lt;/val>
 * &lt;val id="last_name" class="quot quoted str">Fink&lt;/val>
 * &lt;val id="home_page" class="quot quoted str">https://github.com/magicprinc&lt;/val>
 * &lt;/obj>
 * &lt;/arr>
 * &lt;/obj>
 * &lt;/arr>&lt;/obj>
 * </pre>
 * <pre>
 * assert "Fink".equals(doc.select("#contributors obj:eq(1) #last_name").text());
 * assert "jsoup".equals(doc.select("#projects #project_name.str.unquoted").text());
 * </pre>
 *
 * <pre>
 * assertEquals("&lt;arr>&lt;val class=\"bool\">true&lt;/val>&lt;val class=\"bool\">true&lt;/val>&lt;/arr>",
 * JsonTreeBuilder.jsonToXml("[true, true]"));
 * </pre>
 *
 * @author Andrej Fink [aprpda@gmail.com]
 * @author Jesse Wilson
 * <br>see also com.google.gson.stream.JsonReader, com.google.gson.stream.JsonReader#setLenient
 */
@SuppressWarnings("fallthrough")
public class JsonTreeBuilder extends XmlTreeBuilder {
  /** The only non-execute prefix this parser permits */
    static final String NON_EXECUTE_PREFIX = ")]}'\n";
    static final char BOM1 = '\uFeFf';
    static final char BOM2 = '\uFfFe';

    static final char EOF = CharacterReader.EOF;

    static final String STR_VAL = "val";
    static final String STR_OBJ = "obj";
    static final String STR_ARR = "arr";
    static final String STR_UNK = "unk";

    static final String STR_ID = "id";
    static final String STR_CLASS = "class";

    enum NEXT_TOKEN {
        UNKNOWN,

        BEGIN_OBJECT, END_OBJECT,
        BEGIN_ARRAY, END_ARRAY,

        TRUE, FALSE, NULL,

        SINGLE_QUOTED, DOUBLE_QUOTED, UNQUOTED,

        SINGLE_QUOTED_NAME, DOUBLE_QUOTED_NAME, UNQUOTED_NAME,

        EOF
    }

    enum SCOPE {
        EMPTY_DOCUMENT,
        /** A document with at an array or object. */
        NONEMPTY_DOCUMENT,

        EMPTY_ARRAY, ARRAY,

        OBJECT,
        /** An object whose most recent element is a key. The next element must be a value. */
        DANGLING_NAME
    }

    protected enum VALUE_CLASS {
        SINGLE_QUOTED("apos quoted str", "apos quoted num"),
        DOUBLE_QUOTED("quot quoted str", "quot quoted num"),
        UNQUOTED("unquoted str", "unquoted num");

        private final String cssClassString;
        private final String cssClassNumeric;

        VALUE_CLASS(String str, String num) {
            cssClassString = str;
            cssClassNumeric = num;
        }

        public String str () { return cssClassString;}
        public String num () { return cssClassNumeric;}
    }

    static final Token.EndTag endTagObj = new Token.EndTag();
    static final Token.EndTag endTagArr = new Token.EndTag();
    static {
        endTagObj.name(STR_OBJ);
        endTagArr.name(STR_ARR);
    }


    boolean qclass = true;

    final ArrayList<String> comment = new ArrayList<>();
    private final StringBuilder builder = new StringBuilder(2000);
    String currentName;
    SCOPE currentScope = SCOPE.EMPTY_DOCUMENT;
    int tokensCount;//statistics for tests


    NEXT_TOKEN nextToken() {
        tokensCount++;

        SCOPE scope = currentScope;
        char c;
        switch (scope) {
            case EMPTY_ARRAY:
                currentScope = SCOPE.ARRAY;
                break;

            case ARRAY:
                c = nextNonWhitespace();
                switch (c) {
                    case ']':
                    case '}':
                    case EOF:
                        return NEXT_TOKEN.END_ARRAY;// [value>]
                    case ';':
                    case ',':
                        break;
                    default://Unterminated array
                        reader.unconsume();//Don't consume the first character in an unquoted string
                }
                break;

            case OBJECT:
                currentScope = SCOPE.DANGLING_NAME;

                while (true) {
                    c = nextNonWhitespace();

                    switch (c) {
                        case ';':
                        case ',':
                            break;//comma before the next element
                        case '"':
                            return NEXT_TOKEN.DOUBLE_QUOTED_NAME;
                        case '\'':
                            return NEXT_TOKEN.SINGLE_QUOTED_NAME;
                        case '}':
                        case ']':
                        case EOF:
                            return NEXT_TOKEN.END_OBJECT;// ,} is OK too
                        default:
                            reader.unconsume();//Don't consume the first character in an unquoted string
                            return NEXT_TOKEN.UNQUOTED_NAME;
                    }
                }

            case DANGLING_NAME:
                assert currentName != null;
                currentScope = SCOPE.OBJECT;
                c = nextNonWhitespace();
                switch (c) {
                    case EOF:
                        return NEXT_TOKEN.NULL;
                    case ':':
                        break;//a colon before the value !skip
                    case '=':// = or => !skip
                        if (reader.matches('>')) {
                            reader.advance();
                        }
                        break;
                    default://Expected ':'
                        reader.unconsume();//Don't consume the first character in an unquoted string
                }
                break;

            case EMPTY_DOCUMENT:
                consumeNonExecutePrefix();
                currentScope = SCOPE.NONEMPTY_DOCUMENT;
                break;

            case NONEMPTY_DOCUMENT:
                c = nextNonWhitespace();
                switch (c) {
                    case EOF:
                        return NEXT_TOKEN.EOF;
                    case ',':
                    case ';':
                        break;//not in JSON standard, but we allow val1,val2,...
                    default:
                        reader.unconsume();//Don't consume the first character in an unquoted string
                }
        }//scope
        assert currentScope != SCOPE.DANGLING_NAME;
        assert scope != SCOPE.OBJECT;

        c = nextNonWhitespace();
        switch (c) {
            case EOF:
                if (currentName != null || scope == SCOPE.ARRAY) {
                    return NEXT_TOKEN.NULL;
                } else if (scope == SCOPE.EMPTY_ARRAY) {
                    return NEXT_TOKEN.END_ARRAY;
                }
                return NEXT_TOKEN.EOF;
            case ']':
            case '}':
                if (scope == SCOPE.EMPTY_ARRAY) {
                    return NEXT_TOKEN.END_ARRAY;
                }
            case ';':
            case ',':
                if (scope == SCOPE.ARRAY || scope == SCOPE.EMPTY_ARRAY) {
                    reader.unconsume();
                    return NEXT_TOKEN.NULL;
                } else if (currentName != null) {
                    return NEXT_TOKEN.NULL;
                }
                reader.unconsume();
                return NEXT_TOKEN.UNKNOWN;

            case '\'':
                return NEXT_TOKEN.SINGLE_QUOTED;
            case '"':
                return NEXT_TOKEN.DOUBLE_QUOTED;
            case '[':
                return NEXT_TOKEN.BEGIN_ARRAY;
            case '{':
                return NEXT_TOKEN.BEGIN_OBJECT;
            case ':':
            case '=':
                if (scope == SCOPE.DANGLING_NAME) {
                    reader.unconsume();
                    return NEXT_TOKEN.UNQUOTED;//{a:: <obj><val id="id"></val>
                }
            default:
                reader.unconsume();// Don't consume the first character in a literal value
        }

        NEXT_TOKEN result = nextKeyword();
        if (result != NEXT_TOKEN.UNKNOWN) {
            return result;
        }

        if (!isLiteral(reader.current())) {//Expected value
            return NEXT_TOKEN.UNKNOWN;
        }

        return NEXT_TOKEN.UNQUOTED;
    }


    NEXT_TOKEN nextKeyword() {
        // Figure out which keyword we're matching against by its first character.
        char c = reader.current();
        String keyword;
        NEXT_TOKEN peeking;
        if (c == 't' || c == 'T') {
            keyword = "true";
            peeking = NEXT_TOKEN.TRUE;
        } else if (c == 'f' || c == 'F') {
            keyword = "false";
            peeking = NEXT_TOKEN.FALSE;
        } else if (c == 'n' || c == 'N') {
            keyword = "null";
            peeking = NEXT_TOKEN.NULL;
        } else {
            return NEXT_TOKEN.UNKNOWN;
        }

        reader.mark();
        boolean eq = reader.matchConsumeIgnoreCase(keyword);
        if (!eq) {
            reader.rewindToMark();
            return NEXT_TOKEN.UNKNOWN;// Don't match trues, falsey or nullsoft!
        }

        if (!isLiteral(reader.current()) || reader.current() == EOF) {
            return peeking;// We've found the keyword followed either by EOF or by a non-literal character
        }

        reader.rewindToMark();
        return NEXT_TOKEN.UNKNOWN;// Don't match trues, falsey or nullsoft
    }


    /** not literals: / \ ; # = { } [ ] : , space \t \f \r \n */
    boolean isLiteral(char c) {
        int contains = Arrays.binarySearch(NON_LITERALS, c);
        return contains < 0;
    }


    /**
     * Returns the string up to but not including {@code quote}, unescaping any
     * character escape sequences encountered along the way. The opening quote
     * should have already been read. This consumes the closing quote, but does
     * not include it in the returned string.
     *
     * @param quote ' | " | \n
     */
    String collectQuoted(char quote) {
        builder.setLength(0);
        while (true) {
            String s = reader.consumeToAny(quote, '\\');
            builder.append(s);

            char c = reader.consume();//quote, EOF, escape or end-of-buffer

            if (c == quote || c == EOF) {
                return builder.toString();

            } else if (c == '\\') {
                c = readEscapeCharacter();
                if (c != EOF) {// \ isn't at the end of file
                    builder.append(c);
                }
            } else {//just end of buffer
                builder.append(c);
            }
        }//wloop
    }

    String collectTo(String seq) {
        builder.setLength(0);
        while (true) {
            String s = reader.consumeTo(seq);
            builder.append(s);

            boolean found = reader.matchConsume(seq);//seq, EOF or end-of-buffer

            if (found || reader.isEmpty()) {
                return builder.toString();
            }
        }//wloop
    }


    static final char[] NON_LITERALS = new char[16];
    static {
        int i = 0;
        NON_LITERALS[i++] = '/';
        NON_LITERALS[i++] = '\\';
        NON_LITERALS[i++] = ';';
        NON_LITERALS[i++] = '#';
        NON_LITERALS[i++] = '=';
        NON_LITERALS[i++] = '{';
        NON_LITERALS[i++] = '}';
        NON_LITERALS[i++] = '[';
        NON_LITERALS[i++] = ']';
        NON_LITERALS[i++] = ':';
        NON_LITERALS[i++] = ',';
        NON_LITERALS[i++] = ' ';
        NON_LITERALS[i++] = '\t';
        NON_LITERALS[i++] = '\f';
        NON_LITERALS[i++] = '\r';
        NON_LITERALS[i++] = '\n';
        Arrays.sort(NON_LITERALS);
        assert i == 16;
    }

    /** Returns an unquoted value as a string. */
    String collectUnQuoted() {
        builder.setLength(0);
        while (true) {
            String s = reader.consumeToAnySorted(NON_LITERALS);
            builder.append(s);

            char c = reader.consume();//EOF, non-literal or end-of-buffer

            if (c == EOF) {
                return builder.toString();

            } else if (isLiteral(c)) {
                builder.append(c);

            } else {//non-literal
                reader.unconsume();
                return builder.toString();
            }
        }//wloop
    }


    /**
     * Returns the next character in the stream that is neither whitespace nor a
     * part of a comment. When this returns, the returned character is always at
     * {@code buffer[pos-1]}; this means the caller can always push back the
     * returned character by decrementing {@code pos}.
     */
    char nextNonWhitespace() {
        while (true) {
            char c = reader.consume();
            if (c == ' ' || c == '\r' || c == '\t' || c == '\n' || c == '\f') {
                continue;
            }

            if (c == EOF) {
                return EOF;
            }

            if (c == '/') {
                switch (reader.current()) {
                    case '*':// skip a /* c-style comment */
                        reader.advance();
                        comment.add(collectTo("*/"));
                        continue;

                    case '/': // skip a // end-of-line comment
                        reader.advance();
                        comment.add(collectQuoted('\n'));
                        continue;

                    default:
                        return '/';//==c
                }

            } else if (c == '#') {
                /* Skip a # hash end-of-line comment. The JSON RFC doesn't specify this behaviour,
                but it's required to parse existing documents. See http://b/2571423. */
                comment.add(collectQuoted('\n'));
            } else {
                return c;
            }
        }//wloop
    }

    @Override
    public String toString() {
        String s = reader.toString();
        if (s.length() > 80) {
            s = s.substring(0, 80) + "...";
        }
        return getClass().getSimpleName() + "@ " + reader.pos()
                + " ='" + reader.current() + "' U+"+Integer.toHexString(reader.current()) + " scope=" + currentScope
                + ", name='" + currentName + "', tokens: " + tokensCount
                + ", stack: " + stack.size() + " >>" + s;
    }

    /**
     * Unescapes the character identified by the character or characters that
     * immediately follow a backslash. The backslash '\' should have already
     * been read. This supports both unicode escapes "u000A" and two-character escapes "\n".
     */
    char readEscapeCharacter() {
        char escaped = reader.consume();
        switch (escaped) {
            case 'u': // Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4), 16);
                int result = 0;
                for (int i = 0; i < 4; i++) {
                    char c = reader.consume();

                    if (c >= '0' && c <= '9') {
                        result = (result << 4) + (c - '0');
                    } else if (c >= 'a' && c <= 'f') {
                        result = (result << 4) + (c - 'a' + 10);
                    } else if (c >= 'A' && c <= 'F') {
                        result = (result << 4) + (c - 'A' + 10);
                    } else if (c == EOF) {
                        break;
                    } else {
                        reader.unconsume();
                        break;
                    }
                }
                return (char) result;

            case 't':
                return '\t';
            case 'b':
                return '\b';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 'f':
                return '\f';
            //case '\n': case '\'': case '"': case '\\': case '/':
            default://gson throws an error when none of the above cases are matched: Invalid escape sequence
                return escaped;
        }
    }

    /** Consumes the non-execute prefix if it exists. */
    void consumeNonExecutePrefix() {
        char c = reader.current();
        if (c == BOM1 || c == BOM2) {
            reader.advance();
        }
        nextNonWhitespace();// fast forward through the leading whitespace
        reader.unconsume();

        reader.matchConsume(NON_EXECUTE_PREFIX);
    }


    @Override
    protected boolean process(Token token) {
        throw new AssertionError("process(Token) - must not be called, but " + token);
    }


    @Override
    protected void runParser() {
        while (true) {
            NEXT_TOKEN t = nextToken();

            insertComments();

            switch (t) {
                case BEGIN_OBJECT:
                    currentScope = SCOPE.OBJECT;
                    insertStartTag(STR_OBJ);
                    break;

                case END_OBJECT:
                    popStackToClose(endTagObj);
                    detectCurrentScope();
                    break;

                case BEGIN_ARRAY:
                    currentScope = SCOPE.EMPTY_ARRAY;
                    insertStartTag(STR_ARR);
                    break;

                case END_ARRAY:
                    popStackToClose(endTagArr);
                    detectCurrentScope();
                    break;

                case TRUE:
                    insertBoolean(true);
                    break;
                case FALSE:
                    insertBoolean(false);
                    break;

                case NULL:
                    insertNull();
                    break;

                case SINGLE_QUOTED:
                    insertVal(collectQuoted('\''), VALUE_CLASS.SINGLE_QUOTED);
                    break;
                case DOUBLE_QUOTED:
                    insertVal(collectQuoted('"'), VALUE_CLASS.DOUBLE_QUOTED);
                    break;
                case UNQUOTED:
                    insertVal(collectUnQuoted(), VALUE_CLASS.UNQUOTED);
                    break;

                case SINGLE_QUOTED_NAME:
                    currentName = collectQuoted('\'');
                    break;
                case DOUBLE_QUOTED_NAME:
                    currentName = collectQuoted('"');
                    break;
                case UNQUOTED_NAME:
                    currentName = collectUnQuoted();
                    break;

                case EOF:
                    return;
                case UNKNOWN:
                    insertUnknown();
            }
        }//wloop
    }

    void detectCurrentScope() {
        int size = stack.size();
        if (size <= 1) {
            currentScope = SCOPE.NONEMPTY_DOCUMENT;
            return;
        }

        Element owner = stack.get(size - 1);
        if (owner.tagName().equals(STR_OBJ)) {
            currentScope = SCOPE.OBJECT;
        } else {
            assert owner.tagName().equals(STR_ARR);
            currentScope = SCOPE.ARRAY;
        }
    }

    void insertStartTag(String tagName) {
        Tag tag = Tag.valueOf(tagName, settings);

        Element el;
        if (currentName != null) {
            Attributes attr = new Attributes();
            attr.put(STR_ID, currentName);
            currentName = null;
            el = new Element(tag, baseUri, attr);
        } else {
            el = new Element(tag, baseUri);
        }

        insertNode(el);
        stack.add(el);
    }


    void insertBoolean(boolean value) {
        Tag tag = Tag.valueOf(STR_VAL, settings);

        Attributes attr = new Attributes();
        Element el = new Element(tag, baseUri, attr);

        if (currentName != null) {
            attr.put(STR_ID, currentName);
            currentName = null;
        }
        attr.put(STR_CLASS, "bool");

        String s = Boolean.toString(value);
        if (qclass) {
            attr.put("value", s);
        }
        el.appendText(s);

        insertNode(el);
    }


    void insertNull() {
        Tag tag = Tag.valueOf(STR_VAL, settings);
        tag.setSelfClosing();

        Attributes attr = new Attributes();
        Element el = new Element(tag, baseUri, attr);

        if (currentName != null) {
            attr.put(STR_ID, currentName);
            currentName = null;
        }
        attr.put(STR_CLASS, "null");

        insertNode(el);
    }


    void insertVal(String value, VALUE_CLASS typeClass) {
        Tag tag = Tag.valueOf(STR_VAL, settings);

        Attributes attr = new Attributes();
        if (currentName != null) {
            attr.put(STR_ID, currentName);
            currentName = null;
        }
        Element el = new Element(tag, baseUri, attr);
        if (value.length() > 0) {
            el.appendText(value);
        }
        addMetaDataToValue(el, value, typeClass);
        insertNode(el);
    }


    /** Extension point: one can add "number", "string", "url", etc `class` detection */
    protected void addMetaDataToValue(Element el, String value, VALUE_CLASS typeClass) {
        if (qclass) {
            String cls = isNumeric(value) ? typeClass.num() : typeClass.str();
            el.attr(STR_CLASS, cls);
        }
    }


    void insertComments() {
        for (String s : comment) {
            insertNode(new Comment(s));
        }
        comment.clear();
    }


    void insertUnknown() {
        Tag tag = Tag.valueOf(STR_UNK, settings);
        tag.setSelfClosing();

        Attributes attr = new Attributes();

        if (currentName != null) {
            attr.put(STR_ID, currentName);
            currentName = null;
        }
        attr.put("pos", Integer.toString(reader.pos()));
        char bad = reader.consume();
        attr.put("char", String.valueOf(bad));
        attr.put("hex", Integer.toHexString(bad));
        attr.put("scope", currentScope.toString());
        attr.put("stack", Integer.toString(stack.size()));
        attr.put("tokens", Integer.toString(tokensCount));

        Element el = new Element(tag, baseUri, attr);
        insertNode(el);
    }

    protected boolean isNumeric(CharSequence s) {
        int i = 0, len = s.length(), e = 0, dec = 0;
        char c;
        //1.skip left spaces + -
        while (i < len) {
            c = s.charAt(i);
            if (isSpace(c) || c == '+' || c == '-') {
                i++;
            } else {
                break;
            }
        }
        //2.skip right spaces
        while (i < len) {
            c = s.charAt(len - 1);
            if (isSpace(c)) {
                len--;
            } else {
                break;
            }
        }
        //3.go through the middle
        if (i >= len) {
            return false;
        }
        c = s.charAt(i++);
        if (!Character.isDigit(c)) {
            return false;
        }
        c = s.charAt(--len);
        if (!Character.isDigit(c) && (c != '.')) {
            return false;
        }

        while (i < len) {
            c = s.charAt(i++);
            switch (c) {
                case '.':
                    dec++;
                    if (dec > 1 || e > 0) {
                        return false;
                    }
                    break;
                case 'e':
                case 'E':
                    e++;
                    if (e > 1) {
                        return false;
                    }
                    while (i < len) {
                        c = s.charAt(i);
                        if (isSpace(c)) {
                            i++;
                        } else {
                            break;
                        }
                    }
                    if (c == '+' || c == '-') {
                        i++;
                    }
                    if (dec == 1) {
                        dec--;
                    }//second chance for .
                    break;
                default:
                    boolean spaceOrDigit = isSpace(c) || Character.isDigit(c);
                    if (!spaceOrDigit) {
                        return false;
                    }
            }
        }
        return true;
    }

    protected boolean isSpace(char c) {
        return c <= ' ' || Character.isSpaceChar(c) || Character.isWhitespace(c);
    }

    public static Parser jsonParser(boolean extraInfoInAttrs) {
        JsonTreeBuilder treeBuilder = new JsonTreeBuilder();
        treeBuilder.qclass = extraInfoInAttrs;
        return new Parser(treeBuilder);
    }


    /**
     * Converts JSON to XML.
     *
     * @param json any text, preferably JSON
     * @return compact xml
     */
    public static String jsonToXml(String json) {
        Document doc = Jsoup.parse(json, "", jsonParser(false));
        doc.outputSettings().prettyPrint(false);
        return doc.html();
    }
}