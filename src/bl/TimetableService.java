package bl;

import dao.TimetableDAO;
import dao.TimetableDAO.AddSlotResult;
import model.OfferingDropdownItem;
import model.TimetableConflict;
import model.TimetableSlot;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for the Admin Create Timetables feature
 * (US-3.7a / US-3.11a).
 *
 * Responsibilities:
 *   - Validate form inputs before any SP call
 *   - Orchestrate the two-phase add flow:
 *       Phase 1 (force=false): detect conflicts, return to UI for confirmation
 *       Phase 2 (force=true):  insert despite conflicts if admin confirms
 *   - Delegate all DB access to TimetableDAO
 *   - Return typed result objects; UI never interprets raw strings
 *
 * Zero Swing code. Zero SQL.
 */
public class TimetableService {

    // ── Result types ──────────────────────────────────────────────────────────

    /**
     * Outcome of an add-slot attempt.
     *
     * @param outcome     SUCCESS | CONFLICT | DUPLICATE | VALIDATION_ERROR | DB_ERROR
     * @param message     human-readable status message for the UI label
     * @param conflicts   non-empty when outcome == CONFLICT
     * @param newId       generated timetable_id on SUCCESS; −1 otherwise
     */
    public record AddResult(
        Outcome                  outcome,
        String                   message,
        List<TimetableConflict>  conflicts,
        int                      newId
    ) {}

    public enum Outcome {
        SUCCESS,
        CONFLICT,          // room clash detected — admin must confirm to force
        DUPLICATE,         // same offering+day+start already exists
        VALIDATION_ERROR,  // bad input caught before DB call
        DB_ERROR           // unexpected SQL failure
    }

    public record DeleteResult(boolean success, String message) {}

    // ── Day ordering (for sorting the slot table) ─────────────────────────────
    public static final String[] DAYS_OF_WEEK =
        { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final TimetableDAO dao;

    public TimetableService()                  { this.dao = new TimetableDAO(); }
    public TimetableService(TimetableDAO dao)  { this.dao = dao; }

    // ── Data loaders ──────────────────────────────────────────────────────────

    /** All active-semester offerings for the dropdown. */
    public List<OfferingDropdownItem> getAllOfferings() {
        try { return dao.getAllOfferings(); }
        catch (SQLException e) { return List.of(); }
    }

    /** All timetable slots for the active semester (master table). */
    public List<TimetableSlot> getAllSlots() {
        try { return dao.getAllSlots(); }
        catch (SQLException e) { return List.of(); }
    }

    /** Timetable slots for one specific offering (side panel). */
    public List<TimetableSlot> getSlotsForOffering(int offeringId) {
        try { return dao.getSlotsForOffering(offeringId); }
        catch (SQLException e) { return List.of(); }
    }

    // ── Add slot (two-phase) ──────────────────────────────────────────────────

    /**
     * Phase 1 — probe for conflicts without inserting.
     * Call this first. If the result is CONFLICT, show the warning dialog.
     * If the admin confirms, call {@link #addSlotForced}.
     *
     * @param offeringId  offering_id from the dropdown
     * @param dayOfWeek   one of the five weekday strings
     * @param startTime   "HH:mm" (24h)
     * @param endTime     "HH:mm" (24h)
     * @param roomNumber  may be null / blank
     */
    public AddResult addSlot(int offeringId, String dayOfWeek,
                              String startTime, String endTime,
                              String roomNumber) {
        AddResult validation = validate(offeringId, dayOfWeek, startTime, endTime);
        if (validation != null) return validation;

        try {
            AddSlotResult result = dao.addSlot(offeringId, dayOfWeek,
                startTime, endTime, roomNumber, false);
            return interpret(result, dayOfWeek, startTime, endTime);
        } catch (SQLException e) {
            return spError(e.getMessage());
        }
    }

    /**
     * Phase 2 — insert despite any room conflict (admin confirmed).
     * Identical to {@link #addSlot} but passes force=true to the SP.
     */
    public AddResult addSlotForced(int offeringId, String dayOfWeek,
                                    String startTime, String endTime,
                                    String roomNumber) {
        AddResult validation = validate(offeringId, dayOfWeek, startTime, endTime);
        if (validation != null) return validation;

        try {
            AddSlotResult result = dao.addSlot(offeringId, dayOfWeek,
                startTime, endTime, roomNumber, true);
            return interpret(result, dayOfWeek, startTime, endTime);
        } catch (SQLException e) {
            return spError(e.getMessage());
        }
    }

    // ── Delete slot ───────────────────────────────────────────────────────────

    public DeleteResult deleteSlot(int timetableId) {
        if (timetableId <= 0)
            return new DeleteResult(false, "No slot selected.");
        try {
            dao.deleteSlot(timetableId);
            return new DeleteResult(true, "Timetable slot deleted.");
        } catch (SQLException e) {
            return new DeleteResult(false, extractMsg(e.getMessage()));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AddResult validate(int offeringId, String dayOfWeek,
                                String startTime, String endTime) {
        if (offeringId <= 0)
            return new AddResult(Outcome.VALIDATION_ERROR,
                "Please select a course offering.", List.of(), -1);

        boolean validDay = false;
        for (String d : DAYS_OF_WEEK) if (d.equals(dayOfWeek)) { validDay = true; break; }
        if (!validDay)
            return new AddResult(Outcome.VALIDATION_ERROR,
                "Please select a valid day of week.", List.of(), -1);

        if (startTime == null || !startTime.matches("\\d{2}:\\d{2}"))
            return new AddResult(Outcome.VALIDATION_ERROR,
                "Start time must be in HH:mm format.", List.of(), -1);

        if (endTime == null || !endTime.matches("\\d{2}:\\d{2}"))
            return new AddResult(Outcome.VALIDATION_ERROR,
                "End time must be in HH:mm format.", List.of(), -1);

        if (endTime.compareTo(startTime) <= 0)
            return new AddResult(Outcome.VALIDATION_ERROR,
                "End time must be after start time.", List.of(), -1);

        return null;  // no validation error
    }

    private AddResult interpret(AddSlotResult raw,
                                 String day, String start, String end) {
        if (raw.inserted()) {
            return new AddResult(Outcome.SUCCESS,
                "Slot added: " + day + "  " + start + "–" + end + ".",
                List.of(), raw.newId());
        }
        // Not inserted but no exception = conflicts found and force=false
        if (!raw.conflicts().isEmpty()) {
            return new AddResult(Outcome.CONFLICT,
                raw.conflicts().size() + " room conflict"
                    + (raw.conflicts().size() == 1 ? "" : "s") + " detected.",
                raw.conflicts(), -1);
        }
        // Should not reach here normally
        return new AddResult(Outcome.DB_ERROR,
            "Slot was not inserted. Please try again.", List.of(), -1);
    }

    private AddResult spError(String msg) {
        String clean = extractMsg(msg);
        // Classify duplicate key violation from UQ_TimetableSlot
        if (clean.toLowerCase().contains("already has a slot") ||
            clean.toLowerCase().contains("unique") ||
            clean.toLowerCase().contains("duplicate")) {
            return new AddResult(Outcome.DUPLICATE, clean, List.of(), -1);
        }
        return new AddResult(Outcome.DB_ERROR, clean, List.of(), -1);
    }

    private String extractMsg(String jdbcMsg) {
        if (jdbcMsg == null) return "Operation failed.";
        int colon = jdbcMsg.indexOf(':');
        return (colon >= 0 && colon < jdbcMsg.length() - 1)
            ? jdbcMsg.substring(colon + 1).trim()
            : jdbcMsg;
    }
}