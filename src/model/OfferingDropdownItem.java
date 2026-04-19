package model;

/**
 * Lightweight view model for one course offering in the timetable
 * admin form's dropdown.
 *
 * Carries enough display text and the offering_id needed by the DAO.
 *
 * Used by:
 *   - TimetableDAO             → populated from COURSE_OFFERINGS + joins
 *   - TimetableService         → passed to UI for the combo box
 *   - CreateTimetablesPanel    → rendered in the offering JComboBox
 */
public class OfferingDropdownItem {

    private final int    offeringId;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String teacherName;
    private final String semesterName;
    private final String subDeptCode;

    public OfferingDropdownItem(int offeringId, String courseCode,
                                 String courseName, String section,
                                 String teacherName, String semesterName,
                                 String subDeptCode) {
        this.offeringId   = offeringId;
        this.courseCode   = courseCode;
        this.courseName   = courseName;
        this.section      = section;
        this.teacherName  = teacherName;
        this.semesterName = semesterName;
        this.subDeptCode  = subDeptCode;
    }

    public int    getOfferingId()   { return offeringId; }
    public String getCourseCode()   { return courseCode; }
    public String getCourseName()   { return courseName; }
    public String getSection()      { return section; }
    public String getTeacherName()  { return teacherName; }
    public String getSemesterName() { return semesterName; }
    public String getSubDeptCode()  { return subDeptCode; }

    /** Label shown in JComboBox. */
    @Override
    public String toString() {
        return courseCode + "  |  " + courseName
             + "  (Sec " + section + " — " + subDeptCode + ")";
    }
}