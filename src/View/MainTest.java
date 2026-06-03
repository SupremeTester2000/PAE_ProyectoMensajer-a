package View;

import Config.DatabaseConfig;
import java.sql.Connection;

public class MainTest {
 
        public static void main(String[] args) {

        // Obtener la primera instancia
        DatabaseConfig db1 = DatabaseConfig.getInstance();

        // Obtener la segunda instancia
        DatabaseConfig db2 = DatabaseConfig.getInstance();

        // Verificar Singleton
        System.out.println("¿Es la misma instancia?: " + (db1 == db2));

        // Obtener la conexión
        Connection conn = db1.getConnection();

        if (conn != null) {
            System.out.println("Conexión obtenida correctamente.");
        } else {
            System.out.println("No se pudo obtener la conexión.");
        }

        // Cerrar conexión
        db1.closeConnection();
    }

    
}
