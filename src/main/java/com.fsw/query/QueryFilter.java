package com.fsw.query;

import com.fsw.model.ActivityType;

import java.time.LocalDate;

public class QueryFilter {
    private LocalDate startDate;
    private LocalDate endDate;
    private String extension;
    private ActivityType activityType;
    private String directory;

    public QueryFilter withDateRange(LocalDate start, LocalDate end) {
        this.startDate = start;
        this.endDate = end;
        return this;
    }

    public QueryFilter withExtension(String ext) {
        this.extension = ext;
        return this;
    }

    public QueryFilter withActivity(ActivityType type) {
        this.activityType = type;
        return this;
    }

    public QueryFilter withDirectory(String directory) {
        this.directory = directory;
        return this;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getExtension() { return extension; }
    public ActivityType getActivityType() { return activityType; }
    public String getDirectory() { return directory; }
}
