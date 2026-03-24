package bl;

import dao.StudentAttendanceDAO;
import model.StudentAttendanceItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for the Student Attendance feature.
 *
 * Responsibilities:
 *   - Resolve username → student_id (cached for the session)
 *   - Delegate all DB access to StudentAttendanceDAO
 *   - Compute overall attendance stats across all courses for the header summary
 *
 * This class contains zero Swing code and zero SQL.
 */
public class StudentAttendanceService {

    // ── Summary stats returned to the UI header ───────────────────────────────
    /**
     * Aggregate figures shown in the header badge area.
     *
     * @param totalCourses      number of active enrolled courses
     * @param totalClasses      sum of all recorded class sessions
     * @param totalAttended     sum of attended sessions (present + late)
     * @param eligibleCourses   courses where attendance >= threshold
     */
    public record AttendanceSummary(
        int totalCourses,
        int totalClasses,
        int totalAttended,
        int eligibleCourses
    ) {
        /** Overall attendance % across all courses (0 if no classes recorded). */
        public double overallPct() {
            return totalClasses == 0 ? 0.0
                : (totalAttended * 100.0) / totalClasses;
        }

        /** Formatted overall percentage string e.g. "82.3%" */
        public String overallPctStr() {
            return totalClasses == 0 ? "N/A"
                : String.format("%.1f%%", overallPct());
        }
    }

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final StudentAttendanceDAO dao;

    /** Cached after init(); valid for the lifetime of this panel session. */
    private int studentId = -1;

    // ── Constructors ─────────────────────────────────────────────────────────
    public StudentAttendanceService() {
        this.dao = new StudentAttendanceDAO();
    }

    /** Injection constructor for unit tests. */
    public StudentAttendanceService(StudentAttendanceDAO dao) {
        this.dao = dao;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the service for the logged-in student.
     * Must be called once before any other method.
     *
     * @param username  login username from the session
     * @return true if a matching active student account was found
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
     * Returns per-course attendance summaries for the active semester,
     * ordered by course code.
     *
     * @return list of StudentAttendanceItem (never null; may be empty)
     * @throws IllegalStateException if {@link #init} was not called first
     */
    public List<StudentAttendanceItem> getAttendanceSummary() {
        ensureInitialised();
        try {
            return dao.getAttendanceSummary(studentId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Computes aggregate header stats from the course list.
     * Called once after {@link #getAttendanceSummary()} so the UI does not
     * iterate the list twice.
     *
     * @param courses  list returned by {@link #getAttendanceSummary()}
     * @return AttendanceSummary with totals and eligible-course count
     */
    public AttendanceSummary computeSummary(List<StudentAttendanceItem> courses) {
        int totalClasses  = 0;
        int totalAttended = 0;
        int eligible      = 0;

        for (StudentAttendanceItem c : courses) {
            totalClasses  += c.getTotalClasses();
            totalAttended += c.getClassesAttended();
            if (c.isEligible()) eligible++;
        }

        return new AttendanceSummary(courses.size(), totalClasses,
                                     totalAttended, eligible);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────
    private void ensureInitialised() {
        if (studentId < 0) {
            throw new IllegalStateException(
                "StudentAttendanceService.init() must be called before any other method.");
        }
    }
}