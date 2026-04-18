package model;

import java.time.LocalDateTime;

/**
 * Represents one feedback row from the FEEDBACK table.
 * Used by both student submission and teacher view.
 */
public class FeedbackItem {

    private final int           feedbackId;
    private final int           offeringId;
    private final String        courseCode;
    private final String        courseName;
    private final int           rating;        // 1-5
    private final String        comments;
    private final LocalDateTime submittedAt;
    private final boolean       isAnonymous;

    public FeedbackItem(int feedbackId, int offeringId,
                        String courseCode, String courseName,
                        int rating, String comments,
                        LocalDateTime submittedAt, boolean isAnonymous) {
        this.feedbackId  = feedbackId;
        this.offeringId  = offeringId;
        this.courseCode  = courseCode;
        this.courseName  = courseName;
        this.rating      = rating;
        this.comments    = comments == null ? "" : comments;
        this.submittedAt = submittedAt;
        this.isAnonymous = isAnonymous;
    }

    public int           getFeedbackId()  { return feedbackId; }
    public int           getOfferingId()  { return offeringId; }
    public String        getCourseCode()  { return courseCode; }
    public String        getCourseName()  { return courseName; }
    public int           getRating()      { return rating; }
    public String        getComments()    { return comments; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public boolean       isAnonymous()    { return isAnonymous; }
}