package com.fsw.main;

import com.fsw.database.EventDatabase;
import com.fsw.gui.WatcherFrame;

import javax.swing.*;

/**
 * Application entry point.
 *
 * <p>Sets the native look and feel, opens the SQLite event database, and launches
 * the main {@link WatcherFrame} window on the Swing event-dispatch thread. If the
 * database cannot be opened (for example, the JDBC driver is missing from the
 * classpath), an error dialog is shown and the application exits.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class MainWatcher {

    /**
     * Starts the File System Watcher application.
     *
     * @param args command-line arguments (unused)
     */
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
