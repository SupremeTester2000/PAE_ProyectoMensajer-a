package Controllers;

import Model.Conversation;
import Model.User;
import Network.SocketClientManager;
import Service.ConversationService;
import Service.UserService;
import Util.SessionManager;
import Util.TaskManager;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final SocketClientManager socketClientManager;
    private final ObservableList<String> userResults;
    private final List<User> currentUsers;
    private User currentUser;
    private DashboardController dashboardController;
    private Stage stage;
    private ScheduledExecutorService presenceRefreshExecutor;

    public SearchController() {
        this.userService = new UserService();
        this.conversationService = new ConversationService();
        this.socketClientManager = SocketClientManager.getInstance();
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

        // Registrar listener para cambios de estado de conexión
        socketClientManager.addStatusListener(status -> {
            Platform.runLater(this::refreshUserStatus);
        });

        loadAllUsers();
        startPresenceRefresh();
    }

    @FXML
    private void loadAllUsers() {
        if (currentUser == null) {
            showError("Usuario no autenticado.");
            return;
        }

        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                return userService.getAvailableUsers(currentUser.getId());
            }
        };

        task.setOnSucceeded(event -> {
            List<User> users = task.getValue();
            currentUsers.clear();
            currentUsers.addAll(users);
            updateUserResults();
            setStatus(users.isEmpty()
                    ? "No hay usuarios disponibles."
                    : users.size() + " usuario(s) disponible(s).");
        });

        task.setOnFailed(event -> {
            showError("Error al cargar usuarios.");
            event.getSource().getException().printStackTrace();
        });

        TaskManager.getExecutor().submit(task);
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
            loadAllUsers();
            return;
        }

        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                return userService.searchUsers(keyword, currentUser.getId());
            }
        };

        task.setOnSucceeded(event -> {
            List<User> users = task.getValue();
            currentUsers.clear();
            currentUsers.addAll(users);
            updateUserResults();
            setStatus(users.isEmpty()
                    ? "No se encontraron usuarios."
                    : users.size() + " usuario(s) encontrado(s).");
        });

        task.setOnFailed(event -> {
            showError("Error al buscar usuarios.");
            event.getSource().getException().printStackTrace();
        });

        TaskManager.getExecutor().submit(task);
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
        // Detener el refresco de presencia cuando se cierra la ventana
        if (stage != null) {
            stage.setOnCloseRequest(event -> stopPresenceRefresh());
        }
    }

    // --- Refresco de presencia ---

    private void startPresenceRefresh() {
        stopPresenceRefresh();
        presenceRefreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "presence-refresh");
            t.setDaemon(true);
            return t;
        });
        presenceRefreshExecutor.scheduleAtFixedRate(() -> {
            try {
                if (currentUser == null) return;
                List<User> updated = userService.getAvailableUsers(currentUser.getId());
                Platform.runLater(() -> {
                    currentUsers.clear();
                    currentUsers.addAll(updated);
                    updateUserResults();
                });
            } catch (Exception e) {
                System.err.println("[SearchController] Error refrescando presencia: " + e.getMessage());
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    private void stopPresenceRefresh() {
        if (presenceRefreshExecutor != null && !presenceRefreshExecutor.isShutdown()) {
            presenceRefreshExecutor.shutdownNow();
        }
    }

    private void updateUserResults() {
        userResults.clear();

        for (User user : currentUsers) {
            String statusIndicator = user.isConnected() ? "● " : "○ ";
            String statusText = user.isConnected() ? "En línea" : "Desconectado";
            userResults.add(statusIndicator + user.getName() + " (" + user.getEmail() + ") - " + statusText);
        }
    }

    private void refreshUserStatus() {
        // Actualizar indicadores de estado sin recargar la lista completa
        updateUserResults();
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
