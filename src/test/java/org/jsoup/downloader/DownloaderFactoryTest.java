package org.jsoup.downloader;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DownloaderFactoryTest {

    @Test
    public void testCreateUrlDownloader() {
        DownloaderFactory df = new DownloaderFactory();
        assertEquals(true, df.create("UrlsDownloader") instanceof UrlsDownloader);
    }

    @Test
    public void testCreateImageDownloader() {
        DownloaderFactory df = new DownloaderFactory();
        assertEquals(true, df.create("ImageDownloader") instanceof ImageDownloader);
    }

    @Test
    public void testCreateHtmlDownloader() {
        DownloaderFactory df = new DownloaderFactory();
        assertEquals(true, df.create("HtmlDownloader") instanceof HtmlDownloader);
    }

}
