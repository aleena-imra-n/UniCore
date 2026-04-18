package model;

/**
 * Represents one semester entry in the semester filter dropdown.
 * Used by AssignTeachersPanel / AssignTeachersService.
 */
public class SemesterOption {

    private final int    semesterId;
    private final String label;      // e.g.  "Spring 2025  (active)"
    private final boolean active;

    public SemesterOption(int semesterId, String label, boolean active) {
        this.semesterId = semesterId;
        this.label      = label;
        this.active     = active;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int     getSemesterId() { return semesterId; }
    public boolean isActive()      { return active; }

    /** Label shown in the JComboBox. */
    @Override
    public String toString() { return label; }
}
