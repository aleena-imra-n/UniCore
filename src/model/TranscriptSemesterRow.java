package model;

/**
 * Represents one semester's GPA summary in a student's transcript.
 *
 * Populated from RS2 of SP_GetTranscript.
 * One row per semester that has at least one graded course.
 *
 * Used by:
 *   - TranscriptDAO      → populated from SP result set 2
 *   - TranscriptService  → passed to UI after loading
 *   - StudentTranscriptPanel → rendered as the GPA footer of each semester block
 */
public class TranscriptSemesterRow {

    private final int    semesterId;
    private final String semesterName;
    private final int    courseCount;
    private final int    semCreditHours;
    private final double semQualityPoints;
    private final double semesterGpa;     // GPA for this semester only
    private final double cgpaSoFar;       // Cumulative GPA up to and including this semester

    public TranscriptSemesterRow(int semesterId, String semesterName,
                                  int courseCount, int semCreditHours,
                                  double semQualityPoints, double semesterGpa,
                                  double cgpaSoFar) {
        this.semesterId       = semesterId;
        this.semesterName     = semesterName;
        this.courseCount      = courseCount;
        this.semCreditHours   = semCreditHours;
        this.semQualityPoints = semQualityPoints;
        this.semesterGpa      = semesterGpa;
        this.cgpaSoFar        = cgpaSoFar;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getSemesterId()       { return semesterId; }
    public String getSemesterName()     { return semesterName; }
    public int    getCourseCount()      { return courseCount; }
    public int    getSemCreditHours()   { return semCreditHours; }
    public double getSemQualityPoints() { return semQualityPoints; }
    public double getSemesterGpa()      { return semesterGpa; }
    public double getCgpaSoFar()        { return cgpaSoFar; }

    /** Formatted semester GPA string e.g. "3.45" */
    public String getSemesterGpaStr() {
        return String.format("%.2f", semesterGpa);
    }

    /** Formatted CGPA string e.g. "3.52" */
    public String getCgpaSoFarStr() {
        return String.format("%.2f", cgpaSoFar);
    }

    @Override
    public String toString() {
        return semesterName + " — GPA: " + getSemesterGpaStr()
               + " | CGPA: " + getCgpaSoFarStr();
    }
}