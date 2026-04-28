package bl;

import dao.StudentAnnouncementDAO;
import model.StudentAnnouncementItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for the Student Announcements feature.
 *
 * Responsibilities:
 *   - Resolve username → student_id (cached for the session)
 *   - Delegate all DB access to StudentAnnouncementDAO
 *   - Return the announcement list to the UI
 *
 * This class contains zero Swing code and zero SQL.
 */
public class StudentAnnouncementService {

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final StudentAnnouncementDAO dao;

    /** Cached after init(); valid for the lifetime of this panel session. */
    private int studentId = -1;

    // ── Constructors ─────────────────────────────────────────────────────────
    public StudentAnnouncementService() {
        this.dao = new StudentAnnouncementDAO();
    }

    /** Injection constructor for unit tests. */
    public StudentAnnouncementService(StudentAnnouncementDAO dao) {
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
     * Returns all announcements visible to this student, newest first.
     * Includes course-specific, department-wide, and university-wide announcements.
     *
     * @return list of StudentAnnouncementItem (never null; may be empty)
     * @throws IllegalStateException if {@link #init} was not called first
     */
    public List<StudentAnnouncementItem> getAnnouncements() {
        ensureInitialised();
        try {
            return dao.getAnnouncementsForStudent(studentId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────
    private void ensureInitialised() {
        if (studentId < 0) {
            throw new IllegalStateException(
                "StudentAnnouncementService.init() must be called before any other method.");
        }
    }
}