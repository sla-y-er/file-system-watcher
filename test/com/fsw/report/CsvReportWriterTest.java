package com.fsw.report;

import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CsvReportWriter}, the model-layer CSV writer.
 */
class CsvReportWriterTest {

    @TempDir
    Path tempDir;

    private static FileEvent sample() {
        return new FileEvent("a.txt", "txt", "C:/docs/a.txt",
                ActivityType.CREATED, LocalDateTime.of(2026, 6, 8, 10, 0, 0));
    }

    @Test
    @DisplayName("writeToFile creates the file with a header and one row per event")
    void writeToFile() throws IOException {
        CsvReportWriter writer = new CsvReportWriter(tempDir.toString());
        writer.writeToFile(List.of(sample()), "out.csv");

        Path out = tempDir.resolve("out.csv");
        assertTrue(Files.exists(out));

        List<String> lines = Files.readAllLines(out);
        assertEquals("file_name,extension,path,activity,event_datetime", lines.get(0));
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("a.txt"));
        assertTrue(lines.get(1).contains("CREATED"));
    }

    @Test
    @DisplayName("the output directory is created automatically if missing")
    void createsOutputDirectory() {
        Path nested = tempDir.resolve("reports").resolve("sub");
        CsvReportWriter writer = new CsvReportWriter(nested.toString());
        assertDoesNotThrow(() -> writer.writeToFile(List.of(sample()), "x.csv"));
        assertTrue(Files.exists(nested.resolve("x.csv")));
    }

    @Test
    @DisplayName("writeToFileWithHeader prepends the query description before the header")
    void writeWithHeader() throws IOException {
        CsvReportWriter writer = new CsvReportWriter(tempDir.toString());
        Path out = tempDir.resolve("withheader.csv");

        writer.writeToFileWithHeader(List.of(sample()), out.toString(),
                "# Query: extension=txt\n# Generated: 2026-06-08");

        List<String> lines = Files.readAllLines(out);
        assertEquals("# Query: extension=txt", lines.get(0));
        assertEquals("# Generated: 2026-06-08", lines.get(1));
        assertEquals("file_name,extension,path,activity,event_datetime", lines.get(2));
        assertTrue(lines.get(3).contains("a.txt"));
    }

    @Test
    @DisplayName("a blank description is skipped (header comes first)")
    void blankDescriptionSkipped() throws IOException {
        CsvReportWriter writer = new CsvReportWriter(tempDir.toString());
        Path out = tempDir.resolve("blank.csv");

        writer.writeToFileWithHeader(List.of(sample()), out.toString(), "");

        List<String> lines = Files.readAllLines(out);
        assertEquals("file_name,extension,path,activity,event_datetime", lines.get(0));
    }

    @Test
    @DisplayName("null fields on an event are written as empty strings, not the literal 'null'")
    void nullFieldsBecomeEmpty() throws IOException {
        CsvReportWriter writer = new CsvReportWriter(tempDir.toString());
        Path out = tempDir.resolve("nulls.csv");

        FileEvent e = new FileEvent();   // all fields null
        writer.writeToFile(List.of(e), out.getFileName().toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("nulls.csv"));
        assertEquals(2, lines.size());
        assertFalse(lines.get(1).contains("null"), "null fields must not appear as the text 'null'");
    }

    @Test
    @DisplayName("empty event list still writes the header")
    void emptyListWritesHeaderOnly() throws IOException {
        CsvReportWriter writer = new CsvReportWriter(tempDir.toString());
        writer.writeToFile(List.of(), "emptylist.csv");

        List<String> lines = Files.readAllLines(tempDir.resolve("emptylist.csv"));
        assertEquals(1, lines.size());
    }

    @Test
    @DisplayName("printToConsole does not throw for normal or empty input")
    void printToConsoleSafe() {
        CsvReportWriter writer = new CsvReportWriter(tempDir.toString());
        assertDoesNotThrow(() -> writer.printToConsole(List.of(sample())));
        assertDoesNotThrow(() -> writer.printToConsole(List.of()));
    }
}
