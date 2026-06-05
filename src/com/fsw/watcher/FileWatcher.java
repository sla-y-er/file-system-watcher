package com.fsw.watcher;

import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FileWatcher {

    private static final long DEBOUNCE_MS = 500;

    private final Path watchPath;
    private final String extensionFilter; // blank = all files
    private final boolean recursive;       // also watch sub-directories
    private final Consumer<FileEvent> eventCallback;

    private WatchService watchService;
    private volatile boolean running;

    // maps each registered WatchKey to the directory it is watching, so that
    // events from sub-directories resolve to the correct absolute path
    private final Map<WatchKey, Path> watchedDirs = new HashMap<>();

    // tracks last-seen time per (path+kind) to debounce rapid duplicate events
    private final Map<String, Long> lastEventTime = new HashMap<>();

    public FileWatcher(Path path, String extensionFilter, Consumer<FileEvent> eventCallback) {
        this(path, extensionFilter, false, eventCallback);
    }

    public FileWatcher(Path path, String extensionFilter, boolean recursive, Consumer<FileEvent> eventCallback) {
        this.watchPath = path;
        this.extensionFilter = (extensionFilter == null) ? "" : extensionFilter.replace(".", "").trim();
        this.recursive = recursive;
        this.eventCallback = eventCallback;
    }

    /** Starts watching in a background daemon thread. Non-blocking. */
    public void start() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();

        if (recursive) {
            registerTree(watchPath);
        } else {
            register(watchPath);
        }
        running = true;

        Thread thread = new Thread(this::watchLoop, "FileWatcher-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException ignored) {}
    }

    /** Registers a single directory with the watch service. */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );
        watchedDirs.put(key, dir);
    }

    /** Recursively registers a directory and all of its existing sub-directories. */
    private void registerTree(Path root) throws IOException {
        Files.walk(root)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    try { register(dir); }
                    catch (IOException ignored) { /* skip dirs we can't access */ }
                });
    }

    private void watchLoop() {
        try {
            while (running) {
                WatchKey key = watchService.poll(300, TimeUnit.MILLISECONDS);
                if (key == null) continue;

                Path dir = watchedDirs.get(key);
                if (dir == null) { key.reset(); continue; }

                for (WatchEvent<?> event : key.pollEvents()) {
                    processEvent(dir, event);
                }

                if (!key.reset()) {
                    watchedDirs.remove(key);
                    if (watchedDirs.isEmpty()) break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException ignored) {
            // normal stop
        }
    }

    private void processEvent(Path dir, WatchEvent<?> e) {
        WatchEvent.Kind<?> kind = e.kind();
        if (kind == StandardWatchEventKinds.OVERFLOW) return;

        Path fileName = (Path) e.context();
        Path fullPath = dir.resolve(fileName);
        String name = fileName.toString();
        String ext = getExtension(name);

        // when watching recursively, register any newly created sub-directory
        if (recursive && kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
            try { registerTree(fullPath); }
            catch (IOException ignored) {}
        }

        // apply extension filter
        if (!extensionFilter.isBlank() && !ext.equalsIgnoreCase(extensionFilter)) return;

        // debounce: skip if same (path+kind) fired within DEBOUNCE_MS
        String dedupeKey = fullPath + "|" + kind.name();
        long now = System.currentTimeMillis();
        Long last = lastEventTime.get(dedupeKey);
        if (last != null && (now - last) < DEBOUNCE_MS) return;
        lastEventTime.put(dedupeKey, now);

        // keep map from growing unboundedly
        if (lastEventTime.size() > 500) lastEventTime.clear();

        ActivityType activity = resolveActivity(kind);
        FileEvent event = new FileEvent(
                name, ext,
                fullPath.toAbsolutePath().toString(),
                activity,
                LocalDateTime.now()
        );

        if (eventCallback != null) {
            eventCallback.accept(event);
        }
    }

    private ActivityType resolveActivity(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) return ActivityType.CREATED;
        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) return ActivityType.MODIFIED;
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) return ActivityType.DELETED;
        return null;
    }

    private String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(i + 1) : "";
    }
}
