package com.fsw.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link ActivityType} enum.
 */
class ActivityTypeTest {

    @Test
    @DisplayName("defines exactly the four expected constants")
    void hasExpectedConstants() {
        ActivityType[] values = ActivityType.values();
        assertEquals(4, values.length, "expected 4 activity types");
        assertArrayEquals(
                new ActivityType[]{
                        ActivityType.CREATED,
                        ActivityType.MODIFIED,
                        ActivityType.DELETED,
                        ActivityType.RENAMED
                },
                values);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATED", "MODIFIED", "DELETED", "RENAMED"})
    @DisplayName("valueOf round-trips with name() for each constant")
    void valueOfRoundTrip(String name) {
        ActivityType type = ActivityType.valueOf(name);
        assertEquals(name, type.name());
    }

    @Test
    @DisplayName("valueOf rejects an unknown name")
    void valueOfUnknownThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ActivityType.valueOf("MOVED"));
    }

    @Test
    @DisplayName("valueOf is case-sensitive")
    void valueOfCaseSensitive() {
        assertThrows(IllegalArgumentException.class,
                () -> ActivityType.valueOf("created"));
    }
}
