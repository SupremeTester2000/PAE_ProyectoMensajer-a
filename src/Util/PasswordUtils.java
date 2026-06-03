package Util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    public static String hashPassword(String password) {
        try {

            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] hashedPassword = digest.digest(password.getBytes());

            String saltEncoded = Base64.getEncoder().encodeToString(salt);
            String hashEncoded = Base64.getEncoder().encodeToString(hashedPassword);

            return saltEncoded + ":" + hashEncoded;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar hash de contraseña", e);
        }
    }

    public static boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }

            String saltEncoded = parts[0];
            String hashEncoded = parts[1];

            byte[] salt = Base64.getDecoder().decode(saltEncoded);

            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] hashedPassword = digest.digest(password.getBytes());

            String generatedHash = Base64.getEncoder().encodeToString(hashedPassword);

            return generatedHash.equals(hashEncoded);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al verificar contraseña", e);
        }
    }
}
