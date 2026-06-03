package Service;

import DAO.UserDAO;
import Model.User;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public User getUser(int id) {
        try {
            return userDAO.findById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener usuario: " + e.getMessage(), e);
        }
    }

    public boolean updateProfile(User user) {
        return false;
    }

    public List<User> getAvailableUsers(int excludedUserId) {
        try {
            return userDAO.findAllExcept(excludedUserId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener usuarios disponibles: " + e.getMessage(), e);
        }
    }

    public List<User> searchUsers(String keyword) {
        return searchUsers(keyword, 0);
    }

    public List<User> searchUsers(String keyword, int excludedUserId) {
        try {
            String searchTerm = keyword != null ? keyword.trim() : "";
            if (searchTerm.isEmpty()) {
                return new ArrayList<>();
            }
            return userDAO.searchByKeyword(searchTerm, excludedUserId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuarios: " + e.getMessage(), e);
        }
    }
    
    public User getOtherParticipant(
        int conversationId,
        int currentUserId) {

    try {

        return userDAO.getOtherParticipant(
                conversationId,
                currentUserId);

    } catch (SQLException e) {

        throw new RuntimeException(
                "Error al obtener participante: "
                + e.getMessage(),
                e);
    }
}
}

