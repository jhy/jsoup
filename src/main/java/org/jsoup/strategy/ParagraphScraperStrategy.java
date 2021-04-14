package main.java.org.jsoup.strategy;

/**
 * Scraper strategy that scrapes for paragraph elements.
 */
public class ParagraphScraperStrategy implements ScraperStrategy{
    
    /**
     * Selects "p" tags from the given document.
     * 
     * @param doc The document to be scraped.
     */
    @Override
    public Elements scrape(Document doc) {
        return doc.select("p");
    }
}
