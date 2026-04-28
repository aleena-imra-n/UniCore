package model;

/**
 * Lightweight search result for the admin transcript search list.
 *
 * Contains just enough to display the row and identify the student
 * for a subsequent SP_GetTranscript call.
 *
 * Used by:
 *   - IssueTranscriptDAO     → populated from STUDENTS + USERS + SUB_DEPARTMENTS
 *   - IssueTranscriptService → passed directly to UI search list
 *   - IssueTranscriptsPanel  → rendered as one row in the results list
 */
public class StudentSearchResult {

    private final int    studentId;
    private final String fullName;
    private final String rollNumber;
    private final String username;
    private final String email;
    private final String subDeptName;
    private final String subDeptCode;
    private final String majorDeptName;
    private final int    batchYear;
    private final int    currentSemester;
    private final boolean active;

    public StudentSearchResult(int studentId, String fullName, String rollNumber,
                                String username, String email,
                                String subDeptName, String subDeptCode,
                                String majorDeptName,
                                int batchYear, int currentSemester,
                                boolean active) {
        this.studentId       = studentId;
        this.fullName        = fullName;
        this.rollNumber      = rollNumber;
        this.username        = username;
        this.email           = email;
        this.subDeptName     = subDeptName;
        this.subDeptCode     = subDeptCode;
        this.majorDeptName   = majorDeptName;
        this.batchYear       = batchYear;
        this.currentSemester = currentSemester;
        this.active          = active;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int     getStudentId()       { return studentId; }
    public String  getFullName()        { return fullName; }
    public String  getRollNumber()      { return rollNumber; }
    public String  getUsername()        { return username; }
    public String  getEmail()           { return email; }
    public String  getSubDeptName()     { return subDeptName; }
    public String  getSubDeptCode()     { return subDeptCode; }
    public String  getMajorDeptName()   { return majorDeptName; }
    public int     getBatchYear()       { return batchYear; }
    public int     getCurrentSemester() { return currentSemester; }
    public boolean isActive()           { return active; }

    /** One-line summary shown in the search results list. */
    public String getSummaryLine() {
        return rollNumber + "  ·  " + subDeptCode + "  ·  Batch " + batchYear;
    }

    @Override
    public String toString() {
        return fullName + " (" + rollNumber + ")";
    }
}