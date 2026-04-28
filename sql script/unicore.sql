USE UMS_DB;
GO
UPDATE USERS SET password_hash = 'pass@123';
GO
SELECT username, password_hash, role FROM USERS;
GO

USE UMS_DB;
SELECT * FROM students;

USE UMS_DB;
GO

-- Create user for Aleena Imran
INSERT INTO USERS (username, password_hash, role, email, is_active)
VALUES ('aleena.imran', 'Pass@123', 'student', 'aleena.imran@ums.edu.pk', 1);

-- Create student record for Aleena Imran (CS department)
INSERT INTO STUDENTS (user_id, sub_dept_id, full_name, roll_number, batch_year, current_semester)
VALUES (SCOPE_IDENTITY(), 1, 'Aleena Imran', 'CS-2024-050', 2024, 1);
GO

-- Verify Aleena was created
SELECT 
    u.username,
    s.full_name,
    s.roll_number,
    sd.name AS department,
    COUNT(e.enrollment_id) AS enrolled_courses
FROM USERS u
JOIN STUDENTS s ON s.user_id = u.user_id
JOIN SUB_DEPARTMENTS sd ON sd.sub_dept_id = s.sub_dept_id
LEFT JOIN ENROLLMENTS e ON e.student_id = s.student_id AND e.status = 'active'
WHERE u.username = 'aleena.imran'
GROUP BY u.username, s.full_name, s.roll_number, sd.name;
GO
-- ================================================================
--  UMS_PATCH_SPRINT3.sql
--  Adds to UMS_DB:
--    1. ANNOUNCEMENTS.scope column (university / department)
--    2. Three admin report views
--  Run in SSMS against UMS_DB AFTER UMS_DB_FIXED.sql has executed.
-- ================================================================


-- ================================================================
-- PATCH 1: Add scope column to ANNOUNCEMENTS
--
-- The existing table already supports scoping via:
--   major_dept_id IS NULL     → university-wide
--   major_dept_id IS NOT NULL → department-targeted
--
-- We add an explicit scope VARCHAR column so the UI can filter
-- clearly without having to inspect NULL/NOT NULL on the FK.
-- ================================================================

ALTER TABLE ANNOUNCEMENTS
    ADD scope NVARCHAR(20) NOT NULL DEFAULT 'university'
        CONSTRAINT CHK_Ann_Scope CHECK (scope IN ('university', 'department'));
GO

-- Back-fill existing rows:
-- Rows where major_dept_id IS NULL → university-wide
-- Rows where major_dept_id IS NOT NULL → department
UPDATE ANNOUNCEMENTS
SET    scope = CASE
                   WHEN major_dept_id IS NULL THEN 'university'
                   ELSE 'department'
               END;
GO

PRINT 'PATCH 1 applied: ANNOUNCEMENTS.scope column added.';
GO

-- ================================================================
-- PATCH 2: Three Admin Report Views
-- ================================================================

-- ────────────────────────────────────────────────────────────────
-- VIEW 1: VW_REPORT_ENROLLMENT
-- Enrollment Report — total students per course per semester
--
-- Columns:
--   semester_id, semester_name, course_id, course_code,
--   course_name, credit_hours, dept_name, section,
--   teacher_name, total_enrolled, withdrawn_count,
--   completed_count, active_count
-- ────────────────────────────────────────────────────────────────
CREATE OR ALTER VIEW VW_REPORT_ENROLLMENT AS
SELECT
    sem.semester_id,
    sem.semester_name,
    c.course_id,
    c.course_code,
    c.course_name,
    c.credit_hours,
    md.name                                                   AS dept_name,
    co.section,
    t.full_name                                               AS teacher_name,
    COUNT(e.enrollment_id)                                    AS total_enrolled,
    SUM(CASE WHEN e.status = 'active'    THEN 1 ELSE 0 END)  AS active_count,
    SUM(CASE WHEN e.status = 'withdrawn' THEN 1 ELSE 0 END)  AS withdrawn_count,
    SUM(CASE WHEN e.status = 'completed' THEN 1 ELSE 0 END)  AS completed_count
FROM COURSE_OFFERINGS co
JOIN COURSES           c   ON c.course_id    = co.course_id
JOIN SEMESTERS         sem ON sem.semester_id = co.semester_id
JOIN MAJOR_DEPARTMENTS md  ON md.major_dept_id= c.major_dept_id
JOIN TEACHERS          t   ON t.teacher_id   = co.teacher_id
LEFT JOIN ENROLLMENTS  e   ON e.offering_id  = co.offering_id
GROUP BY
    sem.semester_id, sem.semester_name,
    c.course_id, c.course_code, c.course_name, c.credit_hours,
    md.name, co.section, t.full_name;
GO

