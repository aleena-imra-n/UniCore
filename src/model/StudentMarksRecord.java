package model;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents one course's full marks breakdown for a student.
 * Contains a list of MarksItem (one per assessment type recorded)
 * plus computed weighted total and letter grade.
 *
 * Used by DAO → Service → UI (StudentMarksPanel).
 */
public class StudentMarksRecord {

    private final int          enrollmentId;
    private final String       courseCode;
    private final String       courseName;
    private final int          creditHours;
    private final String       section;
    private final String       teacherName;
    private final List<MarksItem> components;   // one per assessment type

    // Computed by service
    private final double totalObtained;
    private final double totalPossible;
    private final double percentage;
    private final String letterGrade;

    public StudentMarksRecord(int enrollmentId,
                               String courseCode, String courseName,
                               int creditHours, String section,
                               String teacherName,
                               List<MarksItem> components,
                               double totalObtained, double totalPossible,
                               double percentage, String letterGrade) {
        this.enrollmentId  = enrollmentId;
        this.courseCode    = courseCode;
        this.courseName    = courseName;
        this.creditHours   = creditHours;
        this.section       = section;
        this.teacherName   = teacherName;
        this.components    = components == null ? new ArrayList<>() : components;
        this.totalObtained = totalObtained;
        this.totalPossible = totalPossible;
        this.percentage    = percentage;
        this.letterGrade   = letterGrade;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int              getEnrollmentId()  { return enrollmentId; }
    public String           getCourseCode()    { return courseCode; }
    public String           getCourseName()    { return courseName; }
    public int              getCreditHours()   { return creditHours; }
    public String           getSection()       { return section; }
    public String           getTeacherName()   { return teacherName; }
    public List<MarksItem>  getComponents()    { return components; }
    public double           getTotalObtained() { return totalObtained; }
    public double           getTotalPossible() { return totalPossible; }
    public double           getPercentage()    { return percentage; }
    public String           getLetterGrade()   { return letterGrade; }

    @Override
    public String toString() {
        return courseCode + " — " + letterGrade + " (" + percentage + "%)";
    }
}