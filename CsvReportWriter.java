package com.fsw.report;

import com.fsw.model.FileEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvReportWriter
        implements ReportWriter {

    @Override
    public void printToConsole(
            List<FileEvent> rows
    ) {

        if (rows == null || rows.isEmpty()) {

            System.out.println(
                    "No events found."
            );

            return;
        }

        System.out.println(
                buildCsvHeader()
        );

        for (FileEvent e : rows) {

            System.out.println(
                    buildCsvRow(e)
            );
        }
    }

    @Override
    public void write(
            List<FileEvent> rows,
            File outputFile
    ) {

        if (rows == null) {

            throw new IllegalArgumentException(
                    "Rows cannot be null."
            );
        }

        try (FileWriter writer =
                     new FileWriter(outputFile)) {

            writer.write(
                    buildCsvHeader()
            );

            writer.write("\n");

            for (FileEvent e : rows) {

                writer.write(
                        buildCsvRow(e)
                );

                writer.write("\n");
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to write CSV report: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private String buildCsvHeader() {

        return "fileName,"
                + "extension,"
                + "fullPath,"
                + "activityType,"
                + "timestamp";
    }

    private String buildCsvRow(
            FileEvent e
    ) {

        return String.format(
                "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                sanitize(e.getFileName()),
                sanitize(e.getExtension()),
                sanitize(e.getFullPath()),
                e.getActivityType(),
                e.getTimestamp()
        );
    }

    private String sanitize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.replace("\"", "\"\"");
    }
}