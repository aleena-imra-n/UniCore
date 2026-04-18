package bl;

import dao.UserManagementDAO;
import model.SubDeptItem;
import model.UserRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business-Logic layer for Admin User Management (US-3.8a / US-3.8b).
 *
 * Responsibilities:
 *   - Resolve admin username → user_id (cached per session)
 *   - Validate all form inputs before SP calls
 *   - Filter the user list by role / department / search term (in-memory,
 *     avoids a round-trip per keystroke)
 *   - Return typed result objects so the UI never interprets raw strings
 *
 * Zero Swing code. Zero SQL.
 */
public class UserManagementService {

    // ── Result types ──────────────────────────────────────────────────────────

    public record CreateResult(boolean success, String message, int newUserId) {}
    public record UpdateResult(boolean success, String message) {}
    public record DeactivateResult(boolean success, String message) {}

    // ── Validation constants ───────────────────────────────────────────────────
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_NAME_LENGTH     = 100;
    public static final int MAX_EMAIL_LENGTH    = 100;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final UserManagementDAO dao;
    private int adminUserId = -1;

    public UserManagementService()                     { this.dao = new UserManagementDAO(); }
    public UserManagementService(UserManagementDAO dao){ this.dao = dao; }

    // ── Init ──────────────────────────────────────────────────────────────────

    /**
     * Must be called once before any other method.
     * @return true if an active admin account was found for this username
     */
    public boolean init(String username) {
        try {
            adminUserId = dao.getAdminUserId(username);
            return adminUserId > 0;
        } catch (SQLException e) {
            adminUserId = -1;
            return false;
        }
    }

    // ── Data loaders ──────────────────────────────────────────────────────────

    /** All users (students + teachers, all statuses). */
    public List<UserRecord> getAllUsers() {
        ensureInit();
        try { return dao.getAllUsers(); }
        catch (SQLException e) { return List.of(); }
    }

    /** All sub-departments for form combo boxes. */
    public List<SubDeptItem> getAllSubDepts() {
        ensureInit();
        try { return dao.getAllSubDepts(); }
        catch (SQLException e) { return List.of(); }
    }

    // ── Filtering (in-memory) ─────────────────────────────────────────────────

    /**
     * Filters a pre-loaded user list by role, majorDeptId, and a free-text
     * search term (matched against name, username, roll/emp-code, email).
     *
     * @param users        full list from {@link #getAllUsers()}
     * @param roleFilter   "student" | "teacher" | null (all)
     * @param majorDeptId  > 0 to filter by faculty; ≤ 0 = all
     * @param searchTerm   case-insensitive substring match; null or blank = no filter
     * @param activeOnly   true = hide deactivated accounts
     */
    public List<UserRecord> filter(List<UserRecord> users,
                                   String roleFilter,
                                   int    majorDeptId,
                                   String searchTerm,
                                   boolean activeOnly) {
        String term = (searchTerm == null) ? "" : searchTerm.trim().toLowerCase();

        return users.stream()
            .filter(u -> roleFilter == null || roleFilter.equalsIgnoreCase(u.getRole()))
            .filter(u -> majorDeptId <= 0 || u.getMajorDeptId() == majorDeptId)
            .filter(u -> !activeOnly || u.isActive())
            .filter(u -> term.isEmpty()
                || u.getFullName().toLowerCase().contains(term)
                || u.getUsername().toLowerCase().contains(term)
                || (u.getRollNumber()    != null && u.getRollNumber().toLowerCase().contains(term))
                || (u.getEmployeeCode()  != null && u.getEmployeeCode().toLowerCase().contains(term))
                || u.getEmail().toLowerCase().contains(term))
            .collect(Collectors.toList());
    }

    // ── CRUD operations ───────────────────────────────────────────────────────

