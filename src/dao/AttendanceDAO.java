package dao;

import model.AttendanceRecord;
import model.AttendanceRecord.Status;
import model.OfferingItem;
import model.RosterStudent;
import util.DBConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * DAO for the Mark-Attendance feature.
 *
 * Covers four DB operations:
 *   1. getOfferingsForTeacher  — populate the course combo box
 *   2. getRosterForOffering    — load the student list + historical %
 *   3. getByOfferingAndDate    — check / load an existing session record
 *   4. saveAttendance          — INSERT new attendance rows (transactional)
 *   5. updateAttendance        — UPDATE existing attendance rows (transactional)
 */
public class AttendanceDAO {

    // ── SQL ──────────────────────────────────────────────────────────────────

    /** All active offerings assigned to a teacher in the current semester. */
    private static final String SQL_OFFERINGS_FOR_TEACHER =
        "SELECT co.offering_id, c.course_code, c.course_name, co.section, sd.code AS sub_dept " +
        "FROM   COURSE_OFFERINGS co " +
        "JOIN   COURSES           c  ON c.course_id    = co.course_id " +
        "JOIN   SUB_DEPARTMENTS   sd ON sd.sub_dept_id = co.sub_dept_id " +
        "JOIN   TEACHERS          t  ON t.teacher_id   = co.teacher_id " +
        "JOIN   USERS             u  ON u.user_id      = t.user_id " +
        "JOIN   SEMESTERS         sem ON sem.semester_id = co.semester_id " +
        "WHERE  u.username = ? " +
        "AND    sem.is_active = 1 " +
        "ORDER  BY c.course_code, co.section";

    /**
     * Roster: every active student enrolled in an offering, plus their
     * running attendance percentage across all recorded sessions so far.
     */
    private static final String SQL_ROSTER =
        "SELECT s.student_id, " +
        "       e.enrollment_id, " +
        "       s.full_name, " +
        "       s.roll_number, " +
        "       sd.code AS sub_dept_code, " +
        "       ISNULL(att_summary.pct, 0) AS attendance_pct " +
        "FROM   ENROLLMENTS    e " +
        "JOIN   STUDENTS        s   ON s.student_id    = e.student_id " +
        "JOIN   SUB_DEPARTMENTS sd  ON sd.sub_dept_id  = s.sub_dept_id " +
        "OUTER APPLY ( " +
        "    SELECT CAST( " +
        "        100.0 * SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) " +
        "        / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) AS pct " +
        "    FROM ATTENDANCE a " +
        "    WHERE a.enrollment_id = e.enrollment_id " +
        ") att_summary " +
        "WHERE  e.offering_id = ? " +
        "AND    e.status = 'active' " +
        "ORDER  BY s.roll_number";

    /**
     * Load an existing attendance session:
     * returns enrollment_id → status for every enrolled student on that date.
     */
    private static final String SQL_LOAD_SESSION =
        "SELECT e.student_id, a.enrollment_id, a.status, a.remarks " +
        "FROM   ATTENDANCE  a " +
        "JOIN   ENROLLMENTS e ON e.enrollment_id = a.enrollment_id " +
        "WHERE  e.offering_id = ? " +
        "AND    a.class_date  = ?";

    /** INSERT one row per student for a new session. */
    private static final String SQL_INSERT_ONE =
        "INSERT INTO ATTENDANCE (enrollment_id, class_date, status, remarks) " +
        "VALUES (?, ?, ?, ?)";

    /** UPDATE one row (matched by enrollment_id + class_date). */
    private static final String SQL_UPDATE_ONE =
        "UPDATE ATTENDANCE " +
        "SET status = ?, remarks = ? " +
        "WHERE enrollment_id = ? AND class_date = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns all active course offerings for the given teacher username
     * in the currently active semester.
     *
     * @param teacherUsername  login username of the teacher
     * @return list of OfferingItem (never null, may be empty)
     */
    public List<OfferingItem> getOfferingsForTeacher(String teacherUsername)
            throws SQLException {

        List<OfferingItem> results = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_OFFERINGS_FOR_TEACHER)) {

            ps.setString(1, teacherUsername);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new OfferingItem(
                        rs.getInt("offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("section"),
                        rs.getString("sub_dept")
                    ));
                }
            }
        }
        return results;
    }

    /**
     * Returns the student roster for a course offering, including each
     * student's running attendance percentage (0 if none recorded yet).
     *
     * @param offeringId  the course offering
     * @return ordered list of RosterStudent (by roll number)
     */
    public List<RosterStudent> getRosterForOffering(int offeringId)
            throws SQLException {

        List<RosterStudent> roster = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ROSTER)) {

            ps.setInt(1, offeringId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roster.add(new RosterStudent(
                        rs.getInt("student_id"),
                        rs.getInt("enrollment_id"),
                        rs.getString("full_name"),
                        rs.getString("roll_number"),
                        rs.getString("sub_dept_code"),
                        rs.getInt("attendance_pct")
                    ));
                }
            }
        }
        return roster;
    }

    /**
     * Checks whether an attendance session already exists for the given
     * offering + date, and returns the recorded statuses if so.
     *
     * @param offeringId  course offering
     * @param date        session date
     * @return map of studentId → AttendanceRecord, empty if no session found
     */
    public Map<Integer, AttendanceRecord> getByOfferingAndDate(
            int offeringId, LocalDate date) throws SQLException {

        Map<Integer, AttendanceRecord> map = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_LOAD_SESSION)) {

            ps.setInt(1, offeringId);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int studentId    = rs.getInt("student_id");
                    int enrollmentId = rs.getInt("enrollment_id");
                    Status status    = AttendanceRecord.fromDbString(rs.getString("status"));
                    String remarks   = rs.getString("remarks");

                    map.put(studentId, new AttendanceRecord(
                        enrollmentId, studentId, date, status, remarks));
                }
            }
        }
        return map;
    }

    /**
     * Saves a brand-new attendance session (INSERT).
     * Runs inside a single transaction — either all rows succeed or none do.
     *
     * @param records  one AttendanceRecord per enrolled student
     * @throws SQLException on any DB error; transaction is rolled back automatically
     */
    public void saveAttendance(List<AttendanceRecord> records) throws SQLException {
        if (records == null || records.isEmpty()) return;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT_ONE)) {
                for (AttendanceRecord rec : records) {
                    ps.setInt(1, rec.getEnrollmentId());
                    ps.setDate(2, Date.valueOf(rec.getClassDate()));
                    ps.setString(3, AttendanceRecord.toDbString(rec.getStatus()));
                    ps.setString(4, rec.getRemarks());
                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Updates an existing attendance session (UPDATE).
     * Runs inside a single transaction — either all rows succeed or none do.
     *
     * @param records  one AttendanceRecord per enrolled student (must already exist)
     * @throws SQLException on any DB error; transaction is rolled back automatically
     */
    public void updateAttendance(List<AttendanceRecord> records) throws SQLException {
        if (records == null || records.isEmpty()) return;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ONE)) {
                for (AttendanceRecord rec : records) {
                    ps.setString(1, AttendanceRecord.toDbString(rec.getStatus()));
                    ps.setString(2, rec.getRemarks());
                    ps.setInt(3, rec.getEnrollmentId());
                    ps.setDate(4, Date.valueOf(rec.getClassDate()));
                    ps.addBatch();
                }
                ps.executeBatch();
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
