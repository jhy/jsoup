package org.jsoup.downloader;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DownloaderTest {

	String testUrl = "https://raw.githubusercontent.com/JsoupMaster/jsoup/master/src/main/javadoc/overview.html";
	String storePath = System.getProperty("user.dir") + "/download/";

	@Test
	public void testUrlsDownloader() {
        File file = new File(storePath);
        file.mkdir();

		Downloader downloader = new UrlsDownloader();
		downloader.download(testUrl, storePath  + "urls.txt");

		try {
			InputStream in = new FileInputStream(storePath  + "urls.txt");
			try {
				String urls = new String(in.readAllBytes());
				assertTrue(urls.equalsIgnoreCase(
						"http://whatwg.org/html\n" +
						"http://jonathanhedley.com/\n" +
						"https://jsoup.org/\n"));

			} catch (IOException e) {
				fail("file is not readable.");
			}
		} catch (FileNotFoundException e) {
			fail("file is not found.");
		}

		file = new File(storePath  + "urls.txt");
		file.delete();

        file = new File(storePath);
		file.delete();
	}
}
