package com.fsw.model;

/**
 * The kinds of file system activity the application can record.
 *
 * <p>{@link #CREATED}, {@link #MODIFIED}, and {@link #DELETED} are detected
 * directly by the watching engine. {@link #RENAMED} is defined for completeness,
 * but the underlying {@link java.nio.file.WatchService} reports a rename as a
 * delete followed by a create rather than a single rename event, so it is not
 * produced in normal operation.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public enum ActivityType {
    /** A new file was created. */
    CREATED,
    /** An existing file's contents were changed. */
    MODIFIED,
    /** A file was removed. */
    DELETED,
    /** A file was renamed (reserved; see the class note). */
    RENAMED
}
