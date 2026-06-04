package Controllers;

import Model.User;
import Service.AuthService;
import Util.Constants;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;


public class RegisterController {
    
    @FXML
    private TextField nameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Button registerButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private Label successLabel;
    
    private AuthService authService;
    private Stage stage;
    
    @FXML
    public void initialize() {
        this.authService = new AuthService();
        
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        if (successLabel != null) {
            successLabel.setText("");
        }
        
        if (registerButton != null) {
            registerButton.setOnAction(event -> handleRegister());
        }
        
        if (backButton != null) {
            backButton.setOnAction(event -> handleBackToLogin());
        }
        
        if (confirmPasswordField != null) {
            confirmPasswordField.setOnAction(event -> handleRegister());
        }
    }
   
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleRegister() {
        try {
            if (nameField == null || nameField.getText().trim().isEmpty()) {
                showError("Por favor ingresa tu nombre.");
                return;
            }
            
            if (emailField == null || emailField.getText().trim().isEmpty()) {
                showError("Por favor ingresa tu correo electrónico.");
                return;
            }
            
            if (passwordField == null || passwordField.getText().isEmpty()) {
                showError("Por favor ingresa una contraseña.");
                return;
            }
            
            if (confirmPasswordField == null || confirmPasswordField.getText().isEmpty()) {
                showError("Por favor confirma tu contraseña.");
                return;
            }
            
            if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                showError("Las contraseñas no coinciden.");
                return;
            }
            
            String nombre = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            
            User newUser = authService.registerUser(nombre, email, password);
            
            if (newUser != null) {
                showSuccess(Constants.SUCCESS_REGISTRATION);
                
                nameField.clear();
                emailField.clear();
                passwordField.clear();
                confirmPasswordField.clear();
                
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(this::handleBackToLogin);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showError(Constants.ERROR_DATABASE);
            }
            
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (RuntimeException e) {
            showError(Constants.ERROR_DATABASE);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleBackToLogin() {
        try {
            loadView("/View/LoginView.fxml", Constants.APP_NAME);
        } catch (IOException e) {
            showError("Error al cargar la vista de login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath, String title) throws IOException {
        // Si no tenemos Stage, intentar obtenerlo desde los campos
        if (stage == null) {
            if (registerButton != null) {
                stage = (Stage) registerButton.getScene().getWindow();
            } else if (emailField != null) {
                stage = (Stage) emailField.getScene().getWindow();
            }
        }
        
        if (stage == null) {
            throw new RuntimeException("No se pudo obtener el Stage de la aplicación.");
        }
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        
        Object controller = loader.getController();
        if (controller instanceof LoginController) {
            ((LoginController) controller).setStage(stage);
        } else if (controller instanceof RegisterController) {
            ((RegisterController) controller).setStage(stage);
        }
        
        Scene scene = new Scene(root);
        
        try {
            String css = getClass().getResource("/View/chatconnect.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (NullPointerException e) {
            System.out.println("CSS no encontrado, continuando sin estilos.");
        }
        
        stage.setScene(scene);
        stage.setTitle(title);
    }
    
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: red;");
        }
        
        if (successLabel != null) {
            successLabel.setText("");
        }
    }

    private void showSuccess(String message) {
        if (successLabel != null) {
            successLabel.setText(message);
            successLabel.setStyle("-fx-text-fill: green;");
        }
        
        // Limpiar etiqueta de error
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }
}