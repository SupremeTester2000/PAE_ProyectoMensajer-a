package Controllers;

import Model.User;
import Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class ContactInfoController {

    @FXML
    private Button btnBack;

    @FXML
    private Label lblName;

    @FXML
    private Label lblEmail;

    @FXML
    private Label lblStatus;

    @FXML
    private ImageView imgProfile;

    private UserService userService;

    private User contact;

    public ContactInfoController() {
        userService = new UserService();
    }

    @FXML
    public void initialize() {

    }

    public void setContact(User contact) {

        this.contact = contact;

        loadContactInfo();
    }

    private void loadContactInfo() {

        if (contact == null) {
            return;
        }

        lblName.setText(contact.getName());
        lblEmail.setText(contact.getEmail());

    }

    @FXML
    private void blockContact() {

    }

    @FXML
    private void goBack() {

        Stage stage
                = (Stage) btnBack
                        .getScene()
                        .getWindow();

        stage.close();
    }
}
