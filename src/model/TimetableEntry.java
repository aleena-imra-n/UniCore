package model;

/**
 * Represents one timetable slot as displayed in the weekly grid
 * for a teacher or student.
 *
 * A single TIMETABLES row maps to one TimetableEntry.
 * The grid groups entries by day then sorts by start_time.
 *
 * Used by:
 *   - MyTimetableDAO     → populated from TIMETABLES + joins
 *   - MyTimetableService → passed to both timetable view panels
 *   - MyTimetablePanel   → rendered as a slot card in the weekly grid
 */
public class TimetableEntry {

    private final int    timetableId;
    private final int    offeringId;
    private final String dayOfWeek;    // "Monday" … "Friday"
    private final String startTime;    // "HH:mm"
    private final String endTime;      // "HH:mm"
    private final String roomNumber;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String teacherName;  // relevant for student view
    private final String subDeptCode;

    public TimetableEntry(int timetableId, int offeringId,
                           String dayOfWeek, String startTime, String endTime,
                           String roomNumber,
                           String courseCode, String courseName,
                           String section, String teacherName,
                           String subDeptCode) {
        this.timetableId = timetableId;
        this.offeringId  = offeringId;
        this.dayOfWeek   = dayOfWeek;
        this.startTime   = startTime;
        this.endTime     = endTime;
        this.roomNumber  = roomNumber;
        this.courseCode  = courseCode;
        this.courseName  = courseName;
        this.section     = section;
        this.teacherName = teacherName;
        this.subDeptCode = subDeptCode;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int    getTimetableId() { return timetableId; }
    public int    getOfferingId()  { return offeringId; }
    public String getDayOfWeek()   { return dayOfWeek; }
    public String getStartTime()   { return startTime; }
    public String getEndTime()     { return endTime; }
    public String getRoomNumber()  { return roomNumber != null ? roomNumber : "—"; }
    public String getCourseCode()  { return courseCode; }
    public String getCourseName()  { return courseName; }
    public String getSection()     { return section; }
    public String getTeacherName() { return teacherName; }
    public String getSubDeptCode() { return subDeptCode; }

    /** Duration in minutes, e.g. "90 min" */
    public String getDurationLabel() {
        try {
            int sh = Integer.parseInt(startTime.substring(0, 2));
            int sm = Integer.parseInt(startTime.substring(3));
            int eh = Integer.parseInt(endTime.substring(0, 2));
            int em = Integer.parseInt(endTime.substring(3));
            int mins = (eh * 60 + em) - (sh * 60 + sm);
            return mins + " min";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String toString() {
        return courseCode + " (" + section + ")  " + dayOfWeek
             + "  " + startTime + "–" + endTime;
    }
}