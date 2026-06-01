package com.fsw.report;

import com.fsw.model.FileEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvReportWriter implements ReportWriter {

    private final String outputDirectory;

    public CsvReportWriter() {
        this("reports");
    }

    public CsvReportWriter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

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
     * @param rows            the result rows to export
     * @param fullOutputPath  absolute file path to write (including .csv extension)
     * @param queryDescription multi-line description of the query that produced the rows
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

    private String formatRow(FileEvent event) {
        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                safe(event.getFileName()),
                safe(event.getExtension()),
                safe(event.getFullPath()),
                event.getActivityType() != null ? event.getActivityType().name() : "",
                event.getTimestamp() != null ? event.getTimestamp().toString() : "");
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
