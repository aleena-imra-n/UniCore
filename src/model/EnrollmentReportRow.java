package model;

/**
 * One row from VW_REPORT_ENROLLMENT.
 * Shows total students per course section per semester.
 */
public class EnrollmentReportRow {

    private final String semesterName;
    private final String deptName;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String teacherName;
    private final int    totalEnrolled;
    private final int    activeCount;
    private final int    withdrawnCount;
    private final int    completedCount;

    public EnrollmentReportRow(String semesterName, String deptName,
                               String courseCode, String courseName,
                               String section, String teacherName,
                               int totalEnrolled, int activeCount,
                               int withdrawnCount, int completedCount) {
        this.semesterName   = semesterName;
        this.deptName       = deptName;
        this.courseCode     = courseCode;
        this.courseName     = courseName;
        this.section        = section;
        this.teacherName    = teacherName;
        this.totalEnrolled  = totalEnrolled;
        this.activeCount    = activeCount;
        this.withdrawnCount = withdrawnCount;
        this.completedCount = completedCount;
    }

    public String getSemesterName()   { return semesterName; }
    public String getDeptName()       { return deptName; }
    public String getCourseCode()     { return courseCode; }
    public String getCourseName()     { return courseName; }
    public String getSection()        { return section; }
    public String getTeacherName()    { return teacherName; }
    public int    getTotalEnrolled()  { return totalEnrolled; }
    public int    getActiveCount()    { return activeCount; }
    public int    getWithdrawnCount() { return withdrawnCount; }
    public int    getCompletedCount() { return completedCount; }
}
