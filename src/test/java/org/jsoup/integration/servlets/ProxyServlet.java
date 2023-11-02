package org.jsoup.integration.servlets;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.integration.TestServer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProxyServlet extends BaseServlet{
    public static TestServer.ProxySettings ProxySettings = TestServer.proxySettings(ProxyServlet.class);
    public static String Via = "1.1 jsoup test proxy";

    @Override
    protected void doIt(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        StringBuffer urlBuf = req.getRequestURL();
        if (req.getQueryString() != null) {
            urlBuf.append('?').append(req.getQueryString());
        }
        String url = urlBuf.toString();
        //log("Proxying URL: " + url);

        Connection.Method method = Enum.valueOf(Connection.Method.class, req.getMethod());
        Connection fetch = Jsoup.connect(url)
            .method(method)
            .followRedirects(false)
            .ignoreHttpErrors(true);

        // request headers
        for (Iterator<String> it = req.getHeaderNames().asIterator(); it.hasNext(); ) {
            String name = it.next();
            Enumeration<String> values = req.getHeaders(name);
            for (Iterator<String> valuesIt = values.asIterator(); valuesIt.hasNext(); ) {
                String value = valuesIt.next();
                //System.out.println("Header: " + name + " = " + value);
                fetch.header(name, value); // todo - this invocation will replace existing header, not add
            }
        }

        // execute
        Connection.Response fetchRes = fetch.execute();
        res.setStatus(fetchRes.statusCode());

        // write the response headers
        res.addHeader("Via", Via);
        for (Map.Entry<String, List<String>> entry : fetchRes.multiHeaders().entrySet()) {
            String header = entry.getKey();
            for (String value : entry.getValue()) {
                res.addHeader(header,value);
            }
        }

        // write the body
        ServletOutputStream outputStream = res.getOutputStream();
        BufferedInputStream inputStream = fetchRes.bodyStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
    }
}
