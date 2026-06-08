package com.fsw.database;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot of summary statistics computed from the event database.
 *
 * <p>Holds the total event count, the time span the events cover, and counts
 * grouped by activity type and by file extension. The grouping maps preserve
 * insertion order (descending by count) so they can be displayed directly.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class DatabaseStats {

    private final int total;
    private final String earliest;
    private final String latest;
    private final Map<String, Integer> byActivity;
    private final Map<String, Integer> byExtension;

    /**
     * Creates a statistics snapshot.
     *
     * @param total       the total number of stored events
     * @param earliest    the earliest event timestamp as text, or {@code null} if none
     * @param latest      the latest event timestamp as text, or {@code null} if none
     * @param byActivity  counts keyed by activity type; {@code null} is treated as empty
     * @param byExtension counts keyed by file extension; {@code null} is treated as empty
     */
    public DatabaseStats(int total, String earliest, String latest,
                         Map<String, Integer> byActivity, Map<String, Integer> byExtension) {
        this.total = total;
        this.earliest = earliest;
        this.latest = latest;
        this.byActivity = byActivity != null ? byActivity : new LinkedHashMap<>();
        this.byExtension = byExtension != null ? byExtension : new LinkedHashMap<>();
    }

    /** @return the total number of stored events */
    public int getTotal() { return total; }

    /** @return the earliest event timestamp as text, or {@code null} if there are no events */
    public String getEarliest() { return earliest; }

    /** @return the latest event timestamp as text, or {@code null} if there are no events */
    public String getLatest() { return latest; }

    /** @return event counts keyed by activity type, in descending-count order (never {@code null}) */
    public Map<String, Integer> getByActivity() { return byActivity; }

    /** @return event counts keyed by file extension, in descending-count order (never {@code null}) */
    public Map<String, Integer> getByExtension() { return byExtension; }
}
