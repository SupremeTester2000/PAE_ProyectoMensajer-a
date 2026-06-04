package Util;

public final class Constants {

    private Constants(){}

    public static final String APP_NAME = "ChatConnect";

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_READ = "READ";

    public static final String PRIVATE_CHAT = "PRIVATE";
    public static final String GROUP_CHAT = "GROUP";
    
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 255;
    public static final int MIN_NAME_LENGTH = 1;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 150;
    public static final String REGEX_EMAIL = "^[A-Za-z0-9+_.-]+@(.+)$";
    
    public static final String ERROR_INVALID_EMAIL = "El correo electrónico no tiene un formato válido.";
    public static final String ERROR_PASSWORD_TOO_SHORT = "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.";
    public static final String ERROR_NAME_EMPTY = "El nombre no puede estar vacío.";
    public static final String ERROR_EMAIL_ALREADY_EXISTS = "El correo electrónico ya está registrado.";
    public static final String ERROR_INVALID_CREDENTIALS = "Correo o contraseña incorrectos.";
    public static final String ERROR_DATABASE = "Error en la base de datos. Intenta más tarde.";
    public static final String ERROR_UNEXPECTED = "Ha ocurrido un error inesperado.";
    
    public static final String SUCCESS_REGISTRATION = "Usuario registrado exitosamente.";
    public static final String SUCCESS_LOGIN = "Inicio de sesión exitoso.";
}
