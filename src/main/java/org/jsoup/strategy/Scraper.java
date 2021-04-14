package main.java.org.jsoup.strategy;

/**
 * This is the class that implements the scraping strategy. Strategies can be 
 * exchanged based on what kind of scraper is needed.
 * 
 * Note: the default strategy is set to Paragraph.
 */
public class Scraper {

    private ScraperStrategy scraperStrategy;

    /**
     * Constructor for Scraper, sets paragraph scraping as default
     */
    public Scraper() {
        this.scraperStrategy = new ParagraphScraperStrategy();
    }

    /**
     * Scrapes the given document based on the set strategy.
     * 
     * @param doc Document to be scraped.
     * @return Elements scraped from the document.
     */
    public Elements scrape(Document doc) {
        return this.scraperStrategy.scrape(doc);
    }

    // GETTERS and SETTERS for the strategy

    public ScraperStrategy getScraperStrategy() {
        return scraperStrategy;
    }

    public void setScraperStrategy(ScraperStrategy scrapingStrategy) {
        this.scraperStrategy = scrapingStrategy;
    }
}