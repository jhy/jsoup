package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.HTMLTable;
import org.jsoup.nodes.TableCell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * This class provides methods to extract HTML tables from a URL, HTML string, or HTML file.
 * It uses the Jsoup library for HTML parsing and manipulation.
 * 
 * @author: Noura Alroomi
 */
public class HTMLTableExtractor {

    /**
     * Extracts HTML tables from a given URL.
     * 
     * @param url The URL from which to extract HTML tables
     * 
     * @return A list of HTMLTable objects representing the extracted tables
     * 
     * @throws IOException If an I/O error occurs while connecting to the URL or parsing the HTML
     */
    public List<HTMLTable> extractTableFromURL(String url) throws IOException {
        // Validate the URL
        Validate.notEmpty(url, "URL must not be empty");
        Validate.isTrue(url.startsWith("http://") || url.startsWith("https://"), "Invalid URL: " + url);
        
        // connect to the URL and retrieve the HTML document
        Document doc = Jsoup.connect(url).get();
        return parseHtmlTable(doc);
    }

    /**
     * Extracts HTML tables from a given HTML string.
     * 
     * @param htmlString The HTML string from which to extract HTML tables
     * 
     * @return A list of HTMLTable objects representing the extracted tables
     * 
     * @throws IOException If an I/O error occurs while parsing the HTML
     */
    public List<HTMLTable> extractTableFromHTML(String htmlString) throws IOException {
        // Validate the htmlString
        Validate.notEmpty(htmlString, "HTML string must not be empty");
        
        // parse the HTML string and retrieve the document
        Document doc = Jsoup.parse(htmlString);
        return parseHtmlTable(doc);
    }

    /**
     * Extracts HTML tables from a given HTML file path.
     * 
     * @param filepath The file path of the HTML file from which to extract HTML tables
     * 
     * @return A list of HTMLTable objects representing the extracted tables
     * 
     * @throws IOException If an I/O error occurs while reading the HTML file or parsing the HTML
     */
    public List<HTMLTable> extractTableFromHTMLFile(String filepath) throws IOException {
        // Validate the filepath
        Validate.notEmpty(filepath, "File path must not be empty");
        
        // create a File object from the file path
        File inputFile = new File(filepath);
        
        // Check if the file exists and is a regular file
        Validate.isTrue(inputFile.exists() && inputFile.isFile(), "File does not exist or is not a regular file: " + filepath);

        // parse the HTML file and retrieve the document
        Document doc = Jsoup.parse(inputFile, "UTF-8");
        return parseHtmlTable(doc);
    }
    
    /**
     * Extracts HTML tables from a given HTML file.
     * 
     * @param inputFile The HTML file from which to extract HTML tables
     * 
     * @return A list of HTMLTable objects representing the extracted tables
     * 
     * @throws IOException If an I/O error occurs while reading the HTML file or parsing the HTML
     */
    public List<HTMLTable> extractTableFromHTMLFile(File inputFile) throws IOException {
        // Validate the inputFile
        Validate.notNull(inputFile, "Input file must not be null");

        // Check if the file exists and is a regular file
        Validate.isTrue(inputFile.exists() && inputFile.isFile(), "File does not exist or is not a regular file: " + inputFile.getAbsolutePath());

        // parse the HTML file and retrieve the document
        Document doc = Jsoup.parse(inputFile, "UTF-8");
        return parseHtmlTable(doc);
    }

