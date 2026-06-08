package com.mycompany.ebookshop.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.PasswordUtils;

public class UserDAO {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    private static final String DEFAULT_ADMIN_EMAIL = "mikiasmulu29@gmail.com";
    private static final String DEFAULT_ADMIN_FULL_NAME = "Administrator";
    private static final String DEFAULT_ADMIN_ADDRESS = "123 Gondar";
    private static final String DEFAULT_ADMIN_PHONE = "0918527553";
    private static final String DEFAULT_ADMIN_ROLE = "admin";

    public User authenticate(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            if (DEFAULT_ADMIN_USERNAME.equals(username) && DEFAULT_ADMIN_PASSWORD.equals(password)) {
                System.out.println("[DEBUG] Logged in with Local Admin Fallback");
                return new User(0, DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD, DEFAULT_ADMIN_FULL_NAME, DEFAULT_ADMIN_ADDRESS, DEFAULT_ADMIN_PHONE, DEFAULT_ADMIN_ROLE);
            }
            return null;
        }

        upgradeDefaultAdminPasswordIfNeeded(conn);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (PasswordUtils.verifyPassword(password, storedHash) || password.equals(storedHash)) {
                        if (needsPasswordUpgrade(storedHash)) {
                            int userId = rs.getInt("id");
                            String upgradedHash = PasswordUtils.hashPassword(password);
                            updateStoredPasswordHash(conn, userId, upgradedHash);
                        }
                        System.out.println("User found in DB: " + username);
                        return extractUserFromResultSet(rs);
                    } else {
                        System.out.println("Invalid password for user: " + username);
                    }
                } else {
                    System.out.println("User not found in DB: " + username);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return null;
    }

    public User getUserByOAuthIdentity(String provider, String subject) {
        Connection conn = DBConnection.getConnection();
        if (conn == null || provider == null || provider.isBlank() || subject == null || subject.isBlank()) return null;

        ensureOAuthColumns(conn);
        if (!hasColumn(conn, "users", "oauth_provider") || !hasColumn(conn, "users", "oauth_subject")) {
            return null;
        }

        String query = "SELECT * FROM users WHERE oauth_provider = ? AND oauth_subject = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, provider.trim().toLowerCase(Locale.ROOT));
            pstmt.setString(2, subject.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return null;
    }

    public boolean linkOAuthIdentity(int userId, String provider, String subject) {
        Connection conn = DBConnection.getConnection();
        if (conn == null || userId <= 0 || provider == null || provider.isBlank() || subject == null || subject.isBlank()) return false;

        ensureOAuthColumns(conn);
        if (!hasColumn(conn, "users", "oauth_provider") || !hasColumn(conn, "users", "oauth_subject")) {
            return false;
        }

        String query = "UPDATE users SET oauth_provider = ?, oauth_subject = ?, email_verified = 1 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, provider.trim().toLowerCase(Locale.ROOT));
            pstmt.setString(2, subject.trim());
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.linkOAuthIdentity error: " + e.getMessage());
            return false;
        }
    }

    public User registerOAuthUser(String provider, String subject, String email, String fullName) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return null;

        ensureOAuthColumns(conn);
        if (!hasColumn(conn, "users", "oauth_provider") || !hasColumn(conn, "users", "oauth_subject")) {
            return null;
        }

        String usernameBase = email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "google_user";
        String username = makeUniqueUsername(conn, usernameBase);
        String tempPassword = generateStrongRandomPassword();

        String query = "INSERT INTO users (username, password, email, full_name, role, oauth_provider, oauth_subject, email_verified) VALUES (?, ?, ?, ?, 'customer', ?, ?, 1)";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, PasswordUtils.hashPassword(tempPassword));
            pstmt.setString(3, email);
            pstmt.setString(4, fullName);
            pstmt.setString(5, provider == null ? "google" : provider.trim().toLowerCase(Locale.ROOT));
            pstmt.setString(6, subject);
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return getUserById(keys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.registerOAuthUser failed for email=" + email + ": " + e.getMessage());
        }
        return null;
    }

    public User getUserByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) return null;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return null;
    }

    public User getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) return null;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return null;
    }

    public boolean registerUser(User user) {
        Connection conn = DBConnection.getConnection();
        if (conn == null || user == null) return false;

        boolean hasProfileColumns = hasColumn(conn, "users", "full_name")
            && hasColumn(conn, "users", "address")
            && hasColumn(conn, "users", "phone");

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        columns.add("username");
        values.add(user.getUsername());
        columns.add("email");
        values.add(user.getEmail());
        
        String rawPassword = blankToEmpty(user.getPassword());
        if (!com.mycompany.ebookshop.utils.PasswordUtils.isStrongPassword(rawPassword)) {
            System.err.println("UserDAO.registerUser: rejected weak password for username=" + user.getUsername());
            return false;
        }
        columns.add("password");
        values.add(PasswordUtils.hashPassword(rawPassword));

        if (hasProfileColumns) {
            columns.add("full_name");
            values.add(blankToNull(user.getFullName()));
            columns.add("address");
            values.add(blankToNull(user.getAddress()));
            columns.add("phone");
            values.add(blankToNull(user.getPhone()));
        }

        columns.add("role");
        values.add(blankToNull(user.getRole()));

        String query = "INSERT INTO users (" + String.join(", ", columns) + ") VALUES ("
            + String.join(", ", Collections.nCopies(columns.size(), "?")) + ")";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.registerUser failed for username=" + user.getUsername() + ", email=" + user.getEmail() + ": " + e.getMessage());
            return false;
        }
    }

    public String resetPasswordByUserId(int userId, String newPassword) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return "Database connection is unavailable.";
        }

        String trimmedPassword = newPassword != null ? newPassword.trim() : "";
        if (trimmedPassword.isBlank()) {
            return "Please enter a new password.";
        }

        if (!PasswordUtils.isStrongPassword(trimmedPassword)) {
            return "The new password is too weak. It must be at least 8 characters and include lowercase, uppercase, a digit, and a special character.";
        }

        if (updateStoredPasswordHash(conn, userId, PasswordUtils.hashPassword(trimmedPassword))) {
            return null;
        }

        return "The password could not be updated. Please try again.";
    }

    public List<User> getAllUsers() {
        String query = "SELECT * FROM users";
        Connection conn = DBConnection.getConnection();
        if (conn == null) return new ArrayList<>();

        List<User> users = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return users;
    }

    public boolean updateUserProfile(User user, String newUsername, String newFullName, String newPassword) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }
        return updateUserProfile(conn, user, newUsername, newFullName, newPassword);
    }

    boolean updateUserProfile(Connection conn, User user, String newUsername, String newFullName, String newPassword) {
        if (user == null || user.getId() <= 0) {
            return false;
        }

        if (conn == null) {
            return false;
        }

        String trimmedUsername = newUsername != null ? newUsername.trim() : "";
        String trimmedFullName = newFullName != null ? newFullName.trim() : "";
        String rawPassword = newPassword != null ? newPassword : "";

        boolean hasFullNameColumn = hasColumn(conn, "users", "full_name");
        boolean usernameChanged = !trimmedUsername.isBlank() && !trimmedUsername.equals(user.getUsername());
        boolean fullNameChanged = hasFullNameColumn
            && !trimmedFullName.isBlank()
            && !trimmedFullName.equals(user.getFullName());
        boolean passwordChanged = !rawPassword.isBlank();

        if (!usernameChanged && !fullNameChanged && !passwordChanged) {
            return true;
        }

        if (usernameChanged && isUsernameTaken(conn, trimmedUsername, user.getId())) {
            return false;
        }

        StringBuilder query = new StringBuilder("UPDATE users SET ");
        List<Object> values = new ArrayList<>();

        if (usernameChanged) {
            query.append("username = ?");
            values.add(trimmedUsername);
        }

        if (fullNameChanged) {
            if (!values.isEmpty()) {
                query.append(", ");
            }
            query.append("full_name = ?");
            values.add(trimmedFullName);
        }

        if (passwordChanged) {
            
            if (!com.mycompany.ebookshop.utils.PasswordUtils.isStrongPassword(rawPassword)) {
                return false;
            }
            if (!values.isEmpty()) {
                query.append(", ");
            }
            query.append("password = ?");
            values.add(PasswordUtils.hashPassword(rawPassword));
        }

        query.append(" WHERE id = ?");
        values.add(user.getId());

        try (PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.updateUserProfile failed for user id=" + user.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password"),
            safeGetString(rs, "full_name", rs.getString("username")),
            safeGetString(rs, "address", ""),
            safeGetString(rs, "phone", ""),
            rs.getString("role")
        );
    }


    public User getUserById(int id) {
        String query = "SELECT * FROM users WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) return null;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return null;
    }
    private boolean hasColumn(Connection conn, String tableName, String columnName) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            String[] tableCandidates = {
                tableName,
                tableName.toUpperCase(Locale.ROOT),
                tableName.toLowerCase(Locale.ROOT)
            };
            String[] columnCandidates = {
                columnName,
                columnName.toUpperCase(Locale.ROOT),
                columnName.toLowerCase(Locale.ROOT)
            };

            for (String tableCandidate : tableCandidates) {
                for (String columnCandidate : columnCandidates) {
                    try (ResultSet columns = metaData.getColumns(null, null, tableCandidate, columnCandidate)) {
                        if (columns.next()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    private String makeUniqueUsername(Connection conn, String base) {
        String candidate = base == null || base.isBlank() ? "google_user" : base.replaceAll("[^A-Za-z0-9_]+", "_");
        if (candidate.isBlank()) candidate = "google_user";
        int i = 1;
        while (isUsernameTaken(conn, candidate, -1)) {
            candidate = base + "_" + i;
            i++;
            if (i > 1000) {
                candidate = "google_user_" + Math.abs(UUID.randomUUID().getMostSignificantBits());
                break;
            }
        }
        return candidate;
    }

    private String generateStrongRandomPassword() {
        return "Gg!" + UUID.randomUUID().toString().replace("-", "") + "9Aa";
    }

    private void ensureOAuthColumns(Connection conn) {
        if (conn == null) {
            return;
        }

        if (!hasColumn(conn, "users", "email_verified")) {
            executeSchemaUpdate(conn, "ALTER TABLE users ADD COLUMN email_verified TINYINT(1) DEFAULT 0");
        }

        if (!hasColumn(conn, "users", "oauth_provider")) {
            executeSchemaUpdate(conn, "ALTER TABLE users ADD COLUMN oauth_provider VARCHAR(50) DEFAULT NULL");
        }

        if (!hasColumn(conn, "users", "oauth_subject")) {
            executeSchemaUpdate(conn, "ALTER TABLE users ADD COLUMN oauth_subject VARCHAR(255) DEFAULT NULL");
        }
    }

    private void executeSchemaUpdate(Connection conn, String sql) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Failed to update users schema: " + e.getMessage());
        }
    }

    private String safeGetString(ResultSet rs, String columnName, String fallback) {
        try {
            String value = rs.getString(columnName);
            return value != null ? value : fallback;
        } catch (SQLException e) {
            return fallback;
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean needsPasswordUpgrade(String storedHash) {
        return storedHash != null && !storedHash.startsWith("pbkdf2$");
    }

    private boolean updateStoredPasswordHash(Connection conn, int userId, String hashedPassword) {
        String query = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to upgrade password hash for user id " + userId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean markEmailVerified(int userId) {
        String q = "UPDATE users SET email_verified = 1 WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;
        try (PreparedStatement pstmt = conn.prepareStatement(q)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.markEmailVerified error: " + e.getMessage());
            return false;
        }
    }

    private void upgradeDefaultAdminPasswordIfNeeded(Connection conn) {
        String selectQuery = "SELECT id, password, email FROM users WHERE username = 'admin' LIMIT 1";
        try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
             ResultSet rs = selectStmt.executeQuery()) {
            if (!rs.next()) {
                return;
            }

            int adminId = rs.getInt("id");
            String storedPassword = rs.getString("password");
            String storedEmail = rs.getString("email");

            boolean needsPasswordUpdate = !PasswordUtils.verifyPassword(DEFAULT_ADMIN_PASSWORD, storedPassword)
                    && !DEFAULT_ADMIN_PASSWORD.equals(storedPassword);
            boolean needsEmailUpdate = storedEmail == null || !DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(storedEmail.trim());

            if (needsPasswordUpdate || needsEmailUpdate) {
                try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET password = ?, email = ? WHERE id = ?")) {
                    updateStmt.setString(1, PasswordUtils.hashPassword(DEFAULT_ADMIN_PASSWORD));
                    updateStmt.setString(2, DEFAULT_ADMIN_EMAIL);
                    updateStmt.setInt(3, adminId);
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to check/upgrade default admin password: " + e.getMessage());
        }
    }

    private boolean isUsernameTaken(Connection conn, String username, int currentUserId) {
        String query = "SELECT id FROM users WHERE username = ? AND id <> ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Failed to validate username uniqueness: " + e.getMessage());
            return true;
        }
    }
}
