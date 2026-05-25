package com.fsw.report;

import com.fsw.model.FileEvent;

import java.io.File;
import java.util.List;

public interface ReportWriter {

    void printToConsole(
            List<FileEvent> rows
    );

    void write(
            List<FileEvent> rows,
            File outputFile
    );
}
