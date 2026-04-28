package dao;

import model.TimetableEntry;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the My Timetable view (US-3.7b).
 * Shared by both the teacher and student timetable panels.
 *
 * Three responsibilities:
 *   1. getTeacherIdByUsername  — resolve login username → teacher_id
 *   2. getStudentIdByUsername  — resolve login username → student_id
 *   3. getTimetableForTeacher  — all TIMETABLES slots for this teacher's
 *                                active-semester offerings
 *   4. getTimetableForStudent  — all TIMETABLES slots for courses the
 *                                student is actively enrolled in this semester
 */
public class MyTimetableDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_TEACHER_ID =
        "SELECT t.teacher_id " +
        "FROM   TEACHERS t JOIN USERS u ON u.user_id = t.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id " +
        "FROM   STUDENTS s JOIN USERS u ON u.user_id = s.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /**
     * All timetable slots for a teacher's active-semester offerings.
     * Ordered by day-of-week index (Mon=1…Fri=5) then start_time.
     */
    private static final String SQL_TEACHER_SLOTS =
        "SELECT tt.timetable_id, tt.offering_id, " +
        "       CASE tt.day_of_week " +
        "           WHEN 'Monday'    THEN 1 WHEN 'Tuesday'   THEN 2 " +
        "           WHEN 'Wednesday' THEN 3 WHEN 'Thursday'  THEN 4 " +
        "           WHEN 'Friday'    THEN 5 ELSE 6 END         AS day_order, " +
        "       tt.day_of_week, " +
        "       CONVERT(VARCHAR(5), tt.start_time, 108)        AS start_time, " +
        "       CONVERT(VARCHAR(5), tt.end_time,   108)        AS end_time, " +
        "       tt.room_number, " +
        "       c.course_code, c.course_name, co.section, " +
        "       t.full_name    AS teacher_name, " +
        "       sd.code        AS sub_dept_code " +
        "FROM   TIMETABLES       tt " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = tt.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   SUB_DEPARTMENTS  sd  ON sd.sub_dept_id  = co.sub_dept_id " +
        "WHERE  co.teacher_id  = ? " +
        "  AND  sem.is_active  = 1 " +
        "ORDER  BY day_order, tt.start_time";

    /**
     * All timetable slots for courses a student is actively enrolled in
     * this semester.
     * Ordered by day-of-week index then start_time.
     */
    private static final String SQL_STUDENT_SLOTS =
        "SELECT tt.timetable_id, tt.offering_id, " +
        "       CASE tt.day_of_week " +
        "           WHEN 'Monday'    THEN 1 WHEN 'Tuesday'   THEN 2 " +
        "           WHEN 'Wednesday' THEN 3 WHEN 'Thursday'  THEN 4 " +
        "           WHEN 'Friday'    THEN 5 ELSE 6 END         AS day_order, " +
        "       tt.day_of_week, " +
        "       CONVERT(VARCHAR(5), tt.start_time, 108)        AS start_time, " +
        "       CONVERT(VARCHAR(5), tt.end_time,   108)        AS end_time, " +
        "       tt.room_number, " +
        "       c.course_code, c.course_name, co.section, " +
        "       t.full_name    AS teacher_name, " +
        "       sd.code        AS sub_dept_code " +
        "FROM   ENROLLMENTS      e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   TIMETABLES       tt  ON tt.offering_id  = e.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   SUB_DEPARTMENTS  sd  ON sd.sub_dept_id  = co.sub_dept_id " +
        "WHERE  e.student_id  = ? " +
        "  AND  e.status      = 'active' " +
        "  AND  sem.is_active = 1 " +
        "ORDER  BY day_order, tt.start_time";

    // ── Public API ────────────────────────────────────────────────────────────

    public int getTeacherIdByUsername(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_TEACHER_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("teacher_id") : -1;
            }
        }
    }

    public int getStudentIdByUsername(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_STUDENT_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("student_id") : -1;
            }
        }
    }

    /**
     * Returns all timetable slots for a teacher's active-semester offerings,
     * ordered Monday → Friday, then by start_time.
     */
    public List<TimetableEntry> getTimetableForTeacher(int teacherId)
            throws SQLException {
        return querySlots(SQL_TEACHER_SLOTS, teacherId);
    }

    /**
     * Returns all timetable slots for a student's active enrollments
     * this semester, ordered Monday → Friday, then by start_time.
     */
    public List<TimetableEntry> getTimetableForStudent(int studentId)
            throws SQLException {
        return querySlots(SQL_STUDENT_SLOTS, studentId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<TimetableEntry> querySlots(String sql, int id)
            throws SQLException {
        List<TimetableEntry> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TimetableEntry(
                        rs.getInt("timetable_id"),
                        rs.getInt("offering_id"),
                        rs.getString("day_of_week"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getString("room_number"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("teacher_name"),
                        rs.getString("sub_dept_code")
                    ));
                }
            }
        }
        return list;
    }
}