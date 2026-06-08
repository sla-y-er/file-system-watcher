package com.fsw.database;

import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit/integration tests for {@link EventDatabase}, the SQLite-backed persistence
 * layer. Each test runs against a fresh temporary database file so tests are isolated
 * and leave nothing behind.
 */
class EventDatabaseTest {

    @TempDir
    Path tempDir;

    private EventDatabase db;

    @BeforeEach
    void setUp() {
        db = new EventDatabase(tempDir.resolve("test.db").toString());
        db.connect();
    }

    @AfterEach
    void tearDown() {
        db.disconnect();
    }

    private static FileEvent event(String name, String ext, String path,
                                   ActivityType type, LocalDateTime when) {
        return new FileEvent(name, ext, path, type, when);
    }

    // ------------------------------------------------------------------ basics

    @Test
    @DisplayName("a freshly connected database is empty")
    void startsEmpty() {
        List<FileEvent> all = db.query(new QueryFilter());
        assertTrue(all.isEmpty());
        assertEquals(0, db.getStatistics().getTotal());
    }

    @Test
    @DisplayName("insert then query returns the stored event with all fields intact")
    void insertAndQuery() {
        LocalDateTime when = LocalDateTime.of(2026, 6, 8, 10, 0, 0);
        db.insert(event("a.txt", "txt", "C:/docs/a.txt", ActivityType.CREATED, when));

        List<FileEvent> all = db.query(new QueryFilter());
        assertEquals(1, all.size());

        FileEvent got = all.get(0);
        assertEquals("a.txt", got.getFileName());
        assertEquals("txt", got.getExtension());
        assertEquals("C:/docs/a.txt", got.getFullPath());
        assertEquals(ActivityType.CREATED, got.getActivityType());
        assertEquals(when, got.getTimestamp());
    }

