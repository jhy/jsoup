package org.jsoup.helper;

import org.jsoup.internal.ConstrainableInputStream;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal static utilities for handling data.
 *
 */
public final class DataUtil {
    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*(?:[\"'])?([^\\s,;\"']*)");
    static final String defaultCharset = "UTF-8"; // used if not found in header or meta charset
    private static final int firstReadBufferSize = 1024 * 5;
    static final int bufferSize = 1024 * 32;
    private static final char[] mimeBoundaryChars =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    static final int boundaryLength = 32;

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
        return parseInputStream(new FileInputStream(in), charsetName, baseUri, Parser.htmlParser());
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
        return parseInputStream(in, charsetName, baseUri, Parser.htmlParser());
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
        return parseInputStream(in, charsetName, baseUri, parser);
    }

    /**
     * Writes the input stream to the output stream. Doesn't close them.
     * @param in input stream to read from
     * @param out output stream to write to
     * @throws IOException on IO error
     */
    static void crossStreams(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    static Document parseInputStream(InputStream input, String charsetName, String baseUri, Parser parser) throws IOException  {
        if (input == null) // empty body
            return new Document(baseUri);
        input = ConstrainableInputStream.wrap(input, bufferSize, 0);

        Document doc = null;
        boolean fullyRead = false;

        // read the start of the stream and look for a BOM or meta charset
        input.mark(bufferSize);
        ByteBuffer firstBytes = readToByteBuffer(input, firstReadBufferSize - 1); // -1 because we read one more to see if completed. First read is < buffer size, so can't be invalid.
        fullyRead = input.read() == -1;
        input.reset();

        // look for BOM - overrides any other header or input
        BomCharset bomCharset = detectCharsetFromBom(firstBytes);
        if (bomCharset != null) {
            charsetName = bomCharset.charset;
            input.skip(bomCharset.offset);
        }

        if (charsetName == null) { // determine from meta. safe first parse as UTF-8
            String docData = Charset.forName(defaultCharset).decode(firstBytes).toString();
            doc = parser.parseInput(docData, baseUri);

            // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
            Elements metaElements = doc.select("meta[http-equiv=content-type], meta[charset]");
            String foundCharset = null; // if not found, will keep utf-8 as best attempt
            for (Element meta : metaElements) {
                if (meta.hasAttr("http-equiv"))
                    foundCharset = getCharsetFromContentType(meta.attr("content"));
                if (foundCharset == null && meta.hasAttr("charset"))
                    foundCharset = meta.attr("charset");
                if (foundCharset != null)
                    break;
            }

            // look for <?xml encoding='ISO-8859-1'?>
            if (foundCharset == null && doc.childNodeSize() > 0 && doc.childNode(0) instanceof XmlDeclaration) {
                XmlDeclaration prolog = (XmlDeclaration) doc.childNode(0);
                if (prolog.name().equals("xml"))
                    foundCharset = prolog.attr("encoding");
            }
            foundCharset = validateCharset(foundCharset);
            if (foundCharset != null && !foundCharset.equalsIgnoreCase(defaultCharset)) { // need to re-decode. (case insensitive check here to match how validate works)
                foundCharset = foundCharset.trim().replaceAll("[\"']", "");
                charsetName = foundCharset;
                doc = null;
            } else if (!fullyRead) {
                doc = null;
            }
        } else { // specified by content type header (or by user on file load)
            Validate.notEmpty(charsetName, "Must set charset arg to character set of file to parse. Set to null to attempt to detect from HTML");
        }
        if (doc == null) {
            if (charsetName == null)
                charsetName = defaultCharset;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, charsetName), bufferSize);
            doc = parser.parseInput(reader, baseUri);
            doc.outputSettings().charset(charsetName);
        }
        input.close();
        return doc;
    }

    /**
     * Read the input stream into a byte buffer. To deal with slow input streams, you may interrupt the thread this
     * method is executing on. The data read until being interrupted will be available.
     * @param inStream the input stream to read from
     * @param maxSize the maximum size in bytes to read from the stream. Set to 0 to be unlimited.
     * @return the filled byte buffer
     * @throws IOException if an exception occurs whilst reading from the input stream.
     */
    public static ByteBuffer readToByteBuffer(InputStream inStream, int maxSize) throws IOException {
        Validate.isTrue(maxSize >= 0, "maxSize must be 0 (unlimited) or larger");
        final ConstrainableInputStream input = ConstrainableInputStream.wrap(inStream, bufferSize, maxSize);
        return input.readToByteBuffer(maxSize);
    }

    static ByteBuffer readToByteBuffer(InputStream inStream) throws IOException {
        return readToByteBuffer(inStream, 0);
    }

    static ByteBuffer readFileToByteBuffer(File file) throws IOException {
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            byte[] bytes = new byte[(int) randomAccessFile.length()];
            randomAccessFile.readFully(bytes);
            return ByteBuffer.wrap(bytes);
        } finally {
            if (randomAccessFile != null)
                randomAccessFile.close();
        }
    }

    static ByteBuffer emptyByteBuffer() {
        return ByteBuffer.allocate(0);
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
            charset = charset.replace("charset=", "");
            return validateCharset(charset);
        }
        return null;
    }

    private static String validateCharset(String cs) {
        if (cs == null || cs.length() == 0) return null;
        cs = cs.trim().replaceAll("[\"']", "");
        try {
            if (Charset.isSupported(cs)) return cs;
            cs = cs.toUpperCase(Locale.ENGLISH);
            if (Charset.isSupported(cs)) return cs;
        } catch (IllegalCharsetNameException e) {
            // if our this charset matching fails.... we just take the default
        }
        return null;
    }

    /**
     * Creates a random string, suitable for use as a mime boundary
     */
    static String mimeBoundary() {
        final StringBuilder mime = new StringBuilder(boundaryLength);
        final Random rand = new Random();
        for (int i = 0; i < boundaryLength; i++) {
            mime.append(mimeBoundaryChars[rand.nextInt(mimeBoundaryChars.length)]);
        }
        return mime.toString();
    }

    private static BomCharset detectCharsetFromBom(final ByteBuffer byteData) {
        final Buffer buffer = byteData; // .mark and rewind used to return Buffer, now ByteBuffer, so cast for backward compat
        buffer.mark();
        byte[] bom = new byte[4];
        if (byteData.remaining() >= bom.length) {
            byteData.get(bom);
            buffer.rewind();
        }
        if (bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF || // BE
            bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == 0x00 && bom[3] == 0x00) { // LE
            return new BomCharset("UTF-32", 0); // and I hope it's on your system
        } else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF || // BE
            bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            return new BomCharset("UTF-16", 0); // in all Javas
        } else if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            return new BomCharset("UTF-8", 3); // in all Javas
            // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        }
        return null;
    }

    private static class BomCharset {
        private final String charset;
        private final int offset;

        public BomCharset(String charset, int offset) {
            this.charset = charset;
            this.offset = offset;
        }
    }
}
