package main.java.org.jsoup.strategy;

public class Scraper {

    private ScraperStrategy scraperStrategy;

    /**
     * Constructor for Scraper, sets paragraph scraping as default
     */
    public Scraper() {
        this.scraperStrategy = new ParagraphScraperStrategy();
    }

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