package dao;

import model.TranscriptCourseRow;
import model.TranscriptSemesterRow;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Transcript feature.
 *
 * Two responsibilities:
 *   1. getStudentId          — resolve login username → student_id
 *   2. getTranscript         — call SP_GetTranscript and parse both result sets
 *
 * SP_GetTranscript returns two result sets:
 *   RS1 — per-course rows  (TranscriptCourseRow)
 *   RS2 — per-semester GPA summary (TranscriptSemesterRow)
 */
public class TranscriptDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** Resolve login username → student_id. */
    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id " +
        "FROM   STUDENTS s " +
        "JOIN   USERS    u ON u.user_id = s.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    /** Execute the transcript stored procedure. */
    private static final String SQL_SP =
        "EXEC SP_GetTranscript @student_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves the login username to a student_id.
     *
     * @param username  login username from the session
     * @return student_id, or -1 if not found / not an active student account
     */
    public int getStudentId(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_STUDENT_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("student_id") : -1;
            }
        }
    }

    /**
     * Executes SP_GetTranscript and returns both result sets.
     *
     * @param studentId  resolved student_id
     * @return TranscriptData containing course rows (RS1) and semester summaries (RS2)
     * @throws SQLException on any DB error
     */
    public TranscriptData getTranscript(int studentId) throws SQLException {

        List<TranscriptCourseRow>   courseRows    = new ArrayList<>();
        List<TranscriptSemesterRow> semesterRows  = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SP)) {

            ps.setInt(1, studentId);

            boolean hasResult = ps.execute();

            // ── RS1: per-course rows ──────────────────────────────────────────
            if (hasResult) {
                try (ResultSet rs = ps.getResultSet()) {
                    while (rs.next()) {
                        courseRows.add(new TranscriptCourseRow(
                            rs.getInt("semester_id"),
                            rs.getString("semester_name"),
                            rs.getString("course_code"),
                            rs.getString("course_name"),
                            rs.getInt("credit_hours"),
                            rs.getString("section"),
                            rs.getString("teacher_name"),
                            rs.getDouble("marks_obtained"),
                            rs.getDouble("marks_total"),
                            rs.getDouble("percentage"),
                            rs.getString("letter_grade"),
                            rs.getDouble("grade_points"),
                            rs.getDouble("quality_points")
                        ));
                    }
                }
            }

            // ── RS2: per-semester GPA summary ─────────────────────────────────
            if (ps.getMoreResults()) {
                try (ResultSet rs = ps.getResultSet()) {
                    while (rs.next()) {
                        semesterRows.add(new TranscriptSemesterRow(
                            rs.getInt("semester_id"),
                            rs.getString("semester_name"),
                            rs.getInt("course_count"),
                            rs.getInt("sem_credit_hours"),
                            rs.getDouble("sem_quality_points"),
                            rs.getDouble("semester_gpa"),
                            rs.getDouble("cgpa_so_far")
                        ));
                    }
                }
            }
        }

        return new TranscriptData(courseRows, semesterRows);
    }

    // ── Inner container ───────────────────────────────────────────────────────

    /**
     * Wraps both result sets returned by SP_GetTranscript.
     * Passed from DAO → Service → UI as a single object.
     */
    public record TranscriptData(
        List<TranscriptCourseRow>   courseRows,
        List<TranscriptSemesterRow> semesterRows
    ) {
        /** True if there is no transcript data at all. */
        public boolean isEmpty() {
            return courseRows.isEmpty() && semesterRows.isEmpty();
        }

        /** Overall CGPA = the cgpa_so_far value from the last semester row. */
        public double getOverallCgpa() {
            if (semesterRows.isEmpty()) return 0.0;
            return semesterRows.get(semesterRows.size() - 1).getCgpaSoFar();
        }

        /** Total credit hours earned across all semesters. */
        public int getTotalCreditHours() {
            return semesterRows.stream()
                               .mapToInt(TranscriptSemesterRow::getSemCreditHours)
                               .sum();
        }

        /** Total number of courses across all semesters. */
        public int getTotalCourses() {
            return courseRows.size();
        }
    }
}