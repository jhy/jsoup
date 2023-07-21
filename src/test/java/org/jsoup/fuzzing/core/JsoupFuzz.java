package org.jsoup.fuzzing.core;


import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

import java.io.File;
import java.io.IOException;


public class JsoupFuzz {

    @FuzzTest
    void cleanTest(FuzzedDataProvider data) {
        String bodyHtml = data.consumeString(1000);
        String baseUri = data.consumeString(100);

        Safelist sf = new Safelist();
        sf.addTags(data.consumeRemainingAsString());
        Jsoup.clean(bodyHtml, baseUri, sf);
    }


    @FuzzTest
    void cleanWithOutputSettingTest(FuzzedDataProvider data) {
        String bodyHtml = data.consumeString(1000);
        String baseUri = data.consumeString(100);

        Safelist sf = new Safelist();
        sf.addTags(data.consumeString(100));
        Document.OutputSettings os = new Document.OutputSettings();

        Jsoup.clean(bodyHtml, baseUri, sf, os);
    }

    @FuzzTest
    void cleanWithoutBaseUriTest(FuzzedDataProvider data) {
        String bodyHtml = data.consumeString(1000);
        Safelist sf = new Safelist();
        sf.addTags(data.consumeString(100));
        Jsoup.clean(bodyHtml, sf);
    }


    @FuzzTest
    void isValidTest(FuzzedDataProvider data) {
        String bodyHtml = data.consumeString(1000);
        Safelist sf = new Safelist();
        sf.addTags(data.consumeString(100));

        Jsoup.isValid(bodyHtml, sf);
    }


    @FuzzTest
    void parseTest1(FuzzedDataProvider data) throws IOException {

        File file = new File(data.consumeString(100));
        String charsetName = data.consumeString(100);
        String baseUri = data.consumeString(50);

        Jsoup.parse(file, charsetName, baseUri, Parser.htmlParser());
    }

    @FuzzTest
    void parseTest2(FuzzedDataProvider data) {
        Jsoup.parse(data.consumeString(1000));
    }


}
