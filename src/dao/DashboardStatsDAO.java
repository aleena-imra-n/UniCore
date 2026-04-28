package dao;

import util.DBConnection;
import java.sql.*;

/**
 * DAO for fetching live dashboard stats for all three roles.
 *
 * Student  : enrolled courses, attendance %, current semester GPA
 * Teacher  : assigned courses, total students taught, pending requests
 * Admin    : total active students, total active teachers, total active courses
 */
public class DashboardStatsDAO {

    // ── Student stats ─────────────────────────────────────────────────────────

    /** Count of active enrollments for the student this semester. */
    private static final String SQL_STUDENT_COURSES =
        "SELECT COUNT(*) AS cnt " +
        "FROM   ENROLLMENTS e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   STUDENTS          s  ON s.student_id    = e.student_id " +
        "JOIN   USERS             u  ON u.user_id       = s.user_id " +
        "WHERE  u.username    = ? " +
        "  AND  e.status      = 'active' " +
        "  AND  sem.is_active = 1";

    /** Overall attendance % for the student this semester. */
    private static final String SQL_STUDENT_ATTENDANCE =
        "SELECT CAST( " +
        "  100.0 * SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) " +
        "  / NULLIF(COUNT(*), 0) AS DECIMAL(5,1)) AS pct " +
        "FROM   ATTENDANCE       a " +
        "JOIN   ENROLLMENTS      e   ON e.enrollment_id = a.enrollment_id " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   STUDENTS          s  ON s.student_id    = e.student_id " +
        "JOIN   USERS             u  ON u.user_id       = s.user_id " +
        "WHERE  u.username    = ? " +
        "  AND  sem.is_active = 1";

    /** Percentage score across all marks for the student this semester. */
    private static final String SQL_STUDENT_GPA =
        "SELECT CAST( " +
        "  100.0 * SUM(m.marks_obtained) / NULLIF(SUM(m.total_marks), 0) " +
        "  AS DECIMAL(5,1)) AS pct " +
        "FROM   MARKS            m " +
        "JOIN   ENROLLMENTS      e   ON e.enrollment_id = m.enrollment_id " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   STUDENTS          s  ON s.student_id    = e.student_id " +
        "JOIN   USERS             u  ON u.user_id       = s.user_id " +
        "WHERE  u.username    = ? " +
        "  AND  sem.is_active = 1";

    // ── Teacher stats ─────────────────────────────────────────────────────────

    /** Number of courses assigned to the teacher this semester. */
    private static final String SQL_TEACHER_COURSES =
        "SELECT COUNT(*) AS cnt " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id   = co.teacher_id " +
        "JOIN   USERS             u  ON u.user_id      = t.user_id " +
        "WHERE  u.username    = ? " +
        "  AND  sem.is_active = 1";

    /** Total distinct students enrolled across all teacher's courses this semester. */
    private static final String SQL_TEACHER_STUDENTS =
        "SELECT COUNT(DISTINCT e.student_id) AS cnt " +
        "FROM   ENROLLMENTS      e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id   = co.teacher_id " +
        "JOIN   USERS             u  ON u.user_id      = t.user_id " +
        "WHERE  u.username    = ? " +
        "  AND  e.status      = 'active' " +
        "  AND  sem.is_active = 1";

    /** Pending academic requests for courses taught by this teacher. */
    private static final String SQL_TEACHER_PENDING =
        "SELECT COUNT(*) AS cnt " +
        "FROM   ACADEMIC_REQUESTS ar " +
        "JOIN   COURSE_OFFERINGS  co  ON co.offering_id = ar.offering_id " +
        "JOIN   TEACHERS           t  ON t.teacher_id  = co.teacher_id " +
        "JOIN   USERS              u  ON u.user_id     = t.user_id " +
        "WHERE  u.username  = ? " +
        "  AND  ar.status   = 'pending'";

    // ── Admin stats ───────────────────────────────────────────────────────────

    /** Total active students in the system. */
    private static final String SQL_ADMIN_STUDENTS =
        "SELECT COUNT(*) AS cnt FROM USERS " +
        "WHERE role = 'student' AND is_active = 1";

    /** Total active teachers in the system. */
    private static final String SQL_ADMIN_TEACHERS =
        "SELECT COUNT(*) AS cnt FROM USERS " +
        "WHERE role = 'teacher' AND is_active = 1";

    /** Total active course offerings this semester. */
    private static final String SQL_ADMIN_COURSES =
        "SELECT COUNT(*) AS cnt " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "WHERE  sem.is_active = 1";

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns {enrolledCourses, attendancePct, marksPct} for a student. */
    public String[] getStudentStats(String username) throws SQLException {
        String courses    = queryScalar(SQL_STUDENT_COURSES,    username, "cnt",  "0");
        String attendance = queryScalar(SQL_STUDENT_ATTENDANCE, username, "pct",  "—");
        String marks      = queryScalar(SQL_STUDENT_GPA,        username, "pct",  "—");
        return new String[]{
            courses,
            attendance.equals("—") ? "—" : attendance + "%",
            marks.equals("—")      ? "—" : marks + "%"
        };
    }

    /** Returns {assignedCourses, totalStudents, pendingRequests} for a teacher. */
    public String[] getTeacherStats(String username) throws SQLException {
        String courses  = queryScalar(SQL_TEACHER_COURSES,  username, "cnt", "0");
        String students = queryScalar(SQL_TEACHER_STUDENTS, username, "cnt", "0");
        String pending  = queryScalar(SQL_TEACHER_PENDING,  username, "cnt", "0");
        return new String[]{courses, students, pending};
    }

    /** Returns {totalStudents, totalTeachers, totalCourses} for admin (no username needed). */
    public String[] getAdminStats() throws SQLException {
        String students = queryScalarNoParam(SQL_ADMIN_STUDENTS, "cnt", "0");
        String teachers = queryScalarNoParam(SQL_ADMIN_TEACHERS, "cnt", "0");
        String courses  = queryScalarNoParam(SQL_ADMIN_COURSES,  "cnt", "0");
        return new String[]{students, teachers, courses};
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String queryScalar(String sql, String username,
                                String col, String fallback) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object val = rs.getObject(col);
                    return val == null ? fallback : val.toString();
                }
            }
        }
        return fallback;
    }

    private String queryScalarNoParam(String sql, String col,
                                       String fallback) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Object val = rs.getObject(col);
                return val == null ? fallback : val.toString();
            }
        }
        return fallback;
    }
}