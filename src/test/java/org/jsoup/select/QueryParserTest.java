package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.jsoup.select.EvaluatorDebug.asElement;
import static org.jsoup.select.EvaluatorDebug.sexpr;
import static org.jsoup.select.Selector.SelectorParseException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Selector Query Parser.
 *
 * @author Jonathan Hedley
 */
public class QueryParserTest {
    @Test public void testConsumeSubQuery() {
        Document doc = Jsoup.parse("<html><head>h</head><body>" +
                "<li><strong>l1</strong></li>" +
                "<a><li><strong>l2</strong></li></a>" +
                "<p><strong>yes</strong></p>" +
                "</body></html>");
        assertEquals("l1 yes", doc.body().select(">p>strong,>li>strong").text()); // selecting immediate from body
        assertEquals("l2 yes", doc.select("body>p>strong,body>*>li>strong").text());
        assertEquals("l2 yes", doc.select("body>*>li>strong,body>p>strong").text());
        assertEquals("l2 yes", doc.select("body>p>strong,body>*>li>strong").text());
    }

    @Test public void testImmediateParentRun() {
        String query = "div > p > bold.brass";
        assertEquals("(ImmediateParentRun (Tag 'div')(Tag 'p')(And (Tag 'bold')(Class '.brass')))", sexpr(query));

        /*
        <ImmediateParentRun css="div > p > bold.brass" cost="11">
          <Tag css="div" cost="1"></Tag>
          <Tag css="p" cost="1"></Tag>
          <And css="bold.brass" cost="7">
            <Tag css="bold" cost="1"></Tag>
            <Class css=".brass" cost="6"></Class>
          </And>
        </ImmediateParentRun>
         */
    }

    @Test public void testOrGetsCorrectPrecedence() {
        // tests that a selector "a b, c d, e f" evals to (a AND b) OR (c AND d) OR (e AND f)"
        // top level or, three child ands
        String query = "a b, c d, e f";
        String parsed = sexpr(query);
        assertEquals("(Or (And (Tag 'b')(Ancestor (Tag 'a')))(And (Tag 'd')(Ancestor (Tag 'c')))(And (Tag 'f')(Ancestor (Tag 'e'))))", parsed);

        /*
        <Or css="a b, c d, e f" cost="9">
          <And css="a b" cost="3">
            <Tag css="b" cost="1"></Tag>
            <Parent css="a " cost="2">
              <Tag css="a" cost="1"></Tag>
            </Parent>
          </And>
          <And css="c d" cost="3">
            <Tag css="d" cost="1"></Tag>
            <Parent css="c " cost="2">
              <Tag css="c" cost="1"></Tag>
            </Parent>
          </And>
          <And css="e f" cost="3">
            <Tag css="f" cost="1"></Tag>
            <Parent css="e " cost="2">
              <Tag css="e" cost="1"></Tag>
            </Parent>
          </And>
        </Or>
         */
    }

    @Test public void testParsesMultiCorrectly() {
        String query = ".foo.qux[attr=bar] > ol.bar, ol > li + li";
        String parsed = sexpr(query);
        assertEquals("(Or (And (Tag 'li')(ImmediatePreviousSibling (ImmediateParentRun (Tag 'ol')(Tag 'li'))))(ImmediateParentRun (And (AttributeWithValue '[attr=bar]')(Class '.foo')(Class '.qux'))(And (Tag 'ol')(Class '.bar'))))", parsed);

        /*
        <Or css=".foo.qux[attr=bar] > ol.bar, ol > li + li" cost="31">
          <And css="ol > li + li" cost="7">
            <Tag css="li" cost="1"></Tag>
            <ImmediatePreviousSibling css="ol > li + " cost="6">
              <ImmediateParentRun css="ol > li" cost="4">
                <Tag css="ol" cost="1"></Tag>
                <Tag css="li" cost="1"></Tag>
              </ImmediateParentRun>
            </ImmediatePreviousSibling>
          </And>
          <ImmediateParentRun css=".foo.qux[attr=bar] > ol.bar" cost="24">
            <And css=".foo.qux[attr=bar]" cost="15">
              <AttributeWithValue css="[attr=bar]" cost="3"></AttributeWithValue>
              <Class css=".foo" cost="6"></Class>
              <Class css=".qux" cost="6"></Class>
            </And>
            <And css="ol.bar" cost="7">
              <Tag css="ol" cost="1"></Tag>
              <Class css=".bar" cost="6"></Class>
            </And>
          </ImmediateParentRun>
        </Or>
         */
    }

