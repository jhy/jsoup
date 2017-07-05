/**
 * 
 */
package org.jsoup.helper;

import java.net.URI;
import java.net.URL;

import org.jsoup.ProtocolConnection;

/**
 * @author Jay Patel
 *
 */
public class Protocols {
	
	public static final String FILE = "file";
	private static boolean isFileProtocolEnable = false; // default 
	

	public static boolean setValue(String protocolName, boolean enable) {
		if(FILE.equalsIgnoreCase(protocolName)){
			isFileProtocolEnable = enable;
			return true;
		}
		return false;
	}

	public static boolean isEnabled(String protocol) {
		if(FILE.equalsIgnoreCase(protocol)){
			return isFileProtocolEnable;
		}
		return false; // if nothing matches return false.
			
	}
	
	public static ProtocolConnection getProtocolConnection(String url) {
		String fileURL = url.replaceAll(" ", "%20");
		URI filepath;
		try {
			filepath = new URL(fileURL).toURI();
			String scheme = filepath.getScheme();
			if(FILE.equalsIgnoreCase(scheme)){
				return new FileConnection();
			}
			return null; 
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		 
	}

}
