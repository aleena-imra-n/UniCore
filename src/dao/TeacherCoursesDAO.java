package dao;

import model.CourseStudent;
import model.TeacherCourse;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Teacher Courses feature.
 *
 * Three focused responsibilities:
 *   1. getTeacherId        — resolve login username → teacher_id
 *   2. getCoursesForTeacher — all offerings + live enrolment counts
 *   3. getStudentsForOffering — roster for "View Students" popup
 */
public class TeacherCoursesDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** Resolve login username → teacher_id. */
    private static final String SQL_TEACHER_ID =
        "SELECT t.teacher_id " +
        "FROM   TEACHERS t " +
        "JOIN   USERS    u ON u.user_id = t.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /**
     * All active offerings for this teacher in the current semester, with a
     * live count of active enrolments computed inline via a subquery.
     */
    private static final String SQL_COURSES =
        "SELECT co.offering_id, " +
        "       c.course_code, " +
        "       c.course_name, " +
        "       co.section, " +
        "       co.schedule_days, " +
        "       CONVERT(VARCHAR(5), co.class_time, 108) AS class_time, " +
        "       co.room_number, " +
        "       co.max_capacity, " +
        "       ( SELECT COUNT(*) " +
        "         FROM   ENROLLMENTS e2 " +
        "         WHERE  e2.offering_id = co.offering_id " +
        "           AND  e2.status = 'active' " +
        "       ) AS enrolled_count " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   COURSES           c   ON c.course_id    = co.course_id " +
        "JOIN   SEMESTERS         sem ON sem.semester_id = co.semester_id " +
        "WHERE  co.teacher_id  = ? " +
        "  AND  sem.is_active  = 1 " +
        "ORDER  BY c.course_code, co.section";

    /**
     * Full student roster for one course offering, including each student's
     * running attendance percentage (0 if no sessions recorded yet).
     */
    private static final String SQL_STUDENTS =
        "SELECT s.student_id, " +
        "       e.enrollment_id, " +
        "       s.full_name, " +
        "       s.roll_number, " +
        "       sd.code  AS sub_dept_code, " +
        "       e.status AS enrollment_status, " +
        "       ISNULL(att.pct, 0) AS attendance_pct " +
        "FROM   ENROLLMENTS     e " +
        "JOIN   STUDENTS         s  ON s.student_id   = e.student_id " +
        "JOIN   SUB_DEPARTMENTS  sd ON sd.sub_dept_id = s.sub_dept_id " +
        "OUTER APPLY ( " +
        "    SELECT CAST( " +
        "        100.0 * SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) " +
        "        / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) AS pct " +
        "    FROM ATTENDANCE a " +
        "    WHERE a.enrollment_id = e.enrollment_id " +
        ") att " +
        "WHERE  e.offering_id = ? " +
        "  AND  e.status      = 'active' " +
        "ORDER  BY s.roll_number";

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
     * Returns all course offerings assigned to the teacher this semester,
     * each carrying a live enrolled-student count.
     *
     * @param teacherId  resolved teacher_id
     * @return list of TeacherCourse (never null; may be empty)
     */
    public List<TeacherCourse> getCoursesForTeacher(int teacherId) throws SQLException {
        List<TeacherCourse> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_COURSES)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TeacherCourse(
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("schedule_days"),
                        rs.getString("class_time"),
                        rs.getString("room_number"),
                        rs.getInt("max_capacity"),
                        rs.getInt("enrolled_count")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Returns the active student roster for a specific course offering,
     * including each student's running attendance percentage.
     *
     * @param offeringId  the course offering to query
     * @return list of CourseStudent ordered by roll number (never null)
     */
    public List<CourseStudent> getStudentsForOffering(int offeringId) throws SQLException {
        List<CourseStudent> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_STUDENTS)) {
            ps.setInt(1, offeringId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CourseStudent(
                        rs.getInt("student_id"),
                        rs.getInt("enrollment_id"),
                        rs.getString("full_name"),
                        rs.getString("roll_number"),
                        rs.getString("sub_dept_code"),
                        rs.getInt("attendance_pct"),
                        rs.getString("enrollment_status")
                    ));
                }
            }
        }
        return list;
    }
}
