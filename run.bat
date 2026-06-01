@echo off
cd /d "%~dp0"

set CP=bin;lib\sqlite-jdbc-3.45.3.0.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-nop-2.0.9.jar

java -cp "%CP%" com.fsw.main.MainWatcher
