package View;

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
            stage.show();

        } catch (Exception e) {

            System.err.println(
                    "Error al cargar LoginView.fxml"
            );

            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}