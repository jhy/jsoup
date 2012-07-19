package org.jsoup.helper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

/**
 * Internal static utilities for handling data.
 *
 */
public class DataUtil {
    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");
    static final String defaultCharset = "UTF-8"; // used if not found in header or meta charset
    private static final int bufferSize = 0x20000; // ~130K.

    private DataUtil() {}

    /**
     * Loads a file to a Document.
     * @param in file to load
     * @param charsetName character set of input
     * @param baseUri base URI of document, to resolve relative links against
     * @return Document
     * @throws IOException on IO error
     */
    public static Document load(File in, String charsetName, String baseUri) throws IOException {
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(in);
            ByteBuffer byteData = readToByteBuffer(inStream);
            return parseByteData(byteData, charsetName, baseUri, Parser.htmlParser());
        } finally {
            if (inStream != null)
                inStream.close();
        }
    }

    /**
     * Parses a Document from an input steam.
     * @param in input stream to parse. You will need to close it.
     * @param charsetName character set of input
     * @param baseUri base URI of document, to resolve relative links against
     * @return Document
     * @throws IOException on IO error
     */
    public static Document load(InputStream in, String charsetName, String baseUri) throws IOException {
        ByteBuffer byteData = readToByteBuffer(in);
        return parseByteData(byteData, charsetName, baseUri, Parser.htmlParser());
    }

    /**
     * Parses a Document from an input steam, using the provided Parser.
     * @param in input stream to parse. You will need to close it.
     * @param charsetName character set of input
     * @param baseUri base URI of document, to resolve relative links against
     * @param parser alternate {@link Parser#xmlParser() parser} to use.
     * @return Document
     * @throws IOException on IO error
     */
    public static Document load(InputStream in, String charsetName, String baseUri, Parser parser) throws IOException {
        ByteBuffer byteData = readToByteBuffer(in);
        return parseByteData(byteData, charsetName, baseUri, parser);
    }

    // reads bytes first into a buffer, then decodes with the appropriate charset. done this way to support
    // switching the chartset midstream when a meta http-equiv tag defines the charset.
    static Document parseByteData(ByteBuffer byteData, String charsetName, String baseUri, Parser parser) {
        String docData;
        Document doc = null;
        if (charsetName == null) { // determine from meta. safe parse as UTF-8
            // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
            docData = Charset.forName(defaultCharset).decode(byteData).toString();
            doc = parser.parseInput(docData, baseUri);
            Element meta = doc.select("meta[http-equiv=content-type], meta[charset]").first();
            if (meta != null) { // if not found, will keep utf-8 as best attempt
                String foundCharset = meta.hasAttr("http-equiv") ? getCharsetFromContentType(meta.attr("content")) : meta.attr("charset");
                if (foundCharset != null && foundCharset.length() != 0 && !foundCharset.equals(defaultCharset)) { // need to re-decode
                    charsetName = foundCharset;
                    byteData.rewind();
                    docData = Charset.forName(foundCharset).decode(byteData).toString();
                    doc = null;
                }
            }
        } else { // specified by content type header (or by user on file load)
            Validate.notEmpty(charsetName, "Must set charset arg to character set of file to parse. Set to null to attempt to detect from HTML");
            docData = Charset.forName(charsetName).decode(byteData).toString();
        }
        if (doc == null) {
            // there are times where there is a spurious byte-order-mark at the start of the text. Shouldn't be present
            // in utf-8. If after decoding, there is a BOM, strip it; otherwise will cause the parser to go straight
            // into head mode
            if (docData.charAt(0) == 65279)
                docData = docData.substring(1);

            doc = parser.parseInput(docData, baseUri);
            doc.outputSettings().charset(charsetName);
        }
        return doc;
    }

    static ByteBuffer readToByteBuffer(InputStream inStream) throws IOException {
        byte[] buffer = new byte[bufferSize];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(bufferSize);
        int read;
        while(true) {
            read  = inStream.read(buffer);
            if (read == -1) break;
            outStream.write(buffer, 0, read);
        }
        ByteBuffer byteData = ByteBuffer.wrap(outStream.toByteArray());
        return byteData;
    }

    /**
     * Parse out a charset from a content type header. If the charset is not supported, returns null (so the default
     * will kick in.)
     * @param contentType e.g. "text/html; charset=EUC-JP"
     * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
     */
    static String getCharsetFromContentType(String contentType) {
        if (contentType == null) return null;
        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            String charset = m.group(1).trim();
            if (Charset.isSupported(charset)) return charset;
            charset = charset.toUpperCase(Locale.ENGLISH);
            if (Charset.isSupported(charset)) return charset;
        }
        return null;
    }
    
    
}
