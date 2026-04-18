package model;

/**
 * One row from VW_REPORT_ACADEMIC_PERFORMANCE.
 * Shows average / min / max percentage per course section per semester.
 */
public class AcademicPerformanceRow {

    private final String semesterName;
    private final String deptName;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final int    studentCount;
    private final double avgPercentage;
    private final double minPercentage;
    private final double maxPercentage;

    public AcademicPerformanceRow(String semesterName, String deptName,
                                  String courseCode, String courseName,
                                  String section, int studentCount,
                                  double avgPercentage, double minPercentage,
                                  double maxPercentage) {
        this.semesterName  = semesterName;
        this.deptName      = deptName;
        this.courseCode    = courseCode;
        this.courseName    = courseName;
        this.section       = section;
        this.studentCount  = studentCount;
        this.avgPercentage = avgPercentage;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
    }

    public String getSemesterName()  { return semesterName; }
    public String getDeptName()      { return deptName; }
    public String getCourseCode()    { return courseCode; }
    public String getCourseName()    { return courseName; }
    public String getSection()       { return section; }
    public int    getStudentCount()  { return studentCount; }
    public double getAvgPercentage() { return avgPercentage; }
    public double getMinPercentage() { return minPercentage; }
    public double getMaxPercentage() { return maxPercentage; }
}
