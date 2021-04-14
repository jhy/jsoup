package main.java.org.jsoup.strategy;

/**
 * Scraper strategy that scrapes for div elements.
 */
public class DivScraperStrategy implements ScraperStrategy{
    
    /**
     * Selects "div" tags from the given document.
     * 
     * @param doc The document to be scraped.
     */
    @Override
    public Elements scrape(Document doc) {
        return doc.select("div");
    }
}
