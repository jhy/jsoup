package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NodesTest {
    @Test void before() {
        Document doc = Jsoup.parse("<span>One</span> <span>Two</span> <span>Three</span>");
        Nodes<TextNode> nodes = doc.selectNodes("::text:contains(o)", TextNode.class);
        nodes.before("<wbr>");
        assertEquals("<span><wbr>One</span> <span><wbr>Two</span> <span>Three</span>", doc.body().html());
    }

    @Test void after() {
        Document doc = Jsoup.parse("<span>One</span> <span>Two</span> <span>Three</span>");
        Nodes<TextNode> nodes = doc.selectNodes("::text:contains(o)", TextNode.class);
        nodes.after("<wbr>");
        assertEquals("<span>One<wbr></span> <span>Two<wbr></span> <span>Three</span>", doc.body().html());
    }

    @Test void wrap() {
        Document doc = Jsoup.parse("<span>One</span> <span>Two</span> <span>Three</span>");
        Nodes<TextNode> nodes = doc.selectNodes("::text:contains(o)", TextNode.class);
        nodes.wrap("<b></b>");
        assertEquals("<span><b>One</b></span> <span><b>Two</b></span> <span>Three</span>", doc.body().html());
    }
}
