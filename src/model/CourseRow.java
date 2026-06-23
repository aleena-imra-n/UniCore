package model;

/**
 * View-model for one row in the Manage Courses catalog table.
 * Carries every column shown in the UI plus the PKs needed for
 * edit / deactivate operations.
 */
public class CourseRow {

    private final int     courseId;
    private final int     majorDeptId;
    private final int     categoryId;
    private final String  courseCode;
    private final String  courseName;
    private final int     creditHours;
    private final int     recommendedSemester;  // which semester this course belongs to (1-8)
    private final String  majorDeptName;    // resolved from MAJOR_DEPARTMENTS
    private final String  categoryName;     // resolved from COURSE_CATEGORIES
    /**
     * Comma-separated prereq course codes, e.g. "CS101" or "CS101, CS201".
     * null / empty means no prerequisites — displayed as "No Pre Req".
     */
    private final String  prereqCodes;
    private       boolean active;

    public CourseRow(int courseId, int majorDeptId, int categoryId,
                     String courseCode, String courseName, int creditHours,
                     int recommendedSemester,
                     String majorDeptName, String categoryName,
                     String prereqCodes, boolean active) {
        this.courseId             = courseId;
        this.majorDeptId          = majorDeptId;
        this.categoryId           = categoryId;
        this.courseCode            = courseCode;
        this.courseName           = courseName;
        this.creditHours          = creditHours;
        this.recommendedSemester  = recommendedSemester;
        this.majorDeptName        = majorDeptName;
        this.categoryName         = categoryName;
        this.prereqCodes          = prereqCodes;
        this.active               = active;
        
    }

    // ── Getters ──────────────────────────────────────────────────
    public int     getCourseId()             { return courseId; }
    public int     getMajorDeptId()          { return majorDeptId; }
    public int     getCategoryId()           { return categoryId; }
    public String  getCourseCode()           { return courseCode; }
    public String  getCourseName()           { return courseName; }
    public int     getCreditHours()          { return creditHours; }
    public int     getRecommendedSemester()  { return recommendedSemester; }
    public String  getMajorDeptName()        { return majorDeptName; }
    public String  getCategoryName()         { return categoryName; }
    public boolean isActive()                { return active; }
    public String  getPreReqCode()           { return prereqCodes;}
    /**
     * Returns the prerequisite course codes as a display string.
     * Returns "No Pre Req" when none exist.
     */
    public String getPrereqDisplay() {
        return (prereqCodes == null || prereqCodes.isBlank())
            ? "No Pre Req" : prereqCodes;
    }

    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return courseCode + " — " + courseName;
    }
}