    /**
     * Validates inputs then calls SP_CreateUser.
     *
     * For students: rollNumber, batchYear required; empCode/designation ignored.
     * For teachers: empCode required; rollNumber/batchYear ignored.
     */
    public CreateResult createUser(String role, String fullName, String username,
                                   String password, String email,
                                   Integer subDeptId,
                                   String rollNumber, Integer batchYear,
                                   String empCode, String designation) {
        ensureInit();

        // ── Common validation ─────────────────────────────────────────────────
        if (role == null || (!role.equals("student") && !role.equals("teacher")))
            return new CreateResult(false, "Role must be 'student' or 'teacher'.", -1);

        String name = trim(fullName);
        if (name.isEmpty())
            return new CreateResult(false, "Full name is required.", -1);
        if (name.length() > MAX_NAME_LENGTH)
            return new CreateResult(false, "Full name exceeds " + MAX_NAME_LENGTH + " chars.", -1);

        String uname = trim(username);
        if (uname.isEmpty())
            return new CreateResult(false, "Username is required.", -1);
        if (uname.contains(" "))
            return new CreateResult(false, "Username must not contain spaces.", -1);

        String pw = password == null ? "" : password;
        if (pw.length() < MIN_PASSWORD_LENGTH)
            return new CreateResult(false,
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.", -1);

        String em = trim(email);
        if (em.isEmpty() || !em.contains("@"))
            return new CreateResult(false, "A valid email address is required.", -1);

        if (subDeptId == null || subDeptId <= 0)
            return new CreateResult(false, "Department selection is required.", -1);

        // ── Role-specific validation ───────────────────────────────────────────
        if ("student".equals(role)) {
            if (trim(rollNumber).isEmpty())
                return new CreateResult(false, "Roll number is required for students.", -1);
            if (batchYear == null || batchYear < 2000 || batchYear > 2100)
                return new CreateResult(false, "A valid batch year is required.", -1);
        } else {
            if (trim(empCode).isEmpty())
                return new CreateResult(false, "Employee code is required for teachers.", -1);
        }

        try {
            int newId = dao.createUser(role, name, uname, pw, em,
                subDeptId, null,
                "student".equals(role) ? trim(rollNumber) : null,
                "student".equals(role) ? batchYear : null,
                "teacher".equals(role) ? trim(empCode) : null,
                "teacher".equals(role) ? trim(designation) : null);

            if (newId < 0)
                return new CreateResult(false, "User created but ID not returned.", -1);

            return new CreateResult(true,
                name + " added successfully (user ID " + newId + ").", newId);

        } catch (SQLException e) {
            return new CreateResult(false, extractSpMsg(e.getMessage()), -1);
        }
    }

    /**
     * Validates inputs then calls SP_UpdateUser.
     * newPassword = null or blank means password is left unchanged.
     */
    public UpdateResult updateUser(UserRecord existing,
                                   String fullName, String email,
                                   String newPassword,
                                   Integer subDeptId,
                                   String designation,
                                   String rollNumber, Integer batchYear,
                                   Integer currentSemester) {
        ensureInit();

        String name = trim(fullName);
        if (name.isEmpty())
            return new UpdateResult(false, "Full name is required.");
        if (name.length() > MAX_NAME_LENGTH)
            return new UpdateResult(false, "Full name exceeds " + MAX_NAME_LENGTH + " chars.");

        String em = trim(email);
        if (em.isEmpty() || !em.contains("@"))
            return new UpdateResult(false, "A valid email address is required.");

        String pw = (newPassword == null || newPassword.isBlank()) ? null : newPassword.trim();
        if (pw != null && pw.length() < MIN_PASSWORD_LENGTH)
            return new UpdateResult(false,
                "New password must be at least " + MIN_PASSWORD_LENGTH + " characters.");

        if (subDeptId == null || subDeptId <= 0)
            return new UpdateResult(false, "Department selection is required.");

        if (existing.isStudent()) {
            if (trim(rollNumber).isEmpty())
                return new UpdateResult(false, "Roll number is required.");
            if (batchYear == null || batchYear < 2000 || batchYear > 2100)
                return new UpdateResult(false, "A valid batch year is required.");
        }

        try {
            dao.updateUser(existing.getUserId(), name, em, pw, subDeptId,
                existing.isTeacher() ? trim(designation) : null,
                existing.isStudent() ? trim(rollNumber) : null,
                existing.isStudent() ? batchYear : null,
                existing.isStudent() ? currentSemester : null);

            return new UpdateResult(true, name + " updated successfully.");
        } catch (SQLException e) {
            return new UpdateResult(false, extractSpMsg(e.getMessage()));
        }
    }

    /**
     * Calls SP_DeactivateUser. Prompts for confirmation are handled by the UI.
     */
    public DeactivateResult deactivateUser(int userId) {
        ensureInit();
        try {
            dao.deactivateUser(userId, adminUserId);
            return new DeactivateResult(true, "User account deactivated.");
        } catch (SQLException e) {
            return new DeactivateResult(false, extractSpMsg(e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void ensureInit() {
        if (adminUserId < 0)
            throw new IllegalStateException("Call init() before any other method.");
    }

    private String trim(String s) { return s == null ? "" : s.trim(); }

    private String extractSpMsg(String jdbcMsg) {
        if (jdbcMsg == null) return "Operation failed. Please try again.";
        int colon = jdbcMsg.indexOf(':');
        return (colon >= 0 && colon < jdbcMsg.length() - 1)
            ? jdbcMsg.substring(colon + 1).trim()
            : jdbcMsg;
    }
}