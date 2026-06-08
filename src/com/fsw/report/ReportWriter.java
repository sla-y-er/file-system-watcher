package com.fsw.report;

import com.fsw.model.FileEvent;

import java.util.List;

/**
 * Strategy for outputting a collection of file events as a report.
 *
 * <p>Implementations decide the destination and format (for example, a console
 * listing or a CSV file). Keeping this behind an interface lets the query layer
 * produce reports without depending on any particular output format.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public interface ReportWriter {

    /**
     * Prints a human-readable listing of the given events to the console.
     *
     * @param rows the events to print
     */
    void printToConsole(List<FileEvent> rows);

    /**
     * Writes the given events to a file.
     *
     * @param rows the events to write
     * @param name the destination file name
     */
    void writeToFile(List<FileEvent> rows, String name);
}
