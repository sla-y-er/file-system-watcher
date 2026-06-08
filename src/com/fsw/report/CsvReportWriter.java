package com.fsw.report;

import com.fsw.model.FileEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * {@link ReportWriter} that prints events to the console and writes them to CSV files.
 *
 * <p>Each CSV row contains the file name, extension, path, activity, and timestamp.
 * Field values are escaped so embedded quotes do not corrupt the output, and
 * missing values are written as empty strings rather than the text {@code "null"}.
 * The output directory is created on construction if it does not already exist.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class CsvReportWriter implements ReportWriter {

    private final String outputDirectory;

    /** Creates a writer that writes into a default {@code reports} directory. */
    public CsvReportWriter() {
        this("reports");
    }

    /**
     * Creates a writer that writes into the given directory, creating it if needed.
     *
     * @param outputDirectory the directory in which {@link #writeToFile} places files
     */
    public CsvReportWriter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Prints a formatted listing of the events to standard output.
     *
     * @param rows the events to print
     */
    @Override
    public void printToConsole(List<FileEvent> rows) {
        System.out.println("\n========== FILE EVENT REPORT ==========");
        for (FileEvent event : rows) {
            System.out.println(
                    event.getTimestamp() + " | " +
                    event.getActivityType() + " | " +
                    event.getFileName() + " | " +
                    event.getExtension() + " | " +
                    event.getFullPath()
            );
        }
        System.out.println("Total events: " + rows.size());
        System.out.println("=======================================\n");
    }

    /**
     * Writes the events to a CSV file named {@code name} inside the output directory.
     *
     * @param rows the events to write
     * @param name the file name (placed inside the configured output directory)
     * @throws RuntimeException if the file cannot be written
     */
    @Override
    public void writeToFile(List<FileEvent> rows, String name) {
        String filePath = outputDirectory + File.separator + name;
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("file_name,extension,path,activity,event_datetime\n");
            for (FileEvent event : rows) {
                writer.write(formatRow(event));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not write CSV report: " + e.getMessage(), e);
        }
    }

    /**
     * Writes query results to a CSV file with a human-readable query summary at the top.
     *
     * @param rows            the result rows to export
     * @param fullOutputPath  absolute file path to write (including .csv extension)
     * @param queryDescription multi-line description of the query that produced the rows
     * @throws RuntimeException if the file cannot be written
     */
    public void writeToFileWithHeader(List<FileEvent> rows, String fullOutputPath, String queryDescription) {
        try (FileWriter writer = new FileWriter(fullOutputPath)) {
            // query info block (prefixed with # so it reads as comments)
            if (queryDescription != null && !queryDescription.isBlank()) {
                for (String line : queryDescription.split("\n", -1)) {
                    writer.write(line + "\n");
                }
            }
            // column headers
            writer.write("file_name,extension,path,activity,event_datetime\n");
            for (FileEvent event : rows) {
                writer.write(formatRow(event));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not write CSV export: " + e.getMessage(), e);
        }
    }

    /**
     * Formats a single event as one escaped CSV line (with trailing newline).
     *
     * @param event the event to format
     * @return the CSV line representing the event
     */
    private String formatRow(FileEvent event) {
        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                safe(event.getFileName()),
                safe(event.getExtension()),
                safe(event.getFullPath()),
                event.getActivityType() != null ? event.getActivityType().name() : "",
                event.getTimestamp() != null ? event.getTimestamp().toString() : "");
    }

    /**
     * Escapes a value for CSV by doubling embedded quotes; null becomes empty.
     *
     * @param value the raw value
     * @return the escaped value, never {@code null}
     */
    private String safe(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
