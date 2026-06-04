package Service;

import DAO.MessageDAO;
import Model.Message;
import Network.NetworkMessage;
import Network.SocketClientManager;
import Util.ValidationUtils;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class MessageService {
    
    private MessageDAO messageDAO;
    private SocketClientManager socketClientManager;
    
    public MessageService() {
        this.messageDAO = new MessageDAO();
        this.socketClientManager = SocketClientManager.getInstance();
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
            
            // 1. Persistir en PostgreSQL
            boolean saved = messageDAO.save(message);
            
            if (saved && message.getId() > 0) {
                // 2. Si la persistencia fue exitosa, enviar por socket
                try {
                    broadcastMessageToNetwork(message);
                } catch (Exception e) {
                    // Log pero no fallar si el socket falla
                    System.err.println("[MessageService] Advertencia: Error enviando por socket: " + e.getMessage());
                }
            }
            
            return saved;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error al enviar mensaje: " + e.getMessage(), e);
        }
    }

    private void broadcastMessageToNetwork(Message message) {
        // Verificar si el socket está conectado antes de enviar
        if (socketClientManager != null && socketClientManager.isConnected()) {
            try {
                NetworkMessage networkMessage = convertToNetworkMessage(message);
                socketClientManager.broadcastNetworkMessage(networkMessage);
            } catch (Exception e) {
                // No lanzar excepción, solo loguear
                System.err.println("[MessageService] Error en broadcastMessageToNetwork: " + e.getMessage());
            }
        }
    }

    private NetworkMessage convertToNetworkMessage(Message message) {
        NetworkMessage networkMsg = new NetworkMessage();
        networkMsg.setMessageId(message.getId());
        networkMsg.setConversationId(message.getConversationId());
        networkMsg.setSenderId(message.getSenderId());
        networkMsg.setContent(message.getContent());
        
        if (message.getTimestamp() != null) {
            networkMsg.setTimestamp(message.getTimestamp().toString());
        }
        
        networkMsg.setStatus(message.getStatus() != null ? message.getStatus() : "SENT");
        networkMsg.setMessageType("TEXT");
        
        return networkMsg;
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
