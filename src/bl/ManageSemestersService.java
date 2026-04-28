package bl;

import dao.ManageSemestersDAO;
import model.SemesterRow;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Business-Logic layer for UC-20 — Semester Management (US-3.12a).
 *
 * Rules enforced here (none in UI, none in DAO):
 *   - Semester name must not be blank and must not duplicate an existing name
 *   - Start date must be provided and must not be after end date
 *   - End date must be provided
 *   - "Set Active" requires explicit confirmation
 *   - Enrollment open/close toggles require explicit confirmation
 *   - Enrollment can only be toggled on the currently active semester
 *
 * Returns typed ActionResult records so the UI only reacts, never decides.
 */
public class ManageSemestersService {

    // ── Result type ───────────────────────────────────────────────────────────
    public record ActionResult(boolean success, String message) {}

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final ManageSemestersDAO dao;

    public ManageSemestersService() {
        this.dao = new ManageSemestersDAO();
    }

    /** Injection constructor for unit tests. */
    public ManageSemestersService(ManageSemestersDAO dao) {
        this.dao = dao;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all semesters ordered: active first, then newest first.
     */
    public List<SemesterRow> loadSemesters() {
        try {
            return dao.getAllSemesters();
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Validates inputs and creates a new semester.
     *
     * @param name      raw text from the name field
     * @param startDate selected start date (null = not chosen)
     * @param endDate   selected end date   (null = not chosen)
     */
    public ActionResult createSemester(String name, LocalDate startDate, LocalDate endDate) {
        String cleanName = name == null ? "" : name.trim();

        if (cleanName.isEmpty())
            return new ActionResult(false, "Semester name is required.");

        if (startDate == null)
            return new ActionResult(false, "Start date is required.");

        if (endDate == null)
            return new ActionResult(false, "End date is required.");

        if (!endDate.isAfter(startDate))
            return new ActionResult(false, "End date must be after the start date.");

        // Duplicate name check
        try {
            if (dao.existsByName(cleanName))
                return new ActionResult(false,
                    "A semester named \"" + cleanName + "\" already exists.");
        } catch (SQLException e) {
            return new ActionResult(false, "DB error checking name: " + e.getMessage());
        }

        try {
            int newId = dao.insertSemester(cleanName, startDate, endDate);
            if (newId < 0)
                return new ActionResult(false, "Insert failed — no ID returned.");
        } catch (SQLException e) {
            return new ActionResult(false, "Failed to create semester: " + e.getMessage());
        }

        return new ActionResult(true,
            "Semester \"" + cleanName + "\" created successfully.");
    }

    /**
     * Sets a semester as the active semester (and deactivates all others).
     * The caller must show a confirmation dialog and pass confirmed = true.
     *
     * @param semesterId   target semester PK
     * @param semesterName display name (for the result message)
     * @param confirmed    true = admin confirmed the action
     */
    public ActionResult setActiveSemester(int semesterId, String semesterName,
                                          boolean confirmed) {
        if (!confirmed)
            return new ActionResult(false, "Action cancelled.");

        try {
            boolean updated = dao.setActiveSemester(semesterId);
            if (!updated)
                return new ActionResult(false, "Semester not found in the database.");
        } catch (SQLException e) {
            return new ActionResult(false, "Failed to set active semester: " + e.getMessage());
        }

        return new ActionResult(true,
            "\"" + semesterName + "\" is now the active semester. Enrollment is closed.");
    }

    /**
     * Opens or closes enrollment for the active semester.
     * The caller must show a confirmation dialog and pass confirmed = true.
     * Enrollment can only be toggled on an active semester.
     *
     * @param semesterId     target semester PK
     * @param semesterName   display name (for the result message)
     * @param isActive       whether this semester is currently active
     * @param openEnrollment true = open enrollment, false = close it
     * @param confirmed      true = admin confirmed the action
     */
    public ActionResult toggleEnrollment(int semesterId, String semesterName,
                                         boolean isActive, boolean openEnrollment,
                                         boolean confirmed) {
        if (!confirmed)
            return new ActionResult(false, "Action cancelled.");

        if (!isActive)
            return new ActionResult(false,
                "Enrollment can only be changed for the active semester.");

        try {
            boolean updated = dao.setEnrollmentOpen(semesterId, openEnrollment);
            if (!updated)
                return new ActionResult(false, "Semester not found in the database.");
        } catch (SQLException e) {
            return new ActionResult(false,
                "Failed to update enrollment status: " + e.getMessage());
        }

        String verb = openEnrollment ? "opened" : "closed";
        return new ActionResult(true,
            "Enrollment " + verb + " for \"" + semesterName + "\".");
    }
}