    /**
     * 
     * Parses HTML tables from a Jsoup Document object.
     * 
     * @param doc The Jsoup Document object representing the HTML
     * 
     * @return A list of HTMLTable objects representing the parsed tables
     */
    public List<HTMLTable> parseHtmlTable(Document doc) {
        // initilize tables list 
        List<HTMLTable> tables = new ArrayList<>();

        // select all table elements in the document
        Elements tableElements = doc.select("table");

        // iterate over each table element
        for (Element tableElement : tableElements) {
            List<List<TableCell>> table = new ArrayList<>();
            Elements rows = tableElement.select("tr");
            int tableHeight = rows.size();
            int tableWidth = 0;
            Map<Integer, Integer> rowspanList = new HashMap<>();
            Map<Integer, TableCell> rowspanValueList = new HashMap<>();
            // iterate over each row in the table
            for (Element row : rows) {
                List<TableCell> tableRow = new ArrayList<>();
                Elements cells = row.select("td, th");
                int totalCols = rowspanList.size();

                // calculate the total number of columns in the current row
                for (Element col : cells) {
                    int cellColSpan = col.hasAttr("colspan") ? tryParse(col.attr("colspan"), 1) : 1;
                    totalCols += cellColSpan;
                }

                // update the maximum table width
                tableWidth = Math.max(tableWidth, totalCols);

                // add empty TableCell objects to the current row for remaining columns
                for (int i = 0; i < tableWidth; i++) {
                    tableRow.add(new TableCell("", "", 1, 1));
                }
                // initiate columns Index
                int colIndex = 0;

                // iterate over each cell in the row
                for (Element cell : cells) {
                    String cellType = cell.tagName();
                    String cellValue = cell.text();
                    int colspan = 1;
                    int rowspan = 1;

                    // check if the current column index is affected by a rowspan from a previous
                    // row
                    while (rowspanList.containsKey(colIndex)) {
                        int rowspans = rowspanList.get(colIndex);
                        TableCell rowSpanCell = rowspanValueList.get(colIndex);
                        tableRow.set(colIndex, rowSpanCell);
                        rowspanList.put(colIndex, --rowspans);

                        if (rowspans == 0) {
                            rowspanList.remove(colIndex);
                            rowspanValueList.remove(colIndex);
                        }
                        colIndex++;
                    }

                    // check if the cell has colspan attribute
                    if (cell.hasAttr("colspan")) {
                        colspan = tryParse(cell.attr("colspan"), 1);
                    }
                    // check if the cell has  rowspan attribute
                    if (cell.hasAttr("rowspan")) {
                        rowspan = tryParse(cell.attr("rowspan"), 1);
                    }

                    // add TableCell objects to the current row based on colspan and rowspan
                    for (int i = 0; i < colspan; i++) {
                        if (i == 0)
                            tableRow.set(colIndex, new TableCell(cellType, cellValue, colspan, rowspan));
                        else
                            tableRow.set(colIndex, new TableCell(cellType, cellValue, 1, 1));

                        if (rowspan > 1) {
                            rowspanList.put(colIndex, rowspan - 1);
                            rowspanValueList.put(colIndex, new TableCell(cellType, cellValue, 1, 1));
                        }
                        colIndex++;
                    }
                }

                // check if there are remaining columns in the row affected by a rowspan from a
                // previous row
                if (colIndex < tableWidth) {
                    while (rowspanList.containsKey(colIndex)) {
                        int rowspans = rowspanList.get(colIndex);
                        TableCell rowSpanCell = rowspanValueList.get(colIndex);
                        tableRow.set(colIndex, rowSpanCell);
                        rowspanList.put(colIndex, --rowspans);

                        if (rowspans == 0) {
                            rowspanList.remove(colIndex);
                            rowspanValueList.remove(colIndex);
                        }
                        colIndex++;
                    }
                }

                // add the current row to the table
                table.add(tableRow);
            }

            // create an HTMLTable object with the parsed table data and add it to the list
            HTMLTable htmlTable = new HTMLTable(table, tableWidth, tableHeight);
            tables.add(htmlTable);
        }

        return tables;
    }

    /**
     * Tries to parse an integer from a string. Returns the default value if parsing
     * fails.
     * 
     * @param value        The string to parse
     * @param defaultValue The default value to return if parsing fails
     * @return The parsed integer value or the default value if parsing fails
     */
    private static int tryParse(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
