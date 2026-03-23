package model;

/**
 * Lightweight view model for one student row in the attendance roster.
 * Contains only the data needed to render and operate the UI row.
 */
public class RosterStudent {

    private final int    studentId;
    private final int    enrollmentId;   // needed for DB INSERT / UPDATE
    private final String fullName;
    private final String rollNumber;
    private final String subDeptCode;
    private final int    attendancePct;  // historical % for the warning colour

    public RosterStudent(int studentId, int enrollmentId,
                         String fullName, String rollNumber,
                         String subDeptCode, int attendancePct) {
        this.studentId     = studentId;
        this.enrollmentId  = enrollmentId;
        this.fullName      = fullName;
        this.rollNumber    = rollNumber;
        this.subDeptCode   = subDeptCode;
        this.attendancePct = attendancePct;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getStudentId()     { return studentId; }
    public int    getEnrollmentId()  { return enrollmentId; }
    public String getFullName()      { return fullName; }
    public String getRollNumber()    { return rollNumber; }
    public String getSubDeptCode()   { return subDeptCode; }
    public int    getAttendancePct() { return attendancePct; }

    @Override
    public String toString() {
        return fullName + " (" + rollNumber + ")";
    }
}
