package model;

/**
 * Represents an authenticated user session.
 * Returned by AuthService after successful login.
 */
public class UserSession {
    
    private final int userId;
    private final String username;
    private final String role;
    private final int personId;  // student_id, teacher_id, or admin_id
    
    public UserSession(int userId, String username, String role, int personId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.personId = personId;
    }
    
    // ── Getters ──────────────────────────────────────────────────────────────
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public int getPersonId() { return personId; }
    
    // ── Role checks ──────────────────────────────────────────────────────────
    public boolean isStudent() { return "student".equalsIgnoreCase(role); }
    public boolean isTeacher() { return "teacher".equalsIgnoreCase(role); }
    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }
    
    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}