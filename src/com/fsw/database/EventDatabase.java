package com.fsw.database;

import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EventDatabase implements Queryable {
    private String dbFilePath;

    public EventDatabase() {
        this("eventlog.db");
    }

    public EventDatabase(String dbFilePath) {
        this.dbFilePath = dbFilePath;
    }

    private List<FileEvent> events = new ArrayList<>();

    public void connect() {
        initStore();
        System.out.println("Initialized event store: " + dbFilePath);
    }

    private void initStore() {
        File dbFile = new File(dbFilePath);
        try {
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize event store: " + e.getMessage(), e);
        }
    }

    public void insert(FileEvent event) {
        events.add(event);
    }

    @Override
    public List<FileEvent> query(QueryFilter f) {
        return events.stream()
                .filter(event -> {
                    if (f == null) {
                        return true;
                    }

                    if (f.getStartDate() != null && event.getTimestamp().toLocalDate().isBefore(f.getStartDate())) {
                        return false;
                    }

                    if (f.getEndDate() != null && event.getTimestamp().toLocalDate().isAfter(f.getEndDate())) {
                        return false;
                    }

                    if (f.getExtension() != null && !f.getExtension().isBlank()) {
                        String ext = event.getExtension() == null ? "" : event.getExtension().replace(".", "");
                        if (!ext.equalsIgnoreCase(f.getExtension().replace(".", ""))) {
                            return false;
                        }
                    }

                    if (f.getActivityType() != null && event.getActivityType() != f.getActivityType()) {
                        return false;
                    }

                    if (f.getDirectory() != null && !f.getDirectory().isBlank()) {
                        if (event.getFullPath() == null || !event.getFullPath().startsWith(f.getDirectory())) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted(Comparator.comparing(FileEvent::getTimestamp).reversed())
                .collect(Collectors.toList());
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
}
