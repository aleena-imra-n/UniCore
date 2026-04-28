package dao;

import model.SemesterRow;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for UC-20 — Semester Management (US-3.12a).
 *
 * Responsibilities:
 *   1. getAllSemesters      — full list for the table
 *   2. insertSemester      — INSERT new semester row
 *   3. setActiveSemester   — sets is_active=1 for one row, 0 for all others (one transaction)
 *   4. setEnrollmentOpen   — UPDATE SEMESTERS SET enrollment_open = ? WHERE semester_id = ?
 *   5. existsByName        — duplicate-name guard before insert
 */
public class ManageSemestersDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** Load every semester. enrollment_open is read defensively (column may not exist yet). */
    private static final String SQL_ALL =
        "SELECT semester_id, semester_name, start_date, end_date, is_active " +
        "FROM   SEMESTERS " +
        "ORDER  BY is_active DESC, semester_id DESC";

    private static final String SQL_EXISTS_NAME =
        "SELECT COUNT(*) FROM SEMESTERS " +
        "WHERE  UPPER(LTRIM(RTRIM(semester_name))) = UPPER(LTRIM(RTRIM(?)))";

    private static final String SQL_INSERT =
        "INSERT INTO SEMESTERS (semester_name, start_date, end_date, is_active) " +
        "VALUES (?, ?, ?, 0)";

    /** Extended INSERT used when enrollment_open column exists. */
    private static final String SQL_INSERT_WITH_ENROLL =
        "INSERT INTO SEMESTERS (semester_name, start_date, end_date, is_active, enrollment_open) " +
        "VALUES (?, ?, ?, 0, 0)";

    private static final String SQL_DEACTIVATE_ALL =
        "UPDATE SEMESTERS SET is_active = 0";

    /** Extended deactivate used when enrollment_open column exists. */
    private static final String SQL_DEACTIVATE_ALL_WITH_ENROLL =
        "UPDATE SEMESTERS SET is_active = 0, enrollment_open = 0";

    private static final String SQL_ACTIVATE_ONE =
        "UPDATE SEMESTERS SET is_active = 1 WHERE semester_id = ?";

    private static final String SQL_SET_ENROLLMENT =
        "UPDATE SEMESTERS SET enrollment_open = ? WHERE semester_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all semesters ordered: active first, then newest first.
     */
    public List<SemesterRow> getAllSemesters() throws SQLException {
        List<SemesterRow> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date sd = rs.getDate("start_date");
                Date ed = rs.getDate("end_date");
                // Read enrollment_open defensively — column may not exist yet
                boolean enrollOpen = false;
                try { enrollOpen = rs.getBoolean("enrollment_open"); } catch (SQLException ignored) {}
                list.add(new SemesterRow(
                    rs.getInt("semester_id"),
                    rs.getString("semester_name"),
                    sd != null ? sd.toLocalDate() : null,
                    ed != null ? ed.toLocalDate() : null,
                    rs.getBoolean("is_active"),
                    enrollOpen
                ));
            }
        }
        return list;
    }

    /**
     * Checks whether a semester with this name already exists (case-insensitive).
     */
    public boolean existsByName(String name) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_EXISTS_NAME)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserts a new semester. New semesters always start inactive with enrollment closed.
     *
     * @return generated semester_id, or -1 on failure
     */
    public int insertSemester(String name, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        // Try with enrollment_open first; fall back to without if column doesn't exist
        String sql;
        try (Connection probe = DBConnection.getConnection()) {
            probe.getMetaData().getColumns(null, null, "SEMESTERS", "enrollment_open")
                 .next(); // throws if column missing in some drivers; safe otherwise
            sql = SQL_INSERT_WITH_ENROLL;
        } catch (Exception ignored) {
            sql = SQL_INSERT;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.setDate(2, startDate != null ? Date.valueOf(startDate) : null);
            ps.setDate(3, endDate   != null ? Date.valueOf(endDate)   : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Makes one semester the active semester in a single transaction:
     * deactivates (and closes enrollment for) all semesters, then activates the target one.
     *
     * @param semesterId  the semester to activate
     * @return true if the target row was updated
     */
    public boolean setActiveSemester(int semesterId) throws SQLException {
        // Choose deactivate SQL based on whether enrollment_open column exists
        boolean hasEnrollCol = enrollmentColExists();
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                String deactivateSql = hasEnrollCol
                    ? SQL_DEACTIVATE_ALL_WITH_ENROLL : SQL_DEACTIVATE_ALL;
                try (PreparedStatement ps1 = con.prepareStatement(deactivateSql)) {
                    ps1.executeUpdate();
                }
                boolean updated;
                try (PreparedStatement ps2 = con.prepareStatement(SQL_ACTIVATE_ONE)) {
                    ps2.setInt(1, semesterId);
                    updated = ps2.executeUpdate() > 0;
                }
                con.commit();
                return updated;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Opens or closes enrollment for a specific semester.
     *
     * @param semesterId     target semester
     * @param enrollmentOpen true = open, false = close
     * @return true if a row was updated
     */
    public boolean setEnrollmentOpen(int semesterId, boolean enrollmentOpen)
            throws SQLException {
        if (!enrollmentColExists()) {
            throw new SQLException(
                "The 'enrollment_open' column does not exist yet. " +
                "Please run the migration SQL provided by the setup guide.");
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SET_ENROLLMENT)) {
            ps.setBoolean(1, enrollmentOpen);
            ps.setInt(2, semesterId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Returns true if the SEMESTERS.enrollment_open column already exists in the DB.
     * Used to safely degrade before the migration SQL has been run.
     */
    private boolean enrollmentColExists() {
        try (Connection con = DBConnection.getConnection();
             ResultSet cols = con.getMetaData()
                 .getColumns(null, null, "SEMESTERS", "enrollment_open")) {
            return cols.next();
        } catch (SQLException e) {
            return false;
        }
    }
}
