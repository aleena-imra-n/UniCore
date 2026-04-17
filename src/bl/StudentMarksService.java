package bl;

import dao.StudentMarksDAO;
import model.MarksItem;
import model.StudentMarksRecord;

import java.sql.SQLException;
import java.util.*;

/**
 * Business-Logic layer for the Student Marks View feature.
 *
 * Responsibilities:
 *   - Resolve username → student_id
 *   - Aggregate marks per course (sum obtained / sum total → percentage)
 *   - Apply letter grade + GPA point scale
 *   - Compute overall CGPA across all courses
 *   - Return typed StudentMarksRecord objects to the UI
 *
 * Grading scale (percentage → letter grade → grade points):
 *   >= 90  → A+  → 4.0
 *   >= 85  → A   → 4.0
 *   >= 80  → A-  → 3.7
 *   >= 75  → B+  → 3.3
 *   >= 70  → B   → 3.0
 *   >= 65  → B-  → 2.7
 *   >= 60  → C+  → 2.3
 *   >= 55  → C   → 2.0
 *   >= 50  → C-  → 1.7
 *   >= 45  → D   → 1.0
 *    < 45  → F   → 0.0
 *
 * Zero Swing code. Zero SQL.
 */
public class StudentMarksService {

    private final StudentMarksDAO dao;
    private int    studentId   = -1;
    private boolean initialised = false;

    public StudentMarksService() {
        this.dao = new StudentMarksDAO();
    }

    public StudentMarksService(StudentMarksDAO dao) {
        this.dao = dao;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean init(String username) {
        try {
            studentId   = dao.getStudentId(username);
            initialised = studentId > 0;
            return initialised;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Returns one StudentMarksRecord per enrolled course,
     * each containing its component marks and computed grade.
     */
    public List<StudentMarksRecord> getMarksRecords() {
        ensureInitialised();
        try {
            Map<Integer, String[]>        enrollInfo = dao.getEnrollmentInfo(studentId);
            Map<Integer, List<MarksItem>> marksMap   = dao.getMarksGrouped(studentId);

            List<StudentMarksRecord> records = new ArrayList<>();

            for (Map.Entry<Integer, String[]> entry : enrollInfo.entrySet()) {
                int      eid        = entry.getKey();
                String[] info       = entry.getValue();
                String   courseCode = info[0];
                String   courseName = info[1];
                int      credits    = Integer.parseInt(info[2]);
                String   section    = info[3];
                String   teacher    = info[4];

                List<MarksItem> components = marksMap.getOrDefault(eid, List.of());

                double totalObtained = components.stream()
                    .mapToDouble(MarksItem::getMarksObtained).sum();
                double totalPossible = components.stream()
                    .mapToDouble(MarksItem::getTotalMarks).sum();

                double pct = totalPossible > 0
                    ? Math.round((totalObtained / totalPossible) * 10000.0) / 100.0
                    : 0.0;

                String grade = components.isEmpty() ? "N/A" : toLetterGrade(pct);

                records.add(new StudentMarksRecord(
                    eid, courseCode, courseName, credits, section, teacher,
                    components, totalObtained, totalPossible, pct, grade
                ));
            }
            return records;

        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Computes the overall CGPA from a list of StudentMarksRecords.
     * Uses credit-hour weighted average of grade points.
     * Excludes courses with no marks yet (grade = "N/A").
     */
    public double computeCGPA(List<StudentMarksRecord> records) {
        double totalPoints  = 0.0;
        int    totalCredits = 0;
        for (StudentMarksRecord r : records) {
            if (r.getLetterGrade().equals("N/A")) continue;
            double gp = toGradePoints(r.getLetterGrade());
            totalPoints  += gp * r.getCreditHours();
            totalCredits += r.getCreditHours();
        }
        if (totalCredits == 0) return 0.0;
        return Math.round((totalPoints / totalCredits) * 100.0) / 100.0;
    }

    // ── Grading helpers ───────────────────────────────────────────────────────

    public static String toLetterGrade(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 85) return "A";
        if (pct >= 80) return "A-";
        if (pct >= 75) return "B+";
        if (pct >= 70) return "B";
        if (pct >= 65) return "B-";
        if (pct >= 60) return "C+";
        if (pct >= 55) return "C";
        if (pct >= 50) return "C-";
        if (pct >= 45) return "D";
        return "F";
    }

    public static double toGradePoints(String grade) {
        return switch (grade) {
            case "A+", "A" -> 4.0;
            case "A-"      -> 3.7;
            case "B+"      -> 3.3;
            case "B"       -> 3.0;
            case "B-"      -> 2.7;
            case "C+"      -> 2.3;
            case "C"       -> 2.0;
            case "C-"      -> 1.7;
            case "D"       -> 1.0;
            default        -> 0.0;
        };
    }

    /** Returns the colour to use for a grade badge. */
    public static java.awt.Color gradeColor(String grade) {
        return switch (grade) {
            case "A+", "A", "A-" -> new java.awt.Color(21, 128, 61);   // green
            case "B+", "B", "B-" -> new java.awt.Color(21, 101, 192);  // blue
            case "C+", "C", "C-" -> new java.awt.Color(180, 100, 0);   // amber
            case "D"             -> new java.awt.Color(198, 40, 40);    // red
            case "F"             -> new java.awt.Color(140, 20, 20);    // dark red
            default              -> new java.awt.Color(120, 120, 120);  // grey (N/A)
        };
    }

    private void ensureInitialised() {
        if (!initialised)
            throw new IllegalStateException(
                "StudentMarksService.init() must be called first.");
    }
}