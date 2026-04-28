package bl;

import dao.AdminAnnouncementDAO;
import model.AdminAnnouncementItem;
import model.AdminAnnouncementItem.Scope;
import model.DeptOption;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic for Admin Send Announcements.
 *
 * Rules enforced here:
 *   - Title must not be blank
 *   - Content must not be blank, max 2000 characters
 *   - When scope = DEPARTMENT, a department must be selected
 *   - targetRole must be one of: all / student / teacher
 *   - Deactivation requires confirmed = true from caller
 */
public class AdminAnnouncementService {

    public record PostResult(boolean success, String message,
                             AdminAnnouncementItem created) {}
    public record ActionResult(boolean success, String message) {}

    public static final int MAX_CONTENT_LENGTH = 2000;
    public static final String[] TARGET_ROLES  = {"all", "student", "teacher"};

    private final AdminAnnouncementDAO dao;

    private int    userId   = -1;
    private String username = "";

    public AdminAnnouncementService() { this.dao = new AdminAnnouncementDAO(); }

    public AdminAnnouncementService(AdminAnnouncementDAO dao) { this.dao = dao; }

    // ── Init ─────────────────────────────────────────────────────────────────

    /**
     * Resolves the admin username to a user_id. Must be called before anything else.
     *
     * @return true if the user was found
     */
    public boolean init(String adminUsername) {
        try {
            this.username = adminUsername;
            this.userId   = dao.getUserId(adminUsername);
            return userId > 0;
        } catch (SQLException e) {
            userId = -1;
            return false;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns all departments for the scope dropdown. */
    public List<DeptOption> loadDepts() {
        try { return dao.getAllDepts(); }
        catch (SQLException e) { return List.of(); }
    }

    /** Returns announcements this admin has already posted, newest first. */
    public List<AdminAnnouncementItem> loadPosted() {
        ensureInit();
        try { return dao.getPostedByAdmin(userId); }
        catch (SQLException e) { return List.of(); }
    }

    /**
     * Validates and posts an announcement.
     *
     * @param scope        UNIVERSITY or DEPARTMENT
     * @param majorDeptId  required when scope = DEPARTMENT, ignored otherwise
     * @param title        announcement title (required)
     * @param content      message body (required, max 2000 chars)
     * @param targetRole   "all" | "student" | "teacher"
     */
    public PostResult post(Scope scope, Integer majorDeptId,
                           String title, String content, String targetRole) {
        ensureInit();

        // ── Validate ─────────────────────────────────────────────────────────
        String cleanTitle   = title   == null ? "" : title.trim();
        String cleanContent = content == null ? "" : content.trim();

        if (cleanTitle.isEmpty())
            return new PostResult(false, "Title is required.", null);

        if (cleanContent.isEmpty())
            return new PostResult(false, "Message content is required.", null);

        if (cleanContent.length() > MAX_CONTENT_LENGTH)
            return new PostResult(false,
                "Content exceeds " + MAX_CONTENT_LENGTH + " characters ("
                    + cleanContent.length() + " entered).", null);

        if (scope == Scope.DEPARTMENT && (majorDeptId == null || majorDeptId < 0))
            return new PostResult(false,
                "Please select a department for department-scoped announcements.", null);

        // Normalise targetRole
        String role = (targetRole == null) ? "all" : targetRole.toLowerCase().trim();
        if (!role.equals("all") && !role.equals("student") && !role.equals("teacher"))
            role = "all";

        // ── Persist ───────────────────────────────────────────────────────────
        try {
            int newId;
            AdminAnnouncementItem created;

            if (scope == Scope.UNIVERSITY) {
                newId = dao.postUniversity(userId, cleanTitle, cleanContent, role);
                created = new AdminAnnouncementItem(newId, cleanTitle, cleanContent,
                    Scope.UNIVERSITY, role, null, null,
                    java.time.LocalDateTime.now(), true);
            } else {
                newId = dao.postDepartment(userId, majorDeptId,
                                           cleanTitle, cleanContent, role);
                // Look up dept name from already-loaded list — pass null; UI resolves
                created = new AdminAnnouncementItem(newId, cleanTitle, cleanContent,
                    Scope.DEPARTMENT, role, majorDeptId, null,
                    java.time.LocalDateTime.now(), true);
            }

            if (newId < 0)
                return new PostResult(false, "Insert failed — no ID returned.", null);

            return new PostResult(true, "Announcement posted successfully.", created);

        } catch (SQLException e) {
            return new PostResult(false, "DB error: " + e.getMessage(), null);
        }
    }

    /**
     * Soft-deletes an announcement. Caller must pass confirmed = true.
     */
    public ActionResult deactivate(int announcementId, boolean confirmed) {
        if (!confirmed)
            return new ActionResult(false, "Deactivation cancelled.");
        try {
            return dao.deactivate(announcementId)
                ? new ActionResult(true,  "Announcement removed.")
                : new ActionResult(false, "Announcement not found.");
        } catch (SQLException e) {
            return new ActionResult(false, "DB error: " + e.getMessage());
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────
    private void ensureInit() {
        if (userId < 0)
            throw new IllegalStateException(
                "AdminAnnouncementService.init() must be called first.");
    }
}
