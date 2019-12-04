package org.jsoup.examples;

import org.jsoup.downloader.Downloader;
import org.jsoup.downloader.DownloaderFactory;
import org.jsoup.downloader.ImageDownloader;
import org.jsoup.downloader.UrlsDownloader;

public class DownloaderTest {
    public static void main(String[] args) {
        String url = "http://en.wikipedia.org/";

        DownloaderFactory df = new DownloaderFactory();

        String ImgStorePath = "/Users/zgy/Downloads/imageDownloaderTEst/";
        Downloader ImgDl = df.create("ImageDownloader");
        ImgDl.download(url, ImgStorePath);


        String UrlStorePath = "/Users/zgy/Downloads/imageDownloaderTEst/urls.txt";
        Downloader UrlDl = df.create("UrlsDownloader");
        UrlDl.download(url, UrlStorePath);

        String htmlStorePath = "/Users/zgy/Downloads/imageDownloaderTEst/";
        Downloader htmlDl = df.create("HtmlDownloader");
        htmlDl.download(url, htmlStorePath);


    }

}
