package org.jsoup.examples;

import java.io.File;
import org.jsoup.downloader.Downloader;
import org.jsoup.downloader.DownloaderFactory;
import org.jsoup.downloader.ImageDownloader;
import org.jsoup.downloader.UrlsDownloader;

public class DepthOneCrawler {
    public static void main(String[] args) {
        String url = "http://en.wikipedia.org/";
        String path = System.getProperty("user.dir") + "/download/";
        File file = new File(path);
        file.mkdir();

        DownloaderFactory df = new DownloaderFactory();

        String ImgStorePath = path;
        Downloader ImgDl = df.create("ImageDownloader");
        ImgDl.download(url, ImgStorePath);

        String UrlStorePath = path + "/urls.txt";
        Downloader UrlDl = df.create("UrlsDownloader");
        UrlDl.download(url, UrlStorePath);

        String htmlStorePath = path;
        Downloader htmlDl = df.create("HtmlDownloader");
        htmlDl.download(url, htmlStorePath);
    }

}