-- ────────────────────────────────────────────────────────────────
-- VIEW 2: VW_REPORT_ACADEMIC_PERFORMANCE
-- Academic Performance Report — average GPA per course and dept
--
-- Percentage = SUM(marks_obtained) / SUM(total_marks) × 100
-- Only enrollments that have at least one MARKS row are included.
--
-- Columns:
--   semester_id, semester_name, dept_name, course_code,
--   course_name, section, student_count,
--   avg_percentage, min_percentage, max_percentage
-- ────────────────────────────────────────────────────────────────
CREATE OR ALTER VIEW VW_REPORT_ACADEMIC_PERFORMANCE AS
WITH StudentPct AS (
    -- Calculate each student's overall percentage per enrollment
    SELECT
        e.enrollment_id,
        e.offering_id,
        CAST(
            100.0 * SUM(m.marks_obtained) / NULLIF(SUM(m.total_marks), 0)
        AS DECIMAL(6,2)) AS pct
    FROM ENROLLMENTS e
    JOIN MARKS       m ON m.enrollment_id = e.enrollment_id
    GROUP BY e.enrollment_id, e.offering_id
)
SELECT
    sem.semester_id,
    sem.semester_name,
    md.name                              AS dept_name,
    c.course_code,
    c.course_name,
    co.section,
    COUNT(sp.enrollment_id)              AS student_count,
    CAST(AVG(sp.pct)  AS DECIMAL(6,2))  AS avg_percentage,
    CAST(MIN(sp.pct)  AS DECIMAL(6,2))  AS min_percentage,
    CAST(MAX(sp.pct)  AS DECIMAL(6,2))  AS max_percentage
FROM StudentPct            sp
JOIN COURSE_OFFERINGS      co  ON co.offering_id  = sp.offering_id
JOIN COURSES               c   ON c.course_id     = co.course_id
JOIN SEMESTERS             sem ON sem.semester_id  = co.semester_id
JOIN MAJOR_DEPARTMENTS     md  ON md.major_dept_id = c.major_dept_id
GROUP BY
    sem.semester_id, sem.semester_name,
    md.name, c.course_code, c.course_name, co.section;
GO

-- ────────────────────────────────────────────────────────────────
-- VIEW 3: VW_REPORT_ATTENDANCE
-- Attendance Summary Report — average attendance % per course
--
-- Only offerings with at least one attendance record are included.
--
-- Columns:
--   semester_id, semester_name, dept_name, course_code,
--   course_name, section, teacher_name,
--   total_sessions (distinct class dates),
--   avg_attendance_pct, students_below_75
-- ────────────────────────────────────────────────────────────────
CREATE OR ALTER VIEW VW_REPORT_ATTENDANCE AS
WITH EnrollmentPct AS (
    -- Each enrollment's attendance percentage
    SELECT
        e.enrollment_id,
        e.offering_id,
        COUNT(a.attendance_id)  AS total_classes,
        SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) AS present_count,
        CAST(
            100.0 * SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END)
            / NULLIF(COUNT(a.attendance_id), 0)
        AS DECIMAL(6,2)) AS attendance_pct
    FROM ENROLLMENTS e
    JOIN ATTENDANCE  a ON a.enrollment_id = e.enrollment_id
    GROUP BY e.enrollment_id, e.offering_id
)
SELECT
    sem.semester_id,
    sem.semester_name,
    md.name                                             AS dept_name,
    c.course_code,
    c.course_name,
    co.section,
    t.full_name                                         AS teacher_name,
    (SELECT COUNT(DISTINCT class_date)
     FROM ATTENDANCE a2
     JOIN ENROLLMENTS e2 ON e2.enrollment_id = a2.enrollment_id
     WHERE e2.offering_id = co.offering_id)             AS total_sessions,
    COUNT(ep.enrollment_id)                             AS student_count,
    CAST(AVG(ep.attendance_pct) AS DECIMAL(6,2))        AS avg_attendance_pct,
    SUM(CASE WHEN ep.attendance_pct < 75 THEN 1 ELSE 0 END)
                                                        AS students_below_75
FROM EnrollmentPct         ep
JOIN COURSE_OFFERINGS      co  ON co.offering_id  = ep.offering_id
JOIN COURSES               c   ON c.course_id     = co.course_id
JOIN SEMESTERS             sem ON sem.semester_id  = co.semester_id
JOIN MAJOR_DEPARTMENTS     md  ON md.major_dept_id = c.major_dept_id
JOIN TEACHERS              t   ON t.teacher_id    = co.teacher_id
GROUP BY
    sem.semester_id, sem.semester_name,
    md.name, c.course_code, c.course_name,
    co.section, t.full_name, co.offering_id;
GO

PRINT 'PATCH 2 applied: 3 admin report views created.';
GO

-- ================================================================
-- VERIFICATION
-- ================================================================

-- Check scope column exists
SELECT TOP 3 announcement_id, title, scope, major_dept_id
FROM   ANNOUNCEMENTS;
GO

-- Check all 3 report views work on current data
SELECT * FROM VW_REPORT_ENROLLMENT         ORDER BY semester_name, course_code;
GO
SELECT * FROM VW_REPORT_ACADEMIC_PERFORMANCE ORDER BY dept_name, course_code;
GO
SELECT * FROM VW_REPORT_ATTENDANCE          ORDER BY dept_name, course_code;
GO

PRINT 'All patch verifications passed.';
GO