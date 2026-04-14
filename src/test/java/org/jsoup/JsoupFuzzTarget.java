package org.jsoup;


import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.ValidationException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.*;
import org.jsoup.nodes.Node;
import org.jsoup.select.Selector;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.QueryParser;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;
import static org.junit.jupiter.api.Assertions.fail;

/*
./jazzer \
  --cp=target/classes:target/test-classes:$(mvn dependency:build-classpath | grep -v 'INFO') \
  --target_class=org.jsoup.JsoupFuzzTarget \
  --instrumentation_includes='org.jsoup.**'
* */

public class JsoupFuzzTarget {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {

        try {
            String html = data.consumeString(32768);
            String baseUri = data.consumeString(32768);

            if (!html.isEmpty() && !baseUri.isEmpty()) {
                Document doc = Jsoup.parse(html, baseUri);
                doc.select("a[href]");
                doc.text();
                doc.html();

                Document xmlDoc = Jsoup.parse(html, baseUri, Parser.xmlParser());

                List<Node> fragment = Parser.parseFragment(html, new Element("div"), baseUri);

                Parser parser = Parser.htmlParser();
                parser.setTrackErrors(10);
                Document doc1 = parser.parseInput(html, baseUri);
                List<ParseError> errors = parser.getErrors();

                Document doc3 = parser.parseInput(html, baseUri);
                System.setProperty("jsoup.useRe2j", data.consumeBoolean() ? "true" : "false");

                // .replaceAll("[^a-zA-Z0-9]", ""
                String contextTag = data.consumeString(32768);
                if (!contextTag.isEmpty()) {
                    Element context = new Element(Tag.valueOf(contextTag), baseUri);
                    List<Node> fragment1 = Parser.parseFragment(html, context, baseUri);
                    new Document(baseUri).appendChildren(fragment1);
                }

                // the CSS selector stuffs
                QueryParser.parse(html);

                // other weird cleaner stuff
                Safelist weirdSafelist = new Safelist();

                String malformed = data.consumeRemainingAsString();
                weirdSafelist.addAttributes(malformed);
                Cleaner weirdCleaner = new Cleaner(weirdSafelist);
                weirdCleaner.clean(doc3);

                CharacterReader cr = new CharacterReader(malformed);

                Connection con = HttpConnection.connect("http://example.com");
                con.execute();
                con.response();
                con.data(malformed);

                HtmlTreeBuilder tb = new HtmlTreeBuilder();

            }

        } catch (ValidationException e) {

        } catch (Selector.SelectorParseException e) {

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}