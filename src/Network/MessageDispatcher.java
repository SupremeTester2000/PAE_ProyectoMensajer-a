package Network;

import Config.SocketConfig;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDispatcher {

    private ServerSocketManager serverSocketManager;
    private ConcurrentHashMap<Integer, ClientHandler> clientHandlers;

    public MessageDispatcher(ServerSocketManager serverSocketManager) {
        this.serverSocketManager = serverSocketManager;
        this.clientHandlers = new ConcurrentHashMap<>();
    }

    public void registerClientHandler(int userId, ClientHandler handler) {
        clientHandlers.put(userId, handler);
        if (SocketConfig.ENABLE_NETWORK_LOGGING) {
            System.out.println("[MessageDispatcher] Usuario " + userId + " registrado");
        }
    }

    public void unregisterClientHandler(int userId) {
        ClientHandler removed = clientHandlers.remove(userId);
        if (removed != null) {
            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[MessageDispatcher] Usuario " + userId + " desregistrado");
            }
        }
    }

    public void distributeMessage(NetworkMessage message, ClientHandler fromHandler) {
        if (message == null) {
            return;
        }

        try {
            int conversationId = message.getConversationId();
            int senderId = message.getSenderId();

            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.out.println("[MessageDispatcher] Distribuyendo mensaje " + message.getMessageId() + 
                        " de usuario " + senderId + " en conversación " + conversationId);
            }

            broadcastToConversationParticipants(conversationId, message, senderId);

        } catch (Exception e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[MessageDispatcher] Error distribuyendo mensaje: " + e.getMessage());
            }
        }
    }

    private void broadcastToConversationParticipants(int conversationId, NetworkMessage message, int senderId) {
        for (ClientHandler handler : clientHandlers.values()) {
            if (handler != null && handler.isConnected()) {
                try {
                    handler.sendMessage(message);
                    
                    if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                        System.out.println("[MessageDispatcher] Mensaje " + message.getMessageId() + 
                                " enviado a usuario " + handler.getUserId());
                    }

                } catch (Exception e) {
                    if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                        System.err.println("[MessageDispatcher] Error enviando a usuario " + 
                                handler.getUserId() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    public void broadcastMessage(NetworkMessage message) {
        for (ClientHandler handler : clientHandlers.values()) {
            if (handler != null && handler.isConnected()) {
                try {
                    handler.sendMessage(message);
                } catch (Exception e) {
                    if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                        System.err.println("[MessageDispatcher] Error en broadcast: " + e.getMessage());
                    }
                }
            }
        }
    }

    public void sendMessageToUser(int userId, NetworkMessage message) {
        ClientHandler handler = clientHandlers.get(userId);
        if (handler != null && handler.isConnected()) {
            try {
                handler.sendMessage(message);
            } catch (Exception e) {
                if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                    System.err.println("[MessageDispatcher] Error enviando a usuario " + userId + ": " + e.getMessage());
                }
            }
        }
    }

    public boolean isUserOnline(int userId) {
        ClientHandler handler = clientHandlers.get(userId);
        return handler != null && handler.isConnected();
    }

    public int getOnlineUsersCount() {
        int count = 0;
        for (ClientHandler handler : clientHandlers.values()) {
            if (handler != null && handler.isConnected()) {
                count++;
            }
        }
        return count;
    }

    public ConcurrentHashMap<Integer, ClientHandler> getClientHandlers() {
        return clientHandlers;
    }

    public ServerSocketManager getServerSocketManager() {
        return serverSocketManager;
    }

    public void dispatch(Object message) {
        // Legacy compatibility
    }
}
