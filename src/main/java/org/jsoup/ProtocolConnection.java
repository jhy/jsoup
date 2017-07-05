/**
 * 
 */
package org.jsoup;

import org.jsoup.nodes.Document;

/**
 * @author Jay Patel
 *
 */
public interface ProtocolConnection {
	
	public Document load(String fileURL) throws Exception;

}
