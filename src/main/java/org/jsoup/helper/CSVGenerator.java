package org.jsoup.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.nodes.HTMLTable;
import org.jsoup.nodes.TableCell;

/**
 * Helper class to generate CSV files from a list of HTML tables.
 * 
 * This class provides a method to generate CSV files from a list of HTML tables and save them to the specified directory with the given file name prefix.
 * Each HTML table will be saved as a separate CSV file. The content of each table cell will be written as a separate cell in the CSV file.
 * Rows are separated by a line break, and columns are separated by a comma.
 * 
 * Example Usage:
 * CSVGenerator csvGenerator = new CSVGenerator();
 * List<HTMLTable> tables = ...; // populate the list of HTML tables
 * String filesSavePath = "/csv-files"; // specify the directory to save the CSV files
 * String filesName = "table"; // specify the file name prefix for the CSV files
 * csvGenerator.generateCSV(tables, filesSavePath, filesName);
 * 
 * @author Noura Alroomi
 */
public class CSVGenerator {

    /**
     * Generates CSV files from a list of HTML tables and saves them to the specified directory with the given file name prefix.
     * Each HTML table will be saved as a separate CSV file. The content of each table cell will be written as a separate cell in the CSV file.
     * Rows are separated by a line break, and columns are separated by a comma.
     * 
     * @param tables        List of HTMLTable objects representing the tables to write into CSV files
     * @param filesSavePath Directory path to save the generated CSV files
     * @param filesName     Prefix for the file names of the CSV files
     * 
     * @throws IOException  If an I/O error occurs while creating or writing to the files
     */
    public void generateCSV(List<HTMLTable> tables, String filesSavePath, String filesName) throws IOException {
        // validate the inputs
        Validate.notEmpty(filesSavePath, "filesSavePath must be set to CSVs save path");
        Validate.notEmpty(filesName, "filesName must be set to CSVs save prefix file name");
        Validate.ensureNotNull(tables, "list of HTML tables must be set");
        Validate.isTrue(tables.size() > 0, "list of HTML tables is empty");

        // create the directory if it doesn't exist
        File directory = new File(filesSavePath);
        FileUtils.forceMkdir(directory);


        // process each HTML table
        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            HTMLTable table = tables.get(tableIndex);
            List<List<TableCell>> tableData = table.getTableData();

            // generate the file name and path for the CSV file
            String fileName = filesName + "_" + (tableIndex + 1);
            String filePath = filesSavePath + File.separator + fileName + ".csv";

            // create a FileWriter to write to the CSV file
            FileWriter writer = new FileWriter(filePath);

            // iterate over each row in the table data
            for (List<TableCell> row : tableData) {
                StringBuilder rowBuilder = new StringBuilder();

                // iterate over each cell in the row
                for (TableCell cell : row) {
                    // get the cell value and replace newline and comma characters
                    String cellValue = cell.getCellValue().replaceAll("\\n", " ");
                    cellValue = cellValue.replaceAll(",", " ");
                    rowBuilder.append(cellValue).append(",");
                }

                // remove the trailing comma and add a line break
                rowBuilder.deleteCharAt(rowBuilder.length() - 1);
                rowBuilder.append("\n");

                // write the row to the CSV file
                writer.write(rowBuilder.toString());
            }

            // close the FileWriter
            writer.close();
        }
    }
}
