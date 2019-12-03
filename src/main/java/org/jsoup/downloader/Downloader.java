package org.jsoup.downloader;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.io.FileOutputStream;

public abstract class Downloader {

    protected String url;
    protected String userAgent;
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
