package org.jsoup;

import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the Pseudo-element selector selects correctly.
 *
 * @author Albert Zhang, 11712023@mail.sustech.edu.cn
 */

public class PseudoElementTest {

    @Test
    //Test the simplest situation, selecting by element
    public void issue1173Scenario1(){
        Elements els = Jsoup.parse("<div id=1>" +
                "<div id=2>" +
                "<p>Hello</p>" +
                "</div>" +
                "</div>").select("div::before");

        assertEquals(2, els.size());
        assertEquals("1", els.get(0).id());
        assertEquals("2", els.get(1).id());
    }

    @Test
    //Test the combination situation, selecting by class
    public void issue1173Scenario2(){//
        Elements els = Jsoup.parse("<div>" +
                "<div>" +
                "<p class=test id=1>Hello</p>" +
                "</div>" +
                "</div>").select(".test::after");

        assertEquals(1, els.size());
        assertEquals("1", els.get(0).id());
    }
}