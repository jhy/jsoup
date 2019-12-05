package org.jsoup.downloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public abstract class Downloader {

    protected String url;
    protected String storePath;
    protected Document doc;

    public final void download(String url, String storePath) {
        initial(url, storePath);
        connect();
        downloadTarget();
    }

    private void initial(String url, String storePath) {
        // todo: check if the given url is valid
        this.url = url;

        if (storePath.substring(storePath.length() - 1) != "/")
            this.storePath = storePath + "/";
        else
            this.storePath = storePath;
    }

    private void connect() {
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract void downloadTarget();
}
