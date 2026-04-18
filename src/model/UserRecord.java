package model;

/**
 * Flat view-model for one user row in the admin Manage Users list.
 *
 * Joins USERS + role profile table (STUDENTS or TEACHERS) + SUB_DEPARTMENTS.
 * Carries every field the admin panel needs to display, search, and pre-fill
 * the edit form — without exposing the password hash.
 *
 * Used by:
 *   - UserManagementDAO     → populated from query results
 *   - UserManagementService → passed to UI after filtering
 *   - ManageUsersPanel      → rendered in the user table and edit form
 */
public class UserRecord {

    private final int     userId;
    private final String  username;
    private final String  role;           // "student" | "teacher"
    private final String  email;
    private final boolean active;

    // ── Profile fields (student) ──────────────────────────────────────────────
    private final String  fullName;
    private final String  rollNumber;     // null for teachers
    private final Integer batchYear;      // null for teachers
    private final Integer currentSemester;// null for teachers

    // ── Profile fields (teacher) ──────────────────────────────────────────────
    private final String  employeeCode;   // null for students
    private final String  designation;    // null for students

    // ── Shared dept fields ────────────────────────────────────────────────────
    private final int     subDeptId;
    private final String  subDeptName;
    private final String  subDeptCode;
    private final int     majorDeptId;
    private final String  majorDeptName;

    public UserRecord(int userId, String username, String role, String email,
                      boolean active, String fullName,
                      String rollNumber, Integer batchYear, Integer currentSemester,
                      String employeeCode, String designation,
                      int subDeptId, String subDeptName, String subDeptCode,
                      int majorDeptId, String majorDeptName) {
        this.userId          = userId;
        this.username        = username;
        this.role            = role;
        this.email           = email;
        this.active          = active;
        this.fullName        = fullName;
        this.rollNumber      = rollNumber;
        this.batchYear       = batchYear;
        this.currentSemester = currentSemester;
        this.employeeCode    = employeeCode;
        this.designation     = designation;
        this.subDeptId       = subDeptId;
        this.subDeptName     = subDeptName;
        this.subDeptCode     = subDeptCode;
        this.majorDeptId     = majorDeptId;
        this.majorDeptName   = majorDeptName;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int     getUserId()          { return userId; }
    public String  getUsername()        { return username; }
    public String  getRole()            { return role; }
    public String  getEmail()           { return email; }
    public boolean isActive()           { return active; }
    public String  getFullName()        { return fullName; }
    public String  getRollNumber()      { return rollNumber; }
    public Integer getBatchYear()       { return batchYear; }
    public Integer getCurrentSemester() { return currentSemester; }
    public String  getEmployeeCode()    { return employeeCode; }
    public String  getDesignation()     { return designation; }
    public int     getSubDeptId()       { return subDeptId; }
    public String  getSubDeptName()     { return subDeptName; }
    public String  getSubDeptCode()     { return subDeptCode; }
    public int     getMajorDeptId()     { return majorDeptId; }
    public String  getMajorDeptName()   { return majorDeptName; }

    public boolean isStudent() { return "student".equalsIgnoreCase(role); }
    public boolean isTeacher() { return "teacher".equalsIgnoreCase(role); }

    /** One-line display used in search results. */
    public String getDisplayLine() {
        return fullName + "  ·  " + username
               + (isStudent() ? "  ·  " + rollNumber : "  ·  " + employeeCode);
    }

    @Override
    public String toString() {
        return fullName + " (" + username + ")";
    }
}