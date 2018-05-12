package org.jsoup.helper;

import java.io.File;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.ProtocolConnection;
import org.jsoup.nodes.Document;

public class FileProtocolConnection implements ProtocolConnection {
	
	private URL _fileURL ;

	public FileProtocolConnection(URL urlObj) {
		this. _fileURL = urlObj;
	}

	@Override
	public Document get() throws Exception {
		File f = new File(_fileURL.toURI());
		Document doc = Jsoup.parse(f, "UTF-8");
		return doc;
	}

}
