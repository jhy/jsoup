package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.FormElement;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.Parser;
import org.jsoup.parser.XmlTreeBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 Tests the URL connection. Not enabled by default, so tests don't require network connection.

 @author Jonathan Hedley, jonathan@hedley.net */
@Ignore // ignored by default so tests don't require network access. comment out to enable.
public class UrlConnectTest {
    private static final String WEBSITE_WITH_INVALID_CERTIFICATE = "https://certs.cac.washington.edu/CAtest/";
    private static final String WEBSITE_WITH_SNI = "https://jsoup.org/";
    private static String echoURL = "http://direct.infohound.net/tools/q.pl";
    private static String browserUa = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36";

    @Test
    public void fetchURl() throws IOException {
        String url = "http://jsoup.org"; // no trailing / to force redir
        Document doc = Jsoup.parse(new URL(url), 10*1000);
        assertTrue(doc.title().contains("jsoup"));
    }

    @Test
    public void fetchURIWithWihtespace() throws IOException {
        Connection con = Jsoup.connect("http://try.jsoup.org/#with whitespaces");
        Document doc = con.get();
        assertTrue(doc.title().contains("jsoup"));
    }

    @Test
    public void fetchBaidu() throws IOException {
        Connection.Response res = Jsoup.connect("http://www.baidu.com/").timeout(10*1000).execute();
        Document doc = res.parse();

        assertEquals("GBK", doc.outputSettings().charset().displayName());
        assertEquals("GBK", res.charset());
        assert(res.hasCookie("BAIDUID"));
        assertEquals("text/html;charset=gbk", res.contentType());
    }
    
    @Test
    public void exceptOnUnknownContentType() {
        String url = "http://direct.jsoup.org/rez/osi_logo.png"; // not text/* but image/png, should throw
        boolean threw = false;
        try {
            Document doc = Jsoup.parse(new URL(url), 3000);
        } catch (UnsupportedMimeTypeException e) {
            threw = true;
            assertEquals("org.jsoup.UnsupportedMimeTypeException: Unhandled content type. Must be text/*, application/xml, or application/xhtml+xml. Mimetype=image/png, URL=http://direct.jsoup.org/rez/osi_logo.png", e.toString());
            assertEquals(url, e.getUrl());
            assertEquals("image/png", e.getMimeType());
        } catch (IOException e) {
        }
        assertTrue(threw);
    }

    @Test
    public void exceptOnUnsupportedProtocol(){
        String url = "file://etc/passwd";
        boolean threw = false;
        try {
            Document doc = Jsoup.connect(url).get();
        } catch (MalformedURLException e) {
            threw = true;
            assertEquals("java.net.MalformedURLException: Only http & https protocols supported", e.toString());
        } catch (IOException e) {
        }
        assertTrue(threw);
    }

    @Test
    public void ignoresContentTypeIfSoConfigured() throws IOException {
        Document doc = Jsoup.connect("http://jsoup.org/rez/osi_logo.png").ignoreContentType(true).get();
        assertEquals("", doc.title()); // this will cause an ugly parse tree
    }

    @Test
    public void doesPost() throws IOException {
        Document doc = Jsoup.connect(echoURL)
            .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
            .cookie("auth", "token")
            .post();

        assertEquals("POST", ihVal("REQUEST_METHOD", doc));
        //assertEquals("gzip", ihVal("HTTP_ACCEPT_ENCODING", doc)); // current proxy removes gzip on post
        assertEquals("auth=token", ihVal("HTTP_COOKIE", doc));
        assertEquals("度一下", ihVal("百", doc));
        assertEquals("Jsoup, Jonathan", ihVal("uname", doc));
    }

    @Test
    public void sendsRequestBody() throws IOException {
        final String body = "{key:value}";
        Document doc = Jsoup.connect(echoURL)
            .requestBody(body)
            .header("Content-Type", "text/plain")
            .userAgent(browserUa)
            .post();
        assertEquals("POST", ihVal("REQUEST_METHOD", doc));
        assertEquals(body, ihVal("keywords", doc));
    }

