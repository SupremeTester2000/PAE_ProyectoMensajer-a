package DAO;

import Config.DatabaseConfig;
import Model.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    
    private DatabaseConfig dbConfig;
    
    public MessageDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    public boolean save(Message message) throws SQLException {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content, status, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, message.getConversationId());
            pstmt.setInt(2, message.getSenderId());
            pstmt.setString(3, message.getContent());
            pstmt.setString(4, message.getStatus() != null ? message.getStatus() : "SENT");
            pstmt.setTimestamp(5, Timestamp.valueOf(
                message.getTimestamp() != null ? 
                message.getTimestamp() : 
                LocalDateTime.now()
            ));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        message.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    public List<Message> findByConversation(int conversationId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        
        String sql = "SELECT id, conversation_id, sender_id, content, status, timestamp " +
                     "FROM messages " +
                     "WHERE conversation_id = ? " +
                     "ORDER BY timestamp ASC";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, conversationId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        }
        
        return messages;
    }

    public Message findById(int messageId) throws SQLException {
        String sql = "SELECT id, conversation_id, sender_id, content, status, timestamp " +
                     "FROM messages WHERE id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, messageId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessage(rs);
                }
            }
        }
        
        return null;
    }

    public boolean updateStatus(int messageId, String status) throws SQLException {
        String sql = "UPDATE messages SET status = ? WHERE id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, messageId);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM messages WHERE id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getInt("id"));
        message.setConversationId(rs.getInt("conversation_id"));
        message.setSenderId(rs.getInt("sender_id"));
        message.setContent(rs.getString("content"));
        message.setStatus(rs.getString("status"));
        
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            message.setTimestamp(timestamp.toLocalDateTime());
        }
        
        return message;
    }
}
