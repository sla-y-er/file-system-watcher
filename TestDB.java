package com.fsw.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDB {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:eventlog.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("------------------------------------------");
                System.out.println("SQLite database created.");
                System.out.println("------------------------------------------");
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}