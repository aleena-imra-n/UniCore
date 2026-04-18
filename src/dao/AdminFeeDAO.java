package dao;

import model.AdminChallanRecord;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Admin Fee Payment Tracking feature.
 *
 * Responsibilities:
 *   1. getAllChallans      — all challans across all students, filterable
 *   2. getSemesters        — populate the semester filter dropdown
 *   3. markAsPaid          — flip is_paid = 1 and update FEE_RECORDS.paid_amount
 */
public class AdminFeeDAO {

    private static final String SQL_ALL_CHALLANS =
        "SELECT fc.challan_id, fc.fee_id, fc.challan_number, " +
        "       s.full_name, s.roll_number, " +
        "       sem.semester_name, " +
        "       fr.total_amount, fr.paid_amount, " +
        "       fc.generated_date, fc.expiry_date, fc.is_paid " +
        "FROM   FEE_CHALLANS     fc " +
        "JOIN   FEE_RECORDS      fr  ON fr.fee_id      = fc.fee_id " +
        "JOIN   STUDENTS          s  ON s.student_id   = fr.student_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = fr.semester_id " +
        "ORDER  BY fc.generated_date DESC";

    private static final String SQL_CHALLANS_BY_SEMESTER =
        "SELECT fc.challan_id, fc.fee_id, fc.challan_number, " +
        "       s.full_name, s.roll_number, " +
        "       sem.semester_name, " +
        "       fr.total_amount, fr.paid_amount, " +
        "       fc.generated_date, fc.expiry_date, fc.is_paid " +
        "FROM   FEE_CHALLANS     fc " +
        "JOIN   FEE_RECORDS      fr  ON fr.fee_id      = fc.fee_id " +
        "JOIN   STUDENTS          s  ON s.student_id   = fr.student_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = fr.semester_id " +
        "WHERE  fr.semester_id = ? " +
        "ORDER  BY fc.generated_date DESC";

    private static final String SQL_SEMESTERS =
        "SELECT semester_id, semester_name FROM SEMESTERS ORDER BY start_date DESC";

    /** Mark challan as paid and set paid_amount = total_amount on fee record. */
    private static final String SQL_MARK_PAID_CHALLAN =
        "UPDATE FEE_CHALLANS SET is_paid = 1 WHERE challan_id = ?";

    private static final String SQL_MARK_PAID_FEE =
        "UPDATE FEE_RECORDS SET paid_amount = total_amount, status = 'paid' " +
        "WHERE fee_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns all challans, optionally filtered by semesterId (-1 = all). */
    public List<AdminChallanRecord> getChallans(int semesterId) throws SQLException {
        String sql = semesterId == -1 ? SQL_ALL_CHALLANS : SQL_CHALLANS_BY_SEMESTER;
        List<AdminChallanRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (semesterId != -1) ps.setInt(1, semesterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date gen = rs.getDate("generated_date");
                    Date exp = rs.getDate("expiry_date");
                    list.add(new AdminChallanRecord(
                        rs.getInt("challan_id"),
                        rs.getInt("fee_id"),
                        rs.getString("challan_number"),
                        rs.getString("full_name"),
                        rs.getString("roll_number"),
                        rs.getString("semester_name"),
                        rs.getDouble("total_amount"),
                        rs.getDouble("paid_amount"),
                        gen != null ? gen.toLocalDate() : null,
                        exp != null ? exp.toLocalDate() : null,
                        rs.getBoolean("is_paid")
                    ));
                }
            }
        }
        return list;
    }

    /** Returns all semesters as {semester_id, semester_name} pairs. */
    public List<String[]> getSemesters() throws SQLException {
        List<String[]> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SEMESTERS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("semester_id")),
                    rs.getString("semester_name")
                });
            }
        }
        return list;
    }

    /**
     * Marks a challan as paid and updates the fee record accordingly.
     * Runs both updates in a single transaction.
     */
    public void markAsPaid(int challanId, int feeId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement p1 = con.prepareStatement(SQL_MARK_PAID_CHALLAN)) {
                    p1.setInt(1, challanId);
                    p1.executeUpdate();
                }
                try (PreparedStatement p2 = con.prepareStatement(SQL_MARK_PAID_FEE)) {
                    p2.setInt(1, feeId);
                    p2.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}