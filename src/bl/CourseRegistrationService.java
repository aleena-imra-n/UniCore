package bl;

import dao.CourseRegistrationDAO;
import model.CourseItem;
import model.EnrolledCourse;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for Course Registration.
 *
 * Responsibilities:
 *   - Resolve username → student_id (once, cached for the session)
 *   - Validate that a course was actually selected before registering
 *   - Check for duplicate enrolment before hitting the SP
 *   - Delegate all DB access to CourseRegistrationDAO
 *   - Return typed result objects so the UI never interprets raw strings
 *
 * This class contains zero Swing code and zero SQL.
 * The UI calls only this class; this class calls only the DAO.
 */
public class CourseRegistrationService {

    // ── Result types returned to the UI ──────────────────────────────────────

    /**
     * Outcome of a registration attempt.
     *
     * @param outcome  one of: SUCCESS | NONE_SELECTED | ALREADY_ENROLLED | ERROR
     * @param message  human-readable message for the status label
     * @param course   the registered CourseItem (non-null only on SUCCESS)
     */
    public record RegisterResult(
        Outcome     outcome,
        String      message,
        CourseItem  course       // null unless outcome == SUCCESS
    ) {}

    public enum Outcome {
        SUCCESS,
        NONE_SELECTED,
        ALREADY_ENROLLED,
        ERROR
    }

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final CourseRegistrationDAO dao;

    /** Cached after the first DB lookup; valid for the lifetime of this panel. */
    private int studentId = -1;

    // ── Constructors ─────────────────────────────────────────────────────────
    public CourseRegistrationService() {
        this.dao = new CourseRegistrationDAO();
    }

    /** Injection constructor for unit tests. */
    public CourseRegistrationService(CourseRegistrationDAO dao) {
        this.dao = dao;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the service for the logged-in student.
     * Must be called once before any other method.
     *
     * @param username  login username from the session
     * @return true if the student account was found; false otherwise
     */
    public boolean init(String username) {
        try {
            studentId = dao.getStudentId(username);
            return studentId > 0;
        } catch (SQLException e) {
            studentId = -1;
            return false;
        }
    }

    /**
     * Returns all course offerings this student can still register for.
     * Already-enrolled offerings are excluded at the DB layer.
     *
     * @return list of CourseItem for the dropdown (never null; may be empty)
     * @throws IllegalStateException if {@link #init} was not called first
     */
    public List<CourseItem> getAvailableCourses() {
        ensureInitialised();
        try {
            return dao.getAvailableCourses(studentId);
        } catch (SQLException e) {
            // Return empty list; caller shows an error footer
            return List.of();
        }
    }

    /**
     * Returns courses the student is currently enrolled in (active semester).
     *
     * @return list of EnrolledCourse for the right-hand panel
     * @throws IllegalStateException if {@link #init} was not called first
     */
    public List<EnrolledCourse> getEnrolledCourses() {
        ensureInitialised();
        try {
            return dao.getEnrolledCourses(studentId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Attempts to register the student for the selected course.
     *
     * Business rules enforced here:
     *   1. A real course must be selected (selectedIndex > 0).
     *   2. Duplicate enrolment check before calling the SP.
     *   3. SP_RegisterStudent handles eligibility + capacity at DB level.
     *
     * @param selectedCourse  the CourseItem chosen in the dropdown,
     *                        or {@code null} if the placeholder ("— Select —") is active
     * @return RegisterResult describing exactly what happened
     */
    public RegisterResult register(CourseItem selectedCourse) {
        ensureInitialised();

        // Rule 1: something must be selected
        if (selectedCourse == null) {
            return new RegisterResult(Outcome.NONE_SELECTED,
                "Please select a course first.", null);
        }

        // Rule 2: duplicate check (fast path before SP round-trip)
        try {
            if (dao.isAlreadyEnrolled(studentId, selectedCourse.getOfferingId())) {
                return new RegisterResult(Outcome.ALREADY_ENROLLED,
                    "You are already enrolled in "
                        + selectedCourse.getCourseCode() + ".", null);
            }
        } catch (SQLException e) {
            return new RegisterResult(Outcome.ERROR,
                "Could not verify enrolment status: " + e.getMessage(), null);
        }

        // Rule 3: delegate to SP (handles eligibility + capacity)
        try {
            dao.enrollStudent(studentId, selectedCourse.getOfferingId());
        } catch (SQLException e) {
            // SP raises errors with meaningful text — surface them directly
            String spMsg = extractSpMessage(e.getMessage());
            return new RegisterResult(Outcome.ERROR, spMsg, null);
        }

        return new RegisterResult(Outcome.SUCCESS,
            selectedCourse.getCourseName() + " registered successfully!",
            selectedCourse);
    }

    /**
     * Returns the total credit hours across all currently enrolled courses.
     * Useful for a credit-load summary (can be displayed in the header).
     */
    public int getTotalCreditHours(List<EnrolledCourse> enrolled) {
        return enrolled.stream()
                       .mapToInt(EnrolledCourse::getCreditHours)
                       .sum();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void ensureInitialised() {
        if (studentId < 0) {
            throw new IllegalStateException(
                "CourseRegistrationService.init() must be called before any other method.");
        }
    }

    /**
     * SQL Server SP errors are wrapped like:
     *   "com.microsoft.sqlserver.jdbc.SQLServerException: Student not eligible..."
     * This strips the JDBC prefix so the UI sees only the human-readable part.
     */
    private String extractSpMessage(String jdbcMessage) {
        if (jdbcMessage == null) return "Registration failed. Please try again.";
        int colon = jdbcMessage.indexOf(':');
        return (colon >= 0 && colon < jdbcMessage.length() - 1)
            ? jdbcMessage.substring(colon + 1).trim()
            : jdbcMessage;
    }
}
