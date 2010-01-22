package org.jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

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
        char[] buffer = new char[0x20000]; // ~ 130K
        StringBuilder data = new StringBuilder(0x20000);
        InputStream inStream = new FileInputStream(in);
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
