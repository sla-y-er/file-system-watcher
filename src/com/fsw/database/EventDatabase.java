package com.fsw.database;

import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite-backed persistence layer for file events.
 *
 * <p>Stores events in a single embedded database file in a {@code file_events}
 * table that is created automatically on {@link #connect()}. Supports inserting
 * events, clearing the store, filtered querying via {@link QueryFilter},
 * computing summary {@link DatabaseStats statistics}, and exporting to CSV. All
 * queries use parameterized SQL, so special characters in file paths cannot break
 * or inject into statements.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class EventDatabase implements Queryable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String dbFilePath;
    private Connection conn;

    /** Creates a database backed by the default {@code eventlog.db} file. */
    public EventDatabase() {
        this("eventlog.db");
    }

    /**
     * Creates a database backed by the given file.
     *
     * @param dbFilePath the SQLite database file path
     */
    public EventDatabase(String dbFilePath) {
        this.dbFilePath = dbFilePath;
    }

    /**
     * Opens the connection and ensures the schema exists.
     *
     * @throws RuntimeException if the JDBC driver is missing or the connection fails
     */
    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
            conn.setAutoCommit(true);
            createTable();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found — make sure sqlite-jdbc jar is on the classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database: " + e.getMessage(), e);
        }
    }

    /** Closes the database connection if it is open; errors are ignored. */
    public void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {}
    }

    /**
     * Creates the {@code file_events} table if absent and backfills any columns
     * missing from an older schema.
     *
     * @throws SQLException if the table cannot be created
     */
    private void createTable() throws SQLException {
        // Create table if it doesn't exist at all
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS file_events (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "file_name TEXT," +
                    "extension TEXT," +
                    "path TEXT," +
                    "activity TEXT," +
                    "event_datetime TEXT)");
        }
        // Add any columns that are missing from an older schema (ALTER TABLE ignores if already present via catch)
        ensureColumn("file_name",       "TEXT");
        ensureColumn("extension",       "TEXT");
        ensureColumn("path",            "TEXT");
        ensureColumn("activity",        "TEXT");
        ensureColumn("event_datetime",  "TEXT");
    }

    /**
     * Adds a column to {@code file_events} if it doesn't already exist. SQLite throws
     * on a duplicate column, which is caught and ignored.
     *
     * @param column the column name
     * @param type   the column's SQL type
     */
    private void ensureColumn(String column, String type) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE file_events ADD COLUMN " + column + " " + type);
        } catch (SQLException ignored) {
            // column already exists — expected and safe to ignore
        }
    }

    /**
     * Inserts a single event.
     *
     * @param event the event to store
     * @throws RuntimeException if the insert fails
     */
    public void insert(FileEvent event) {
        String sql = "INSERT INTO file_events (file_name, extension, path, activity, event_datetime) " +
                     "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.getFileName());
            ps.setString(2, event.getExtension());
            ps.setString(3, event.getFullPath());
            ps.setString(4, event.getActivityType() != null ? event.getActivityType().name() : "");
            ps.setString(5, event.getTimestamp() != null ? event.getTimestamp().format(FMT) : "");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Inserts a batch of events, each committed individually via autocommit.
     *
     * @param events the events to store; {@code null} or empty is a no-op
     */
    public void insertAll(List<FileEvent> events) {
        if (events == null || events.isEmpty()) return;
        for (FileEvent e : events) {
            insert(e);
        }
    }

    /**
     * Deletes all stored events.
     *
     * @throws RuntimeException if the delete fails
     */
    public void clearAll() {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM file_events");
        } catch (SQLException e) {
            throw new RuntimeException("Clear failed: " + e.getMessage(), e);
        }
    }

    /**
     * Computes summary statistics over all stored events using SQL aggregation.
     *
     * @return a snapshot of totals, time span, and per-activity / per-extension counts
     * @throws RuntimeException if a statistics query fails
     */
    public DatabaseStats getStatistics() {
        int total = 0;
        String earliest = null;
        String latest = null;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) AS n, MIN(event_datetime) AS lo, MAX(event_datetime) AS hi " +
                 "FROM file_events WHERE event_datetime IS NOT NULL AND event_datetime <> ''")) {
            if (rs.next()) {
                total    = rs.getInt("n");
                earliest = rs.getString("lo");
                latest   = rs.getString("hi");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Statistics query failed: " + e.getMessage(), e);
        }

        Map<String, Integer> byActivity = groupCount(
            "SELECT activity AS k, COUNT(*) AS n FROM file_events " +
            "GROUP BY activity ORDER BY n DESC");

        Map<String, Integer> byExtension = groupCount(
            "SELECT CASE WHEN extension IS NULL OR extension = '' THEN '(none)' ELSE extension END AS k, " +
            "COUNT(*) AS n FROM file_events GROUP BY k ORDER BY n DESC");

        return new DatabaseStats(total, earliest, latest, byActivity, byExtension);
    }

    /**
     * Runs a "SELECT key, count" grouping query and returns an insertion-ordered map.
     *
     * @param sql a query selecting a key column {@code k} and a count column {@code n}
     * @return the counts keyed by {@code k}, preserving the query's ordering
     * @throws RuntimeException if the query fails
     */
    private Map<String, Integer> groupCount(String sql) {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString("k");
                map.put((key == null || key.isBlank()) ? "(none)" : key, rs.getInt("n"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Statistics query failed: " + e.getMessage(), e);
        }
        return map;
    }

    /**
     * Returns the stored events matching the given filter, newest first.
     *
     * @param f the criteria to filter by; {@code null} or an empty filter matches all
     * @return the matching events ordered by timestamp descending
     * @throws RuntimeException if the query fails
     */
    @Override
    public List<FileEvent> query(QueryFilter f) {
        StringBuilder sql = new StringBuilder(
                "SELECT file_name, extension, path, activity, event_datetime " +
                "FROM file_events WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (f != null) {
            if (f.getStartDate() != null) {
                sql.append(" AND event_datetime >= ?");
                params.add(f.getStartDate().atStartOfDay().format(FMT));
            }
            if (f.getEndDate() != null) {
                sql.append(" AND event_datetime <= ?");
                params.add(f.getEndDate().atTime(23, 59, 59).format(FMT));
            }
            if (f.getExtension() != null && !f.getExtension().isBlank()) {
                sql.append(" AND LOWER(extension) = LOWER(?)");
                params.add(f.getExtension().replace(".", "").trim());
            }
            if (f.getActivityType() != null) {
                sql.append(" AND activity = ?");
                params.add(f.getActivityType().name());
            }
            if (f.getDirectory() != null && !f.getDirectory().isBlank()) {
                sql.append(" AND path LIKE ?");
                params.add(f.getDirectory() + "%");
            }
        }

        sql.append(" ORDER BY event_datetime DESC");

        List<FileEvent> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FileEvent e = new FileEvent();
                    e.setFileName(rs.getString("file_name"));
                    e.setExtension(rs.getString("extension"));
                    e.setFullPath(rs.getString("path"));
                    String activity = rs.getString("activity");
                    if (activity != null && !activity.isBlank()) {
                        try { e.setActivityType(ActivityType.valueOf(activity)); }
                        catch (IllegalArgumentException ignored) {}
                    }
                    String dt = rs.getString("event_datetime");
                    if (dt != null && !dt.isBlank()) {
                        try { e.setTimestamp(LocalDateTime.parse(dt, FMT)); }
                        catch (Exception ignored) {}
                    }
                    results.add(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
        return results;
    }

    /**
     * Exports every stored event to a CSV file at the given path.
     *
     * @param path the destination file path
     * @throws RuntimeException if the file cannot be written
     */
    @Override
    public void exportToCsv(String path) {
        List<FileEvent> rows = query(new QueryFilter());
        try (java.io.FileWriter writer = new java.io.FileWriter(path)) {
            writer.write("file_name,extension,path,activity,event_datetime\n");
            for (FileEvent e : rows) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        safe(e.getFileName()), safe(e.getExtension()),
                        safe(e.getFullPath()), e.getActivityType(), e.getTimestamp()));
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("CSV export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Escapes a value for CSV by doubling embedded quotes; null becomes empty.
     *
     * @param v the raw value
     * @return the escaped value, never {@code null}
     */
    private String safe(String v) {
        return v == null ? "" : v.replace("\"", "\"\"");
    }
}
