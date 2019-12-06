package org.jsoup.downloader;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class ImageDownloader extends Downloader {

    /**
     * Download all the images in the given URL, store them into given storePath.
     */
    @Override
    public void downloadTarget() {


        Elements imgsOnPage = doc.select("img");

        for (Element imageElement : imgsOnPage) {
            String imageUrl = imageElement.attr("abs:src");

            downloadImage(imageUrl);
        }
    }

    private void downloadImage(String imgURL) {
        String imgName = imgURL.substring(imgURL.lastIndexOf("/") + 1);
        imgName = imgName.replace('?', '_');

        if (imgName.equalsIgnoreCase("") || imgName == null)
            return;

        System.out.println("Saving: " + imgName + ", from " + imgURL);

        try (OutputStream out = new FileOutputStream(storePath  + imgName)) {

            URL urlImage = new URL(imgURL);
            InputStream in = urlImage.openStream();

            byte[] buffer = new byte[4096];
            int n = -1;

            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            out.close();

            System.out.println("Image saved");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
