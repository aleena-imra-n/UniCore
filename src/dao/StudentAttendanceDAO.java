package dao;

import model.StudentAttendanceItem;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Student Attendance feature.
 *
 * Two responsibilities:
 *   1. getStudentId              — resolve login username → student_id
 *   2. getAttendanceSummary      — per-course attendance totals for the student
 */
public class StudentAttendanceDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** Resolve login username → student_id. */
    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id " +
        "FROM   STUDENTS s " +
        "JOIN   USERS    u ON u.user_id = s.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /**
     * Per-course attendance summary for a student in the active semester.
     *
     * For each active enrollment we compute:
     *   total_classes   — distinct class_date rows recorded for that enrollment
     *   classes_present — rows with status = 'present'
     *   classes_late    — rows with status = 'late'
     *   classes_absent  — rows with status = 'absent'
     *   classes_attended = classes_present + classes_late
     *     (late students were physically present, so they count toward attendance)
     *
     * Courses with zero recorded sessions are still included (all counts = 0)
     * so the student can see every enrolled course, not just ones with data.
     */
    private static final String SQL_ATTENDANCE_SUMMARY =
        "SELECT e.enrollment_id, " +
        "       c.course_code, " +
        "       c.course_name, " +
        "       co.section, " +
        "       t.full_name                                          AS teacher_name, " +
        "       COUNT(a.attendance_id)                               AS total_classes, " +
        "       SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) AS classes_present, " +
        "       SUM(CASE WHEN a.status = 'late'    THEN 1 ELSE 0 END) AS classes_late, " +
        "       SUM(CASE WHEN a.status = 'absent'  THEN 1 ELSE 0 END) AS classes_absent " +
        "FROM   ENROLLMENTS      e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "LEFT   JOIN ATTENDANCE   a  ON a.enrollment_id = e.enrollment_id " +
        "WHERE  e.student_id  = ? " +
        "  AND  e.status      = 'active' " +
        "  AND  sem.is_active = 1 " +
        "GROUP  BY e.enrollment_id, c.course_code, c.course_name, " +
        "          co.section, t.full_name " +
        "ORDER  BY c.course_code";

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
     * Returns the attendance summary for every active enrolled course
     * this semester, ordered by course code.
     *
     * @param studentId  resolved student_id
     * @return list of StudentAttendanceItem (never null; may be empty)
     */
    public List<StudentAttendanceItem> getAttendanceSummary(int studentId)
            throws SQLException {

        List<StudentAttendanceItem> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ATTENDANCE_SUMMARY)) {

            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int present = rs.getInt("classes_present");
                    int late    = rs.getInt("classes_late");
                    int absent  = rs.getInt("classes_absent");
                    int attended = present + late; // late counts as attended

                    list.add(new StudentAttendanceItem(
                        rs.getInt("enrollment_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("teacher_name"),
                        rs.getInt("total_classes"),
                        attended,
                        absent,
                        late
                    ));
                }
            }
        }
        return list;
    }
}