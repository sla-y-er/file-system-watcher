package com.fsw.query;

import com.fsw.database.Queryable;
import com.fsw.model.FileEvent;
import com.fsw.report.ReportWriter;

import java.io.File;
import java.util.List;

/**
 * Coordinates running queries against the data store and producing reports.
 *
 * <p>The engine validates a {@link QueryFilter}, retrieves matching events from a
 * {@link Queryable} store, and hands the results to a {@link ReportWriter} for
 * console display or file export. It depends only on those interfaces, not on any
 * concrete storage or output implementation.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class QueryEngine {

    private Queryable db;
    private ReportWriter writer;

    /**
     * Creates an engine wired to a data store and a report writer.
     *
     * @param db     the source of stored events
     * @param writer the report output strategy
     */
    public QueryEngine(Queryable db, ReportWriter writer) {
        this.db = db;
        this.writer = writer;
    }

    /**
     * Validates and runs a query, printing the matching events to the console.
     *
     * @param q the filter criteria; an invalid or {@code null} filter is rejected
     */
    public void runQuery(QueryFilter q) {
        if (!validateFilter(q)) {
            System.out.println("Invalid query filter.");
            return;
        }

        List<FileEvent> rows = db.query(q);

        if (rows.isEmpty()) {
            System.out.println("No matching events found.");
            return;
        }

        writer.printToConsole(rows);
    }

    /**
     * Exports the given rows to a CSV file.
     *
     * @param rows the events to export; nothing is written if {@code null} or empty
     */
    public void promptExport(List<FileEvent> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("No rows available to export.");
            return;
        }

        writer.writeToFile(rows, "query_results.csv");
        System.out.println("Results exported successfully.");
    }

    /**
     * Prepares an email with the given file as an attachment (mock).
     *
     * @param file the file to attach; must exist
     */
    public void promptEmail(File file) {
        if (file == null || !file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        System.out.println("Email feature is currently running in mock mode.");
        System.out.println("Prepared attachment: " + file.getName());
    }

    /**
     * Checks that a filter is usable: non-null and, if both dates are set, that the
     * start date is not after the end date.
     *
     * @param q the filter to validate
     * @return {@code true} if the filter can be used to query
     */
    private boolean validateFilter(QueryFilter q) {
        if (q == null) {
            return false;
        }

        if (q.getStartDate() != null &&
                q.getEndDate() != null &&
                q.getStartDate().isAfter(q.getEndDate())) {
            return false;
        }

        return true;
    }
}
