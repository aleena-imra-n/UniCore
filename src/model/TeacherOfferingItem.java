package model;

/**
 * Lightweight model for one course offering shown in the teacher's
 * "Post Announcement" dropdown.
 *
 * Carries the offeringId needed for the ANNOUNCEMENTS.offering_id FK,
 * along with display fields for the combo box label.
 */
public class TeacherOfferingItem {

    private final int    offeringId;
    private final String courseCode;
    private final String courseName;
    private final String section;

    public TeacherOfferingItem(int offeringId, String courseCode,
                               String courseName, String section) {
        this.offeringId = offeringId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.section    = section;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getOfferingId() { return offeringId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getSection()    { return section; }

    /** Label shown in the JComboBox. */
    @Override
    public String toString() {
        return courseCode + "  |  " + courseName + " (Sec " + section + ")";
    }
}
