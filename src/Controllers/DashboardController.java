package Controllers;

import Model.Conversation;
import Model.User;
import Service.ConversationService;
import Service.UserService;
import Util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.concurrent.Task;
import Util.TaskManager;

public class DashboardController {

    @FXML
    private Label lblUsername;

    @FXML
    private TextField txtSearch;

    @FXML
    private ListView<String> conversationList;

    private final ConversationService conversationService;

    private User currentUser;

    private ObservableList<String> conversationNames;

    private List<Conversation> currentConversations;

    private Stage stage;

    private final UserService userService = new UserService();

    private final List<User> currentUsers = new ArrayList<>();

    private boolean showingUsers = false;

    public DashboardController() {
        this.conversationService = new ConversationService();
    }

    @FXML
    public void initialize() {

        currentUser = SessionManager.getInstance().getCurrentUser();

        conversationNames = FXCollections.observableArrayList();

        currentConversations = new ArrayList<>();

        if (currentUser != null && lblUsername != null) {
            lblUsername.setText(currentUser.getName());
        }

        if (conversationList != null) {

            conversationList.setItems(conversationNames);

            conversationList.setOnMouseClicked(event -> {

                if (event.getClickCount() == 2
                        && conversationList.getSelectionModel().getSelectedIndex() >= 0) {

                    openSelectedConversation();
                }
            });
        }

        loadConversations();
    }

    private void loadConversations() {
        showingUsers = false;

        currentUsers.clear();
        if (currentUser == null) {
            return;
        }

        Task<List<Conversation>> task = new Task<>() {

            @Override
            protected List<Conversation> call() throws Exception {
                return conversationService.getUserConversations(currentUser.getId());
            }
        };
        task.setOnSucceeded(event -> {

            currentConversations
                    = task.getValue();

            if (currentConversations == null) {
                currentConversations
                        = new ArrayList<>();
            }

            updateConversationList(
                    currentConversations);
        });

        task.setOnFailed(event -> {
            Throwable error = task.getException();

            showError("Error cargando conversaciones: " + error.getMessage());
        });

        TaskManager.getExecutor().submit(task);
    }

    public void refreshConversations() {
        loadConversations();
    }

    @FXML
    private void searchConversations() {

        if (currentUser == null) {
            return;
        }

        String keyword = txtSearch.getText().trim();

        if (keyword.isEmpty()) {
            showingUsers = false;
            loadConversations();
            return;
        }
        try {
            List<Conversation> conversations = conversationService.searchConversations(currentUser.getId(),keyword);
            if (!conversations.isEmpty()) {
                showingUsers = false;
                currentConversations.clear();
                currentConversations.addAll(conversations);
                updateConversationList(currentConversations);
                return;
            }

            List<User> users
                    = userService.searchUsers(
                            keyword,
                            currentUser.getId());

            showingUsers = true;

            currentUsers.clear();
            currentUsers.addAll(users);

            conversationNames.clear();

            for (User user : users) {
                conversationNames.add("👤 " + user.getName());
            }

        } catch (Exception e) {

            showError(
                    "Error en la búsqueda: "
                    + e.getMessage());

            e.printStackTrace();
        }
    }

    private void updateConversationList(
            List<Conversation> conversations) {

        conversationNames.clear();

        for (Conversation conversation : conversations) {

            String displayName
                    = conversationService.getConversationDisplayName(
                            conversation.getId(),
                            currentUser.getId());

            conversationNames.add(displayName);
        }
    }

    private void openSelectedConversation() {

        if (showingUsers) {

            openSelectedUser();

            return;
        }

        if (currentConversations == null
                || currentConversations.isEmpty()) {

            showError("No existen conversaciones.");
            return;
        }

        int selectedIndex
                = conversationList.getSelectionModel()
                        .getSelectedIndex();

        if (selectedIndex < 0
                || selectedIndex >= currentConversations.size()) {

            showError("Selecciona una conversación.");
            return;
        }

        Conversation selectedConversation
                = currentConversations.get(selectedIndex);

        try {

            loadChatView(selectedConversation);

        } catch (IOException e) {

            showError("No fue posible abrir la conversación.");

            e.printStackTrace();
        }
    }

