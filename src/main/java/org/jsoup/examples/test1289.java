package org.jsoup.examples;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import java.io.IOException;

public class test1289 {
    public static void main(String[] args) throws IOException{
        String html =
                "<html>\n" +
                        "<head>\n" +
                        "<link href=\"css/test.css\" rel=\"stylesheet\">\n" +
                        "<div id ='t'> foo</div> \n" +
                        "<div id ='tt'>y foo</div>\n" +
                        "<div>x</div>\n" +
                        "<div>1</div>"+
                        "</head>\n" +
                        "<body>\n" +
                        "</body>\n" +
                        "</html>";
        String html2 = "<div> test </div>";
        String html3 = "<div> test2 </div>";

        Document document = Jsoup.parse(html);
        Node root = document.root();
        Document doc2 = Jsoup.parse(html2);
        Document doc3 = Jsoup.parse(html3);
        Node testNode = doc2.root().childNode(0).childNodes().get(1).childNode(0);
        Node testNode2 = doc3.root().childNode(0).childNodes().get(1).childNode(0);
        Node node1 = root.childNode(0).childNode(2).childNode(2);
//        System.out.println(root.childNode(0).childNode(2).childNode(2));
        Elements es =  document.getElementsByTag("div");
//        es.before("<div> )test</div>");
        Element es1 = document.getElementById("t");
        Element es2 = document.getElementById("tt");
        Node testNode3 = testNode.clone();
        es.before(testNode);
//        es1.before(testNode);
//        es1.before(html3);
        System.out.println(document);
//        es2.before(testNode3);
//        System.out.println(document);
//        es1.before() {
//        });

//        System.out.println(es.first());

    }
}
