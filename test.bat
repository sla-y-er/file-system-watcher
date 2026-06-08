@echo off
cd /d "%~dp0"

set JUNIT=lib\junit-platform-console-standalone-1.10.2.jar
set CP=lib\sqlite-jdbc-3.45.3.0.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-nop-2.0.9.jar

if not exist bin mkdir bin
if not exist bin-test mkdir bin-test

echo Compiling main sources...
javac -d bin -cp "%CP%" ^
  src\com\fsw\model\*.java ^
  src\com\fsw\database\*.java ^
  src\com\fsw\watcher\*.java ^
  src\com\fsw\query\*.java ^
  src\com\fsw\report\*.java ^
  src\com\fsw\email\*.java
if %ERRORLEVEL% NEQ 0 ( echo Main compile FAILED. & exit /b 1 )

echo Compiling tests...
javac -d bin-test -cp "bin;%CP%;%JUNIT%" ^
  test\com\fsw\model\*.java ^
  test\com\fsw\query\*.java ^
  test\com\fsw\database\*.java ^
  test\com\fsw\report\*.java ^
  test\com\fsw\watcher\*.java
if %ERRORLEVEL% NEQ 0 ( echo Test compile FAILED. & exit /b 1 )

echo Running tests...
java -jar "%JUNIT%" execute ^
  --class-path "bin;bin-test;%CP%" ^
  --scan-class-path bin-test
