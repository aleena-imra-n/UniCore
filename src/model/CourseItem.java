package model;

/**
 * Represents one course in the available-courses catalog.
 *
 * Used by:
 *   - DAO     → populates from COURSES / COURSE_OFFERINGS tables
 *   - Service → passes to UI after eligibility filtering
 *   - UI      → renders the dropdown and info tiles
 */
public class CourseItem {

    private final int    offeringId;    // COURSE_OFFERINGS.offering_id
    private final String courseCode;    // e.g. "CS301"
    private final String courseName;    // e.g. "Database Systems"
    private final int    creditHours;   // e.g. 3
    private final String section;       // e.g. "A"
    private final String teacherName;   // e.g. "Dr. Asif Mehmood"
    private final String scheduleDays;  // e.g. "Mon/Wed"
    private final String classTime;     // e.g. "08:30"

    public CourseItem(int offeringId, String courseCode, String courseName,
                      int creditHours, String section, String teacherName,
                      String scheduleDays, String classTime) {
        this.offeringId   = offeringId;
        this.courseCode   = courseCode;
        this.courseName   = courseName;
        this.creditHours  = creditHours;
        this.section      = section;
        this.teacherName  = teacherName;
        this.scheduleDays = scheduleDays;
        this.classTime    = classTime;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getOfferingId()   { return offeringId; }
    public String getCourseCode()   { return courseCode; }
    public String getCourseName()   { return courseName; }
    public int    getCreditHours()  { return creditHours; }
    public String getSection()      { return section; }
    public String getTeacherName()  { return teacherName; }
    public String getScheduleDays() { return scheduleDays; }
    public String getClassTime()    { return classTime; }

    /** Label shown in the JComboBox dropdown. */
    @Override
    public String toString() {
        return courseCode + "  |  " + courseName;
    }
}
