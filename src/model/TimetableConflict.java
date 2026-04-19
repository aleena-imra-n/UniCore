package model;

/**
 * Represents one room-conflict row returned by SP_AddTimetableSlot (RS1).
 *
 * When the SP finds an overlapping slot in the same room on the same day,
 * it returns these rows so the admin UI can display a clear warning.
 *
 * Used by:
 *   - TimetableDAO     → populated from SP result set 1
 *   - TimetableService → included in AddSlotResult when conflicts exist
 *   - CreateTimetablesPanel → shown in the conflict warning dialog
 */
public class TimetableConflict {

    private final int    conflictingOfferingId;
    private final String courseCode;
    private final String section;
    private final String dayOfWeek;
    private final String startTime;
    private final String endTime;
    private final String roomNumber;

    public TimetableConflict(int conflictingOfferingId,
                              String courseCode, String section,
                              String dayOfWeek, String startTime,
                              String endTime, String roomNumber) {
        this.conflictingOfferingId = conflictingOfferingId;
        this.courseCode            = courseCode;
        this.section               = section;
        this.dayOfWeek             = dayOfWeek;
        this.startTime             = startTime;
        this.endTime               = endTime;
        this.roomNumber            = roomNumber;
    }

    public int    getConflictingOfferingId() { return conflictingOfferingId; }
    public String getCourseCode()  { return courseCode; }
    public String getSection()     { return section; }
    public String getDayOfWeek()   { return dayOfWeek; }
    public String getStartTime()   { return startTime; }
    public String getEndTime()     { return endTime; }
    public String getRoomNumber()  { return roomNumber; }

    /** One-line description for the warning dialog. */
    public String describe() {
        return courseCode + " (Sec " + section + ")  ·  "
             + dayOfWeek + "  " + startTime + "–" + endTime
             + "  in room " + roomNumber;
    }

    @Override
    public String toString() { return describe(); }
}