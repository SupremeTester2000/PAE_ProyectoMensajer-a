package DAO;

import Config.DatabaseConfig;
import Model.Conversation;
import Model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConversationDAO {
    
    private DatabaseConfig dbConfig;
    
    public ConversationDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    public Conversation findById(int id) throws SQLException {
        String sql = "SELECT id, type, group_name, creation_date FROM conversations WHERE id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToConversation(rs);
                }
            }
        }
        
        return null;
    }

    public List<Conversation> findByUser(int userId) throws SQLException {
        List<Conversation> conversations = new ArrayList<>();
        
        String sql = "SELECT c.id, c.type, c.group_name, c.creation_date " +
                     "FROM conversations c " +
                     "INNER JOIN participants_conversation pc ON c.id = pc.conversation_id " +
                     "WHERE pc.user_id = ? " +
                     "ORDER BY c.creation_date DESC";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    conversations.add(mapResultSetToConversation(rs));
                }
            }
        }
        
        return conversations;
    }
    
    public List<Conversation> searchByName(int userId, String searchTerm) throws SQLException {
        List<Conversation> conversations = new ArrayList<>();
        
        String sql = "SELECT c.id, c.type, c.group_name, c.creation_date " +
                     "FROM conversations c " +
                     "INNER JOIN participants_conversation pc ON c.id = pc.conversation_id " +
                     "WHERE pc.user_id = ? AND (c.group_name ILIKE ? OR c.type ILIKE ?) " +
                     "ORDER BY c.creation_date DESC";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String pattern = "%" + searchTerm + "%";
            pstmt.setInt(1, userId);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    conversations.add(mapResultSetToConversation(rs));
                }
            }
        }
        
        return conversations;
    }

    public boolean save(Conversation conversation) throws SQLException {
        String sql = "INSERT INTO conversations (type, group_name, creation_date) " +
                     "VALUES (?, ?, ?)";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, conversation.getType());
            pstmt.setString(2, conversation.getGroupName());
            pstmt.setTimestamp(3, Timestamp.valueOf(
                conversation.getCreationDate() != null ? 
                conversation.getCreationDate() : 
                LocalDateTime.now()
            ));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        conversation.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    public Conversation findPrivateConversationBetween(
            int firstUserId,
            int secondUserId) throws SQLException {

        String sql = "SELECT c.id, c.type, c.group_name, c.creation_date "
                + "FROM conversations c "
                + "INNER JOIN participants_conversation pc1 ON c.id = pc1.conversation_id "
                + "INNER JOIN participants_conversation pc2 ON c.id = pc2.conversation_id "
                + "WHERE c.type = 'PRIVATE' "
                + "AND pc1.user_id = ? "
                + "AND pc2.user_id = ? "
                + "LIMIT 1";

        try (Connection conn = dbConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, firstUserId);
            pstmt.setInt(2, secondUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToConversation(rs);
                }
            }
        }

        return null;
    }

    public Conversation saveWithParticipants(
            Conversation conversation,
            List<Integer> participantIds) throws SQLException {

        String conversationSql = "INSERT INTO conversations (type, group_name, creation_date) "
                + "VALUES (?, ?, ?)";
        String participantSql = "INSERT INTO participants_conversation "
                + "(user_id, conversation_id, role) VALUES (?, ?, ?)";

        Connection conn = dbConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement conversationStmt = conn.prepareStatement(
                    conversationSql,
                    Statement.RETURN_GENERATED_KEYS)) {

                conversationStmt.setString(1, conversation.getType());
                conversationStmt.setString(2, conversation.getGroupName());
                conversationStmt.setTimestamp(3, Timestamp.valueOf(
                        conversation.getCreationDate() != null
                        ? conversation.getCreationDate()
                        : LocalDateTime.now()));

                int affectedRows = conversationStmt.executeUpdate();

                if (affectedRows == 0) {
                    conn.rollback();
                    return null;
                }

                try (ResultSet generatedKeys = conversationStmt.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        conn.rollback();
                        return null;
                    }
                    conversation.setId(generatedKeys.getInt(1));
                }
            }

            try (PreparedStatement participantStmt = conn.prepareStatement(participantSql)) {
                for (Integer participantId : participantIds) {
                    participantStmt.setInt(1, participantId);
                    participantStmt.setInt(2, conversation.getId());
                    participantStmt.setString(3, "MEMBER");
                    participantStmt.addBatch();
                }
                participantStmt.executeBatch();
            }

            conn.commit();
            return conversation;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public boolean addParticipants(
            int conversationId,
            List<Integer> participantIds) throws SQLException {

        String sql = "INSERT INTO participants_conversation "
                + "(user_id, conversation_id, role) VALUES (?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Integer participantId : participantIds) {
                pstmt.setInt(1, participantId);
                pstmt.setInt(2, conversationId);
                pstmt.setString(3, "MEMBER");
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            return results.length > 0;
        }
    }

    public List<User> findParticipantsByConversation(int conversationId) throws SQLException {
        List<User> participants = new ArrayList<>();

        String sql = "SELECT u.id, u.name, u.email, u.password, "
                + "u.profile_photo, u.status, u.connected "
                + "FROM users u "
                + "INNER JOIN participants_conversation pc ON u.id = pc.user_id "
                + "WHERE pc.conversation_id = ? "
                + "ORDER BY u.name ASC";

        try (Connection conn = dbConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, conversationId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participants.add(mapResultSetToUser(rs));
                }
            }
        }

        return participants;
    }

    public boolean update(Conversation conversation) throws SQLException {
        String sql = "UPDATE conversations SET type = ?, group_name = ? WHERE id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, conversation.getType());
            pstmt.setString(2, conversation.getGroupName());
            pstmt.setInt(3, conversation.getId());
            
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM conversations WHERE id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public String getConversationDisplayName(int conversationId, int currentUserId) throws SQLException {
        String sql = "SELECT type, group_name FROM conversations WHERE id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, conversationId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("type");
                    
                    if ("GROUP".equals(type)) {
                        return rs.getString("group_name");
                    } else if ("PRIVATE".equals(type)) {
                        return getOtherUserNameInPrivateConversation(conversationId, currentUserId);
                    }
                }
            }
        }
        
        return "Conversación";
    }

    private String getOtherUserNameInPrivateConversation(int conversationId, int currentUserId) throws SQLException {
        String sql = "SELECT u.name FROM users u " +
                     "INNER JOIN participants_conversation pc ON u.id = pc.user_id " +
                     "WHERE pc.conversation_id = ? AND u.id != ? " +
                     "LIMIT 1";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, conversationId);
            pstmt.setInt(2, currentUserId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        
        return "Usuario Desconocido";
    }

    private Conversation mapResultSetToConversation(ResultSet rs) throws SQLException {
        Conversation conversation = new Conversation();
        conversation.setId(rs.getInt("id"));
        conversation.setType(rs.getString("type"));
        conversation.setGroupName(rs.getString("group_name"));
        
        Timestamp timestamp = rs.getTimestamp("creation_date");
        if (timestamp != null) {
            conversation.setCreationDate(timestamp.toLocalDateTime());
        }
        
        return conversation;
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