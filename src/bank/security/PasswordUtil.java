package bank.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtil {

    public static final int PASSWORD_LENGTH = 6;

    private PasswordUtil() {
    }

    public static boolean isValidLength(String password) {
        return password != null && password.length() == PASSWORD_LENGTH;
    }

    public static String lengthRequirementMessage() {
        return "Password must be exactly " + PASSWORD_LENGTH + " characters.";
    }

    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String plain, String hash) {
        return hash(plain).equals(hash);
    }
}
