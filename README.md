# File System Watcher

A Java Swing desktop application that monitors a folder for file activity, displays events live in a GUI table, persists them to SQLite, and supports querying and CSV export.

## Requirements

- Java 11 or later (Java 21 recommended)
- `lib\sqlite-jdbc-3.45.3.0.jar` — already included; download from Maven Central if missing

## Build

**From the project root in Command Prompt (cmd.exe):**

```
build.bat
```

Or compile manually:

```
javac -d bin -cp lib\sqlite-jdbc-3.45.3.0.jar ^
  src\com\fsw\model\*.java ^
  src\com\fsw\database\*.java ^
  src\com\fsw\watcher\*.java ^
  src\com\fsw\query\*.java ^
  src\com\fsw\report\*.java ^
  src\com\fsw\email\*.java ^
  src\com\fsw\gui\*.java ^
  src\com\fsw\main\*.java
```

## Run

```
run.bat
```

Or manually:

```
java -cp "bin;lib\sqlite-jdbc-3.45.3.0.jar" com.fsw.main.MainWatcher
```

## Usage

1. **Watch Folder** — type or Browse for a directory to monitor.
2. **Extension** — pick a preset (`.txt`, `.java`, …) or choose `Custom…` and type one. Choose `All Files` to watch everything.
3. **Start (F5)** — begins monitoring. File events appear live in the table with columns: File Name, Extension, Path, Activity, Date/Time.
4. **Stop (F6)** — stops monitoring. Events already displayed remain.
5. **Write DB (Ctrl+S)** — saves all new (unsaved) events to `eventlog.db` (SQLite). Click multiple times safely; already-saved events are not duplicated.
6. **Query DB (Ctrl+D)** — opens the Query Database window where you can filter by date range, extension, activity type, or directory path.
7. **Export CSV** — from the Query window, export filtered results to a CSV file with a query summary header.
8. **Clear Database** — available from the Database menu or the Query window; asks for confirmation.
9. **Exit (Ctrl+Q)** — if there are unsaved events, you are prompted to save, discard, or cancel.

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| F5 | Start monitoring |
| F6 | Stop monitoring |
| Ctrl+S | Write events to database |
| Ctrl+D | Open query window |
| Ctrl+L | Clear event display |
| Ctrl+Q | Exit |
| F1 | About / Help |
| Esc | Close query window |

## Project Structure

```
src/com/fsw/
  gui/         WatcherFrame.java   — main window
               QueryFrame.java     — query & export dialog
  database/    EventDatabase.java  — SQLite persistence (JDBC)
               Queryable.java      — interface
  watcher/     FileWatcher.java    — NIO WatchService, background thread
  model/       FileEvent.java      ActivityType.java
  query/       QueryEngine.java    QueryFilter.java
  report/      CsvReportWriter.java  ReportWriter.java
  email/       EmailSender.java    — mock email stub
  main/        MainWatcher.java    — entry point
lib/
  sqlite-jdbc-3.45.3.0.jar
bin/           compiled .class files
eventlog.db    SQLite database (created automatically)
```

## Notes

- File renames appear as a DELETED + CREATED pair (Java NIO WatchService limitation).
- A 500 ms debounce window suppresses rapid duplicate MODIFY events.
- The database is created automatically if it does not exist.
