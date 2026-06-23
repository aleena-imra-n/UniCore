package dao;

import model.CourseItem;
import model.EnrolledCourse;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Course Registration feature.
 *
 * Six focused responsibilities:
 *   1. getStudentId          — resolve login username → student_id
 *   2. getAvailableCourses   — offerings visible + registrable for this student
 *   3. getImprovementCourses — completed courses offered again this semester
 *   4. getEnrolledCourses    — current semester's active enrollments
 *   5. isAlreadyEnrolled     — duplicate-enroll guard
 *   6. enrollStudent         — calls SP_RegisterStudent (validates eligibility + capacity)
 */
public class CourseRegistrationDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** Resolve a login username to the student's student_id. */
    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id " +
        "FROM   STUDENTS s " +
        "JOIN   USERS    u ON u.user_id = s.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /**
     * All offerings the student is eligible to register in this active semester.
     * Uses VW_STUDENT_REGISTRABLE_OFFERINGS to enforce:
     *   - Sub-dept eligibility
     *   - Semester gating (recommended_semester <= current_semester)
     *   - Prerequisite pre-filtering (mandatory prereqs must be passed)
     *   - Excludes already-completed courses (those go to improvement tab)
     * Also excludes offerings the student is already enrolled in.
     */
    private static final String SQL_AVAILABLE =
        "SELECT v.offering_id, " +
        "       v.course_code, " +
        "       v.course_name, " +
        "       c.credit_hours, " +
        "       c.recommended_semester, " +
        "       v.section, " +
        "       v.teacher_name, " +
        "       v.schedule_days, " +
        "       CONVERT(VARCHAR(5), v.class_time, 108) AS class_time " +
        "FROM   VW_STUDENT_REGISTRABLE_OFFERINGS v " +
        "JOIN   COURSES c ON c.course_code = v.course_code " +
        "WHERE  v.student_id = ? " +
        "  AND  v.offering_id NOT IN ( " +
        "           SELECT offering_id FROM ENROLLMENTS " +
        "           WHERE  student_id = ? AND status = 'active' " +
        "       ) " +
        "ORDER  BY c.recommended_semester, v.course_code, v.section";

    /**
     * Courses the student has already completed that are offered again
     * in the active semester — available for grade improvement.
     * Uses VW_STUDENT_IMPROVEMENT_COURSES.
     */
    private static final String SQL_IMPROVEMENT =
        "SELECT v.offering_id, " +
        "       v.course_code, " +
        "       v.course_name, " +
        "       c.credit_hours, " +
        "       c.recommended_semester, " +
        "       v.section, " +
        "       v.teacher_name, " +
        "       v.schedule_days, " +
        "       CONVERT(VARCHAR(5), v.class_time, 108) AS class_time " +
        "FROM   VW_STUDENT_IMPROVEMENT_COURSES v " +
        "JOIN   COURSES c ON c.course_code = v.course_code " +
        "WHERE  v.student_id = ? " +
        "ORDER  BY v.course_code, v.section";

    /**
     * All active enrollments for the student in the currently active semester.
     * Shown in the "Enrolled Courses" panel on the right.
     */
    private static final String SQL_ENROLLED =
        "SELECT e.enrollment_id, " +
        "       co.offering_id, " +
        "       c.course_code, " +
        "       c.course_name, " +
        "       c.credit_hours, " +
        "       co.section, " +
        "       t.full_name  AS teacher_name, " +
        "       e.enrollment_date, " +
        "       e.status " +
        "FROM   ENROLLMENTS      e " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = e.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "WHERE  e.student_id = ? " +
        "  AND  e.status     = 'active' " +
        "  AND  sem.is_active = 1 " +
        "ORDER  BY c.course_code";

    /** Quick existence check — used before calling the SP. */
    private static final String SQL_IS_ENROLLED =
        "SELECT COUNT(*) " +
        "FROM   ENROLLMENTS " +
        "WHERE  student_id  = ? " +
        "  AND  offering_id = ? " +
        "  AND  status      = 'active'";

    /**
     * Calls SP_RegisterStudent which enforces:
     *   - Sub-dept eligibility
     *   - Section capacity
     *   - Duplicate enrolment
     * Throws SQLException with the SP's RAISERROR message on any violation.
     */
    private static final String SQL_SP_REGISTER =
        "EXEC SP_RegisterStudent @student_id = ?, @offering_id = ?";

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
     * Returns all offerings the student can still register for this semester.
     * Already-enrolled offerings are excluded automatically.
     * Semester gating and prerequisite pre-filtering are enforced by the view.
     *
     * @param studentId  resolved student_id
     * @return list of CourseItem ready for the dropdown (never null)
     */
    public List<CourseItem> getAvailableCourses(int studentId) throws SQLException {
        List<CourseItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_AVAILABLE)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CourseItem(
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("credit_hours"),
                        rs.getInt("recommended_semester"),
                        rs.getString("section"),
                        rs.getString("teacher_name"),
                        rs.getString("schedule_days"),
                        rs.getString("class_time")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Returns courses the student has already completed that are offered
     * again in the active semester — available for grade improvement.
     *
     * @param studentId  resolved student_id
     * @return list of CourseItem for the improvement tab (never null)
     */
    public List<CourseItem> getImprovementCourses(int studentId) throws SQLException {
        List<CourseItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_IMPROVEMENT)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CourseItem(
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("credit_hours"),
                        rs.getInt("recommended_semester"),
                        rs.getString("section"),
                        rs.getString("teacher_name"),
                        rs.getString("schedule_days"),
                        rs.getString("class_time")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Returns the student's active enrollments in the current semester.
     *
     * @param studentId  resolved student_id
     * @return list of EnrolledCourse for the right-hand panel
     */
    public List<EnrolledCourse> getEnrolledCourses(int studentId) throws SQLException {
        List<EnrolledCourse> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ENROLLED)) {
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

    /**
     * Checks whether the student is already actively enrolled in an offering.
     * Called by the service before invoking the stored procedure.
     *
     * @param studentId   resolved student_id
     * @param offeringId  target course offering
     * @return true if an active enrollment row exists
     */
    public boolean isAlreadyEnrolled(int studentId, int offeringId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_IS_ENROLLED)) {
            ps.setInt(1, studentId);
            ps.setInt(2, offeringId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Registers the student via SP_RegisterStudent.
     * The stored procedure is the single source of truth for:
     *   - Eligibility (sub-dept must match offering)
     *   - Capacity    (enrolled count < max_capacity)
     *   - Duplicates  (already enrolled guard at DB level)
     *
     * On SP-raised error the SQLException message contains the SP's RAISERROR text,
     * which the service layer surfaces to the UI.
     *
     * @param studentId   student to enrol
     * @param offeringId  offering to enrol into
     * @throws SQLException with the SP's error message if any rule is violated
     */
    public void enrollStudent(int studentId, int offeringId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SP_REGISTER)) {
            ps.setInt(1, studentId);
            ps.setInt(2, offeringId);
            ps.execute();
        }
    }
}

