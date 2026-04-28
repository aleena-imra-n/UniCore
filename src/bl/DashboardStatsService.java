package bl;

import dao.DashboardStatsDAO;
import java.sql.SQLException;

/**
 * Service layer for dashboard stats.
 * Wraps DashboardStatsDAO and returns safe String[][] arrays
 * that can be passed directly to BaseDashboard.getDashboardStats().
 */
public class DashboardStatsService {

    private final DashboardStatsDAO dao;

    public DashboardStatsService() {
        this.dao = new DashboardStatsDAO();
    }

    /**
     * Returns student stats as a String[][] for BaseDashboard.
     * Format: {{value, label, colour}, ...}
     */
    public String[][] getStudentStats(String username) {
        try {
            String[] s = dao.getStudentStats(username);
            return new String[][]{
                {s[0], "Courses",  "#1565C0"},
                {s[1], "Attend.",  "#2E7D32"},
                {s[2], "Marks",    "#F57C00"},
            };
        } catch (SQLException e) {
            return fallbackStudentStats();
        }
    }

    /**
     * Returns teacher stats as a String[][] for BaseDashboard.
     */
    public String[][] getTeacherStats(String username) {
        try {
            String[] s = dao.getTeacherStats(username);
            return new String[][]{
                {s[0], "Courses",  "#1565C0"},
                {s[1], "Students", "#2E7D32"},
                {s[2], "Pending",  "#C62828"},
            };
        } catch (SQLException e) {
            return fallbackTeacherStats();
        }
    }

    /**
     * Returns admin stats as a String[][] for BaseDashboard.
     */
    public String[][] getAdminStats() {
        try {
            String[] s = dao.getAdminStats();
            return new String[][]{
                {s[0], "Students", "#1565C0"},
                {s[1], "Teachers", "#2E7D32"},
                {s[2], "Courses",  "#F57C00"},
            };
        } catch (SQLException e) {
            return fallbackAdminStats();
        }
    }

    // ── Fallbacks (shown if DB fails) ─────────────────────────────────────────
    private String[][] fallbackStudentStats() {
        return new String[][]{{"—","Courses","#1565C0"},{"—","Attend.","#2E7D32"},{"—","Marks","#F57C00"}};
    }
    private String[][] fallbackTeacherStats() {
        return new String[][]{{"—","Courses","#1565C0"},{"—","Students","#2E7D32"},{"—","Pending","#C62828"}};
    }
    private String[][] fallbackAdminStats() {
        return new String[][]{{"—","Students","#1565C0"},{"—","Teachers","#2E7D32"},{"—","Courses","#F57C00"}};
    }
}