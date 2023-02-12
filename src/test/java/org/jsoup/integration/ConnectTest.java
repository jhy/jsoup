package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.integration.servlets.*;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.Parser;
import org.jsoup.parser.XmlTreeBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
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
            assertEquals("org.jsoup.HttpStatusException: HTTP error fetching URL. Status=404, URL=[" + e.getUrl() + "]", e.toString());
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
    public void canSendSecFetchHeaders() throws IOException {
        // https://github.com/jhy/jsoup/issues/1461
        Document doc = Jsoup.connect(echoUrl)
            .header("Random-Header-name", "hello")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Sec-Fetch-Mode", "cors")
            .get();

        assertEquals("hello", ihVal("Random-Header-name", doc));
        assertEquals("cross-site", ihVal("Sec-Fetch-Site", doc));
        assertEquals("cors", ihVal("Sec-Fetch-Mode", doc));
    }

    @Test
    public void secFetchHeadersSurviveRedirect() throws IOException {
        Document doc = Jsoup
            .connect(RedirectServlet.Url)
            .data(RedirectServlet.LocationParam, echoUrl)
            .header("Random-Header-name", "hello")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Sec-Fetch-Mode", "cors")
            .get();

        assertEquals("hello", ihVal("Random-Header-name", doc));
        assertEquals("cross-site", ihVal("Sec-Fetch-Site", doc));
        assertEquals("cors", ihVal("Sec-Fetch-Mode", doc));
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
        File html = ParseTest.getFile("/htmltests/large.html");

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
        assertEquals("large.html", ihVal("Part secondPart Filename", res));
        assertEquals("280735", ihVal("Part secondPart Size", res));

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

    @Test public void requestCookiesSurviveRedirect() throws IOException {
        // this test makes sure that Request keyval cookies (not in the cookie store) are sent on subsequent redirections,
        // when not using the session method
        Connection con = Jsoup.connect(RedirectServlet.Url)
            .data(RedirectServlet.LocationParam, echoUrl)
            .cookie("LetMeIn", "True")
            .cookie("DoesItWork", "Yes");

        Connection.Response res = con.execute();
        assertEquals(0, res.cookies().size()); // were not set by Redir or Echo servlet
        Document doc = res.parse();
        assertEquals(echoUrl, doc.location());
        assertEquals("True", ihVal("Cookie: LetMeIn", doc));
        assertEquals("Yes", ihVal("Cookie: DoesItWork", doc));
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
        assertNotNull(title);
        assertEquals("jsoup RSS news", title.text());
        assertEquals("channel", title.parent().nodeName());
        assertEquals("", doc.title()); // the document title is unset, this tag is channel>title, not html>head>title
        assertEquals(3, doc.select("link").size());
        assertEquals("application/rss+xml", con.response().contentType());
        assertTrue(doc.parser().getTreeBuilder() instanceof XmlTreeBuilder);
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
        String localUrl = FileServlet.urlTo(localPath);

        Document docFromLocalServer = Jsoup.connect(localUrl).get();
        Document docFromFileRead = Jsoup.parse(ParseTest.getFile(localPath), "UTF-8");

        String text = docFromLocalServer.body().text();
        assertEquals(14766, text.length());
        assertEquals(text, docFromLocalServer.body().text());
        assertEquals(text, docFromFileRead.body().text());
    }

    /**
     * Test fetching a form, and submitting it with a file attached.
     */
    @Test
    public void postHtmlFile() throws IOException {
        Document index = Jsoup.connect(FileServlet.urlTo("/htmltests/upload-form.html")).get();
        List<FormElement> forms = index.select("[name=tidy]").forms();
        assertEquals(1, forms.size());
        FormElement form = forms.get(0);
        Connection post = form.submit();

        File uploadFile = ParseTest.getFile("/htmltests/large.html");
        FileInputStream stream = new FileInputStream(uploadFile);

        Connection.KeyVal fileData = post.data("_file");
        assertNotNull(fileData);
        fileData.value("check.html");
        fileData.inputStream(stream);

        Connection.Response res;
        try {
            res = post.execute();
        } finally {
            stream.close();
        }

        Document doc = res.parse();
        assertEquals(ihVal("Method", doc), "POST"); // from form action
        assertEquals(ihVal("Part _file Filename", doc), "check.html");
        assertEquals(ihVal("Part _file Name", doc), "_file");
        assertEquals(ihVal("_function", doc), "tidy");
    }

    @Test
    public void fetchHandlesXml() throws IOException {
        String[] types = {"text/xml", "application/xml", "application/rss+xml", "application/xhtml+xml"};
        for (String type : types) {
            fetchHandlesXml(type);
        }
    }

    void fetchHandlesXml(String contentType) throws IOException {
        // should auto-detect xml and use XML parser, unless explicitly requested the html parser
        String xmlUrl = FileServlet.urlTo("/htmltests/xml-test.xml");
        Connection con = Jsoup.connect(xmlUrl);
        con.data(FileServlet.ContentTypeParam, contentType);
        Document doc = con.get();
        Connection.Request req = con.request();
        assertTrue(req.parser().getTreeBuilder() instanceof XmlTreeBuilder);
        assertEquals("<doc><val>One<val>Two</val>Three</val></doc>\n", doc.outerHtml());
        assertEquals(con.response().contentType(), contentType);
    }

    @Test
    public void fetchHandlesXmlAsHtmlWhenParserSet() throws IOException {
        // should auto-detect xml and use XML parser, unless explicitly requested the html parser
        String xmlUrl = FileServlet.urlTo("/htmltests/xml-test.xml");
        Connection con = Jsoup.connect(xmlUrl).parser(Parser.htmlParser());
        con.data(FileServlet.ContentTypeParam, "application/xml");
        Document doc = con.get();
        Connection.Request req = con.request();
        assertTrue(req.parser().getTreeBuilder() instanceof HtmlTreeBuilder);
        assertEquals("<html> <head></head> <body> <doc> <val> One <val> Two </val>Three </val> </doc> </body> </html>", StringUtil.normaliseWhitespace(doc.outerHtml()));
    }

    @Test
    public void combinesSameHeadersWithComma() throws IOException {
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
        Connection con = Jsoup.connect(echoUrl);
        con.get();

        Connection.Response res = con.response();
        assertEquals("text/html;charset=utf-8", res.header("Content-Type"));
        assertEquals("no-cache, no-store", res.header("Cache-Control"));

        List<String> header = res.headers("Cache-Control");
        assertEquals(2, header.size());
        assertEquals("no-cache", header.get(0));
        assertEquals("no-store", header.get(1));
    }

    @Test
    public void sendHeadRequest() throws IOException {
        String url = FileServlet.urlTo("/htmltests/xml-test.xml");
        Connection con = Jsoup.connect(url)
            .method(Connection.Method.HEAD)
            .data(FileServlet.ContentTypeParam, "text/xml");
        final Connection.Response response = con.execute();
        assertEquals("text/xml", response.header("Content-Type"));
        assertEquals("", response.body()); // head ought to have no body
        Document doc = response.parse();
        assertEquals("", doc.text());
    }

    @Test
    public void fetchToW3c() throws IOException {
        String url = FileServlet.urlTo("/htmltests/upload-form.html");
        Document doc = Jsoup.connect(url).get();

        W3CDom dom = new W3CDom();
        org.w3c.dom.Document wDoc = dom.fromJsoup(doc);
        assertEquals(url, wDoc.getDocumentURI());
        String html = dom.asString(wDoc);
        assertTrue(html.contains("Upload"));
    }

    @Test
    public void baseHrefCorrectAfterHttpEquiv() throws IOException {
        // https://github.com/jhy/jsoup/issues/440
        Connection.Response res = Jsoup.connect(FileServlet.urlTo("/htmltests/charset-base.html")).execute();
        Document doc = res.parse();
        assertEquals("http://example.com/foo.jpg", doc.select("img").first().absUrl("src"));
    }

    @Test
    public void maxBodySize() throws IOException {
        String url = FileServlet.urlTo("/htmltests/large.html"); // 280 K

        Connection.Response defaultRes = Jsoup.connect(url).execute();
        Connection.Response smallRes = Jsoup.connect(url).maxBodySize(50 * 1024).execute(); // crops
        Connection.Response mediumRes = Jsoup.connect(url).maxBodySize(200 * 1024).execute(); // crops
        Connection.Response largeRes = Jsoup.connect(url).maxBodySize(300 * 1024).execute(); // does not crop
        Connection.Response unlimitedRes = Jsoup.connect(url).maxBodySize(0).execute();

        int actualDocText = 269535;
        assertEquals(actualDocText, defaultRes.parse().text().length());
        assertEquals(49165, smallRes.parse().text().length());
        assertEquals(196577, mediumRes.parse().text().length());
        assertEquals(actualDocText, largeRes.parse().text().length());
        assertEquals(actualDocText, unlimitedRes.parse().text().length());
    }

    @Test public void repeatable() throws IOException {
        String url = FileServlet.urlTo("/htmltests/large.html"); // 280 K
        Connection con = Jsoup.connect(url).parser(Parser.xmlParser());
        Document doc1 = con.get();
        Document doc2 = con.get();
        assertEquals("Large HTML", doc1.title());
        assertEquals("Large HTML", doc2.title());
    }

    @Test
    public void maxBodySizeInReadToByteBuffer() throws IOException {
        // https://github.com/jhy/jsoup/issues/1774
        // when calling readToByteBuffer, contents were not buffered up
        String url = FileServlet.urlTo("/htmltests/large.html"); // 280 K

        Connection.Response defaultRes = Jsoup.connect(url).execute();
        Connection.Response smallRes = Jsoup.connect(url).maxBodySize(50 * 1024).execute(); // crops
        Connection.Response mediumRes = Jsoup.connect(url).maxBodySize(200 * 1024).execute(); // crops
        Connection.Response largeRes = Jsoup.connect(url).maxBodySize(300 * 1024).execute(); // does not crop
        Connection.Response unlimitedRes = Jsoup.connect(url).maxBodySize(0).execute();

        int actualDocText = 280735;
        assertEquals(actualDocText, defaultRes.body().length());
        assertEquals(50 * 1024, smallRes.body().length());
        assertEquals(200 * 1024, mediumRes.body().length());
        assertEquals(actualDocText, largeRes.body().length());
        assertEquals(actualDocText, unlimitedRes.body().length());
    }

    @Test void formLoginFlow() throws IOException {
        String echoUrl = EchoServlet.Url;
        String cookieUrl = CookieServlet.Url;

        String startUrl = FileServlet.urlTo("/htmltests/form-tests.html");
        Document loginDoc = Jsoup.connect(startUrl).get();
        FormElement form = loginDoc.expectForm("#login");
        assertNotNull(form);
        form.expectFirst("[name=username]").val("admin");
        form.expectFirst("[name=password]").val("Netscape engineers are weenies!");

        // post it- should go to Cookie then bounce to Echo
        Connection submit = form.submit();
        assertEquals(Connection.Method.POST, submit.request().method());
        Connection.Response postRes = submit.execute();
        assertEquals(echoUrl, postRes.url().toExternalForm());
        assertEquals(Connection.Method.GET, postRes.method());
        Document resultDoc = postRes.parse();
        assertEquals("One=EchoServlet; One=Root", ihVal("Cookie", resultDoc));
        // should be no form data sent to the echo redirect
        assertEquals("", ihVal("Query String", resultDoc));

        // new request to echo, should not have form data, but should have cookies from implicit session
        Document newEcho = submit.newRequest().url(echoUrl).get();
        assertEquals("One=EchoServlet; One=Root", ihVal("Cookie", newEcho));
        assertEquals("", ihVal("Query String", newEcho));

        Document cookieDoc = submit.newRequest().url(cookieUrl).get();
        assertEquals("CookieServlet", ihVal("One", cookieDoc)); // different cookie path

    }

    @Test void formLoginFlow2() throws IOException {
        String echoUrl = EchoServlet.Url;
        String cookieUrl = CookieServlet.Url;
        String startUrl = FileServlet.urlTo("/htmltests/form-tests.html");

        Connection session = Jsoup.newSession();
        Document loginDoc = session.newRequest().url(startUrl).get();
        FormElement form = loginDoc.expectForm("#login2");
        assertNotNull(form);
        String username = "admin";
        form.expectFirst("[name=username]").val(username);
        String password = "Netscape engineers are weenies!";
        form.expectFirst("[name=password]").val(password);

        Connection submit = form.submit();
        assertEquals(username, submit.data("username").value());
        assertEquals(password, submit.data("password").value());

        Connection.Response postRes = submit.execute();
        assertEquals(cookieUrl, postRes.url().toExternalForm());
        assertEquals(Connection.Method.POST, postRes.method());
        Document resultDoc = postRes.parse();

        Document echo2 = resultDoc.connection().newRequest().url(echoUrl).get();
        assertEquals("", ihVal("Query String", echo2)); // should not re-send the data
        assertEquals("One=EchoServlet; One=Root", ihVal("Cookie", echo2));
    }

    @Test void preservesUrlFragment() throws IOException {
        // confirms https://github.com/jhy/jsoup/issues/1686
        String url = EchoServlet.Url + "#fragment";
        Document doc = Jsoup.connect(url).get();
        assertEquals(url, doc.location());
    }
}
