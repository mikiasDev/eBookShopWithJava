package com.mycompany.ebookshop.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.mycompany.ebookshop.database.DBConnection;
import com.mycompany.ebookshop.utils.PasswordUtils;

public class CreateTestUser {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java CreateTestUser <username> <password> <email>");
            System.exit(1);
        }

        String username = args[0];
        String password = args[1];
        String email = args[2];

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.out.println("❌ Database connection failed!");
            return;
        }

        
        if (!PasswordUtils.isStrongPassword(password)) {
            System.err.println("Provided password does not meet strength requirements. Use at least 8 chars with lowercase, uppercase, digit and special character.");
            System.exit(2);
        }

        String hashedPassword = PasswordUtils.hashPassword(password);
        String query = "INSERT IGNORE INTO users (username, password, email, full_name, role) VALUES (?, ?, ?, ?, 'customer')";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, email);
            pstmt.setString(4, username);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Test user created successfully!");
                System.out.println("   Username: " + username);
                System.out.println("   Email: " + email);
                System.out.println("   Password: " + password + " (hashed)");
            } else {
                System.out.println("⚠️ User already exists or failed to insert");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error creating user: " + e.getMessage());
        }
    }
}
