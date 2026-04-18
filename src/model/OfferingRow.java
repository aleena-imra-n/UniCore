package model;

/**
 * View-model for one row in the Assign Teachers table.
 *
 * Carries every column shown in the UI plus the PKs needed for
 * assign / remove operations.
 *
 * Columns displayed:
 *   Course Code | Course Name | Section | Department | Credits | Assigned Teacher | Action
 */
public class OfferingRow {

    private final int    offeringId;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String deptName;
    private final int    creditHours;

    // Nullable — null or empty means "Unassigned"
    private       Integer teacherId;
    private       String  teacherName;

    public OfferingRow(int offeringId, String courseCode, String courseName,
                       String section, String deptName, int creditHours,
                       Integer teacherId, String teacherName) {
        this.offeringId  = offeringId;
        this.courseCode  = courseCode;
        this.courseName  = courseName;
        this.section     = section;
        this.deptName    = deptName;
        this.creditHours = creditHours;
        this.teacherId   = teacherId;
        this.teacherName = teacherName;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int     getOfferingId()  { return offeringId; }
    public String  getCourseCode()  { return courseCode; }
    public String  getCourseName()  { return courseName; }
    public String  getSection()     { return section; }
    public String  getDeptName()    { return deptName; }
    public int     getCreditHours() { return creditHours; }
    public Integer getTeacherId()   { return teacherId; }
    public String  getTeacherName() { return teacherName; }

    public boolean isAssigned() {
        return teacherId != null && teacherId > 0;
    }

    // ── Mutators (updated after assign / remove) ──────────────────────────────
    public void setTeacherId(Integer teacherId)     { this.teacherId   = teacherId; }
    public void setTeacherName(String teacherName)  { this.teacherName = teacherName; }

    @Override
    public String toString() {
        return courseCode + " — " + courseName + " (" + section + ")";
    }
}
