package bl;

import dao.FeedbackDAO;
import model.FeedbackItem;
import model.OfferingItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for the Feedback feature.
 * Shared by both student (submit) and teacher (view) panels.
 * Zero Swing. Zero SQL.
 */
public class FeedbackService {

    public record SubmitResult(Outcome outcome, String message) {}

    public enum Outcome {
        SUCCESS, ALREADY_SUBMITTED, NO_COURSE, INVALID_RATING, ERROR
    }

    private final FeedbackDAO dao;
    private int     studentId   = -1;
    private boolean initialised = false;
    private String  username;

    public FeedbackService()            { this.dao = new FeedbackDAO(); }
    public FeedbackService(FeedbackDAO d){ this.dao = d; }

    /** Init for student. */
    public boolean initStudent(String username) {
        this.username = username;
        try {
            studentId   = dao.getStudentId(username);
            initialised = studentId > 0;
            return initialised;
        } catch (SQLException e) { return false; }
    }

    /** Init for teacher (just stores username — no student_id needed). */
    public boolean initTeacher(String username) {
        this.username   = username;
        this.initialised = username != null && !username.isBlank();
        return initialised;
    }

    // ── Student API ───────────────────────────────────────────────────────────

    public List<OfferingItem> getEnrolledOfferings() {
        ensureInit();
        try { return dao.getEnrolledOfferings(username); }
        catch (SQLException e) { return List.of(); }
    }

    public List<FeedbackItem> getSubmittedFeedback() {
        ensureInit();
        try { return dao.getSubmittedFeedback(studentId); }
        catch (SQLException e) { return List.of(); }
    }

    /**
     * Validates and submits feedback.
     * rating = average of the 4 criteria (1–5), rounded to nearest int.
     */
    public SubmitResult submit(OfferingItem offering, int[] criteriaRatings,
                               String comments) {
        ensureInit();
        if (offering == null)
            return new SubmitResult(Outcome.NO_COURSE, "Please select a course.");

        for (int r : criteriaRatings)
            if (r < 1 || r > 5)
                return new SubmitResult(Outcome.INVALID_RATING,
                    "Please rate all criteria (1–5).");

        // Compute average rating
        int sum = 0;
        for (int r : criteriaRatings) sum += r;
        int avgRating = (int) Math.round((double) sum / criteriaRatings.length);

        try {
            if (dao.hasAlreadySubmitted(studentId, offering.getOfferingId()))
                return new SubmitResult(Outcome.ALREADY_SUBMITTED,
                    "You have already submitted feedback for this course.");

            dao.submitFeedback(studentId, offering.getOfferingId(),
                avgRating, comments == null ? "" : comments.trim());
            return new SubmitResult(Outcome.SUCCESS,
                "Feedback submitted successfully!");
        } catch (SQLException e) {
            return new SubmitResult(Outcome.ERROR,
                "Failed to submit: " + e.getMessage());
        }
    }

    // ── Teacher API ───────────────────────────────────────────────────────────

    /** Returns offerings with at least 1 feedback + count + avg rating. */
    public List<Object[]> getOfferingsWithFeedback() {
        ensureInit();
        try { return dao.getOfferingsWithFeedback(username); }
        catch (SQLException e) { return List.of(); }
    }

    /** Returns all (anonymous) feedback for one offering. */
    public List<FeedbackItem> getFeedbackForOffering(int offeringId) {
        ensureInit();
        try { return dao.getFeedbackForOffering(offeringId); }
        catch (SQLException e) { return List.of(); }
    }

    private void ensureInit() {
        if (!initialised)
            throw new IllegalStateException("FeedbackService.init() must be called first.");
    }
}