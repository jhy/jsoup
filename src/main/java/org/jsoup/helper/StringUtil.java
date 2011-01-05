package org.jsoup.helper;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;

/**
 * A minimal String utility class. Designed for interal jsoup use only.
 */
public final class StringUtil {
    // memoised padding up to 10
    private static final String[] padding = {"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ", "         ", "          "};

    private static final String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private static final int lineLength = 76;

    /**
     * Join a collection of strings by a seperator
     * @param strings collection of string objects
     * @param sep string to place between strings
     * @return joined string
     */
    public static String join(Collection<String> strings, String sep) {
        return join(strings.iterator(), sep);
    }

    /**
     * Join a collection of strings by a seperator
     * @param strings iterator of string objects
     * @param sep string to place between strings
     * @return joined string
     */
    public static String join(Iterator<String> strings, String sep) {
        if (!strings.hasNext())
            return "";

        String start = strings.next();
        if (!strings.hasNext()) // only one, avoid builder
            return start;

        StringBuilder sb = new StringBuilder(64).append(start);
        while (strings.hasNext()) {
            sb.append(sep);
            sb.append(strings.next());
        }
        return sb.toString();
    }

    /**
     * Returns space padding
     * @param width amount of padding desired
     * @return string of spaces * width
     */
    public static String padding(int width) {
        if (width < 0)
            throw new IllegalArgumentException("width must be > 0");

        if (width < padding.length)
            return padding[width];

        char[] out = new char[width];
        for (int i = 0; i < width; i++)
            out[i] = ' ';
        return String.valueOf(out);
    }

    /**
     * Tests if a string is blank: null, emtpy, or only whitespace (" ", \r\n, \t, etc)
     * @param string string to test
     * @return if string is blank
     */
    public static boolean isBlank(String string) {
        if (string == null || string.length() == 0)
            return true;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            if (!Character.isWhitespace(string.codePointAt(i)))
                return false;
        }
        return true;
    }

    /**
     * Tests if a string is numeric, i.e. contains only digit characters
     * @param string string to test
     * @return true if only digit chars, false if empty or null or contains non-digit chrs
     */
    public static boolean isNumeric(String string) {
        if (string == null || string.length() == 0)
            return false;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            if (!Character.isDigit(string.codePointAt(i)))
                return false;
        }
        return true;
    }

    public static String normaliseWhitespace(String string) {
        StringBuilder sb = new StringBuilder(string.length());

        boolean lastWasWhite = false;
        boolean modified = false;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            int c = string.codePointAt(i);
            if (Character.isWhitespace(c)) {
                if (lastWasWhite) {
                    modified = true;
                    continue;
                }
                if (c != ' ')
                    modified = true;
                sb.append(' ');
                lastWasWhite = true;
            }
            else {
                sb.appendCodePoint(c);
                lastWasWhite = false;
            }
        }
        return modified ? sb.toString() : string;
    }
    
	public static byte[] zeroPad(int length, byte[] bytes) {
		byte[] padded = new byte[length];
		System.arraycopy(bytes, 0, padded, 0, bytes.length);
		return padded;
	}
	
	public static String encode(String s) {
		StringBuffer sb = new StringBuffer();
		byte[] stringBytes;
		try {
			stringBytes = s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			stringBytes = s.getBytes();
		}
		int padding = (3 - (stringBytes.length % 3)) % 3;
		stringBytes = zeroPad(stringBytes.length + padding, stringBytes);
		for (int i = 0; i < stringBytes.length; i += 3) {
			int j = ((stringBytes[i] & 0xff) << 16) + ((stringBytes[i + 1] & 0xff) << 8) + (stringBytes[i + 2] & 0xff);
			sb.append(base64Chars.charAt((j >> 18) & 0x3f) + base64Chars.charAt((j >> 12) & 0x3f)
					+ base64Chars.charAt((j >> 6) & 0x3f) + base64Chars.charAt(j & 0x3f));
		}
		String encoded = sb.toString();
		return splitLines(encoded.substring(0, encoded.length() - padding) + "==".substring(0, padding));
	}

	private static String splitLines(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i += lineLength) {
			sb.append(s.substring(i, Math.min(s.length(), i + lineLength)));
			sb.append("\r\n");
		}
		return sb.toString();
	}
}
