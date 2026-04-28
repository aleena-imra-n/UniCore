package bl;

import dao.WithdrawalDAO;
import model.EnrolledCourse;
import model.WithdrawalRequest;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for the Course Withdrawal feature.
 *
 * Shared by both the student panel (submit) and the admin panel (review).
 * Responsibilities:
 *   - Resolve username → student_id or admin user_id (cached per session)
 *   - Validate reason field before hitting the SP
 *   - Return typed result objects so neither UI interprets raw strings
 *   - Delegate all DB access to WithdrawalDAO
 *
 * Zero Swing code. Zero SQL.
 */
public class WithdrawalService {

    // ── Result types ──────────────────────────────────────────────────────────

    public record SubmitResult(boolean success, String message) {}

    public record ReviewResult(boolean success, String message) {}

    // ── Configuration ─────────────────────────────────────────────────────────
    public static final int MAX_REASON_LENGTH = 500;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final WithdrawalDAO dao;

    private int     studentId   = -1;
    private int     adminUserId = -1;

    // ── Constructors ─────────────────────────────────────────────────────────
    public WithdrawalService() { this.dao = new WithdrawalDAO(); }
    public WithdrawalService(WithdrawalDAO dao) { this.dao = dao; }

    // ── Init (student side) ───────────────────────────────────────────────────

    /**
     * Initialises for a logged-in student.
     * @return true if the student account was found
     */
    public boolean initStudent(String username) {
        try {
            studentId = dao.getStudentId(username);
            return studentId > 0;
        } catch (SQLException e) {
            studentId = -1;
            return false;
        }
    }

    /**
     * Initialises for a logged-in admin.
     * @return true if the admin account was found
     */
    public boolean initAdmin(String username) {
        try {
            adminUserId = dao.getAdminUserId(username);
            return adminUserId > 0;
        } catch (SQLException e) {
            adminUserId = -1;
            return false;
        }
    }

    // ── Student API ───────────────────────────────────────────────────────────

    /**
     * Returns active enrollments the student can request withdrawal from.
     * Enrollments with an existing pending request are excluded.
     */
    public List<EnrolledCourse> getActiveEnrollments() {
        ensureStudent();
        try { return dao.getActiveEnrollments(studentId); }
        catch (SQLException e) { return List.of(); }
    }

    /**
     * All withdrawal requests the student has submitted (any status),
     * newest first.
     */
    public List<WithdrawalRequest> getMyRequests() {
        ensureStudent();
        try { return dao.getRequestsForStudent(studentId); }
        catch (SQLException e) { return List.of(); }
    }

    /**
     * Validates and submits a withdrawal request.
     *
     * Rules:
     *   1. An enrollment must be selected.
     *   2. Reason must not be blank.
     *   3. Reason must not exceed MAX_REASON_LENGTH characters.
     */
    public SubmitResult submitRequest(EnrolledCourse selectedCourse, String reason) {
        ensureStudent();

        if (selectedCourse == null)
            return new SubmitResult(false, "Please select a course to withdraw from.");

        String cleanReason = (reason == null) ? "" : reason.trim();
        if (cleanReason.isEmpty())
            return new SubmitResult(false, "Please provide a reason for withdrawal.");

        if (cleanReason.length() > MAX_REASON_LENGTH)
            return new SubmitResult(false,
                "Reason exceeds " + MAX_REASON_LENGTH + " characters ("
                + cleanReason.length() + " entered).");

        try {
            dao.submitRequest(selectedCourse.getEnrollmentId(), cleanReason);
            return new SubmitResult(true,
                "Withdrawal request submitted for " + selectedCourse.getCourseCode()
                + ". Await admin review.");
        } catch (SQLException e) {
            return new SubmitResult(false, extractSpMessage(e.getMessage()));
        }
    }

    // ── Admin API ─────────────────────────────────────────────────────────────

    /**
     * Returns all pending withdrawal requests across all students,
     * ordered oldest-first so admins work through them in FIFO order.
     */
    public List<WithdrawalRequest> getAllPendingRequests() {
        ensureAdmin();
        try { return dao.getAllPendingRequests(); }
        catch (SQLException e) { return List.of(); }
    }

    /**
     * Approves or rejects a withdrawal request.
     *
     * Rules:
     *   1. decision must be "approved" or "rejected".
     *   2. adminComment is optional but trimmed before saving.
     */
    public ReviewResult reviewRequest(int requestId, String decision, String adminComment) {
        ensureAdmin();

        if (!"approved".equals(decision) && !"rejected".equals(decision))
            return new ReviewResult(false, "Invalid decision value.");

        String cleanComment = (adminComment == null) ? "" : adminComment.trim();
        try {
            dao.reviewRequest(requestId, adminUserId, decision,
                              cleanComment.isEmpty() ? null : cleanComment);
            String verb = "approved".equals(decision) ? "approved" : "rejected";
            return new ReviewResult(true, "Request " + verb + " successfully.");
        } catch (SQLException e) {
            return new ReviewResult(false, extractSpMessage(e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void ensureStudent() {
        if (studentId < 0)
            throw new IllegalStateException("Call initStudent() first.");
    }

    private void ensureAdmin() {
        if (adminUserId < 0)
            throw new IllegalStateException("Call initAdmin() first.");
    }

    private String extractSpMessage(String jdbcMsg) {
        if (jdbcMsg == null) return "Operation failed. Please try again.";
        int colon = jdbcMsg.indexOf(':');
        return (colon >= 0 && colon < jdbcMsg.length() - 1)
            ? jdbcMsg.substring(colon + 1).trim()
            : jdbcMsg;
    }
}