package org.jsoup;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class SessionTest {

    @Test public void Session() throws IOException {
        Session session = Jsoup.newSession();
        session.ignoreContentType(true).ignoreHttpErrors(true);

        session.connect("https://github.com/jhy").execute();
        String device_id = session.cookie("_device_id");
        session.connect("https://github.com/jhy/jsoup").execute();

        assertEquals(device_id, session.cookie("_device_id"));
    }

    @Test public void SessionSerializable() throws IOException, ClassNotFoundException {
        File file = new File("session.txt");

        Session session = Jsoup.newSession();
        session.ext.put("name", "val");
        session.ignoreContentType(true).ignoreHttpErrors(true);

        ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(file));
        oo.writeObject(session);
        oo.close();

        ObjectInputStream oi = new ObjectInputStream(new FileInputStream(file));
        Object object = oi.readObject();

        assertEquals(session, object);

        if (file.exists()) file.delete();
    }

}
