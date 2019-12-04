package org.jsoup.downloader;

public class DownloaderFactory {

    public Downloader create(String name) {
        if (name == "ImageDownloader") {
            return new ImageDownloader();
        } else if (name == "UrlsDownloader") {
            return new UrlsDownloader();
        } else {
            return null;
        }
    }
}
