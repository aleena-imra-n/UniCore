package model;

import java.time.LocalDate;

/**
 * View-model for one row in the Manage Semesters table (UC-20 / US-3.12a).
 *
 * Maps directly to the SEMESTERS table columns:
 *   semester_id, semester_name, start_date, end_date,
 *   is_active, enrollment_open
 */
public class SemesterRow {

    private final int       semesterId;
    private final String    semesterName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private       boolean   active;
    private       boolean   enrollmentOpen;

    public SemesterRow(int semesterId, String semesterName,
                       LocalDate startDate, LocalDate endDate,
                       boolean active, boolean enrollmentOpen) {
        this.semesterId      = semesterId;
        this.semesterName    = semesterName;
        this.startDate       = startDate;
        this.endDate         = endDate;
        this.active          = active;
        this.enrollmentOpen  = enrollmentOpen;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int       getSemesterId()     { return semesterId; }
    public String    getSemesterName()   { return semesterName; }
    public LocalDate getStartDate()      { return startDate; }
    public LocalDate getEndDate()        { return endDate; }
    public boolean   isActive()          { return active; }
    public boolean   isEnrollmentOpen()  { return enrollmentOpen; }

    // ── Mutators (updated after set-active / toggle-enrollment) ──────────────
    public void setActive(boolean active)                  { this.active = active; }
    public void setEnrollmentOpen(boolean enrollmentOpen)  { this.enrollmentOpen = enrollmentOpen; }

    @Override
    public String toString() { return semesterName; }
}
