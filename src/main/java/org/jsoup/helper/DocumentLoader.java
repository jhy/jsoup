package org.jsoup.helper;

import org.jsoup.internal.Normalizer;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class DocumentLoader {
    /**
     * Loads and parses a file to a Document, with the HtmlParser. Files that are compressed with gzip (and end in {@code .gz} or {@code .z})
     * are supported in addition to uncompressed files.
     *
     * @param file        file to load
     * @param charsetName (optional) character set of input; specify {@code null} to attempt to autodetect. A BOM in
     *                    the file will always override this setting.
     * @param baseUri     base URI of document, to resolve relative links against
     * @return Document
     * @throws IOException on IO error
     */
    public static Document load(File file, @Nullable String charsetName, String baseUri) throws IOException {
        return load(file, charsetName, baseUri, Parser.htmlParser());
    }

    /**
     * Loads and parses a file to a Document. Files that are compressed with gzip (and end in {@code .gz} or {@code .z})
     * are supported in addition to uncompressed files.
     *
     * @param file        file to load
     * @param charsetName (optional) character set of input; specify {@code null} to attempt to autodetect. A BOM in
     *                    the file will always override this setting.
     * @param baseUri     base URI of document, to resolve relative links against
     * @param parser      alternate {@link Parser#xmlParser() parser} to use.
     * @return Document
     * @throws IOException on IO error
     * @since 1.14.2
     */
    public static Document load(File file, @Nullable String charsetName, String baseUri, Parser parser) throws IOException {
        InputStream stream = new FileInputStream(file);
        String name = Normalizer.lowerCase(file.getName());
        if (name.endsWith(".gz") || name.endsWith(".z")) {
            // unfortunately file input streams don't support marks (why not?), so we will close and reopen after read
            boolean zipped;
            try {
                zipped = (stream.read() == 0x1f && stream.read() == 0x8b); // gzip magic bytes
            } finally {
                stream.close();

            }
            stream = zipped ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file);
        }
        return DataUtil.parseInputStream(stream, charsetName, baseUri, parser);
    }

    /**
     * Parses a Document from an input steam.
     *
     * @param in          input stream to parse. The stream will be closed after reading.
     * @param charsetName character set of input (optional)
     * @param baseUri     base URI of document, to resolve relative links against
     * @return Document
     * @throws IOException on IO error
     */
    public static Document load(InputStream in, @Nullable String charsetName, String baseUri) throws IOException {
        return DataUtil.parseInputStream(in, charsetName, baseUri, Parser.htmlParser());
    }

    /**
     * Parses a Document from an input steam, using the provided Parser.
     *
     * @param in          input stream to parse. The stream will be closed after reading.
     * @param charsetName character set of input (optional)
     * @param baseUri     base URI of document, to resolve relative links against
     * @param parser      alternate {@link Parser#xmlParser() parser} to use.
     * @return Document
     * @throws IOException on IO error
     */
    public static Document load(InputStream in, @Nullable String charsetName, String baseUri, Parser parser) throws IOException {
        return DataUtil.parseInputStream(in, charsetName, baseUri, parser);
    }
}