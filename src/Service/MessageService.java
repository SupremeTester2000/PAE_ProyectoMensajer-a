package Service;

import DAO.MessageDAO;
import Model.Message;
import Util.ValidationUtils;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class MessageService {
    
    private MessageDAO messageDAO;
    
    public MessageService() {
        this.messageDAO = new MessageDAO();
    }

    public boolean sendMessage(Message message) {
        try {
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("El contenido del mensaje no puede estar vacío.");
            }
            
            if (message.getConversationId() <= 0 || message.getSenderId() <= 0) {
                throw new IllegalArgumentException("ID de conversación y remitente requeridos.");
            }
            
            if (message.getTimestamp() == null) {
                message.setTimestamp(LocalDateTime.now());
            }
            if (message.getStatus() == null) {
                message.setStatus("SENT");
            }
            
            return messageDAO.save(message);
        } catch (SQLException e) {
            throw new RuntimeException("Error al enviar mensaje: " + e.getMessage(), e);
        }
    }
    
    public List<Message> getMessages(int conversationId) {
        try {
            return messageDAO.findByConversation(conversationId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener mensajes: " + e.getMessage(), e);
        }
    }

    public Message getMessage(int messageId) {
        try {
            return messageDAO.findById(messageId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener mensaje: " + e.getMessage(), e);
        }
    }

    public boolean updateMessageStatus(int messageId, String status) {
        try {
            if (status == null || status.trim().isEmpty()) {
                throw new IllegalArgumentException("El estado del mensaje no puede estar vacío.");
            }
            return messageDAO.updateStatus(messageId, status);
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado del mensaje: " + e.getMessage(), e);
        }
    }
    
    public boolean deleteMessage(int messageId) {
        try {
            return messageDAO.delete(messageId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar mensaje: " + e.getMessage(), e);
        }
    }
}
