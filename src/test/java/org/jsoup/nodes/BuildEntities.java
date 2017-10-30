package org.jsoup.nodes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.integration.UrlConnectTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * Fetches HTML entity names from w3.org json, and outputs data files for optimized used in Entities.
 * I refuse to believe that entity names like "NotNestedLessLess" are valuable or useful for HTML authors. Implemented
 * only to be complete.
 */
class BuildEntities {
    public static void main(String[] args) throws IOException {
        String url = "https://www.w3.org/TR/2012/WD-html5-20121025/entities.json";
        Connection.Response res = Jsoup.connect(url)
            .ignoreContentType(true)
            .userAgent(UrlConnectTest.browserUa)
            .execute();

        Gson gson = new Gson();
        Map<String, CharacterRef> input = gson.fromJson(res.body(),
            new TypeToken<Map<String, CharacterRef>>() {
            }.getType());


        // build name sorted base and full character lists:
        ArrayList<CharacterRef> base = new ArrayList<>();
        ArrayList<CharacterRef> full = new ArrayList<>();

        for (Map.Entry<String, CharacterRef> entry : input.entrySet()) {
            String name = entry.getKey().substring(1); // name is like &acute or &acute; , trim &
            CharacterRef ref = entry.getValue();
            if (name.endsWith(";")) {
                name = name.substring(0, name.length() - 1);
                full.add(ref);
            } else {
                base.add(ref);
            }
            ref.name = name;
        }
        Collections.sort(base, byName);
        Collections.sort(full, byName);

        // now determine code point order
        ArrayList<CharacterRef> baseByCode = new ArrayList<>(base);
        ArrayList<CharacterRef> fullByCode = new ArrayList<>(full);
        Collections.sort(baseByCode, byCode);
        Collections.sort(fullByCode, byCode);

        // and update their codepoint index.
        @SuppressWarnings("unchecked") ArrayList<CharacterRef>[] codelists = new ArrayList[]{baseByCode, fullByCode};
        for (ArrayList<CharacterRef> codelist : codelists) {
            for (int i = 0; i < codelist.size(); i++) {
                codelist.get(i).codeIndex = i;
            }
        }

        // now write them
        persist("entities-full", full);
        persist("entities-base", base);

        System.out.println("Full size: " + full.size() + ", base size: " + base.size());
    }

    private static void persist(String name, ArrayList<CharacterRef> refs) throws IOException {
        File file = Files.createTempFile(name, ".txt").toFile();
        FileWriter writer = new FileWriter(file, false);
        writer.append("static final String points = \"");
        for (CharacterRef ref : refs) {
            writer.append(ref.toString()).append('&');
        }
        writer.append("\";\n");
        writer.close();

        System.out.println("Wrote " + name + " to " + file.getAbsolutePath());
    }


    private static class CharacterRef {
        int[] codepoints;
        String name;
        int codeIndex;

        @Override
        public String toString() {
            return name
                + "="
                + d(codepoints[0])
                + (codepoints.length > 1 ? "," + d(codepoints[1]) : "")
                + ";" + d(codeIndex);
        }
    }

    private static String d(int d) {
        return Integer.toString(d, Entities.codepointRadix);
    }

    private static class ByName implements Comparator<CharacterRef> {
        public int compare(CharacterRef o1, CharacterRef o2) {
            return o1.name.compareTo(o2.name);
        }
    }

    private static class ByCode implements Comparator<CharacterRef> {
        public int compare(CharacterRef o1, CharacterRef o2) {
            int[] c1 = o1.codepoints;
            int[] c2 = o2.codepoints;
            int first = c1[0] - c2[0];
            if (first != 0)
                return first;
            if (c1.length == 1 && c2.length == 1) { // for the same code, use the shorter name
                int len = o2.name.length() - o1.name.length();
                if (len != 0)
                    return len;
                return o1.name.compareTo(o2.name);
            }
            if (c1.length == 2 && c2.length == 2)
                return c1[1] - c2[1];
            else
                return c2.length - c1.length; // pushes multi down the list so hits on singles first (don't support multi lookup by codepoint yet)
        }
    }

    private static ByName byName = new ByName();
    private static ByCode byCode = new ByCode();
}
