package com.fsw.report;

import com.fsw.model.FileEvent;

import java.util.List;

public interface ReportWriter {

    void printToConsole(List<FileEvent> rows);

    void writeToFile(List<FileEvent> rows, String name);
}