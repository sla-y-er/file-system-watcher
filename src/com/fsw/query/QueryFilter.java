package com.fsw.query;

import com.fsw.model.ActivityType;

import java.time.LocalDate;

public class QueryFilter {

    private LocalDate startDate;
    private LocalDate endDate;
    private String extension;
    private ActivityType activityType;
    private String directory;

    public QueryFilter() {
    }

    public QueryFilter(LocalDate startDate,
                       LocalDate endDate,
                       String extension,
                       ActivityType activityType,
                       String directory) {

        this.startDate = startDate;
        this.endDate = endDate;
        this.extension = extension;
        this.activityType = activityType;
        this.directory = directory;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}