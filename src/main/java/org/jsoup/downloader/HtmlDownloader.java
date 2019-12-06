package org.jsoup.downloader;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

public class HtmlDownloader extends Downloader {

    @Override
    void downloadTarget() {

        Set<String> urls = new HashSet<>();
        Elements urlsOnPage = doc.select("a[href]");

        for (Element el : urlsOnPage) {
            String url = el.attr("abs:href");
            if (!urls.contains(url)) {
                urls.add(url);
                downloadHtml(url);
            }
        }
    }

    private void downloadHtml(String url) {

        String name = url.substring(url.lastIndexOf("/") + 1);
        name = name.replace('?', '_');

        if (name.equalsIgnoreCase("") || name == null)
            return;

        System.out.println("Saving: " + name);

        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(new java.io.File(storePath + name + ".html")))) {
            Connection.Response urlResponse = Jsoup.connect(url).ignoreContentType(true).execute();

            out.write(urlResponse.body());

            out.close();

            System.out.println("HTML saved");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
