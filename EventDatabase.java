package com.fsw.database;

import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventDatabase implements Queryable {
    private String dbFilePath;
    private Connection connection;

    public EventDatabase() {
        this("eventlog.db");
    }

    public EventDatabase(String dbFilePath) {
        this.dbFilePath = dbFilePath;
    }

    public void connect() {
        try {
            String url = "jdbc:sqlite:" + dbFilePath;
            connection = DriverManager.getConnection(url);
            initSchema();
            System.out.println("Connected to SQLite database: " + dbFilePath);
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to SQLite database: " + e.getMessage(), e);
        }
    }

    public void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS file_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_name TEXT NOT NULL,
                    extension TEXT,
                    full_path TEXT NOT NULL,
                    activity_type TEXT NOT NULL,
                    timestamp TEXT NOT NULL
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Could not create file_events table: " + e.getMessage(), e);
        }
    }

    public void insert(FileEvent event) {
        if (connection == null) {
            connect();
        }

        String sql = """
                INSERT INTO file_events
                (file_name, extension, full_path, activity_type, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, event.getFileName());
            ps.setString(2, event.getExtension());
            ps.setString(3, event.getFullPath());
            ps.setString(4, event.getActivityType().name());
            ps.setString(5, event.getTimestamp().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not insert file event: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileEvent> query(QueryFilter f) {
        if (connection == null) {
            connect();
        }

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT file_name, extension, full_path, activity_type, timestamp FROM file_events WHERE 1=1");

        if (f != null) {
            if (f.getStartDate() != null) {
                sql.append(" AND timestamp >= ?");
                params.add(f.getStartDate().atStartOfDay().toString());
            }

            if (f.getEndDate() != null) {
                sql.append(" AND timestamp < ?");
                params.add(f.getEndDate().plusDays(1).atStartOfDay().toString());
            }

            if (f.getExtension() != null && !f.getExtension().isBlank()) {
                sql.append(" AND extension = ?");
                params.add(f.getExtension().replace(".", ""));
            }

            if (f.getActivityType() != null) {
                sql.append(" AND activity_type = ?");
                params.add(f.getActivityType().name());
            }

            if (f.getDirectory() != null && !f.getDirectory().isBlank()) {
                sql.append(" AND full_path LIKE ?");
                params.add(f.getDirectory() + "%");
            }
        }

        sql.append(" ORDER BY timestamp DESC");

        List<FileEvent> rows = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new FileEvent(
                            rs.getString("file_name"),
                            rs.getString("extension"),
                            rs.getString("full_path"),
                            ActivityType.valueOf(rs.getString("activity_type")),
                            LocalDateTime.parse(rs.getString("timestamp"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not query file events: " + e.getMessage(), e);
        }

        return rows;
    }

    @Override
    public void exportToCsv(String path) {
        List<FileEvent> rows = query(new QueryFilter());

        try (FileWriter writer = new FileWriter(path)) {
            writer.write("fileName,extension,fullPath,activityType,timestamp\n");
            for (FileEvent e : rows) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        e.getFileName(),
                        e.getExtension(),
                        e.getFullPath(),
                        e.getActivityType(),
                        e.getTimestamp()
                ));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not export CSV: " + e.getMessage(), e);
        }
    }

    protected String buildWhereClause(QueryFilter f) {
        if (f == null) {
            return "";
        }

        List<String> clauses = new ArrayList<>();

        if (f.getStartDate() != null) clauses.add("timestamp >= ?");
        if (f.getEndDate() != null) clauses.add("timestamp < ?");
        if (f.getExtension() != null && !f.getExtension().isBlank()) clauses.add("extension = ?");
        if (f.getActivityType() != null) clauses.add("activity_type = ?");
        if (f.getDirectory() != null && !f.getDirectory().isBlank()) clauses.add("full_path LIKE ?");

        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }
}
