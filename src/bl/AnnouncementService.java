package bl;

import dao.AnnouncementDAO;
import model.AnnouncementItem;
import model.TeacherOfferingItem;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business-Logic layer for the Teacher Announcements feature.
 *
 * Responsibilities:
 *   - Resolve username → teacher_id and user_id (cached for the session)
 *   - Validate that a course was selected before posting
 *   - Validate that a title and message are not blank
 *   - Enforce a reasonable message length limit
 *   - Delegate all DB reads/writes to AnnouncementDAO
 *   - Return typed PostResult objects so the UI never interprets raw strings
 *
 * This class contains zero Swing code and zero SQL.
 */
public class AnnouncementService {

    // ── Result types returned to the UI ──────────────────────────────────────

    /**
     * Outcome of a post-announcement attempt.
     *
     * @param outcome       SUCCESS | NO_COURSE | NO_TITLE | NO_MESSAGE |
     *                      MESSAGE_TOO_LONG | ERROR
     * @param message       human-readable feedback for the status label
     * @param announcement  the newly created AnnouncementItem (non-null on SUCCESS)
     */
    public record PostResult(
        Outcome          outcome,
        String           message,
        AnnouncementItem announcement   // null unless outcome == SUCCESS
    ) {}

    public enum Outcome {
        SUCCESS,
        NO_COURSE,
        NO_TITLE,
        NO_MESSAGE,
        MESSAGE_TOO_LONG,
        ERROR
    }

    // ── Configuration constants ───────────────────────────────────────────────
    /** Maximum allowed characters in the message body. */
    public static final int MAX_MESSAGE_LENGTH = 1000;

    /** Default target role sent to the DB when the teacher posts. */
    private static final String DEFAULT_TARGET_ROLE = "student";

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final AnnouncementDAO dao;

    /** Cached after init(); valid for the lifetime of this panel session. */
    private int teacherId = -1;
    private int userId    = -1;

    // ── Constructors ─────────────────────────────────────────────────────────
    public AnnouncementService() {
        this.dao = new AnnouncementDAO();
    }

    /** Injection constructor for unit tests. */
    public AnnouncementService(AnnouncementDAO dao) {
        this.dao = dao;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the service for the logged-in teacher.
     * Must be called once before any other method.
     *
     * @param username  login username from the session
     * @return true if the teacher account was found; false otherwise
     */
    public boolean init(String username) {
        try {
            teacherId = dao.getTeacherId(username);
            userId    = dao.getUserId(username);
            return teacherId > 0 && userId > 0;
        } catch (SQLException e) {
            teacherId = -1;
            userId    = -1;
            return false;
        }
    }

    /**
     * Returns all course offerings the teacher can post announcements to.
     *
     * @return list of TeacherOfferingItem for the dropdown (never null)
     */
    public List<TeacherOfferingItem> getOfferings() {
        ensureInitialised();
        try {
            return dao.getOfferingsForTeacher(teacherId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Returns announcements the teacher has already posted (newest first).
     *
     * @return list of AnnouncementItem for the right-hand panel (never null)
     */
    public List<AnnouncementItem> getPostedAnnouncements() {
        ensureInitialised();
        try {
            return dao.getPostedAnnouncements(teacherId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * Validates the form inputs and, if valid, persists the announcement.
     *
     * Business rules enforced here:
     *   1. A course offering must be selected (not the placeholder).
     *   2. Title must not be blank.
     *   3. Message body must not be blank.
     *   4. Message body must not exceed MAX_MESSAGE_LENGTH characters.
     *
     * @param selectedOffering  offering chosen in the dropdown (null = placeholder)
     * @param title             content of the title field
     * @param message           content of the message text area
     * @return PostResult describing exactly what happened
     */
    public PostResult post(TeacherOfferingItem selectedOffering,
                           String title, String message) {
        ensureInitialised();

        // Rule 1: course must be selected
        if (selectedOffering == null) {
            return new PostResult(Outcome.NO_COURSE,
                "Please select a course.", null);
        }

        // Rule 2: title required
        String cleanTitle = (title == null) ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            return new PostResult(Outcome.NO_TITLE,
                "Please enter a title.", null);
        }

        // Rule 3: message required
        String cleanMsg = (message == null) ? "" : message.trim();
        if (cleanMsg.isEmpty()) {
            return new PostResult(Outcome.NO_MESSAGE,
                "Please enter a message.", null);
        }

        // Rule 4: length guard
        if (cleanMsg.length() > MAX_MESSAGE_LENGTH) {
            return new PostResult(Outcome.MESSAGE_TOO_LONG,
                "Message exceeds " + MAX_MESSAGE_LENGTH + " characters ("
                    + cleanMsg.length() + " entered).", null);
        }

        // Persist
        int newId;
        try {
            newId = dao.postAnnouncement(
                userId,
                selectedOffering.getOfferingId(),
                cleanTitle,
                cleanMsg,
                DEFAULT_TARGET_ROLE
            );
        } catch (SQLException e) {
            return new PostResult(Outcome.ERROR,
                "Failed to post announcement: " + e.getMessage(), null);
        }

        AnnouncementItem created = new AnnouncementItem(
            newId,
            selectedOffering.getOfferingId(),
            selectedOffering.getCourseCode(),
            selectedOffering.getCourseName(),
            cleanTitle,
            cleanMsg,
            DEFAULT_TARGET_ROLE,
            LocalDateTime.now()
        );

        return new PostResult(Outcome.SUCCESS,
            "Announcement posted successfully!", created);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void ensureInitialised() {
        if (teacherId < 0 || userId < 0) {
            throw new IllegalStateException(
                "AnnouncementService.init() must be called before any other method.");
        }
    }
}
