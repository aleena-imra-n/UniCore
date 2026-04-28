package dao;

import model.FeeRecord;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Fee Challan and Fee Details features.
 *
 * Responsibilities:
 *   1. getStudentId           — resolve username → student_id
 *   2. getActiveSemesterId    — get the current active semester
 *   3. getFeeRecords          — all fee records + challan info for the student
 *   4. generateChallan        — calls SP_GenerateChallan, returns new challan info
 */
public class FeeDAO {

    private static final String SQL_STUDENT_ID =
        "SELECT s.student_id " +
        "FROM   STUDENTS s JOIN USERS u ON u.user_id = s.user_id " +
        "WHERE  u.username = ? AND u.is_active = 1";

    private static final String SQL_ACTIVE_SEMESTER =
        "SELECT semester_id FROM SEMESTERS WHERE is_active = 1";

    /**
     * All fee records for the student, left-joined with their latest challan.
     * Ordered newest semester first.
     */
    private static final String SQL_FEE_RECORDS =
        "SELECT fr.fee_id, fr.semester_id, " +
        "       sem.semester_name AS semester_name, " +
        "       fr.total_amount, fr.paid_amount, " +
        "       fr.due_date,      fr.status, " +
        "       fc.challan_number, fc.generated_date, " +
        "       fc.expiry_date,   fc.is_paid " +
        "FROM   FEE_RECORDS fr " +
        "JOIN   SEMESTERS   sem ON sem.semester_id = fr.semester_id " +
        "LEFT   JOIN FEE_CHALLANS fc " +
        "         ON fc.fee_id = fr.fee_id " +
        "        AND fc.challan_id = ( " +
        "            SELECT TOP 1 challan_id FROM FEE_CHALLANS " +
        "            WHERE fee_id = fr.fee_id " +
        "            ORDER BY generated_date DESC " +
        "        ) " +
        "WHERE  fr.student_id = ? " +
        "ORDER  BY sem.start_date DESC";

    private static final String SQL_SP_CHALLAN =
        "EXEC SP_GenerateChallan @student_id = ?, @semester_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    public int getStudentId(String username) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_STUDENT_ID)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("student_id") : -1;
            }
        }
    }

    public int getActiveSemesterId() throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ACTIVE_SEMESTER);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("semester_id") : -1;
        }
    }

    public List<FeeRecord> getFeeRecords(int studentId) throws SQLException {
        List<FeeRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FEE_RECORDS)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date gen = rs.getDate("generated_date");
                    Date exp = rs.getDate("expiry_date");
                    Date due = rs.getDate("due_date");
                    list.add(new FeeRecord(
                        rs.getInt("fee_id"),
                        rs.getInt("semester_id"),
                        rs.getString("semester_name"),
                        rs.getDouble("total_amount"),
                        rs.getDouble("paid_amount"),
                        due != null ? due.toLocalDate() : null,
                        rs.getString("status"),
                        rs.getString("challan_number"),
                        gen != null ? gen.toLocalDate() : null,
                        exp != null ? exp.toLocalDate() : null,
                        rs.getBoolean("is_paid")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Calls SP_GenerateChallan and returns the new challan's
     * {challan_number, expiry_date} or throws if SP raises an error.
     */
    public String[] generateChallan(int studentId, int semesterId)
            throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SP_CHALLAN)) {
            ps.setInt(1, studentId);
            ps.setInt(2, semesterId);
            boolean hasResult = ps.execute();
            if (hasResult) {
                try (ResultSet rs = ps.getResultSet()) {
                    if (rs.next()) {
                        return new String[]{
                            rs.getString("challan_number"),
                            rs.getDate("expiry_date").toString()
                        };
                    }
                }
            }
        }
        return null;
    }
}