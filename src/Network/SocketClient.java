package Network;

import Config.SocketConfig;
import Util.TaskManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;

public class SocketClient {

    private Socket socket;
    private String serverHost;
    private int serverPort;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected;
    private Consumer<NetworkMessage> messageCallback;
    private Consumer<String> disconnectionCallback;

    public SocketClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.isConnected = false;
    }

    public boolean connect() {
        try {
            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[SocketClient] Conectando a " + serverHost + ":" + serverPort);
            }

            socket = new Socket(serverHost, serverPort);
            socket.setSoTimeout(SocketConfig.SOCKET_TIMEOUT_MILLIS);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), SocketConfig.CHARSET));

            isConnected = true;

            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[SocketClient] Conectado exitosamente");
            }

            startReadLoop();
            return true;

        } catch (IOException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClient] Error al conectar: " + e.getMessage());
            }
            isConnected = false;
            return false;
        }
    }

    private void startReadLoop() {
        TaskManager.getExecutor().submit(this::readLoop);
    }

    private void readLoop() {
        try {
            String line;
            while (isConnected && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    NetworkMessage message = deserializeMessage(line);
                    if (message != null && messageCallback != null) {
                        messageCallback.accept(message);
                    }
                } catch (Exception e) {
                    if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                        System.err.println("[SocketClient] Error deserializando mensaje: " + e.getMessage());
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.out.println("[SocketClient] Socket timeout: " + e.getMessage());
            }
        } catch (IOException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.out.println("[SocketClient] Conexión cerrada: " + e.getMessage());
            }
        } finally {
            disconnect();
            if (disconnectionCallback != null) {
                disconnectionCallback.accept("Desconectado del servidor");
            }
        }
    }

    public void sendMessage(NetworkMessage message) {
        if (!isConnected || out == null) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClient] No conectado. No se puede enviar mensaje.");
            }
            return;
        }

        try {
            String serialized = serializeMessage(message);
            out.println(serialized);
            out.flush();

            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.out.println("[SocketClient] Mensaje enviado: " + message.getMessageId());
            }
        } catch (Exception e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClient] Error enviando mensaje: " + e.getMessage());
            }
        }
    }

    public void onMessageReceived(Consumer<NetworkMessage> callback) {
        this.messageCallback = callback;
    }

    public void onDisconnection(Consumer<String> callback) {
        this.disconnectionCallback = callback;
    }

    public void disconnect() {
        try {
            isConnected = false;

            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[SocketClient] Desconectado");
            }
        } catch (IOException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClient] Error desconectando: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    private String serializeMessage(NetworkMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.getMessageId()).append("|");
        sb.append(message.getConversationId()).append("|");
        sb.append(message.getSenderId()).append("|");
        sb.append(message.getSenderName() != null ? message.getSenderName() : "").append("|");
        sb.append(message.getContent() != null ? message.getContent() : "").append("|");
        sb.append(message.getTimestamp()).append("|");
        sb.append(message.getStatus()).append("|");
        sb.append(message.getMessageType()).append("|");
        sb.append(message.getSyncToken() != null ? message.getSyncToken() : "");

        return sb.toString();
    }

    private NetworkMessage deserializeMessage(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 9) {
                return null;
            }

            NetworkMessage message = new NetworkMessage();
            message.setMessageId(Integer.parseInt(parts[0]));
            message.setConversationId(Integer.parseInt(parts[1]));
            message.setSenderId(Integer.parseInt(parts[2]));
            message.setSenderName(parts[3].isEmpty() ? null : parts[3]);
            message.setContent(parts[4].isEmpty() ? null : parts[4]);
            message.setTimestamp(parts[5]);
            message.setStatus(parts[6]);
            message.setMessageType(parts[7]);
            message.setSyncToken(parts[8].isEmpty() ? null : parts[8]);

            return message;
        } catch (NumberFormatException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[SocketClient] Error parsing message: " + e.getMessage());
            }
            return null;
        }
    }
}
