package model;

/**
 * Represents one timetable slot row from the TIMETABLES table.
 *
 * One slot = one day/time block for a course offering.
 * A Mon+Wed course has two TimetableSlot objects.
 *
 * Used by:
 *   - TimetableDAO     → populated from TIMETABLES + joins
 *   - TimetableService → passed to admin UI after loading
 *   - CreateTimetablesPanel → rendered in the existing-slots table
 */
public class TimetableSlot {

    private final int    timetableId;
    private final int    offeringId;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String teacherName;
    private final String semesterName;
    private final String dayOfWeek;
    private final String startTime;    // "HH:mm" display string
    private final String endTime;      // "HH:mm" display string
    private final String roomNumber;

    public TimetableSlot(int timetableId, int offeringId,
                          String courseCode, String courseName,
                          String section, String teacherName,
                          String semesterName,
                          String dayOfWeek, String startTime,
                          String endTime, String roomNumber) {
        this.timetableId  = timetableId;
        this.offeringId   = offeringId;
        this.courseCode   = courseCode;
        this.courseName   = courseName;
        this.section      = section;
        this.teacherName  = teacherName;
        this.semesterName = semesterName;
        this.dayOfWeek    = dayOfWeek;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.roomNumber   = roomNumber;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int    getTimetableId()  { return timetableId; }
    public int    getOfferingId()   { return offeringId; }
    public String getCourseCode()   { return courseCode; }
    public String getCourseName()   { return courseName; }
    public String getSection()      { return section; }
    public String getTeacherName()  { return teacherName; }
    public String getSemesterName() { return semesterName; }
    public String getDayOfWeek()    { return dayOfWeek; }
    public String getStartTime()    { return startTime; }
    public String getEndTime()      { return endTime; }
    public String getRoomNumber()   { return roomNumber != null ? roomNumber : "—"; }

    /** One-line description for the table e.g. "CS101 (A) · Mon 08:30–10:00 · CS-101" */
    public String getSummary() {
        return courseCode + " (" + section + ")  ·  "
             + dayOfWeek + "  " + startTime + "–" + endTime
             + "  ·  " + getRoomNumber();
    }

    @Override
    public String toString() { return getSummary(); }
}