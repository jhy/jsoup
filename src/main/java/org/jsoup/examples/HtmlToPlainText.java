package org.jsoup.examples;

import org.jsoup.Connection;
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
import java.util.HashSet;

/**
 HTML to plain-text. This example program demonstrates the use of jsoup to convert HTML input to lightly-formatted
 plain-text. That is divergent from the general goal of jsoup's .text() methods, which is to get clean data from a
 scrape.
 <p>
 Note that this is a fairly simplistic formatter -- for real world use you'll want to embrace and extend.
 </p>
 <p>
 To invoke from the command line, assuming you've downloaded the jsoup-examples jar to your current directory:</p>
 <p><code>java -jar jsoup-examples.jar url [selector]</code></p>
 where <i>url</i> is the URL to fetch, and <i>selector</i> is an optional CSS selector.
*/
public class HtmlToPlainText {
    private static final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 (jsoup-example)";
    private static final int timeout = 5 * 1000;

    public static void main(String... args) throws IOException {
        Validate.isTrue(args.length == 1 || args.length == 2, "usage: java -jar jsoup-examples.jar url [selector]");
        final String url = args[0];
        final String selector = args.length == 2 ? args[1] : null;

        // fetch the specified URL and parse to a HTML DOM:
        Connection session = Jsoup.newSession() // .newSession creates a session to maintain settings and cookies across multiple requests
            .userAgent(userAgent)
            .timeout(timeout);
        Document doc = session.newRequest(url).get(); // .get executes a GET request, and parses the result

        if (selector != null) {
            Elements elements = doc.select(selector); // get each element that matches the CSS selector
            elements = trimParents(elements); // trim out elements that descend from a previously seen element
            for (Element element : elements) {
                String plainText = getPlainText(element); // format that element to plain text
                System.out.println(plainText);
            }
        } else { // format the whole doc
            String plainText = getPlainText(doc);
            System.out.println(plainText);
        }
    }

    /**
     * Format an Element to plain-text
     * @param element the root element to format
     * @return formatted text
     */
    static String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor.traverse(formatter, element); // walk the DOM, and call .head() and .tail() for each node

        return formatter.toString();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private static class FormattingVisitor implements NodeVisitor {
        private static final int maxWidth = 80;
        private int width = 0;
        private StringBuilder accum = new StringBuilder(); // holds the accumulated text

        // hit when the node is first seen
        @Override
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode)
                append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
            else if (name.equals("li"))
                append("\n * ");
            else if (name.equals("dt"))
                append("  ");
            else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
                append("\n");
        }

        // hit when all of the node's children (if any) have been visited
        @Override
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
                width = 0; // reset counter if starts with a newline. only from formats above, not in natural text
            if (text.equals(" ") &&
                    (accum.length() == 0 || StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
                return; // don't accumulate long runs of empty spaces

            if (text.length() + width > maxWidth) { // won't fit, needs to wrap
                String[] words = text.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    boolean last = i == words.length - 1;
                    if (!last) // insert a space if not the last word
                        word = word + " ";
                    if (word.length() + width > maxWidth) { // wrap and reset counter
                        accum.append("\n").append(word);
                        width = word.length();
                    } else {
                        accum.append(word);
                        width += word.length();
                    }
                }
            } else { // fits as is, without need to wrap text
                accum.append(text);
                width += text.length();
            }
        }

        @Override
        public String toString() {
            return accum.toString();
        }
    }

    static Elements trimParents(final Elements elements) {
        // removes elements from the list if their parent / ancestor is already in the list; prevents redundant output for selectors that match nested elements
        HashSet<Element> seen = new HashSet<>(elements.size());
        Elements trimmed = new Elements();

        EACH: for (Element el: elements) {
            Element current = el;
            while (current.parent() != null) {
                if (seen.contains(current.parent())) {
                    continue EACH;
                }
                current = current.parent();
            }
            seen.add(el);
            trimmed.add(el);
        }

        return trimmed;
    }
}