    private void loadChatView(
            Conversation conversation)
            throws IOException {

        if (stage == null) {
            stage = (Stage) conversationList
                    .getScene()
                    .getWindow();
        }

        FXMLLoader loader
                = new FXMLLoader(
                        getClass().getResource(
                                "/View/ChatView.fxml"));

        Parent root = loader.load();

        ChatController controller
                = loader.getController();

        controller.setConversation(conversation);

        controller.setStage(stage);

        Scene scene = new Scene(root);

        stage.setScene(scene);

        stage.setTitle(
                "ChatConnect");
    }

    @FXML
    private void openProfile() {

        try {

            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    "/View/ProfileView.fxml"));

            Parent root = loader.load();

            Stage profileStage = new Stage();
            profileStage.setTitle("ChatConnect - Perfil");
            profileStage.setScene(new Scene(root));
            profileStage.initOwner(
                    lblUsername
                            .getScene()
                            .getWindow());

            profileStage.showAndWait();

        } catch (IOException e) {

            showError("No fue posible abrir el perfil.");

            e.printStackTrace();
        }
    }

    @FXML
    private void openSearch() {

        try {

            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    "/View/SearchView.fxml"));

            Parent root = loader.load();

            SearchController controller
                    = loader.getController();

            Stage searchStage = new Stage();
            searchStage.setTitle("ChatConnect - Buscar usuarios");
            searchStage.setScene(new Scene(root));
            searchStage.initOwner(
                    conversationList
                            .getScene()
                            .getWindow());

            controller.setDashboardController(this);
            controller.setStage(searchStage);

            searchStage.showAndWait();
            refreshConversations();

        } catch (IOException e) {

            showError("No fue posible abrir la busqueda de usuarios.");

            e.printStackTrace();
        }
    }

    @FXML
    private void openCreateGroup() {

        try {

            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    "/View/CreateGroupView.fxml"));

            Parent root = loader.load();

            CreateGroupController controller
                    = loader.getController();

            Stage groupStage = new Stage();
            groupStage.setTitle("ChatConnect - Crear grupo");
            groupStage.setScene(new Scene(root));
            groupStage.initOwner(
                    conversationList
                            .getScene()
                            .getWindow());

            controller.setDashboardController(this);
            controller.setStage(groupStage);

            groupStage.showAndWait();
            refreshConversations();

        } catch (IOException e) {

            showError("No fue posible abrir la creación de grupo.");

            e.printStackTrace();
        }
    }

    private void openSelectedUser() {

        int selectedIndex
                = conversationList
                        .getSelectionModel()
                        .getSelectedIndex();

        if (selectedIndex < 0
                || selectedIndex >= currentUsers.size()) {

            showError(
                    "Selecciona un usuario.");

            return;
        }

        User selectedUser
                = currentUsers.get(
                        selectedIndex);

        try {

            List<Integer> participants
                    = new ArrayList<>();

            participants.add(
                    selectedUser.getId());

            Conversation conversation
                    = conversationService
                            .createConversation(
                                    currentUser.getId(),
                                    participants);

            if (conversation == null) {

                showError(
                        "No fue posible crear la conversación.");

                return;
            }

            loadConversations();

        } catch (Exception e) {

            showError(
                    "Error creando conversación.");

            e.printStackTrace();
        }
    }

    @FXML
    private void logout() {

        try {

            SessionManager
                    .getInstance()
                    .clearSession();

            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    "/View/LoginView.fxml"));

            Parent root = loader.load();

            Stage currentStage
                    = (Stage) lblUsername
                            .getScene()
                            .getWindow();

            currentStage.setScene(
                    new Scene(root));

            currentStage.setTitle(
                    "ChatConnect - Login");

        } catch (IOException e) {

            showError(
                    "Error al cerrar sesión.");

            e.printStackTrace();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void showError(String message) {

        Alert alert
                = new Alert(Alert.AlertType.ERROR);

        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private void showInfo(
            String title,
            String message) {

        Alert alert
                = new Alert(
                        Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}
