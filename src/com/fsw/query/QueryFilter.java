package com.fsw.query;

import com.fsw.model.ActivityType;

import java.time.LocalDate;

/**
 * A set of optional criteria used to filter stored file events.
 *
 * <p>Every field is optional: a {@code null} or blank field means "do not filter
 * on this attribute." A filter with no criteria set therefore matches all events.
 * When several criteria are set, they are combined with logical AND by the
 * persistence layer.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class QueryFilter {

    private LocalDate startDate;
    private LocalDate endDate;
    private String extension;
    private ActivityType activityType;
    private String directory;

    /** Creates an empty filter that matches all events. */
    public QueryFilter() {
    }

    /**
     * Creates a filter with all criteria specified.
     *
     * @param startDate    earliest event date to include, inclusive (or {@code null})
     * @param endDate      latest event date to include, inclusive (or {@code null})
     * @param extension    file extension to match, dot optional (or {@code null}/blank)
     * @param activityType activity type to match (or {@code null})
     * @param directory    directory path prefix to match (or {@code null}/blank)
     */
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

    /** @return the inclusive start date, or {@code null} for no lower bound */
    public LocalDate getStartDate() {
        return startDate;
    }

    /** @param startDate the inclusive start date, or {@code null} for no lower bound */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /** @return the inclusive end date, or {@code null} for no upper bound */
    public LocalDate getEndDate() {
        return endDate;
    }

    /** @param endDate the inclusive end date, or {@code null} for no upper bound */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /** @return the extension to match, or {@code null}/blank to match any */
    public String getExtension() {
        return extension;
    }

    /** @param extension the extension to match (dot optional), or {@code null}/blank for any */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /** @return the activity type to match, or {@code null} to match any */
    public ActivityType getActivityType() {
        return activityType;
    }

    /** @param activityType the activity type to match, or {@code null} for any */
    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    /** @return the directory prefix to match, or {@code null}/blank to match any */
    public String getDirectory() {
        return directory;
    }

    /** @param directory the directory prefix to match, or {@code null}/blank for any */
    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
