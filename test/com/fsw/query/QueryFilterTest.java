package com.fsw.query;

import com.fsw.model.ActivityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QueryFilter} — the model object that carries query criteria.
 */
class QueryFilterTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END   = LocalDate.of(2026, 12, 31);

    @Test
    @DisplayName("no-arg constructor leaves all criteria null (match-all)")
    void emptyFilter() {
        QueryFilter f = new QueryFilter();
        assertNull(f.getStartDate());
        assertNull(f.getEndDate());
        assertNull(f.getExtension());
        assertNull(f.getActivityType());
        assertNull(f.getDirectory());
    }

    @Test
    @DisplayName("all-arg constructor stores every criterion")
    void fullConstructor() {
        QueryFilter f = new QueryFilter(START, END, "java",
                ActivityType.MODIFIED, "C:/src");

        assertEquals(START, f.getStartDate());
        assertEquals(END, f.getEndDate());
        assertEquals("java", f.getExtension());
        assertEquals(ActivityType.MODIFIED, f.getActivityType());
        assertEquals("C:/src", f.getDirectory());
    }

    @Test
    @DisplayName("setters round-trip each field")
    void settersRoundTrip() {
        QueryFilter f = new QueryFilter();

        f.setStartDate(START);
        f.setEndDate(END);
        f.setExtension("txt");
        f.setActivityType(ActivityType.DELETED);
        f.setDirectory("/var/log");

        assertEquals(START, f.getStartDate());
        assertEquals(END, f.getEndDate());
        assertEquals("txt", f.getExtension());
        assertEquals(ActivityType.DELETED, f.getActivityType());
        assertEquals("/var/log", f.getDirectory());
    }

    @Test
    @DisplayName("fields can be reset back to null")
    void resetToNull() {
        QueryFilter f = new QueryFilter(START, END, "java",
                ActivityType.CREATED, "C:/src");
        f.setExtension(null);
        f.setActivityType(null);
        assertNull(f.getExtension());
        assertNull(f.getActivityType());
    }
}
