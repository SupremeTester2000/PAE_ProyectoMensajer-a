package Config;

public class SocketConfig {

    // Server configuration
    public static final int SOCKET_PORT = 9999;
    public static final String SOCKET_HOST = "localhost";
    public static final int SOCKET_TIMEOUT_SECONDS = 30;
    public static final int SOCKET_TIMEOUT_MILLIS = SOCKET_TIMEOUT_SECONDS * 1000;

    // Connection configuration
    public static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    public static final int HEARTBEAT_INTERVAL_MILLIS = HEARTBEAT_INTERVAL_SECONDS * 1000;

    // Reconnection strategy
    public static final int MAX_RECONNECTION_ATTEMPTS = 12;
    public static final int INITIAL_RECONNECTION_DELAY_SECONDS = 5;
    public static final int MAX_RECONNECTION_DELAY_SECONDS = 60;
    public static final double RECONNECTION_BACKOFF_MULTIPLIER = 1.5;

    // Message buffer configuration
    public static final int MESSAGE_QUEUE_CAPACITY = 1000;
    public static final int CHUNK_SIZE_BYTES = 65536;

    // Thread pool configuration
    public static final int SERVER_THREAD_POOL_SIZE = 20;
    public static final int CLIENT_THREAD_POOL_SIZE = 5;

    // Serialization configuration
    public static final String CHARSET = "UTF-8";
    public static final String MESSAGE_DELIMITER = "\n";

    // Logging configuration
    public static final boolean ENABLE_DEBUG_LOGGING = true;
    public static final boolean ENABLE_NETWORK_LOGGING = true;

    // Validation configuration
    public static final int MAX_MESSAGE_LENGTH = 10000;
    public static final int MAX_ATTACHMENT_COUNT = 10;
    public static final int MAX_CONCURRENT_CONNECTIONS = 100;

    // Feature flags
    public static final boolean ENABLE_COMPRESSION = false;
    public static final boolean ENABLE_ENCRYPTION = false;
    public static final boolean ENABLE_RATE_LIMITING = true;
    public static final int RATE_LIMIT_MESSAGES_PER_MINUTE = 100;

    private SocketConfig() {
        // Utility class, no instantiation
    }

    public static String getServerAddress() {
        return SOCKET_HOST + ":" + SOCKET_PORT;
    }

    public static long getHeartbeatIntervalMillis() {
        return HEARTBEAT_INTERVAL_MILLIS;
    }

    public static long getSocketTimeoutMillis() {
        return SOCKET_TIMEOUT_MILLIS;
    }

    public static int getInitialReconnectionDelayMillis() {
        return INITIAL_RECONNECTION_DELAY_SECONDS * 1000;
    }

    public static int getMaxReconnectionDelayMillis() {
        return MAX_RECONNECTION_DELAY_SECONDS * 1000;
    }
}
