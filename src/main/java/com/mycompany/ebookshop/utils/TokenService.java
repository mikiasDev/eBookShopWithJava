package com.mycompany.ebookshop.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.mycompany.ebookshop.database.DBConnection;

public class TokenService {

    private static final SecureRandom RNG = new SecureRandom();

    
    public String createToken(int userId, String type, long secondsValid) {
        String raw;
        
        if ("reset".equalsIgnoreCase(type)) {
            raw = generateNumericToken();
        } else {
            raw = generateRandomToken();
        }
        String hash = hashToken(raw);
        Instant expires = Instant.now().plusSeconds(secondsValid);

        String deleteSql = "DELETE FROM user_tokens WHERE user_id = ? AND type = ? AND used = 0";
        String sql = "INSERT INTO user_tokens (user_id, token_hash, type, expires_at, used) VALUES (?, ?, ?, ?, 0)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement deleteStmt = conn.prepareStatement(deleteSql); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            deleteStmt.setInt(1, userId);
            deleteStmt.setString(2, type);
            deleteStmt.executeUpdate();

            pstmt.setInt(1, userId);
            pstmt.setString(2, hash);
            pstmt.setString(3, type);
            pstmt.setTimestamp(4, Timestamp.from(expires));
            pstmt.executeUpdate();
            return raw;
        } catch (SQLException e) {
            System.err.println("TokenService.createToken error: " + e.getMessage());
            return null;
        }
    }

    private String generateRandomToken() {
        
        byte[] b = new byte[16];
        RNG.nextBytes(b);
        String rand = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        return UUID.randomUUID().toString() + "." + rand;
    }

    private String generateNumericToken() {
        int v = RNG.nextInt(1_000_000); 
        return String.format("%06d", v);
    }

    private String hashToken(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    
    public int verifyToken(String rawToken, String type) {
        if (rawToken == null || rawToken.isBlank()) return -1;
        String hash = hashToken(rawToken);
        String select = "SELECT id, user_id, expires_at, used FROM user_tokens WHERE token_hash = ? AND type = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(select)) {
            pstmt.setString(1, hash);
            pstmt.setString(2, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) return -1;
                long id = rs.getLong("id");
                int userId = rs.getInt("user_id");
                boolean used = rs.getInt("used") != 0;
                if (used) return -1;
                Timestamp expiresAt = rs.getTimestamp("expires_at");
                if (expiresAt == null) return -1;
                Instant expiresAtInstant = expiresAt.toInstant();
                if (Instant.now().isAfter(expiresAtInstant)) return -1;

                
                String update = "UPDATE user_tokens SET used = 1 WHERE id = ?";
                try (PreparedStatement up = conn.prepareStatement(update)) {
                    up.setLong(1, id);
                    up.executeUpdate();
                }
                return userId;
            }
        } catch (SQLException e) {
            System.err.println("TokenService.verifyToken error: " + e.getMessage());
            return -1;
        }
    }
}
