package com.fsw.query;

import com.fsw.database.Queryable;
import com.fsw.model.FileEvent;
import com.fsw.report.ReportWriter;

import java.io.File;
import java.util.List;

public class QueryEngine {

    private Queryable db;
    private ReportWriter writer;

    public QueryEngine(Queryable db, ReportWriter writer) {
        this.db = db;
        this.writer = writer;
    }

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

    public void promptExport(List<FileEvent> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("No rows available to export.");
            return;
        }

        writer.writeToFile(rows, "query_results.csv");
        System.out.println("Results exported successfully.");
    }

    public void promptEmail(File file) {
        if (file == null || !file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        System.out.println("Email feature is currently running in mock mode.");
        System.out.println("Prepared attachment: " + file.getName());
    }

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