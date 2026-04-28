package model;

/**
 * Represents the attendance summary for one enrolled course.
 *
 * Used by:
 *   - StudentAttendanceDAO     → populated from ATTENDANCE + ENROLLMENTS + COURSES
 *   - StudentAttendanceService → passed to the UI after loading
 *   - StudentAttendancePanel   → renders each course row/card
 */
public class StudentAttendanceItem {

    // ── Eligibility threshold ─────────────────────────────────────────────────
    /** Minimum attendance % required to be eligible for exams. */
    public static final int ELIGIBILITY_THRESHOLD = 75;

    private final int    enrollmentId;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String teacherName;
    private final int    totalClasses;    // distinct class_date rows for this enrollment
    private final int    classesAttended; // rows where status = 'present' OR 'late'
    private final int    classesAbsent;   // rows where status = 'absent'
    private final int    classesLate;     // rows where status = 'late'

    public StudentAttendanceItem(int enrollmentId,
                                 String courseCode, String courseName,
                                 String section, String teacherName,
                                 int totalClasses, int classesAttended,
                                 int classesAbsent, int classesLate) {
        this.enrollmentId   = enrollmentId;
        this.courseCode     = courseCode;
        this.courseName     = courseName;
        this.section        = section;
        this.teacherName    = teacherName;
        this.totalClasses   = totalClasses;
        this.classesAttended = classesAttended;
        this.classesAbsent  = classesAbsent;
        this.classesLate    = classesLate;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int    getEnrollmentId()    { return enrollmentId; }
    public String getCourseCode()      { return courseCode; }
    public String getCourseName()      { return courseName; }
    public String getSection()         { return section; }
    public String getTeacherName()     { return teacherName; }
    public int    getTotalClasses()    { return totalClasses; }
    public int    getClassesAttended() { return classesAttended; }
    public int    getClassesAbsent()   { return classesAbsent; }
    public int    getClassesLate()     { return classesLate; }

    /**
     * Attendance percentage = classesAttended / totalClasses * 100.
     * Returns 0 if no classes have been recorded yet.
     */
    public double getAttendancePct() {
        if (totalClasses == 0) return 0.0;
        return (classesAttended * 100.0) / totalClasses;
    }

    /** Formatted percentage string e.g. "87.5%" */
    public String getAttendancePctStr() {
        if (totalClasses == 0) return "N/A";
        return String.format("%.1f%%", getAttendancePct());
    }

    /**
     * Returns true if the student meets the minimum attendance threshold
     * and is eligible to sit the exam.
     */
    public boolean isEligible() {
        return getAttendancePct() >= ELIGIBILITY_THRESHOLD;
    }

    /**
     * How many more classes the student must attend to reach the threshold.
     * Returns 0 if already eligible.
     */
    public int classesNeededForEligibility() {
        if (isEligible()) return 0;
        // Solve: (attended + x) / (total + x) >= threshold/100
        // x >= (threshold*total - 100*attended) / (100 - threshold)
        double threshold = ELIGIBILITY_THRESHOLD / 100.0;
        int needed = (int) Math.ceil(
            (threshold * totalClasses - classesAttended) / (1 - threshold));
        return Math.max(0, needed);
    }

    @Override
    public String toString() {
        return courseCode + " — " + courseName
               + " (" + getAttendancePctStr() + ")";
    }
}