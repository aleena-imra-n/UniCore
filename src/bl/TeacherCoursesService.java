package bl;

import dao.TeacherCoursesDAO;
import model.CourseStudent;
import model.TeacherCourse;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for the Teacher Courses feature.
 *
 * Responsibilities:
 *   - Resolve username → teacher_id (cached for the session)
 *   - Supply courses list and per-course summary stats to the UI
 *   - Supply student roster for the "View Students" action
 *   - Compute aggregate stats (total courses, total enrolled, total seats)
 *     so the UI never does arithmetic
 *
 * This class contains zero Swing code and zero SQL.
 */
public class TeacherCoursesService {

    // ── Summary stats returned to the UI header badge ─────────────────────────
    /**
     * Pre-computed aggregate figures for the header badge area.
     *
     * @param totalCourses   number of assigned offerings this semester
     * @param totalEnrolled  sum of enrolled students across all offerings
     * @param totalSeats     sum of max_capacity across all offerings
     */
    public record CourseSummary(
        int totalCourses,
        int totalEnrolled,
        int totalSeats
    ) {
        /** Total available (unfilled) seats across all offerings. */
        public int availableSeats() { return totalSeats - totalEnrolled; }
    }

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final TeacherCoursesDAO dao;

    /** Cached after init(); valid for the lifetime of this panel session. */
    private int teacherId = -1;

    // ── Constructors ─────────────────────────────────────────────────────────
    public TeacherCoursesService() {
        this.dao = new TeacherCoursesDAO();
    }

    /** Injection constructor for unit tests. */
    public TeacherCoursesService(TeacherCoursesDAO dao) {
        this.dao = dao;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the service for the logged-in teacher.
     * Must be called once before any other method.
     *
     * @param username  login username from the session
     * @return true if a matching active teacher account was found
     */
    public boolean init(String username) {
        try {
            teacherId = dao.getTeacherId(username);
            return teacherId > 0;
        } catch (SQLException e) {
            teacherId = -1;
            return false;
        }
    }

    /**
     * Returns all course offerings assigned to the teacher this semester,
     * each with a live enrolled-student count from the DB.
     *
     * @return list of TeacherCourse ordered by course code (never null)
     */
    public List<TeacherCourse> getCourses() {
        ensureInitialised();
        try {
            return dao.getCoursesForTeacher(teacherId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Computes aggregate header stats from a courses list.
     * Called once after {@link #getCourses()} so the UI does not iterate twice.
     *
     * @param courses  the list returned by {@link #getCourses()}
     * @return CourseSummary with totalCourses, totalEnrolled, totalSeats
     */
    public CourseSummary getSummary(List<TeacherCourse> courses) {
        int enrolled = 0, seats = 0;
        for (TeacherCourse c : courses) {
            enrolled += c.getEnrolledCount();
            seats    += c.getMaxCapacity();
        }
        return new CourseSummary(courses.size(), enrolled, seats);
    }

    /**
     * Returns the active student roster for a specific offering.
     * Called when the teacher clicks "View Students" on a course card.
     *
     * @param offeringId  the offering to look up
     * @return list of CourseStudent ordered by roll number (never null)
     */
    public List<CourseStudent> getStudentsForOffering(int offeringId) {
        ensureInitialised();
        try {
            return dao.getStudentsForOffering(offeringId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────
    private void ensureInitialised() {
        if (teacherId < 0) {
            throw new IllegalStateException(
                "TeacherCoursesService.init() must be called before any other method.");
        }
    }
}
