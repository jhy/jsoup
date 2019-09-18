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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import junit.framework.TestCase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.JsonTreeBuilder.NEXT_TOKEN;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import static org.jsoup.parser.JsonTreeBuilder.jsonParser;
import static org.jsoup.parser.JsonTreeBuilder.jsonToXml;

@SuppressWarnings({"resource", "EmptyCatchBlock", "ProhibitedExceptionCaught",
    "SingleCharacterStringConcatenation","UnnecessarilyQualifiedStaticallyImportedElement",
    "ResultOfObjectAllocationIgnored","JUnitTestMethodWithNoAssertions"})
public final class JsonTreeBuilderTest extends TestCase {

  public void testReadArray() {
    String json = "[true, true]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testReadEmptyArray() {
    String json = "[]";
    String xml = "<arr></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testReadObject() {
    String json = "{\"a\": \"android\", \"b\": \"banana\"}";
    String xml = "<obj><val id=\"a\">android</val><val id=\"b\">banana</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testReadEmptyObject() {
    String json = "{}";
    String xml = "<obj></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipArray() {
    String json = "{\"a\": [\"one\", \"two\", \"three\"], \"b\": 123}";
    String xml = "<obj><arr id=\"a\"><val>one</val><val>two</val><val>three</val></arr><val id=\"b\">123</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipObject() {
    String json = "{\"a\": { \"c\": [], \"d\": [true, true, {}] }, \"b\": \"banana\"}";
    String xml = "<obj><obj id=\"a\"><arr id=\"c\"></arr><arr id=\"d\"><val class=\"bool\">true</val><val class=\"bool\">true</val><obj></obj></arr></obj><val id=\"b\">banana</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipObjectAfterPeek() throws Exception {
    String json = "{" + "  \"one\": { \"num\": 1 }"
        + ", \"two\": { \"num\": 2 }" + ", \"three\": { \"num\": 3 }" + "}";
    String xml = "<obj><obj id=\"one\"><val id=\"num\">1</val></obj>" +
        "<obj id=\"two\"><val id=\"num\">2</val></obj><obj id=\"three\"><val id=\"num\">3</val></obj></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipInteger() {
    String json = "{\"a\":123456789,\"b\":-123456789}";
    String xml = "<obj><val id=\"a\">123456789</val><val id=\"b\">-123456789</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipDouble() {
    String json = "{\"a\":-123.456e-789,\"b\":123456789.0}";
    String xml = "<obj><val id=\"a\">-123.456e-789</val><val id=\"b\">123456789.0</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testHelloWorld() {
    String json = "{\n" +
        "   \"hello\": true,\n" +
        "   \"foo\": [\"world\"]\n" +
        "}";
    String xml = "<obj>" +
        "<val id=\"hello\" class=\"bool\">true</val>" +
        "<arr id=\"foo\"><val>world</val></arr>" +
        "</obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testInvalidJsonInput() {
    String json = "{\n"
        + "   \"h\\ello\": true,\n"
        + "   \"foo\": [\"world\"]\n"
        + "}";
    //gson: IOException expected
    String xml = "<obj>" +
        "<val id=\"hello\" class=\"bool\">true</val>" +
        "<arr id=\"foo\"><val>world</val></arr>" +
        "</obj>";
    assertEquals(xml, jsonToXml(json));
  }
  
