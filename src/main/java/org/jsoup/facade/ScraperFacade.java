package main.java.org.jsoup.facade;

import main.java.org.jsoup.strategy.ScraperStrategy;

public class ScraperFacade {
    
    private Scraper scraper;

    public ScraperFacade() {
        this.scraper = new Scraper();
    }

    public void scrape(String url, ScraperStrategy strategy) {
        this.scraper.setScraperStrategy(strategy);
        
        Document doc = Jsoup.connect(url).get();
        Elements elements = this.scraper.scrape(doc);

        for(Element e: elements) {
            System.out.println(e.toString());
        }
    }

}
