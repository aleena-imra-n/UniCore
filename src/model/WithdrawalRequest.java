package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents one course-withdrawal request row.
 *
 * Used by:
 *   - WithdrawalDAO     → populated from WITHDRAWAL_REQUESTS + joins
 *   - WithdrawalService → passed to both student and admin UIs
 *   - CourseWithdrawalPanel  → student view (my requests)
 *   - AdminWithdrawalPanel   → admin view (all pending requests)
 */
public class WithdrawalRequest {

    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a");

    private final int           requestId;
    private final int           enrollmentId;
    private final int           studentId;
    private final String        studentName;
    private final String        rollNumber;
    private final String        courseCode;
    private final String        courseName;
    private final String        section;
    private final String        semesterName;
    private final String        reason;
    private final String        status;          // "pending" | "approved" | "rejected"
    private final String        adminComment;    // may be null
    private final LocalDateTime requestedAt;
    private final LocalDateTime reviewedAt;      // may be null

    public WithdrawalRequest(int requestId, int enrollmentId, int studentId,
                              String studentName, String rollNumber,
                              String courseCode, String courseName,
                              String section, String semesterName,
                              String reason, String status, String adminComment,
                              LocalDateTime requestedAt, LocalDateTime reviewedAt) {
        this.requestId    = requestId;
        this.enrollmentId = enrollmentId;
        this.studentId    = studentId;
        this.studentName  = studentName;
        this.rollNumber   = rollNumber;
        this.courseCode   = courseCode;
        this.courseName   = courseName;
        this.section      = section;
        this.semesterName = semesterName;
        this.reason       = reason;
        this.status       = status;
        this.adminComment = adminComment;
        this.requestedAt  = requestedAt;
        this.reviewedAt   = reviewedAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int           getRequestId()    { return requestId; }
    public int           getEnrollmentId() { return enrollmentId; }
    public int           getStudentId()    { return studentId; }
    public String        getStudentName()  { return studentName; }
    public String        getRollNumber()   { return rollNumber; }
    public String        getCourseCode()   { return courseCode; }
    public String        getCourseName()   { return courseName; }
    public String        getSection()      { return section; }
    public String        getSemesterName() { return semesterName; }
    public String        getReason()       { return reason; }
    public String        getStatus()       { return status; }
    public String        getAdminComment() { return adminComment; }
    public LocalDateTime getRequestedAt()  { return requestedAt; }
    public LocalDateTime getReviewedAt()   { return reviewedAt; }

    public boolean isPending()  { return "pending".equalsIgnoreCase(status); }
    public boolean isApproved() { return "approved".equalsIgnoreCase(status); }
    public boolean isRejected() { return "rejected".equalsIgnoreCase(status); }

    public String getFormattedRequestedAt() {
        return requestedAt != null ? requestedAt.format(DISPLAY_FMT) : "—";
    }

    /** One-line display label. */
    public String getCourseLabel() {
        return courseCode + "  ·  " + courseName + "  (Sec " + section + ")";
    }

    @Override
    public String toString() {
        return "WithdrawalRequest{id=" + requestId
               + ", course=" + courseCode
               + ", status=" + status + "}";
    }
}