  public void testNulls() throws Exception {
    try {
      jsonToXml(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testEmptyString() {
    String json = "";
    String xml = "";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testCharacterUnescaping() {
    String json = "[\"a\","
        + "\"a\\\"\","
        + "\"\\\"\","
        + "\":\","
        + "\",\","
        + "\"\\b\","
        + "\"\\f\","
        + "\"\\n\","
        + "\"\\r\","
        + "\"\\t\","
        + "\" \","
        + "\"\\\\\","
        + "\"{\","
        + "\"}\","
        + "\"[\","
        + "\"]\","
        + "\"\\u0000\","
        + "\"\\u0019\","
        + "\"\\u20AC\""
        + "]";
    String xml = "<arr><val>a</val>"+//"a"
        "<val>a\"</val>"+//"a\""
        "<val>\"</val>"+//"\""
        "<val>:</val>"+//":"
        "<val>,</val>"+//","
        "<val>\b</val>"+//"\b"
        "<val>\f</val>"+//"\f"
        "<val>\n</val>"+//"\n"
        "<val>\r</val>"+//"\r"
        "<val>\t</val>"+//"\t"
        "<val> </val>"+//" "
        "<val>\\</val>"+//"\\"
        "<val>{</val>"+//"{"
        "<val>}</val>"+//"}"
        "<val>[</val>"+//"["
        "<val>]</val>"+//"]"
        "<val>\0</val>"+//"\0"
        "<val>\u0019</val>"+//"\u0019"
        "<val>\u20AC</val>"+//"\u20AC"
        "</arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testUnescapingInvalidCharacters() {
    String json = "[\"\\u000g\"]";
    //gson: expected NumberFormatException: \\u000g
    String xml = "<arr><val>\u0000g</val></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testUnescapingTruncatedCharacters() {
    String json = "[\"\\u000";
    //gson: expected IOException = MalformedJsonException: Unterminated escape sequence at line 1 column 5 path $[0]
    String xml = "<arr><val>\u0000</val></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testUnescapingTruncatedSequence() {
    String json = "[\"\\";
    //gson: JsonSyntaxException: com.google.gson.stream.MalformedJsonException: Unterminated escape sequence at line 1 column 4 path $[0]
    String xml = "<arr><val></val></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testIntegersWithFractionalPartSpecified() {
    String json = "[-1,+1.0,1.000]";
    String xml = "<arr><val>-1</val><val>+1.0</val><val>1.000</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testDoubles() {
    String json = "[-0.0,"
        + "1.0,"
        + "1.7976931348623157E308,"
        + "4.9E-324,"
        + "0.0,"
        + "-0.5,"
        + "2.2250738585072014E-308,"
        + "3.141592653589793,"
        + "2.718281828459045]";
    String xml = "<arr><val>-0.0</val>" +
        "<val>1.0</val>" +
        "<val>1.7976931348623157E308</val>" +
        "<val>4.9E-324</val>" +
        "<val>0.0</val>" +
        "<val>-0.5</val>" +
        "<val>2.2250738585072014E-308</val>" +
        "<val>3.141592653589793</val>" +
        "<val>2.718281828459045</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictNonFiniteDoubles() {
    String json = "[NaN]";
    String xml = "<arr><val>NaN</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictQuotedNonFiniteDoubles() {
    String json = "[\"NaN\"]";
    String xml = "<arr><val>NaN</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientNonFiniteDoubles() {
    String json = "[NaN, -Infinity, Infinity]";
    String xml = "<arr><val>NaN</val><val>-Infinity</val><val>Infinity</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientQuotedNonFiniteDoubles() {
    String json = "[\"NaN\", \"-Infinity\", \"Infinity\"]";
    String xml = "<arr><val>NaN</val><val>-Infinity</val><val>Infinity</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictNonFiniteDoublesWithSkipValue() {
    String json = "[NaN]";
    String xml = "<arr><val>NaN</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLongs() {
    String json = "[0,0,0,"
        + "1,1,1,"
        + "-1,-1,-1,"
        + "-9223372036854775808,"
        + "9223372036854775807]";
    String xml = "<arr><val>0</val><val>0</val><val>0</val><val>1</val><val>1</val><val>1</val><val>-1</val><val>-1</val><val>-1</val><val>-9223372036854775808</val><val>9223372036854775807</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testNumberWithOctalPrefix() {
    String json = "[01]";
    String xml = "<arr><val>01</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testBooleans() {
    String json = "[true,false]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"bool\">false</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testPeekingUnquotedStringsPrefixedWithBooleans() {
    String json = "[truey]";
    String xml = "<arr><val>truey</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testMalformedNumbers() {
    assertNotANumber("-");
    assertNotANumber(".");

    // exponent lacks digit
    assertNotANumber("e");
    assertNotANumber("0e");
    assertNotANumber(".e");
    assertNotANumber("0.e");
    assertNotANumber("-.0e");

    // no integer
    assertNotANumber("e1");
    assertNotANumber(".e1");
    assertNotANumber("-e1");

    // trailing characters
    assertNotANumber("1x");
    assertNotANumber("1.1x");
    assertNotANumber("1e1x");
    assertNotANumber("1ex");
    assertNotANumber("1.1ex");
    assertNotANumber("1.1e1x");

    // fraction has no digit
    assertNotANumber("0.");
    assertNotANumber("-0.");
    assertNotANumber("0.e1");
    assertNotANumber("-0.e1");

    // no leading digit
    assertNotANumber(".0");
    assertNotANumber("-.0");
    assertNotANumber(".0e1");
    assertNotANumber("-.0e1");
  }

  private void assertNotANumber(String s) {
    Document doc = Jsoup.parse("["+s+"]", "", jsonParser(false));
    doc.outputSettings().prettyPrint(false);
    assertEquals("<arr><val>"+s+"</val></arr>", doc.html());
    assertEquals(1, doc.childNodeSize());
    assertEquals(s, doc.text());
  }

  public void testPeekingUnquotedStringsPrefixedWithIntegers() {
    String json = "[12.34e5x]";
    String xml = "<arr><val>12.34e5x</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
    xml = "<arr><val class=\"unquoted str\">12.34e5x</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));
  }

  public void testPeekLongMinValue() {
    String json = "[-9223372036854775808]";
    String xml = "<arr><val>-9223372036854775808</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "-9223372036854775808";
    xml = "<val>-9223372036854775808</val>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testPeekLongMaxValue() {
    String json = "[9223372036854775807]";
    String xml = "<arr><val>9223372036854775807</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
    xml = "<arr><val class=\"unquoted num\">9223372036854775807</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));
    json = json.replace("[", "['").replace("]", "']");
    xml = "<arr><val class=\"apos quoted num\">9223372036854775807</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));
  }

  public void testLongLargerThanMaxLongThatWrapsAround() {
    String json = "[22233720368547758070]";
    String xml = "<arr><val>22233720368547758070</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLongLargerThanMinLongThatWrapsAround() {
    String json = "[-22233720368547758070]";
    String xml = "<arr><val>-22233720368547758070</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }
  
  public void testNegativeZero() {
    String json = "[--0]";
    String xml = "<arr><val>--0</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
    xml = "<arr><val class=\"unquoted num\">--0</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));
  }

  /**
   * This test fails because there's no double for 9223372036854775808, and our
   * long parsing uses Double.parseDouble() for fractional values.
   */
  public void testPeekLargerThanLongMaxValue() {
    String json = "[9223372036854775808]";
    String xml = "<arr><val>9223372036854775808</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testPeekLargerThanLongMinValue() {
    String json = "[-9223372036854775809]";
    String xml = "<arr><val>-9223372036854775809</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  /**
   * This test fails because there's no double for 9223372036854775806, and
   * our long parsing uses Double.parseDouble() for fractional values.
   */
  public void testHighPrecisionLong() {
    String json = "[9223372036854775806.000]";
    String xml = "<arr><val>9223372036854775806.000</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testPeekMuchLargerThanLongMinValue() {
    String json = "[-92233720368547758080]";
    String xml = "<arr><val>-92233720368547758080</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testQuotedNumberWithEscape() {
    String json = "[\"12\u00334\"]";
    String xml = "<arr><val>1234</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
    xml = "<arr><val class=\"quot quoted num\">1234</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));
  }

  public void testMixedCaseLiterals() {
    String json = "[True,TruE,False,FALSE,NULL,nulL]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"bool\">true</val>" +
        "<val class=\"bool\">false</val><val class=\"bool\">false</val>" +
        "<val class=\"null\" /><val class=\"null\" /></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testMissingValue() {
    String json = "{\"a\":}";//MalformedJsonException: Expected value at line 1 column 6 path $.a
    String xml = "<obj><val id=\"a\" class=\"null\" /></obj>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testPrematureEndOfInput() {
    String json = "{\"a\":true,";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    //gson: JsonSyntaxException: EOFException: End of input at line 1 column 11 path $.a
    assertEquals(xml, jsonToXml(json));
  }

  static class FakeInputStream extends InputStream {
    byte[] ba;
    int count;
    int pos;

    FakeInputStream (byte[] _ba, int _count) {
      ba = _ba; count = _count;
    }

    @Override public int read () {
      if (pos < count) {
        return ba[pos++];
      }
      return -1;
    }
  }

  public void testPrematurelyClosed() throws IOException {
    final byte[] ba = "{\"a\":[]}".getBytes("UTF-8");

    FakeInputStream is = new FakeInputStream(ba, 0);
    Document doc = Jsoup.parse(is, "UTF-8", "", jsonParser());
    assertEquals("", doc.html());

    is = new FakeInputStream(ba, 1);
    doc = Jsoup.parse(is, "UTF-8", "", jsonParser());
    assertEquals("<obj></obj>", doc.html());

    is = new FakeInputStream(ba, 2);
    doc = Jsoup.parse(is, "UTF-8", "", jsonParser());
    String xml = "<obj>\n <val id=\"\" class=\"null\" />\n</obj>";
    assertEquals(xml, doc.html());

    is = new FakeInputStream(ba, 4);
    doc = Jsoup.parse(is, "UTF-8", "", jsonParser());
    assertEquals(xml.replace("id=\"\"", "id=\"a\""), doc.html());
  }

  public void testNextFailuresDoNotAdvance() {
    String json = "{\"a\":true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testIntegerMismatchFailuresDoNotAdvance() {
    String json = "[1.5]";
    String xml = "<arr><val>1.5</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStringNullIsNotNull() {
    String json = "[\"null\"]";
    String xml = "<arr><val>null</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testNullLiteralIsNotAString() {
    String json = "[null]";
    String xml = "<arr><val class=\"null\" /></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testNameValueSeparator() {
    String json = "{\"a\"=true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));

    json = "{'a'=>true}";
    
    assertEquals(xml, jsonToXml(json));

    json = "{a=>true}";
    
    assertEquals(xml, jsonToXml(json));

    json = "{a:true}";
    
    assertEquals(xml, jsonToXml(json));

    json = "{a true}";
    assertEquals(xml, jsonToXml(json));

    json = "{\"a\" true}";
    assertEquals(xml, jsonToXml(json));

    json = "{a \n true}";
    assertEquals(xml, jsonToXml(json));
    json = "{ \n \t 'a' \n true}";
    assertEquals(xml, jsonToXml(json));
    json = "{ \n \t a \n true}";
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictNameValueSeparatorWithSkipValue() {
    String json = "{\"a\"=true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));

    json = "{\"a\"=>true}";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testCommentsInStringValue() throws Exception {
    String json = "[\"// comment\"]";
    String xml = "<arr><val>// comment</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "{\"a\":\"#someComment\"}";
    xml = "<obj><val id=\"a\">#someComment</val></obj>";
    
    assertEquals(xml, jsonToXml(json));

    json = "{\"#//a\":\"#some //Comment\"}";
    xml = "<obj><val id=\"#//a\">#some //Comment</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictComments () {
    String json = "[// comment \n true]";
    String xml = "<arr><!-- comment --><val class=\"bool\">true</val></arr>";
    assertEquals(xml, jsonToXml(json));

    json = "[# comment \n true]";
    assertEquals(xml, jsonToXml(json));

    json = "[/* comment */ true]";
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientComments() {
    String json = "[// comment \n true]";
    String xml = "<arr><!-- comment --><val class=\"bool\">true</val></arr>";
    assertEquals(xml, jsonToXml(json));

    json = "[# comment \n true]";
    assertEquals(xml, jsonToXml(json));

    json = "[/* comment */ true]";
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictCommentsWithSkipValue() {
    String json = "[// comment \n true]";
    String sameXml = "<arr><!-- comment --><val class=\"bool\">true</val></arr>";
    String xml = sameXml;
    assertEquals(xml, jsonToXml(json));
    final String bigData = "\tbig "+repeat('x', 1234567)+" data ;-) ";
    json = json.replace("comment", bigData);
    xml = xml.replace("comment", bigData);
    assertEquals(xml, jsonToXml(json));

    json = "[# comment \n true]";
    xml = sameXml;
    assertEquals(xml, jsonToXml(json));
    json = json.replace("comment", bigData);
    xml = xml.replace("comment", bigData);
    assertEquals(xml, jsonToXml(json));

    json = "[/* comment */ true]";
    xml = sameXml;
    assertEquals(xml, jsonToXml(json));
    json = json.replace("comment", "\tbig "+repeat('x', 1234567)+" data ;-) ");
    xml = xml.replace("comment", bigData);
    assertEquals(xml, jsonToXml(json));

    assertEquals("<!--comment \t-->", jsonToXml("\t\n\r //comment \t\n\r"));
    assertEquals("<!-- comment -->", jsonToXml("// comment "));
    assertEquals("<!--comment -->", jsonToXml("#comment "));
    assertEquals("<!--comment \t -->", jsonToXml("\t\n\r #comment \t \n\r "));
    assertEquals("<!--comment-->", jsonToXml("/*comment*/"));
    assertEquals("<!--\t\rcomment\t\r-->", jsonToXml("\r\n\t /*\t\rcomment\t\r*/\t \n"));
    assertEquals("<!--"+bigData+"-->", jsonToXml("\r\n\t #"+bigData));
    assertEquals("<!--"+bigData+"-->", jsonToXml("\r\n\t //"+bigData));
  }

  public void testStrictUnquotedNames() {
    String json = "{a:true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
    xml = "<obj><val id=\"a\" class=\"bool\" value=\"true\">true</val></obj>";
    assertEquals(xml, jsonToDetailedXml(json));
  }

  public void testLenientUnquotedNames() {
    String json = "{a:true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictUnquotedNamesWithSkipValue() {
    String json = "{a:true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictSingleQuotedNames() {
    String json = "{'a':true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientSingleQuotedNames() {
    String json = "{'a':true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictSingleQuotedNamesWithSkipValue() {
    String json = "{'a':true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictUnquotedStrings() {
    String json = "[a]";
    String xml = "<arr><val>a</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictUnquotedStringsWithSkipValue() {
    String json = "[a]";
    String xml = "<arr><val>a</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientUnquotedStrings() {
    String json = "[a]";
    String xml = "<arr><val>a</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictSingleQuotedStrings() {
    String json = "['a']";
    String xml = "<arr><val>a</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
    xml = "<arr><val class=\"apos quoted str\">a</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));
  }

  public void testLenientSingleQuotedStrings() {
    String json = "['a']";
    String xml = "<arr><val>a</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictSingleQuotedStringsWithSkipValue() {
    String json = "['a']";
    String xml = "<arr><val>a</val></arr>";
    
    assertEquals(xml, jsonToXml(json));  }

  public void testStrictSemicolonDelimitedArray() {
    String json = "[true;true]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientSemicolonDelimitedArray() {
    String json = "[true;true]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictSemicolonDelimitedArrayWithSkipValue() {
    String json = "[true;true]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[true  ; true]";
    
    assertEquals(xml, jsonToXml(json));

    json = "[true true]";//MalformedJsonException: Unterminated array at line 1 column 8 path $[1]
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictSemicolonDelimitedNameValuePair() {
    String json = "{\"a\":true;\"b\":true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val><val id=\"b\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientSemicolonDelimitedNameValuePair() {
    String json = "{\"a\":true;\"b\":true}";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val><val id=\"b\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictSemicolonDelimitedNameValuePairWithSkipValue() {
    String json = "\uFfFe{\"a\":true;\"b\"  :  true;;;;;;;;;;;;;;;;;;;;,;,;,;};";
    String xml = "<obj><val id=\"a\" class=\"bool\">true</val><val id=\"b\" class=\"bool\">true</val></obj>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictUnnecessaryArraySeparators() {
    String json = "[true,,true]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"null\" /><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[,true]";
    xml = "<arr><val class=\"null\" /><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[true,]";
    xml = "<arr><val class=\"bool\">true</val><val class=\"null\" /></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[,]";
    xml = "<arr><val class=\"null\" /><val class=\"null\" /></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientUnnecessaryArraySeparators() {
    String json = "[true,,true]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"null\" /><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[,true]";
    xml = "<arr><val class=\"null\" /><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[true,]";
    xml = "<arr><val class=\"bool\">true</val><val class=\"null\" /></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[,]";
    xml = "<arr><val class=\"null\" /><val class=\"null\" /></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictUnnecessaryArraySeparatorsWithSkipValue() {
    String json = "[true,,true]";
    String xml = "<arr><val class=\"bool\">true</val><val class=\"null\" /><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[,true]";
    xml = "<arr><val class=\"null\" /><val class=\"bool\">true</val></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[true,]";
    xml = "<arr><val class=\"bool\">true</val><val class=\"null\" /></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[,]";
    xml = "<arr><val class=\"null\" /><val class=\"null\" /></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictMultipleTopLevelValues() {
    String json = "[] []";
    String xml = "<arr></arr><arr></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientMultipleTopLevelValues() {
    String json = "[] true {}";
    String xml = "<arr></arr><val class=\"bool\">true</val><obj></obj>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictMultipleTopLevelValuesWithSkipValue() {
    String json = "[] []";
    String xml = "<arr></arr><arr></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testTopLevelValueTypes() {
    String json = "true";
    String xml = "<val class=\"bool\">true</val>";
    
    assertEquals(xml, jsonToXml(json));

    json = "false";
    xml = "<val class=\"bool\">false</val>";
    
    assertEquals(xml, jsonToXml(json));

    json = "null";
    xml = "<val class=\"null\" />";
    
    assertEquals(xml, jsonToXml(json));

    json = "123 ";
    xml = "<val>123</val>";
    
    assertEquals(xml, jsonToXml(json));

    json = "123.4";
    xml = "<val>123.4</val>";
    
    assertEquals(xml, jsonToXml(json));

    json = "\"a\"";
    xml = "<val>a</val>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testTopLevelValueTypeWithSkipValue() {
    String json = "true";
    String xml = "<val class=\"bool\">true</val>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictNonExecutePrefix() {
    String json = ")]}'\n []";
    String xml = "<arr></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictNonExecutePrefixWithSkipValue() {
    String json = ")]}'\n []";
    String xml = "<arr></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "\t\n )]}'\n []";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientNonExecutePrefix() {
    String json = ")]}'\n []";
    String xml = "<arr></arr>";
    
    assertEquals(xml, jsonToXml(json));

    json = "\n\n\r)]}'\n []";
    
    assertEquals(xml, jsonToXml(json));

    json = "\uFFFE\n\n\r)]}'\n []";
    assertEquals(xml, jsonToXml(json));

    json = "\uFEFF\n\n\r)]}'\n []";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientNonExecutePrefixWithLeadingWhitespace() {
    String json = "\r\n \t)]}'\n []";
    String xml = "<arr></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientPartialNonExecutePrefix() {
    String json = ")]}' []";
    String xml = "<val>)</val>" +
        "<unk pos=\"1\" char=\"]\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"2\" />" +
        "<unk pos=\"2\" char=\"}\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"3\" />" +
        "<val> []</val>";
    assertEquals(xml, jsonToXml(json));

    json = ")]} []";
    xml = "<val>)</val>" +
        "<unk pos=\"1\" char=\"]\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"2\" />" +
        "<unk pos=\"2\" char=\"}\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"3\" /><arr></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testBomIgnoredAsFirstCharacterOfDocument() {
    String json = "\ufEff[]";
    String xml = "<arr></arr>";
    
    assertEquals(xml, jsonToXml(json));
    json = "\uffFe[]";
    xml = "<arr></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testBomForbiddenAsOtherCharacterInDocument() {
    String json = "[\ufeff]";
    String xml = "<arr><val>﻿</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPosition() {
    //gson: Expected value at line 6 column 5 path $[1]
    String json = "[\n\n\n\n\n\"a\",}]";
    String xml = "<arr><val>a</val><val class=\"null\" /></arr><unk pos=\"11\" char=\"]\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"5\" />";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPositionGreaterThanBufferSize() {
    String spaces = repeat(' ', 123456);
    String json = "[\n\n"+spaces+"\n\n\n\"a\",}]";//Expected value at line 6 column 5 path $[1]
    String xml = "<arr><val>a</val><val class=\"null\" /></arr>" +
        "<unk pos=\"123467\" char=\"]\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"5\" />";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPositionOverSlashSlashEndOfLineComment() {
    String json = "\n// foo\n\n//bar\r\n[\"a\",}";//Expected value at line 5 column 6 path $[1]
    String xml = "<!-- foo--><!--bar\r--><arr><val>a</val><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPositionOverHashEndOfLineComment() {
    String json = "\n# foo\n\n#bar\r\n[\"a\",}";//Expected value at line 5 column 6 path $[1]
    String xml = "<!-- foo--><!--bar\r--><arr><val>a</val><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPositionOverCStyleComment() {
    String json = "\n\n/* foo\n*\n*\r\nbar */[\"a\",}";//
    String xml = "<!-- foo\n*\n*\r\nbar --><arr><val>a</val><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPositionOverQuotedString () {
    String json = "[\"foo\nbar\r\nbaz\n\",\n  }";//Expected value at line 5 column 3 path $[1]
    String xml = "<arr><val>foo\nbar\r\nbaz\n</val><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPositionOverUnquotedString() {
    //gson MalformedJsonException: Expected value at line 5 column 2 path $[1]
    String json = "[\n\nabcd\n\n,}";
    String xml = "<arr><val>abcd</val><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithEscapedNewlineCharacter() {
    String json = "[\n\n\"\\\n\n\",}";//Expected value at line 5 column 3 path $[1]
    String xml = "<arr><val>\n\n"+
        "</val><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPositionIsOffsetByBom() {
    String json = "\ufeff[\"a\",}]";//Expected value at line 1 column 6 path $[1]
    String xml = "<arr><val>a</val><val class=\"null\" /></arr>" +
        "<unk pos=\"7\" char=\"]\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"5\" />";
    assertEquals(xml, jsonToXml(json));
  }

  public void testFailWithPositionDeepPath() {
    String json = "[1,{\"a\":[2,3,}";//MalformedJsonException: Expected value at line 1 column 14 path $[1].a[2]
    String xml = "<arr><val>1</val>" +
        "<obj><arr id=\"a\"><val>2</val><val>3</val><val class=\"null\" />" +
        "</arr></obj></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictVeryLongNumber() {
    String json = "[0."+repeat('9', 8192)+"]";
    String xml = json.replace("[", "<arr><val>").replace("]", "</val></arr>");
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientVeryLongNumber() {
    String json = "[0."+repeat('9', 8192)+"]";
    String xml = json.replace("[", "<arr><val>").replace("]", "</val></arr>");
    
    assertEquals(xml, jsonToXml(json));
    xml = xml.replace("<val>", "<val class=\"unquoted num\">");
    assertEquals(xml, jsonToDetailedXml(json));
  }

  public void testVeryLongUnquotedLiteral() {
    String literal = "a" + repeat('b', 3111337) + "c";

    String json = "["+literal+"]";
    String xml = "<arr><val>"+literal+"</val></arr>";

    assertEquals(xml, jsonToXml(json));
  }

  public void testDeeplyNestedArrays() {
    String json = "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]";
    String xml = json.replace("[", "<arr>").replace("]", "</arr>");
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testDeeplyNestedObjects() {
    // Build a JSON document structured like {"a":{"a":{"a":{"a":true}}}}, but 5000 levels deep
    String jarray = "{\"a\":%s}";
    String xarray = "<obj id=\"a\">%s</obj>";
    String json = "true";
    String xml = "<val id=\"a\" class=\"bool\">true</val>";
    for (int i = 0; i < 9000; i++) {
      json = String.format(jarray, json);
      xml = String.format(xarray, xml);
    }
    xml = "<obj>"+xml.substring(12);

    //gson: JsonParseException: Failed parsing JSON source: JsonReader at line 1 column 19457 path $.a.a (StackOverflowError)
    assertEquals(xml, jsonToXml(json));
  }

  public void testStringEndingInSlash() {
    String json = "/";//MalformedJsonException: Expected value at line 1 column 1 path $
    String xml = "<unk pos=\"0\" char=\"/\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"1\" />";
    assertEquals(xml, jsonToXml(json));
  }

  public void testDocumentWithCommentEndingInSlash() {
    String json = "/* foo *//";//MalformedJsonException: Expected value at line 1 column 10 path $
    String xml = "<!-- foo --><unk pos=\"9\" char=\"/\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"1\" />";
    assertEquals(xml, jsonToXml(json));
  }

  public void testStringWithLeadingSlash() {
    String json = "/x";
    String xml = "<unk pos=\"0\" char=\"/\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"1\" /><val>x</val>";
    //gson: JsonSyntaxException: MalformedJsonException: Expected value at line 1 column 1 path $
    assertEquals(xml, jsonToXml(json));
  }

  public void testUnterminatedObject() {
    String json = "{\"a\":\"android\"x";//MalformedJsonException: Unterminated object at line 1 column 16 path $.a
    String xml = "<obj><val id=\"a\">android</val><val id=\"x\" class=\"null\" /></obj>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testVeryLongQuotedString() {
    char[] stringChars = new char[1024 * 125];
    Arrays.fill(stringChars, 'x');
    String string = new String(stringChars);
    String json = "[\"" + string + "\"]";
    String xml = json.replace("[\"", "<arr><val>").replace("\"]", "</val></arr>");
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testVeryLongUnquotedString() {
    char[] stringChars = new char[1024 * 127];
    Arrays.fill(stringChars, 'x');
    String string = new String(stringChars);
    String json = "[" + string + "]";
    String xml = "<arr><val>"+string+"</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testVeryLongUnterminatedString() {
    char[] stringChars = new char[111333];//EOFException: End of input at line 1 column 111335 path $[1]
    Arrays.fill(stringChars, 'x');
    String string = new String(stringChars);
    String json = "[" + string;
    String xml = "<arr><val>"+json.substring(1)+"</val></arr>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipVeryLongUnquotedString() {
    String json = "["+repeat('x', 123456)+"]";
    String xml = json.replace("[", "<arr><val>").replace("]", "</val></arr>");
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipTopLevelUnquotedString() {
    String json = repeat('x', 111111);
    String xml = "<val>"+json+"</val>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipVeryLongQuotedString() {
    String x = repeat('x', 98192);
    String json = "[\""+x+"\"]";
    String xml = "<arr><val>"+x+"</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testSkipTopLevelQuotedString() {
    String json = "\""+repeat('x', 12345)+"\" ";
    String xml = json.replace("\" ", "</val>").replace("\"", "<val>");
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStringAsNumberWithTruncatedExponent() {
    String json = "[123e]";
    String xml = "<arr><val>123e</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStringAsNumberWithDigitAndNonDigitExponent() {
    String json = "[123e4b]";
    String xml = "<arr><val>123e4b</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStringAsNumberWithNonDigitExponent() {
    String json = "[123eb]";
    String xml = "<arr><val>123eb</val></arr>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testEmptyStringName() {
    String json = "{\"\":true}";
    String xml = "<obj><val id=\"\" class=\"bool\">true</val></obj>";
    
    assertEquals(xml, jsonToXml(json));
  }

  public void testStrictExtraCommasInMaps() {
    String json = "{\"a\":\"b\",}";//MalformedJsonException: Expected name at line 1 column 11 path $.a
    String xml = "<obj><val id=\"a\">b</val></obj>";
    assertEquals(xml, jsonToXml(json));
  }

  public void testLenientExtraCommasInMaps() {
    String json = "{\"a\":\"b\",}";
    String xml = "<obj><val id=\"a\">b</val></obj>";
    //gson: JsonSyntaxException: MalformedJsonException: Expected name at line 1 column 11 path $.a
    assertEquals(xml, jsonToXml(json));
  }

  private String repeat(char c, int count) {
    char[] array = new char[count];
    Arrays.fill(array, c);
    return new String(array);
  }

  public void testMalformedDocuments() {
    assertEquals("", jsonToXml(""));
    assertEquals("<obj></obj>", jsonToXml("{]"));
    assertEquals("<obj></obj>", jsonToXml("{,"));
    assertEquals("<obj><obj id=\"\"></obj></obj>", jsonToXml("{{"));
    assertEquals("<obj><obj id=\"\"><obj id=\"\"><obj id=\"\"><obj id=\"\"></obj></obj></obj></obj></obj>", jsonToXml("{{{{{"));
    assertEquals("<obj><obj id=\"\"><obj id=\"\"><obj id=\"\"><obj id=\"\"></obj></obj></obj></obj></obj>", jsonToXml("{{{{{]]]]]"));
    assertEquals("<obj><arr id=\"\"></arr></obj>", jsonToXml("{["));
    assertEquals("<obj><arr id=\"\"><obj><arr id=\"\"><obj><arr id=\"\"></arr></obj></arr></obj></arr></obj>", jsonToXml("{[{[{["));
    assertEquals("<arr><obj><arr id=\"\"><obj><arr id=\"\"><obj><arr id=\"\"></arr></obj></arr></obj></arr></obj></arr>",
        jsonToXml("[{[{[{["));
    assertEquals("<obj><val id=\"\" class=\"null\" /></obj>", jsonToXml("{:"));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\""));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\","));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\":}"));
    assertEquals("<obj><val id=\"name\"></val><val id=\"\" class=\"null\" /></obj>", jsonToXml("{\"name\"::"));
    assertEquals("<obj><val id=\"name\"></val><val id=\"\"></val><val id=\"\" class=\"null\" /></obj>", jsonToXml("{\"name\":::"));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\":,"));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\"=}"));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\"=>}"));
    assertEquals("<obj><val id=\"name\">string</val><val id=\"\" class=\"null\" /></obj>", jsonToXml("{\"name\"=>\"string\":"));
    assertEquals("<obj><val id=\"name\">string</val><val id=\"\" class=\"null\" /></obj>", jsonToXml("{\"name\"=>\"string\"="));
    assertEquals("<obj><val id=\"name\">string</val><val id=\"\" class=\"null\" /></obj>", jsonToXml("{\"name\"=>\"string\"=>"));
    assertEquals("<obj><val id=\"name\">string</val></obj>", jsonToXml("{\"name\"=>\"string\","));
    assertEquals("<obj><val id=\"name\">string</val><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\"=>\"string\",\"name\""));
    assertEquals("<arr></arr>", jsonToXml("[}"));
    assertEquals("<arr><val class=\"null\" /><val class=\"null\" /></arr>", jsonToXml("[,]"));
    assertEquals("<obj></obj>", jsonToXml("{"));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\""));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{\"name\","));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{'name'"));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{'name',"));
    assertEquals("<obj><val id=\"name\" class=\"null\" /></obj>", jsonToXml("{name"));
    assertEquals("<arr></arr>", jsonToXml("["));
    assertEquals("<arr><val>string</val></arr>", jsonToXml("[string"));
    assertEquals("<arr><val>string</val></arr>", jsonToXml("[\"string\""));
    assertEquals("<arr><val>string</val></arr>", jsonToXml("['string'"));
    assertEquals("<arr><val>123</val></arr>", jsonToXml("[123"));
    assertEquals("<arr><val>123</val><val class=\"null\" /></arr>", jsonToXml("[123,"));
    assertEquals("<obj><val id=\"name\">123</val></obj>", jsonToXml("{\"name\":123"));
    assertEquals("<obj><val id=\"name\">123</val></obj>", jsonToXml("{\"name\":123,"));
    assertEquals("<obj><val id=\"name\">string</val></obj>", jsonToXml("{\"name\":\"string\""));
    assertEquals("<obj><val id=\"name\">string</val></obj>", jsonToXml("{\"name\":\"string\","));
    assertEquals("<obj><val id=\"name\">string</val></obj>", jsonToXml("{\"name\":'string'"));
    assertEquals("<obj><val id=\"name\">string</val></obj>", jsonToXml("{\"name\":'string',"));
    assertEquals("<obj><val id=\"name\" class=\"bool\">false</val></obj>", jsonToXml("{\"name\":false"));
    assertEquals("<obj><val id=\"name\" class=\"bool\">false</val></obj>", jsonToXml("{\"name\":false,,"));
  }

