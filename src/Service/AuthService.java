package Service;

import DAO.UserDAO;
import Model.User;
import Util.Constants;
import Util.PasswordUtils;
import Util.SessionManager;
import Util.ValidationUtils;
import java.sql.SQLException;

public class AuthService {
    
    private UserDAO userDAO;
    private SessionManager sessionManager;
    
    public AuthService() {
        this.userDAO = new UserDAO();
        this.sessionManager = SessionManager.getInstance();
    }
    
    public boolean authenticate(String email, String password) throws IllegalArgumentException, RuntimeException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo no puede estar vacío.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        
        try {
            User user = userDAO.findByEmail(email);
            
            if (user == null) {
                return false;
            }
            
            if (PasswordUtils.verifyPassword(password, user.getPassword())) {
                sessionManager.setCurrentUser(user);
                // Marcar usuario como conectado en la base de datos
                try {
                    userDAO.updateUserConnected(user.getId(), true);
                    user.setConnected(true);
                } catch (SQLException ex) {
                    System.err.println("[AuthService] No se pudo actualizar estado conectado: " + ex.getMessage());
                }
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            throw new RuntimeException(Constants.ERROR_DATABASE, e);
        }
    }

    public User registerUser(String nombre, String correo, String contraseña) 
            throws IllegalArgumentException, RuntimeException {
        
        if (!ValidationUtils.isValidName(nombre)) {
            throw new IllegalArgumentException(Constants.ERROR_NAME_EMPTY);
        }
        
        if (!ValidationUtils.isValidEmail(correo)) {
            throw new IllegalArgumentException(Constants.ERROR_INVALID_EMAIL);
        }
        
        if (!ValidationUtils.isValidPassword(contraseña)) {
            throw new IllegalArgumentException(Constants.ERROR_PASSWORD_TOO_SHORT);
        }
        
        try {
            if (userDAO.emailExists(correo)) {
                throw new IllegalArgumentException(Constants.ERROR_EMAIL_ALREADY_EXISTS);
            }
            
            User newUser = new User();
            newUser.setName(nombre);
            newUser.setEmail(correo);
            newUser.setPassword(contraseña);
            newUser.setProfilePhoto("");
            newUser.setStatus("");
            newUser.setConnected(false);
            
            User createdUser = userDAO.create(newUser);
            
            if (createdUser == null) {
                throw new RuntimeException(Constants.ERROR_DATABASE);
            }
            
            return createdUser;
            
        } catch (SQLException e) {
            throw new RuntimeException(Constants.ERROR_DATABASE, e);
        }
    }

    public void logout() {
        // Marcar usuario como desconectado en la base de datos
        Util.SessionManager sm = sessionManager;
        Model.User u = sm.getCurrentUser();
        if (u != null) {
            try {
                userDAO.updateUserConnected(u.getId(), false);
            } catch (SQLException ex) {
                System.err.println("[AuthService] No se pudo actualizar estado desconectado: " + ex.getMessage());
            }
        }
        sessionManager.clearSession();
    }

    public boolean isSessionActive() {
        return sessionManager.isSessionActive();
    }

    public User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }
}
