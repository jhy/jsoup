package org.jsoup.integration.servlets;

import org.jsoup.integration.TestServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Slowly, interminably writes output. For the purposes of testing timeouts and interrupts.
 */
public class SlowRider extends BaseServlet {
    public static final String Url = TestServer.map(SlowRider.class);
    private static final int SleepTime = 1000;
    public static final String MaxTimeParam = "maxTime";


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
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
            try {
                Thread.sleep(SleepTime);
            } catch (InterruptedException e) {
                break;
            }

            if (maxTime > 0 && System.currentTimeMillis() > startTime + maxTime) {
                w.println("<h1>outatime</h1>");
                break;
            }
        }
    }

    // allow the servlet to run as a main program, for local test
    public static void main(String[] args) {
        TestServer.start();
        System.out.println(Url);
    }
}
