package org.jsoup.integration.servlets;

import org.jsoup.integration.TestServer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Slowly, interminably writes output. For the purposes of testing timeouts and interrupts.
 */
public class SlowRider extends BaseServlet {
    public static final String Url = TestServer.map(SlowRider.class);
    private static final int SleepTime = 2000;
    public static final String MaxTimeParam = "maxTime";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        pause(1000);
        res.setContentType(TextHtml);
        res.setStatus(HttpServletResponse.SC_OK);
        PrintWriter w = res.getWriter();

        int maxTime = -1;
        String maxTimeP = req.getParameter(MaxTimeParam);
        if (maxTimeP != null) {
            maxTime = Integer.valueOf(maxTimeP);
        }

        long startTime = System.currentTimeMillis();
        while (true) {
            w.println("<p>Are you still there?");
            boolean err = w.checkError(); // flush, and check still ok
            if (err) {
                log("Remote connection lost");
                break;
            }
            if (pause(SleepTime)) break;

            if (maxTime > 0 && System.currentTimeMillis() > startTime + maxTime) {
                w.println("<h1>outatime</h1>");
                break;
            }
        }
    }

    private static boolean pause(int sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            return true;
        }
        return false;
    }

    // allow the servlet to run as a main program, for local test
    public static void main(String[] args) {
        TestServer.start();
        System.out.println(Url);
    }
}
