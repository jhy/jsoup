/**
 * 
 */
package org.jsoup.helper;

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
	

}
