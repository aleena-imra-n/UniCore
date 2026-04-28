package bl;

import dao.AttendanceDAO;
import model.AttendanceRecord;
import model.AttendanceRecord.Status;
import model.OfferingItem;
import model.RosterStudent;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Business-Logic layer for the Mark Attendance feature.
 *
 * Responsibilities:
 *   - Date parsing and validation
 *   - Determining new vs. edit mode
 *   - Building AttendanceRecord lists from UI state
 *   - Checking "all students marked" before save
 *   - Delegating DB reads/writes to AttendanceDAO
 *
 * The UI calls only this class.  This class calls only AttendanceDAO.
 * Neither layer reaches into the other's territory.
 */
public class AttendanceService {

    // ── Result types returned to the UI ──────────────────────────────────────

    /** Outcome of a load-date request. */
    public record LoadResult(
        boolean     found,          // true = existing record; false = new session
        String      message,        // human-readable message for the footer
        Map<Integer, Status> statusMap  // studentId → Status (empty when !found)
    ) {}

    /** Outcome of a save/update request. */
    public record SaveResult(
        boolean success,
        String  message
    ) {}

    // ── Constants ────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final AttendanceDAO dao;

    public AttendanceService() {
        this.dao = new AttendanceDAO();
    }

    /** Constructor for unit testing (inject a mock DAO). */
    public AttendanceService(AttendanceDAO dao) {
        this.dao = dao;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns all course offerings for the given teacher in the active semester.
     *
     * @param teacherUsername  login username
     * @return list of OfferingItem ready to populate the combo box
     * @throws SQLException on DB failure
     */
    public List<OfferingItem> getOfferingsForTeacher(String teacherUsername)
            throws SQLException {
        return dao.getOfferingsForTeacher(teacherUsername);
    }

    /**
     * Loads the student roster for a course offering.
     *
     * @param offeringId  selected offering
     * @return list of RosterStudent ordered by roll number
     * @throws SQLException on DB failure
     */
    public List<RosterStudent> getRoster(int offeringId) throws SQLException {
        return dao.getRosterForOffering(offeringId);
    }

    /**
     * Validates the date string, then checks the DB for an existing session.
     *
     * Rules enforced here (not in the UI):
     *   - Date must parse as yyyy-MM-dd
     *   - Date must not be in the future
     *   - An offering must be selected
     *
     * @param dateText   raw text from the date field
     * @param offeringId currently selected offering (-1 = none selected)
     * @return LoadResult describing what the UI should do next
     */
    public LoadResult loadAttendanceForDate(String dateText, int offeringId) {

        // ── Validate date format ─────────────────────────────────────────────
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(dateText.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return new LoadResult(false,
                "Invalid date format — please use YYYY-MM-DD.", Map.of());
        }

        // ── Validate date is not future ──────────────────────────────────────
        if (targetDate.isAfter(LocalDate.now())) {
            return new LoadResult(false,
                "Cannot load attendance for a future date.", Map.of());
        }

        // ── Validate offering selected ───────────────────────────────────────
        if (offeringId < 0) {
            return new LoadResult(false,
                "Please select a course first.", Map.of());
        }

        // ── Query DB ─────────────────────────────────────────────────────────
        Map<Integer, AttendanceRecord> saved;
        try {
            saved = dao.getByOfferingAndDate(offeringId, targetDate);
        } catch (SQLException e) {
            return new LoadResult(false,
                "DB error loading attendance: " + e.getMessage(), Map.of());
        }

        // ── Nothing found → tell UI it's a new session ───────────────────────
        if (saved.isEmpty()) {
            return new LoadResult(false,
                "No record found for " + dateText
                    + ". You can mark a new session.",
                Map.of());
        }

        // ── Found → flatten to studentId → Status for the UI ─────────────────
        Map<Integer, Status> statusMap = new LinkedHashMap<>();
        saved.forEach((studentId, rec) -> statusMap.put(studentId, rec.getStatus()));

        return new LoadResult(true,
            "Loaded record for " + dateText
                + " — edit then click Update to save changes.",
            statusMap);
    }

    /**
     * Saves a new attendance session after validating that every student
     * on the roster has been marked.
     *
     * @param offeringId   selected offering
     * @param dateText     session date string (yyyy-MM-dd)
     * @param roster       current roster (defines which students must be marked)
     * @param statusMap    studentId → Status as set in the UI
     * @return SaveResult with success flag and user-facing message
     */
    public SaveResult saveAttendance(int offeringId, String dateText,
                                     List<RosterStudent> roster,
                                     Map<Integer, Status> statusMap) {

        // ── All-marked check ─────────────────────────────────────────────────
        List<String> unmarked = findUnmarked(roster, statusMap);
        if (!unmarked.isEmpty()) {
            return new SaveResult(false,
                unmarked.size() + " student(s) not yet marked: "
                    + String.join(", ", unmarked));
        }

        LocalDate date = LocalDate.parse(dateText.trim(), DATE_FMT);
        List<AttendanceRecord> records = buildRecords(roster, statusMap, date);

        try {
            dao.saveAttendance(records);
        } catch (SQLException e) {
            return new SaveResult(false,
                "Failed to save attendance: " + e.getMessage());
        }

        return new SaveResult(true,
            "Attendance saved for " + dateText
                + " (" + records.size() + " students).");
    }

    /**
     * Updates an existing attendance session.
     * Applies the same "all-marked" validation as save.
     *
     * @param offeringId   selected offering
     * @param dateText     session date string (yyyy-MM-dd)
     * @param roster       current roster
     * @param statusMap    studentId → Status as set in the UI
     * @return SaveResult with success flag and user-facing message
     */
    public SaveResult updateAttendance(int offeringId, String dateText,
                                       List<RosterStudent> roster,
                                       Map<Integer, Status> statusMap) {

        // ── All-marked check ─────────────────────────────────────────────────
        List<String> unmarked = findUnmarked(roster, statusMap);
        if (!unmarked.isEmpty()) {
            return new SaveResult(false,
                unmarked.size() + " student(s) not yet marked: "
                    + String.join(", ", unmarked));
        }

        LocalDate date = LocalDate.parse(dateText.trim(), DATE_FMT);
        List<AttendanceRecord> records = buildRecords(roster, statusMap, date);

        try {
            dao.updateAttendance(records);
        } catch (SQLException e) {
            return new SaveResult(false,
                "Failed to update attendance: " + e.getMessage());
        }

        return new SaveResult(true,
            "Attendance updated for " + dateText
                + " (" + records.size() + " students).");
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Returns the full names of students who have no status (or UNMARKED) set.
     */
    private List<String> findUnmarked(List<RosterStudent> roster,
                                       Map<Integer, Status> statusMap) {
        List<String> unmarked = new ArrayList<>();
        for (RosterStudent s : roster) {
            Status st = statusMap.getOrDefault(s.getStudentId(), Status.UNMARKED);
            if (st == Status.UNMARKED) {
                unmarked.add(s.getFullName());
            }
        }
        return unmarked;
    }

    /**
     * Converts the UI's statusMap + roster into a list of AttendanceRecord
     * objects ready to be persisted by the DAO.
     */
    private List<AttendanceRecord> buildRecords(List<RosterStudent> roster,
                                                 Map<Integer, Status> statusMap,
                                                 LocalDate date) {
        List<AttendanceRecord> records = new ArrayList<>();
        for (RosterStudent s : roster) {
            Status status = statusMap.getOrDefault(s.getStudentId(), Status.PRESENT);
            records.add(new AttendanceRecord(
                s.getEnrollmentId(),
                s.getStudentId(),
                date,
                status,
                null   // remarks not collected in this UI; extend if needed
            ));
        }
        return records;
    }

    /**
     * Convenience: counts how many students have each status.
     * Returns an int[3] where [0]=present, [1]=absent, [2]=late.
     */
    public int[] countByStatus(List<RosterStudent> roster,
                                Map<Integer, Status> statusMap) {
        int p = 0, a = 0, l = 0;
        for (RosterStudent s : roster) {
            switch (statusMap.getOrDefault(s.getStudentId(), Status.UNMARKED)) {
                case PRESENT -> p++;
                case ABSENT  -> a++;
                case LATE    -> l++;
                default      -> {}
            }
        }
        return new int[]{ p, a, l };
    }
}
