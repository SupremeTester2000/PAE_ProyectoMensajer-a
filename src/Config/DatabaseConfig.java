package Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String url = "jdbc:postgresql://localhost:5432/chatconnect";
    private static final String user = "turkey";
    private static final String password = "kippycit0!";

    private static DatabaseConfig instance;
    private Connection connection;

    private DatabaseConfig() {
        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión a PostgreSQL establecida.");
        } catch (SQLException e) {
            System.err.println("Error al conectar con PostgreSQL.");
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() {

        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexión cerrada.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}