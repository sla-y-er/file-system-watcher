package com.fsw.main;

import com.fsw.database.EventDatabase;
import com.fsw.gui.WatcherFrame;

import javax.swing.*;

public class MainWatcher {

    public static void main(String[] args) {
        // Use the OS look-and-feel so the app feels native on Windows
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        EventDatabase database = new EventDatabase("eventlog.db");
        try {
            database.connect();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Failed to open the database:\n" + e.getMessage() +
                "\n\nMake sure sqlite-jdbc-3.45.3.0.jar is on the classpath.",
                "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            WatcherFrame frame = new WatcherFrame(database);
            frame.setVisible(true);
        });
    }
}
