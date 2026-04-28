package dao;

import util.DBConnection;
import java.sql.*;

/**
 * Data Access Object for user authentication.
 * Handles database queries for login verification.
 */
public class AuthDAO {
    
    // Query to authenticate user and get their role and IDs
    private static final String SQL_AUTHENTICATE = 
        "SELECT u.user_id, u.username, u.role, " +
        "       CASE " +
        "           WHEN u.role = 'student' THEN s.student_id " +
        "           WHEN u.role = 'teacher' THEN t.teacher_id " +
        "           WHEN u.role = 'admin'   THEN a.admin_id " +
        "       END as person_id " +
        "FROM USERS u " +
        "LEFT JOIN STUDENTS s ON u.user_id = s.user_id AND u.role = 'student' " +
        "LEFT JOIN TEACHERS t ON u.user_id = t.user_id AND u.role = 'teacher' " +
        "LEFT JOIN ADMINS   a ON u.user_id = a.user_id AND u.role = 'admin' " +
        "WHERE u.username = ? AND u.password_hash = ? AND u.is_active = 1";
    
    /**
     * Authenticates a user against the database.
     * 
     * @param username the login username
     * @param password the login password (plain text)
     * @return AuthResult with user details, or null if authentication fails
     * @throws SQLException if database error occurs
     */
    public AuthResult authenticate(String username, String password) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_AUTHENTICATE)) {
            
            ps.setString(1, username);
            ps.setString(2, password);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthResult(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getInt("person_id")
                    );
                }
                return null;
            }
        }
    }
    
    /**
     * Inner class to hold authentication result before converting to model.
     */
    public static class AuthResult {
        private final int userId;
        private final String username;
        private final String role;
        private final int personId;
        
        public AuthResult(int userId, String username, String role, int personId) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.personId = personId;
        }
        
        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public int getPersonId() { return personId; }
    }
}