  public void testUnterminatedStringFailure() {
    String json = "[\"string";
    String xml = "<arr><val class=\"quot quoted str\">string</val></arr>";
    //gson: JsonSyntaxException: MalformedJsonException: Unterminated string at line 1 column 9 path $[0]
    assertEquals(xml, jsonToDetailedXml(json));

    json = "['string";
    xml = "<arr><val class=\"apos quoted str\">string</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));

    json = "[string";
    xml = "<arr><val class=\"unquoted str\">string</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));

    json = "[null";
    xml = "<arr><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToDetailedXml(json));

    json = "[true";
    xml = "<arr><val class=\"bool\" value=\"true\">true</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));

    json = "[42";
    xml = "<arr><val class=\"unquoted num\">42</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));
    json = "['42";
    xml = "<arr><val class=\"apos quoted num\">42</val></arr>";
    assertEquals(xml, jsonToDetailedXml(json));
  }

  public void testDeepArray () throws Exception {
    String json = repeat('[', 12345)+"42"+repeat(']', 12345);
    String xml = json.replace("[", "<arr>").replace("]", "</arr>").replace("42", "<val>42</val>");
    //gson: JsonParseException: Failed parsing JSON source: JsonReader at line 1 column 7792 path $[0] - StackOverflowError
    assertEquals(xml, jsonToXml(json));

    json = json.replace("42", " ");
    xml = xml.replace("<val>42</val>", "");
    assertEquals(xml, jsonToXml(json));
  }

