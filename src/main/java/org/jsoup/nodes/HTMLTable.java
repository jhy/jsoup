package org.jsoup.nodes;

import java.util.List;
/**
 * Represents an HTML table.
 * 
 * @author Noura Alroomi
 */
public class HTMLTable {

    private List<List<TableCell>> tableData;
    
    private int tableWidth = 0;
    private int tableHeight = 0;

    /**
     * Constructs an HTMLTable object with the provided table data, maintain span flag, table width, and table height.
     *
     * @param tableData     List of List of TableCell objects representing the table data
     * @param tableWidth    Width of the table (number of columns)
     * @param tableHeight   Height of the table (number of rows)
     */
    public HTMLTable(List<List<TableCell>> tableData, int tableWidth, int tableHeight) {
        this.tableData = tableData;
        this.tableWidth = tableWidth;
        this.tableHeight = tableHeight;
    }
    
    /**
     * Constructs an empty HTMLTable object.
     */
    public HTMLTable() {}

    /**
     * Retrieves the table data.
     *
     * @return List of List of TableCell objects representing the table data
     */
    public List<List<TableCell>> getTableData() {
        return tableData;
    }

    /**
     * Sets the table data.
     *
     * @param tableData  List of List of TableCell objects representing the table data
     */
    public void setTableData(List<List<TableCell>> tableData) {
        this.tableData = tableData;
    }

    /**
     * Retrieves the width of the table (number of columns).
     *
     * @return Width of the table
     */
    public int getTableWidth() {
        return tableWidth;
    }

    /**
     * Sets the width of the table (number of columns).
     *
     * @param tableWidth  Width of the table
     */
    public void setTableWidth(int tableWidth) {
        this.tableWidth = tableWidth;
    }

    /**
     * Retrieves the height of the table (number of rows).
     *
     * @return Height of the table
     */
    public int getTableHeight() {
        return tableHeight;
    }

    /**
     * Sets the height of the table (number of rows).
     *
     * @param tableHeight  Height of the table
     */
    public void setTableHeight(int tableHeight) {
        this.tableHeight = tableHeight;
    }

    /**
     * Increments the table width by the specified amount.
     *
     * @param tableWidth  Amount to increment the table width
     */
    public void incrementTableWidth(int tableWidth) {
        this.tableWidth += tableWidth;
    }

    /**
     * Increments the table width by 1.
     */
    public void incrementTableWidth() {
        this.tableWidth += 1;
    }

    /**
     * Increments the table height by the specified amount.
     *
     * @param tableHeight  Amount to increment the table height
     */
    public void incrementTableHeight(int tableHeight) {
        this.tableHeight += tableHeight;
    }

    /**
     * Increments the table height by 1.
     */
    public void incrementTableHeight() {
        this.tableHeight += 1;
    }

    /**
     * Retrieves the size of the table as an array containing the width and height.
     *
     * @return Array containing the width and height of the table
     */
    public int[] getTableSize() {
        return new int[]{this.tableWidth, this.tableHeight};
    }

    /**
     * Returns a string representation of the HTMLTable object.
     *
     * @return String representation of the HTMLTable object
     */
    @Override
    public String toString() {
        return "HTMLTable [tableData=" + tableData + ", tableWidth=" + tableWidth
                + ", tableHeight=" + tableHeight + "]";
    }
}
