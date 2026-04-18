package model;

/**
 * Represents one course row in a student's academic transcript.
 *
 * Populated from RS1 of SP_GetTranscript.
 * One row per enrolled + graded course across all semesters.
 *
 * Used by:
 *   - TranscriptDAO      → populated from SP result set 1
 *   - TranscriptService  → passed to UI after loading
 *   - StudentTranscriptPanel → rendered inside each semester block
 */
public class TranscriptCourseRow {

    private final int     semesterId;
    private final String  semesterName;
    private final String  courseCode;
    private final String  courseName;
    private final int     creditHours;
    private final String  section;
    private final String  teacherName;
    private final double  marksObtained;
    private final double  marksTotal;
    private final double  percentage;
    private final String  letterGrade;
    private final double  gradePoints;    // 4.0-scale value (e.g. 3.70)
    private final double  qualityPoints;  // gradePoints × creditHours

    public TranscriptCourseRow(int semesterId, String semesterName,
                                String courseCode, String courseName,
                                int creditHours, String section, String teacherName,
                                double marksObtained, double marksTotal,
                                double percentage, String letterGrade,
                                double gradePoints, double qualityPoints) {
        this.semesterId    = semesterId;
        this.semesterName  = semesterName;
        this.courseCode    = courseCode;
        this.courseName    = courseName;
        this.creditHours   = creditHours;
        this.section       = section;
        this.teacherName   = teacherName;
        this.marksObtained = marksObtained;
        this.marksTotal    = marksTotal;
        this.percentage    = percentage;
        this.letterGrade   = letterGrade;
        this.gradePoints   = gradePoints;
        this.qualityPoints = qualityPoints;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int     getSemesterId()    { return semesterId; }
    public String  getSemesterName()  { return semesterName; }
    public String  getCourseCode()    { return courseCode; }
    public String  getCourseName()    { return courseName; }
    public int     getCreditHours()   { return creditHours; }
    public String  getSection()       { return section; }
    public String  getTeacherName()   { return teacherName; }
    public double  getMarksObtained() { return marksObtained; }
    public double  getMarksTotal()    { return marksTotal; }
    public double  getPercentage()    { return percentage; }
    public String  getLetterGrade()   { return letterGrade; }
    public double  getGradePoints()   { return gradePoints; }
    public double  getQualityPoints() { return qualityPoints; }

    /** Formatted percentage string e.g. "87.50%" */
    public String getPercentageStr() {
        return String.format("%.2f%%", percentage);
    }

    /** True if the student passed this course (grade != F). */
    public boolean isPassed() {
        return !"F".equals(letterGrade);
    }

    @Override
    public String toString() {
        return courseCode + " — " + letterGrade
               + " (" + String.format("%.2f", percentage) + "%)";
    }
}