package com.fsw.report;

import com.fsw.model.FileEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvReportWriter implements ReportWriter {

    private String outputDirectory;

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
            writer.write("fileName,extension,fullPath,activityType,timestamp\n");

            for (FileEvent event : rows) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        safe(event.getFileName()),
                        safe(event.getExtension()),
                        safe(event.getFullPath()),
                        event.getActivityType(),
                        event.getTimestamp()
                ));
            }

            System.out.println("CSV report saved to: " + filePath);

        } catch (IOException e) {
            throw new RuntimeException("Could not write CSV report: " + e.getMessage(), e);
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\"", "\"\"");
    }
}