package dao;

import model.SubDeptItem;
import model.UserRecord;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Admin User Management feature (US-3.8a / US-3.8b).
 *
 * Responsibilities:
 *   1. getAdminUserId       — resolve admin username → user_id
 *   2. getAllUsers           — full joined list (students + teachers) for the panel
 *   3. getAllSubDepts        — populate department combo boxes
 *   4. createUser           — calls SP_CreateUser
 *   5. updateUser           — calls SP_UpdateUser
 *   6. deactivateUser       — calls SP_DeactivateUser
 */
public class UserManagementDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_ADMIN_USER_ID =
        "SELECT user_id FROM USERS " +
        "WHERE username = ? AND is_active = 1 AND role = 'admin'";

    /**
     * Returns all student and teacher accounts (active and inactive),
     * joined to their profile and department tables.
     * UNION: student rows first, teacher rows second, sorted by full_name.
     */
    private static final String SQL_ALL_USERS =
        // ── Students ──────────────────────────────────────────────────────────
        "SELECT u.user_id, u.username, u.role, u.email, u.is_active, " +
        "       s.full_name, s.roll_number, s.batch_year, s.current_semester, " +
        "       NULL AS employee_code, NULL AS designation, " +
        "       sd.sub_dept_id, sd.name AS sub_dept_name, sd.code AS sub_dept_code, " +
        "       md.major_dept_id, md.name AS major_dept_name " +
        "FROM   USERS u " +
        "JOIN   STUDENTS          s  ON s.user_id      = u.user_id " +
        "JOIN   SUB_DEPARTMENTS   sd ON sd.sub_dept_id = s.sub_dept_id " +
        "JOIN   MAJOR_DEPARTMENTS md ON md.major_dept_id = sd.major_dept_id " +
        "WHERE  u.role = 'student' " +

        "UNION ALL " +

        // ── Teachers ──────────────────────────────────────────────────────────
        "SELECT u.user_id, u.username, u.role, u.email, u.is_active, " +
        "       t.full_name, NULL AS roll_number, NULL AS batch_year, NULL AS current_semester, " +
        "       t.employee_code, t.designation, " +
        "       sd.sub_dept_id, sd.name AS sub_dept_name, sd.code AS sub_dept_code, " +
        "       md.major_dept_id, md.name AS major_dept_name " +
        "FROM   USERS u " +
        "JOIN   TEACHERS          t  ON t.user_id      = u.user_id " +
        "JOIN   SUB_DEPARTMENTS   sd ON sd.sub_dept_id = t.sub_dept_id " +
        "JOIN   MAJOR_DEPARTMENTS md ON md.major_dept_id = sd.major_dept_id " +
        "WHERE  u.role = 'teacher' " +

        "ORDER  BY full_name ASC";

    private static final String SQL_ALL_SUB_DEPTS =
        "SELECT sd.sub_dept_id, sd.major_dept_id, sd.name, sd.code, md.name AS major_dept_name " +
        "FROM   SUB_DEPARTMENTS   sd " +
        "JOIN   MAJOR_DEPARTMENTS md ON md.major_dept_id = sd.major_dept_id " +
        "ORDER  BY md.name, sd.name";

    private static final String SQL_CREATE =
        "EXEC SP_CreateUser " +
        "  @role = ?, @full_name = ?, @username = ?, @password = ?, @email = ?, " +
        "  @sub_dept_id = ?, @major_dept_id = ?, " +
        "  @roll_number = ?, @batch_year = ?, " +
        "  @emp_code = ?, @designation = ?";

    private static final String SQL_UPDATE =
        "EXEC SP_UpdateUser " +
        "  @user_id = ?, @full_name = ?, @email = ?, @new_password = ?, " +
        "  @sub_dept_id = ?, @designation = ?, " +
        "  @roll_number = ?, @batch_year = ?, @current_semester = ?";

    private static final String SQL_DEACTIVATE =
        "EXEC SP_DeactivateUser @user_id = ?, @requesting_admin_user_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    public int getAdminUserId(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ADMIN_USER_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }

    /** Returns the complete user list (students + teachers, all statuses). */
    public List<UserRecord> getAllUsers() throws SQLException {
        List<UserRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ALL_USERS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Integer batchYear = rs.getObject("batch_year") != null
                    ? rs.getInt("batch_year") : null;
                Integer curSem = rs.getObject("current_semester") != null
                    ? rs.getInt("current_semester") : null;
                list.add(new UserRecord(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getString("email"),
                    rs.getBoolean("is_active"),
                    rs.getString("full_name"),
                    rs.getString("roll_number"),
                    batchYear,
                    curSem,
                    rs.getString("employee_code"),
                    rs.getString("designation"),
                    rs.getInt("sub_dept_id"),
                    rs.getString("sub_dept_name"),
                    rs.getString("sub_dept_code"),
                    rs.getInt("major_dept_id"),
                    rs.getString("major_dept_name")
                ));
            }
        }
        return list;
    }

    /** Returns all sub-departments for the role/dept combo boxes. */
    public List<SubDeptItem> getAllSubDepts() throws SQLException {
        List<SubDeptItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ALL_SUB_DEPTS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new SubDeptItem(
                    rs.getInt("sub_dept_id"),
                    rs.getInt("major_dept_id"),
                    rs.getString("name"),
                    rs.getString("code"),
                    rs.getString("major_dept_name")
                ));
            }
        }
        return list;
    }

    /**
     * Calls SP_CreateUser. Returns the new user_id on success.
     * Throws SQLException with the SP's RAISERROR text on any violation.
     *
     * Pass null for fields not relevant to the role.
     */
    public int createUser(String role, String fullName, String username,
                          String password, String email,
                          Integer subDeptId, Integer majorDeptId,
                          String rollNumber, Integer batchYear,
                          String empCode, String designation) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_CREATE)) {
            ps.setString(1, role);
            ps.setString(2, fullName);
            ps.setString(3, username);
            ps.setString(4, password);
            ps.setString(5, email);
            setNullableInt(ps, 6, subDeptId);
            setNullableInt(ps, 7, majorDeptId);
            ps.setString(8, rollNumber);
            setNullableInt(ps, 9, batchYear);
            ps.setString(10, empCode);
            ps.setString(11, designation);

            boolean hasResult = ps.execute();
            if (hasResult) {
                try (ResultSet rs = ps.getResultSet()) {
                    if (rs.next()) return rs.getInt("new_user_id");
                }
            }
            return -1;
        }
    }

    /**
     * Calls SP_UpdateUser. Pass null for fields that should not change.
     * newPassword = null means password is left unchanged.
     */
    public void updateUser(int userId, String fullName, String email,
                           String newPassword, Integer subDeptId,
                           String designation,
                           String rollNumber, Integer batchYear,
                           Integer currentSemester) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, userId);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, newPassword);          // null = keep existing
            setNullableInt(ps, 5, subDeptId);
            ps.setString(6, designation);
            ps.setString(7, rollNumber);
            setNullableInt(ps, 8, batchYear);
            setNullableInt(ps, 9, currentSemester);
            ps.execute();
        }
    }

    /** Calls SP_DeactivateUser. */
    public void deactivateUser(int userId, int adminUserId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DEACTIVATE)) {
            ps.setInt(1, userId);
            ps.setInt(2, adminUserId);
            ps.execute();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private void setNullableInt(PreparedStatement ps, int idx, Integer val)
            throws SQLException {
        if (val == null) ps.setNull(idx, Types.INTEGER);
        else             ps.setInt(idx, val);
    }
}