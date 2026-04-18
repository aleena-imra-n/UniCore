package model;

/**
 * One row from VW_REPORT_ATTENDANCE.
 * Shows average attendance % per course section plus count of students below 75%.
 */
public class AttendanceReportRow {

    private final String semesterName;
    private final String deptName;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String teacherName;
    private final int    totalSessions;
    private final int    studentCount;
    private final double avgAttendancePct;
    private final int    studentsBelowThreshold;

    public AttendanceReportRow(String semesterName, String deptName,
                               String courseCode, String courseName,
                               String section, String teacherName,
                               int totalSessions, int studentCount,
                               double avgAttendancePct, int studentsBelowThreshold) {
        this.semesterName           = semesterName;
        this.deptName               = deptName;
        this.courseCode             = courseCode;
        this.courseName             = courseName;
        this.section                = section;
        this.teacherName            = teacherName;
        this.totalSessions          = totalSessions;
        this.studentCount           = studentCount;
        this.avgAttendancePct       = avgAttendancePct;
        this.studentsBelowThreshold = studentsBelowThreshold;
    }

    public String getSemesterName()          { return semesterName; }
    public String getDeptName()              { return deptName; }
    public String getCourseCode()            { return courseCode; }
    public String getCourseName()            { return courseName; }
    public String getSection()               { return section; }
    public String getTeacherName()           { return teacherName; }
    public int    getTotalSessions()         { return totalSessions; }
    public int    getStudentCount()          { return studentCount; }
    public double getAvgAttendancePct()      { return avgAttendancePct; }
    public int    getStudentsBelowThreshold(){ return studentsBelowThreshold; }
}
