package model;

/**
 * Represents one course offering assigned to a teacher for the current semester.
 *
 * Used by:
 *   - DAO     → populated from COURSE_OFFERINGS + COURSES + SEMESTERS + ENROLLMENTS
 *   - Service → passed to UI after loading
 *   - UI      → renders each course card
 */
public class TeacherCourse {

    private final int    offeringId;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String scheduleDays;
    private final String classTime;
    private final String roomNumber;
    private final int    maxCapacity;
    private final int    enrolledCount;   // active enrollments for this offering

    public TeacherCourse(int offeringId, String courseCode, String courseName,
                         String section, String scheduleDays, String classTime,
                         String roomNumber, int maxCapacity, int enrolledCount) {
        this.offeringId    = offeringId;
        this.courseCode    = courseCode;
        this.courseName    = courseName;
        this.section       = section;
        this.scheduleDays  = scheduleDays;
        this.classTime     = classTime;
        this.roomNumber    = roomNumber;
        this.maxCapacity   = maxCapacity;
        this.enrolledCount = enrolledCount;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getOfferingId()    { return offeringId; }
    public String getCourseCode()    { return courseCode; }
    public String getCourseName()    { return courseName; }
    public String getSection()       { return section; }
    public String getScheduleDays()  { return scheduleDays; }
    public String getClassTime()     { return classTime; }
    public String getRoomNumber()    { return roomNumber; }
    public int    getMaxCapacity()   { return maxCapacity; }
    public int    getEnrolledCount() { return enrolledCount; }

    /** Convenience: how many seats are still open. */
    public int getAvailableSeats() {
        return Math.max(0, maxCapacity - enrolledCount);
    }

    /** Convenience label for the card's meta row. */
    public String getScheduleLabel() {
        return scheduleDays + "  " + classTime + "  ·  " + roomNumber;
    }

    @Override
    public String toString() {
        return courseCode + " — " + courseName + " (Sec " + section + ")";
    }
}
