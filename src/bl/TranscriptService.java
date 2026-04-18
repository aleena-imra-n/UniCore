package bl;

import dao.TranscriptDAO;
import dao.TranscriptDAO.TranscriptData;
import model.TranscriptCourseRow;
import model.TranscriptSemesterRow;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Business-Logic layer for the Student Transcript feature.
 *
 * Responsibilities:
 *   - Resolve username → student_id (cached for the session)
 *   - Call the DAO and receive both result sets from SP_GetTranscript
 *   - Group the flat course-row list by semester for the UI
 *   - Expose overall CGPA and credit-hour totals
 *
 * This class contains zero Swing code and zero SQL.
 */
public class TranscriptService {

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final TranscriptDAO dao;

    /** Cached after init(); valid for the lifetime of this panel session. */
    private int studentId = -1;

    // ── Constructors ─────────────────────────────────────────────────────────
    public TranscriptService() {
        this.dao = new TranscriptDAO();
    }

    /** Injection constructor for unit tests. */
    public TranscriptService(TranscriptDAO dao) {
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
     * Loads the full transcript from the database.
     * Returns null on any DB failure (caller shows an error message).
     *
     * @return TranscriptData with both result sets, or null on error
     * @throws IllegalStateException if {@link #init} was not called first
     */
    public TranscriptData loadTranscript() {
        ensureInitialised();
        try {
            return dao.getTranscript(studentId);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Groups the flat course rows by semester (preserving chronological order).
     * The returned map's iteration order is insertion-order (LinkedHashMap),
     * so the UI can simply iterate it.
     *
     * Key   = semester name (e.g. "Fall 2023")
     * Value = list of course rows for that semester
     *
     * @param courseRows  list from {@link TranscriptData#courseRows()}
     * @return ordered map of semesterName → courses
     */
    public Map<String, List<TranscriptCourseRow>> groupBySemester(
            List<TranscriptCourseRow> courseRows) {

        Map<String, List<TranscriptCourseRow>> grouped = new LinkedHashMap<>();
        for (TranscriptCourseRow row : courseRows) {
            grouped.computeIfAbsent(row.getSemesterName(), k -> new ArrayList<>())
                   .add(row);
        }
        return grouped;
    }

    /**
     * Builds a semester-name → TranscriptSemesterRow lookup map
     * so the UI can retrieve the GPA footer for each semester block in O(1).
     *
     * @param semesterRows  list from {@link TranscriptData#semesterRows()}
     * @return map of semesterName → TranscriptSemesterRow
     */
    public Map<String, TranscriptSemesterRow> buildSemesterIndex(
            List<TranscriptSemesterRow> semesterRows) {

        Map<String, TranscriptSemesterRow> index = new LinkedHashMap<>();
        for (TranscriptSemesterRow row : semesterRows) {
            index.put(row.getSemesterName(), row);
        }
        return index;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void ensureInitialised() {
        if (studentId < 0) {
            throw new IllegalStateException(
                "TranscriptService.init() must be called before any other method.");
        }
    }
}