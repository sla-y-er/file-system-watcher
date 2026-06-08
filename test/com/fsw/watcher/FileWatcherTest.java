package com.fsw.watcher;

import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for {@link FileWatcher}.
 *
 * <p>These exercise the real Java NIO {@code WatchService} against temporary
 * directories. Because the OS delivers file-system events asynchronously (and on
 * Windows with noticeable latency), assertions poll with a generous timeout rather
 * than asserting immediately.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class FileWatcherTest {

    /** Max time to wait for an expected event to arrive. */
    private static final long WAIT_MS = 12_000;

    private FileWatcher watcher;

    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }
    }

    /** Polls the collected events until {@code condition} holds or the timeout elapses. */
    private static boolean waitFor(List<FileEvent> events, Predicate<List<FileEvent>> condition) {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.test(events)) return true;
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return condition.test(events);
    }

    private static boolean hasEvent(List<FileEvent> events, String name, ActivityType type) {
        return events.stream().anyMatch(e ->
                name.equals(e.getFileName()) && e.getActivityType() == type);
    }

    @Test
    @DisplayName("detects creation of a file in the watched folder")
    void detectsCreate(@TempDir Path dir) throws IOException {
        List<FileEvent> events = new CopyOnWriteArrayList<>();
        watcher = new FileWatcher(dir, "", events::add);
        watcher.start();

        Files.writeString(dir.resolve("hello.txt"), "hi");

        assertTrue(waitFor(events, ev -> hasEvent(ev, "hello.txt", ActivityType.CREATED)),
                "expected a CREATED event for hello.txt");
    }

    @Test
    @DisplayName("detects deletion of a file in the watched folder")
    void detectsDelete(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("gone.txt");
        Files.writeString(file, "temp");

        List<FileEvent> events = new CopyOnWriteArrayList<>();
        watcher = new FileWatcher(dir, "", events::add);
        watcher.start();

        Files.delete(file);

        assertTrue(waitFor(events, ev -> hasEvent(ev, "gone.txt", ActivityType.DELETED)),
                "expected a DELETED event for gone.txt");
    }

    @Test
    @DisplayName("extension filter ignores files with other extensions")
    void extensionFilterExcludes(@TempDir Path dir) throws IOException {
        List<FileEvent> events = new CopyOnWriteArrayList<>();
        watcher = new FileWatcher(dir, "txt", events::add);   // only .txt
        watcher.start();

        Files.writeString(dir.resolve("keep.txt"), "yes");
        Files.writeString(dir.resolve("skip.log"), "no");

        assertTrue(waitFor(events, ev -> hasEvent(ev, "keep.txt", ActivityType.CREATED)),
                "the .txt file should be reported");
        assertFalse(events.stream().anyMatch(e -> "skip.log".equals(e.getFileName())),
                "the .log file should be filtered out");
    }

    @Test
    @DisplayName("recursive watcher reports events from an existing sub-folder")
    void recursiveWatchesSubfolder(@TempDir Path dir) throws IOException {
        Path sub = Files.createDirectory(dir.resolve("nested"));

        List<FileEvent> events = new CopyOnWriteArrayList<>();
        watcher = new FileWatcher(dir, "", true, events::add);   // recursive
        watcher.start();

        Files.writeString(sub.resolve("deep.txt"), "data");

        assertTrue(waitFor(events, ev -> hasEvent(ev, "deep.txt", ActivityType.CREATED)),
                "expected a CREATED event from the sub-folder");
    }

    @Test
    @DisplayName("non-recursive watcher ignores events in sub-folders")
    void nonRecursiveIgnoresSubfolder(@TempDir Path dir) throws IOException {
        Path sub = Files.createDirectory(dir.resolve("nested"));

        List<FileEvent> events = new CopyOnWriteArrayList<>();
        watcher = new FileWatcher(dir, "", false, events::add);  // not recursive
        watcher.start();

        // a change in the root should register; a change in the sub-folder should not
        Files.writeString(sub.resolve("deep.txt"), "data");
        Files.writeString(dir.resolve("root.txt"), "data");

        assertTrue(waitFor(events, ev -> hasEvent(ev, "root.txt", ActivityType.CREATED)),
                "root-level event should be reported");
        assertFalse(events.stream().anyMatch(e -> "deep.txt".equals(e.getFileName())),
                "sub-folder event should NOT be reported when non-recursive");
    }

    @Test
    @DisplayName("populated FileEvent fields are correct (name, extension, path)")
    void eventFieldsArePopulated(@TempDir Path dir) throws IOException {
        List<FileEvent> events = new CopyOnWriteArrayList<>();
        watcher = new FileWatcher(dir, "", events::add);
        watcher.start();

        Path file = dir.resolve("report.csv");
        Files.writeString(file, "x");

        assertTrue(waitFor(events, ev -> hasEvent(ev, "report.csv", ActivityType.CREATED)));

        FileEvent e = events.stream()
                .filter(ev -> "report.csv".equals(ev.getFileName()))
                .findFirst().orElseThrow();
        assertEquals("csv", e.getExtension());
        assertNotNull(e.getTimestamp());
        assertTrue(e.getFullPath().endsWith("report.csv"));
    }

    @Test
    @DisplayName("after stop() no further events are delivered")
    void stopHaltsDelivery(@TempDir Path dir) throws Exception {
        List<FileEvent> events = new CopyOnWriteArrayList<>();
        watcher = new FileWatcher(dir, "", events::add);
        watcher.start();
        watcher.stop();

        // give the watch thread a moment to wind down, then make a change
        Thread.sleep(800);
        Files.writeString(dir.resolve("after.txt"), "late");
        Thread.sleep(2_000);

        assertFalse(events.stream().anyMatch(e -> "after.txt".equals(e.getFileName())),
                "no events should be delivered after stop()");
    }
}
