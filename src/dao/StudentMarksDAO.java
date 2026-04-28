package dao;

import model.MarksItem;
import util.DBConnection;

import java.sql.*;
import java.util.*;

/**
 * DAO for the Student Marks View feature.
 *
 * Responsibilities:
 *   1. getStudentId          — resolve username → student_id
 *   2. getMarksForStudent    — all marks grouped by enrollment/course
 *   3. getCourseInfo         — credit hours + teacher for each enrollment
 */
public class StudentMarksDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id " +
        "FROM   STUDENTS s " +
        "JOIN   USERS    u ON u.user_id = s.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /**
     * All marks for a student in the active semester,
     * with course + enrollment info joined in.
     */
    private static final String SQL_MARKS =
        "SELECT e.enrollment_id, " +
        "       c.course_code, " +
        "       c.course_name, " +
        "       c.credit_hours, " +
        "       co.section, " +
        "       t.full_name      AS teacher_name, " +
        "       m.mark_id, " +
        "       m.assessment_type, " +
        "       m.marks_obtained, " +
        "       m.total_marks, " +
        "       m.remarks " +
        "FROM   ENROLLMENTS      e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "JOIN   MARKS             m  ON m.enrollment_id = e.enrollment_id " +
        "WHERE  e.student_id  = ? " +
        "  AND  e.status      = 'active' " +
        "  AND  sem.is_active = 1 " +
        "ORDER  BY c.course_code, m.assessment_type";

    /**
     * All active enrollments for the student this semester,
     * even those with no marks yet.
     */
    private static final String SQL_ENROLLMENTS =
        "SELECT e.enrollment_id, " +
        "       c.course_code, " +
        "       c.course_name, " +
        "       c.credit_hours, " +
        "       co.section, " +
        "       t.full_name AS teacher_name " +
        "FROM   ENROLLMENTS      e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "WHERE  e.student_id  = ? " +
        "  AND  e.status      = 'active' " +
        "  AND  sem.is_active = 1 " +
        "ORDER  BY c.course_code";

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

    /**
     * Returns all marks for the student this semester,
     * grouped as: enrollmentId → list of MarksItem.
     */
    public Map<Integer, List<MarksItem>> getMarksGrouped(int studentId)
            throws SQLException {
        Map<Integer, List<MarksItem>> map = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_MARKS)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int eid = rs.getInt("enrollment_id");
                    map.computeIfAbsent(eid, k -> new ArrayList<>()).add(
                        new MarksItem(
                            rs.getInt("mark_id"),
                            eid,
                            "",   // student name not needed here
                            "",
                            rs.getString("assessment_type"),
                            rs.getDouble("marks_obtained"),
                            rs.getDouble("total_marks"),
                            rs.getString("remarks")
                        )
                    );
                }
            }
        }
        return map;
    }

    /**
     * Returns all active enrollments (with course + teacher info)
     * for the student this semester, including those with no marks yet.
     * Result: enrollmentId → String[]{courseCode, courseName, creditHours, section, teacherName}
     */
    public Map<Integer, String[]> getEnrollmentInfo(int studentId)
            throws SQLException {
        Map<Integer, String[]> map = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ENROLLMENTS)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int eid = rs.getInt("enrollment_id");
                    map.put(eid, new String[]{
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        String.valueOf(rs.getInt("credit_hours")),
                        rs.getString("section"),
                        rs.getString("teacher_name")
                    });
                }
            }
        }
        return map;
    }
}