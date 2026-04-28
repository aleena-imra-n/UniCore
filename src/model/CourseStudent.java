package model;

/**
 * Represents one student enrolled in a teacher's course offering.
 * Used by the "View Students" action button to populate the student list popup.
 */
public class CourseStudent {

    private final int    studentId;
    private final int    enrollmentId;
    private final String fullName;
    private final String rollNumber;
    private final String subDeptCode;
    private final int    attendancePct;    // running attendance % for this offering
    private final String enrollmentStatus; // "active" | "withdrawn" | "completed"

    public CourseStudent(int studentId, int enrollmentId,
                         String fullName, String rollNumber,
                         String subDeptCode, int attendancePct,
                         String enrollmentStatus) {
        this.studentId        = studentId;
        this.enrollmentId     = enrollmentId;
        this.fullName         = fullName;
        this.rollNumber       = rollNumber;
        this.subDeptCode      = subDeptCode;
        this.attendancePct    = attendancePct;
        this.enrollmentStatus = enrollmentStatus;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getStudentId()        { return studentId; }
    public int    getEnrollmentId()     { return enrollmentId; }
    public String getFullName()         { return fullName; }
    public String getRollNumber()       { return rollNumber; }
    public String getSubDeptCode()      { return subDeptCode; }
    public int    getAttendancePct()    { return attendancePct; }
    public String getEnrollmentStatus() { return enrollmentStatus; }

    @Override
    public String toString() {
        return fullName + " (" + rollNumber + ")";
    }
}
