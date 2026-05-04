package com.fsw.main;

import com.fsw.database.EventDatabase;
import com.fsw.query.QueryEngine;
import com.fsw.watcher.FileWatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class MainWatcher {
    private FileWatcher watcher;
    private QueryEngine engine;
    private Scanner scanner;
    private EventDatabase database;

    public static void main(String[] args) {
        MainWatcher app = new MainWatcher();
        app.showMenu();
    }

    public void showMenu() {
        scanner = new Scanner(System.in);
        database = new EventDatabase("eventlog.db");
        database.connect();

        System.out.println("File System Watcher");
        System.out.print("Enter folder path to monitor: ");
        String folderPath = scanner.nextLine();

        startWatch(folderPath);
    }

    public void startWatch(String folderPath) {
        Path path = Paths.get(folderPath);
        watcher = new FileWatcher(path, database);
        watcher.start();
    }

    public void startWatch() {
        showMenu();
    }
}
