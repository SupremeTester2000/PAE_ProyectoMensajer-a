package DAO;

import Config.DatabaseConfig;
import Model.Attachment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AttachmentDAO {

    public Attachment create(Attachment attachment) throws SQLException {
        String sql = """
            INSERT INTO attachments (message_id, file_name, file_type, file_path, file_size)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, attachment.getMessageId());
            pstmt.setString(2, attachment.getFileName());
            pstmt.setString(3, attachment.getFileType());
            pstmt.setString(4, attachment.getFilePath());
            pstmt.setDouble(5, attachment.getFileSize());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        attachment.setId(generatedKeys.getInt(1));
                        return attachment;
                    }
                }
            }
        }
        return null;
    }

    public boolean save(Attachment attachment) {
        try {
            return create(attachment) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    public Attachment findById(int id) throws SQLException {
        String sql = """
            SELECT id, message_id, file_name, file_type, file_path, file_size
            FROM attachments
            WHERE id = ?
        """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAttachment(rs);
                }
            }
        }
        return null;
    }

    public List<Attachment> findByMessage(int messageId) {
        List<Attachment> attachments = new ArrayList<>();
        try {
            return findByMessageId(messageId);
        } catch (SQLException e) {
            return attachments;
        }
    }

    public List<Attachment> findByMessageId(int messageId) throws SQLException {
        List<Attachment> attachments = new ArrayList<>();
        String sql = """
            SELECT id, message_id, file_name, file_type, file_path, file_size
            FROM attachments
            WHERE message_id = ?
            ORDER BY id ASC
        """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, messageId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    attachments.add(mapResultSetToAttachment(rs));
                }
            }
        }
        return attachments;
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean deleteByMessageId(int messageId) throws SQLException {
        String sql = "DELETE FROM attachments WHERE message_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, messageId);
            pstmt.executeUpdate();
            return true;
        }
    }

    private Attachment mapResultSetToAttachment(ResultSet rs) throws SQLException {
        Attachment attachment = new Attachment();
        attachment.setId(rs.getInt("id"));
        attachment.setMessageId(rs.getInt("message_id"));
        attachment.setFileName(rs.getString("file_name"));
        attachment.setFileType(rs.getString("file_type"));
        attachment.setFilePath(rs.getString("file_path"));
        attachment.setFileSize(rs.getDouble("file_size"));
        return attachment;
    }
}