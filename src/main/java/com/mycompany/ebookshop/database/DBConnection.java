package com.mycompany.ebookshop.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    
    private static final String URL = "jdbc:mysql://localhost:3306/ebookshop?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=5000";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final long RETRY_COOLDOWN_MS = 10000;
    
    private static Connection connection = null;
    private static volatile long nextRetryAllowedAt = 0;
    private static volatile String lastErrorSignature = "";
    private static volatile boolean driverMissingLogged = false;
    
    
    public static synchronized Connection getConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }
        } catch (SQLException e) {
            connection = null;
        }

        long now = System.currentTimeMillis();
        if (now < nextRetryAllowedAt) {
            return null;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            nextRetryAllowedAt = 0;
            lastErrorSignature = "";
            driverMissingLogged = false;
            System.out.println(" Connected to MySQL Database!");
        } catch (ClassNotFoundException e) {
            if (!driverMissingLogged) {
                System.err.println(" MySQL Driver not found! Run the app with project dependencies (use VS Code launch config: 'Run eBookShop MainApp (with MySQL)').");
                driverMissingLogged = true;
            }
            connection = null;
            nextRetryAllowedAt = now + RETRY_COOLDOWN_MS;
        } catch (SQLException e) {
            String signature = e.getClass().getSimpleName() + ":" + safeMessage(e.getMessage());
            if (!signature.equals(lastErrorSignature)) {
                System.err.println(" Database connection failed: " + safeMessage(e.getMessage()));
                System.err.println("ℹ Retrying in " + (RETRY_COOLDOWN_MS / 1000) + " seconds...");
                lastErrorSignature = signature;
            }
            connection = null;
            nextRetryAllowedAt = now + RETRY_COOLDOWN_MS;
        }
        return connection;
    }
    
    
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
            connection = null;
        }
    }
    
    
    public static void testConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1 as test")) {
                    if (rs.next()) {
                        System.out.println("Database connection test passed!");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database test failed: " + e.getMessage());
        }
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown database error";
        }

        
        int newlineIndex = message.indexOf('\n');
        String firstLine = newlineIndex >= 0 ? message.substring(0, newlineIndex) : message;
        return firstLine.trim();
    }
}