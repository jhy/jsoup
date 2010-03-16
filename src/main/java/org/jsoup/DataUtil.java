package org.jsoup;

import org.apache.commons.lang.Validate;

import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * Internal static utilities for handling data.
 *
 */
class DataUtil {
    
    /**
     * Loads a file to a String.
     * @param in
     * @param charsetName
     * @return
     * @throws IOException
     */
    static String load(File in, String charsetName) throws IOException {        
        InputStream inStream = new FileInputStream(in);
        String data = readInputStream(inStream, charsetName);
        inStream.close();
        return data;
    }

    /**
     Fetches a URL and gets as a string.
     @param url
     @param timeoutMillis
     @return
     @throws IOException
     */
    static String load(URL url, int timeoutMillis) throws IOException {
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
        String charSet = "UTF-8"; // todo[must]: get from content-encoding, or http-equiv (two-pass?)

        String data = readInputStream(inStream, charSet);
        inStream.close();
        return data;
    }

    private static String readInputStream(InputStream inStream, String charsetName) throws IOException {
        char[] buffer = new char[0x20000]; // ~ 130K
        StringBuilder data = new StringBuilder(0x20000);
        Reader inReader = new InputStreamReader(inStream, charsetName);
        int read;
        do {
            read = inReader.read(buffer, 0, buffer.length);
            if (read > 0) {
                data.append(buffer, 0, read);
            }

        } while (read >= 0);

        return data.toString();
    }
    
    
}
