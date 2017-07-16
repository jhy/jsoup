/**
 * 
 */
package org.jsoup.helper;

import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.ProtocolConnection;

/**
 * @author 
 *
 */
public class ProtocolHelper {

	public static ProtocolConnection getHandler(String url) throws MalformedURLException {
		URL urlObj = new URL(url);
    	ProtocolConnection pc = null; 
    	switch (urlObj.getProtocol().toLowerCase()) {
		case "file":
			pc = new FileProtocolConnection(urlObj);
			break;

		default:
			break;
		};
		return pc;		
	}
	
	

}
