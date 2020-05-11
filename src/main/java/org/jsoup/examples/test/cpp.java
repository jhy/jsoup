package org.jsoup.examples.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import javax.print.Doc;
import java.io.IOException;
import java.net.URL;

public class cpp {public static void main(String[] args) throws IOException {
//    final Document doc = Jsoup.parse("<div> )foo</div> \n<div>y foo</div>\n<div>\\)</div>\n<div>1</div>");

//        System.out.println(doc.select("div:matches(" + "1" + ")"));
//    System.out.println(doc.select("div:matches(" + "\\)" + ")"));

    String html = "<div>"
            + "<![if true]>1<![endif]>" // downlevel-revealed
            + "<!--[if true]>2<![endif]-->" // downlevel-hidden
            + "</div>";
    Document document = Jsoup.parse(html);



    String HTML="<html>\n" +
            " <head></head>\n" +
            " <body>\n" +
            "  <a href=\"http://www.baidu.com/xxx\"> 百度首页 </a>\n" +
            " </body>\n" +
            "</html>";
    String baseurl="http://www.baidu.com/";


//    URL url = new URL("http://www.baidu.com");
//    Document doc = Jsoup.parse(url, 5000);
//    Element link = doc.select("a").first();
//    System.out.println(link.toString());
    String HTML2="<a href='/xxx'> 百度首页 </a>";
//   System.out.println(Jsoup.isValid(HTML2, Whitelist.basic()));
   System.out.println(Jsoup.isValid(HTML2, baseurl,Whitelist.basic()));
//    Document doc=Jsoup.parse(HTML2,baseurl);
//    Element link = doc.select("a").first();
//    System.out.println(link.absUrl("href"));
//    doc.select()
//    System.out.println(doc);

//        System.out.println(doc.select("div:matches(" + Pattern.quote("*") + ")"));




//       System.out.println(doc.select("div:matches(" +"" + "\\)"+")"));


}
}
