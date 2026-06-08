package com.fsw.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DatabaseStats} — the immutable statistics snapshot.
 */
class DatabaseStatsTest {

    @Test
    @DisplayName("stores all values passed to the constructor")
    void storesValues() {
        Map<String, Integer> byActivity = new LinkedHashMap<>();
        byActivity.put("CREATED", 5);
        byActivity.put("MODIFIED", 3);

        Map<String, Integer> byExtension = new LinkedHashMap<>();
        byExtension.put("txt", 4);

        DatabaseStats stats = new DatabaseStats(8,
                "2026-06-01 09:00:00", "2026-06-08 17:00:00",
                byActivity, byExtension);

        assertEquals(8, stats.getTotal());
        assertEquals("2026-06-01 09:00:00", stats.getEarliest());
        assertEquals("2026-06-08 17:00:00", stats.getLatest());
        assertEquals(byActivity, stats.getByActivity());
        assertEquals(byExtension, stats.getByExtension());
    }

    @Test
    @DisplayName("null maps are replaced with empty maps, never null")
    void nullMapsBecomeEmpty() {
        DatabaseStats stats = new DatabaseStats(0, null, null, null, null);

        assertNotNull(stats.getByActivity());
        assertNotNull(stats.getByExtension());
        assertTrue(stats.getByActivity().isEmpty());
        assertTrue(stats.getByExtension().isEmpty());
    }

    @Test
    @DisplayName("null earliest/latest are preserved as null")
    void nullTimestampsPreserved() {
        DatabaseStats stats = new DatabaseStats(0, null, null, null, null);
        assertEquals(0, stats.getTotal());
        assertNull(stats.getEarliest());
        assertNull(stats.getLatest());
    }

    @Test
    @DisplayName("activity map preserves insertion (descending-count) order")
    void preservesInsertionOrder() {
        Map<String, Integer> byActivity = new LinkedHashMap<>();
        byActivity.put("MODIFIED", 10);
        byActivity.put("CREATED", 6);
        byActivity.put("DELETED", 1);

        DatabaseStats stats = new DatabaseStats(17, "a", "b", byActivity, null);

        assertArrayEquals(
                new String[]{"MODIFIED", "CREATED", "DELETED"},
                stats.getByActivity().keySet().toArray(new String[0]));
    }
}