  public void testJustTextNotJson () throws Exception {
    String json = "This is not a JSON! But... Try it, ok? 42, 1; 2; 'tester' 17\n 95, foo = \"bar\"; true";
    String xml = "<val>This</val><val>is</val><val>not</val><val>a</val><val>JSON!</val>" +
        "<val>But...</val><val>Try</val><val>it</val><val>ok?</val>" +
        "<val>42</val><val>1</val><val>2</val><val>tester</val><val>17</val>" +
        "<val>95</val><val>foo</val>" +
        "<unk pos=\"70\" char=\"=\" scope=\"NONEMPTY_DOCUMENT\" stack=\"1\" tokens=\"17\" />" +
        "<val>bar</val><val class=\"bool\">true</val>";
    assertEquals(xml, jsonToXml(json));
  }


  public void testMetaData () throws Exception {
    String json = "{'a'=-42, b: \"1sorry \", \"c\" : ' +3.14159 \t\n' d=>'http://ya.ru'}";

    JsonTreeBuilder treeBuilder = new JsonTreeBuilder(){
      @Override protected void addMetaDataToValue (Element el, String value, VALUE_CLASS typeClass) {
        assert typeClass != null;
        String t;
        String v = value.trim();
        el.textNodes().get(0).text(v);
        try {
          new BigDecimal(v);
          t = typeClass.num().concat("ber");
        } catch (NumberFormatException ignore) {
          try {
            new URL(v);
            t = typeClass.str().concat(" url");
          } catch(MalformedURLException e){
            t = typeClass.str().concat("ing");
          }
        }

        el.attr(STR_CLASS, t);
      }
    };
    treeBuilder.qclass = true;

    Document doc = Jsoup.parse(json, "baseUri", new Parser(treeBuilder));
    doc.outputSettings().prettyPrint(false);
    assertEquals("<obj><val id=\"a\" class=\"unquoted number\">-42</val>" +
        "<val id=\"b\" class=\"quot quoted string\">1sorry</val>" +
        "<val id=\"c\" class=\"apos quoted number\">+3.14159</val>" +
        "<val id=\"d\" class=\"apos quoted str url\">http://ya.ru</val>"+
        "</obj>", doc.html());

    Elements els = doc.select(".number");
    assertEquals(2, els.size());
    assertEquals("-42 +3.14159", els.text());

    els = doc.select(".string");
    assertEquals(1, els.size());
    assertEquals("1sorry", els.get(0).wholeText());

    els = doc.select(".UNQUOTED");
    assertEquals(1, els.size());
    assertEquals("-42", els.get(0).wholeText());

    els = doc.select(".quoted");
    assertEquals(3, els.size());
    assertEquals("1sorry +3.14159 http://ya.ru", els.text());
  }


