package com.fsw.watcher;

import com.fsw.database.EventDatabase;
import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;

import java.nio.file.*;
import java.time.LocalDateTime;

public class FileWatcher {
    private Path watchPath;
    private WatchService watchService;
    private EventDatabase db;
    private boolean running;

    public FileWatcher(Path path, EventDatabase db) {
        this.watchPath = path;
        this.db = db;
    }

    public void start() {
        if (!Files.exists(watchPath) || !Files.isDirectory(watchPath)) {
            System.out.println("Invalid folder path: " + watchPath);
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();

            watchPath.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );

            running = true;
            System.out.println("Watching: " + watchPath);

            while (running) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    processEvent(event);
                }

                if (!key.reset()) {
                    break;
                }
            }

        } catch (Exception e) {
            System.out.println("Watcher error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (Exception e) {
            System.out.println("Error stopping watcher: " + e.getMessage());
        }
    }

    private void processEvent(WatchEvent<?> e) {
        WatchEvent.Kind<?> kind = e.kind();

        if (kind == StandardWatchEventKinds.OVERFLOW) {
            return;
        }

        Path fileName = (Path) e.context();

        FileEvent event = new FileEvent(
                fileName.toString(),
                getExtension(fileName.toString()),
                watchPath.resolve(fileName).toString(),
                resolveActivity(kind),
                LocalDateTime.now()
        );

        onEvent(event);
    }

    private ActivityType resolveActivity(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) return ActivityType.CREATED;
        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) return ActivityType.MODIFIED;
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) return ActivityType.DELETED;
        return null;
    }

    protected void onEvent(FileEvent event) {
        System.out.println("Event: " + event.getActivityType() + " - " + event.getFileName());
        db.insert(event);
    }

    private String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(i + 1) : "";
    }
}
