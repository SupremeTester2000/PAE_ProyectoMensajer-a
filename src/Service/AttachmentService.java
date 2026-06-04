    package Service;

    import DAO.AttachmentDAO;
    import Model.Attachment;
    import Util.TaskManager;
    import java.io.File;
    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.sql.SQLException;
    import java.util.List;
    import java.util.UUID;
    import java.util.function.Consumer;

    public class AttachmentService {

        private static final String ATTACHMENTS_FOLDER = "attachments";
        private final AttachmentDAO attachmentDAO;

        public AttachmentService() {
            this.attachmentDAO = new AttachmentDAO();
            initializeAttachmentsFolder();
        }

        private void initializeAttachmentsFolder() {
            try {
                Path path = Paths.get(ATTACHMENTS_FOLDER);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
            } catch (IOException e) {
                System.err.println("Error al crear carpeta de adjuntos: " + e.getMessage());
            }
        }

        public void saveAttachmentAsync(File sourceFile, int messageId, Consumer<Attachment> onSuccess, Consumer<String> onError) {
            TaskManager.getExecutor().submit(() -> {
                try {
                    Attachment attachment = saveAttachment(sourceFile, messageId);
                    if (onSuccess != null) {
                        onSuccess.accept(attachment);
                    }
                } catch (IOException | SQLException e) {
                    if (onError != null) {
                        onError.accept("Error al guardar adjunto: " + e.getMessage());
                    }
                }
            });
        }

        public Attachment saveAttachment(File sourceFile, int messageId) throws IOException, SQLException {
            if (!sourceFile.exists()) {
                throw new IllegalArgumentException("El archivo no existe: " + sourceFile.getAbsolutePath());
            }

            String originalFileName = sourceFile.getName();
            String fileExtension = getFileExtension(originalFileName);
            String fileType = getFileType(fileExtension);
            String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;

            Path targetPath = Paths.get(ATTACHMENTS_FOLDER, uniqueFileName);
            Files.copy(sourceFile.toPath(), targetPath);

            Attachment attachment = new Attachment();
            attachment.setMessageId(messageId);
            attachment.setFileName(originalFileName);
            attachment.setFileType(fileType);
            attachment.setFilePath(targetPath.toString());
            attachment.setFileSize(sourceFile.length());

            return attachmentDAO.create(attachment);
        }

        public List<Attachment> getAttachmentsByMessage(int messageId) {
            try {
                return attachmentDAO.findByMessageId(messageId);
            } catch (SQLException e) {
                throw new RuntimeException("Error al obtener adjuntos: " + e.getMessage(), e);
            }
        }

        public Attachment getAttachment(int attachmentId) {
            try {
                return attachmentDAO.findById(attachmentId);
            } catch (SQLException e) {
                throw new RuntimeException("Error al obtener adjunto: " + e.getMessage(), e);
            }
        }

        public boolean deleteAttachment(int attachmentId) {
            try {
                Attachment attachment = attachmentDAO.findById(attachmentId);
                if (attachment != null && !attachment.getFilePath().isEmpty()) {
                    Files.deleteIfExists(Paths.get(attachment.getFilePath()));
                }
                return attachmentDAO.delete(attachmentId);
            } catch (SQLException | IOException e) {
                throw new RuntimeException("Error al eliminar adjunto: " + e.getMessage(), e);
            }
        }

        public boolean deleteAttachmentsByMessage(int messageId) {
            try {
                List<Attachment> attachments = attachmentDAO.findByMessageId(messageId);
                for (Attachment attachment : attachments) {
                    Files.deleteIfExists(Paths.get(attachment.getFilePath()));
                }
                return attachmentDAO.deleteByMessageId(messageId);
            } catch (SQLException | IOException e) {
                throw new RuntimeException("Error al eliminar adjuntos: " + e.getMessage(), e);
            }
        }

        private String getFileExtension(String fileName) {
            int lastDotIndex = fileName.lastIndexOf('.');
            return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : "unknown";
        }

        private String getFileType(String extension) {
            return switch (extension) {
                case "pdf" -> "application/pdf";
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "doc", "docx" -> "application/msword";
                case "xls", "xlsx" -> "application/vnd.ms-excel";
                case "ppt", "pptx" -> "application/vnd.ms-powerpoint";
                case "zip" -> "application/zip";
                case "txt" -> "text/plain";
                case "mp3" -> "audio/mpeg";
                case "mp4" -> "video/mp4";
                default -> "application/octet-stream";
            };
        }
    }