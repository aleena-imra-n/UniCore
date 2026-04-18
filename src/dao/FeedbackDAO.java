package dao;

import model.FeedbackItem;
import model.OfferingItem;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DAO for the Feedback feature (both student submission and teacher view).
 *
 * Responsibilities:
 *   1. getStudentId               — resolve username → student_id
 *   2. getEnrolledOfferings       — courses student can give feedback for
 *   3. hasAlreadySubmitted        — check UQ_Feedback constraint before insert
 *   4. submitFeedback             — INSERT one feedback row
 *   5. getSubmittedFeedback       — all feedback the student already submitted
 *   6. getOfferingsWithFeedback   — teacher: courses that have >= 1 feedback
 *   7. getFeedbackForOffering     — teacher: all feedback rows for one course
 *   8. getAverageRating           — teacher: avg rating per offering
 */
public class FeedbackDAO {

    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id FROM STUDENTS s JOIN USERS u ON u.user_id = s.user_id " +
        "WHERE u.username = ? AND u.is_active = 1";

    /** Enrolled offerings for the student this semester. */
    private static final String SQL_ENROLLED_OFFERINGS =
        "SELECT co.offering_id, c.course_code, c.course_name, co.section " +
        "FROM   ENROLLMENTS      e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   STUDENTS          s  ON s.student_id    = e.student_id " +
        "JOIN   USERS             u  ON u.user_id       = s.user_id " +
        "WHERE  u.username    = ? " +
        "  AND  e.status      = 'active' " +
        "  AND  sem.is_active = 1 " +
        "ORDER  BY c.course_code";

    private static final String SQL_HAS_SUBMITTED =
        "SELECT COUNT(*) FROM FEEDBACK " +
        "WHERE student_id = ? AND offering_id = ?";

    private static final String SQL_INSERT =
        "INSERT INTO FEEDBACK (student_id, offering_id, rating, comments, is_anonymous) " +
        "VALUES (?, ?, ?, ?, 1)";

    /** All feedback the student has already submitted this semester. */
    private static final String SQL_SUBMITTED =
        "SELECT f.feedback_id, f.offering_id, c.course_code, c.course_name, " +
        "       f.rating, f.comments, f.submitted_at " +
        "FROM   FEEDBACK         f " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id = f.offering_id " +
        "JOIN   COURSES           c  ON c.course_id    = co.course_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "WHERE  f.student_id  = ? " +
        "  AND  sem.is_active = 1";

    /** Teacher: offerings that have at least 1 feedback row. */
    private static final String SQL_OFFERINGS_WITH_FEEDBACK =
        "SELECT co.offering_id, c.course_code, c.course_name, co.section, " +
        "       COUNT(f.feedback_id) AS feedback_count, " +
        "       CAST(AVG(CAST(f.rating AS FLOAT)) AS DECIMAL(3,2)) AS avg_rating " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   COURSES           c   ON c.course_id    = co.course_id " +
        "JOIN   SEMESTERS        sem  ON sem.semester_id = co.semester_id " +
        "JOIN   TEACHERS          t   ON t.teacher_id   = co.teacher_id " +
        "JOIN   USERS             u   ON u.user_id      = t.user_id " +
        "JOIN   FEEDBACK          f   ON f.offering_id  = co.offering_id " +
        "WHERE  u.username    = ? " +
        "  AND  sem.is_active = 1 " +
        "GROUP  BY co.offering_id, c.course_code, c.course_name, co.section " +
        "ORDER  BY c.course_code";

    /** Teacher: all (anonymous) feedback rows for one offering. */
    private static final String SQL_FEEDBACK_FOR_OFFERING =
        "SELECT f.feedback_id, f.offering_id, c.course_code, c.course_name, " +
        "       f.rating, f.comments, f.submitted_at " +
        "FROM   FEEDBACK         f " +
        "JOIN   COURSE_OFFERINGS co ON co.offering_id = f.offering_id " +
        "JOIN   COURSES           c ON c.course_id    = co.course_id " +
        "WHERE  f.offering_id = ? " +
        "ORDER  BY f.submitted_at DESC";

    // ── Public API ────────────────────────────────────────────────────────────

    public int getStudentId(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_STUDENT_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("student_id") : -1;
            }
        }
    }

    public List<OfferingItem> getEnrolledOfferings(String username) throws SQLException {
        List<OfferingItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ENROLLED_OFFERINGS)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OfferingItem(
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"), ""));
                }
            }
        }
        return list;
    }

    public boolean hasAlreadySubmitted(int studentId, int offeringId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_HAS_SUBMITTED)) {
            ps.setInt(1, studentId);
            ps.setInt(2, offeringId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void submitFeedback(int studentId, int offeringId,
                               int rating, String comments) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, studentId);
            ps.setInt(2, offeringId);
            ps.setInt(3, rating);
            ps.setString(4, comments);
            ps.executeUpdate();
        }
    }

    public List<FeedbackItem> getSubmittedFeedback(int studentId) throws SQLException {
        List<FeedbackItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SUBMITTED)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("submitted_at");
                    list.add(new FeedbackItem(
                        rs.getInt("feedback_id"),
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("rating"),
                        rs.getString("comments"),
                        ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                        true));
                }
            }
        }
        return list;
    }

    /** Returns offerings with feedback + count + avg rating for a teacher. */
    public List<Object[]> getOfferingsWithFeedback(String teacherUsername)
            throws SQLException {
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_OFFERINGS_WITH_FEEDBACK)) {
            ps.setString(1, teacherUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getInt("feedback_count"),
                        rs.getDouble("avg_rating")
                    });
                }
            }
        }
        return list;
    }

    public List<FeedbackItem> getFeedbackForOffering(int offeringId) throws SQLException {
        List<FeedbackItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FEEDBACK_FOR_OFFERING)) {
            ps.setInt(1, offeringId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("submitted_at");
                    list.add(new FeedbackItem(
                        rs.getInt("feedback_id"),
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("rating"),
                        rs.getString("comments"),
                        ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                        true));
                }
            }
        }
        return list;
    }
}