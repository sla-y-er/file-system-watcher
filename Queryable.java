package com.fsw.database;

import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;

import java.util.List;

public interface Queryable {
    List<FileEvent> query(QueryFilter f);
    void exportToCsv(String path);
}
