package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents one announcement visible to a student.
 *
 * Used by:
 *   - StudentAnnouncementDAO  → populated from ANNOUNCEMENTS + joins
 *   - StudentAnnouncementService → passed to UI after loading
 *   - StudentAnnouncementsPanel  → renders each announcement card
 *
 * scopeType is one of: "Course" | "Department" | "University"
 */
public class StudentAnnouncementItem {

    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a");

    private final int           announcementId;
    private final String        title;
    private final String        content;
    private final String        courseName;    // course name, dept name, or "University-wide"
    private final String        courseCode;    // course code or dept code (empty for university)
    private final String        scopeType;     // "Course" | "Department" | "University"
    private final String        postedByName;  // teacher name or admin username
    private final LocalDateTime postedAt;

    public StudentAnnouncementItem(int announcementId, String title, String content,
                                   String courseName, String courseCode,
                                   String scopeType, String postedByName,
                                   LocalDateTime postedAt) {
        this.announcementId = announcementId;
        this.title          = title;
        this.content        = content;
        this.courseName     = courseName;
        this.courseCode     = courseCode;
        this.scopeType      = scopeType;
        this.postedByName   = postedByName;
        this.postedAt       = postedAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int           getAnnouncementId() { return announcementId; }
    public String        getTitle()          { return title; }
    public String        getContent()        { return content; }
    public String        getCourseName()     { return courseName; }
    public String        getCourseCode()     { return courseCode; }
    public String        getScopeType()      { return scopeType; }
    public String        getPostedByName()   { return postedByName; }
    public LocalDateTime getPostedAt()       { return postedAt; }

    /** Human-readable timestamp for display. */
    public String getFormattedDate() {
        return postedAt != null ? postedAt.format(DISPLAY_FMT) : "—";
    }

    /**
     * Label shown on the scope badge.
     * e.g. "CS301 · Course" or "Computing · Department"
     */
    public String getScopeLabel() {
        if (courseCode != null && !courseCode.isEmpty()) {
            return courseCode + "  ·  " + scopeType;
        }
        return scopeType;
    }

    @Override
    public String toString() {
        return "StudentAnnouncementItem{id=" + announcementId
               + ", title=" + title
               + ", scope=" + scopeType
               + ", postedAt=" + postedAt + "}";
    }
}