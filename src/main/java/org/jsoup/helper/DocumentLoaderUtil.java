package org.jsoup.helper;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DocumentLoaderUtil {
    public static Document load(File file, String charsetName, String baseUri) throws IOException {
        return DataUtil.load(file, charsetName, baseUri, Parser.htmlParser());
    }

    public static Document load(File file, String charsetName, String baseUri, Parser parser) throws IOException {
        return DataUtil.load(file, charsetName, baseUri, parser);
    }

    public static Document load(InputStream in, String charsetName, String baseUri) throws IOException {
        return DataUtil.load(in, charsetName, baseUri, Parser.htmlParser());
    }

        public static Document load(InputStream in, String charsetName, String baseUri, Parser parser) throws IOException {
        return DataUtil.load(in, charsetName, baseUri, parser);
    }
}
