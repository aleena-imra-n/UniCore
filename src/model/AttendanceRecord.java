package model;

import java.time.LocalDate;

/**
 * Represents one student's attendance on one class date.
 * Shared between DAO, BL, and UI layers.
 */
public class AttendanceRecord {

    public enum Status { PRESENT, ABSENT, LATE, UNMARKED }

    private final int       enrollmentId;
    private final int       studentId;
    private final LocalDate classDate;
    private       Status    status;
    private       String    remarks;

    public AttendanceRecord(int enrollmentId, int studentId,
                            LocalDate classDate, Status status, String remarks) {
        this.enrollmentId = enrollmentId;
        this.studentId    = studentId;
        this.classDate    = classDate;
        this.status       = status;
        this.remarks      = remarks;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public int       getEnrollmentId() { return enrollmentId; }
    public int       getStudentId()    { return studentId; }
    public LocalDate getClassDate()    { return classDate; }
    public Status    getStatus()       { return status; }
    public String    getRemarks()      { return remarks; }

    public void setStatus(Status status)   { this.status  = status; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    /** Converts the DB string ("present"/"absent"/"late") to enum. */
    public static Status fromDbString(String s) {
        if (s == null) return Status.UNMARKED;
        return switch (s.toLowerCase()) {
            case "present" -> Status.PRESENT;
            case "absent"  -> Status.ABSENT;
            case "late"    -> Status.LATE;
            default        -> Status.UNMARKED;
        };
    }

    /** Converts enum to the DB-expected lowercase string. */
    public static String toDbString(Status s) {
        return switch (s) {
            case PRESENT  -> "present";
            case ABSENT   -> "absent";
            case LATE     -> "late";
            default       -> "present"; // fallback – BL should never pass UNMARKED
        };
    }

    @Override
    public String toString() {
        return "AttendanceRecord{enrollmentId=" + enrollmentId
               + ", studentId=" + studentId
               + ", classDate=" + classDate
               + ", status=" + status + "}";
    }
}
