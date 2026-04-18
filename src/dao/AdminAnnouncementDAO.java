package dao;

import model.AdminAnnouncementItem;
import model.AdminAnnouncementItem.Scope;
import model.DeptOption;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Admin Send Announcements feature.
 *
 * Responsibilities:
 *   1. getUserId          — resolve admin username → user_id (for posted_by FK)
 *   2. getAllDepts        — populate the department dropdown
 *   3. getPostedByAdmin   — load announcements previously posted by this admin
 *   4. postAnnouncement  — INSERT new announcement row (university or dept scoped)
 *   5. deactivate        — soft-delete (is_active = 0)
 */
public class AdminAnnouncementDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_USER_ID =
        "SELECT user_id FROM USERS WHERE username = ? AND is_active = 1";

    private static final String SQL_ALL_DEPTS =
        "SELECT major_dept_id, name, code " +
        "FROM   MAJOR_DEPARTMENTS ORDER BY name";

    /**
     * Loads all active announcements posted by a specific user (the admin),
     * ordered newest first.
     */
    private static final String SQL_POSTED_BY_ADMIN =
        "SELECT a.announcement_id, a.title, a.content, a.scope, " +
        "       a.target_role, a.major_dept_id, a.posted_at, a.is_active, " +
        "       md.name AS dept_name " +
        "FROM   ANNOUNCEMENTS a " +
        "LEFT JOIN MAJOR_DEPARTMENTS md ON md.major_dept_id = a.major_dept_id " +
        "WHERE  a.posted_by = ? " +
        "ORDER  BY a.posted_at DESC";

    /**
     * INSERT a university-wide announcement (major_dept_id = NULL).
     */
    private static final String SQL_INSERT_UNIVERSITY =
        "INSERT INTO ANNOUNCEMENTS " +
        "(posted_by, major_dept_id, offering_id, title, content, target_role, scope) " +
        "VALUES (?, NULL, NULL, ?, ?, ?, 'university')";

    /**
     * INSERT a department-scoped announcement (major_dept_id set).
     */
    private static final String SQL_INSERT_DEPT =
        "INSERT INTO ANNOUNCEMENTS " +
        "(posted_by, major_dept_id, offering_id, title, content, target_role, scope) " +
        "VALUES (?, ?, NULL, ?, ?, ?, 'department')";

    private static final String SQL_DEACTIVATE =
        "UPDATE ANNOUNCEMENTS SET is_active = 0 WHERE announcement_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves the admin's login username to their user_id.
     *
     * @param username  login username
     * @return user_id, or -1 if not found
     */
    public int getUserId(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_USER_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }

    /**
     * Returns all major departments for the department scope dropdown.
     */
    public List<DeptOption> getAllDepts() throws SQLException {
        List<DeptOption> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ALL_DEPTS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DeptOption(
                    rs.getInt("major_dept_id"),
                    rs.getString("name"),
                    rs.getString("code")
                ));
            }
        }
        return list;
    }

    /**
     * Returns all announcements posted by a specific admin user,
     * newest first. Used to populate the "Posted Announcements" panel.
     *
     * @param userId  the admin's user_id
     */
    public List<AdminAnnouncementItem> getPostedByAdmin(int userId) throws SQLException {
        List<AdminAnnouncementItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_POSTED_BY_ADMIN)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String scopeStr = rs.getString("scope");
                    Scope  scope    = "university".equalsIgnoreCase(scopeStr)
                                     ? Scope.UNIVERSITY : Scope.DEPARTMENT;

                    int deptIdRaw = rs.getInt("major_dept_id");
                    Integer deptId = rs.wasNull() ? null : deptIdRaw;

                    Timestamp ts = rs.getTimestamp("posted_at");
                    LocalDateTime postedAt = ts != null
                        ? ts.toLocalDateTime() : LocalDateTime.now();

                    list.add(new AdminAnnouncementItem(
                        rs.getInt("announcement_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        scope,
                        rs.getString("target_role"),
                        deptId,
                        rs.getString("dept_name"),
                        postedAt,
                        rs.getBoolean("is_active")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Posts a university-wide announcement (visible to everyone, no dept filter).
     *
     * @param userId     admin's user_id
     * @param title      announcement title
     * @param content    message body
     * @param targetRole "all" | "student" | "teacher"
     * @return generated announcement_id, or -1 on failure
     */
    public int postUniversity(int userId, String title,
                              String content, String targetRole) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SQL_INSERT_UNIVERSITY, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setString(4, targetRole);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Posts a department-scoped announcement (visible only to that faculty's
     * students and teachers).
     *
     * @param userId      admin's user_id
     * @param majorDeptId target department
     * @param title       announcement title
     * @param content     message body
     * @param targetRole  "all" | "student" | "teacher"
     * @return generated announcement_id, or -1 on failure
     */
    public int postDepartment(int userId, int majorDeptId, String title,
                              String content, String targetRole) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SQL_INSERT_DEPT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, majorDeptId);
            ps.setString(3, title);
            ps.setString(4, content);
            ps.setString(5, targetRole);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Soft-deletes an announcement by setting is_active = 0.
     * The announcement disappears from all student/teacher views immediately.
     *
     * @param announcementId target announcement
     * @return true if a row was updated
     */
    public boolean deactivate(int announcementId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DEACTIVATE)) {
            ps.setInt(1, announcementId);
            return ps.executeUpdate() > 0;
        }
    }
}
