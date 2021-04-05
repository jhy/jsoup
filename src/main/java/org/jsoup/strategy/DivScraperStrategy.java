package main.java.org.jsoup.strategy;

public class DivScraperStrategy implements ScraperStrategy{
    
    @Override
    public Elements scrape(Document doc) {
        return doc.select("div");
    }
}
