package View;

import Service.AuthService;
import Util.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource("/View/LoginView.fxml")
                    );

            Scene scene =
                    new Scene(loader.load());

            scene.getStylesheets().add(
                    getClass()
                            .getResource("chatconnect.css")
                            .toExternalForm()
            );

            stage.setTitle("ChatConnect");
            stage.setScene(scene);
            stage.setResizable(true);

            // Marcar usuario como desconectado cuando se cierra la ventana (botón X)
            stage.setOnCloseRequest(event -> {
                markUserDisconnected();
            });

            stage.show();

        } catch (Exception e) {

            System.err.println(
                    "Error al cargar LoginView.fxml"
            );

            e.printStackTrace();
        }
    }

    /**
     * Llamado por JavaFX al cerrar la aplicación (cualquier vía).
     */
    @Override
    public void stop() throws Exception {
        markUserDisconnected();
        super.stop();
    }

    private void markUserDisconnected() {
        try {
            if (SessionManager.getInstance().isSessionActive()) {
                AuthService authService = new AuthService();
                authService.logout();
                System.out.println("[Main] Usuario marcado como desconectado al cerrar la app.");
            }
        } catch (Exception e) {
            System.err.println("[Main] Error al marcar desconectado: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}