  public void testArrNull () {
    assertEquals("<arr>" +
        "<val class=\"null\" />" +
        "<val class=\"null\" />" +
        "<val>1</val>" +
        "<val class=\"null\" />" +
        "<val class=\"null\" />" +
        "</arr>", jsonToXml("[,,1,,]"));

    String xml = "<arr><arr><arr>" +
        "<val class=\"null\" />" +
        "<val class=\"null\" />" +
        "<val class=\"null\" />" +
        "<val class=\"null\" />" +
        "</arr></arr></arr>";
    assertEquals(xml, jsonToXml("[[[,,,]]]"));

    assertEquals(xml, jsonToXml("[[[null,null,null,null]]]"));
  }


  private Document loadDoc (String fileName, Parser parser) throws AssertionError {
    try {
      InputStream is = getClass().getResourceAsStream(fileName);
      try {
        Document doc = Jsoup.parse(is, "UTF-8", "", parser);
        doc.outputSettings().prettyPrint(false);
        assertEquals(2, doc.childNodeSize());
        assertTrue(doc.childNode(0) instanceof Comment);
        assertEquals(13, doc.childNode(1).childNodeSize());
        return doc;
      } finally {
        is.close();
      }
    } catch(IOException e){
      throw new AssertionError("fail", e);
    }
  }


