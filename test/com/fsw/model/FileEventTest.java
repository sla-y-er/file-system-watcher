package com.fsw.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FileEvent} — the core data object of the model layer.
 * Covers both constructors and the full getter/setter contract.
 */
class FileEventTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 6, 8, 14, 30, 0);

    @Nested
    @DisplayName("No-argument constructor")
    class NoArgConstructor {

        @Test
        @DisplayName("leaves every field null")
        void allFieldsNull() {
            FileEvent e = new FileEvent();
            assertNull(e.getFileName());
            assertNull(e.getExtension());
            assertNull(e.getFullPath());
            assertNull(e.getActivityType());
            assertNull(e.getTimestamp());
        }
    }

    @Nested
    @DisplayName("All-argument constructor")
    class AllArgConstructor {

        @Test
        @DisplayName("stores every argument verbatim")
        void storesAllArgs() {
            FileEvent e = new FileEvent("notes.txt", "txt", "C:/docs/notes.txt",
                    ActivityType.CREATED, WHEN);

            assertEquals("notes.txt", e.getFileName());
            assertEquals("txt", e.getExtension());
            assertEquals("C:/docs/notes.txt", e.getFullPath());
            assertEquals(ActivityType.CREATED, e.getActivityType());
            assertEquals(WHEN, e.getTimestamp());
        }

        @Test
        @DisplayName("accepts null values without throwing")
        void acceptsNulls() {
            assertDoesNotThrow(() -> new FileEvent(null, null, null, null, null));
        }
    }

    @Nested
    @DisplayName("Setters and getters")
    class Accessors {

        @Test
        @DisplayName("round-trip each field independently")
        void roundTrip() {
            FileEvent e = new FileEvent();

            e.setFileName("data.csv");
            e.setExtension("csv");
            e.setFullPath("/tmp/data.csv");
            e.setActivityType(ActivityType.MODIFIED);
            e.setTimestamp(WHEN);

            assertEquals("data.csv", e.getFileName());
            assertEquals("csv", e.getExtension());
            assertEquals("/tmp/data.csv", e.getFullPath());
            assertEquals(ActivityType.MODIFIED, e.getActivityType());
            assertEquals(WHEN, e.getTimestamp());
        }

        @Test
        @DisplayName("a setter overwrites a previously set value")
        void overwrite() {
            FileEvent e = new FileEvent("a.txt", "txt", "/a.txt",
                    ActivityType.CREATED, WHEN);

            e.setActivityType(ActivityType.DELETED);
            assertEquals(ActivityType.DELETED, e.getActivityType());

            e.setFileName("b.txt");
            assertEquals("b.txt", e.getFileName());
        }

        @Test
        @DisplayName("setting a field back to null is allowed")
        void setNull() {
            FileEvent e = new FileEvent("a.txt", "txt", "/a.txt",
                    ActivityType.CREATED, WHEN);
            e.setTimestamp(null);
            assertNull(e.getTimestamp());
        }
    }
}
