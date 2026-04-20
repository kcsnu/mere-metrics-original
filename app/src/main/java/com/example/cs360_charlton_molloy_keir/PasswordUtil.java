package com.example.cs360_charlton_molloy_keir;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

    // Number of random bytes used for the password salt
    private static final int SALT_BYTES = 16;

    // Number of PBKDF2 iterations
    private static final int ITERATIONS = 100_000;

    // Size of the derived key in bits
    private static final int KEY_BITS = 256;

    // Random generator used for creating salts
    private static final SecureRandom RNG = new SecureRandom();

    // Private constructor to prevent creating instances of this utility class
    private PasswordUtil() { }

    // Function to create a salted hash for a plain-text password
    // Returns a HashResult containing Base64 salt and Base64 hash
    public static HashResult hashPassword(String password) {
        if (password == null) {
            return null;
        }

        // Do not allow empty passwords
        if (password.isEmpty()) {
            return null;
        }

        // Generate a random salt
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);

        // Create the PBKDF2 hash
        byte[] hash = pbkdf2(password, salt);

        // If hashing fails, return null so the caller can handle it
        if (hash == null || hash.length == 0) {
            return null;
        }

        // Encode salt and hash as Base64 so they can be stored as TEXT in SQLite
        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);

        // Check to avoid storing empty values
        if (saltB64 == null || saltB64.isEmpty() || hashB64 == null || hashB64.isEmpty()) {
            return null;
        }

        return new HashResult(saltB64, hashB64);
    }

    // Function to verify a plain-text password against a stored Base64 salt and Base64 hash
    public static boolean verifyPassword(String password, String saltB64, String expectedHashB64) {
        if (password == null || saltB64 == null || expectedHashB64 == null) {
            return false;
        }

        // Do not allow empty passwords
        if (password.isEmpty()) {
            return false;
        }

        try {
            // Decode Base64 salt and expected hash back to bytes
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            byte[] expected = Base64.decode(expectedHashB64, Base64.NO_WRAP);

            // If stored values are invalid or empty, fail verification
            if (salt == null || salt.length == 0 || expected == null || expected.length == 0) {
                return false;
            }

            // Hash the provided password with the stored salt
            byte[] actual = pbkdf2(password, salt);

            // If hashing fails, fail verification
            if (actual == null || actual.length == 0) {
                return false;
            }

            // Compare the two hashes
            return MessageDigest.isEqual(actual, expected);

        } catch (IllegalArgumentException e) {
            // Base64 decoding failed (stored strings are not valid Base64)
            return false;
        }
    }

    // Function to create a PBKDF2 hash (prefers HMAC-SHA256, falls back to HMAC-SHA1)
    private static byte[] pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);

            try {
                // Use PBKDF2 with SHA-256 if available
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                return factory.generateSecret(spec).getEncoded();
            } catch (Exception ignored) {
                // Fall back to PBKDF2 with SHA-1 on older devices
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                return factory.generateSecret(spec).getEncoded();
            }

        } catch (Exception e) {
            // If hashing fails, return null so callers can fail safely
            return null;
        }
    }

    // Class to hold the salt and hash strings
    public static final class HashResult {

        // Base64 encoded salt
        public final String saltB64;

        // Base64 encoded hash
        public final String hashB64;

        public HashResult(String saltB64, String hashB64) {
            this.saltB64 = saltB64;
            this.hashB64 = hashB64;
        }
    }
}