package org.jsoup.text;

import org.junit.Test;
import static org.junit.Assert.*;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.parser.Parser;

/**
 *  Point tests.
 */
public class PointTest {
    static public TextNode firstTextNode(Node node) {
        for(Node child : node.childNodes()) {
            if(child instanceof TextNode)
                return (TextNode)child;
            if(child.childNodeSize() > 0) {
                TextNode result = firstTextNode(child);
                if(result != null)
                    return result;
            }
        }
        return null;
    }

    @Test
    public void isBasicallySane() {
        Document html = Parser.parseBodyFragment("<p>Three words here", "");
        TextNode text = (TextNode)html.select("p").first().childNode(0);
        Point p0 = new Point(text, 0);
        Point p2 = new Point(text, 2);
        Point p42 = new Point(text, 42);
        Point p69 = new Point(text, 42);

        assertTrue(p0.equals(p0));
        assertFalse(p0.equals(p2));
        assertFalse(p42.equals(p2));
        assertTrue(p42.equals(p69));

        assertEquals(text, p0.getTextNode());
        assertEquals(2, p2.getOffset());
    }

    @Test
    public void testLeftAndRightBoundness() {
        Document html = Parser.parseBodyFragment("<p>One<b>two</b></p>", "");
        Element p = html.select("p").first();
        Element b = html.select("b").first();

        Point left = new Point((TextNode)p.childNode(0), 3);
        Point right = new Point((TextNode)b.childNode(0), 0);
        assertTrue(right == right.rightBound());
        assertTrue(left == left.leftBound());
        assertEquals("two", right.getTextNode().text());
        assertEquals(3, right.leftBound().getOffset());
        assertEquals(left, right.leftBound());
        assertEquals(right, left.rightBound());
        assertEquals(right, right.leftBound().rightBound());

        Point start = new Point((TextNode)p.childNode(0), 0);
        Point end = new Point((TextNode)b.childNode(0), 3);
        assertTrue(start == start.leftBound());
        assertTrue(start == start.rightBound());
        assertTrue(end == end.leftBound());
        assertTrue(end == end.rightBound());
    }

    @Test
    public void splits() {
        Document html = Parser.parseBodyFragment("<p>a<b>c</b>", "");
        Element p = html.select("p").first();
        (new Point(firstTextNode(p), 1)).splitAfterUpTo(p.parentNode());
        assertEquals("<p>a</p>", p.outerHtml());
    }

    // This needs a several-paragraph test; the first four paragraphs of
    // http://www.nybooks.com/articles/2014/10/23/find-your-beach/ will do.

    static private String LONG_DOCUMENT =
        "<p>Across the way from our apartment—on Houston, I guess—there’s a new wall ad. The site is forty feet high, twenty feet wide. It changes once or twice a year. Whatever’s on that wall is my view: I look at it more than the sky or the new World Trade Center, more than the water towers, the passing cabs. It has a subliminal effect. Last semester it was a spot for high-end vodka, and while I wrangled children into their snowsuits, chock-full of domestic resentment, I’d find myself dreaming of cold martinis.\n" +
        "<p>Before that came an ad so high-end I couldn’t tell what it was for. There was no text—or none that I could see—and the visual was of a yellow firebird set upon a background of hellish red. It seemed a gnomic message, deliberately placed to drive a sleepless woman mad. Once, staring at it with a newborn in my arms, I saw another mother, in the tower opposite, holding her baby. It was 4 AM. We stood there at our respective windows, separated by a hundred feet of expensive New York air.\n"+
        "<p>The tower I live in is university accommodation; so is the tower opposite. The idea occurred that it was quite likely that the woman at the window also wrote books for a living, and, like me, was not writing anything right now. Maybe she was considering antidepressants. Maybe she was already on them. It was hard to tell. Certainly she had no way of viewing the ad in question, not without opening her window, jumping, and turning as she fell. I was her view. I was the ad for what she already had.\n"+
        "<p>But that was all some time ago. Now the ad says: <i>Find your beach.</i> The bottle of beer—it’s an ad for beer—is very yellow and the background luxury-holiday-blue. It seems to me uniquely well placed, like a piece of commissioned public art in perfect sympathy with its urban site. The tone is pure Manhattan. Echoes can be found in the personal growth section of the bookstore (“Find your happy”), and in exercise classes (“Find your soul”), and in the therapist’s office (“Find your self”). I find it significant that there exists a more expansive, national version of this ad that runs in magazines, and on television.\n";

    @Test
    public void findsNeighbouringText() {
        Document html = Parser.parseBodyFragment(LONG_DOCUMENT, "");
        Region beach = html.find("Find your beach").first();
        Region justbefore = beach.getStart().precedingRegion(32);
        assertEquals("some time ago. Now the ad says:", justbefore.getText());
        Region furtherbefore = justbefore.getStart().precedingRegion(30);
        assertEquals("already had. But that was all",
                     furtherbefore.getText());
        Region beginning =
            furtherbefore.getStart().precedingRegion(LONG_DOCUMENT.length());
        assertTrue(beginning.getText().startsWith("Across the way from"));
        assertTrue(beginning.getText().endsWith("what she"));

        assertEquals(justbefore.getText(),
                     furtherbefore.getEnd().followingRegion(32).getText());
    }

    @Test
    public void findsParagraphLimits() {
        Document html = Parser.parseBodyFragment(LONG_DOCUMENT, "");
        Region beach = html.find("beach").first();
        assertEquals("Find your beach.",
                     new Region(beach.getStart().atStartOfElement(),
                                beach.getEnd().atEndOfElement()).getText());
        Region paragraph = new Region(beach.getStart().atStartOfBlock(),
                                      beach.getEnd().atEndOfBlock());
        assertTrue(paragraph.getText().startsWith("But that was all") &&
                   paragraph.getText().endsWith("and on television."));
    }
}
