package org.jsoup.helper;

import org.jsoup.UncheckedIOException;
import org.jsoup.internal.ConstrainableInputStream;
import org.jsoup.internal.Normalizer;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Internal static utilities for handling data.
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
public final class DataUtil {

    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*(?:[\"'])?([^\\s,;\"']*)");

    // Don't use StandardCharsets, as those only appear in Android API 19, and we target 10.
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    // used if not found in header or meta charset
    static final String defaultCharsetName = UTF_8.name();

    private static final int firstReadBufferSize = 1024 * 5;

    static final int bufferSize = 1024 * 32;

    private static final char[] mimeBoundaryChars = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    static final int boundaryLength = 32;

    private DataUtil() {
    }

    /**
     * Loads and parses a file to a Document, with the HtmlParser. Files that are compressed with gzip (and end in {@code .gz} or {@code .z})
     * are supported in addition to uncompressed files.
     *
     * @param file file to load
     * @param charsetName (optional) character set of input; specify {@code null} to attempt to autodetect. A BOM in
     *     the file will always override this setting.
     * @param baseUri base URI of document, to resolve relative links against
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
     * @param file file to load
     * @param charsetName (optional) character set of input; specify {@code null} to attempt to autodetect. A BOM in
     *     the file will always override this setting.
     * @param baseUri base URI of document, to resolve relative links against
     * @param parser alternate {@link Parser#xmlParser() parser} to use.
     *
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
                // gzip magic bytes
                zipped = (stream.read() == 0x1f && stream.read() == 0x8b);
            } finally {
                stream.close();
            }
            stream = zipped ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file);
        }
        return parseInputStream(stream, charsetName, baseUri, parser);
    }

    /**
     * Parses a Document from an input steam.
     * @param in input stream to parse. The stream will be closed after reading.
     * @param charsetName character set of input (optional)
     * @param baseUri base URI of document, to resolve relative links against
     * @return Document
     * @throws IOException on IO error
     */
    public static Document load(@WillClose InputStream in, @Nullable String charsetName, String baseUri) throws IOException {
        return parseInputStream(in, charsetName, baseUri, Parser.htmlParser());
    }

    /**
     * Parses a Document from an input steam, using the provided Parser.
     * @param in input stream to parse. The stream will be closed after reading.
     * @param charsetName character set of input (optional)
     * @param baseUri base URI of document, to resolve relative links against
     * @param parser alternate {@link Parser#xmlParser() parser} to use.
     * @return Document
     * @throws IOException on IO error
     */
    public static Document load(@WillClose InputStream in, @Nullable String charsetName, String baseUri, Parser parser) throws IOException {
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

    static Document parseInputStream(@Nullable @WillClose InputStream input, @Nullable String charsetName, String baseUri, Parser parser) throws IOException {
        if (// empty body
        input == null)
            return new Document(baseUri);
        input = ConstrainableInputStream.wrap(input, bufferSize, 0);
        @Nullable
        Document doc = null;
        // read the start of the stream and look for a BOM or meta charset
        try {
            input.mark(bufferSize);
            // -1 because we read one more to see if completed. First read is < buffer size, so can't be invalid.
            ByteBuffer firstBytes = readToByteBuffer(input, firstReadBufferSize - 1);
            boolean fullyRead = (input.read() == -1);
            input.reset();
            // look for BOM - overrides any other header or input
            BomCharset bomCharset = detectCharsetFromBom(firstBytes);
            if (bomCharset != null)
                charsetName = bomCharset.charset;
            if (charsetName == null) {
                // determine from meta. safe first parse as UTF-8
                try {
                    CharBuffer defaultDecoded = UTF_8.decode(firstBytes);
                    if (defaultDecoded.hasArray())
                        doc = parser.parseInput(new CharArrayReader(defaultDecoded.array(), defaultDecoded.arrayOffset(), defaultDecoded.limit()), baseUri);
                    else
                        doc = parser.parseInput(defaultDecoded.toString(), baseUri);
                } catch (UncheckedIOException e) {
                    throw e.ioException();
                }
                // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
                Elements metaElements = doc.select("meta[http-equiv=content-type], meta[charset]");
                // if not found, will keep utf-8 as best attempt
                String foundCharset = null;
                for (Element meta : metaElements) {
                    if (meta.hasAttr("http-equiv"))
                        foundCharset = getCharsetFromContentType(meta.attr("content"));
                    if (foundCharset == null && meta.hasAttr("charset"))
                        foundCharset = meta.attr("charset");
                    if (foundCharset != null)
                        break;
                }
                // look for <?xml encoding='ISO-8859-1'?>
                if (foundCharset == null && doc.childNodeSize() > 0) {
                    Node first = doc.childNode(0);
                    XmlDeclaration decl = null;
                    if (first instanceof XmlDeclaration)
                        decl = (XmlDeclaration) first;
                    else if (first instanceof Comment) {
                        Comment comment = (Comment) first;
                        if (comment.isXmlDeclaration())
                            decl = comment.asXmlDeclaration();
                    }
                    if (decl != null && decl.name().equalsIgnoreCase("xml"))
                        foundCharset = decl.attr("encoding");
                }
                foundCharset = validateCharset(foundCharset);
                if (foundCharset != null && !foundCharset.equalsIgnoreCase(defaultCharsetName)) {
                    // need to re-decode. (case insensitive check here to match how validate works)
                    foundCharset = foundCharset.trim().replaceAll("[\"']", "");
                    charsetName = foundCharset;
                    doc = null;
                } else if (!fullyRead) {
                    doc = null;
                }
            } else {
                // specified by content type header (or by user on file load)
                Validate.notEmpty(charsetName, "Must set charset arg to character set of file to parse. Set to null to attempt to detect from HTML");
            }
            if (doc == null) {
                if (charsetName == null)
                    charsetName = defaultCharsetName;
                // Android level does not allow us try-with-resources
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName(charsetName)), bufferSize);
                try {
                    if (bomCharset != null && bomCharset.offset) {
                        // creating the buffered reader ignores the input pos, so must skip here
                        long skipped = reader.skip(1);
                        // WTF if this fails.
                        Validate.isTrue(skipped == 1);
                    }
                    try {
                        doc = parser.parseInput(reader, baseUri);
                    } catch (UncheckedIOException e) {
                        // io exception when parsing (not seen before because reading the stream as we go)
                        throw e.ioException();
                    }
                    Charset charset = charsetName.equals(defaultCharsetName) ? UTF_8 : Charset.forName(charsetName);
                    doc.outputSettings().charset(charset);
                    if (!charset.canEncode()) {
                        // some charsets can read but not encode; switch to an encodable charset and update the meta el
                        doc.charset(UTF_8);
                    }
                } finally {
                    reader.close();
                }
            }
        } finally {
            input.close();
        }
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

    static ByteBuffer emptyByteBuffer() {
        return ByteBuffer.allocate(0);
    }

    /**
     * Parse out a charset from a content type header. If the charset is not supported, returns null (so the default
     * will kick in.)
     * @param contentType e.g. "text/html; charset=EUC-JP"
     * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
     */
    @Nullable
    static String getCharsetFromContentType(@Nullable String contentType) {
        if (contentType == null)
            return null;
        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            String charset = m.group(1).trim();
            charset = charset.replace("charset=", "");
            return validateCharset(charset);
        }
        return null;
    }

