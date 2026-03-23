package dao;

import model.AnnouncementItem;
import model.TeacherOfferingItem;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Teacher Announcements feature.
 *
 * Four focused responsibilities:
 *   1. getTeacherId           — resolve login username → teacher_id
 *   2. getOfferingsForTeacher — populate the course dropdown
 *   3. getPostedAnnouncements — load existing announcements for the right panel
 *   4. postAnnouncement       — INSERT a new announcement row
 */
public class AnnouncementDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** Resolve login username → teacher_id. */
    private static final String SQL_TEACHER_ID =
        "SELECT t.teacher_id " +
        "FROM   TEACHERS t " +
        "JOIN   USERS    u ON u.user_id = t.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /**
     * All active offerings assigned to this teacher in the current semester.
     * Used to populate the "Course" dropdown on the form.
     */
    private static final String SQL_OFFERINGS =
        "SELECT co.offering_id, c.course_code, c.course_name, co.section " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   COURSES           c   ON c.course_id    = co.course_id " +
        "JOIN   SEMESTERS         sem ON sem.semester_id = co.semester_id " +
        "WHERE  co.teacher_id  = ? " +
        "  AND  sem.is_active  = 1 " +
        "ORDER  BY c.course_code, co.section";

    /**
     * All active announcements the teacher has already posted,
     * newest first. Shown in the right-hand "Posted Announcements" panel.
     */
    private static final String SQL_POSTED =
        "SELECT a.announcement_id, " +
        "       co.offering_id, " +
        "       c.course_code, " +
        "       c.course_name, " +
        "       a.title, " +
        "       a.content, " +
        "       a.target_role, " +
        "       a.posted_at " +
        "FROM   ANNOUNCEMENTS    a " +
        "JOIN   COURSE_OFFERINGS co ON co.offering_id = a.offering_id " +
        "JOIN   COURSES           c ON c.course_id    = co.course_id " +
        "WHERE  co.teacher_id  = ? " +
        "  AND  a.is_active    = 1 " +
        "ORDER  BY a.posted_at DESC";

    /**
     * INSERT a new announcement.
     * posted_at and is_active use DB defaults (GETDATE() and 1).
     */
    private static final String SQL_INSERT =
        "INSERT INTO ANNOUNCEMENTS " +
        "  (posted_by, offering_id, title, content, target_role) " +
        "VALUES (?, ?, ?, ?, ?)";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves the login username to a teacher_id.
     *
     * @param username  login username from the session
     * @return teacher_id, or -1 if not found / not an active teacher account
     */
    public int getTeacherId(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_TEACHER_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("teacher_id") : -1;
            }
        }
    }

    /**
     * Returns the teacher's user_id (needed as posted_by FK in ANNOUNCEMENTS).
     * Derived from teacher_id via a join on USERS.
     */
    public int getUserId(String username) throws SQLException {
        String sql =
            "SELECT u.user_id FROM USERS u " +
            "JOIN TEACHERS t ON t.user_id = u.user_id " +
            "WHERE u.username = ? AND u.is_active = 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }

    /**
     * Returns all course offerings assigned to the teacher in the active semester.
     *
     * @param teacherId  resolved teacher_id
     * @return list of TeacherOfferingItem for the dropdown (never null)
     */
    public List<TeacherOfferingItem> getOfferingsForTeacher(int teacherId)
            throws SQLException {
        List<TeacherOfferingItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_OFFERINGS)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TeacherOfferingItem(
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Returns all active announcements the teacher has already posted,
     * ordered newest-first.
     *
     * @param teacherId  resolved teacher_id
     * @return list of AnnouncementItem for the right panel (never null)
     */
    public List<AnnouncementItem> getPostedAnnouncements(int teacherId)
            throws SQLException {
        List<AnnouncementItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_POSTED)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("posted_at");
                    LocalDateTime postedAt = (ts != null)
                        ? ts.toLocalDateTime() : LocalDateTime.now();
                    list.add(new AnnouncementItem(
                        rs.getInt("announcement_id"),
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("target_role"),
                        postedAt
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Inserts a new announcement row and returns the generated announcement_id.
     *
     * @param postedBy    user_id of the teacher (FK to USERS)
     * @param offeringId  target course offering
     * @param title       short subject line
     * @param content     full message body
     * @param targetRole  "all" | "student" | "teacher"
     * @return generated announcement_id, or -1 on failure
     */
    public int postAnnouncement(int postedBy, int offeringId,
                                String title, String content,
                                String targetRole) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, postedBy);
            ps.setInt(2, offeringId);
            ps.setString(3, title);
            ps.setString(4, content);
            ps.setString(5, targetRole);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }
}
