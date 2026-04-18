package bl;

import dao.AdminReportsDAO;
import model.AcademicPerformanceRow;
import model.AttendanceReportRow;
import model.EnrollmentReportRow;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic for Admin Reports.
 *
 * Converts filter dropdown selections (String IDs) to integers,
 * delegates all queries to the DAO, and returns typed lists.
 * The UI never touches the DAO directly.
 */
public class AdminReportsService {

    public enum ReportType { ENROLLMENT, ACADEMIC_PERFORMANCE, ATTENDANCE }

    /** Container returned to the UI for filter options. */
    public record FilterOption(String id, String label) {
        @Override public String toString() { return label; }
    }

    private final AdminReportsDAO dao;

    public AdminReportsService()                     { this.dao = new AdminReportsDAO(); }
    public AdminReportsService(AdminReportsDAO dao)  { this.dao = dao; }

    // ── Filter options ────────────────────────────────────────────────────────

    /**
     * Returns semester options for the filter dropdown.
     * First entry is always { "-1", "All Semesters" }.
     */
    public List<FilterOption> getSemesterOptions() {
        try {
            List<FilterOption> list = new java.util.ArrayList<>();
            list.add(new FilterOption("-1", "All Semesters"));
            dao.getSemesterOptions().forEach(row ->
                list.add(new FilterOption(row[0], row[1])));
            return list;
        } catch (SQLException e) {
            return List.of(new FilterOption("-1", "All Semesters"));
        }
    }

    /**
     * Returns department options for the filter dropdown.
     * First entry is always { "-1", "All Departments" }.
     */
    public List<FilterOption> getDeptOptions() {
        try {
            List<FilterOption> list = new java.util.ArrayList<>();
            list.add(new FilterOption("-1", "All Departments"));
            dao.getDeptOptions().forEach(row ->
                list.add(new FilterOption(row[0], row[1])));
            return list;
        } catch (SQLException e) {
            return List.of(new FilterOption("-1", "All Departments"));
        }
    }

    // ── Report loads ──────────────────────────────────────────────────────────

    /**
     * Loads the Enrollment Report.
     *
     * @param semesterFilter  FilterOption selected by admin ("-1" = all)
     * @param deptFilter      FilterOption selected by admin ("-1" = all)
     */
    public List<EnrollmentReportRow> loadEnrollmentReport(
            FilterOption semesterFilter, FilterOption deptFilter) {
        int semId  = parseId(semesterFilter);
        int deptId = parseId(deptFilter);
        try {
            return dao.getEnrollmentReport(semId, deptId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Loads the Academic Performance Report.
     */
    public List<AcademicPerformanceRow> loadAcademicReport(
            FilterOption semesterFilter, FilterOption deptFilter) {
        int semId  = parseId(semesterFilter);
        int deptId = parseId(deptFilter);
        try {
            return dao.getAcademicReport(semId, deptId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Loads the Attendance Summary Report.
     */
    public List<AttendanceReportRow> loadAttendanceReport(
            FilterOption semesterFilter, FilterOption deptFilter) {
        int semId  = parseId(semesterFilter);
        int deptId = parseId(deptFilter);
        try {
            return dao.getAttendanceReport(semId, deptId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────
    private int parseId(FilterOption option) {
        if (option == null) return -1;
        try { return Integer.parseInt(option.id()); }
        catch (NumberFormatException e) { return -1; }
    }
}
