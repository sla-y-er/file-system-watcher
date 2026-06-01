package com.fsw.database;

import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EventDatabase implements Queryable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String dbFilePath;
    private Connection conn;

    public EventDatabase() {
        this("eventlog.db");
    }

    public EventDatabase(String dbFilePath) {
        this.dbFilePath = dbFilePath;
    }

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

    public void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {}
    }

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

    /** Adds a column to file_events if it doesn't already exist. SQLite throws on duplicate — we ignore that. */
    private void ensureColumn(String column, String type) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE file_events ADD COLUMN " + column + " " + type);
        } catch (SQLException ignored) {
            // column already exists — expected and safe to ignore
        }
    }

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

    /** Inserts a batch of events, each committed individually via autocommit. */
    public void insertAll(List<FileEvent> events) {
        if (events == null || events.isEmpty()) return;
        for (FileEvent e : events) {
            insert(e);
        }
    }

    public void clearAll() {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM file_events");
        } catch (SQLException e) {
            throw new RuntimeException("Clear failed: " + e.getMessage(), e);
        }
    }

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

    private String safe(String v) {
        return v == null ? "" : v.replace("\"", "\"\"");
    }
}
