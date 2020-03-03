package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.integration.servlets.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static org.jsoup.helper.HttpConnection.CONTENT_TYPE;
import static org.jsoup.helper.HttpConnection.MULTIPART_FORM_DATA;
import static org.jsoup.integration.UrlConnectTest.browserUa;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Jsoup.connect against a local server.
 */
public class ConnectTest {
    private static String echoUrl;

    @BeforeAll
    public static void setUp() {
        TestServer.start();
        echoUrl = EchoServlet.Url;
    }

    @AfterAll
    public static void tearDown() {
        TestServer.stop();
    }

    @Test
    public void canConnectToLocalServer() throws IOException {
        String url = HelloServlet.Url;
        Document doc = Jsoup.connect(url).get();
        Element p = doc.selectFirst("p");
        assertEquals("Hello, World!", p.text());
    }

    @Test
    public void fetchURl() throws IOException {
        Document doc = Jsoup.parse(new URL(echoUrl), 10 * 1000);
        assertTrue(doc.title().contains("Environment Variables"));
    }

    @Test
    public void fetchURIWithWhitespace() throws IOException {
        Connection con = Jsoup.connect(echoUrl + "#with whitespaces");
        Document doc = con.get();
        assertTrue(doc.title().contains("Environment Variables"));
    }

    @Test
    public void exceptOnUnsupportedProtocol() {
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

    private static String ihVal(String key, Document doc) {
        final Element first = doc.select("th:contains(" + key + ") + td").first();
        return first != null ? first.text() : null;
    }

    @Test
    public void throwsExceptionOn404() {
        String url = EchoServlet.Url;
        Connection con = Jsoup.connect(url).header(EchoServlet.CodeParam, "404");

        boolean threw = false;
        try {
            Document doc = con.get();
        } catch (HttpStatusException e) {
            threw = true;
            assertEquals("org.jsoup.HttpStatusException: HTTP error fetching URL. Status=404, URL=" + e.getUrl(), e.toString());
            assertTrue(e.getUrl().startsWith(url));
            assertEquals(404, e.getStatusCode());
        } catch (IOException e) {
        }
        assertTrue(threw);
    }

    @Test
    public void ignoresExceptionIfSoConfigured() throws IOException {
        String url = EchoServlet.Url;
        Connection con = Jsoup.connect(url)
            .header(EchoServlet.CodeParam, "404")
            .ignoreHttpErrors(true);
        Connection.Response res = con.execute();
        Document doc = res.parse();
        assertEquals(404, res.statusCode());
        assertEquals("Webserver Environment Variables", doc.title());
    }

    @Test
    public void doesPost() throws IOException {
        Document doc = Jsoup.connect(echoUrl)
            .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
            .cookie("auth", "token")
            .post();

        assertEquals("POST", ihVal("Method", doc));
        assertEquals("gzip", ihVal("Accept-Encoding", doc));
        assertEquals("auth=token", ihVal("Cookie", doc));
        assertEquals("度一下", ihVal("百", doc));
        assertEquals("Jsoup, Jonathan", ihVal("uname", doc));
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", ihVal("Content-Type", doc));
    }

    @Test
    public void doesPostMultipartWithoutInputstream() throws IOException {
        Document doc = Jsoup.connect(echoUrl)
                .header(CONTENT_TYPE, MULTIPART_FORM_DATA)
                .userAgent(browserUa)
                .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
                .post();

        assertTrue(ihVal("Content-Type", doc).contains(MULTIPART_FORM_DATA));

        assertTrue(ihVal("Content-Type", doc).contains("boundary")); // should be automatically set
        assertEquals("Jsoup, Jonathan", ihVal("uname", doc));
        assertEquals("度一下", ihVal("百", doc));
    }

    @Test
    public void sendsRequestBodyJsonWithData() throws IOException {
        final String body = "{key:value}";
        Document doc = Jsoup.connect(echoUrl)
            .requestBody(body)
            .header("Content-Type", "application/json")
            .userAgent(browserUa)
            .data("foo", "true")
            .post();
        assertEquals("POST", ihVal("Method", doc));
        assertEquals("application/json", ihVal("Content-Type", doc));
        assertEquals("foo=true", ihVal("Query String", doc));
        assertEquals(body, ihVal("Post Data", doc));
    }

    @Test
    public void sendsRequestBodyJsonWithoutData() throws IOException {
        final String body = "{key:value}";
        Document doc = Jsoup.connect(echoUrl)
            .requestBody(body)
            .header("Content-Type", "application/json")
            .userAgent(browserUa)
            .post();
        assertEquals("POST", ihVal("Method", doc));
        assertEquals("application/json", ihVal("Content-Type", doc));
        assertEquals(body, ihVal("Post Data", doc));
    }

    @Test
    public void sendsRequestBody() throws IOException {
        final String body = "{key:value}";
        Document doc = Jsoup.connect(echoUrl)
            .requestBody(body)
            .header("Content-Type", "text/plain")
            .userAgent(browserUa)
            .post();
        assertEquals("POST", ihVal("Method", doc));
        assertEquals("text/plain", ihVal("Content-Type", doc));
        assertEquals(body, ihVal("Post Data", doc));
    }

    @Test
    public void sendsRequestBodyWithUrlParams() throws IOException {
        final String body = "{key:value}";
        Document doc = Jsoup.connect(echoUrl)
            .requestBody(body)
            .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
            .header("Content-Type", "text/plain") // todo - if user sets content-type, we should append postcharset
            .userAgent(browserUa)
            .post();
        assertEquals("POST", ihVal("Method", doc));
        assertEquals("uname=Jsoup&uname=Jonathan&%E7%99%BE=%E5%BA%A6%E4%B8%80%E4%B8%8B", ihVal("Query String", doc));
        assertEquals(body, ihVal("Post Data", doc));
    }

    @Test
    public void doesGet() throws IOException {
        Connection con = Jsoup.connect(echoUrl + "?what=the")
            .userAgent("Mozilla")
            .referrer("http://example.com")
            .data("what", "about & me?");

        Document doc = con.get();
        assertEquals("what=the&what=about+%26+me%3F", ihVal("Query String", doc));
        assertEquals("the, about & me?", ihVal("what", doc));
        assertEquals("Mozilla", ihVal("User-Agent", doc));
        assertEquals("http://example.com", ihVal("Referer", doc));
    }

    @Test
    public void doesPut() throws IOException {
        Connection.Response res = Jsoup.connect(echoUrl)
            .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
            .cookie("auth", "token")
            .method(Connection.Method.PUT)
            .execute();

        Document doc = res.parse();
        assertEquals("PUT", ihVal("Method", doc));
        assertEquals("gzip", ihVal("Accept-Encoding", doc));
        assertEquals("auth=token", ihVal("Cookie", doc));
    }

    /**
     * Tests upload of content to a remote service.
     */
    @Test
    public void postFiles() throws IOException {
        File thumb = ParseTest.getFile("/htmltests/thumb.jpg");
        File html = ParseTest.getFile("/htmltests/google-ipod.html.gz");

        Document res = Jsoup
            .connect(EchoServlet.Url)
            .data("firstname", "Jay")
            .data("firstPart", thumb.getName(), new FileInputStream(thumb), "image/jpeg")
            .data("secondPart", html.getName(), new FileInputStream(html)) // defaults to "application-octetstream";
            .data("surname", "Soup")
            .post();

        assertEquals("4", ihVal("Parts", res));

        assertEquals("application/octet-stream", ihVal("Part secondPart ContentType", res));
        assertEquals("secondPart", ihVal("Part secondPart Name", res));
        assertEquals("google-ipod.html.gz", ihVal("Part secondPart Filename", res));
        assertEquals("12212", ihVal("Part secondPart Size", res));

        assertEquals("image/jpeg", ihVal("Part firstPart ContentType", res));
        assertEquals("firstPart", ihVal("Part firstPart Name", res));
        assertEquals("thumb.jpg", ihVal("Part firstPart Filename", res));
        assertEquals("1052", ihVal("Part firstPart Size", res));

        assertEquals("Jay", ihVal("firstname", res));
        assertEquals("Soup", ihVal("surname", res));

        /*
        <tr><th>Part secondPart ContentType</th><td>application/octet-stream</td></tr>
        <tr><th>Part secondPart Name</th><td>secondPart</td></tr>
        <tr><th>Part secondPart Filename</th><td>google-ipod.html</td></tr>
        <tr><th>Part secondPart Size</th><td>43972</td></tr>
        <tr><th>Part firstPart ContentType</th><td>image/jpeg</td></tr>
        <tr><th>Part firstPart Name</th><td>firstPart</td></tr>
        <tr><th>Part firstPart Filename</th><td>thumb.jpg</td></tr>
        <tr><th>Part firstPart Size</th><td>1052</td></tr>
         */
    }

    @Test
    public void multipleParsesOkAfterBufferUp() throws IOException {
        Connection.Response res = Jsoup.connect(echoUrl).execute().bufferUp();

        Document doc = res.parse();
        assertTrue(doc.title().contains("Environment"));

        Document doc2 = res.parse();
        assertTrue(doc2.title().contains("Environment"));
    }

    @Test
    public void bodyAfterParseThrowsValidationError() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection.Response res = Jsoup.connect(echoUrl).execute();
            Document doc = res.parse();
            String body = res.body();
        });
    }

    @Test
    public void bodyAndBytesAvailableBeforeParse() throws IOException {
        Connection.Response res = Jsoup.connect(echoUrl).execute();
        String body = res.body();
        assertTrue(body.contains("Environment"));
        byte[] bytes = res.bodyAsBytes();
        assertTrue(bytes.length > 100);

        Document doc = res.parse();
        assertTrue(doc.title().contains("Environment"));
    }

    @Test
    public void parseParseThrowsValidates() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection.Response res = Jsoup.connect(echoUrl).execute();
            Document doc = res.parse();
            assertTrue(doc.title().contains("Environment"));
            Document doc2 = res.parse(); // should blow up because the response input stream has been drained
        });
    }


    @Test
    public void multiCookieSet() throws IOException {
        Connection con = Jsoup
                .connect(RedirectServlet.Url)
                .data(RedirectServlet.CodeParam, "302")
                .data(RedirectServlet.SetCookiesParam, "true")
                .data(RedirectServlet.LocationParam, echoUrl);
        Connection.Response res = con.execute();

        // test cookies set by redirect:
        Map<String, String> cookies = res.cookies();
        assertEquals("asdfg123", cookies.get("token"));
        assertEquals("jhy", cookies.get("uid"));

        // send those cookies into the echo URL by map:
        Document doc = Jsoup.connect(echoUrl).cookies(cookies).get();
        assertEquals("token=asdfg123; uid=jhy", ihVal("Cookie", doc));
    }

    @Test
    public void supportsDeflate() throws IOException {
        Connection.Response res = Jsoup.connect(Deflateservlet.Url).execute();
        assertEquals("deflate", res.header("Content-Encoding"));

        Document doc = res.parse();
        assertEquals("Hello, World!", doc.selectFirst("p").text());
    }

    @Test
    public void handlesLargerContentLengthParseRead() throws IOException {
        // this handles situations where the remote server sets a content length greater than it actually writes

        Connection.Response res = Jsoup.connect(InterruptedServlet.Url)
            .data(InterruptedServlet.Magnitude, InterruptedServlet.Larger)
            .timeout(400)
            .execute();

        Document document = res.parse();
        assertEquals("Something", document.title());
        assertEquals(0, document.select("p").size());
        // current impl, jetty won't write past content length
        // todo - find way to trick jetty into writing larger than set header. Take over the stream?
    }

    @Test
    public void handlesWrongContentLengthDuringBufferedRead() throws IOException {
        Connection.Response res = Jsoup.connect(InterruptedServlet.Url)
                .timeout(400)
                .execute();
        // this servlet writes max_buffer data, but sets content length to max_buffer/2. So will read up to that.
        // previous versions of jetty would allow to write less, and would throw except here

        res.bufferUp();
        Document doc = res.parse();
        assertEquals(0, doc.select("p").size());
    }

    @Test public void handlesRedirect() throws IOException {
        Document doc = Jsoup.connect(RedirectServlet.Url)
            .data(RedirectServlet.LocationParam, HelloServlet.Url)
            .get();

        Element p = doc.selectFirst("p");
        assertEquals("Hello, World!", p.text());

        assertEquals(HelloServlet.Url, doc.location());
    }

    @Test public void handlesEmptyRedirect() {
        boolean threw = false;
        try {
            Connection.Response res = Jsoup.connect(RedirectServlet.Url)
                .execute();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Too many redirects"));
            threw = true;
        }
        assertTrue(threw);
    }

    @Test public void doesNotPostFor302() throws IOException {
        final Document doc = Jsoup.connect(RedirectServlet.Url)
            .data("Hello", "there")
            .data(RedirectServlet.LocationParam, EchoServlet.Url)
            .post();

        assertEquals(EchoServlet.Url, doc.location());
        assertEquals("GET", ihVal("Method", doc));
        assertNull(ihVal("Hello", doc)); // data not sent
    }

    @Test public void doesPostFor307() throws IOException {
        final Document doc = Jsoup.connect(RedirectServlet.Url)
            .data("Hello", "there")
            .data(RedirectServlet.LocationParam, EchoServlet.Url)
            .data(RedirectServlet.CodeParam, "307")
            .post();

        assertEquals(EchoServlet.Url, doc.location());
        assertEquals("POST", ihVal("Method", doc));
        assertEquals("there", ihVal("Hello", doc));
    }

    @Test public void getUtf8Bom() throws IOException {
        Connection con = Jsoup.connect(FileServlet.urlTo("/bomtests/bom_utf8.html"));
        Document doc = con.get();

        assertEquals("UTF-8", con.response().charset());
        assertEquals("OK", doc.title());
    }

    @Test
    public void testBinaryContentTypeThrowsException() {
        Connection con = Jsoup.connect(FileServlet.urlTo("/htmltests/thumb.jpg"));
        con.data(FileServlet.ContentTypeParam, "image/jpeg");

        boolean threw = false;
        try {
            con.execute();
            Document doc = con.response().parse();
        } catch (IOException e) {
            threw = true;
            assertEquals("Unhandled content type. Must be text/*, application/xml, or application/*+xml", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test public void testParseRss() throws IOException {
        // test that we switch automatically to xml, and we support application/rss+xml
        Connection con = Jsoup.connect(FileServlet.urlTo("/htmltests/test-rss.xml"));
        con.data(FileServlet.ContentTypeParam, "application/rss+xml");
        Document doc = con.get();
        Element title = doc.selectFirst("title");
        assertEquals("jsoup RSS news", title.text());
        assertEquals("channel", title.parent().nodeName());
        assertEquals("jsoup RSS news", doc.title());
        assertEquals(3, doc.select("link").size());
        assertEquals("application/rss+xml", con.response().contentType());
        assertEquals(Document.OutputSettings.Syntax.xml, doc.outputSettings().syntax());
    }

    @Test
    public void canFetchBinaryAsBytes() throws IOException {
        Connection.Response res = Jsoup.connect(FileServlet.urlTo("/htmltests/thumb.jpg"))
            .data(FileServlet.ContentTypeParam, "image/jpeg")
            .ignoreContentType(true)
            .execute();

        byte[] bytes = res.bodyAsBytes();
        assertEquals(1052, bytes.length);
    }

    @Test
    public void handlesUnknownEscapesAcrossBuffer() throws IOException {
        String localPath = "/htmltests/escapes-across-buffer.html";
        String url =
            "https://gist.githubusercontent.com/krystiangorecki/d3bad50ef5615f06b077438607423533/raw/71adfdf81121282ea936510ed6cfe440adeb2d83/JsoupIssue1218.html";
        String localUrl = FileServlet.urlTo(localPath);

        Document docFromGithub = Jsoup.connect(url).get(); // different chunks meant GH would error but local not...
        Document docFromLocalServer = Jsoup.connect(localUrl).get();
        Document docFromFileRead = Jsoup.parse(ParseTest.getFile(localPath), "UTF-8");

        String text = docFromGithub.body().text();
        assertEquals(14766, text.length());
        assertEquals(text, docFromLocalServer.body().text());
        assertEquals(text, docFromFileRead.body().text());
    }
}
