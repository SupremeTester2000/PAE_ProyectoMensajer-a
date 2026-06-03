package DAO;

import Model.Notification;
import java.util.List;

public class NotificationDAO {

    public boolean save(Notification notification){return false;}
    
    public List<Notification> findByUser(int userId){return null;}
    
    public boolean markAsRead(int notificationId){return false;}
}
