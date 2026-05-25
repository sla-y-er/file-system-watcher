package com.fsw.query;

import com.fsw.model.ActivityType;

import java.time.LocalDate;

public class QueryFilter {

    private LocalDate startDate;
    private LocalDate endDate;
    private String extension;
    private ActivityType activityType;
    private String directory;

    public QueryFilter withDateRange(
            LocalDate start,
            LocalDate end
    ) {

        this.startDate = start;
        this.endDate = end;

        return this;
    }

    public QueryFilter withExtension(
            String ext
    ) {

        if (ext == null || ext.isBlank()) {
            this.extension = null;
            return this;
        }

        this.extension =
                ext.replace(".", "")
                        .trim()
                        .toLowerCase();

        return this;
    }

    public QueryFilter withActivity(
            ActivityType type
    ) {

        this.activityType = type;
        return this;
    }

    public QueryFilter withDirectory(
            String directory
    ) {

        if (directory == null ||
                directory.isBlank()) {

            this.directory = null;
            return this;
        }

        this.directory =
                directory.trim();

        return this;
    }

    public boolean hasDateRange() {

        return startDate != null &&
                endDate != null;
    }

    public boolean hasExtension() {

        return extension != null &&
                !extension.isBlank();
    }

    public boolean hasActivityType() {

        return activityType != null;
    }

    public boolean hasDirectory() {

        return directory != null &&
                !directory.isBlank();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getExtension() {
        return extension;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public String getDirectory() {
        return directory;
    }

    @Override
    public String toString() {

        return "QueryFilter{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", extension='" + extension + '\'' +
                ", activityType=" + activityType +
                ", directory='" + directory + '\'' +
                '}';
    }
}