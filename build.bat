@echo off
cd /d "%~dp0"

set CP=lib\sqlite-jdbc-3.45.3.0.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-nop-2.0.9.jar

if not exist bin mkdir bin

echo Compiling...
javac -d bin -cp "%CP%" ^
  src\com\fsw\model\*.java ^
  src\com\fsw\database\*.java ^
  src\com\fsw\watcher\*.java ^
  src\com\fsw\query\*.java ^
  src\com\fsw\report\*.java ^
  src\com\fsw\email\*.java ^
  src\com\fsw\gui\*.java ^
  src\com\fsw\main\*.java

if %ERRORLEVEL% EQU 0 (
    echo Build successful. Run with:  run.bat
) else (
    echo Build FAILED.
    exit /b 1
)
