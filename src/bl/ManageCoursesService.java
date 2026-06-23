package bl;

import dao.ManageCoursesDAO;
import model.CategoryOption;
import model.CourseRow;
import model.DeptOption;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for UC-17 Manage Course Catalog.
 *
 * Scope enforcement (new):
 *   - init(adminUsername) resolves the admin's major_dept_id once and caches it.
 *   - All course loads and adds are automatically scoped to that faculty.
 *   - An admin can only see / add / edit / deactivate courses in their own faculty.
 *
 * Validation rules:
 *   - course_code must not be blank and not duplicate existing codes
 *   - course_name must not be blank
 *   - credit_hours must be between 1 and 6
 *   - A category must be selected
 *   - Deactivation requires explicit confirmation (caller passes confirmed=true)
 */
public class ManageCoursesService {

    // ── Result types ──────────────────────────────────────────────────────────
    public record ActionResult(boolean success, String message) {}

    // ── Constants ─────────────────────────────────────────────────────────────
    public static final int MIN_CREDITS = 1;
    public static final int MAX_CREDITS = 6;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final ManageCoursesDAO dao;

    /** Cached after init() — used to scope every DB call. */
    private int    majorDeptId   = -1;
    private String deptName      = "";  // for display in the panel header

    public ManageCoursesService() { this.dao = new ManageCoursesDAO(); }

    /** Injection constructor for unit tests. */
    public ManageCoursesService(ManageCoursesDAO dao) { this.dao = dao; }

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Resolves the admin's username to their major_dept_id and caches it.
     * Must be called once before any other method.
     *
     * @param adminUsername  login username of the logged-in admin
     * @return true if the admin's department was found; false otherwise
     */
    public boolean init(String adminUsername) {
        try {
            majorDeptId = dao.getMajorDeptIdForAdmin(adminUsername);
            if (majorDeptId > 0) {
                List<DeptOption> d = dao.getDeptForAdmin(majorDeptId);
                if (!d.isEmpty()) deptName = d.get(0).getName();
            }
            return majorDeptId > 0;
        } catch (SQLException e) {
            majorDeptId = -1;
            return false;
        }
    }

    /**
     * Returns the name of the admin's faculty (e.g. "Computing").
     * Used by the UI to personalise the panel subtitle.
     */
    public String getDeptName() { return deptName; }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Loads courses scoped to the admin's own faculty.
     */
    /** Last load error message — surfaced by the UI when the catalog query fails. */
    private String lastLoadError = "";

    public String getLastLoadError() { return lastLoadError; }

    public List<CourseRow> loadCourses() {
        ensureInit();
        lastLoadError = "";
        try {
            return dao.getCoursesByDept(majorDeptId);
        } catch (SQLException e) {
            lastLoadError = e.getMessage();
            return List.of();
        }
    }

