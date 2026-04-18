package dao;

import model.AcademicPerformanceRow;
import model.AttendanceReportRow;
import model.EnrollmentReportRow;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Admin Reports feature.
 * Queries the three views added by UMS_PATCH_SPRINT3.sql.
 *
 * Each method accepts optional filters:
 *   - semesterId  = -1 → all semesters
 *   - majorDeptId = -1 → all departments
 */
public class AdminReportsDAO {

    // ── Semester / dept filter helpers ────────────────────────────────────────

    /** All semesters for the filter dropdown. */
    private static final String SQL_SEMESTERS =
        "SELECT semester_id, semester_name FROM SEMESTERS ORDER BY start_date DESC";

    /** All departments for the filter dropdown. */
    private static final String SQL_DEPTS =
        "SELECT major_dept_id, name FROM MAJOR_DEPARTMENTS ORDER BY name";

    // ── Report 1: Enrollment ──────────────────────────────────────────────────
    private static final String SQL_ENROLLMENT_ALL =
        "SELECT semester_name, dept_name, course_code, course_name, " +
        "       section, teacher_name, total_enrolled, " +
        "       active_count, withdrawn_count, completed_count " +
        "FROM   VW_REPORT_ENROLLMENT " +
        "ORDER  BY semester_name, dept_name, course_code, section";

    private static final String SQL_ENROLLMENT_BY_SEMESTER =
        SQL_ENROLLMENT_ALL.replace(
            "FROM   VW_REPORT_ENROLLMENT",
            "FROM   VW_REPORT_ENROLLMENT " +
            "WHERE  semester_id = ?");

    private static final String SQL_ENROLLMENT_BY_DEPT =
        SQL_ENROLLMENT_ALL.replace(
            "FROM   VW_REPORT_ENROLLMENT",
            "FROM   VW_REPORT_ENROLLMENT v " +
            "JOIN   COURSES c2 ON c2.course_code = v.course_code " +
            "WHERE  c2.major_dept_id = ?");

    // Instead of dynamic SQL generation, use a single flexible query
    private static final String SQL_ENROLLMENT =
        "SELECT semester_name, dept_name, course_code, course_name, " +
        "       section, teacher_name, total_enrolled, " +
        "       active_count, withdrawn_count, completed_count " +
        "FROM   VW_REPORT_ENROLLMENT " +
        "WHERE  (? = -1 OR semester_id = ?) " +
        "  AND  (? = -1 OR dept_name   = " +
        "        (SELECT name FROM MAJOR_DEPARTMENTS WHERE major_dept_id = ?)) " +
        "ORDER  BY semester_name, dept_name, course_code, section";

    // ── Report 2: Academic Performance ───────────────────────────────────────
    private static final String SQL_ACADEMIC =
        "SELECT semester_name, dept_name, course_code, course_name, " +
        "       section, student_count, " +
        "       avg_percentage, min_percentage, max_percentage " +
        "FROM   VW_REPORT_ACADEMIC_PERFORMANCE " +
        "WHERE  (? = -1 OR semester_id = ?) " +
        "  AND  (? = -1 OR dept_name   = " +
        "        (SELECT name FROM MAJOR_DEPARTMENTS WHERE major_dept_id = ?)) " +
        "ORDER  BY semester_name, dept_name, course_code, section";

    // ── Report 3: Attendance ──────────────────────────────────────────────────
    private static final String SQL_ATTENDANCE =
        "SELECT semester_name, dept_name, course_code, course_name, " +
        "       section, teacher_name, total_sessions, " +
        "       student_count, avg_attendance_pct, students_below_75 " +
        "FROM   VW_REPORT_ATTENDANCE " +
        "WHERE  (? = -1 OR semester_id = ?) " +
        "  AND  (? = -1 OR dept_name   = " +
        "        (SELECT name FROM MAJOR_DEPARTMENTS WHERE major_dept_id = ?)) " +
        "ORDER  BY semester_name, dept_name, course_code, section";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all semesters as [semester_id, semester_name] pairs.
     * Used to populate the semester filter dropdown.
     */
    public List<int[]> getSemesters() throws SQLException {
        List<int[]> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SEMESTERS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(new int[]{ rs.getInt("semester_id") });
            // Re-query with name
        }
        // Simpler: return as String pairs
        return list;
    }

    /**
     * Returns semester filter options as String[][2] = {id, name}.
     */
    public List<String[]> getSemesterOptions() throws SQLException {
        List<String[]> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SEMESTERS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(new String[]{
                    String.valueOf(rs.getInt("semester_id")),
                    rs.getString("semester_name")
                });
        }
        return list;
    }

    /**
     * Returns department filter options as String[][2] = {id, name}.
     */
    public List<String[]> getDeptOptions() throws SQLException {
        List<String[]> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DEPTS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(new String[]{
                    String.valueOf(rs.getInt("major_dept_id")),
                    rs.getString("name")
                });
        }
        return list;
    }

    /**
     * Returns the Enrollment Report filtered by semester and/or department.
     *
     * @param semesterId  -1 = all semesters
     * @param majorDeptId -1 = all departments
     */
    public List<EnrollmentReportRow> getEnrollmentReport(int semesterId,
                                                          int majorDeptId)
            throws SQLException {
        List<EnrollmentReportRow> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ENROLLMENT)) {
            ps.setInt(1, semesterId);  ps.setInt(2, semesterId);
            ps.setInt(3, majorDeptId); ps.setInt(4, majorDeptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new EnrollmentReportRow(
                        rs.getString("semester_name"),
                        rs.getString("dept_name"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("teacher_name"),
                        rs.getInt("total_enrolled"),
                        rs.getInt("active_count"),
                        rs.getInt("withdrawn_count"),
                        rs.getInt("completed_count")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Returns the Academic Performance Report filtered by semester and/or department.
     *
     * @param semesterId  -1 = all semesters
     * @param majorDeptId -1 = all departments
     */
    public List<AcademicPerformanceRow> getAcademicReport(int semesterId,
                                                            int majorDeptId)
            throws SQLException {
        List<AcademicPerformanceRow> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ACADEMIC)) {
            ps.setInt(1, semesterId);  ps.setInt(2, semesterId);
            ps.setInt(3, majorDeptId); ps.setInt(4, majorDeptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AcademicPerformanceRow(
                        rs.getString("semester_name"),
                        rs.getString("dept_name"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getInt("student_count"),
                        rs.getDouble("avg_percentage"),
                        rs.getDouble("min_percentage"),
                        rs.getDouble("max_percentage")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Returns the Attendance Summary Report filtered by semester and/or department.
     *
     * @param semesterId  -1 = all semesters
     * @param majorDeptId -1 = all departments
     */
    public List<AttendanceReportRow> getAttendanceReport(int semesterId,
                                                          int majorDeptId)
            throws SQLException {
        List<AttendanceReportRow> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ATTENDANCE)) {
            ps.setInt(1, semesterId);  ps.setInt(2, semesterId);
            ps.setInt(3, majorDeptId); ps.setInt(4, majorDeptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AttendanceReportRow(
                        rs.getString("semester_name"),
                        rs.getString("dept_name"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("teacher_name"),
                        rs.getInt("total_sessions"),
                        rs.getInt("student_count"),
                        rs.getDouble("avg_attendance_pct"),
                        rs.getInt("students_below_75")
                    ));
                }
            }
        }
        return list;
    }
}
