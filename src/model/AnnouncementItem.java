package model;

import java.time.LocalDateTime;

/**
 * Represents one announcement as stored in and retrieved from the database.
 *
 * Used by:
 *   - DAO     → populated from ANNOUNCEMENTS + COURSE_OFFERINGS + COURSES
 *   - Service → passes to/from UI after validation
 *   - UI      → renders each row in the posted-announcements panel
 */
public class AnnouncementItem {

    private final int           announcementId;   // PK from ANNOUNCEMENTS
    private final int           offeringId;       // FK to COURSE_OFFERINGS (null = dept-wide)
    private final String        courseCode;       // e.g. "CS3009"  (display only)
    private final String        courseName;       // e.g. "Software Engineering"
    private final String        title;            // short subject line
    private final String        content;          // full message body
    private final String        targetRole;       // "all" | "student" | "teacher"
    private final LocalDateTime postedAt;

    public AnnouncementItem(int announcementId, int offeringId,
                            String courseCode, String courseName,
                            String title, String content,
                            String targetRole, LocalDateTime postedAt) {
        this.announcementId = announcementId;
        this.offeringId     = offeringId;
        this.courseCode     = courseCode;
        this.courseName     = courseName;
        this.title          = title;
        this.content        = content;
        this.targetRole     = targetRole;
        this.postedAt       = postedAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int           getAnnouncementId() { return announcementId; }
    public int           getOfferingId()     { return offeringId; }
    public String        getCourseCode()     { return courseCode; }
    public String        getCourseName()     { return courseName; }
    public String        getTitle()          { return title; }
    public String        getContent()        { return content; }
    public String        getTargetRole()     { return targetRole; }
    public LocalDateTime getPostedAt()       { return postedAt; }

    /** Convenience label shown in list rows. */
    public String getCourseLabel() {
        return courseCode + "  |  " + courseName;
    }

    @Override
    public String toString() {
        return "AnnouncementItem{id=" + announcementId
               + ", course=" + courseCode
               + ", postedAt=" + postedAt + "}";
    }
}
