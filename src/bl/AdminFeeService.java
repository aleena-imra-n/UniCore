package bl;

import dao.AdminFeeDAO;
import model.AdminChallanRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business-Logic layer for Admin Fee Payment Tracking.
 * Zero Swing. Zero SQL.
 */
public class AdminFeeService {

    public record MarkPaidResult(boolean success, String message) {}

    private final AdminFeeDAO dao;

    public AdminFeeService()             { this.dao = new AdminFeeDAO(); }
    public AdminFeeService(AdminFeeDAO d){ this.dao = d; }

    /** Returns all challans filtered by semester (-1 = all) and status filter. */
    public List<AdminChallanRecord> getChallans(int semesterId, String statusFilter) {
        try {
            List<AdminChallanRecord> all = dao.getChallans(semesterId);
            if (statusFilter == null || statusFilter.equals("All")) return all;
            return all.stream()
                .filter(r -> r.getDisplayStatus().equals(statusFilter))
                .collect(Collectors.toList());
        } catch (SQLException e) {
            return List.of();
        }
    }

    /** Returns all semesters for the filter dropdown. */
    public List<String[]> getSemesters() {
        try { return dao.getSemesters(); }
        catch (SQLException e) { return List.of(); }
    }

    /** Marks a challan as paid. */
    public MarkPaidResult markAsPaid(int challanId, int feeId) {
        try {
            dao.markAsPaid(challanId, feeId);
            return new MarkPaidResult(true, "Challan marked as paid successfully!");
        } catch (SQLException e) {
            return new MarkPaidResult(false, "Failed to mark as paid: " + e.getMessage());
        }
    }
}