    @Test
    public void sendsRequestBodyWithUrlParams() throws IOException {
        final String body = "{key:value}";
        Document doc = Jsoup.connect(echoURL)
            .requestBody(body)
            .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
            .header("Content-Type", "text/plain") // todo - if user sets content-type, we should append postcharset
            .userAgent(browserUa)
            .post();
        assertEquals("POST", ihVal("REQUEST_METHOD", doc));
        assertEquals("uname=Jsoup&uname=Jonathan&%E7%99%BE=%E5%BA%A6%E4%B8%80%E4%B8%8B", ihVal("QUERY_STRING", doc));
        assertEquals(body, ihVal("POSTDATA", doc));
    }

    @Test
    public void doesGet() throws IOException {
        Connection con = Jsoup.connect(echoURL + "?what=the")
            .userAgent("Mozilla")
            .referrer("http://example.com")
            .data("what", "about & me?");

        Document doc = con.get();
        assertEquals("what=the&what=about+%26+me%3F", ihVal("QUERY_STRING", doc));
        assertEquals("the, about & me?", ihVal("what", doc));
        assertEquals("Mozilla", ihVal("HTTP_USER_AGENT", doc));
        assertEquals("http://example.com", ihVal("HTTP_REFERER", doc));
    }

    @Test
    public void doesPut() throws IOException {
        Connection.Response res = Jsoup.connect(echoURL)
                .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
                .cookie("auth", "token")
                .method(Connection.Method.PUT)
                .execute();

        Document doc = res.parse();
        assertEquals("PUT", ihVal("REQUEST_METHOD", doc));
        //assertEquals("gzip", ihVal("HTTP_ACCEPT_ENCODING", doc)); // current proxy removes gzip on post
        assertEquals("auth=token", ihVal("HTTP_COOKIE", doc));
    }


    private static String ihVal(String key, Document doc) {
        return doc.select("th:contains("+key+") + td").first().text();
    }

