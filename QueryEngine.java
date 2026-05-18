package com.fsw.query;

import com.fsw.database.Queryable;
import com.fsw.model.FileEvent;
import com.fsw.report.ReportWriter;

import java.io.File;
import java.util.List;

public class QueryEngine {
    private Queryable db;
    private ReportWriter writer;

    public void runQuery(QueryFilter q) {}

    public void promptExport(List<FileEvent> rows) {}

    public void promptEmail(File file) {}

    private boolean validateFilter(QueryFilter q) { return true; }
}