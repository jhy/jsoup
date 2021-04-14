package main.java.org.jsoup.strategy;

public class LinkScraperStrategy implements ScraperStrategy{
    
    @Override
    public Elements scrape(Document doc) {
        return doc.select("a[href]");
    }
}
