package com.mycompany.ebookshop.database;

import java.sql.*;

public class DBConnection {
    
    private static final String URL = "jdbc:mysql://localhost:3306/ebookshop?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    
    private static Connection connection = null;
    
    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("✅ Connected to MySQL Database!");
            } catch (ClassNotFoundException e) {
                System.err.println("❌ MySQL Driver not found!");
            } catch (SQLException e) {
                System.err.println("❌ Database connection failed: " + e.getMessage());
            }
        }
        return connection;
    }
    
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 'Connected!' as status");
                if (rs.next()) {
                    System.out.println("✅ " + rs.getString("status"));
                }
                rs.close();
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("❌ Test failed: " + e.getMessage());
        }
    }
    
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}