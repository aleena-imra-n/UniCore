package dao;

import model.CategoryOption;
import model.CourseRow;
import model.DeptOption;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Manage Course Catalog feature (UC-17 / US-3.9a).
 *
 * Responsibilities:
 *   1. getCoursesByDept     — catalog scoped to one faculty (admin scope enforcement)
 *   2. getMajorDeptIdForAdmin — resolve admin username → their major_dept_id
 *   3. getAllDepts          — major departments for the dropdown
 *   4. getAllCategories     — course categories for the dropdown
 *   5. existsByCode        — duplicate-code guard before insert
 *   6. addCourse           — INSERT new course row
 *   7. updateCourse        — UPDATE name, credit_hours, dept, category
 *   8. setActiveStatus     — toggle is_active (deactivate / reactivate)
 */
public class ManageCoursesDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /**
     * Load every course belonging to a specific major department,
     * with dept + category names joined in, plus a comma-separated list
     * of prerequisite course codes aggregated in one pass.
     * NULL prereq_codes = no prerequisites.
     *
     * @param majorDeptId  the faculty to scope the result to
     */
    private static final String SQL_COURSES_BY_DEPT =
        "SELECT c.course_id, c.major_dept_id, c.category_id, " +
        "       c.course_code, c.course_name, c.credit_hours, " +
        "       c.recommended_semester, " +
        "       c.is_active, " +
        "       md.name  AS dept_name, " +
        "       cc.name  AS category_name, " +
        "       STUFF(( " +
        "           SELECT ', ' + pc.course_code " +
        "           FROM   COURSE_PREREQUISITES cp2 " +
        "           JOIN   COURSES pc ON pc.course_id = cp2.prereq_course_id " +
        "           WHERE  cp2.course_id = c.course_id " +
        "           FOR XML PATH(''), TYPE).value('.','NVARCHAR(MAX)'), 1, 2, '') " +
        "           AS prereq_codes " +
        "FROM   COURSES c " +
        "JOIN   MAJOR_DEPARTMENTS md ON md.major_dept_id = c.major_dept_id " +
        "JOIN   COURSE_CATEGORIES cc ON cc.category_id   = c.category_id " +
        "WHERE  c.major_dept_id = ? " +
        "ORDER  BY c.recommended_semester, c.course_code";

    /**
     * Resolves an admin's login username to their major_dept_id.
     * Returns -1 (no rows) if the admin has no department assigned
     * (which should not happen in a correctly seeded DB).
     */
    private static final String SQL_DEPT_FOR_ADMIN =
        "SELECT a.major_dept_id " +
        "FROM   ADMINS a " +
        "JOIN   USERS  u ON u.user_id = a.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /** Only departments the admin may assign courses to (their own faculty). */
    private static final String SQL_DEPT_BY_ID =
        "SELECT major_dept_id, name, code " +
        "FROM   MAJOR_DEPARTMENTS " +
        "WHERE  major_dept_id = ?";

    /** All active course categories for the category dropdown. */
    private static final String SQL_CATEGORIES =
        "SELECT category_id, name " +
        "FROM   COURSE_CATEGORIES " +
        "WHERE  is_active = 1 " +
        "ORDER  BY name";

    /** Check whether a course code already exists (case-insensitive). */
    private static final String SQL_EXISTS_CODE =
        "SELECT COUNT(*) FROM COURSES WHERE UPPER(course_code) = UPPER(?)";

    /** Insert a brand-new course. */
    private static final String SQL_INSERT =
        "INSERT INTO COURSES " +
        "(major_dept_id, category_id, course_code, course_name, credit_hours, recommended_semester) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    /** Update the editable fields of an existing course. */
    private static final String SQL_UPDATE =
        "UPDATE COURSES " +
        "SET    course_name          = ?, " +
        "       credit_hours         = ?, " +
        "       major_dept_id        = ?, " +
        "       category_id          = ?, " +
        "       recommended_semester = ? " +
        "WHERE  course_id = ?";

    /** Toggle is_active. */
    private static final String SQL_SET_ACTIVE =
        "UPDATE COURSES SET is_active = ? WHERE course_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves an admin's username to their assigned major_dept_id.
     * Called once by the service to scope everything else.
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
     * Returns courses scoped to the admin's own faculty, with each course's
     * prerequisite codes aggregated into a single comma-separated string.
     *
     * @param majorDeptId  the faculty to scope the result to
     */
    public List<CourseRow> getCoursesByDept(int majorDeptId) throws SQLException {
        List<CourseRow> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_COURSES_BY_DEPT)) {
            ps.setInt(1, majorDeptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CourseRow(
                        rs.getInt("course_id"),
                        rs.getInt("major_dept_id"),
                        rs.getInt("category_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("credit_hours"),
                        rs.getInt("recommended_semester"),
                        rs.getString("dept_name"),
                        rs.getString("category_name"),
                        rs.getString("prereq_codes"),   // null when no prereqs
                        rs.getBoolean("is_active")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Returns only the admin's own department for the department dropdown.
     * Admins cannot move a course to a different faculty.
     *
     * @param majorDeptId  the admin's faculty
     */
    public List<DeptOption> getDeptForAdmin(int majorDeptId) throws SQLException {
        List<DeptOption> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DEPT_BY_ID)) {
            ps.setInt(1, majorDeptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    list.add(new DeptOption(
                        rs.getInt("major_dept_id"),
                        rs.getString("name"),
                        rs.getString("code")
                    ));
                }
            }
        }
        return list;
    }

    /** Returns all active course categories for the Add / Edit category dropdown. */
    public List<CategoryOption> getAllCategories() throws SQLException {
        List<CategoryOption> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_CATEGORIES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CategoryOption(
                    rs.getInt("category_id"),
                    rs.getString("name")
                ));
            }
        }
        return list;
    }

    /**
     * Checks whether a course code already exists in the catalog.
     *
     * @param courseCode  code to check (case-insensitive)
     * @return true if a matching row already exists
     */
    public boolean existsByCode(String courseCode) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_EXISTS_CODE)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserts a new course into the catalog.
     *
     * @return generated course_id, or -1 on failure
     */
    public int addCourse(int majorDeptId, int categoryId, String courseCode,
            String courseName, int creditHours, int recommendedSemester,
            String prereqCode) throws SQLException {

    	Connection con = null;
    	PreparedStatement psCourse = null;
    	PreparedStatement psPrereq = null;

    	try {
    		con = DBConnection.getConnection();
    		con.setAutoCommit(false); // 🔴 important for transaction

    		// ── STEP 1: Insert Course ─────────────────────────
    		psCourse = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);

    		psCourse.setInt(1, majorDeptId);
    		psCourse.setInt(2, categoryId);
    		psCourse.setString(3, courseCode.trim().toUpperCase());
    		psCourse.setString(4, courseName.trim());
    		psCourse.setInt(5, creditHours);
    		psCourse.setInt(6, recommendedSemester);

    		psCourse.executeUpdate();

    		int courseId = -1;
    		try (ResultSet keys = psCourse.getGeneratedKeys()) {
    			if (keys.next()) {
    				courseId = keys.getInt(1);
    			}
    		}

    		// ── STEP 2: Insert Prerequisite (if provided) ─────
    		if (prereqCode != null && !prereqCode.trim().isEmpty()) {

    			// Get prereq course_id from code
    			String getIdSQL = "SELECT course_id FROM COURSES WHERE course_code = ?";
    			try (PreparedStatement psGetId = con.prepareStatement(getIdSQL)) {

    				psGetId.setString(1, prereqCode.trim().toUpperCase());

    				try (ResultSet rs = psGetId.executeQuery()) {
    					if (rs.next()) {
    						int prereqId = rs.getInt("course_id");

    						String insertPrereqSQL =
    								"INSERT INTO COURSE_PREREQUISITES (course_id, prereq_course_id) VALUES (?, ?)";

    						psPrereq = con.prepareStatement(insertPrereqSQL);
    						psPrereq.setInt(1, courseId);
    						psPrereq.setInt(2, prereqId);
    						psPrereq.executeUpdate();
    					}
    				}	
    			}
    		}

    		con.commit(); // ✅ success
    		return courseId;

    	} catch (Exception e) {
    		if (con != null) con.rollback(); // ❌ rollback on error
    		throw e;
    	} finally {
    		if (psCourse != null) psCourse.close();
    		if (psPrereq != null) psPrereq.close();
    		if (con != null) con.setAutoCommit(true);
    		if (con != null) con.close();
    	}
    }

    /**
     * Updates the editable fields of an existing course.
     * course_code is intentionally NOT editable after creation.
     *
     * @return true if a row was updated
     */
    public boolean updateCourse(int courseId, String courseName,
            int creditHours, int majorDeptId,
            int categoryId, int recommendedSemester,
            String prereqCode) throws SQLException {

    	Connection con = null;
    	PreparedStatement psUpdate = null;
    	PreparedStatement psDelete = null;
    	PreparedStatement psInsert = null;

    	try {
    		con = DBConnection.getConnection();
    		con.setAutoCommit(false); // 🔴 transaction start

    		// ── STEP 1: Update main course ───────────────────
    		psUpdate = con.prepareStatement(SQL_UPDATE);

    		psUpdate.setString(1, courseName.trim());
    		psUpdate.setInt(2, creditHours);
    		psUpdate.setInt(3, majorDeptId);
    		psUpdate.setInt(4, categoryId);
    		psUpdate.setInt(5, recommendedSemester);
    		psUpdate.setInt(6, courseId);

    		psUpdate.executeUpdate();

    		// ── STEP 2: Remove old prerequisites ─────────────
    		String deleteSQL = "DELETE FROM COURSE_PREREQUISITES WHERE course_id = ?";
    		psDelete = con.prepareStatement(deleteSQL);
    		psDelete.setInt(1, courseId);
    		psDelete.executeUpdate();
    		
    		// ── STEP 3: Insert new prerequisite (if any) ─────
    		if (prereqCode != null && !prereqCode.trim().isEmpty()) {

    			// Get prereq course_id
    			String getIdSQL = "SELECT course_id FROM COURSES WHERE course_code = ?";
    			try (PreparedStatement psGetId = con.prepareStatement(getIdSQL)) {

    				psGetId.setString(1, prereqCode.trim().toUpperCase());

    				try (ResultSet rs = psGetId.executeQuery()) {
    					if (rs.next()) {
    						int prereqId = rs.getInt("course_id");

    						String insertSQL =
    								"INSERT INTO COURSE_PREREQUISITES (course_id, prereq_course_id) VALUES (?, ?)";

    						psInsert = con.prepareStatement(insertSQL);
    						psInsert.setInt(1, courseId);
    						psInsert.setInt(2, prereqId);
    						psInsert.executeUpdate();
    					}
    				}
    			}
    		}

    		con.commit(); // ✅ success
    		return true;

    	} catch (Exception e) {
    		if (con != null) con.rollback(); // ❌ rollback
    		throw e;
    	} finally {
    		if (psUpdate != null) psUpdate.close();
    		if (psDelete != null) psDelete.close();
    		if (psInsert != null) psInsert.close();	
    		if (con != null) con.setAutoCommit(true);
    		if (con != null) con.close();
    	}
    }

    /**
     * Sets the is_active flag on a course.
     *
     * @param courseId target course
     * @param active   new active status
     * @return true if a row was updated
     */
    public boolean setActiveStatus(int courseId, boolean active) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SET_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setInt(2, courseId);
            return ps.executeUpdate() > 0;
        }
    }
}


