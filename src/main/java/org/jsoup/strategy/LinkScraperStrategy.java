package main.java.org.jsoup.strategy;

/**
 * Scraper strategy that scrapes for link elements.
 */
public class LinkScraperStrategy implements ScraperStrategy{
    
    /**
     * Selects "link" tags from the given document with a filled "href" attribiute.
     * 
     * @param doc The document to be scraped.
     */
    @Override
    public Elements scrape(Document doc) {
        return doc.select("a[href]");
    }
}