    @Test
    @DisplayName("insertAll stores every event")
    void insertAll() {
        db.insertAll(Arrays.asList(
                event("a.txt", "txt", "/a.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 1, 9, 0, 0)),
                event("b.log", "log", "/b.log", ActivityType.MODIFIED, LocalDateTime.of(2026, 6, 2, 9, 0, 0)),
                event("c.csv", "csv", "/c.csv", ActivityType.DELETED, LocalDateTime.of(2026, 6, 3, 9, 0, 0))
        ));
        assertEquals(3, db.query(new QueryFilter()).size());
    }

    @Test
    @DisplayName("insertAll with an empty or null list is a no-op")
    void insertAllEmpty() {
        assertDoesNotThrow(() -> db.insertAll(List.of()));
        assertDoesNotThrow(() -> db.insertAll(null));
        assertEquals(0, db.query(new QueryFilter()).size());
    }

    @Test
    @DisplayName("clearAll removes every row")
    void clearAll() {
        db.insert(event("a.txt", "txt", "/a.txt", ActivityType.CREATED, LocalDateTime.now()));
        db.insert(event("b.txt", "txt", "/b.txt", ActivityType.CREATED, LocalDateTime.now()));
        assertEquals(2, db.query(new QueryFilter()).size());

        db.clearAll();
        assertEquals(0, db.query(new QueryFilter()).size());
    }

    @Test
    @DisplayName("data persists across reconnects to the same file")
    void persistsAcrossReconnect() {
        db.insert(event("a.txt", "txt", "/a.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 8, 10, 0, 0)));
        db.disconnect();

        db = new EventDatabase(tempDir.resolve("test.db").toString());
        db.connect();
        assertEquals(1, db.query(new QueryFilter()).size());
    }

    @Test
    @DisplayName("results are ordered by date/time descending (newest first)")
    void orderedNewestFirst() {
        db.insert(event("old.txt", "txt", "/old.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 1, 8, 0, 0)));
        db.insert(event("new.txt", "txt", "/new.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 7, 8, 0, 0)));
        db.insert(event("mid.txt", "txt", "/mid.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 4, 8, 0, 0)));

        List<FileEvent> all = db.query(new QueryFilter());
        assertEquals("new.txt", all.get(0).getFileName());
        assertEquals("mid.txt", all.get(1).getFileName());
        assertEquals("old.txt", all.get(2).getFileName());
    }

    // --------------------------------------------------------------- filtering

    @Test
    @DisplayName("null filter matches all rows")
    void nullFilterMatchesAll() {
        seedThree();
        assertEquals(3, db.query(null).size());
    }

    @Test
    @DisplayName("filter by extension (case-insensitive, dot ignored)")
    void filterByExtension() {
        seedThree();

        QueryFilter byTxt = new QueryFilter();
        byTxt.setExtension("TXT");           // upper-case
        assertEquals(1, db.query(byTxt).size());

        QueryFilter withDot = new QueryFilter();
        withDot.setExtension(".log");        // leading dot should be ignored
        assertEquals(1, db.query(withDot).size());
    }

    @Test
    @DisplayName("filter by activity type")
    void filterByActivity() {
        seedThree();
        QueryFilter f = new QueryFilter();
        f.setActivityType(ActivityType.DELETED);

        List<FileEvent> result = db.query(f);
        assertEquals(1, result.size());
        assertEquals(ActivityType.DELETED, result.get(0).getActivityType());
    }

    @Test
    @DisplayName("filter by directory prefix")
    void filterByDirectory() {
        db.insert(event("a.txt", "txt", "C:/projects/a.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 1, 9, 0, 0)));
        db.insert(event("b.txt", "txt", "C:/other/b.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 2, 9, 0, 0)));

        QueryFilter f = new QueryFilter();
        f.setDirectory("C:/projects");
        List<FileEvent> result = db.query(f);
        assertEquals(1, result.size());
        assertEquals("a.txt", result.get(0).getFileName());
    }

    @Test
    @DisplayName("filter by inclusive date range")
    void filterByDateRange() {
        db.insert(event("jan.txt", "txt", "/jan.txt", ActivityType.CREATED, LocalDateTime.of(2026, 1, 15, 9, 0, 0)));
        db.insert(event("jun.txt", "txt", "/jun.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 15, 9, 0, 0)));
        db.insert(event("dec.txt", "txt", "/dec.txt", ActivityType.CREATED, LocalDateTime.of(2026, 12, 15, 9, 0, 0)));

        QueryFilter f = new QueryFilter();
        f.setStartDate(LocalDate.of(2026, 6, 1));
        f.setEndDate(LocalDate.of(2026, 6, 30));

        List<FileEvent> result = db.query(f);
        assertEquals(1, result.size());
        assertEquals("jun.txt", result.get(0).getFileName());
    }

    @Test
    @DisplayName("end-of-day boundary: an event late on the end date still matches")
    void dateRangeEndInclusiveToEndOfDay() {
        db.insert(event("late.txt", "txt", "/late.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 30, 23, 30, 0)));

        QueryFilter f = new QueryFilter();
        f.setStartDate(LocalDate.of(2026, 6, 1));
        f.setEndDate(LocalDate.of(2026, 6, 30));

        assertEquals(1, db.query(f).size());
    }

    @Test
    @DisplayName("multiple criteria combine with AND")
    void combinedFilters() {
        db.insert(event("match.txt", "txt", "C:/projects/match.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 10, 9, 0, 0)));
        db.insert(event("wrongtype.txt", "txt", "C:/projects/wrongtype.txt", ActivityType.DELETED, LocalDateTime.of(2026, 6, 10, 9, 0, 0)));
        db.insert(event("wrongext.log", "log", "C:/projects/wrongext.log", ActivityType.CREATED, LocalDateTime.of(2026, 6, 10, 9, 0, 0)));

        QueryFilter f = new QueryFilter();
        f.setExtension("txt");
        f.setActivityType(ActivityType.CREATED);
        f.setDirectory("C:/projects");

        List<FileEvent> result = db.query(f);
        assertEquals(1, result.size());
        assertEquals("match.txt", result.get(0).getFileName());
    }

    // -------------------------------------------------------------- statistics

    @Test
    @DisplayName("statistics report total, earliest and latest timestamps")
    void statisticsTotalsAndRange() {
        db.insert(event("a.txt", "txt", "/a.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 1, 8, 0, 0)));
        db.insert(event("b.txt", "txt", "/b.txt", ActivityType.MODIFIED, LocalDateTime.of(2026, 6, 8, 17, 0, 0)));

        DatabaseStats stats = db.getStatistics();
        assertEquals(2, stats.getTotal());
        assertEquals("2026-06-01 08:00:00", stats.getEarliest());
        assertEquals("2026-06-08 17:00:00", stats.getLatest());
    }

    @Test
    @DisplayName("statistics group counts by activity and by extension")
    void statisticsGroupings() {
        db.insert(event("a.txt", "txt", "/a.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 1, 8, 0, 0)));
        db.insert(event("b.txt", "txt", "/b.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 2, 8, 0, 0)));
        db.insert(event("c.log", "log", "/c.log", ActivityType.MODIFIED, LocalDateTime.of(2026, 6, 3, 8, 0, 0)));

        DatabaseStats stats = db.getStatistics();

        Map<String, Integer> byActivity = stats.getByActivity();
        assertEquals(2, byActivity.get("CREATED"));
        assertEquals(1, byActivity.get("MODIFIED"));

        Map<String, Integer> byExtension = stats.getByExtension();
        assertEquals(2, byExtension.get("txt"));
        assertEquals(1, byExtension.get("log"));
    }

    @Test
    @DisplayName("events with a blank extension are grouped under '(none)'")
    void statisticsBlankExtension() {
        db.insert(event("README", "", "/README", ActivityType.CREATED, LocalDateTime.of(2026, 6, 1, 8, 0, 0)));
        DatabaseStats stats = db.getStatistics();
        assertEquals(1, stats.getByExtension().get("(none)"));
    }

    // ------------------------------------------------------------------ export

    @Test
    @DisplayName("exportToCsv writes a header plus one line per row")
    void exportToCsv() throws IOException {
        db.insert(event("a.txt", "txt", "C:/docs/a.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 8, 10, 0, 0)));
        db.insert(event("b.log", "log", "C:/docs/b.log", ActivityType.DELETED, LocalDateTime.of(2026, 6, 8, 11, 0, 0)));

        Path out = tempDir.resolve("export.csv");
        db.exportToCsv(out.toString());

        assertTrue(Files.exists(out));
        List<String> lines = Files.readAllLines(out);
        assertEquals("file_name,extension,path,activity,event_datetime", lines.get(0));
        assertEquals(3, lines.size(), "header + 2 data rows");
        assertTrue(lines.get(1).contains("a.txt") || lines.get(2).contains("a.txt"));
    }

    @Test
    @DisplayName("exportToCsv on an empty database writes just the header")
    void exportEmpty() throws IOException {
        Path out = tempDir.resolve("empty.csv");
        db.exportToCsv(out.toString());
        List<String> lines = Files.readAllLines(out);
        assertEquals(1, lines.size());
    }

    // --------------------------------------------------------------- robustness

    @Test
    @DisplayName("an event with a null activity type is stored and read back safely")
    void nullActivityTypeRoundTrips() {
        db.insert(event("a.txt", "txt", "/a.txt", null, LocalDateTime.of(2026, 6, 8, 10, 0, 0)));
        List<FileEvent> all = db.query(new QueryFilter());
        assertEquals(1, all.size());
        assertNull(all.get(0).getActivityType());
    }

    @Test
    @DisplayName("a single-quote in a path does not break inserts (no SQL injection)")
    void handlesQuotesInValues() {
        String tricky = "C:/o'brien/'; DROP TABLE file_events;--.txt";
        db.insert(event("o'brien.txt", "txt", tricky, ActivityType.CREATED, LocalDateTime.of(2026, 6, 8, 10, 0, 0)));

        List<FileEvent> all = db.query(new QueryFilter());
        assertEquals(1, all.size(), "table should still exist and contain the row");
        assertEquals(tricky, all.get(0).getFullPath());
    }

    /** Inserts one txt/CREATED, one log/MODIFIED, and one csv/DELETED event. */
    private void seedThree() {
        db.insert(event("a.txt", "txt", "/dir/a.txt", ActivityType.CREATED, LocalDateTime.of(2026, 6, 1, 9, 0, 0)));
        db.insert(event("b.log", "log", "/dir/b.log", ActivityType.MODIFIED, LocalDateTime.of(2026, 6, 2, 9, 0, 0)));
        db.insert(event("c.csv", "csv", "/dir/c.csv", ActivityType.DELETED, LocalDateTime.of(2026, 6, 3, 9, 0, 0)));
    }
}
