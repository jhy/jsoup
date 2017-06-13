package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.*;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

/**
 * Integration test: parses from real-world example HTML.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class ParseTest {

    @Test
    public void testSmhBizArticle() throws IOException {
        File in = getFile("/htmltests/smh-biz-article-1.html");
        Document doc = Jsoup.parse(in, "UTF-8",
                "http://www.smh.com.au/business/the-boards-next-fear-the-female-quota-20100106-lteq.html");
        assertEquals("The board’s next fear: the female quota",
                doc.title()); // note that the apos in the source is a literal ’ (8217), not escaped or '
        assertEquals("en", doc.select("html").attr("xml:lang"));

        Elements articleBody = doc.select(".articleBody > *");
        assertEquals(17, articleBody.size());
        // todo: more tests!

    }

    @Test
    public void testNewsHomepage() throws IOException {
        File in = getFile("/htmltests/news-com-au-home.html");
        Document doc = Jsoup.parse(in, "UTF-8", "http://www.news.com.au/");
        assertEquals("News.com.au | News from Australia and around the world online | NewsComAu", doc.title());
        assertEquals("Brace yourself for Metro meltdown", doc.select(".id1225817868581 h4").text().trim());

        Element a = doc.select("a[href=/entertainment/horoscopes]").first();
        assertEquals("/entertainment/horoscopes", a.attr("href"));
        assertEquals("http://www.news.com.au/entertainment/horoscopes", a.attr("abs:href"));

        Element hs = doc.select("a[href*=naughty-corners-are-a-bad-idea]").first();
        assertEquals(
                "http://www.heraldsun.com.au/news/naughty-corners-are-a-bad-idea-for-kids/story-e6frf7jo-1225817899003",
                hs.attr("href"));
        assertEquals(hs.attr("href"), hs.attr("abs:href"));
    }

    @Test
    public void testGoogleSearchIpod() throws IOException {
        File in = getFile("/htmltests/google-ipod.html");
        Document doc = Jsoup.parse(in, "UTF-8", "http://www.google.com/search?hl=en&q=ipod&aq=f&oq=&aqi=g10");
        assertEquals("ipod - Google Search", doc.title());
        Elements results = doc.select("h3.r > a");
        assertEquals(12, results.size());
        assertEquals(
                "http://news.google.com/news?hl=en&q=ipod&um=1&ie=UTF-8&ei=uYlKS4SbBoGg6gPf-5XXCw&sa=X&oi=news_group&ct=title&resnum=1&ved=0CCIQsQQwAA",
                results.get(0).attr("href"));
        assertEquals("http://www.apple.com/itunes/",
                results.get(1).attr("href"));
    }

    @Test
    public void testBinary() throws IOException {
        File in = getFile("/htmltests/thumb.jpg");
        Document doc = Jsoup.parse(in, "UTF-8");
        // nothing useful, but did not blow up
        assertTrue(doc.text().contains("gd-jpeg"));
    }

    @Test
    public void testYahooJp() throws IOException {
        File in = getFile("/htmltests/yahoo-jp.html");
        Document doc = Jsoup.parse(in, "UTF-8", "http://www.yahoo.co.jp/index.html"); // http charset is utf-8.
        assertEquals("Yahoo! JAPAN", doc.title());
        Element a = doc.select("a[href=t/2322m2]").first();
        assertEquals("http://www.yahoo.co.jp/_ylh=X3oDMTB0NWxnaGxsBF9TAzIwNzcyOTYyNjUEdGlkAzEyBHRtcGwDZ2Ex/t/2322m2",
                a.attr("abs:href")); // session put into <base>
        assertEquals("全国、人気の駅ランキング", a.text());
    }

    @Test
    public void testBaidu() throws IOException {
        // tests <meta http-equiv="Content-Type" content="text/html;charset=gb2312">
        File in = getFile("/htmltests/baidu-cn-home.html");
        Document doc = Jsoup.parse(in, null,
                "http://www.baidu.com/"); // http charset is gb2312, but NOT specifying it, to test http-equiv parse
        Element submit = doc.select("#su").first();
        assertEquals("百度一下", submit.attr("value"));

        // test from attribute match
        submit = doc.select("input[value=百度一下]").first();
        assertEquals("su", submit.id());
        Element newsLink = doc.select("a:contains(新)").first();
        assertEquals("http://news.baidu.com", newsLink.absUrl("href"));

        // check auto-detect from meta
        assertEquals("GB2312", doc.outputSettings().charset().displayName());
        assertEquals("<title>百度一下，你就知道      </title>", doc.select("title").outerHtml());

        doc.outputSettings().charset("ascii");
        assertEquals("<title>&#x767e;&#x5ea6;&#x4e00;&#x4e0b;&#xff0c;&#x4f60;&#x5c31;&#x77e5;&#x9053;      </title>",
                doc.select("title").outerHtml());
    }

    @Test
    public void testBaiduVariant() throws IOException {
        // tests <meta charset> when preceded by another <meta>
        File in = getFile("/htmltests/baidu-variant.html");
        Document doc = Jsoup.parse(in, null,
                "http://www.baidu.com/"); // http charset is gb2312, but NOT specifying it, to test http-equiv parse
        // check auto-detect from meta
        assertEquals("GB2312", doc.outputSettings().charset().displayName());
        assertEquals("<title>百度一下，你就知道</title>", doc.select("title").outerHtml());
    }

    @Test
    public void testHtml5Charset() throws IOException {
        // test that <meta charset="gb2312"> works
        File in = getFile("/htmltests/meta-charset-1.html");
        Document doc = Jsoup.parse(in, null, "http://example.com/"); //gb2312, has html5 <meta charset>
        assertEquals("新", doc.text());
        assertEquals("GB2312", doc.outputSettings().charset().displayName());

        // double check, no charset, falls back to utf8 which is incorrect
        in = getFile("/htmltests/meta-charset-2.html"); //
        doc = Jsoup.parse(in, null, "http://example.com"); // gb2312, no charset
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());
        assertFalse("新".equals(doc.text()));

        // confirm fallback to utf8
        in = getFile("/htmltests/meta-charset-3.html");
        doc = Jsoup.parse(in, null, "http://example.com/"); // utf8, no charset
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());
        assertEquals("新", doc.text());
    }

    @Test
    public void testBrokenHtml5CharsetWithASingleDoubleQuote() throws IOException {
        InputStream in = inputStreamFrom("<html>\n" +
                "<head><meta charset=UTF-8\"></head>\n" +
                "<body></body>\n" +
                "</html>");
        Document doc = Jsoup.parse(in, null, "http://example.com/");
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());
    }

    @Test
    public void testNytArticle() throws IOException {
        // has tags like <nyt_text>
        File in = getFile("/htmltests/nyt-article-1.html");
        Document doc = Jsoup.parse(in, null, "http://www.nytimes.com/2010/07/26/business/global/26bp.html?hp");

        Element headline = doc.select("nyt_headline[version=1.0]").first();
        assertEquals("As BP Lays Out Future, It Will Not Include Hayward", headline.text());
    }

    @Test
    public void testYahooArticle() throws IOException {
        File in = getFile("/htmltests/yahoo-article-1.html");
        Document doc = Jsoup.parse(in, "UTF-8", "http://news.yahoo.com/s/nm/20100831/bs_nm/us_gm_china");
        Element p = doc.select("p:contains(Volt will be sold in the United States)").first();
        assertEquals("In July, GM said its electric Chevrolet Volt will be sold in the United States at $41,000 -- $8,000 more than its nearest competitor, the Nissan Leaf.", p.text());
    }

    @Test
    public void testLowercaseUtf8Charset() throws IOException {
        File in = getFile("/htmltests/lowercase-charset-test.html");
        Document doc = Jsoup.parse(in, null);

        Element form = doc.select("#form").first();
        assertEquals(2, form.children().size());
        assertEquals("UTF-8", doc.outputSettings().charset().name());
    }

    public static File getFile(String resourceName) {
        try {
            File file = new File(ParseTest.class.getResource(resourceName).toURI());
            return file;
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static InputStream inputStreamFrom(String s) {
        try {
            return new ByteArrayInputStream(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
