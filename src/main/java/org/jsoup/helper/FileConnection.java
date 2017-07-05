/**
 * 
 */
package org.jsoup.helper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.ProtocolConnection;
import org.jsoup.nodes.Document;



/**
 * @author Jay Patel
 *
 */
public class FileConnection implements ProtocolConnection {

	@Override
	public Document load(String fileURL) throws Exception {
		try {
			fileURL = fileURL.replaceAll(" ", "%20");
			
			URI filepath = new URL(fileURL).toURI();
			
			String protocol = filepath.getScheme();
			
			if(Protocols.isEnabled(protocol)){
				File f = new File(filepath);
				Document doc = Jsoup.parse(f, null);
				return doc;
			}else {
				throw new IllegalAccessError("Enable jsoup to handle "+ protocol +" protocol. ");
			}
			
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL ", e);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid URL ", e);
		} catch (Exception e) {
			throw new Exception("Unknown reason ", e);
		}
	}

	

}
