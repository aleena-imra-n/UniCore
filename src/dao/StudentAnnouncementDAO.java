package dao;

import model.StudentAnnouncementItem;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Student Announcements feature.
 *
 * Fetches all announcements visible to a given student, which means:
 *   1. Course-specific announcements  — student is actively enrolled in that offering
 *   2. Department-wide announcements  — student's sub-dept belongs to that major dept
 *   3. University-wide announcements  — offering_id IS NULL AND major_dept_id IS NULL
 *
 * Results are always ordered newest-first (posted_at DESC).
 */
public class StudentAnnouncementDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** Resolve login username → student_id. */
    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id " +
        "FROM   STUDENTS s " +
        "JOIN   USERS    u ON u.user_id = s.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /**
     * All announcements visible to this student, newest first.
     *
     * Visibility rules (UNION of three scopes):
     *   1. Course-specific: student is enrolled in the announcement's offering
     *   2. Dept-wide:       student's sub-dept → major_dept matches announcement's major_dept
     *   3. University-wide: both offering_id and major_dept_id are NULL
     *
     * Only active announcements (is_active = 1) targeting 'student' or 'all' are returned.
     */
    private static final String SQL_ANNOUNCEMENTS =
        // ── Scope 1: course-specific ──────────────────────────────────────────
        "SELECT a.announcement_id, " +
        "       a.title, " +
        "       a.content, " +
        "       c.course_name, " +
        "       c.course_code, " +
        "       'Course' AS scope_type, " +
        "       t.full_name AS posted_by_name, " +
        "       a.posted_at " +
        "FROM   ANNOUNCEMENTS     a " +
        "JOIN   COURSE_OFFERINGS  co ON co.offering_id = a.offering_id " +
        "JOIN   COURSES            c ON c.course_id    = co.course_id " +
        "JOIN   TEACHERS           t ON t.teacher_id   = co.teacher_id " +
        "JOIN   ENROLLMENTS        e ON e.offering_id  = a.offering_id " +
        "                           AND e.student_id   = ? " +
        "                           AND e.status       = 'active' " +
        "WHERE  a.is_active   = 1 " +
        "  AND  a.target_role IN ('all', 'student') " +
        "  AND  a.offering_id IS NOT NULL " +

        "UNION " +

        // ── Scope 2: department-wide ──────────────────────────────────────────
        "SELECT a.announcement_id, " +
        "       a.title, " +
        "       a.content, " +
        "       md.name AS course_name, " +
        "       md.code AS course_code, " +
        "       'Department' AS scope_type, " +
        "       u.username  AS posted_by_name, " +
        "       a.posted_at " +
        "FROM   ANNOUNCEMENTS      a " +
        "JOIN   MAJOR_DEPARTMENTS  md ON md.major_dept_id = a.major_dept_id " +
        "JOIN   USERS               u ON u.user_id        = a.posted_by " +
        "JOIN   STUDENTS            s ON s.student_id     = ? " +
        "JOIN   SUB_DEPARTMENTS    sd ON sd.sub_dept_id   = s.sub_dept_id " +
        "                           AND sd.major_dept_id  = a.major_dept_id " +
        "WHERE  a.is_active      = 1 " +
        "  AND  a.target_role   IN ('all', 'student') " +
        "  AND  a.offering_id   IS NULL " +
        "  AND  a.major_dept_id IS NOT NULL " +

        "UNION " +

        // ── Scope 3: university-wide ──────────────────────────────────────────
        "SELECT a.announcement_id, " +
        "       a.title, " +
        "       a.content, " +
        "       'University-wide' AS course_name, " +
        "       ''               AS course_code, " +
        "       'University'     AS scope_type, " +
        "       u.username       AS posted_by_name, " +
        "       a.posted_at " +
        "FROM   ANNOUNCEMENTS a " +
        "JOIN   USERS          u ON u.user_id = a.posted_by " +
        "WHERE  a.is_active      = 1 " +
        "  AND  a.target_role   IN ('all', 'student') " +
        "  AND  a.offering_id   IS NULL " +
        "  AND  a.major_dept_id IS NULL " +

        "ORDER  BY posted_at DESC";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves the login username to a student_id.
     *
     * @param username  login username from the session
     * @return student_id, or -1 if not found / not an active student account
     */
    public int getStudentId(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_STUDENT_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("student_id") : -1;
            }
        }
    }

    /**
     * Returns all announcements visible to this student, newest first.
     *
     * @param studentId  resolved student_id
     * @return list of StudentAnnouncementItem (never null; may be empty)
     */
    public List<StudentAnnouncementItem> getAnnouncementsForStudent(int studentId)
            throws SQLException {

        List<StudentAnnouncementItem> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ANNOUNCEMENTS)) {

            // Three scopes each need studentId once
            ps.setInt(1, studentId); // scope 1
            ps.setInt(2, studentId); // scope 2
            // scope 3 needs no studentId parameter

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("posted_at");
                    LocalDateTime postedAt = (ts != null)
                        ? ts.toLocalDateTime() : LocalDateTime.now();

                    list.add(new StudentAnnouncementItem(
                        rs.getInt("announcement_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("course_name"),
                        rs.getString("course_code"),
                        rs.getString("scope_type"),
                        rs.getString("posted_by_name"),
                        postedAt
                    ));
                }
            }
        }
        return list;
    }
}