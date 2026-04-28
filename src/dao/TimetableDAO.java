package dao;

import model.OfferingDropdownItem;
import model.TimetableConflict;
import model.TimetableSlot;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Admin Create Timetables feature (US-3.7a / US-3.11a).
 *
 * Five responsibilities:
 *   1. getAllOfferings   — all course offerings for the active semester (dropdown)
 *   2. getSlotsForOffering — existing slots for one offering (right-panel list)
 *   3. getAllSlots          — full slot list (used for the master table view)
 *   4. addSlot             — calls SP_AddTimetableSlot; returns conflicts + result
 *   5. deleteSlot          — calls SP_DeleteTimetableSlot
 */
public class TimetableDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** All active-semester offerings, ordered for the dropdown. */
    private static final String SQL_OFFERINGS =
        "SELECT co.offering_id, c.course_code, c.course_name, co.section, " +
        "       t.full_name AS teacher_name, sem.semester_name, sd.code AS sub_dept_code " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   COURSES           c   ON c.course_id    = co.course_id " +
        "JOIN   TEACHERS          t   ON t.teacher_id   = co.teacher_id " +
        "JOIN   SEMESTERS         sem ON sem.semester_id = co.semester_id " +
        "JOIN   SUB_DEPARTMENTS   sd  ON sd.sub_dept_id = co.sub_dept_id " +
        "WHERE  sem.is_active = 1 " +
        "ORDER  BY c.course_code, co.section";

    /** All slots for every offering, joined for display. */
    private static final String SQL_ALL_SLOTS =
        "SELECT tt.timetable_id, tt.offering_id, " +
        "       c.course_code, c.course_name, co.section, " +
        "       t.full_name AS teacher_name, " +
        "       sem.semester_name, " +
        "       tt.day_of_week, " +
        "       CONVERT(VARCHAR(5), tt.start_time, 108) AS start_time, " +
        "       CONVERT(VARCHAR(5), tt.end_time,   108) AS end_time, " +
        "       tt.room_number " +
        "FROM   TIMETABLES       tt " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = tt.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "WHERE  sem.is_active = 1 " +
        "ORDER  BY tt.day_of_week, tt.start_time, c.course_code";

    /** Slots for one offering only (same join structure). */
    private static final String SQL_SLOTS_FOR_OFFERING =
        "SELECT tt.timetable_id, tt.offering_id, " +
        "       c.course_code, c.course_name, co.section, " +
        "       t.full_name AS teacher_name, " +
        "       sem.semester_name, " +
        "       tt.day_of_week, " +
        "       CONVERT(VARCHAR(5), tt.start_time, 108) AS start_time, " +
        "       CONVERT(VARCHAR(5), tt.end_time,   108) AS end_time, " +
        "       tt.room_number " +
        "FROM   TIMETABLES       tt " +
        "JOIN   COURSE_OFFERINGS co  ON co.offering_id  = tt.offering_id " +
        "JOIN   COURSES           c  ON c.course_id     = co.course_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id    = co.teacher_id " +
        "JOIN   SEMESTERS        sem ON sem.semester_id = co.semester_id " +
        "WHERE  tt.offering_id = ? " +
        "ORDER  BY tt.day_of_week, tt.start_time";

    private static final String SQL_ADD =
        "EXEC SP_AddTimetableSlot " +
        "  @offering_id = ?, @day_of_week = ?, " +
        "  @start_time = ?, @end_time = ?, " +
        "  @room_number = ?, @force = ?";

    private static final String SQL_DELETE =
        "EXEC SP_DeleteTimetableSlot @timetable_id = ?";

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * Wraps the two result sets returned by SP_AddTimetableSlot.
     *
     * @param conflicts  list of conflicting slots (RS1); empty = no conflict
     * @param inserted   true if the row was actually inserted (RS2)
     * @param newId      the generated timetable_id (−1 if not inserted)
     */
    public record AddSlotResult(
        java.util.List<TimetableConflict> conflicts,
        boolean inserted,
        int newId
    ) {}

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns all course offerings for the active semester. */
    public List<OfferingDropdownItem> getAllOfferings() throws SQLException {
        List<OfferingDropdownItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_OFFERINGS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new OfferingDropdownItem(
                    rs.getInt("offering_id"),
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getString("section"),
                    rs.getString("teacher_name"),
                    rs.getString("semester_name"),
                    rs.getString("sub_dept_code")
                ));
            }
        }
        return list;
    }

    /** Returns all timetable slots for the active semester (master view). */
    public List<TimetableSlot> getAllSlots() throws SQLException {
        return querySlots(SQL_ALL_SLOTS, -1);
    }

    /** Returns timetable slots for one specific offering. */
    public List<TimetableSlot> getSlotsForOffering(int offeringId) throws SQLException {
        return querySlots(SQL_SLOTS_FOR_OFFERING, offeringId);
    }

    /**
     * Calls SP_AddTimetableSlot and processes both result sets.
     *
     * RS1 = conflict rows (may be empty).
     * RS2 = single row: inserted (bit) + new_timetable_id.
     *
     * @param offeringId  target offering
     * @param dayOfWeek   "Monday" … "Friday"
     * @param startTime   "HH:mm" string
     * @param endTime     "HH:mm" string
     * @param roomNumber  may be null
     * @param force       true = insert even when conflicts exist
     */
    public AddSlotResult addSlot(int offeringId, String dayOfWeek,
                                  String startTime, String endTime,
                                  String roomNumber, boolean force)
            throws SQLException {

        List<TimetableConflict> conflicts = new ArrayList<>();
        boolean inserted = false;
        int newId = -1;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ADD)) {

            ps.setInt(1, offeringId);
            ps.setString(2, dayOfWeek);
            ps.setString(3, startTime);
            ps.setString(4, endTime);
            if (roomNumber == null || roomNumber.isBlank())
                ps.setNull(5, Types.NVARCHAR);
            else
                ps.setString(5, roomNumber.trim());
            ps.setInt(6, force ? 1 : 0);

            boolean hasResult = ps.execute();

            // RS1: conflict rows
            if (hasResult) {
                try (ResultSet rs = ps.getResultSet()) {
                    while (rs.next()) {
                        conflicts.add(new TimetableConflict(
                            rs.getInt("conflicting_offering"),
                            rs.getString("course_code"),
                            rs.getString("section"),
                            rs.getString("day_of_week"),
                            rs.getString("start_time"),
                            rs.getString("end_time"),
                            rs.getString("room_number")
                        ));
                    }
                }
            }

            // RS2: inserted flag + new id
            if (ps.getMoreResults()) {
                try (ResultSet rs = ps.getResultSet()) {
                    if (rs.next()) {
                        inserted = rs.getInt("inserted") == 1;
                        Object idObj = rs.getObject("new_timetable_id");
                        newId = (idObj != null) ? ((Number) idObj).intValue() : -1;
                    }
                }
            }
        }

        return new AddSlotResult(conflicts, inserted, newId);
    }

    /**
     * Calls SP_DeleteTimetableSlot.
     * Throws SQLException with the SP's RAISERROR text if the slot is not found.
     */
    public void deleteSlot(int timetableId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, timetableId);
            ps.execute();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<TimetableSlot> querySlots(String sql, int param) throws SQLException {
        List<TimetableSlot> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param >= 0) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TimetableSlot(
                        rs.getInt("timetable_id"),
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("teacher_name"),
                        rs.getString("semester_name"),
                        rs.getString("day_of_week"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getString("room_number")
                    ));
                }
            }
        }
        return list;
    }
}