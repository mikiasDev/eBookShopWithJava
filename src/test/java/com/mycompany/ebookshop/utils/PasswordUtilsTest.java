package com.mycompany.ebookshop.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.util.Base64;

import org.junit.jupiter.api.Test;

public class PasswordUtilsTest {

    @Test
    public void testHashAndVerify() {
        String password = "TestPass123!";
        String stored = PasswordUtils.hashPassword(password);
        assertNotNull(stored);
        assertTrue(stored.startsWith("pbkdf2$"));
        assertTrue(PasswordUtils.verifyPassword(password, stored));
        assertFalse(PasswordUtils.verifyPassword("wrong", stored));
    }

    @Test
    public void testLegacySha256Fallback() throws Exception {
        String password = "legacyPass";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        String base64 = Base64.getEncoder().encodeToString(hash);

        assertTrue(PasswordUtils.verifyPassword(password, base64));
        assertFalse(PasswordUtils.verifyPassword("nope", base64));
    }
}
