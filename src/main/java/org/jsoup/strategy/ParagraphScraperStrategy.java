package main.java.org.jsoup.strategy;

public class ParagraphScraperStrategy implements ScraperStrategy{
    
    @Override
    public Elements scrape(Document doc) {
        return doc.select("p");
    }
}
