package Network;

import Config.SocketConfig;
import Model.Message;
import Util.TaskManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SocketClientManager {

    private static SocketClientManager instance;
    private SocketClient socketClient;
    private boolean isConnected;
    private int currentUserId;
    private String currentUsername;
    private ReconnectionStrategy reconnectionStrategy;
    private List<Consumer<NetworkMessage>> messageListeners;
    private List<Consumer<String>> statusListeners;

    private SocketClientManager() {
        this.messageListeners = new CopyOnWriteArrayList<>();
        this.statusListeners = new CopyOnWriteArrayList<>();
        this.reconnectionStrategy = new ReconnectionStrategy();
        this.isConnected = false;
    }

    public static synchronized SocketClientManager getInstance() {
        if (instance == null) {
            instance = new SocketClientManager();
        }
        return instance;
    }

    public boolean connect(String host, int port, int userId, String username) {
        try {
            if (isConnected && socketClient != null) {
                disconnect();
            }

            this.currentUserId = userId;
            this.currentUsername = username;
            this.socketClient = new SocketClient(host, port);

            boolean connected = socketClient.connect();

            if (connected) {
                socketClient.onMessageReceived(this::handleMessageReceived);
                socketClient.onDisconnection(this::handleDisconnection);
                this.isConnected = true;
                this.reconnectionStrategy.reset();

                notifyStatusChange("Conectado como " + username);

                if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                    System.out.println("[SocketClientManager] Conectado exitosamente");
                }

                return true;
            } else {
                this.isConnected = false;
                notifyStatusChange("Fallo al conectar al servidor");
                initiateReconnection(host, port);
                return false;
            }

        } catch (Exception e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClientManager] Error en connect: " + e.getMessage());
            }
            this.isConnected = false;
            return false;
        }
    }

    public void broadcastMessage(Message message) {
        if (!isConnected || socketClient == null) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClientManager] No conectado. Mensaje no enviado.");
            }
            return;
        }

        try {
            NetworkMessage networkMsg = convertToNetworkMessage(message);
            socketClient.sendMessage(networkMsg);
        } catch (Exception e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClientManager] Error enviando mensaje: " + e.getMessage());
            }
        }
    }

    public void broadcastNetworkMessage(NetworkMessage message) {
        if (!isConnected || socketClient == null) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClientManager] No conectado. Mensaje de red no enviado.");
            }
            return;
        }

        try {
            socketClient.sendMessage(message);
        } catch (Exception e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClientManager] Error enviando mensaje de red: " + e.getMessage());
            }
        }
    }

    public void addMessageListener(Consumer<NetworkMessage> listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(Consumer<NetworkMessage> listener) {
        messageListeners.remove(listener);
    }

    public void addStatusListener(Consumer<String> listener) {
        statusListeners.add(listener);
    }

    public void removeStatusListener(Consumer<String> listener) {
        statusListeners.remove(listener);
    }

    public void disconnect() {
        try {
            this.isConnected = false;
            if (socketClient != null) {
                socketClient.disconnect();
            }
            notifyStatusChange("Desconectado");

            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[SocketClientManager] Desconectado");
            }
        } catch (Exception e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClientManager] Error desconectando: " + e.getMessage());
            }
        }
    }

    private void handleMessageReceived(NetworkMessage message) {
        if (SocketConfig.ENABLE_DEBUG_LOGGING) {
            System.out.println("[SocketClientManager] Mensaje recibido: " + message.getMessageId());
        }

        for (Consumer<NetworkMessage> listener : messageListeners) {
            try {
                listener.accept(message);
            } catch (Exception e) {
                if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                    System.err.println("[SocketClientManager] Error en listener: " + e.getMessage());
                }
            }
        }
    }

    private void handleDisconnection(String reason) {
        this.isConnected = false;
        notifyStatusChange("Desconectado: " + reason);

        if (SocketConfig.ENABLE_NETWORK_LOGGING) {
            System.out.println("[SocketClientManager] Desconexión detectada. Iniciando reconexión...");
        }

        // Attempt reconnection (sera implementado en siguiente fase con servidor)
    }

    private void initiateReconnection(String host, int port) {
        TaskManager.getExecutor().submit(() -> {
            for (int attempt = 1; attempt <= SocketConfig.MAX_RECONNECTION_ATTEMPTS; attempt++) {
                long delayMillis = reconnectionStrategy.getDelayMillis(attempt);

                if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                    System.out.println("[SocketClientManager] Intento " + attempt + " de " +
                            SocketConfig.MAX_RECONNECTION_ATTEMPTS + " en " + (delayMillis / 1000) + "s");
                }

                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (connect(host, port, currentUserId, currentUsername)) {
                    if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                        System.out.println("[SocketClientManager] Reconexión exitosa en intento " + attempt);
                    }
                    return;
                }
            }

            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[SocketClientManager] Fallo al reconectar después de " +
                        SocketConfig.MAX_RECONNECTION_ATTEMPTS + " intentos");
            }
            notifyStatusChange("No se pudo reconectar al servidor");
        });
    }

    public boolean isConnected() {
        return isConnected && socketClient != null && socketClient.isConnected();
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    private void notifyStatusChange(String status) {
        for (Consumer<String> listener : statusListeners) {
            try {
                listener.accept(status);
            } catch (Exception e) {
                if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                    System.err.println("[SocketClientManager] Error en status listener: " + e.getMessage());
                }
            }
        }
    }

    private NetworkMessage convertToNetworkMessage(Message message) {
        NetworkMessage networkMsg = new NetworkMessage();
        networkMsg.setMessageId(message.getId());
        networkMsg.setConversationId(message.getConversationId());
        networkMsg.setSenderId(message.getSenderId());
        networkMsg.setSenderName(currentUsername);
        networkMsg.setContent(message.getContent());
        if (message.getTimestamp() != null) {
            networkMsg.setTimestamp(message.getTimestamp().toString());
        }
        networkMsg.setStatus(message.getStatus() != null ? message.getStatus() : "SENT");
        networkMsg.setMessageType("TEXT");
        return networkMsg;
    }
}
