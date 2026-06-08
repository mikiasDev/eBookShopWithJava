package com.mycompany.ebookshop.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.PasswordUtils;

class UserDAOProfileUpdateTest {

    private Connection connection;
    private UserDAO userDAO;

    @BeforeEach
    void setup() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:ebookshop_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        userDAO = new UserDAO();

        try (PreparedStatement create = connection.prepareStatement(
                "CREATE TABLE users ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "username VARCHAR(50) NOT NULL UNIQUE,"
                        + "password VARCHAR(255) NOT NULL,"
                        + "email VARCHAR(100) NOT NULL UNIQUE,"
                        + "full_name VARCHAR(100),"
                        + "address VARCHAR(255),"
                        + "phone VARCHAR(30),"
                        + "role VARCHAR(20) DEFAULT 'customer'"
                        + ")")) {
            create.execute();
        }

        insertUser(1, "google_user", "oldPass", "google@example.com", "Google User");
        insertUser(2, "existing_user", "otherPass", "existing@example.com", "Existing User");
    }

    @AfterEach
    void teardown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            try (PreparedStatement drop = connection.prepareStatement("DROP ALL OBJECTS")) {
                drop.execute();
            }
            connection.close();
        }
    }

    @Test
    void updatesUsernameFullNameAndPassword() throws Exception {
        User currentUser = new User(1, "google_user", "google@example.com", "oldPass", "Google User", "", "", "customer");

        boolean updated = userDAO.updateUserProfile(connection, currentUser, "google_user_new", "Updated Name", "NewPass123");

        assertTrue(updated);

        try (PreparedStatement select = connection.prepareStatement(
                "SELECT username, full_name, password FROM users WHERE id = 1");
             ResultSet rs = select.executeQuery()) {
            assertTrue(rs.next());
            assertEquals("google_user_new", rs.getString("username"));
            assertEquals("Updated Name", rs.getString("full_name"));

            String storedPassword = rs.getString("password");
            assertNotNull(storedPassword);
            assertTrue(storedPassword.startsWith("pbkdf2$"));
            assertTrue(PasswordUtils.verifyPassword("NewPass123", storedPassword));
        }
    }

    @Test
    void rejectsUpdateWhenUsernameAlreadyUsedByAnotherUser() {
        User currentUser = new User(1, "google_user", "google@example.com", "oldPass", "Google User", "", "", "customer");

        boolean updated = userDAO.updateUserProfile(connection, currentUser, "existing_user", "Updated Name", "NewPass123");

        assertFalse(updated);
    }

    @Test
    void returnsFalseForInvalidUserId() {
        User invalidUser = new User(0, "google_user", "google@example.com", "oldPass", "Google User", "", "", "customer");

        boolean updated = userDAO.updateUserProfile(connection, invalidUser, "new_name", "Updated Name", "NewPass123");

        assertFalse(updated);
    }

    @Test
    void returnsTrueWhenNothingChanges() {
        User currentUser = new User(1, "google_user", "google@example.com", "oldPass", "Google User", "", "", "customer");

        boolean updated = userDAO.updateUserProfile(connection, currentUser, "google_user", "Google User", "");

        assertTrue(updated);
    }

    private void insertUser(int id, String username, String rawPassword, String email, String fullName) throws SQLException {
        String insertSql = "INSERT INTO users (id, username, password, email, full_name, address, phone, role) VALUES (?, ?, ?, ?, ?, '', '', 'customer')";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setInt(1, id);
            insert.setString(2, username);
            insert.setString(3, PasswordUtils.hashPassword(rawPassword));
            insert.setString(4, email);
            insert.setString(5, fullName);
            insert.executeUpdate();
        }
    }
}
