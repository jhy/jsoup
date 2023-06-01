package org.jsoup.nodes;

/**
 * Represents a cell within an HTML table.
 * 
 * @author Noura Alroomi
 */
public class TableCell {

    String cellType;
    String cellValue;
    int cellColSpan = 1;
    int cellRowSpan = 1;

    /**
     * Constructs a TableCell object with the provided cell type and cell value.
     *
     * @param cellType   Type of the cell (e.g., "td", "th")
     * @param cellValue  Value of the cell
     */
    public TableCell(String cellType, String cellValue) {
        this.cellType = cellType;
        this.cellValue = cellValue;
        this.cellColSpan = 1;
        this.cellRowSpan = 1;
    }

    /**
     * Constructs a TableCell object with the provided cell type, cell value, column span, and row span.
     *
     * @param cellType      Type of the cell (e.g., "td", "th")
     * @param cellValue     Value of the cell
     * @param cellColSpan   Column span of the cell
     * @param cellRowSpan   Row span of the cell
     */
    public TableCell(String cellType, String cellValue, int cellColSpan, int cellRowSpan) {
        this.cellType = cellType;
        this.cellValue = cellValue;
        this.cellColSpan = cellColSpan;
        this.cellRowSpan = cellRowSpan;
    }
    
    /**
     * Retrieves the type of the cell.
     *
     * @return Type of the cell
     */
    public String getCellType() {
        return cellType;
    }

    /**
     * Sets the type of the cell.
     *
     * @param cellType  Type of the cell
     */
    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    /**
     * Retrieves the value of the cell.
     *
     * @return Value of the cell
     */
    public String getCellValue() {
        return cellValue;
    }

    /**
     * Sets the value of the cell.
     *
     * @param cellValue  Value of the cell
     */
    public void setCellValue(String cellValue) {
        this.cellValue = cellValue;
    }

    /**
     * Retrieves the column span of the cell.
     *
     * @return Column span of the cell
     */
    public int getCellColSpan() {
        return cellColSpan;
    }

    /**
     * Sets the column span of the cell.
     *
     * @param cellColSpan  Column span of the cell
     */
    public void setCellColSpan(int cellColSpan) {
        this.cellColSpan = cellColSpan;
    }

    /**
     * Retrieves the row span of the cell.
     *
     * @return Row span of the cell
     */
    public int getCellRowSpan() {
        return cellRowSpan;
    }

    /**
     * Sets the row span of the cell.
     *
     * @param cellRowSpan  Row span of the cell
     */
    public void setCellRowSpan(int cellRowSpan) {
        this.cellRowSpan = cellRowSpan;
    }

    /**
     * Returns a string representation of the TableCell object.
     *
     * @return String representation of the TableCell object
     */
    @Override
    public String toString() {
        return "TableCell [cellType=" + cellType + ", cellValue=" + cellValue + ", cellColSpan=" + cellColSpan
                + ", cellRowSpan=" + cellRowSpan + "]";
    }
}
