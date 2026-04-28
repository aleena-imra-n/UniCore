package model;

import java.time.LocalDateTime;

/**
 * Represents one announcement posted by an admin.
 * Scope is either "university" (all students/teachers) or
 * "department" (one specific major_dept_id only).
 */
public class AdminAnnouncementItem {

    public enum Scope { UNIVERSITY, DEPARTMENT }

    private final int           announcementId;
    private final String        title;
    private final String        content;
    private final Scope         scope;
    private final String        targetRole;      // "all" | "student" | "teacher"
    private final Integer       majorDeptId;     // null when scope = UNIVERSITY
    private final String        deptName;        // null when scope = UNIVERSITY
    private final LocalDateTime postedAt;
    private final boolean       isActive;

    public AdminAnnouncementItem(int announcementId, String title, String content,
                                 Scope scope, String targetRole,
                                 Integer majorDeptId, String deptName,
                                 LocalDateTime postedAt, boolean isActive) {
        this.announcementId = announcementId;
        this.title          = title;
        this.content        = content;
        this.scope          = scope;
        this.targetRole     = targetRole;
        this.majorDeptId    = majorDeptId;
        this.deptName       = deptName;
        this.postedAt       = postedAt;
        this.isActive       = isActive;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int           getAnnouncementId() { return announcementId; }
    public String        getTitle()          { return title; }
    public String        getContent()        { return content; }
    public Scope         getScope()          { return scope; }
    public String        getTargetRole()     { return targetRole; }
    public Integer       getMajorDeptId()    { return majorDeptId; }
    public boolean       isActive()          { return isActive; }

    public LocalDateTime getPostedAt()       { return postedAt; }

    /** Human-readable scope label for the UI badge. */
    public String getScopeLabel() {
        return scope == Scope.UNIVERSITY
            ? "University-wide"
            : (deptName != null ? deptName : "Department");
    }

    @Override public String toString() {
        return title + " [" + getScopeLabel() + "]";
    }
}
