package com.fsw.report;

import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;

import java.util.List;

public class CsvReportWriter implements ReportWriter {
    private String outputDirectory;

    @Override
    public void printToConsole(List<FileEvent> rows) {}

    @Override
    public void writeToFile(List<FileEvent> rows, String name) {}

    private String buildCsvHeader() { return null; }

    private String buildMetaBlock(QueryFilter q) { return null; }
}