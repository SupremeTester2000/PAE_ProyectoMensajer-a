package Controllers;

import Model.Conversation;
import Model.Message;
import Model.User;

import Service.MessageService;
import Service.UserService;

import Util.SessionManager;
import Util.TaskManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import java.io.IOException;
import javafx.concurrent.Task;

public class ChatController {

    @FXML
    private Label lblConversationName;

    @FXML
    private ListView<String> messageList;

    @FXML
    private TextArea txtMessageInput;

    @FXML
    private Button btnContactInfo;

    @FXML
    private Button btnGroupInfo;

    @FXML
    private Button btnDelete;

    private MessageService messageService;
    private UserService userService;
    private Conversation conversation;
    private DashboardController dashboardController;
    private Stage stage;
    private User currentUser;
    private User contactUser;
    private ObservableList<String> messageTexts;
    private List<Message> messages;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Metodo de inicializacion llamado automaticamente despues de cargar el
     * FXML.
     */
    @FXML
    public void initialize() {
        this.messageService = new MessageService();
        this.userService = new UserService();
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        this.messageTexts = FXCollections.observableArrayList();

        if (messageList != null) {
            messageList.setItems(messageTexts);
        }
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;

        // Mostrar nombre de la conversacion
        if (lblConversationName != null && conversation != null) {
            String displayName = conversation.getGroupName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "Conversacion Privada";
            }
            lblConversationName.setText(displayName);
        }

        if (conversation != null) {
            if ("PRIVATE".equals(conversation.getType())) {
                if (btnContactInfo != null) {
                    btnContactInfo.setVisible(true);
                }
                if (btnGroupInfo != null) {
                    btnGroupInfo.setVisible(false);
                }
            } else if ("GROUP".equals(conversation.getType())) {
                if (btnContactInfo != null) {
                    btnContactInfo.setVisible(false);
                }
                if (btnGroupInfo != null) {
                    btnGroupInfo.setVisible(true);
                }
            }
        }
        if ("PRIVATE".equals(conversation.getType())) {

            try {

                contactUser
                        = userService.getOtherParticipant(
                                conversation.getId(),
                                currentUser.getId());

                if (contactUser != null
                        && lblConversationName != null) {

                    lblConversationName.setText(
                            contactUser.getName());
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        loadMessages();
    }

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }


    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void loadMessages() {

        if (conversation == null
                || currentUser == null) {

            return;
        }

        Task<List<Message>> task = new Task<>() {
            @Override
            protected List<Message> call() throws Exception {
                return messageService.getMessages(conversation.getId());
            }
        };

        task.setOnSucceeded(event -> {

            messages = task.getValue();

            updateMessageList(messages);
        });

        task.setOnFailed(event -> {

            Throwable error
                    = task.getException();

            showError(
                    "Error al cargar mensajes: "
                    + error.getMessage());
        });

        TaskManager.getExecutor().submit(task);
    }

    private void updateMessageList(
            List<Message> messagesList) {

        messageTexts.clear();

        for (Message msg : messagesList) {

            String senderName;

            if (msg.getSenderId()
                    == currentUser.getId()) {

                senderName = "Tú";

            } else {

                senderName
                        = (contactUser != null)
                                ? contactUser.getName()
                                : "Usuario";
            }

            String time
                    = msg.getTimestamp() != null
                    ? msg.getTimestamp()
                            .format(timeFormatter)
                    : "--:--";

            String displayText
                    = String.format(
                            "[%s] %s: %s",
                            time,
                            senderName,
                            msg.getContent());

            messageTexts.add(displayText);
        }
    }

    @FXML
    private void sendMessage() {

        if (conversation == null || currentUser == null) {

            showError(
                    "No hay conversacion o usuario seleccionado.");

            return;
        }

        String messageContent
                = txtMessageInput != null
                        ? txtMessageInput.getText()
                        : "";

        if (messageContent == null
                || messageContent.trim().isEmpty()) {

            showError(
                    "El mensaje no puede estar vacio.");

            return;
        }

        Message message = new Message();

        message.setConversationId(
                conversation.getId());

        message.setSenderId(
                currentUser.getId());

        message.setContent(
                messageContent);

        message.setStatus(
                "SENT");

        Task<Boolean> task
                = new Task<>() {

            @Override
            protected Boolean call()
                    throws Exception {

                System.out.println(
                        "Enviando mensaje desde: "
                        + Thread.currentThread()
                                .getName());

                return messageService
                        .sendMessage(message);
            }
        };

        task.setOnSucceeded(event -> {

            boolean success
                    = task.getValue();

            if (success) {

                if (txtMessageInput != null) {

                    txtMessageInput.clear();
                }

                loadMessages();

                if (messageList != null
                        && messageTexts.size() > 0) {

                    messageList.scrollTo(
                            messageTexts.size() - 1);
                }

            } else {

                showError(
                        "No se pudo enviar el mensaje.");
            }
        });

        task.setOnFailed(event -> {

            Throwable error
                    = task.getException();

            showError(
                    "Error al enviar mensaje: "
                    + error.getMessage());

            error.printStackTrace();
        });

        TaskManager
                .getExecutor()
                .submit(task);
    }

    @FXML
    private void openContactInfo() {

        try {

            if (conversation == null) {

                showError("No hay conversación seleccionada.");
                return;
            }

            User contact
                    = userService.getOtherParticipant(
                            conversation.getId(),
                            currentUser.getId());

            if (contact == null) {

                showError(
                        "No se pudo obtener la información del contacto.");
                return;
            }

            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    "/View/ContactInfoView.fxml"));

            Parent root = loader.load();

            ContactInfoController controller
                    = loader.getController();

            controller.setContact(contact);

            Stage contactStage = new Stage();

            contactStage.setTitle(
                    "Información de Contacto");

            contactStage.setScene(
                    new Scene(root));

            contactStage.show();

        } catch (Exception e) {

            showError(
                    "Error al abrir contacto: "
                    + e.getMessage());

            e.printStackTrace();
        }
    }

    @FXML
    private void openGroupInfo() {
        showInfo("Info Grupo", "Informacion del grupo - Implementacion futura");
    }

    @FXML
    private void deleteMessage() {
        showInfo("Eliminar", "Funcionalidad de eliminar mensaje - Implementacion futura");
    }

    @FXML
    private void returnToDashboard() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/View/DashboardView.fxml"));

            Parent root = loader.load();

            Stage currentStage
                    = (Stage) messageList.getScene().getWindow();

            Scene scene = new Scene(root);

            currentStage.setScene(scene);

            currentStage.setTitle("ChatConnect - Dashboard");

            currentStage.show();

        } catch (IOException e) {

            showError(
                    "Error al volver al Dashboard: "
                    + e.getMessage());

            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