    /**
     * Returns only the admin's own department for the department dropdown.
     * An admin cannot reassign a course to a different faculty.
     */
    public List<DeptOption> loadDepts() {
        ensureInit();
        try {
            return dao.getDeptForAdmin(majorDeptId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /** Returns all active course categories for the Add / Edit category dropdown. */
    public List<CategoryOption> loadCategories() {
        try {
            return dao.getAllCategories();
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Validates inputs and adds a new course to the admin's faculty.
     *
     * @param categoryId   selected category  (-1 = none)
     * @param courseCode   raw text from the code field
     * @param courseName   raw text from the name field
     * @param creditHours  credit value from the spinner
     */
    public ActionResult addCourse(int categoryId, String courseCode,
                                  String courseName, int creditHours,
                                  int recommendedSemester, String preReqCode) {
        ensureInit();

        String cleanCode = courseCode == null ? "" : courseCode.trim().toUpperCase();
        String cleanName = courseName == null ? "" : courseName.trim();

        if (cleanCode.isEmpty())
            return new ActionResult(false, "Course code is required.");
        if (cleanName.isEmpty())
            return new ActionResult(false, "Course name is required.");
        if (creditHours < MIN_CREDITS || creditHours > MAX_CREDITS)
            return new ActionResult(false,
                "Credit hours must be between " + MIN_CREDITS + " and " + MAX_CREDITS + ".");
        if (recommendedSemester < 1 || recommendedSemester > 8)
            return new ActionResult(false,
                "Recommended semester must be between 1 and 8.");
        if (categoryId < 0)
            return new ActionResult(false, "Please select a category.");

        try {
            if (dao.existsByCode(cleanCode))
                return new ActionResult(false,
                    "Course code \"" + cleanCode + "\" already exists in the catalog.");
        } catch (SQLException e) {
            return new ActionResult(false, "DB error checking code: " + e.getMessage());
        }

        try {
            int newId = dao.addCourse(majorDeptId, categoryId,
                                      cleanCode, cleanName, creditHours,
                                      recommendedSemester, preReqCode);
            if (newId < 0)
                return new ActionResult(false, "Insert failed — no ID returned.");
        } catch (SQLException e) {
            return new ActionResult(false, "Failed to add course: " + e.getMessage());
        }

        return new ActionResult(true,
            "Course \"" + cleanCode + " — " + cleanName + "\" added successfully.");
    }

    /**
     * Validates inputs and updates an existing course.
     * majorDeptId is not editable — dept is always the admin's own faculty.
     *
     * @param courseId    course being edited
     * @param courseName  new name
     * @param creditHours new credit value
     * @param categoryId  new category
     */
    public ActionResult updateCourse(int courseId, String courseName,
                                     int creditHours, int categoryId,
                                     int recommendedSemester, String prereqCode) {
        ensureInit();
        String cleanName = courseName == null ? "" : courseName.trim();

        if (cleanName.isEmpty())
            return new ActionResult(false, "Course name is required.");
        if (creditHours < MIN_CREDITS || creditHours > MAX_CREDITS)
            return new ActionResult(false,
                "Credit hours must be between " + MIN_CREDITS + " and " + MAX_CREDITS + ".");
        if (recommendedSemester < 1 || recommendedSemester > 8)
            return new ActionResult(false,
                "Recommended semester must be between 1 and 8.");
        if (categoryId < 0)
            return new ActionResult(false, "Please select a category.");

        try {
            boolean updated = dao.updateCourse(courseId, cleanName,
                                               creditHours, majorDeptId, categoryId,
                                               recommendedSemester, prereqCode);
            if (!updated)
                return new ActionResult(false, "No matching course found to update.");
        } catch (SQLException e) {
            return new ActionResult(false, "Failed to update course: " + e.getMessage());
        }

        return new ActionResult(true, "Course updated successfully.");
    }

    /**
     * Deactivates a course. Caller must pass confirmed=true after showing dialog.
     */
    public ActionResult deactivateCourse(int courseId, boolean confirmed) {
        if (!confirmed)
            return new ActionResult(false, "Deactivation cancelled.");
        try {
            boolean updated = dao.setActiveStatus(courseId, false);
            if (!updated)
                return new ActionResult(false, "No matching course found.");
        } catch (SQLException e) {
            return new ActionResult(false, "Failed to deactivate: " + e.getMessage());
        }
        return new ActionResult(true, "Course deactivated successfully.");
    }

    /** Reactivates a previously deactivated course. */
    public ActionResult reactivateCourse(int courseId) {
        try {
            boolean updated = dao.setActiveStatus(courseId, true);
            if (!updated)
                return new ActionResult(false, "No matching course found.");
        } catch (SQLException e) {
            return new ActionResult(false, "Failed to reactivate: " + e.getMessage());
        }
        return new ActionResult(true, "Course reactivated successfully.");
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private void ensureInit() {
        if (majorDeptId < 0)
            throw new IllegalStateException(
                "ManageCoursesService.init() must be called before any other method.");
    }
}