    @Nullable
    private static String validateCharset(@Nullable String cs) {
        if (cs == null || cs.length() == 0)
            return null;
        cs = cs.trim().replaceAll("[\"']", "");
        try {
            if (Charset.isSupported(cs))
                return cs;
            cs = cs.toUpperCase(Locale.ENGLISH);
            if (Charset.isSupported(cs))
                return cs;
        } catch (IllegalCharsetNameException e) {
            // if our this charset matching fails.... we just take the default
        }
        return null;
    }

    /**
     * Creates a random string, suitable for use as a mime boundary
     */
    static String mimeBoundary() {
        final StringBuilder mime = StringUtil.borrowBuilder();
        final Random rand = new Random();
        for (int i = 0; i < boundaryLength; i++) {
            mime.append(mimeBoundaryChars[rand.nextInt(mimeBoundaryChars.length)]);
        }
        return StringUtil.releaseBuilder(mime);
    }

    @Nullable
    private static BomCharset detectCharsetFromBom(final ByteBuffer byteData) {
        // .mark and rewind used to return Buffer, now ByteBuffer, so cast for backward compat
        @SuppressWarnings("UnnecessaryLocalVariable")
        final Buffer buffer = byteData;
        buffer.mark();
        byte[] bom = new byte[4];
        if (byteData.remaining() >= bom.length) {
            byteData.get(bom);
            buffer.rewind();
        }
        if (// BE
        bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF || bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == 0x00 && bom[3] == 0x00) {
            // LE
            // and I hope it's on your system
            return new BomCharset("UTF-32", false);
        } else if (// BE
        bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF || bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            // in all Javas
            return new BomCharset("UTF-16", false);
        } else if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            // in all Javas
            return new BomCharset("UTF-8", true);
            // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        }
        return null;
    }

    private static class BomCharset {

        private final String charset;

        private final boolean offset;

        public BomCharset(String charset, boolean offset) {
            this.charset = charset;
            this.offset = offset;
        }
    }
}
