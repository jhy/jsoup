package org.jsoup.examples;

import org.jsoup.Jsoup;
import org.jsoup.helper.Protocols;
import org.jsoup.nodes.Document;

public class FileProtocolLoad {

	public static void main(String[] args) throws Exception {
		
		String fileURL = "file:///C:/Users/Jay Patel/Desktop/sample.html";
		
		 Document doc = Jsoup.allowProtocol(Protocols.FILE, true).load(fileURL);
		 System.out.println(doc.toString());
		
		// type 2
		//Jsoup.allowProtocol(Protocols.FILE, true);
		//Jsoup.allowProtocol(Protocols.FILE, true);
		//Document doc = Jsoup.load(fileURL);
		
		System.out.println(doc.toString());
		
		
		
	}

}
