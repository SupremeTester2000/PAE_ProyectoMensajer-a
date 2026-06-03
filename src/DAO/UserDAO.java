package DAO;

import Config.DatabaseConfig;
import Model.User;
import Util.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    
    public User findById(int id) throws SQLException {
        String sql = "SELECT id, name, email, password, profile_photo, status, connected FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, name, email, password, profile_photo, status, connected FROM users WHERE email = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public User create(User user) throws SQLException {
        // Hash de la contraseña antes de guardar
        String hashedPassword = PasswordUtils.hashPassword(user.getPassword());

        String sql = """
           INSERT INTO users
           (
            name,
            email,
            password,
            profile_photo,
            status,
            connected
            )
            VALUES (?, ?, ?, ?, ?, ?)
    """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4,
                    user.getProfilePhoto() != null
                    ? user.getProfilePhoto()
                    : null);

            pstmt.setString(5,
                    user.getStatus() != null
                    ? user.getStatus()
                    : "Disponible");

            pstmt.setBoolean(6, false);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        user.setId(generatedId);
                        // Retornar el usuario sin la contraseña en texto plano (solo el hash)
                        user.setPassword(hashedPassword);
                        return user;
                    }
                }
            }
        }
        return null;
    }

    public List<User> findAll() {
        return null;
    }

    public List<User> findAllExcept(int excludedUserId) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, password, profile_photo, status, connected "
                + "FROM users "
                + "WHERE id != ? "
                + "ORDER BY name ASC";

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, excludedUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }

        return users;
    }

    public List<User> searchByKeyword(String keyword, int excludedUserId) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, password, profile_photo, status, connected "
                + "FROM users "
                + "WHERE id != ? AND (name ILIKE ? OR email ILIKE ?) "
                + "ORDER BY name ASC";

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            pstmt.setInt(1, excludedUserId);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }

        return users;
    }

    public User getOtherParticipant(
            int conversationId,
            int currentUserId) throws SQLException {

        String sql = """
        SELECT u.id,
               u.name,
               u.email,
               u.password,
               u.profile_photo,
               u.status,
               u.connected
        FROM users u
        INNER JOIN participants_conversation pc
            ON u.id = pc.user_id
        WHERE pc.conversation_id = ?
          AND u.id <> ?
        LIMIT 1
    """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, conversationId);
            pstmt.setInt(2, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }

        return null;
    }

    public boolean update(User user) {
        return false;
    }

    public boolean delete(int id) {
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setProfilePhoto(rs.getString("profile_photo"));
        user.setStatus(rs.getString("status"));
        user.setConnected(rs.getBoolean("connected"));
        return user;
    }
}
