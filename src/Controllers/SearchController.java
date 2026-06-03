package Controllers;

import Model.Conversation;
import Model.User;
import Service.ConversationService;
import Service.UserService;
import Util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SearchController {

    @FXML
    private TextField txtUserSearch;

    @FXML
    private ListView<String> userList;

    @FXML
    private Button btnCreateConversation;

    @FXML
    private Label lblStatus;

    private final UserService userService;
    private final ConversationService conversationService;
    private final ObservableList<String> userResults;
    private final List<User> currentUsers;
    private User currentUser;
    private DashboardController dashboardController;
    private Stage stage;

    public SearchController() {
        this.userService = new UserService();
        this.conversationService = new ConversationService();
        this.userResults = FXCollections.observableArrayList();
        this.currentUsers = new ArrayList<>();
    }

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (userList != null) {
            userList.setItems(userResults);
            userList.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    createPrivateConversation();
                }
            });
        }

        setStatus("Busca por nombre o correo.");
    }

    @FXML
    private void searchUsers() {
        if (currentUser == null) {
            showError("Usuario no autenticado.");
            return;
        }

        String keyword = txtUserSearch != null
                ? txtUserSearch.getText().trim()
                : "";

        if (keyword.isEmpty()) {
            clearResults();
            setStatus("Ingresa un nombre o correo.");
            return;
        }

        try {
            List<User> users = userService.searchUsers(keyword, currentUser.getId());
            currentUsers.clear();
            currentUsers.addAll(users);
            updateUserResults();
            setStatus(users.isEmpty()
                    ? "No se encontraron usuarios."
                    : users.size() + " usuario(s) encontrado(s).");
        } catch (Exception e) {
            showError("Error al buscar usuarios.");
            e.printStackTrace();
        }
    }

    @FXML
    private void createPrivateConversation() {
        if (currentUser == null) {
            showError("Usuario no autenticado.");
            return;
        }

        int selectedIndex = userList != null
                ? userList.getSelectionModel().getSelectedIndex()
                : -1;

        if (selectedIndex < 0 || selectedIndex >= currentUsers.size()) {
            showError("Selecciona un usuario.");
            return;
        }

        User selectedUser = currentUsers.get(selectedIndex);

        try {
            List<Integer> participantIds = new ArrayList<>();
            participantIds.add(selectedUser.getId());

            Conversation conversation = conversationService.createConversation(
                    currentUser.getId(),
                    participantIds);

            if (conversation == null) {
                showError("No fue posible crear la conversación.");
                return;
            }

            if (dashboardController != null) {
                dashboardController.refreshConversations();
            }

            closeWindow();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Error al crear la conversación.");
            e.printStackTrace();
        }
    }

    @FXML
    private void clearResults() {
        currentUsers.clear();
        userResults.clear();
    }

    @FXML
    private void closeWindow() {
        Stage currentStage = stage;

        if (currentStage == null && userList != null && userList.getScene() != null) {
            currentStage = (Stage) userList.getScene().getWindow();
        }

        if (currentStage != null) {
            currentStage.close();
        }
    }

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void updateUserResults() {
        userResults.clear();

        for (User user : currentUsers) {
            userResults.add(user.getName() + " <" + user.getEmail() + ">");
        }
    }

    private void setStatus(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
