package dao;

import model.OfferingRow;
import model.SemesterOption;
import model.TeacherOption;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for UC-18 — Assign Teachers to Course Offerings.
 *
 * Responsibilities:
 *   1. getMajorDeptIdForAdmin  — resolve admin username → their major_dept_id
 *   2. getDeptNameById         — resolve major_dept_id → faculty display name
 *   3. getAllSemesters          — all semesters for the semester filter dropdown
 *   4. getOfferingsBySemester  — all offerings for a given semester with current teacher
 *   5. getActiveTeachersByDept — active teachers scoped to one faculty
 *   6. assignTeacher           — UPDATE COURSE_OFFERINGS SET teacher_id = ?
 *   7. removeTeacher           — SET teacher_id = NULL WHERE offering_id = ?
 */
public class AssignTeachersDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** Resolves an admin's login username to their major_dept_id. */
    private static final String SQL_DEPT_FOR_ADMIN =
        "SELECT a.major_dept_id " +
        "FROM   ADMINS a " +
        "JOIN   USERS  u ON u.user_id = a.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /** Resolves a major_dept_id to its display name. */
    private static final String SQL_DEPT_NAME_BY_ID =
        "SELECT name " +
        "FROM   MAJOR_DEPARTMENTS " +
        "WHERE  major_dept_id = ?";

    /** All semesters ordered newest-first, active semester always on top. */
    private static final String SQL_SEMESTERS =
        "SELECT semester_id, semester_name, is_active " +
        "FROM   SEMESTERS " +
        "ORDER  BY is_active DESC, semester_id DESC";

    /**
     * All course offerings for a given semester,
     * left-joined with teacher so unassigned rows still appear.
     */
    private static final String SQL_OFFERINGS =
        "SELECT co.offering_id, " +
        "       c.course_code, " +
        "       c.course_name, " +
        "       co.section, " +
        "       md.name   AS dept_name, " +
        "       c.credit_hours, " +
        "       co.teacher_id, " +
        "       t.full_name AS teacher_name " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   COURSES           c   ON c.course_id     = co.course_id " +
        "JOIN   MAJOR_DEPARTMENTS md  ON md.major_dept_id = c.major_dept_id " +
        "LEFT JOIN TEACHERS       t   ON t.teacher_id    = co.teacher_id " +
        "WHERE  co.semester_id = ? " +
        "ORDER  BY c.course_code, co.section";

    /**
     * Active teachers scoped to one faculty only.
     *
     * TEACHERS has no direct major_dept_id column — the link is:
     *   TEACHERS.sub_dept_id → SUB_DEPARTMENTS.major_dept_id
     * We must join through SUB_DEPARTMENTS to filter by faculty.
     */
    private static final String SQL_ACTIVE_TEACHERS_BY_DEPT =
        "SELECT t.teacher_id, t.full_name, t.employee_code " +
        "FROM   TEACHERS        t " +
        "JOIN   USERS           u  ON u.user_id      = t.user_id " +
        "JOIN   SUB_DEPARTMENTS sd ON sd.sub_dept_id  = t.sub_dept_id " +
        "WHERE  u.is_active       = 1 " +
        "AND    sd.major_dept_id  = ? " +
        "ORDER  BY t.full_name";

    /** Assign (or reassign) a teacher to an offering. */
    private static final String SQL_ASSIGN =
        "UPDATE COURSE_OFFERINGS " +
        "SET    teacher_id = ? " +
        "WHERE  offering_id = ?";

    /** Remove the teacher from an offering (set NULL). */
    private static final String SQL_REMOVE =
        "UPDATE COURSE_OFFERINGS " +
        "SET    teacher_id = NULL " +
        "WHERE  offering_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves an admin's login username to their assigned major_dept_id.
     * Called once by the service's init() to scope all teacher loading.
     *
     * @param adminUsername  login username
     * @return major_dept_id, or -1 if not found
     */
    public int getMajorDeptIdForAdmin(String adminUsername) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DEPT_FOR_ADMIN)) {
            ps.setString(1, adminUsername);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("major_dept_id") : -1;
            }
        }
    }

    /**
     * Returns the display name of a faculty given its PK.
     * Used by the service's init() to populate the Panel subtitle.
     *
     * @param majorDeptId  the faculty PK
     * @return faculty name, or empty string if not found
     */
    public String getDeptNameById(int majorDeptId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DEPT_NAME_BY_ID)) {
            ps.setInt(1, majorDeptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : "";
            }
        }
    }

    /**
     * Returns all semesters for the filter dropdown.
     * Active semester appears first.
     */
    public List<SemesterOption> getAllSemesters() throws SQLException {
        List<SemesterOption> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SEMESTERS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                boolean active = rs.getBoolean("is_active");
                String label   = rs.getString("semester_name")
                                 + (active ? "  ✦ active" : "");
                list.add(new SemesterOption(
                    rs.getInt("semester_id"),
                    label,
                    active
                ));
            }
        }
        return list;
    }

    /**
     * Returns all course offerings for the given semester with their
     * currently assigned teacher (or null if unassigned).
     *
     * @param semesterId  target semester PK
     */
    public List<OfferingRow> getOfferingsBySemester(int semesterId) throws SQLException {
        List<OfferingRow> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_OFFERINGS)) {
            ps.setInt(1, semesterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int teacherIdRaw = rs.getInt("teacher_id");
                    Integer teacherId = rs.wasNull() ? null : teacherIdRaw;
                    list.add(new OfferingRow(
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("dept_name"),
                        rs.getInt("credit_hours"),
                        teacherId,
                        rs.getString("teacher_name")   // may be null
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Returns active teachers belonging to the specified faculty only.
     * Called by the service after resolving the admin's own majorDeptId,
     * so a Computing admin only sees Computing teachers, Engineering only
     * Engineering teachers, and so on.
     *
     * @param majorDeptId  the admin's faculty PK (from ADMINS.major_dept_id)
     */
    public List<TeacherOption> getActiveTeachersByDept(int majorDeptId) throws SQLException {
        List<TeacherOption> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ACTIVE_TEACHERS_BY_DEPT)) {
            ps.setInt(1, majorDeptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TeacherOption(
                        rs.getInt("teacher_id"),
                        rs.getString("full_name"),
                        rs.getString("employee_code")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Assigns (or reassigns) a teacher to a course offering.
     *
     * @param offeringId  target offering PK
     * @param teacherId   teacher to assign
     * @return true if a row was updated
     */
    public boolean assignTeacher(int offeringId, int teacherId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ASSIGN)) {
            ps.setInt(1, teacherId);
            ps.setInt(2, offeringId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Removes the teacher assignment from a course offering (sets teacher_id = NULL).
     *
     * @param offeringId  target offering PK
     * @return true if a row was updated
     */
    public boolean removeTeacher(int offeringId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_REMOVE)) {
            ps.setInt(1, offeringId);
            return ps.executeUpdate() > 0;
        }
    }
}
