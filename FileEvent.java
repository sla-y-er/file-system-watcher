package com.fsw.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class FileEvent {

    private String fileName;
    private String extension;
    private String fullPath;
    private ActivityType activityType;
    private LocalDateTime timestamp;

    public FileEvent() {
    }

    public FileEvent(
            String fileName,
            String extension,
            String fullPath,
            ActivityType activityType,
            LocalDateTime timestamp
    ) {

        this.fileName = fileName;
        this.extension = extension;
        this.fullPath = fullPath;
        this.activityType = activityType;
        this.timestamp = timestamp;
    }

    public String getFileName() {

        return fileName;
    }

    public String getExtension() {

        return extension;
    }

    public String getFullPath() {

        return fullPath;
    }

    public ActivityType getActivityType() {

        return activityType;
    }

    public LocalDateTime getTimestamp() {

        return timestamp;
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    public void setExtension(String extension) {

        this.extension = extension;
    }

    public void setFullPath(String fullPath) {

        this.fullPath = fullPath;
    }

    public void setActivityType(
            ActivityType activityType
    ) {

        this.activityType = activityType;
    }

    public void setTimestamp(
            LocalDateTime timestamp
    ) {

        this.timestamp = timestamp;
    }

    @Override
    public String toString() {

        return "FileEvent{"
                + "fileName='"
                + fileName
                + '\''
                + ", extension='"
                + extension
                + '\''
                + ", fullPath='"
                + fullPath
                + '\''
                + ", activityType="
                + activityType
                + ", timestamp="
                + timestamp
                + '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof FileEvent)) {

            return false;
        }

        FileEvent that = (FileEvent) o;

        return Objects.equals(
                fileName,
                that.fileName
        )
                && Objects.equals(
                extension,
                that.extension
        )
                && Objects.equals(
                fullPath,
                that.fullPath
        )
                && activityType
                == that.activityType
                && Objects.equals(
                timestamp,
                that.timestamp
        );
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                fileName,
                extension,
                fullPath,
                activityType,
                timestamp
        );
    }
}