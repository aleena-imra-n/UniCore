package bl;

import dao.IssueTranscriptDAO;
import dao.TranscriptDAO;
import dao.TranscriptDAO.TranscriptData;
import model.StudentSearchResult;
import model.TranscriptCourseRow;
import model.TranscriptSemesterRow;

import java.sql.SQLException;
import java.util.*;

/**
 * Business-Logic layer for the Admin Issue Transcripts feature (US-3.14a).
 *
 * Responsibilities:
 *   - Validate search term length before firing a DB query
 *   - Delegate student search to IssueTranscriptDAO
 *   - Load transcript data via TranscriptDAO (reuses SP_GetTranscript)
 *   - Expose grouping/indexing helpers (delegated to TranscriptService methods
 *     to keep DRY — both work on the same TranscriptData record)
 *
 * Zero Swing code. Zero SQL.
 */
public class IssueTranscriptService {

    // ── Config ────────────────────────────────────────────────────────────────
    public static final int MIN_SEARCH_LENGTH = 2;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final IssueTranscriptDAO issueDao;
    private final TranscriptDAO      transcriptDao;
    private final TranscriptService  transcriptService;

    public IssueTranscriptService() {
        this.issueDao          = new IssueTranscriptDAO();
        this.transcriptDao     = new TranscriptDAO();
        this.transcriptService = new TranscriptService(transcriptDao);
    }

    /** Injection constructor for unit tests. */
    public IssueTranscriptService(IssueTranscriptDAO issueDao,
                                   TranscriptDAO transcriptDao) {
        this.issueDao          = issueDao;
        this.transcriptDao     = transcriptDao;
        this.transcriptService = new TranscriptService(transcriptDao);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Searches students by name or roll number.
     *
     * Returns an empty list (never null) if the term is too short,
     * so the UI can safely call this without guarding for null.
     *
     * @param term  raw text from the admin's search field
     * @return list of up to 50 matching students, ordered by name
     */
    public List<StudentSearchResult> search(String term) {
        String t = term == null ? "" : term.trim();
        if (t.length() < MIN_SEARCH_LENGTH) return List.of();
        try {
            return issueDao.searchStudents(t);
        } catch (SQLException e) {
            return List.of();
        }
    }

    // ── Transcript load ───────────────────────────────────────────────────────

    /**
     * Loads the full transcript for a specific student_id.
     * Uses the same SP_GetTranscript stored procedure as the student view.
     *
     * @param studentId  the student_id to load
     * @return TranscriptData, or null on any DB error
     */
    public TranscriptData loadTranscript(int studentId) {
        try {
            TranscriptData data = transcriptDao.getTranscript(studentId);
            return data.isEmpty() ? null : data;
        } catch (SQLException e) {
            return null;
        }
    }

    // ── Grouping helpers (delegates to TranscriptService) ────────────────────

    /**
     * Groups flat course rows by semester in chronological order.
     * Key = semester name, Value = ordered list of course rows.
     */
    public Map<String, List<TranscriptCourseRow>> groupBySemester(
            List<TranscriptCourseRow> rows) {
        return transcriptService.groupBySemester(rows);
    }

    /**
     * Builds a semester-name → TranscriptSemesterRow lookup for the UI.
     */
    public Map<String, TranscriptSemesterRow> buildSemesterIndex(
            List<TranscriptSemesterRow> semRows) {
        return transcriptService.buildSemesterIndex(semRows);
    }
}