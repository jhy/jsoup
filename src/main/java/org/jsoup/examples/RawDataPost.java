package org.jsoup.examples;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 *This example demonstrates the use of jsoup to execute a raw http post to an example URL
 *
 * @author Bui Dinh Ngoc aka ngocbd
 */

public class RawDataPost {
    public static void main(String args[]) throws IOException {
        //{"data":{"type":"User","attributes":{"userName":"test","password":"password"}}}
        String payload = "{\"data\":{\"type\":\"User\",\"attributes\":{\"userName\":\"test\",\"password\":\"password\"}}}";
        String result = Jsoup.connect("http://ohayvai.appspot.com/login")
                .dataBinary(payload.getBytes())
                .method(Connection.Method.POST)
                .ignoreHttpErrors(true)
                .execute().body();
        System.out.println(result);
    }


}
