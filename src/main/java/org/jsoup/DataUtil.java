package org.jsoup;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal static utilities for handling data.
 *
 */
class DataUtil {
    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=([^\\s;]*)");
    private static final String defaultCharset = "UTF-8"; // used if not found in header or meta charset
    private static final int bufferSize = 0x20000; // ~130K.
    
    /**
     * Loads a file to a String.
     * @param in
     * @param charsetName
     * @return
     * @throws IOException
     */
    static Document load(File in, String charsetName, String baseUri) throws IOException {
        InputStream inStream = new FileInputStream(in);
        Document doc = readInputStream(inStream, charsetName, baseUri);
        inStream.close();
        return doc;
    }

    /**
     Fetches a URL and gets as a string.
     @param url
     @param timeoutMillis
     @return
     @throws IOException
     */
    static Document load(URL url, int timeoutMillis) throws IOException {
        String protocol = url.getProtocol();
        Validate.isTrue(protocol.equals("http") || protocol.equals("https"), "Only http & https protocols supported");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(timeoutMillis);
        conn.setReadTimeout(timeoutMillis);
        conn.connect();

        int res = conn.getResponseCode();
        if (res != HttpURLConnection.HTTP_OK)
            throw new IOException(res + " error loading URL " + url.toString());
        
        String contentType = conn.getContentType();
        if (contentType == null || !contentType.startsWith("text/"))
            throw new IOException(String.format("Unhandled content type \"%s\" on URL %s. Must be text/*", 
                    contentType, url.toString()));
        
        InputStream inStream = new BufferedInputStream(conn.getInputStream());
        String charSet = getCharsetFromContentType(contentType); // may be null, readInputStream deals with it

        Document doc = readInputStream(inStream, charSet, url.toExternalForm());
        inStream.close();
        return doc;
    }

    // reads bytes first into a buffer, then decodes with the appropriate charset. done this way to support
    // switching the chartset midstream when a meta http-equiv tag defines the charset.
    private static Document readInputStream(InputStream inStream, String charsetName, String baseUri) throws IOException {
        byte[] buffer = new byte[bufferSize];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(bufferSize);
        int read;
        while(true) {
            read  = inStream.read(buffer);
            if (read == -1) break;
            outStream.write(buffer, 0, read);
        }
        ByteBuffer byteData = ByteBuffer.wrap(outStream.toByteArray());
        
        String docData;
        Document doc = null;
        if (charsetName == null) { // determine from meta. safe parse as UTF-8
            // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
            docData = Charset.forName(defaultCharset).decode(byteData).toString();
            doc = Jsoup.parse(docData, baseUri);
            Element meta = doc.select("meta[http-equiv=content-type], meta[charset]").first();
            if (meta != null) { // if not found, will keep utf-8 as best attempt
                String foundCharset = meta.hasAttr("http-equiv") ? getCharsetFromContentType(meta.attr("content")) : meta.attr("charset");
                if (foundCharset != null && !foundCharset.equals(defaultCharset)) { // need to re-decode
                    charsetName = foundCharset;
                    byteData.rewind();
                    docData = Charset.forName(foundCharset).decode(byteData).toString();
                    doc = null;
                }
            }
        } else { // specified by content type header (or by user on file load)
            docData = Charset.forName(charsetName).decode(byteData).toString();
        }
        if (doc == null) {
            doc = Jsoup.parse(docData, baseUri);
            doc.outputSettings().charset(charsetName);
        }
        return doc;
    }
    
    /**
     * Parse out a charset from a content type header.
     * @param contentType e.g. "text/html; charset=EUC-JP"
     * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
     */
    static String getCharsetFromContentType(String contentType) {
        if (contentType == null) return null;
        
        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            return m.group(1).trim().toUpperCase();
        }
        return null;
    }
    
    
}