  private long loopIt (Runnable r, int times) {
    long t = System.currentTimeMillis();
    for (int i = 0; i < times; i++) {
      r.run();
    }
    t = System.currentTimeMillis()-t;
    System.out.println("time="+t/1000.);
    return t;
  }

  void gsonVisitor (JsonElement el, List<JsonElement> result) {
    if (el.isJsonPrimitive()) {
      if ("a6a2f2e0-6d5b-11e6-adf3-8c705a50cbf0".equals(el.getAsJsonPrimitive().getAsString())) {
        result.add(el);
      }
    } else if (el.isJsonArray()) {
      for (JsonElement i : el.getAsJsonArray()) {
        gsonVisitor(i, result);
      }
    } else if (el.isJsonObject()) {
      for (Entry<String,JsonElement> kv : el.getAsJsonObject().entrySet()) {
        gsonVisitor(kv.getValue(), result);
      }
    }//else JsonNull
  }

  public void testSpeed () {
    Runnable rg = new Runnable() {
      @Override public void run () {
        try(Reader reader = new InputStreamReader(getClass().getResourceAsStream("/bigdata.json"), "UTF-8")) {
          JsonElement doc = new JsonParser().parse(reader);

          assert doc.getAsJsonObject().size() == 13;

          ArrayList<JsonElement> r = new ArrayList<>();
          gsonVisitor(doc, r);

          assert r.size() == 132;
          assert r.get(r.size()-1).getAsJsonPrimitive().isString();

        } catch(Exception e){
          throw new AssertionError("", e);
        }
      }
    };
    rg.run();

    Runnable rj = new Runnable(){
      @Override public void run () {
        loadDoc("/bigdata.json", jsonParser(false));
      }
    }; 
    rj.run();

    Runnable rx = new Runnable() {
      @Override public void run () {
        loadDoc("/bigdata.xml", Parser.xmlParser());
      }
    };
    rx.run();

    loopIt(rg, 200);
    loopIt(rj, 200);
    loopIt(rx, 200);
    System.out.println("Contest!");

    long tg = loopIt(rg, 1000);
    long tj = loopIt(rj, 1000);
    long tx = loopIt(rx, 1000);

    assert tg < tj;
    assert tj < tx/3 : "tj="+tj+", tx="+tx;
  }


