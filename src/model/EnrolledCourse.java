package model;

import java.time.LocalDate;

/**
 * Represents one course that a student is actively enrolled in.
 *
 * Used by:
 *   - DAO     → populated from ENROLLMENTS + COURSES + COURSE_OFFERINGS
 *   - Service → returned to UI for the "Enrolled Courses" panel
 *   - UI      → renders each row in the enrolled list
 */
public class EnrolledCourse {

    private final int       enrollmentId;
    private final int       offeringId;
    private final String    courseCode;
    private final String    courseName;
    private final int       creditHours;
    private final String    section;
    private final String    teacherName;
    private final LocalDate enrollmentDate;
    private final String    status;         // "active" | "withdrawn" | "completed"

    public EnrolledCourse(int enrollmentId, int offeringId,
                          String courseCode, String courseName,
                          int creditHours, String section,
                          String teacherName, LocalDate enrollmentDate,
                          String status) {
        this.enrollmentId   = enrollmentId;
        this.offeringId     = offeringId;
        this.courseCode     = courseCode;
        this.courseName     = courseName;
        this.creditHours    = creditHours;
        this.section        = section;
        this.teacherName    = teacherName;
        this.enrollmentDate = enrollmentDate;
        this.status         = status;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int       getEnrollmentId()   { return enrollmentId; }
    public int       getOfferingId()     { return offeringId; }
    public String    getCourseCode()     { return courseCode; }
    public String    getCourseName()     { return courseName; }
    public int       getCreditHours()    { return creditHours; }
    public String    getSection()        { return section; }
    public String    getTeacherName()    { return teacherName; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public String    getStatus()         { return status; }

    @Override
    public String toString() {
        return courseCode + " — " + courseName + " (Sec " + section + ")";
    }
}
