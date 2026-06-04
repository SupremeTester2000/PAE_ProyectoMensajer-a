package Network;

import Config.SocketConfig;
import Util.TaskManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerSocketManager {

    private static ServerSocketManager instance;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private ConcurrentHashMap<Integer, ClientHandler> clientHandlers;
    private MessageDispatcher messageDispatcher;
    private AtomicInteger clientIdCounter;

    private ServerSocketManager() {
        this.clientHandlers = new ConcurrentHashMap<>();
        this.messageDispatcher = new MessageDispatcher(this);
        this.clientIdCounter = new AtomicInteger(0);
        this.isRunning = false;
    }

    public static synchronized ServerSocketManager getInstance() {
        if (instance == null) {
            instance = new ServerSocketManager();
        }
        return instance;
    }

    public boolean start(int port) {
        try {
            if (isRunning) {
                if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                    System.out.println("[ServerSocketManager] Servidor ya está en ejecución");
                }
                return false;
            }

            serverSocket = new ServerSocket(port);
            System.out.println("Servidor iniciado en puerto " + port);
            serverSocket.setReuseAddress(true);
            isRunning = true;

            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[ServerSocketManager] Servidor iniciado en puerto " + port);
            }

            startAcceptLoop();
            return true;

        } catch (IOException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[ServerSocketManager] Error al iniciar servidor: " + e.getMessage());
            }
            isRunning = false;
            return false;
        }
    }

    private void startAcceptLoop() {
        TaskManager.getExecutor().submit(this::acceptLoop);
    }

    private void acceptLoop() {
        try {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setKeepAlive(true);
                    clientSocket.setTcpNoDelay(true);

                    int clientId = clientIdCounter.incrementAndGet();

                    if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                        System.out.println("[ServerSocketManager] Nueva conexión: " + 
                                clientSocket.getInetAddress().getHostAddress() + 
                                " (Cliente ID: " + clientId + ")");
                    }

                    ClientHandler handler = new ClientHandler(clientId, clientSocket, messageDispatcher);
                    clientHandlers.put(handler.getUserId(), handler);

                    TaskManager.getExecutor().submit(handler);

                } catch (IOException e) {
                    if (isRunning) {
                        if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                            System.err.println("[ServerSocketManager] Error aceptando conexión: " + e.getMessage());
                        }
                    }
                }
            }
        } finally {
            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[ServerSocketManager] Accept loop terminado");
            }
        }
    }

    public void registerClientHandler(int userId, ClientHandler handler) {
        clientHandlers.put(userId, handler);
        if (SocketConfig.ENABLE_NETWORK_LOGGING) {
            System.out.println("[ServerSocketManager] Usuario " + userId + " registrado");
        }
    }

    public void unregisterClientHandler(int userId) {
        ClientHandler removed = clientHandlers.remove(userId);
        if (removed != null) {
            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[ServerSocketManager] Usuario " + userId + " desregistrado");
            }
        }
    }

    public ClientHandler getClientHandler(int userId) {
        return clientHandlers.get(userId);
    }

    public void broadcastToUser(int userId, NetworkMessage message) {
        ClientHandler handler = clientHandlers.get(userId);
        if (handler != null && handler.isConnected()) {
            handler.sendMessage(message);
        }
    }

    public ConcurrentHashMap<Integer, ClientHandler> getClientHandlers() {
        return clientHandlers;
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        try {
            isRunning = false;

            for (ClientHandler handler : clientHandlers.values()) {
                try {
                    handler.disconnect();
                } catch (Exception e) {
                    if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                        System.err.println("[ServerSocketManager] Error desconectando cliente: " + e.getMessage());
                    }
                }
            }

            clientHandlers.clear();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[ServerSocketManager] Servidor detenido");
            }

        } catch (IOException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[ServerSocketManager] Error deteniendo servidor: " + e.getMessage());
            }
        }
    }

    public int getConnectedClientsCount() {
        return clientHandlers.size();
    }

    public void startServer() {
        start(SocketConfig.SOCKET_PORT);
    }

    public void stopServer() {
        stop();
    }
}
