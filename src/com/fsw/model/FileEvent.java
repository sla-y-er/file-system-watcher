package com.fsw.model;

import java.time.LocalDateTime;

public class FileEvent {
    private String fileName;
    private String extension;
    private String fullPath;
    private ActivityType activityType;
    private LocalDateTime timestamp;

    public FileEvent() {
    }

    public FileEvent(String fileName, String extension, String fullPath, ActivityType activityType, LocalDateTime timestamp) {
        this.fileName = fileName;
        this.extension = extension;
        this.fullPath = fullPath;
        this.activityType = activityType;
        this.timestamp = timestamp;
    }

    public String getFileName() { return fileName; }
    public String getExtension() { return extension; }
    public String getFullPath() { return fullPath; }
    public ActivityType getActivityType() { return activityType; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setExtension(String extension) { this.extension = extension; }
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
