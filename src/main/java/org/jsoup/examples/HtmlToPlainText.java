package org.jsoup.examples;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import java.io.IOException;

/**
 * HTML to plain-text. This example program demonstrates the use of jsoup to convert HTML input to lightly-formatted
 * plain-text. That is divergent from the general goal of jsoup's .text() methods, which is to get clean data from a
 * scrape.
 * <p>
 * Note that this is a fairly simplistic formatter -- for real world use you'll want to embrace and extend.
 * </p>
 * <p>
 * To invoke from the command line, assuming you've downloaded the jsoup jar to your current directory:</p>
 * <p><code>java -cp jsoup.jar org.jsoup.examples.HtmlToPlainText url [selector]</code></p>
 * where <i>url</i> is the URL to fetch, and <i>selector</i> is an optional CSS selector.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class HtmlToPlainText {

    private static final String userAgent = "Mozilla/5.0 (jsoup)";

    private static final int timeout = 5 * 1000;

    public static void main(String... args) throws IOException {
        Validate.isTrue(args.length == 1 || args.length == 2, "usage: java -cp jsoup.jar org.jsoup.examples.HtmlToPlainText url [selector]");
        final String url = args[0];
        final String selector = args.length == 2 ? args[1] : null;
        // fetch the specified URL and parse to a HTML DOM
        Document doc = Jsoup.connect(url).userAgent(userAgent).timeout(timeout).get();
        HtmlToPlainText formatter = new HtmlToPlainText();
        if (selector != null) {
            // get each element that matches the CSS selector
            Elements elements = doc.select(selector);
            for (Element element : elements) {
                // format that element to plain text
                String plainText = formatter.getPlainText(element);
                System.out.println(plainText);
            }
        } else {
            // format the whole doc
            String plainText = formatter.getPlainText(doc);
            System.out.println(plainText);
        }
    }

    /**
     * Format an Element to plain-text
     * @param element the root element to format
     * @return formatted text
     */
    public String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        // walk the DOM, and call .head() and .tail() for each node
        NodeTraversor.traverse(formatter, element);
        return formatter.toString();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private static class FormattingVisitor implements NodeVisitor {

        private static final int maxWidth = 80;

        private int width = 0;

        // holds the accumulated text
        private StringBuilder accum = new StringBuilder();

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode)
                // TextNodes carry all user-readable text in the DOM.
                append(((TextNode) node).text());
            else if (name.equals("li"))
                append("\n * ");
            else if (name.equals("dt"))
                append("  ");
            else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
                append("\n");
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5"))
                append("\n");
            else if (name.equals("a"))
                append(String.format(" <%s>", node.absUrl("href")));
        }

        // appends text to the string builder with a simple word wrap method
        private void append(String text) {
            if (text.startsWith("\n"))
                // reset counter if starts with a newline. only from formats above, not in natural text
                width = 0;
            if (text.equals(" ") && (accum.length() == 0 || StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
                // don't accumulate long runs of empty spaces
                return;
            if (text.length() + width > maxWidth) {
                // won't fit, needs to wrap
                String[] words = text.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    boolean last = i == words.length - 1;
                    if (// insert a space if not the last word
                    !last)
                        word = word + " ";
                    if (word.length() + width > maxWidth) {
                        // wrap and reset counter
                        accum.append("\n").append(word);
                        width = word.length();
                    } else {
                        accum.append(word);
                        width += word.length();
                    }
                }
            } else {
                // fits as is, without need to wrap text
                accum.append(text);
                width += text.length();
            }
        }

        @Override
        public String toString() {
            return accum.toString();
        }
    }
}
