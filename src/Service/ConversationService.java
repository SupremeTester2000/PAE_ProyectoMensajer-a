package Service;

import DAO.ConversationDAO;
import DAO.UserDAO;
import Model.Conversation;
import Model.User;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConversationService {
    
    private ConversationDAO conversationDAO;
    private UserDAO userDAO;
    
    public ConversationService() {
        this.conversationDAO = new ConversationDAO();
        this.userDAO = new UserDAO();
    }

    public Conversation createConversation(int creatorId, List<Integer> participantes) {
        if (creatorId <= 0) {
            throw new IllegalArgumentException("Usuario creador inválido.");
        }

        if (participantes == null || participantes.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un usuario para crear la conversación.");
        }

        Set<Integer> participantSet = new LinkedHashSet<>();
        participantSet.add(creatorId);

        for (Integer participantId : participantes) {
            if (participantId != null && participantId > 0 && participantId != creatorId) {
                participantSet.add(participantId);
            }
        }

        if (participantSet.size() != 2) {
            throw new IllegalArgumentException("Las conversaciones privadas requieren exactamente dos usuarios.");
        }

        List<Integer> participantIds = new ArrayList<>(participantSet);
        int otherUserId = participantIds.get(0) == creatorId
                ? participantIds.get(1)
                : participantIds.get(0);

        try {
            User creator = userDAO.findById(creatorId);
            User otherUser = userDAO.findById(otherUserId);

            if (creator == null || otherUser == null) {
                throw new IllegalArgumentException("Uno de los usuarios no existe.");
            }

            Conversation existingConversation =
                    conversationDAO.findPrivateConversationBetween(creatorId, otherUserId);

            if (existingConversation != null) {
                return existingConversation;
            }

            Conversation conversation = new Conversation();
            conversation.setType("PRIVATE");
            conversation.setGroupName(null);
            conversation.setCreationDate(LocalDateTime.now());

            return conversationDAO.saveWithParticipants(conversation, participantIds);
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear conversación: " + e.getMessage(), e);
        }
    }

    public Conversation createGroupConversation(
            int creatorId,
            String groupName,
            List<Integer> participantes) {

        if (creatorId <= 0) {
            throw new IllegalArgumentException("Usuario creador inválido.");
        }

        String cleanGroupName = groupName != null ? groupName.trim() : "";
        if (cleanGroupName.isEmpty()) {
            throw new IllegalArgumentException("Ingresa un nombre para el grupo.");
        }

        if (participantes == null || participantes.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un participante.");
        }

        Set<Integer> participantSet = new LinkedHashSet<>();
        participantSet.add(creatorId);

        for (Integer participantId : participantes) {
            if (participantId != null && participantId > 0 && participantId != creatorId) {
                participantSet.add(participantId);
            }
        }

        if (participantSet.size() < 2) {
            throw new IllegalArgumentException("Selecciona al menos un participante.");
        }

        try {
            for (Integer participantId : participantSet) {
                if (userDAO.findById(participantId) == null) {
                    throw new IllegalArgumentException("Uno de los usuarios no existe.");
                }
            }

            Conversation conversation = new Conversation();
            conversation.setType("GROUP");
            conversation.setGroupName(cleanGroupName);
            conversation.setCreationDate(LocalDateTime.now());

            return conversationDAO.saveWithParticipants(
                    conversation,
                    new ArrayList<>(participantSet));
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear grupo: " + e.getMessage(), e);
        }
    }

    public boolean addParticipants(int conversationId, List<Integer> participantIds) {
        if (conversationId <= 0) {
            throw new IllegalArgumentException("Conversación inválida.");
        }

        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("Selecciona participantes.");
        }

        Set<Integer> participantSet = new LinkedHashSet<>();
        for (Integer participantId : participantIds) {
            if (participantId != null && participantId > 0) {
                participantSet.add(participantId);
            }
        }

        if (participantSet.isEmpty()) {
            throw new IllegalArgumentException("Selecciona participantes válidos.");
        }

        try {
            Conversation conversation = conversationDAO.findById(conversationId);
            if (conversation == null) {
                throw new IllegalArgumentException("La conversación no existe.");
            }

            for (Integer participantId : participantSet) {
                if (userDAO.findById(participantId) == null) {
                    throw new IllegalArgumentException("Uno de los usuarios no existe.");
                }
            }

            return conversationDAO.addParticipants(
                    conversationId,
                    new ArrayList<>(participantSet));
        } catch (SQLException e) {
            throw new RuntimeException("Error al agregar participantes: " + e.getMessage(), e);
        }
    }

    public List<User> getParticipants(int conversationId) {
        try {
            return conversationDAO.findParticipantsByConversation(conversationId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener participantes: " + e.getMessage(), e);
        }
    }

    public List<Conversation> getUserConversations(int userId) {
        try {
            return conversationDAO.findByUser(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener conversaciones del usuario: " + e.getMessage(), e);
        }
    }

    public List<Conversation> searchConversations(int userId, String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return conversationDAO.findByUser(userId);
            }
            return conversationDAO.searchByName(userId, searchTerm);
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar conversaciones: " + e.getMessage(), e);
        }
    }

    public Conversation getConversation(int conversationId) {
        try {
            return conversationDAO.findById(conversationId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener conversación: " + e.getMessage(), e);
        }
    }

    public String getConversationDisplayName(int conversationId, int currentUserId) {
        try {
            return conversationDAO.getConversationDisplayName(conversationId, currentUserId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener nombre de conversación: " + e.getMessage(), e);
        }
    }
}