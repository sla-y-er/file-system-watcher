package com.fsw.database;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot of summary statistics computed from the event database.
 * Maps preserve insertion order (descending by count) for display.
 */
public class DatabaseStats {

    private final int total;
    private final String earliest;
    private final String latest;
    private final Map<String, Integer> byActivity;
    private final Map<String, Integer> byExtension;

    public DatabaseStats(int total, String earliest, String latest,
                         Map<String, Integer> byActivity, Map<String, Integer> byExtension) {
        this.total = total;
        this.earliest = earliest;
        this.latest = latest;
        this.byActivity = byActivity != null ? byActivity : new LinkedHashMap<>();
        this.byExtension = byExtension != null ? byExtension : new LinkedHashMap<>();
    }

    public int getTotal() { return total; }
    public String getEarliest() { return earliest; }
    public String getLatest() { return latest; }
    public Map<String, Integer> getByActivity() { return byActivity; }
    public Map<String, Integer> getByExtension() { return byExtension; }
}
