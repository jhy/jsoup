package org.jsoup.examples.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class cpp {public static void main(String[] args) throws IOException {
    final Document doc = Jsoup.parse("<div> )foo</div> \n<div>y foo</div>\n<div>\\)</div>\n<div>1</div>");

//        System.out.println(doc.select("div:matches(" + "1" + ")"));
    System.out.println(doc.select("div:matches(" + "\\)" + ")"));

//        System.out.println(doc.select("div:matches(" + Pattern.quote("*") + ")"));




//       System.out.println(doc.select("div:matches(" +"" + "\\)"+")"));


}
}
