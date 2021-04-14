package main.java.org.jsoup.strategy;

/**
 * Strategy interface that defines the scrape method to be overriden by concrete strategies.
 */
public interface ScraperStrategy {
    
    /**
     * Selects certain elements from the given document.
     * To be overriden by the concrete strategies.
     * 
     * @param doc The document to be scraped.
     */
    public Elements scrape(Document doc);
}
