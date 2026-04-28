package bl;

import dao.AssignTeachersDAO;
import model.OfferingRow;
import model.SemesterOption;
import model.TeacherOption;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for UC-18 — Assign Teachers to Course Offerings.
 *
 * Scope enforcement (mirrors ManageCoursesService):
 *   - init(adminUsername) resolves the admin's major_dept_id once and caches it.
 *   - loadActiveTeachers() is automatically scoped to that faculty — no dept
 *     parameter needed from the Panel, so the UI cannot accidentally bypass scoping.
 *
 * Rules enforced here (none in UI, none in DAO):
 *   - A semester must be selected before loading offerings.
 *   - A teacher must be selected before assigning.
 *   - Removal requires explicit confirmation (caller passes confirmed = true).
 *   - Returns typed ActionResult records so the UI only reacts, never decides.
 */
public class AssignTeachersService {

    // ── Result type ───────────────────────────────────────────────────────────
    public record ActionResult(boolean success, String message) {}

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final AssignTeachersDAO dao;

    /** Cached after init() — scopes teacher loading to the admin's own faculty. */
    private int    majorDeptId = -1;
    private String deptName    = "";

    public AssignTeachersService() {
        this.dao = new AssignTeachersDAO();
    }

    /** Injection constructor for unit tests. */
    public AssignTeachersService(AssignTeachersDAO dao) {
        this.dao = dao;
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Resolves the admin's username to their major_dept_id and caches it.
     * Must be called once before loadActiveTeachers().
     *
     * @param adminUsername  login username of the logged-in admin
     * @return true if the admin's department was found; false otherwise
     */
    public boolean init(String adminUsername) {
        try {
            majorDeptId = dao.getMajorDeptIdForAdmin(adminUsername);
            if (majorDeptId > 0) {
                deptName = dao.getDeptNameById(majorDeptId);
            }
            return majorDeptId > 0;
        } catch (SQLException e) {
            majorDeptId = -1;
            return false;
        }
    }

    /** Returns the admin's faculty name — used by the Panel subtitle. */
    public String getDeptName() { return deptName; }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all semesters for the filter dropdown.
     * Active semester is always first in the list.
     */
    public List<SemesterOption> loadSemesters() {
        try {
            return dao.getAllSemesters();
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Returns all course offerings for a semester with their assigned teacher.
     *
     * @param semesterId  selected semester PK (-1 = none selected)
     */
    public List<OfferingRow> loadOfferings(int semesterId) {
        if (semesterId < 0) return List.of();
        try {
            return dao.getOfferingsBySemester(semesterId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Returns active teachers scoped to the admin's own faculty.
     * Dept is resolved internally from the cached majorDeptId — the Panel
     * does NOT pass a dept parameter, so scoping cannot be bypassed.
     */
    public List<TeacherOption> loadActiveTeachers() {
        ensureInit();
        try {
            return dao.getActiveTeachersByDept(majorDeptId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Assigns (or reassigns) a teacher to a course offering.
     *
     * @param offeringId  target offering
     * @param teacherId   teacher to assign (-1 = nothing selected)
     * @param teacherName display name (for the success message)
     * @param courseCode  display code  (for the success message)
     */
    public ActionResult assignTeacher(int offeringId, int teacherId,
                                      String teacherName, String courseCode) {
        if (offeringId < 0)
            return new ActionResult(false, "No offering selected.");
        if (teacherId < 0)
            return new ActionResult(false, "Please select a teacher.");

        try {
            boolean updated = dao.assignTeacher(offeringId, teacherId);
            if (!updated)
                return new ActionResult(false, "Offering not found in the database.");
        } catch (SQLException e) {
            return new ActionResult(false, "Failed to assign teacher: " + e.getMessage());
        }

        return new ActionResult(true,
            teacherName + " assigned to " + courseCode + " successfully.");
    }

    /**
     * Removes the teacher from a course offering.
     * Caller must pass confirmed = true after showing a confirmation dialog.
     *
     * @param offeringId  target offering
     * @param confirmed   true = admin confirmed the removal
     * @param courseCode  display code (for the success / cancel message)
     */
    public ActionResult removeTeacher(int offeringId, boolean confirmed, String courseCode) {
        if (!confirmed)
            return new ActionResult(false, "Removal cancelled.");
        if (offeringId < 0)
            return new ActionResult(false, "No offering selected.");

        try {
            boolean updated = dao.removeTeacher(offeringId);
            if (!updated)
                return new ActionResult(false, "Offering not found in the database.");
        } catch (SQLException e) {
            return new ActionResult(false, "Failed to remove teacher: " + e.getMessage());
        }

        return new ActionResult(true,
            "Teacher removed from " + courseCode + " successfully.");
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private void ensureInit() {
        if (majorDeptId < 0)
            throw new IllegalStateException(
                "AssignTeachersService.init() must be called before loadActiveTeachers().");
    }
}
