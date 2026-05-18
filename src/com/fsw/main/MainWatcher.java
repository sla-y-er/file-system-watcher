package com.fsw.main;

import com.fsw.database.EventDatabase;
import com.fsw.email.EmailSender;
import com.fsw.query.QueryEngine;
import com.fsw.report.CsvReportWriter;
import com.fsw.report.ReportWriter;
import com.fsw.watcher.FileWatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class MainWatcher {

    private FileWatcher watcher;
    private QueryEngine engine;
    private Scanner scanner;
    private EventDatabase database;
    private EmailSender emailSender;

    public static void main(String[] args) {
        MainWatcher app = new MainWatcher();
        app.showMenu();
    }

    public void showMenu() {

        scanner = new Scanner(System.in);

        database = new EventDatabase("eventlog.db");
        database.connect();

        ReportWriter writer = new CsvReportWriter("reports");
        engine = new QueryEngine(database, writer);

        emailSender = new EmailSender();

        System.out.println("=================================");
        System.out.println("     File System Watcher");
        System.out.println("=================================");

        System.out.print("Enter folder path to monitor: ");

        String folderPath = scanner.nextLine();

        startWatch(folderPath);
    }

    public void startWatch(String folderPath) {

        Path path = Paths.get(folderPath);

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            System.out.println("Invalid folder path.");
            return;
        }

        watcher = new FileWatcher(path, database);

        System.out.println("Watching folder: "
                + path.toAbsolutePath());

        watcher.start();
    }

    public void startWatch() {
        showMenu();
    }
}