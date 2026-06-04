package Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String url = "jdbc:postgresql://localhost:5433/chatconnect";
    private static final String user = "turkey";
    private static final String password = "kippycit0!";

    private static DatabaseConfig instance;

    private DatabaseConfig() {}

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Cada llamada retorna una conexion nueva e independiente.
     * Los DAOs deben cerrarla en un try-with-resources.
     * Esto evita conflictos entre hilos que compartian una sola conexion.
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.err.println("[DatabaseConfig] Error al conectar: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /** Conservado por compatibilidad. */
    public void closeConnection() {}
}