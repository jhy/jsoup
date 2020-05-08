package org.jsoup.parser;

import java.util.Vector;

/**
 Increase efficiency: Shrink html text by ignoring specified content.
 An additional feature provided to users.
 Can reduce amount of Textnode by ignore "\n" to speed up parse.*/
public class HtmlShrinker {

    private static int[] getNext(String aim) {
        char[] aimArray = aim.toCharArray();
        int[] next = new int[aimArray.length];
        next[0] = -1;
        int j = 0;
        int k = -1;
        while (j < aimArray.length - 1) {
            if (k == -1 || aimArray[j] == aimArray[k]) {
                next[j+1] = k+1;
                j++;
                k++;
            } else {
                k = next[k];
            }
        }
        return next;
    }

    private static Vector<Integer> KMP(String essay, String aim) {
        char[] t = essay.toCharArray();
        char[] p = aim.toCharArray();
        int i = 0, j = 0;
        Vector<Integer> appear = new Vector<>();
        int[] next = getNext(aim);
        while (i < t.length && j < p.length) {
            if (j == -1) {
                i++;
                j=0;
            }
            else if(t[i] == p[j]){
                i++;
                j++;
                if (j == p.length) {
                    appear.addElement(Integer.valueOf(i-j+1));
                    j=next[j-1];
                    i= i - 1;
                }
            }
            else {
                j = next[j];
            }
        }
        return appear;
    }

    /**
     * Delete Strings need to be ignored from original html text
     * @param html current html text
     * @param shrinkTarget ignored content
     * @return this, new html without ignored contents.
     */
    public static String htmlShrink(String html, String shrinkTarget){
        Vector<Integer> appearList = KMP(html,shrinkTarget);
        char[] htmlChars = html.toCharArray();
        StringBuffer shrinkedHtml = new StringBuffer();
        int htmlLength = htmlChars.length;
        int targetLength = shrinkTarget.length();
        int appearTimes = appearList.size();
        if(appearTimes == 0) return html;
        int count = 0;
        boolean shrinkFinish = false;
        for(int i=0; i<htmlLength; i++){
            if(shrinkFinish==false && i == (appearList.get(count)-1) && (i==0 || htmlChars[i-1]!='\\')) {
                i = i + (targetLength -1);
                count++;
                if(count == appearTimes) shrinkFinish=true;
                continue;
            }
            shrinkedHtml.append(htmlChars[i]);
        }
        return shrinkedHtml.toString();
    }
}
