package Network;

import Config.SocketConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {

    private int clientHandlerId;
    private Socket socket;
    private int userId;
    private String username;
    private MessageDispatcher messageDispatcher;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected;

    public ClientHandler(int clientHandlerId, Socket socket, MessageDispatcher messageDispatcher) {
        this.clientHandlerId = clientHandlerId;
        this.socket = socket;
        this.messageDispatcher = messageDispatcher;
        this.userId = -1;
        this.isConnected = false;
    }

    @Override
    public void run() {
        try {
            initializeStreams();

            if (!isConnected) {
                if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                    System.err.println("[ClientHandler] Fallo inicializando streams");
                }
                disconnect();
                return;
            }

            messageLoop();

        } catch (Exception e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[ClientHandler " + clientHandlerId + "] Error: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void initializeStreams() {
        try {
            socket.setSoTimeout(SocketConfig.SOCKET_TIMEOUT_MILLIS);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), SocketConfig.CHARSET));

            isConnected = true;

            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                System.out.println("[ClientHandler " + clientHandlerId + "] Streams inicializados");
            }

        } catch (IOException e) {
            isConnected = false;
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[ClientHandler] Error inicializando streams: " + e.getMessage());
            }
        }
    }

    private void messageLoop() {
        try {
            String line;
            while (isConnected && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    NetworkMessage message = deserializeMessage(line);

                    if (message != null) {
                        if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                            System.out.println("[ClientHandler " + clientHandlerId + "] Mensaje recibido: " + 
                                    message.getMessageId() + " (conv: " + message.getConversationId() + ")");
                        }

                        if (userId == -1) {
                            userId = message.getSenderId();
                            username = message.getSenderName();
                            messageDispatcher.registerClientHandler(userId, this);

                            if (SocketConfig.ENABLE_NETWORK_LOGGING) {
                                System.out.println("[ClientHandler " + clientHandlerId + "] Usuario autenticado: " + 
                                        username + " (ID: " + userId + ")");
                            }
                        }

                        messageDispatcher.distributeMessage(message, this);
                    }

                } catch (Exception e) {
                    if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                        System.err.println("[ClientHandler] Error procesando mensaje: " + e.getMessage());
                    }
                }
            }

        } catch (SocketException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.out.println("[ClientHandler " + clientHandlerId + "] Socket cerrado: " + e.getMessage());
            }
        } catch (IOException e) {
            if (isConnected) {
                if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                    System.err.println("[ClientHandler " + clientHandlerId + "] Error leyendo: " + e.getMessage());
                }
            }
        }
    }

    public void sendMessage(NetworkMessage message) {
        if (!isConnected || out == null) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[ClientHandler " + clientHandlerId + "] No conectado. No se puede enviar.");
            }
            return;
        }

        try {
            String serialized = serializeMessage(message);
            out.println(serialized);
            out.flush();

            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.out.println("[ClientHandler " + clientHandlerId + "] Mensaje enviado: " + message.getMessageId());
            }

        } catch (Exception e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[ClientHandler " + clientHandlerId + "] Error enviando: " + e.getMessage());
            }
            disconnect();
        }
    }

    public void disconnect() {
        try {
            isConnected = false;

            if (userId != -1) {
                messageDispatcher.unregisterClientHandler(userId);
            }

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
                System.out.println("[ClientHandler " + clientHandlerId + "] Desconectado");
            }

        } catch (IOException e) {
            if (SocketConfig.ENABLE_DEBUG_LOGGING) {
                System.err.println("[ClientHandler " + clientHandlerId + "] Error desconectando: " + e.getMessage());
            }
        }
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public int getClientHandlerId() {
        return clientHandlerId;
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
                System.err.println("[ClientHandler] Error parsing message: " + e.getMessage());
            }
            return null;
        }
    }
}
