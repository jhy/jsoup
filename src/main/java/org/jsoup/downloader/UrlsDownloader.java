package org.jsoup.downloader;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class UrlsDownloader extends Downloader{

    /**
     * Download all the images in the given URL, store them into given storePath.
     */
    @Override
    public void downloadTarget() {
        Set<String> urls = new HashSet<>();
        Elements urlsOnPage = doc.select("a[href]");

        for (Element el : urlsOnPage) {
            String url = el.attr("abs:href");
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }

        try (FileOutputStream out = new FileOutputStream(storePath)){
            for (String url : urls) {
                out.write((url + "\n").getBytes() );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