    @Test void idDescenderClassOrder() {
        // https://github.com/jhy/jsoup/issues/2254
        // '#id .class' cost
        String query = "#id .class";
        String parsed = sexpr(query);
        assertEquals("(And (Class '.class')(Ancestor (Id '#id')))", parsed);

        /*
        <And css="#id .class" cost="22">
         <Class css=".class" cost="6"></Class>
         <Ancestor css="#id " cost="16">
          <Id css="#id" cost="2"></Id>
         </Ancestor>
        </And>
         */
    }


    @Test
    public void exceptionOnUncloseAttribute() {
        Selector.SelectorParseException exception =
            assertThrows(Selector.SelectorParseException.class, () -> QueryParser.parse("section > a[href=\"]"));
        assertEquals(
            "Did not find balanced marker at 'href='",
            exception.getMessage());
    }

    @Test
    public void testParsesSingleQuoteInContains() {
        Selector.SelectorParseException exception =
            assertThrows(Selector.SelectorParseException.class, () -> QueryParser.parse("p:contains(One \" One)"));
        assertEquals("Did not find balanced marker at 'One '",
            exception.getMessage());
    }

    @Test
    public void exceptOnEmptySelector() {
        SelectorParseException exception = assertThrows(SelectorParseException.class, () -> QueryParser.parse(""));
        assertEquals("String must not be empty", exception.getMessage());
    }

    @Test
    public void exceptOnNullSelector() {
        SelectorParseException exception = assertThrows(SelectorParseException.class, () -> QueryParser.parse(null));
        assertEquals("String must not be empty", exception.getMessage());
    }

    @Test
    public void exceptOnUnhandledEvaluator() {
        SelectorParseException exception =
            assertThrows(SelectorParseException.class, () -> QueryParser.parse("div / foo"));
        assertEquals("Could not parse query '/': unexpected token at '/'", exception.getMessage());
    }

    @Test public void okOnSpacesForeAndAft() {
        Evaluator parse = QueryParser.parse(" span div  ");
        assertEquals("span div", parse.toString());
    }

    @Test public void structuralEvaluatorsToString() {
        String q = "a:not(:has(span.foo)) b d > e + f ~ g";
        Evaluator parse = QueryParser.parse(q);
        assertEquals(q, parse.toString());
        String parsed = sexpr(q);
        assertEquals("(And (Tag 'g')(PreviousSibling (And (Tag 'f')(ImmediatePreviousSibling (ImmediateParentRun (And (Tag 'd')(Ancestor (And (Tag 'b')(Ancestor (And (Tag 'a')(Not (Has (And (Tag 'span')(Class '.foo')))))))))(Tag 'e'))))))", parsed);
    }

    @Test public void parsesOrAfterAttribute() {
        // https://github.com/jhy/jsoup/issues/2073
        String q = "#parent [class*=child], .some-other-selector .nested";
        String parsed = sexpr(q);
        assertEquals("(Or (And (AttributeWithValueContaining '[class*=child]')(Ancestor (Id '#parent')))(And (Class '.nested')(Ancestor (Class '.some-other-selector'))))", parsed);

        assertEquals("(Or (Class '.some-other-selector')(And (AttributeWithValueContaining '[class*=child]')(Ancestor (Id '#parent'))))", sexpr("#parent [class*=child], .some-other-selector"));
        assertEquals("(Or (And (Id '#el')(AttributeWithValueContaining '[class*=child]'))(Class '.some-other-selector'))", sexpr("#el[class*=child], .some-other-selector"));
        assertEquals("(Or (And (AttributeWithValueContaining '[class*=child]')(Ancestor (Id '#parent')))(And (Class '.nested')(Ancestor (Class '.some-other-selector'))))", sexpr("#parent [class*=child], .some-other-selector .nested"));
    }
}
