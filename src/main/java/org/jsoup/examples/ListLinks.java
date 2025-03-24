package org.jsoup.examples;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;

/**
 * Example program to list links from a URL.
 */ //(false negative)(Deficient encapsulation)(design smell) (move method, extract class, replace conditional, with polymorphism)
public class ListLinks {
    private static final Logger logger = Logger.getLogger(ListLinks.class.getName());

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            logger.log(Level.SEVERE, "Usage: supply a URL to fetch");
            return;
        }

        String url = args[0];
        logger.log(Level.INFO, "Fetching {0}...", url);

        // Extract links using LinkExtractor class
        LinkExtractor extractor = new LinkExtractor(url);
        extractor.fetch();

        printLinks("Media", extractor.getMedia());
        printLinks("Imports", extractor.getImports());
        printLinks("Links", extractor.getHrefLinks());
    }

    private static void printLinks(String category, List<LinkType> links) {
        logger.log(Level.INFO, "\n{0}: ({1})", new Object[]{category, links.size()});
        for (LinkType link : links) {
            logger.log(Level.INFO, " * {0}", link.getFormattedOutput());
        }
    }
}

/**
 * Extract class - Handles fetching and processing links.
 */
class LinkExtractor {
    private final String url;
    private Document doc;

    public LinkExtractor(String url) {
        this.url = url;
    }

    public void fetch() throws IOException {
        this.doc = Jsoup.connect(url).get();
    }

    public List<LinkType> getMedia() {
        return extractLinks(doc.select("[src]"), MediaLink::new);
    }

    public List<LinkType> getImports() {
        return extractLinks(doc.select("link[href]"), ImportLink::new);
    }

    public List<LinkType> getHrefLinks() {
        return extractLinks(doc.select("a[href]"), HrefLink::new);
    }

    private List<LinkType> extractLinks(Elements elements, LinkFactory factory) {
        List<LinkType> links = new ArrayList<>();
        for (Element element : elements) {
            links.add(factory.create(element));
        }
        return links;
    }
}

/**
 * Interface for different link types - Used for Replace Conditional with Polymorphism.
 */
interface LinkType {
    String getFormattedOutput();
}

/**
 * Factory interface to create LinkType instances.
 */
interface LinkFactory {
    LinkType create(Element element);
}

/**
 * Represents Media links (images, videos, etc.).
 */
class MediaLink implements LinkType {
    private static final int MAX_ALT_TEXT_WIDTH = 20;
    private final Element element;

    public MediaLink(Element element) {
        this.element = element;
    }

    @Override
    public String getFormattedOutput() {
        if (element.tagName().equals("img")) {
            return String.format(" * %s: <%s> %sx%s (%s)", element.tagName(), element.attr("abs:src"),
                    element.attr("width"), element.attr("height"), trim(element.attr("alt"), MAX_ALT_TEXT_WIDTH));
        } else {
            return String.format(" * %s: <%s>", element.tagName(), element.attr("abs:src"));
        }
    }

    private String trim(String s, int width) {
        return (s.length() > width) ? s.substring(0, width - 1) + "." : s;
    }
}

/**
 * Represents Import links (CSS, external resources).
 */
class ImportLink implements LinkType {
    private final Element element;

    public ImportLink(Element element) {
        this.element = element;
    }

    @Override
    public String getFormattedOutput() {
        return String.format(" * %s <%s> (%s)", element.tagName(), element.attr("abs:href"), element.attr("rel"));
    }
}

/**
 * Represents Hyperlink references.
 */
class HrefLink implements LinkType {
    private static final int MAX_TEXT_WIDTH = 35;
    private final Element element;

    public HrefLink(Element element) {
        this.element = element;
    }

    @Override
    public String getFormattedOutput() {
        return String.format(" * a: <%s>  (%s)", element.attr("abs:href"), trim(element.text(), MAX_TEXT_WIDTH));
    }

    private String trim(String s, int width) {
        return (s.length() > width) ? s.substring(0, width - 1) + "." : s;
    }
}

