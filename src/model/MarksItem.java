package model;

/**
 * Represents one row in the MARKS table.
 * Used by DAO → Service → UI.
 */
public class MarksItem {

    private final int    markId;
    private final int    enrollmentId;
    private final String studentName;
    private final String rollNumber;
    private final String assessmentType;   // quiz|assignment|mid|final|lab|project
    private final double marksObtained;
    private final double totalMarks;
    private final String remarks;

    public MarksItem(int markId, int enrollmentId,
                     String studentName, String rollNumber,
                     String assessmentType,
                     double marksObtained, double totalMarks,
                     String remarks) {
        this.markId         = markId;
        this.enrollmentId   = enrollmentId;
        this.studentName    = studentName;
        this.rollNumber     = rollNumber;
        this.assessmentType = assessmentType;
        this.marksObtained  = marksObtained;
        this.totalMarks     = totalMarks;
        this.remarks        = remarks == null ? "" : remarks;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getMarkId()         { return markId; }
    public int    getEnrollmentId()   { return enrollmentId; }
    public String getStudentName()    { return studentName; }
    public String getRollNumber()     { return rollNumber; }
    public String getAssessmentType() { return assessmentType; }
    public double getMarksObtained()  { return marksObtained; }
    public double getTotalMarks()     { return totalMarks; }
    public String getRemarks()        { return remarks; }

    /** Percentage score for this mark entry. */
    public double getPercentage() {
        return totalMarks > 0 ? (marksObtained / totalMarks) * 100.0 : 0.0;
    }

    @Override
    public String toString() {
        return rollNumber + " — " + assessmentType
               + " (" + marksObtained + "/" + totalMarks + ")";
    }
}