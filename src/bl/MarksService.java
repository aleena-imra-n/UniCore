package bl;

import dao.MarksDAO;
import model.MarksItem;
import model.OfferingItem;
import model.RosterStudent;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for the Upload Marks feature.
 *
 * Responsibilities:
 *   - Resolve username → teacher session
 *   - Validate mark inputs (range, total marks)
 *   - Delegate all DB reads/writes to MarksDAO
 *   - Return typed SaveResult objects so UI never interprets raw strings
 *
 * Zero Swing code. Zero SQL.
 */
public class MarksService {

    // ── Result types ──────────────────────────────────────────────────────────
    public record SaveResult(Outcome outcome, String message) {}

    public enum Outcome {
        SUCCESS,
        INVALID_MARKS,       // obtained > total or negative values
        INVALID_TOTAL,       // total marks is zero or negative
        NO_COURSE,
        NO_ASSESSMENT,
        ERROR
    }

    // ── Valid assessment types ────────────────────────────────────────────────
    public static final String[] ASSESSMENT_TYPES =
        {"quiz", "assignment", "mid", "final", "lab", "project"};

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final MarksDAO dao;
    private String username;
    private boolean initialised = false;

    public MarksService() {
        this.dao = new MarksDAO();
    }

    /** Injection constructor for unit tests. */
    public MarksService(MarksDAO dao) {
        this.dao = dao;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the service for the logged-in teacher.
     * Must be called once before any other method.
     */
    public boolean init(String username) {
        this.username     = username;
        this.initialised  = true;
        return username != null && !username.isBlank();
    }

    /** Returns all course offerings assigned to the teacher this semester. */
    public List<OfferingItem> getOfferings() {
        ensureInitialised();
        try {
            return dao.getOfferingsForTeacher(username);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /** Returns all actively enrolled students for a course offering. */
    public List<RosterStudent> getRoster(int offeringId) {
        ensureInitialised();
        try {
            return dao.getRosterForOffering(offeringId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Returns existing marks for a course + assessment type.
     * Used to pre-fill the marks table when a teacher revisits an entry.
     */
    public List<MarksItem> getExistingMarks(int offeringId, String assessmentType) {
        ensureInitialised();
        try {
            return dao.getMarksForOffering(offeringId, assessmentType);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Validates and saves marks for a single student.
     *
     * Business rules:
     *   1. marksObtained must be >= 0
     *   2. totalMarks must be > 0
     *   3. marksObtained must be <= totalMarks
     */
    public SaveResult saveMark(int enrollmentId, String assessmentType,
                               double marksObtained, double totalMarks,
                               String remarks) {
        ensureInitialised();

        if (assessmentType == null || assessmentType.isBlank())
            return new SaveResult(Outcome.NO_ASSESSMENT, "Please select an assessment type.");

        if (totalMarks <= 0)
            return new SaveResult(Outcome.INVALID_TOTAL,
                "Total marks must be greater than 0.");

        if (marksObtained < 0)
            return new SaveResult(Outcome.INVALID_MARKS,
                "Marks obtained cannot be negative.");

        if (marksObtained > totalMarks)
            return new SaveResult(Outcome.INVALID_MARKS,
                "Marks obtained (" + marksObtained + ") cannot exceed total marks ("
                    + totalMarks + ").");

        try {
            dao.upsertMark(enrollmentId, assessmentType,
                           marksObtained, totalMarks,
                           remarks == null ? "" : remarks.trim());
            return new SaveResult(Outcome.SUCCESS, "Marks saved successfully!");
        } catch (SQLException e) {
            return new SaveResult(Outcome.ERROR,
                "Failed to save marks: " + e.getMessage());
        }
    }

    /**
     * Validates and saves marks for an entire class in one transaction.
     * Skips rows where totalMarks is 0 (teacher left them blank).
     */
    public SaveResult saveBatch(List<Integer> enrollmentIds,
                                String assessmentType,
                                List<Double> obtained,
                                List<Double> totals,
                                List<String> remarks) {
        ensureInitialised();

        if (assessmentType == null || assessmentType.isBlank())
            return new SaveResult(Outcome.NO_ASSESSMENT, "Please select an assessment type.");

        // Validate all rows before touching the DB
        for (int i = 0; i < enrollmentIds.size(); i++) {
            double tot = totals.get(i);
            double obt = obtained.get(i);
            if (tot == 0) continue;  // blank row — skip
            if (tot < 0)
                return new SaveResult(Outcome.INVALID_TOTAL,
                    "Total marks must be > 0 (row " + (i + 1) + ").");
            if (obt < 0 || obt > tot)
                return new SaveResult(Outcome.INVALID_MARKS,
                    "Invalid marks for row " + (i + 1) + ": "
                        + obt + " / " + tot + ".");
        }

        // Build batch lists (skip blanks)
        List<int[]>  eidList  = new java.util.ArrayList<>();
        List<Double> obtList  = new java.util.ArrayList<>();
        List<Double> totList  = new java.util.ArrayList<>();
        List<String> remList  = new java.util.ArrayList<>();

        for (int i = 0; i < enrollmentIds.size(); i++) {
            if (totals.get(i) == 0) continue;
            eidList.add(new int[]{enrollmentIds.get(i)});
            obtList.add(obtained.get(i));
            totList.add(totals.get(i));
            remList.add(remarks.get(i) == null ? "" : remarks.get(i).trim());
        }

        if (eidList.isEmpty())
            return new SaveResult(Outcome.INVALID_TOTAL,
                "No marks entered. Please fill in at least one row.");

        try {
            dao.saveBatch(eidList, assessmentType, obtList, totList, remList);
            return new SaveResult(Outcome.SUCCESS,
                "Marks saved for " + eidList.size() + " student(s)!");
        } catch (SQLException e) {
            return new SaveResult(Outcome.ERROR,
                "Failed to save marks: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private void ensureInitialised() {
        if (!initialised)
            throw new IllegalStateException(
                "MarksService.init() must be called before any other method.");
    }
}