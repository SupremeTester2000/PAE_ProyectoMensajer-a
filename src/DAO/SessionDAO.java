package DAO;

import Model.Session;

public class SessionDAO {
 
    public boolean save(Session session){return false;}
    
    public Session findActiveSession(int userId){return null;}
 
    public boolean closeSession(int sessionid){return false;}
    
}