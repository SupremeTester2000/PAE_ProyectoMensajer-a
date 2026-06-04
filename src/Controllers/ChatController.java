package Controllers;

import Model.Attachment;
import Model.Conversation;
import Model.Message;
import Model.User;

import Network.NetworkMessage;
import Network.SocketClientManager;
import Service.AttachmentService;
import Service.MessageService;
import Service.UserService;

import Util.SessionManager;
import Util.TaskManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatController {

    @FXML private Label lblConversationName;
    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextArea txtMessageInput;
    @FXML private Button btnContactInfo;
    @FXML private Button btnGroupInfo;
    @FXML private Button btnDelete;
    @FXML private Button btnAttach;

    private MessageService     messageService;
    private UserService        userService;
    private AttachmentService  attachmentService;
    private SocketClientManager socketClientManager;
    private Conversation       conversation;
    private DashboardController dashboardController;
    private Stage              stage;
    private User               currentUser;
    private User               contactUser;
    private List<File>         selectedAttachments;

    /** IDs de mensajes ya renderizados en la UI */
    private final Set<Integer>         displayedMessageIds = new HashSet<>();
    /** Mapeo msgId -> nodo VBox en el contenedor, para poder borrar de la UI */
    private final Map<Integer, Node>   messageNodes        = new HashMap<>();

    private ScheduledExecutorService chatRefreshExecutor;
    private final DateTimeFormatter  timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    // ---- Holder para pasar datos entre hilo BG y FX thread ----
    private record MsgWithAttachments(Message msg, List<Attachment> attachments) {}

    // =========================================================
    // Init
    // =========================================================

    @FXML
    public void initialize() {
        messageService      = new MessageService();
        userService         = new UserService();
        attachmentService   = new AttachmentService();
        socketClientManager = SocketClientManager.getInstance();
        currentUser         = SessionManager.getInstance().getCurrentUser();
        selectedAttachments = new ArrayList<>();
        socketClientManager.addMessageListener(this::onNetworkMessageReceived);
    }

    // =========================================================
    // Conversation setup
    // =========================================================

    public void setConversation(Conversation conv) {
        this.conversation = conv;

        if (lblConversationName != null && conv != null) {
            String n = conv.getGroupName();
            lblConversationName.setText((n == null || n.isEmpty()) ? "Conversacion Privada" : n);
        }
        if (conv != null) {
            boolean priv = "PRIVATE".equals(conv.getType());
            if (btnContactInfo != null) btnContactInfo.setVisible(priv);
            if (btnGroupInfo   != null) btnGroupInfo.setVisible(!priv);
        }
        if ("PRIVATE".equals(conv.getType())) {
            try {
                contactUser = userService.getOtherParticipant(conv.getId(), currentUser.getId());
                if (contactUser != null && lblConversationName != null)
                    lblConversationName.setText(contactUser.getName());
            } catch (Exception e) { e.printStackTrace(); }
        }

        // Clear previous state
        displayedMessageIds.clear();
        messageNodes.clear();
        if (messageContainer != null) messageContainer.getChildren().clear();

        loadMessages();
        startChatAutoRefresh();
    }

    public void setDashboardController(DashboardController d) { dashboardController = d; }
    public void setStage(Stage s)                             { stage = s; }

    // =========================================================
    // Load messages (background)
    // =========================================================

    @FXML
    private void loadMessages() {
        if (conversation == null || currentUser == null) return;

        Task<List<MsgWithAttachments>> task = new Task<>() {
            @Override protected List<MsgWithAttachments> call() throws Exception {
                List<Message> msgs = messageService.getMessages(conversation.getId());
                List<MsgWithAttachments> result = new ArrayList<>();
                for (Message m : msgs) {
                    List<Attachment> atts = attachmentService.getAttachmentsByMessage(m.getId());
                    result.add(new MsgWithAttachments(m, atts));
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            displayedMessageIds.clear();
            messageNodes.clear();
            if (messageContainer != null) messageContainer.getChildren().clear();
            for (MsgWithAttachments mwa : task.getValue()) {
                renderMessage(mwa.msg(), mwa.attachments());
            }
            scrollToBottom();
        });
        task.setOnFailed(e ->
            showError("Error al cargar mensajes: " + task.getException().getMessage()));

        TaskManager.getExecutor().submit(task);
    }

    // =========================================================
    // Send message
    // =========================================================

    @FXML
    private void sendMessage() {
        System.out.println("sendMessage ejecutado");
        if (conversation == null || currentUser == null) {
            showError("No hay conversacion o usuario seleccionado.");
            return;
        }
        String content = txtMessageInput != null ? txtMessageInput.getText().trim() : "";
        if (content.isEmpty() && selectedAttachments.isEmpty()) {
            showError("El mensaje no puede estar vacio.");
            return;
        }

        Message msg = new Message();
        msg.setConversationId(conversation.getId());
        msg.setSenderId(currentUser.getId());
        msg.setContent(content.isEmpty() ? "[Adjuntos]" : content);
        msg.setStatus("SENT");
        msg.setTimestamp(LocalDateTime.now());

        List<File> toSend = new ArrayList<>(selectedAttachments);
        selectedAttachments.clear();

        Task<MsgWithAttachments> task = new Task<>() {
            @Override protected MsgWithAttachments call() throws Exception {
                System.out.println("Insertando mensaje en BD: " + msg.getContent());
                boolean saved = messageService.sendMessage(msg);
                if (!saved) return null;
                List<Attachment> atts = new ArrayList<>();
                if (msg.getId() > 0 && !toSend.isEmpty()) {
                    for (File f : toSend) {
                        try {
                            Attachment att = attachmentService.saveAttachment(f, msg.getId());
                            if (att != null) atts.add(att);
                        } catch (Exception ex) {
                            System.err.println("Error adjunto: " + ex.getMessage());
                        }
                    }
                }
                return new MsgWithAttachments(msg, atts);
            }
        };

        task.setOnSucceeded(e -> {
            MsgWithAttachments result = task.getValue();
            if (result != null) {
                renderMessage(result.msg(), result.attachments());
                scrollToBottom();
                if (txtMessageInput != null) txtMessageInput.clear();
            } else {
                showError("No se pudo enviar el mensaje.");
            }
        });
        task.setOnFailed(e -> {
            showError("Error: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });
        TaskManager.getExecutor().submit(task);
    }

    // =========================================================
    // Delete message
    // =========================================================

    private void deleteMessageById(int msgId, Node nodeToRemove) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar mensaje");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Seguro que deseas eliminar este mensaje y sus archivos?");
        Optional<ButtonType> answer = confirm.showAndWait();

        if (answer.isEmpty() || answer.get() != ButtonType.OK) return;

        TaskManager.getExecutor().submit(() -> {
            try {
                // Delete attachments first
                attachmentService.deleteAttachmentsByMessage(msgId);
                // Delete message
                messageService.deleteMessage(msgId);

                Platform.runLater(() -> {
                    displayedMessageIds.remove(msgId);
                    messageNodes.remove(msgId);
                    if (messageContainer != null) {
                        messageContainer.getChildren().remove(nodeToRemove);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Error al eliminar: " + ex.getMessage()));
            }
        });
    }

    // =========================================================
    // Network (socket) message received
    // =========================================================

    private void onNetworkMessageReceived(NetworkMessage nm) {
        if (conversation == null || nm.getConversationId() != conversation.getId()) return;
        if (displayedMessageIds.contains(nm.getMessageId())) return;

        Platform.runLater(() -> {
            try {
                String sender = (nm.getSenderId() == currentUser.getId()) ? "Tu"
                        : (nm.getSenderName() != null ? nm.getSenderName() : "Usuario");
                String time = nm.getTimestamp() != null
                        ? nm.getTimestamp().substring(11, 16) : "--:--";

                Message fake = new Message();
                fake.setId(nm.getMessageId());
                fake.setSenderId(nm.getSenderId());
                fake.setContent(nm.getContent());
                fake.setTimestamp(LocalDateTime.now());

                renderMessage(fake, List.of());
                scrollToBottom();
            } catch (Exception ex) {
                System.err.println("[ChatController] Socket msg error: " + ex.getMessage());
            }
        });
    }

    // =========================================================
    // Auto-refresh (background thread, no DB on FX thread)
    // =========================================================

    private void startChatAutoRefresh() {
        stopChatAutoRefresh();
        chatRefreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "chat-refresh");
            t.setDaemon(true);
            return t;
        });
        chatRefreshExecutor.scheduleAtFixedRate(() -> {
            if (conversation == null) return;
            try {
                List<Message> all = messageService.getMessages(conversation.getId());
                // Only fetch attachments for new messages (not already displayed)
                List<MsgWithAttachments> newMsgs = new ArrayList<>();
                for (Message m : all) {
                    if (!displayedMessageIds.contains(m.getId())) {
                        List<Attachment> atts = attachmentService.getAttachmentsByMessage(m.getId());
                        newMsgs.add(new MsgWithAttachments(m, atts));
                    }
                }
                if (!newMsgs.isEmpty()) {
                    Platform.runLater(() -> {
                        for (MsgWithAttachments mwa : newMsgs) {
                            renderMessage(mwa.msg(), mwa.attachments());
                        }
                        scrollToBottom();
                    });
                }
            } catch (Exception e) {
                System.err.println("[ChatController] Refresh error: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void stopChatAutoRefresh() {
        if (chatRefreshExecutor != null && !chatRefreshExecutor.isShutdown()) {
            chatRefreshExecutor.shutdownNow();
        }
    }

    // =========================================================
    // Render a message node (always on FX thread)
    // =========================================================

    private void renderMessage(Message msg, List<Attachment> attachments) {
        if (displayedMessageIds.contains(msg.getId())) return;
        displayedMessageIds.add(msg.getId());

        boolean isOwn = msg.getSenderId() == currentUser.getId();
        String sender = isOwn ? "Tu" : (contactUser != null ? contactUser.getName() : "Usuario");
        String time   = msg.getTimestamp() != null ? msg.getTimestamp().format(timeFmt) : "--:--";

        VBox wrapper = new VBox(4);

        if (!attachments.isEmpty()) {
            for (Attachment att : attachments) {
                wrapper.getChildren().add(buildAttachmentNode(att, time, sender, isOwn, msg.getId(), wrapper));
            }
            if (!"[Adjuntos]".equals(msg.getContent())) {
                wrapper.getChildren().add(buildTextBubble(msg.getId(), time, sender, msg.getContent(), isOwn, wrapper));
            }
        } else {
            wrapper.getChildren().add(buildTextBubble(msg.getId(), time, sender, msg.getContent(), isOwn, wrapper));
        }

        messageNodes.put(msg.getId(), wrapper);
        if (messageContainer != null) messageContainer.getChildren().add(wrapper);
    }

    // =========================================================
    // UI builders
    // =========================================================

    private Node buildTextBubble(int msgId, String time, String sender,
                                  String content, boolean isOwn, VBox wrapper) {
        Label lbl = new Label("[" + time + "] " + sender + ": " + content);
        lbl.setWrapText(true);
        lbl.setMaxWidth(540);
        lbl.setPadding(new Insets(8, 12, 8, 12));
        lbl.setStyle(isOwn
                ? "-fx-background-color:#4A90D9;-fx-text-fill:white;-fx-background-radius:12;"
                : "-fx-background-color:#2D2D3A;-fx-text-fill:#E0E0E0;-fx-background-radius:12;");

        Button delBtn = makeDeleteButton(msgId, wrapper);

        HBox row = new HBox(6);
        if (isOwn) {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(delBtn, lbl);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(lbl, delBtn);
        }
        row.setPadding(new Insets(2, 4, 2, 4));
        return row;
    }

    private Node buildAttachmentNode(Attachment att, String time, String sender,
                                      boolean isOwn, int msgId, VBox wrapper) {
        String type = att.getFileType() != null ? att.getFileType() : "";
        if (type.startsWith("image/")) {
            return buildImageNode(att, time, sender, isOwn, msgId, wrapper);
        }
        return buildFileNode(att, time, sender, isOwn, msgId, wrapper);
    }

    private Node buildImageNode(Attachment att, String time, String sender,
                                 boolean isOwn, int msgId, VBox wrapper) {
        File f = new File(att.getFilePath());
        if (!f.exists()) return buildFileNode(att, time, sender, isOwn, msgId, wrapper);

        Image img;
        try {
            img = new Image(f.toURI().toString(), 260, 200, true, true, true);
        } catch (Exception e) {
            return buildFileNode(att, time, sender, isOwn, msgId, wrapper);
        }

        ImageView iv = new ImageView(img);
        iv.setFitWidth(260);
        iv.setPreserveRatio(true);
        iv.setOnMouseClicked(ev -> openWithDesktop(att.getFilePath()));
        iv.setStyle("-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,.3),6,0,0,2);");

        Label caption = new Label("[" + time + "] " + sender + ": 🖼 " + att.getFileName());
        caption.setStyle("-fx-text-fill:#9E9EB5;-fx-font-size:11;");

        Button delBtn = makeDeleteButton(msgId, wrapper);

        VBox inner = new VBox(4, iv, caption);

        HBox row = new HBox(6);
        if (isOwn) {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(delBtn, inner);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(inner, delBtn);
        }
        row.setPadding(new Insets(2, 4, 2, 4));
        return row;
    }

    private Node buildFileNode(Attachment att, String time, String sender,
                                boolean isOwn, int msgId, VBox wrapper) {
        String icon = fileIcon(att.getFileType());

        Label nameLbl = new Label(icon + " " + att.getFileName());
        nameLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;");
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(300);

        Label sizeLbl = new Label(fmtSize(att.getFileSize()));
        sizeLbl.setStyle("-fx-text-fill:#C0C0D0;-fx-font-size:11;");

        Button dlBtn = new Button("Descargar");
        dlBtn.setStyle("-fx-background-color:#4A90D9;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;");
        dlBtn.setOnAction(e -> downloadAttachment(att));

        Label header = new Label("[" + time + "] " + sender + ":");
        header.setStyle("-fx-text-fill:#9E9EB5;-fx-font-size:11;");

        Button delBtn = makeDeleteButton(msgId, wrapper);

        VBox inner = new VBox(4, header, nameLbl, sizeLbl, dlBtn);
        inner.setPadding(new Insets(10, 14, 10, 14));
        inner.setMaxWidth(360);
        inner.setStyle("-fx-background-color:#2D2D3A;-fx-background-radius:12;");

        HBox row = new HBox(6);
        if (isOwn) {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(delBtn, inner);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(inner, delBtn);
        }
        row.setPadding(new Insets(2, 4, 2, 4));
        return row;
    }

    private Button makeDeleteButton(int msgId, VBox wrapper) {
        Button btn = new Button("🗑");
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#FF6B6B;-fx-cursor:hand;-fx-font-size:13;");
        btn.setTooltip(new javafx.scene.control.Tooltip("Eliminar mensaje"));
        btn.setOnAction(e -> deleteMessageById(msgId, wrapper));
        return btn;
    }

    // =========================================================
    // Download with FileChooser
    // =========================================================

    private void downloadAttachment(Attachment att) {
        File src = new File(att.getFilePath());
        if (!src.exists()) { showError("Archivo no encontrado."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar archivo como...");
        fc.setInitialFileName(att.getFileName());
        String ext = ext(att.getFileName());
        if (!ext.isEmpty())
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(ext.toUpperCase(), "*." + ext));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));

        Stage s = resolveStage();
        File dest = fc.showSaveDialog(s);
        if (dest == null) return;

        TaskManager.getExecutor().submit(() -> {
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Platform.runLater(() ->
                    showInfo("Descarga completa", "Guardado en:\n" + dest.getAbsolutePath()));
            } catch (IOException e) {
                Platform.runLater(() -> showError("Error al guardar: " + e.getMessage()));
            }
        });
    }

    private void openWithDesktop(String path) {
        try { new java.io.File(path); java.awt.Desktop.getDesktop().open(new File(path)); }
        catch (Exception e) { showError("No se pudo abrir: " + e.getMessage()); }
    }

    // =========================================================
    // Attach file picker
    // =========================================================

    @FXML
    private void attachFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivos");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Todos", "*.*"),
            new FileChooser.ExtensionFilter("Imagenes", "*.jpg","*.jpeg","*.png","*.gif","*.webp"),
            new FileChooser.ExtensionFilter("PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Documentos", "*.doc","*.docx","*.txt"),
            new FileChooser.ExtensionFilter("Hojas de calculo", "*.xls","*.xlsx"),
            new FileChooser.ExtensionFilter("Multimedia", "*.mp3","*.mp4","*.avi","*.mov"),
            new FileChooser.ExtensionFilter("Comprimidos", "*.zip","*.rar")
        );
        List<File> files = fc.showOpenMultipleDialog(resolveStage());
        if (files != null && !files.isEmpty()) {
            selectedAttachments.addAll(files);
            StringBuilder sb = new StringBuilder("Adjuntos: ");
            files.forEach(f -> sb.append(f.getName()).append(", "));
            showInfo("Archivos seleccionados", sb.toString());
        }
    }

    @FXML private void clearAttachments() {
        selectedAttachments.clear();
        showInfo("Adjuntos", "Archivos adjuntos limpiados.");
    }

    // =========================================================
    // Navigation
    // =========================================================

    @FXML
    private void openContactInfo() {
        try {
            if (conversation == null) { showError("No hay conversacion."); return; }
            User c = userService.getOtherParticipant(conversation.getId(), currentUser.getId());
            if (c == null) { showError("No se encontro el contacto."); return; }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/ContactInfoView.fxml"));
            Parent root = loader.load();
            ((ContactInfoController) loader.getController()).setContact(c);
            Stage s = new Stage();
            s.setTitle("Info de Contacto");
            s.setScene(new Scene(root));
            s.show();
        } catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    @FXML private void openGroupInfo()  { showInfo("Info Grupo", "Implementacion futura."); }
    @FXML private void deleteMessage()  { showInfo("Eliminar", "Usa el boton en cada mensaje."); }

    @FXML
    private void returnToDashboard() {
        try {
            stopChatAutoRefresh();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/DashboardView.fxml"));
            Parent root = loader.load();
            Stage s = (Stage) messageContainer.getScene().getWindow();
            s.setScene(new Scene(root));
            s.setTitle("ChatConnect - Dashboard");
        } catch (IOException e) {
            showError("Error al volver: " + e.getMessage());
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    private void scrollToBottom() {
        if (messageScrollPane != null)
            Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
    }

    private Stage resolveStage() {
        if (stage != null) return stage;
        if (messageContainer != null && messageContainer.getScene() != null)
            return (Stage) messageContainer.getScene().getWindow();
        return null;
    }

    private String ext(String name) {
        int i = name.lastIndexOf('.'); return i > 0 ? name.substring(i+1).toLowerCase() : "";
    }

    private String fileIcon(String mime) {
        if (mime == null) return "📎";
        return switch (mime) {
            case "application/pdf"              -> "📄";
            case "application/msword"           -> "📝";
            case "application/vnd.ms-excel"     -> "📊";
            case "application/vnd.ms-powerpoint"-> "📊";
            case "application/zip"              -> "🗜";
            case "text/plain"                   -> "📃";
            case "audio/mpeg"                   -> "🎵";
            case "video/mp4"                    -> "🎬";
            default                             -> "📎";
        };
    }

    private String fmtSize(double b) {
        if (b < 1024)          return (int) b + " B";
        if (b < 1024*1024)     return String.format("%.1f KB", b/1024);
        return                        String.format("%.1f MB", b/(1024*1024));
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
