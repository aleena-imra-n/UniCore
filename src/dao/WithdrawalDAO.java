package dao;

import model.EnrolledCourse;
import model.WithdrawalRequest;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Course Withdrawal feature.
 *
 * Five responsibilities:
 *   1. getStudentId          — resolve username → student_id
 *   2. getAdminUserId        — resolve username → user_id (for reviewed_by FK)
 *   3. getActiveEnrollments  — courses student can request withdrawal from
 *   4. getRequestsForStudent — all withdrawal requests for the student (any status)
 *   5. getAllPendingRequests  — all pending requests (admin view)
 *   6. submitRequest         — calls SP_RequestWithdrawal
 *   7. reviewRequest         — calls SP_ReviewWithdrawal
 */
public class WithdrawalDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id " +
        "FROM   STUDENTS s JOIN USERS u ON u.user_id = s.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    private static final String SQL_ADMIN_USER_ID =
        "SELECT u.user_id " +
        "FROM   USERS u " +
        "WHERE  u.username = ? AND u.is_active = 1 AND u.role = 'admin'";

    /**
     * Active enrollments this semester for the student.
     * Excludes enrollments that already have a PENDING request
     * (student cannot re-submit while one is in-flight).
     */
    private static final String SQL_ACTIVE_ENROLLMENTS =
        "SELECT e.enrollment_id, co.offering_id, c.course_code, c.course_name, " +
        "       c.credit_hours, co.section, t.full_name AS teacher_name, " +
        "       e.enrollment_date, e.status " +
        "FROM   ENROLLMENTS      e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "WHERE  e.student_id  = ? " +
        "  AND  e.status      = 'active' " +
        "  AND  sem.is_active = 1 " +
        "  AND  e.enrollment_id NOT IN ( " +
        "           SELECT enrollment_id FROM WITHDRAWAL_REQUESTS " +
        "           WHERE  status = 'pending' " +
        "       ) " +
        "ORDER  BY c.course_code";

    /** All withdrawal requests ever submitted by this student. */
    private static final String SQL_MY_REQUESTS =
        "SELECT wr.request_id, wr.enrollment_id, wr.student_id, " +
        "       s.full_name AS student_name, s.roll_number, " +
        "       c.course_code, c.course_name, co.section, " +
        "       sem.semester_name, " +
        "       wr.reason, wr.status, wr.admin_comment, " +
        "       wr.requested_at, wr.reviewed_at " +
        "FROM   WITHDRAWAL_REQUESTS wr " +
        "JOIN   ENROLLMENTS      e   ON e.enrollment_id  = wr.enrollment_id " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id   = e.offering_id " +
        "JOIN   COURSES          c   ON c.course_id      = co.course_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id  = co.semester_id " +
        "JOIN   STUDENTS         s   ON s.student_id     = wr.student_id " +
        "WHERE  wr.student_id = ? " +
        "ORDER  BY wr.requested_at DESC";

    /** All pending withdrawal requests — admin view. */
    private static final String SQL_ALL_PENDING =
        "SELECT wr.request_id, wr.enrollment_id, wr.student_id, " +
        "       s.full_name AS student_name, s.roll_number, " +
        "       c.course_code, c.course_name, co.section, " +
        "       sem.semester_name, " +
        "       wr.reason, wr.status, wr.admin_comment, " +
        "       wr.requested_at, wr.reviewed_at " +
        "FROM   WITHDRAWAL_REQUESTS wr " +
        "JOIN   ENROLLMENTS      e   ON e.enrollment_id  = wr.enrollment_id " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id   = e.offering_id " +
        "JOIN   COURSES          c   ON c.course_id      = co.course_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id  = co.semester_id " +
        "JOIN   STUDENTS         s   ON s.student_id     = wr.student_id " +
        "WHERE  wr.status = 'pending' " +
        "ORDER  BY wr.requested_at ASC";

    private static final String SQL_SUBMIT =
        "EXEC SP_RequestWithdrawal @enrollment_id = ?, @reason = ?";

    private static final String SQL_REVIEW =
        "EXEC SP_ReviewWithdrawal " +
        "  @request_id = ?, @admin_user_id = ?, " +
        "  @decision = ?, @admin_comment = ?";

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

    public int getAdminUserId(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ADMIN_USER_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }

    /**
     * Active enrollments the student can request withdrawal from
     * (excludes courses that already have a pending request).
     */
    public List<EnrolledCourse> getActiveEnrollments(int studentId) throws SQLException {
        List<EnrolledCourse> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ACTIVE_ENROLLMENTS)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("enrollment_date");
                    list.add(new EnrolledCourse(
                        rs.getInt("enrollment_id"),
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("credit_hours"),
                        rs.getString("section"),
                        rs.getString("teacher_name"),
                        d != null ? d.toLocalDate() : LocalDate.now(),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    /** All withdrawal requests the student has ever submitted (any status). */
    public List<WithdrawalRequest> getRequestsForStudent(int studentId) throws SQLException {
        return queryRequests(SQL_MY_REQUESTS, studentId);
    }

    /** All pending withdrawal requests across all students (admin view). */
    public List<WithdrawalRequest> getAllPendingRequests() throws SQLException {
        return queryRequests(SQL_ALL_PENDING, -1);
    }

    /**
     * Submits a withdrawal request via SP_RequestWithdrawal.
     * Throws SQLException with the SP's RAISERROR text on validation failure.
     */
    public void submitRequest(int enrollmentId, String reason) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SUBMIT)) {
            ps.setInt(1, enrollmentId);
            ps.setString(2, reason);
            ps.execute();
        }
    }

    /**
     * Approves or rejects a request via SP_ReviewWithdrawal.
     * On approval the SP also marks the enrollment as 'withdrawn'.
     */
    public void reviewRequest(int requestId, int adminUserId,
                              String decision, String adminComment) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_REVIEW)) {
            ps.setInt(1, requestId);
            ps.setInt(2, adminUserId);
            ps.setString(3, decision);
            ps.setString(4, adminComment);
            ps.execute();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<WithdrawalRequest> queryRequests(String sql, int paramId)
            throws SQLException {
        List<WithdrawalRequest> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (paramId >= 0) ps.setInt(1, paramId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp reqTs = rs.getTimestamp("requested_at");
                    Timestamp revTs = rs.getTimestamp("reviewed_at");
                    list.add(new WithdrawalRequest(
                        rs.getInt("request_id"),
                        rs.getInt("enrollment_id"),
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("roll_number"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("semester_name"),
                        rs.getString("reason"),
                        rs.getString("status"),
                        rs.getString("admin_comment"),
                        reqTs != null ? reqTs.toLocalDateTime() : null,
                        revTs != null ? revTs.toLocalDateTime() : null
                    ));
                }
            }
        }
        return list;
    }
}