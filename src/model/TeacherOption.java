package model;

/**
 * Represents one active teacher entry in the "Assign Teacher" dropdown.
 * Used by AssignTeachersPanel / AssignTeachersService.
 */
public class TeacherOption {

    private final int    teacherId;
    private final String fullName;
    private final String employeeCode;  // e.g. "T-042"

    public TeacherOption(int teacherId, String fullName, String employeeCode) {
        this.teacherId    = teacherId;
        this.fullName     = fullName;
        this.employeeCode = employeeCode;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getTeacherId()    { return teacherId; }
    public String getFullName()     { return fullName; }
    public String getEmployeeCode() { return employeeCode; }

    /** Label shown in the JComboBox. */
    @Override
    public String toString() {
        return fullName + "  [" + employeeCode + "]";
    }
}
