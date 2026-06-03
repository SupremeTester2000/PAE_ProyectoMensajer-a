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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreateGroupController {

    @FXML
    private TextField txtGroupName;

    @FXML
    private ListView<String> userList;

    @FXML
    private Button btnCreateGroup;

    @FXML
    private Label lblStatus;

    private final UserService userService;
    private final ConversationService conversationService;
    private final ObservableList<String> userNames;
    private final List<User> availableUsers;
    private User currentUser;
    private DashboardController dashboardController;
    private Stage stage;

    public CreateGroupController() {
        this.userService = new UserService();
        this.conversationService = new ConversationService();
        this.userNames = FXCollections.observableArrayList();
        this.availableUsers = new ArrayList<>();
    }

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (userList != null) {
            userList.setItems(userNames);
            userList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }

        loadAvailableUsers();
    }

    private void loadAvailableUsers() {
        if (currentUser == null) {
            showError("Usuario no autenticado.");
            return;
        }

        try {
            List<User> users = userService.getAvailableUsers(currentUser.getId());
            availableUsers.clear();
            availableUsers.addAll(users);
            updateUserList();
            setStatus(users.isEmpty()
                    ? "No hay usuarios disponibles."
                    : "Selecciona los participantes del grupo.");
        } catch (Exception e) {
            showError("Error al cargar usuarios disponibles.");
            e.printStackTrace();
        }
    }

    @FXML
    private void createGroup() {
        if (currentUser == null) {
            showError("Usuario no autenticado.");
            return;
        }

        String groupName = txtGroupName != null ? txtGroupName.getText().trim() : "";
        if (groupName.isEmpty()) {
            showError("Ingresa un nombre para el grupo.");
            return;
        }

        List<Integer> participantIds = getSelectedParticipantIds();
        if (participantIds.isEmpty()) {
            showError("Selecciona al menos un participante.");
            return;
        }

        try {
            Conversation conversation = conversationService.createGroupConversation(
                    currentUser.getId(),
                    groupName,
                    participantIds);

            if (conversation == null) {
                showError("No fue posible crear el grupo.");
                return;
            }

            if (dashboardController != null) {
                dashboardController.refreshConversations();
            }

            closeWindow();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Error al crear el grupo.");
            e.printStackTrace();
        }
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

    private List<Integer> getSelectedParticipantIds() {
        List<Integer> participantIds = new ArrayList<>();

        if (userList == null) {
            return participantIds;
        }

        ObservableList<Integer> selectedIndices =
                userList.getSelectionModel().getSelectedIndices();

        for (Integer index : selectedIndices) {
            if (index != null && index >= 0 && index < availableUsers.size()) {
                participantIds.add(availableUsers.get(index).getId());
            }
        }

        return participantIds;
    }

    private void updateUserList() {
        userNames.clear();

        for (User user : availableUsers) {
            userNames.add(user.getName() + " <" + user.getEmail() + ">");
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
