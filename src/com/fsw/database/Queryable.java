package com.fsw.database;

import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;

import java.util.List;

/**
 * Abstraction over the persistence layer that supplies stored file events.
 *
 * <p>Decoupling callers from the concrete {@link EventDatabase} implementation
 * keeps the query and reporting code independent of the storage technology and
 * makes the layer easy to substitute (for example, with an in-memory store in
 * tests).
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public interface Queryable {

    /**
     * Returns the stored events that match the given filter.
     *
     * @param f the criteria to filter by; a filter with no criteria set
     *          (or {@code null}) matches every stored event
     * @return the matching events, ordered most-recent first
     */
    List<FileEvent> query(QueryFilter f);

    /**
     * Exports every stored event to a CSV file at the given path.
     *
     * @param path the destination file path (including the {@code .csv} name)
     */
    void exportToCsv(String path);
}
