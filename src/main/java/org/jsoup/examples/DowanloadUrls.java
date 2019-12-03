package org.jsoup.examples;

import org.jsoup.downloader.Downloader;
import org.jsoup.downloader.UrlsDownloader;

public class DowanloadUrls {
    public static void main(String[] args) {
        String url = "http://en.wikipedia.org/";
        String path = "C:\\Users\\Guang\\OneDrive\\Desktop\\download-test.txt";

        Downloader dl = new UrlsDownloader();
        dl.download(url, path);
    }
}
