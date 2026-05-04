package com.fsw.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDB {
    public static void main(String[] args) {
        // This is the path to your new database file
        String url = "jdbc:sqlite:eventlog.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("------------------------------------------");
                System.out.println("✅ Success! SQLite database created.");
                System.out.println("Look at your project files for 'eventlog.db'");
                System.out.println("------------------------------------------");
            }
        } catch (SQLException e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
        }
    }
}