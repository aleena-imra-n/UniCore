package model;

/**
 * Represents one course offering shown in the combo box.
 * Used by the UI to display and by the BL/DAO to identify the offering.
 */
public class OfferingItem {

    private final int    offeringId;
    private final String courseCode;
    private final String courseName;
    private final String section;
    private final String subDeptCode;

    public OfferingItem(int offeringId, String courseCode,
                        String courseName, String section, String subDeptCode) {
        this.offeringId  = offeringId;
        this.courseCode  = courseCode;
        this.courseName  = courseName;
        this.section     = section;
        this.subDeptCode = subDeptCode;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getOfferingId()  { return offeringId; }
    public String getCourseCode()  { return courseCode; }
    public String getCourseName()  { return courseName; }
    public String getSection()     { return section; }
    public String getSubDeptCode() { return subDeptCode; }

    /** Label shown in the JComboBox. */
    @Override
    public String toString() {
        return courseCode + " — " + courseName
               + " (" + subDeptCode + "-" + section + ")";
    }
}
