package bl;

import dao.AuthDAO;
import model.UserSession;
import java.sql.SQLException;

/**
 * Business Logic for user authentication.
 * Handles login validation and session creation.
 */
public class AuthService {
    
    private final AuthDAO authDAO;
    
    public AuthService() {
        this.authDAO = new AuthDAO();
    }
    
    /**
     * Authenticates a user and creates a session.
     * 
     * @param username login username
     * @param password login password
     * @return UserSession if authentication succeeds, null otherwise
     */
    public UserSession login(String username, String password) {
        // Basic validation
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        if (password == null || password.isEmpty()) {
            return null;
        }
        
        try {
            AuthDAO.AuthResult result = authDAO.authenticate(username, password);
            if (result != null) {
                return new UserSession(
                    result.getUserId(),
                    result.getUsername(),
                    result.getRole(),
                    result.getPersonId()
                );
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}