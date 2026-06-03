package Controllers;

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

public class LoginController {
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Button registerButton;
    
    @FXML
    private Button forgotPasswordButton;
    
    @FXML
    private Label errorLabel;
    
    private AuthService authService;
    private Stage stage;

    @FXML
    public void initialize() {
        this.authService = new AuthService();
        
        // Limpiar mensajes de error iniciales
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        
        // Configurar event handlers
        if (loginButton != null) {
            loginButton.setOnAction(event -> handleLogin());
        }
        
        if (registerButton != null) {
            registerButton.setOnAction(event -> handleRegisterClick());
        }
        
        if (forgotPasswordButton != null) {
            forgotPasswordButton.setOnAction(event -> handleForgotPasswordClick());
        }
        
        if (passwordField != null) {
            passwordField.setOnAction(event -> handleLogin());
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleLogin() {
        try {
            if (emailField == null || emailField.getText().trim().isEmpty()) {
                showError("Por favor ingresa tu correo electrónico.");
                return;
            }
            
            if (passwordField == null || passwordField.getText().isEmpty()) {
                showError("Por favor ingresa tu contraseña.");
                return;
            }
            
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            
            boolean isAuthenticated = authService.authenticate(email, password);
            
            if (isAuthenticated) {
                loadView("/View/DashboardView.fxml", "Dashboard");
            } else {
                showError(Constants.ERROR_INVALID_CREDENTIALS);
                passwordField.clear(); // Limpiar contraseña por seguridad
            }
            
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (RuntimeException e) {
            showError(Constants.ERROR_DATABASE);
            e.printStackTrace();
        } catch (IOException e) {
            showError("Error al cargar la vista: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegisterClick() {
        try {
            loadView("/View/RegisterView.fxml", "Registro");
        } catch (IOException e) {
            showError("Error al cargar la vista de registro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPasswordClick() {
        try {
            loadView("/View/ForgotPasswordView.fxml", "Recuperar Contraseña");
        } catch (IOException e) {
            showError("Error al cargar la vista de recuperación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath, String title) throws IOException {
        if (stage == null) {
            if (loginButton != null) {
                stage = (Stage) loginButton.getScene().getWindow();
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
        } else {
            // Si no hay label, mostrar Alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
}