  boolean binarySame (String canonicalXmlFileName, String generatedXmlStr) throws IOException {
    byte[] generatedXml = generatedXmlStr.getBytes("UTF-8");
    int total = 0;

    InputStream is = getClass().getResourceAsStream(canonicalXmlFileName);
    try {
      byte[] buf = new byte[16*1024];

      while (true) {
        int r = is.read(buf);
        if (r < 0) {
          break;//EOF
        }

        for (int i = 0; i < r; i++, total++) {
          if (buf[i] != generatedXml[total]) {
            System.out.println("pos="+total+" expected="+(char)buf[i]+", but="+(char)generatedXml[total]);
            return false;
          }
        }
      }
    } finally {
      is.close();
    }

    boolean sameSize = total == generatedXml.length;
    if (!sameSize) {
      System.out.println("expected size="+total+", but="+generatedXml.length);
    }
    return sameSize;
  }


  public void testBinarySame () throws Exception {
    Document docJson = loadDoc("/bigdata.json", jsonParser(false));
    Document docXml  = loadDoc("/bigdata.xml",  Parser.xmlParser());

    String jsonXml = docJson.html();
    String xmlXml = docXml.html();
    assertEquals(jsonXml, xmlXml);
    assertEquals(docJson.text(), docXml.text());

    assertTrue(binarySame("/bigdata.xml", jsonXml));
    assertTrue(binarySame("/bigdata.xml", xmlXml));
    assertFalse(binarySame("/bigdata.xml", jsonXml+' '));
    assertFalse(binarySame("/bigdata.xml", jsonXml.substring(1)));
    assertFalse(binarySame("/bigdata.xml", ' '+jsonXml.substring(1)));

    Elements els = docJson.select(":containsOwn(a6a2f2e0-6d5b-11e6-adf3-8c705a50cbf0)");
    assertEquals(132, els.size());
    assertEquals("val", els.get(els.size()-1).tagName());
    assertEquals("crowdanki_uuid", els.get(els.size()-1).id());
    assertEquals("val", els.get(els.size()-1).nextElementSibling().tagName());
    assertEquals("css", els.get(els.size()-1).nextElementSibling().id());
    assertEquals(229, docXml.select("#guid").size());
  }


  public void testBad () throws Exception {
    String json = "\uFFFE\n\n)]}'\n true ; [[{;;,,\n\n]]} , [[,]] {] [} [{true=>false; 42 32\n yes='no' \"no\":'yes';n;z:";
    String xml = "<val class=\"bool\">true</val>" +
        "<arr><arr><obj></obj></arr></arr>" +
        "<arr><arr><val class=\"null\" /><val class=\"null\" /></arr></arr>" +
        "<obj></obj>" +
        "<arr></arr>" +
        "<arr><obj><val id=\"true\" class=\"bool\">false</val>" +
        "<val id=\"42\">32</val>" +
        "<val id=\"yes\">no</val>" +
        "<val id=\"no\">yes</val>" +
        "<val id=\"n\" class=\"null\" />" +
        "<val id=\"z\" class=\"null\" /></obj></arr>";
    assertEquals(xml, jsonToXml(json));
  }


  public void testEof () throws Exception {
    String json = "[";//gson: EOFException: End of input at line 1 column 2 path $[0]
    String xml = "<arr></arr>";
    assertEquals(xml, jsonToXml(json));

    json = "{";//gson: EOFException: End of input at line 1 column 2 path $.
    xml = "<obj></obj>";
    assertEquals(xml, jsonToXml(json));

    json = "true";
    xml = "<val class=\"bool\">true</val>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[true";//gson: EOFException: End of input at line 1 column 6 path $[1]
    xml = "<arr><val class=\"bool\">true</val></arr>";
    assertEquals(xml, jsonToXml(json));

    json = "[true,";//gson: EOFException: End of input at line 1 column 7 path $[1]
    xml = "<arr><val class=\"bool\">true</val><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToXml(json));

    json = "str";
    xml = "<val>str</val>";
    
    assertEquals(xml, jsonToXml(json));
    json = "'str";//MalformedJsonException: Unterminated string at line 1 column 5 path $
    assertEquals(xml, jsonToXml(json));
    json = "\"str";//MalformedJsonException: Unterminated string at line 1 column 5 path $
    assertEquals(xml, jsonToXml(json));

    json = "[str";//gson: EOFException: End of input at line 1 column 5 path $[1]
    xml = "<arr><val>str</val></arr>";
    assertEquals(xml, jsonToXml(json));
    json = "['str";//gson: EOFException: End of input at line 1 column 5 path $[1]
    assertEquals(xml, jsonToXml(json));
    json = "[\"str";
    assertEquals(xml, jsonToXml(json));

    json = "42";
    xml = "<val>42</val>";
    
    assertEquals(xml, jsonToXml(json));

    json = "[42";
    xml = "<arr><val>42</val></arr>";
    assertEquals(xml, jsonToXml(json));

    json = "[42;";
    xml = "<arr><val>42</val><val class=\"null\" /></arr>";
    assertEquals(xml, jsonToXml(json));

    json = "{;";//MalformedJsonException: Expected name at line 1 column 2 path $.
    xml = "<obj></obj>";
    assertEquals(xml, jsonToXml(json));

    json = "{foo";//EOFException: End of input at line 1 column 5 path $.foo
    xml = "<obj><val id=\"foo\" class=\"null\" /></obj>";
    assertEquals(xml, jsonToXml(json));

    json = "{'foo";//MalformedJsonException: Unterminated string at line 1 column 6 path $.
    assertEquals(xml, jsonToXml(json));

    json = "{\"foo";//MalformedJsonException: Unterminated string at line 1 column 6 path $.
    assertEquals(xml, jsonToXml(json));

    json = "{foo:";//EOFException: End of input at line 1 column 6 path $.foo
    assertEquals(xml, jsonToXml(json));

    json = "{foo:bar";//EOFException: End of input at line 1 column 9 path $.foo
    xml = "<obj><val id=\"foo\">bar</val></obj>";
    assertEquals(xml, jsonToXml(json));

    json = "{foo\n=>\n'bar";//MalformedJsonException: Unterminated string at line 1 column 10 path $.foo
    assertEquals(xml, jsonToXml(json));

    json = "{foo\n=>\n'bar'\n";
    assertEquals(xml, jsonToXml(json));

    json = "{//comment";//EOFException: End of input at line 1 column 11 path $.
    xml = "<obj><!--comment--></obj>";
    assertEquals(xml, jsonToXml(json));

    json = "{/*comment*/";
    assertEquals(xml, jsonToXml(json));

    json = "[//comment";//EOFException: End of input at line 1 column 11 path $.
    xml = "<arr><!--comment--></arr>";
    assertEquals(xml, jsonToXml(json));

    json = "[/*comment*/";
    assertEquals(xml, jsonToXml(json));

    json = "/*comment*/{";
    xml = "<!--comment--><obj></obj>";
    assertEquals(xml, jsonToXml(json));

    json = "/*comment*";
    xml = "<!--comment*-->";
    assertEquals(xml, jsonToXml(json));
  }

