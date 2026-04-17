package dao;

import model.MarksItem;
import model.OfferingItem;
import model.RosterStudent;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Upload Marks feature.
 *
 * Responsibilities:
 *   1. getOfferingsForTeacher  — populate the course dropdown
 *   2. getRosterForOffering    — load enrolled students for a course
 *   3. getMarksForOffering     — load existing marks for a course + assessment type
 *   4. upsertMark              — INSERT new mark or UPDATE existing one
 *   5. deleteMark              — remove a mark entry
 */
public class MarksDAO {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /** All active offerings for teacher in active semester. */
    private static final String SQL_OFFERINGS =
        "SELECT co.offering_id, c.course_code, c.course_name, co.section " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   COURSES           c   ON c.course_id    = co.course_id " +
        "JOIN   SEMESTERS         sem ON sem.semester_id = co.semester_id " +
        "JOIN   TEACHERS          t   ON t.teacher_id   = co.teacher_id " +
        "JOIN   USERS             u   ON u.user_id      = t.user_id " +
        "WHERE  u.username   = ? " +
        "  AND  sem.is_active = 1 " +
        "ORDER  BY c.course_code, co.section";

    /** All active students enrolled in an offering. */
    private static final String SQL_ROSTER =
        "SELECT s.student_id, e.enrollment_id, s.full_name, s.roll_number, " +
        "       sd.code AS sub_dept_code " +
        "FROM   ENROLLMENTS    e " +
        "JOIN   STUDENTS        s  ON s.student_id   = e.student_id " +
        "JOIN   SUB_DEPARTMENTS sd ON sd.sub_dept_id = s.sub_dept_id " +
        "WHERE  e.offering_id = ? AND e.status = 'active' " +
        "ORDER  BY s.roll_number";

    /** Existing marks for a given offering + assessment type. */
    private static final String SQL_MARKS =
        "SELECT m.mark_id, m.enrollment_id, s.full_name, s.roll_number, " +
        "       m.assessment_type, m.marks_obtained, m.total_marks, m.remarks " +
        "FROM   MARKS          m " +
        "JOIN   ENROLLMENTS    e ON e.enrollment_id = m.enrollment_id " +
        "JOIN   STUDENTS        s ON s.student_id   = e.student_id " +
        "WHERE  e.offering_id     = ? " +
        "  AND  m.assessment_type = ? " +
        "ORDER  BY s.roll_number";

    /** Check if a mark already exists for enrollment + assessment type. */
    private static final String SQL_EXISTS =
        "SELECT mark_id FROM MARKS " +
        "WHERE enrollment_id = ? AND assessment_type = ?";

    /** Insert new mark. */
    private static final String SQL_INSERT =
        "INSERT INTO MARKS (enrollment_id, assessment_type, marks_obtained, total_marks, remarks) " +
        "VALUES (?, ?, ?, ?, ?)";

    /** Update existing mark. */
    private static final String SQL_UPDATE =
        "UPDATE MARKS SET marks_obtained = ?, total_marks = ?, remarks = ? " +
        "WHERE mark_id = ?";

    /** Delete a mark. */
    private static final String SQL_DELETE =
        "DELETE FROM MARKS WHERE mark_id = ?";

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns all active course offerings for the teacher this semester. */
    public List<OfferingItem> getOfferingsForTeacher(String username) throws SQLException {
        List<OfferingItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_OFFERINGS)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OfferingItem(
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        ""
                    ));
                }
            }
        }
        return list;
    }

    /** Returns all actively enrolled students for an offering. */
    public List<RosterStudent> getRosterForOffering(int offeringId) throws SQLException {
        List<RosterStudent> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ROSTER)) {
            ps.setInt(1, offeringId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RosterStudent(
                        rs.getInt("student_id"),
                        rs.getInt("enrollment_id"),
                        rs.getString("full_name"),
                        rs.getString("roll_number"),
                        rs.getString("sub_dept_code"),
                        0
                    ));
                }
            }
        }
        return list;
    }

    /** Returns existing marks for a course offering + assessment type. */
    public List<MarksItem> getMarksForOffering(int offeringId,
                                                String assessmentType) throws SQLException {
        List<MarksItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_MARKS)) {
            ps.setInt(1, offeringId);
            ps.setString(2, assessmentType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MarksItem(
                        rs.getInt("mark_id"),
                        rs.getInt("enrollment_id"),
                        rs.getString("full_name"),
                        rs.getString("roll_number"),
                        rs.getString("assessment_type"),
                        rs.getDouble("marks_obtained"),
                        rs.getDouble("total_marks"),
                        rs.getString("remarks")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Inserts or updates a mark for a student.
     * If a mark already exists for enrollment + assessment type → UPDATE.
     * Otherwise → INSERT.
     *
     * @return the mark_id of the inserted/updated row
     */
    public int upsertMark(int enrollmentId, String assessmentType,
                          double marksObtained, double totalMarks,
                          String remarks) throws SQLException {
        // Check if exists
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement check = con.prepareStatement(SQL_EXISTS)) {
                check.setInt(1, enrollmentId);
                check.setString(2, assessmentType);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        // UPDATE
                        int markId = rs.getInt("mark_id");
                        try (PreparedStatement upd = con.prepareStatement(SQL_UPDATE)) {
                            upd.setDouble(1, marksObtained);
                            upd.setDouble(2, totalMarks);
                            upd.setString(3, remarks);
                            upd.setInt(4, markId);
                            upd.executeUpdate();
                        }
                        return markId;
                    }
                }
            }
            // INSERT
            try (PreparedStatement ins = con.prepareStatement(
                    SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ins.setInt(1, enrollmentId);
                ins.setString(2, assessmentType);
                ins.setDouble(3, marksObtained);
                ins.setDouble(4, totalMarks);
                ins.setString(5, remarks);
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : -1;
                }
            }
        }
    }

    /** Saves a batch of marks inside a single transaction. */
    public void saveBatch(List<int[]> enrollmentIds, String assessmentType,
                          List<Double> obtained, List<Double> totals,
                          List<String> remarks) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                for (int i = 0; i < enrollmentIds.size(); i++) {
                    int    eid     = enrollmentIds.get(i)[0];
                    double obt     = obtained.get(i);
                    double tot     = totals.get(i);
                    String remark  = remarks.get(i);
                    // reuse single-row upsert logic via direct SQL
                    upsertMarkConn(con, eid, assessmentType, obt, tot, remark);
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

    // ── Private helpers ───────────────────────────────────────────────────────

    private void upsertMarkConn(Connection con, int enrollmentId,
                                String assessmentType,
                                double marksObtained, double totalMarks,
                                String remarks) throws SQLException {
        try (PreparedStatement check = con.prepareStatement(SQL_EXISTS)) {
            check.setInt(1, enrollmentId);
            check.setString(2, assessmentType);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    int markId = rs.getInt("mark_id");
                    try (PreparedStatement upd = con.prepareStatement(SQL_UPDATE)) {
                        upd.setDouble(1, marksObtained);
                        upd.setDouble(2, totalMarks);
                        upd.setString(3, remarks);
                        upd.setInt(4, markId);
                        upd.executeUpdate();
                    }
                    return;
                }
            }
        }
        try (PreparedStatement ins = con.prepareStatement(SQL_INSERT)) {
            ins.setInt(1, enrollmentId);
            ins.setString(2, assessmentType);
            ins.setDouble(3, marksObtained);
            ins.setDouble(4, totalMarks);
            ins.setString(5, remarks);
            ins.executeUpdate();
        }
    }
}