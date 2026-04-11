package org.jsoup.parser;

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

public class CharacterReaderFuzzer {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {

        try {
            String malformed = data.consumeString(32768);
            int n = data.consumeInt();
            // System.out.println(malformed);

            if (!malformed.isEmpty()) {
                CharacterReader cr = new CharacterReader(malformed);
                cr.mark();
                // cr.rewindToMark();
                cr.pos();
                cr.readFully();
                cr.lineNumber(n);
                cr.columnNumber(n);
                cr.posLineCol();
                cr.consumeToAny(malformed.toCharArray());
                cr.rewindToMark();
                cr.consumeData();
                cr.consumeRawData();
                cr.consumeTagName();
                cr.consumeToAnySorted(malformed.toCharArray());
                cr.consumeToEnd();
                cr.consumeLetterSequence();
                cr.consumeLetterThenDigitSequence();
                cr.consumeLetterThenDigitSequence();
                cr.consumeHexSequence();
                cr.consumeDigitSequence();
                cr.matches(malformed);
                cr.matchesIgnoreCase(malformed);
                cr.matchesAny(malformed.toCharArray());
                cr.matchesAnySorted(malformed.toCharArray());
                cr.matchesAsciiAlpha();
                cr.matchConsume(malformed);
                cr.matchConsumeIgnoreCase(malformed);
                cr.containsIgnoreCase(malformed);
            }
        } catch (ValidationException e) {

        }
    }
}
