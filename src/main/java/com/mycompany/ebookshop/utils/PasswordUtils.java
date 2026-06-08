package com.mycompany.ebookshop.utils;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtils {
    private static final int ITERATIONS = 10000; 
    private static final int KEY_LENGTH = 256; 

    
    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            SecureRandom.getInstanceStrong().nextBytes(salt);

            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            return String.format("pbkdf2$%d$%s$%s", ITERATIONS,
                    Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(hash));
        } catch (GeneralSecurityException e) {
            
            return sha256Base64(password);
        }
    }

    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (storedHash == null) return false;
        if (storedHash.startsWith("pbkdf2$")) {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4 || !parts[1].matches("\\d+") || !isBase64(parts[2]) || !isBase64(parts[3])) {
                return false;
            }

            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] hash = Base64.getDecoder().decode(parts[3]);

                byte[] testHash = pbkdf2(plainPassword.toCharArray(), salt, iterations, hash.length * 8);
                return slowEquals(hash, testHash);
            } catch (GeneralSecurityException e) {
                return false;
            }
        }

        
        return sha256Base64(plainPassword).equals(storedHash);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return f.generateSecret(spec).getEncoded();
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.min(a.length, b.length); i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

    private static boolean isBase64(String value) {
        return value != null && value.matches("[A-Za-z0-9+/=]+");
    }

    private static String sha256Base64(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    
    public static boolean isStrongPassword(String password) {
        if (password == null) return false;
        String p = password.trim();
        if (p.length() < 8) return false;
        boolean hasLower = p.matches(".*[a-z].*");
        boolean hasUpper = p.matches(".*[A-Z].*");
        boolean hasDigit = p.matches(".*[0-9].*");
        
        return hasLower && hasUpper && hasDigit;
    }
}