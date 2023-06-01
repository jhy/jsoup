package org.jsoup.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.nodes.HTMLTable;
import org.jsoup.nodes.TableCell;

/**
* Helper class to generate a spreadsheet from a list of HTML tables.
*
* This class provides a method to generate a spreadsheet from a list of HTML tables and save it to the specified file path.
* The generated spreadsheet will contain each HTML table as a separate sheet.
* The isMaintainSpan flag can be used to control whether spanned cells should be maintained or not.
* 
* Example Usage:
* SpreadsheetGenerator spreadsheetGenerator = new SpreadsheetGenerator();
* List<HTMLTable> tables = ...; // populate the list of HTML tables
* String fileSavePath = "spreadsheets/spreadsheet.xlsx"; // specify the file path to save the spreadsheet
* boolean isMaintainSpan = true; // set whether to maintain spanned cells
* spreadsheetGenerator.generateSpreadsheet(tables, fileSavePath, isMaintainSpan);
*
* @author Noura Alroomi
*/
public class SpreadsheetGenerator {

    /**
    * Generates a spreadsheet from a list of HTML tables and saves it to the specified file path.
    * Each HTML table will be saved as a separate sheet in the spreadsheet.
    * The isMaintainSpan flag can be used to maintain spanned cells in the spreadsheet.
    *
    * @param tables List of HTMLTable objects representing the tables to write into the spreadsheet
    * @param fileSavePath File path to save the generated spreadsheet
    * @param isMaintainSpan Boolean flag to maintain spanned cells in the spreadsheet
    *
    * @throws IOException If an I/O error occurs while creating or writing to the file
    */
    public void generateSpreadsheet(List<HTMLTable> tables, String fileSavePath, Boolean isMaintainSpan) throws IOException {
        // validate the inputs
        Validate.notNull(isMaintainSpan, "isMaintainSpan value must be set");
        Validate.notEmpty(fileSavePath, "fileSavePath must be set to Spreadsheet save path");
        Validate.isTrue(fileSavePath.endsWith(".xlsx"), "fileSavePath must have .xlsx extension");
        Validate.ensureNotNull(tables, "list of HTML tables must be set");
        Validate.isTrue(tables.size() > 0, "list of HTML tables is empty");

        // create a new workbook
        Workbook workbook = new XSSFWorkbook();

        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            // get the HTMLTable object
            HTMLTable htmlTable = tables.get(tableIndex);

            // create a new sheet for the table
            Sheet sheet = workbook.createSheet("Table " + (tableIndex + 1));

            // get the table data, width, and height
            List<List<TableCell>> table = htmlTable.getTableData();
            int tableWidth = htmlTable.getTableWidth();
            int tableHeight = htmlTable.getTableHeight();

            // map to keep track of rowspan cells
            Map<Integer, Integer> rowspanList = new HashMap<>();

            for (int rowIndex = 0; rowIndex < tableHeight; rowIndex++) {
                // get the current row from the table data
                List<TableCell> row = table.get(rowIndex);

                // create a new row in the sheet
                Row sheetRow = sheet.createRow(rowIndex);

                for (int colIndex = 0; colIndex < tableWidth; colIndex++) {
                    // get the current cell
                    TableCell cell = row.get(colIndex);

                    // create a new cell in the row
                    Cell sheetCell = sheetRow.createCell(colIndex);

                    // check if the cell needs to be skipped due to rowspan
                    if (isMaintainSpan && rowspanList.containsKey(colIndex)) {
                        int rowspans = rowspanList.get(colIndex);
                        rowspanList.put(colIndex, --rowspans);
                        if (rowspans == 0) {
                            rowspanList.remove(colIndex);
                        }
                        continue;
                    }

                    // check if the cell has colspan or rowspan
                    if (isMaintainSpan && (cell.getCellColSpan() > 1 || cell.getCellRowSpan() > 1)) {
                        // merge cells with colspan and rowspan
                        CellRangeAddress range = new CellRangeAddress(rowIndex, rowIndex + cell.getCellRowSpan() - 1,
                                colIndex, colIndex + cell.getCellColSpan() - 1);
                        sheet.addMergedRegion(range);

                        // set the value in the top-left merged cell
                        Cell firstCell = sheet.getRow(rowIndex).getCell(colIndex);
                        firstCell.setCellValue(cell.getCellValue());

                        // apply cell style to the merged region
                        CellStyle cellStyle = workbook.createCellStyle();
                        cellStyle.setWrapText(true);
                        sheetCell.setCellStyle(cellStyle);

                        // update rowspanList if rowspan > 1
                        if (cell.getCellRowSpan() > 1) {
                            for (int colspanIndex = 0; colspanIndex < cell.getCellColSpan(); colspanIndex++) {
                                rowspanList.put(colIndex + colspanIndex, cell.getCellRowSpan() - 1);
                            }
                        }

                        // skip the columns covered by colspan
                        if (cell.getCellColSpan() > 1 && cell.getCellRowSpan() == 1) {
                            colIndex += cell.getCellColSpan() - 1;
                        }
                    } else {
                        // no colspan or rowspan, set cell value
                        sheetCell.setCellValue(cell.getCellValue());

                        // apply cell style with wrap text
                        CellStyle cellStyle = workbook.createCellStyle();
                        cellStyle.setWrapText(true);
                        sheetCell.setCellStyle(cellStyle);
                    }
                }
            }

            // autosize columns to fit the content
            for (int colIndex = 0; colIndex < tableWidth; colIndex++) {
                sheet.autoSizeColumn(colIndex);
            }
        }

        // create the file path if it doesn't exist
        File file = new File(fileSavePath);
        FileUtils.forceMkdirParent(file);
        FileUtils.touch(file);

        // write the workbook to the file
        FileOutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        workbook.close();
        
    }
}
