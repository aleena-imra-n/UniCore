package dao;

import model.StudentSearchResult;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Admin Issue Transcripts feature (US-3.14a).
 *
 * Two responsibilities:
 *   1. searchStudents  — find students by name or roll number
 *   2. getStudentById  — fetch one student's full profile by student_id
 *
 * The actual transcript data is fetched via the existing TranscriptDAO
 * (which calls SP_GetTranscript) — this DAO only handles student lookup.
 */
public class IssueTranscriptDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /**
     * Searches students by full name OR roll number (case-insensitive LIKE).
     * Returns up to 50 results ordered by full_name.
     * Includes inactive students so the admin can still issue historical records.
     */
    private static final String SQL_SEARCH =
        "SELECT TOP 50 " +
        "       s.student_id, s.full_name, s.roll_number, " +
        "       u.username, u.email, u.is_active, " +
        "       s.batch_year, s.current_semester, " +
        "       sd.name  AS sub_dept_name, " +
        "       sd.code  AS sub_dept_code, " +
        "       md.name  AS major_dept_name " +
        "FROM   STUDENTS          s " +
        "JOIN   USERS             u  ON u.user_id      = s.user_id " +
        "JOIN   SUB_DEPARTMENTS   sd ON sd.sub_dept_id = s.sub_dept_id " +
        "JOIN   MAJOR_DEPARTMENTS md ON md.major_dept_id = sd.major_dept_id " +
        "WHERE  s.full_name   LIKE ? " +
        "   OR  s.roll_number LIKE ? " +
        "ORDER  BY s.full_name ASC";

    /** Fetch one student by student_id (for pre-fill after selection). */
    private static final String SQL_BY_ID =
        "SELECT s.student_id, s.full_name, s.roll_number, " +
        "       u.username, u.email, u.is_active, " +
        "       s.batch_year, s.current_semester, " +
        "       sd.name  AS sub_dept_name, " +
        "       sd.code  AS sub_dept_code, " +
        "       md.name  AS major_dept_name " +
        "FROM   STUDENTS          s " +
        "JOIN   USERS             u  ON u.user_id      = s.user_id " +
        "JOIN   SUB_DEPARTMENTS   sd ON sd.sub_dept_id = s.sub_dept_id " +
        "JOIN   MAJOR_DEPARTMENTS md ON md.major_dept_id = sd.major_dept_id " +
        "WHERE  s.student_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns students whose name or roll number contains the search term.
     * Passing a blank term returns an empty list (avoids full-table scans).
     *
     * @param term  search text entered by the admin (trimmed by caller)
     * @return list of up to 50 StudentSearchResult, ordered by name
     */
    public List<StudentSearchResult> searchStudents(String term) throws SQLException {
        List<StudentSearchResult> list = new ArrayList<>();
        if (term == null || term.isBlank()) return list;

        String pattern = "%" + term.trim() + "%";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SEARCH)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Fetches a single student profile by student_id.
     *
     * @param studentId  the student_id to look up
     * @return StudentSearchResult, or null if not found
     */
    public StudentSearchResult getStudentById(int studentId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_BY_ID)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private StudentSearchResult mapRow(ResultSet rs) throws SQLException {
        return new StudentSearchResult(
            rs.getInt("student_id"),
            rs.getString("full_name"),
            rs.getString("roll_number"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("sub_dept_name"),
            rs.getString("sub_dept_code"),
            rs.getString("major_dept_name"),
            rs.getInt("batch_year"),
            rs.getInt("current_semester"),
            rs.getBoolean("is_active")
        );
    }
}