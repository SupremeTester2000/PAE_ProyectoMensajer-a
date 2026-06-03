package Util;

import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(Constants.REGEX_EMAIL);

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        if (email.length() > Constants.MAX_EMAIL_LENGTH) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return false;
        }
        return password.length() <= Constants.MAX_PASSWORD_LENGTH;
    }

    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (name.length() > Constants.MAX_NAME_LENGTH) {
            return false;
        }
        return true;
    }
}