  public void testJavaDocExample () throws Exception {
    Document doc = Jsoup.parse(getClass().getResourceAsStream("/example.json"), "UTF-8", "", jsonParser());
    System.out.println(repeat('#', 100)+doc.html()+repeat('#', 100));
    doc.outputSettings().prettyPrint(false);
    assertEquals("<obj><arr id=\"projects\">" +
        "<obj><val id=\"project_name\" class=\"quot quoted str\">Google Gson</val>" +
        "<val id=\"url\" class=\"quot quoted str\">https://github.com/google/gson</val>" +
        "<val id=\"rating\" class=\"unquoted num\">4.956</val>"+
        "<arr id=\"contributors\"><obj><val id=\"first_name\" class=\"quot quoted str\">Jesse</val>" +
        "<val id=\"last_name\" class=\"quot quoted str\">Wilson</val>" +
        "<val id=\"home_page\" class=\"quot quoted str\">https://medium.com/@swankjesse</val></obj></arr></obj>" +
        "<obj><val id=\"project_name\" class=\"quot quoted str\">jsoup</val>" +
        "<val id=\"url\" class=\"quot quoted str\">https://jsoup.org</val>" +
        "<val id=\"rating\" class=\"unquoted num\">5e10</val>"+
        "<arr id=\"contributors\"><obj><val id=\"first_name\" class=\"quot quoted str\">Jonathan</val>" +
        "<val id=\"last_name\" class=\"quot quoted str\">Hedley</val>" +
        "<val id=\"home_page\" class=\"quot quoted str\">https://jhy.io</val></obj>" +
        "<obj><val id=\"first_name\" class=\"quot quoted str\">Andrej</val><val id=\"last_name\" class=\"quot quoted str\">Fink</val>" +
        "<val id=\"home_page\" class=\"quot quoted str\">https://github.com/magicprinc</val></obj></arr></obj></arr></obj>", doc.html());
    assertEquals("Fink", doc.select("#contributors obj:eq(1) #last_name").text());
  }


  public void testIsNumeric () throws Exception {
    JsonTreeBuilder j = new JsonTreeBuilder();
    assertFalse(j.isNumeric(""));
    assertFalse(j.isNumeric(" \t\n"));
    assertFalse(j.isNumeric(" e"));
    assertFalse(j.isNumeric(" E"));
    assertFalse(j.isNumeric(" ."));
    assertFalse(j.isNumeric(" 1.+"));
    assertFalse(j.isNumeric(" 1.-"));
    assertFalse(j.isNumeric("  1 2 3 e  -+ 1 7 "));
    assertFalse(j.isNumeric("1e77+..2"));
    assertFalse(j.isNumeric("1e77+2"));
    assertFalse(j.isNumeric("1e77+...2"));
    assertFalse(j.isNumeric("177..2"));
    assertFalse(j.isNumeric("177..2"));
    assertFalse(j.isNumeric("177e7...2"));
    assertFalse(j.isNumeric("177e..2"));
    assertFalse(j.isNumeric("1e+77..2"));
    assertFalse(j.isNumeric(" 127.e-10.2"));
    assertFalse(j.isNumeric("+- -+1E-77.2."));
    assertFalse(j.isNumeric("1..0"));
    assertFalse(j.isNumeric("177e.2"));
    assertFalse(j.isNumeric(" +1.1E-1.2\n"));
    assertFalse(j.isNumeric("1ee1"));

    assertTrue(j.isNumeric(" 127.e-10"));
    assertTrue(j.isNumeric(" 127.e + 10"));
    assertTrue(j.isNumeric("  123456789012345678901234567890 "));
    assertTrue(j.isNumeric("  123456789012345678901234567890. "));
    assertTrue(j.isNumeric("  1 2 3 .1 2 3 "));
    assertTrue(j.isNumeric("  1 2 3 e 1 7 "));
    assertTrue(j.isNumeric("  1 2 3 e  + 1 7 "));
    assertTrue(j.isNumeric("  1 2 3 e  - 1 7 "));
    assertTrue(j.isNumeric(""+Math.PI));
    assertTrue(j.isNumeric("+- -+1.E-77"));
    assertTrue(j.isNumeric("+- -+1E-77"));
    assertTrue(j.isNumeric("+- -+1.00E-77"));
    assertTrue(j.isNumeric("177.2e2"));
    assertTrue(j.isNumeric(" +1 . 1 E - 1 2\n"));
  }


  public void testYandexTranslate () throws Exception {
    String json = "{\"code\":200,\"lang\":\"de-en\",\"text\":[\"My Name is Andrey.\"]}";
    String xml = "<obj><val id=\"code\">200</val><val id=\"lang\">de-en</val><arr id=\"text\"><val>My Name is Andrey.</val></arr></obj>";
    
    assertEquals(xml, jsonToXml(json));
    xml = "<obj><val id=\"code\" class=\"unquoted num\">200</val>" +
        "<val id=\"lang\" class=\"quot quoted str\">de-en</val>" +
        "<arr id=\"text\"><val class=\"quot quoted str\">My Name is Andrey.</val></arr></obj>";
    assertEquals(xml, jsonToDetailedXml(json));
  }


  public void testParseFragment () throws Exception {
    Element root = new Element("fake");
    JsonTreeBuilder jtb = new JsonTreeBuilder();
    List<Node> nodes = jtb.parseFragment("[a,b,] 42", root, "foo.bar", new Parser(jtb));
    assertEquals(2, nodes.size());
    assertEquals("ab", ((Element)nodes.get(0)).text());
    assertEquals("42", ((Element)nodes.get(1)).text());
    assertEquals("<val class=\"unquoted str\">\n a\n</val>\n"+
        "<val class=\"unquoted str\">\n b\n</val>\n"+
        "<val class=\"null\" />", ((Element)nodes.get(0)).html());
    assertEquals("42", ((Element)nodes.get(1)).html());
    assertEquals("<val class=\"unquoted num\">\n 42\n</val>", nodes.get(1).outerHtml());
  }


  public void test100 () throws Exception {
    String json = " \r\t\n\f5";

    JsonTreeBuilder treeBuilder = new JsonTreeBuilder();
    Document doc = Jsoup.parse(json, "", new Parser(treeBuilder));
    doc.outputSettings().prettyPrint(false);
    assertEquals("<val class=\"unquoted num\">5</val>", doc.html());

    treeBuilder = new JsonTreeBuilder();
    treeBuilder.initialiseParse(new StringReader(json), "", new Parser(treeBuilder));
    assertEquals(NEXT_TOKEN.UNQUOTED, treeBuilder.nextToken());
    assertEquals("JsonTreeBuilder @ 5 ='5' scope=NONEMPTY_DOCUMENT, name='null', tokens: 1, stack: 1 >>5", treeBuilder.toString());

    assertEquals("<val>\u00ff</val>", jsonToXml("'\\uff"));

    treeBuilder = new JsonTreeBuilder();
    treeBuilder.initialiseParse(new StringReader("z,"+repeat('x', 100)), "", new Parser(treeBuilder));
    assertEquals(NEXT_TOKEN.UNQUOTED, treeBuilder.nextToken());
    assertEquals("JsonTreeBuilder @ 0 ='z' scope=NONEMPTY_DOCUMENT, name='null', tokens: 1, stack: 1 >>z,"+repeat('x', 78)+"...",
        treeBuilder.toString());
  }


  public void testAttrEscape () {
    JsonTreeBuilder j = new JsonTreeBuilder();
    assertEquals(" ", j.attrEscape(' '));
    assertEquals("&quot;", j.attrEscape('"'));
    assertEquals("'", j.attrEscape('\''));
    assertEquals("&#xa;", j.attrEscape('\n'));
    assertEquals("&nbsp;", j.attrEscape('\u00A0'));
    assertEquals("&#x1c;", j.attrEscape('\u001C'));
  }


  public static String jsonToDetailedXml (String json) {
    Document doc = Jsoup.parse(json, "", jsonParser());
    doc.outputSettings().prettyPrint(false);
    return doc.html();
  }
}