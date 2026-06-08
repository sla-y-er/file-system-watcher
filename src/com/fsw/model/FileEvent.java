package com.fsw.model;

import java.time.LocalDateTime;

/**
 * Immutable-style data object representing a single file system event.
 *
 * <p>A {@code FileEvent} captures everything the application records about one
 * change to a watched directory: the file's name and extension, its absolute
 * path, the kind of {@link ActivityType activity} that occurred, and the time
 * it was observed. Instances are produced by the file-watching engine and are
 * persisted, queried, displayed, and exported throughout the application.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class FileEvent {
    private String fileName;
    private String extension;
    private String fullPath;
    private ActivityType activityType;
    private LocalDateTime timestamp;

    /** Creates an empty event with all fields unset; used when reading rows back from storage. */
    public FileEvent() {
    }

    /**
     * Creates a fully populated file event.
     *
     * @param fileName     the file's name (e.g. {@code report.txt})
     * @param extension    the file's extension without the dot (e.g. {@code txt})
     * @param fullPath     the file's absolute path
     * @param activityType the kind of change that occurred
     * @param timestamp    the moment the event was observed
     */
    public FileEvent(String fileName, String extension, String fullPath, ActivityType activityType, LocalDateTime timestamp) {
        this.fileName = fileName;
        this.extension = extension;
        this.fullPath = fullPath;
        this.activityType = activityType;
        this.timestamp = timestamp;
    }

    /** @return the file's name, or {@code null} if unset */
    public String getFileName() { return fileName; }

    /** @return the file's extension without the leading dot, or {@code null} if unset */
    public String getExtension() { return extension; }

    /** @return the file's absolute path, or {@code null} if unset */
    public String getFullPath() { return fullPath; }

    /** @return the kind of change that occurred, or {@code null} if unset */
    public ActivityType getActivityType() { return activityType; }

    /** @return the time the event was observed, or {@code null} if unset */
    public LocalDateTime getTimestamp() { return timestamp; }

    /** @param fileName the file's name */
    public void setFileName(String fileName) { this.fileName = fileName; }

    /** @param extension the file's extension (without the leading dot) */
    public void setExtension(String extension) { this.extension = extension; }

    /** @param fullPath the file's absolute path */
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }

    /** @param activityType the kind of change that occurred */
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }

    /** @param timestamp the time the event was observed */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