    @Test
    public void followsTempRedirect() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/302.pl"); // http://jsoup.org
        Document doc = con.get();
        assertTrue(doc.title().contains("jsoup"));
    }

    @Test
    public void followsNewTempRedirect() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/307.pl"); // http://jsoup.org
        Document doc = con.get();
        assertTrue(doc.title().contains("jsoup"));
        assertEquals("http://jsoup.org", con.response().url().toString());
    }

    @Test
    public void postRedirectsFetchWithGet() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/302.pl")
                .data("Argument", "Riposte")
                .method(Connection.Method.POST);
        Connection.Response res = con.execute();
        assertEquals("http://jsoup.org", res.url().toExternalForm());
        assertEquals(Connection.Method.GET, res.method());
    }

    @Test
    public void followsRedirectToHttps() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/302-secure.pl"); // https://www.google.com
        con.data("id", "5");
        Document doc = con.get();
        assertTrue(doc.title().contains("Google"));
    }

    @Test
    public void followsRelativeRedirect() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/302-rel.pl"); // to /tidy/
        Document doc = con.post();
        assertTrue(doc.title().contains("HTML Tidy Online"));
    }

    @Test
    public void followsRelativeDotRedirect() throws IOException {
        // redirects to "./ok.html", should resolve to http://direct.infohound.net/tools/ok.html
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/302-rel-dot.pl"); // to ./ok.html
        Document doc = con.post();
        assertTrue(doc.title().contains("OK"));
        assertEquals(doc.location(), "http://direct.infohound.net/tools/ok.html");
    }

    @Test
    public void followsRelativeDotRedirect2() throws IOException {
        //redirects to "esportspenedes.cat/./ep/index.php", should resolve to "esportspenedes.cat/ep/index.php"
        Connection con = Jsoup.connect("http://esportspenedes.cat")  // note lack of trailing / - server should redir to / first, then to ./ep/...; but doesn't'
                .timeout(10000);
        Document doc = con.post();
        assertEquals(doc.location(), "http://esportspenedes.cat/ep/index.php");
    }

    @Test
    public void followsRedirectsWithWithespaces() throws IOException {
        Connection con = Jsoup.connect("http://tinyurl.com/kgofxl8"); // to http://www.google.com/?q=white spaces
        Document doc = con.get();
        assertTrue(doc.title().contains("Google"));
    }

    @Test
    public void gracefullyHandleBrokenLocationRedirect() throws IOException {
        Connection con = Jsoup.connect("http://aag-ye.com"); // has Location: http:/temp/AAG_New/en/index.php
        con.get(); // would throw exception on error
        assertTrue(true);
    }

    @Test
    public void throwsExceptionOnError() {
        String url = "http://direct.infohound.net/tools/404";
        Connection con = Jsoup.connect(url);
        boolean threw = false;
        try {
            Document doc = con.get();
        } catch (HttpStatusException e) {
            threw = true;
            assertEquals("org.jsoup.HttpStatusException: HTTP error fetching URL. Status=404, URL=http://direct.infohound.net/tools/404", e.toString());
            assertEquals(url, e.getUrl());
            assertEquals(404, e.getStatusCode());
        } catch (IOException e) {
        }
        assertTrue(threw);
    }

    @Test
    public void ignoresExceptionIfSoConfigured() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/404").ignoreHttpErrors(true);
        Connection.Response res = con.execute();
        Document doc = res.parse();
        assertEquals(404, res.statusCode());
        assertEquals("404 Not Found", doc.select("h1").first().text());
    }

    @Test
    public void ignores500tExceptionIfSoConfigured() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/500.pl").ignoreHttpErrors(true);
        Connection.Response res = con.execute();
        Document doc = res.parse();
        assertEquals(500, res.statusCode());
        assertEquals("Application Error", res.statusMessage());
        assertEquals("Woops", doc.select("h1").first().text());
    }

    @Test
    public void ignores500NoWithContentExceptionIfSoConfigured() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/500-no-content.pl").ignoreHttpErrors(true);
        Connection.Response res = con.execute();
        Document doc = res.parse();
        assertEquals(500, res.statusCode());
        assertEquals("Application Error", res.statusMessage());
    }

    @Test
    public void ignores200NoWithContentExceptionIfSoConfigured() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/200-no-content.pl").ignoreHttpErrors(true);
        Connection.Response res = con.execute();
        Document doc = res.parse();
        assertEquals(200, res.statusCode());
        assertEquals("All Good", res.statusMessage());
    }

    @Test
    public void doesntRedirectIfSoConfigured() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/302.pl").followRedirects(false);
        Connection.Response res = con.execute();
        assertEquals(302, res.statusCode());
        assertEquals("http://jsoup.org", res.header("Location"));
    }

    @Test
    public void redirectsResponseCookieToNextResponse() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/302-cookie.pl");
        Connection.Response res = con.execute();
        assertEquals("asdfg123", res.cookie("token")); // confirms that cookies set on 1st hit are presented in final result
        Document doc = res.parse();
        assertEquals("token=asdfg123; uid=jhy", ihVal("HTTP_COOKIE", doc)); // confirms that redirected hit saw cookie
    }

    @Test
    public void maximumRedirects() {
        boolean threw = false;
        try {
            Document doc = Jsoup.connect("http://direct.infohound.net/tools/loop.pl").get();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Too many redirects"));
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    public void multiCookieSet() throws IOException {
        Connection con = Jsoup.connect("http://direct.infohound.net/tools/302-cookie.pl");
        Connection.Response res = con.execute();

        // test cookies set by redirect:
        Map<String, String> cookies = res.cookies();
        assertEquals("asdfg123", cookies.get("token"));
        assertEquals("jhy", cookies.get("uid"));

        // send those cookies into the echo URL by map:
        Document doc = Jsoup.connect(echoURL).cookies(cookies).get();
        assertEquals("token=asdfg123; uid=jhy", ihVal("HTTP_COOKIE", doc));
    }

    @Test
    public void handlesDodgyCharset() throws IOException {
        // tests that when we get back "UFT8", that it is recognised as unsupported, and falls back to default instead
        String url = "http://direct.infohound.net/tools/bad-charset.pl";
        Connection.Response res = Jsoup.connect(url).execute();
        assertEquals("text/html; charset=UFT8", res.header("Content-Type")); // from the header
        assertEquals(null, res.charset()); // tried to get from header, not supported, so returns null
        Document doc = res.parse(); // would throw an error if charset unsupported
        assertTrue(doc.text().contains("Hello!"));
        assertEquals("UTF-8", res.charset()); // set from default on parse
    }

    @Test
    public void maxBodySize() throws IOException {
        String url = "http://direct.infohound.net/tools/large.html"; // 280 K

        Connection.Response defaultRes = Jsoup.connect(url).execute();
        Connection.Response smallRes = Jsoup.connect(url).maxBodySize(50 * 1024).execute(); // crops
        Connection.Response mediumRes = Jsoup.connect(url).maxBodySize(200 * 1024).execute(); // crops
        Connection.Response largeRes = Jsoup.connect(url).maxBodySize(300 * 1024).execute(); // does not crop
        Connection.Response unlimitedRes = Jsoup.connect(url).maxBodySize(0).execute();

        int actualString = 280735;
        assertEquals(actualString, defaultRes.body().length());
        assertEquals(50 * 1024, smallRes.body().length());
        assertEquals(200 * 1024, mediumRes.body().length());
        assertEquals(actualString, largeRes.body().length());
        assertEquals(actualString, unlimitedRes.body().length());

        int actualDocText = 269541;
        assertEquals(actualDocText, defaultRes.parse().text().length());
        assertEquals(49165, smallRes.parse().text().length());
        assertEquals(196577, mediumRes.parse().text().length());
        assertEquals(actualDocText, largeRes.parse().text().length());
        assertEquals(actualDocText, unlimitedRes.parse().text().length());
    }

    /**
     * Verify that security disabling feature works properly.
     * <p/>
     * 1. try to hit url with invalid certificate and evaluate that exception is thrown
     *
     * @throws Exception
     */
    @Test(expected = IOException.class)
    public void testUnsafeFail() throws Exception {
        String url = WEBSITE_WITH_INVALID_CERTIFICATE;
        Jsoup.connect(url).execute();
    }


    /**
     * Verify that requests to websites with SNI fail on jdk 1.6
     * <p/>
     * read for more details:
     * http://en.wikipedia.org/wiki/Server_Name_Indication
     *
     * Test is ignored independent from others as it requires JDK 1.6
     * @throws Exception
     */
    @Test(expected = IOException.class)
    public void testSNIFail() throws Exception {
        String url = WEBSITE_WITH_SNI;
        Jsoup.connect(url).execute();
    }

    /**
     * Verify that requests to websites with SNI pass
     * <p/>
     * <b>NB!</b> this test is FAILING right now on jdk 1.6
     *
     * @throws Exception
     */
    @Test
    public void testSNIPass() throws Exception {
        String url = WEBSITE_WITH_SNI;
        Connection.Response defaultRes = Jsoup.connect(url).validateTLSCertificates(false).execute();
        assertEquals(defaultRes.statusCode(), 200);
    }

    /**
     * Verify that security disabling feature works properly.
     * <p/>
     * 1. disable security checks and call the same url to verify that content is consumed correctly
     *
     * @throws Exception
     */
    @Test
    public void testUnsafePass() throws Exception {
        String url = WEBSITE_WITH_INVALID_CERTIFICATE;
        Connection.Response defaultRes = Jsoup.connect(url).validateTLSCertificates(false).execute();
        assertEquals(defaultRes.statusCode(), 200);
    }

    @Test
    public void shouldWorkForCharsetInExtraAttribute() throws IOException {
        Connection.Response res = Jsoup.connect("https://www.creditmutuel.com/groupe/fr/").execute();
        Document doc = res.parse(); // would throw an error if charset unsupported
        assertEquals("ISO-8859-1", res.charset());
    }

    // The following tests were added to test specific domains if they work. All code paths
    // which make the following test green are tested in other unit or integration tests, so the following lines
    // could be deleted

    @Test
    public void shouldSelectFirstCharsetOnWeirdMultileCharsetsInMetaTags() throws IOException {
        Connection.Response res = Jsoup.connect("http://aamo.info/").execute();
        res.parse(); // would throw an error if charset unsupported
        assertEquals("ISO-8859-1", res.charset());
    }

    @Test
    public void shouldParseBrokenHtml5MetaCharsetTagCorrectly() throws IOException {
        Connection.Response res = Jsoup.connect("http://9kuhkep.net").execute();
        res.parse(); // would throw an error if charset unsupported
        assertEquals("UTF-8", res.charset());
    }

    @Test
    public void shouldEmptyMetaCharsetCorrectly() throws IOException {
        Connection.Response res = Jsoup.connect("http://aastmultimedia.com").execute();
        res.parse(); // would throw an error if charset unsupported
        assertEquals("UTF-8", res.charset());
    }

    @Test
    public void shouldWorkForDuplicateCharsetInTag() throws IOException {
        Connection.Response res = Jsoup.connect("http://aaptsdassn.org").execute();
        Document doc = res.parse(); // would throw an error if charset unsupported
        assertEquals("ISO-8859-1", res.charset());
    }

    @Test
    public void baseHrefCorrectAfterHttpEquiv() throws IOException {
        // https://github.com/jhy/jsoup/issues/440
        Connection.Response res = Jsoup.connect("http://direct.infohound.net/tools/charset-base.html").execute();
        Document doc = res.parse();
        assertEquals("http://example.com/foo.jpg", doc.select("img").first().absUrl("src"));
    }

    /**
     * Test fetching a form, and submitting it with a file attached.
     */
    @Test
    public void postHtmlFile() throws IOException {
        Document index = Jsoup.connect("http://direct.infohound.net/tidy/").get();
        FormElement form = index.select("[name=tidy]").forms().get(0);
        Connection post = form.submit();

        File uploadFile = ParseTest.getFile("/htmltests/google-ipod.html");
        FileInputStream stream = new FileInputStream(uploadFile);
        
        Connection.KeyVal fileData = post.data("_file");
        fileData.value("check.html");
        fileData.inputStream(stream);

        Connection.Response res;
        try {
            res = post.execute();
        } finally {
            stream.close();
        }

        Document out = res.parse();
        assertTrue(out.text().contains("HTML Tidy Complete"));
    }

    /**
     * Tests upload of binary content to a remote service.
     */
    @Test
    public void postJpeg() throws IOException {
        File thumb = ParseTest.getFile("/htmltests/thumb.jpg");
        Document result = Jsoup
            .connect("http://regex.info/exif.cgi")
            .data("f", thumb.getName(), new FileInputStream(thumb))
            .userAgent(browserUa)
            .post();

        assertEquals("Baseline DCT, Huffman coding", result.select("td:contains(Process) + td").text());
        assertEquals("1052 bytes 30 × 30", result.select("td:contains(Size) + td").text());
    }

    @Test
    public void handles201Created() throws IOException {
        Document doc = Jsoup.connect("http://direct.infohound.net/tools/201.pl").get(); // 201, location=jsoup
        assertEquals("http://jsoup.org", doc.location());
    }

    @Test
    public void fetchToW3c() throws IOException {
        String url = "http://jsoup.org";
        Document doc = Jsoup.connect(url).get();

        W3CDom dom = new W3CDom();
        org.w3c.dom.Document wDoc = dom.fromJsoup(doc);
        assertEquals(url, wDoc.getDocumentURI());
        String html = dom.asString(wDoc);
        assertTrue(html.contains("jsoup"));
    }

    @Test
    public void fetchHandlesXml() throws IOException {
        // should auto-detect xml and use XML parser, unless explicitly requested the html parser
        String xmlUrl = "http://direct.infohound.net/tools/parse-xml.xml";
        Connection con = Jsoup.connect(xmlUrl);
        Document doc = con.get();
        Connection.Request req = con.request();
        assertTrue(req.parser().getTreeBuilder() instanceof XmlTreeBuilder);
        assertEquals("<xml> <link> one </link> <table> Two </table> </xml>", StringUtil.normaliseWhitespace(doc.outerHtml()));
    }

    @Test
    public void fetchHandlesXmlAsHtmlWhenParserSet() throws IOException {
        // should auto-detect xml and use XML parser, unless explicitly requested the html parser
        String xmlUrl = "http://direct.infohound.net/tools/parse-xml.xml";
        Connection con = Jsoup.connect(xmlUrl).parser(Parser.htmlParser());
        Document doc = con.get();
        Connection.Request req = con.request();
        assertTrue(req.parser().getTreeBuilder() instanceof HtmlTreeBuilder);
        assertEquals("<html> <head></head> <body> <xml> <link>one <table> Two </table> </xml> </body> </html>", StringUtil.normaliseWhitespace(doc.outerHtml()));
    }

    @Test
    public void combinesSameHeadersWithComma() throws IOException {
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
        String url = "http://direct.infohound.net/tools/q.pl";
        Connection con = Jsoup.connect(url);
        con.get();

        assertEquals("text/html", con.response().header("Content-Type"));
        assertEquals("no-cache, no-store", con.response().header("Cache-Control"));
    }

    @Test
    public void sendHeadRequest() throws IOException {
        String url = "http://direct.infohound.net/tools/parse-xml.xml";
        Connection con = Jsoup.connect(url).method(Connection.Method.HEAD);
        final Connection.Response response = con.execute();
        assertEquals("text/xml", response.header("Content-Type"));
        assertEquals("", response.body()); // head ought to have no body
        Document doc = response.parse();
        assertEquals("", doc.text());
    }


    /*
     Proxy tests. Assumes local proxy running on 8888, without system propery set (so that specifying it is required).
     */

    @Test
    public void fetchViaHttpProxy() throws IOException {
        String url = "http://jsoup.org";
        Proxy proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("localhost", 8888));
        Document doc = Jsoup.connect(url).proxy(proxy).get();
        assertTrue(doc.title().contains("jsoup"));
    }

    @Test
    public void fetchViaHttpProxySetByArgument() throws IOException {
        String url = "http://jsoup.org";
        Document doc = Jsoup.connect(url).proxy("localhost", 8888).get();
        assertTrue(doc.title().contains("jsoup"));
    }

    @Test
    public void invalidProxyFails() throws IOException {
        boolean caught = false;
        String url = "http://jsoup.org";
        try {
            Document doc = Jsoup.connect(url).proxy("localhost", 8889).get();
        } catch (IOException e) {
            caught = e instanceof ConnectException;
        }
        assertTrue(caught);
    }

    @Test
    public void proxyGetAndSet() throws IOException {
        String url = "http://jsoup.org";
        Proxy proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("localhost", 8889)); // invalid
        final Connection con = Jsoup.connect(url).proxy(proxy);

        assert con.request().proxy() == proxy;
        con.request().proxy(null); // disable
        Document doc = con.get();
        assertTrue(doc.title().contains("jsoup")); // would fail if actually went via proxy
    }

    @Test
    public void throwsIfRequestBodyForGet() throws IOException {
        boolean caught = false;
        String url = "http://jsoup.org";
        try {
            Document doc = Jsoup.connect(url).requestBody("fail").get();
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue(caught);
    }
}
