# file-system-watcher

## Build and Run

This project now uses only standard Java and no external runtime JARs.

From the workspace root:

1. Compile the source:

```cmd
javac -d bin src\com\fsw\database\*.java src\com\fsw\email\*.java src\com\fsw\main\*.java src\com\fsw\model\*.java src\com\fsw\query\*.java src\com\fsw\report\*.java src\com\fsw\watcher\*.java
```

2. Run the application:

```cmd
java -cp bin com.fsw.main.MainWatcher
```

No `lib` folder or extra helper scripts are required.

## Build and Run

This project uses `sqlite-jdbc.jar` plus SLF4J runtime dependencies.

1. Open a terminal in the project root.
2. Run `build.bat` to compile.
3. Run `run.bat` to start the watcher.

## Dependencies

Required files in `lib\`:
- `sqlite-jdbc.jar`
- `slf4j-api-1.7.36.jar`
- `slf4j-simple-1.7.36.jar`
