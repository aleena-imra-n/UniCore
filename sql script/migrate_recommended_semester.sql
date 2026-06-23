-- =============================================================================
-- Migration: add recommended_semester + refresh registration views
-- Run once against an EXISTING UMS_DB that was created before this column
-- existed. Safe to re-run (guarded with IF NOT EXISTS / DROP VIEW IF EXISTS).
-- =============================================================================
USE UMS_DB;
GO

-- 1. Add column if missing
IF NOT EXISTS (
    SELECT 1
    FROM   INFORMATION_SCHEMA.COLUMNS
    WHERE  TABLE_NAME = 'COURSES'
      AND  COLUMN_NAME = 'recommended_semester'
)
BEGIN
    ALTER TABLE COURSES
        ADD recommended_semester TINYINT NOT NULL
            CONSTRAINT DF_COURSES_recommended_semester DEFAULT 1;
END
GO

-- 2. Set semester values for seeded catalog courses
UPDATE COURSES SET recommended_semester = 1
WHERE course_code IN (N'CS101', N'CE101', N'EE101', N'BA101');

UPDATE COURSES SET recommended_semester = 3
WHERE course_code IN (N'CS201', N'AI201', N'DS201', N'CY201', N'SE201',
                      N'CE201', N'AF201', N'FT201');

UPDATE COURSES SET recommended_semester = 5
WHERE course_code = N'CS301';
GO

-- 3. Recreate registrable-offerings view (semester gating + prereq filter)
IF OBJECT_ID('VW_STUDENT_REGISTRABLE_OFFERINGS', 'V') IS NOT NULL
    DROP VIEW VW_STUDENT_REGISTRABLE_OFFERINGS;
GO

CREATE VIEW VW_STUDENT_REGISTRABLE_OFFERINGS AS
SELECT
    s.student_id,
    s.roll_number,
    co.offering_id,
    c.course_code,
    c.course_name,
    c.recommended_semester,
    co.section,
    co.room_number,
    co.schedule_days,
    co.class_time,
    co.max_capacity,
    sem.semester_name,
    t.full_name  AS teacher_name
FROM STUDENTS s
JOIN COURSE_OFFERINGS           co  ON co.sub_dept_id  = s.sub_dept_id
JOIN COURSES                    c   ON c.course_id     = co.course_id
JOIN SEMESTERS                  sem ON sem.semester_id = co.semester_id
JOIN TEACHERS                   t   ON t.teacher_id    = co.teacher_id
JOIN COURSE_SUB_DEPT_ELIGIBILITY e
    ON e.course_id   = co.course_id
   AND e.sub_dept_id = s.sub_dept_id
WHERE sem.is_active = 1
  AND c.recommended_semester <= s.current_semester
  AND NOT EXISTS (
      SELECT 1
      FROM   COURSE_PREREQUISITES cp
      WHERE  cp.course_id    = c.course_id
        AND  cp.is_mandatory = 1
        AND  NOT EXISTS (
            SELECT 1
            FROM   ENROLLMENTS       e2
            JOIN   COURSE_OFFERINGS  co2 ON co2.offering_id = e2.offering_id
            JOIN (
                SELECT m.enrollment_id,
                       CAST(100.0 * SUM(m.marks_obtained)
                            / NULLIF(SUM(m.total_marks), 0)
                       AS DECIMAL(5,2)) AS pct
                FROM   MARKS m
                GROUP  BY m.enrollment_id
            ) grades ON grades.enrollment_id = e2.enrollment_id
            WHERE  e2.student_id  = s.student_id
              AND  co2.course_id  = cp.prereq_course_id
              AND  e2.status      = 'completed'
              AND  grades.pct    >= cp.min_grade_pct
        )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM   ENROLLMENTS      e3
      JOIN   COURSE_OFFERINGS co3 ON co3.offering_id = e3.offering_id
      WHERE  e3.student_id  = s.student_id
        AND  co3.course_id  = c.course_id
        AND  e3.status      = 'completed'
  );
GO

-- 4. Create improvement-courses view (grade improvement tab)
IF OBJECT_ID('VW_STUDENT_IMPROVEMENT_COURSES', 'V') IS NOT NULL
    DROP VIEW VW_STUDENT_IMPROVEMENT_COURSES;
GO

CREATE VIEW VW_STUDENT_IMPROVEMENT_COURSES AS
SELECT
    s.student_id,
    s.roll_number,
    co.offering_id,
    c.course_code,
    c.course_name,
    c.recommended_semester,
    co.section,
    co.room_number,
    co.schedule_days,
    co.class_time,
    co.max_capacity,
    sem.semester_name,
    t.full_name  AS teacher_name
FROM STUDENTS s
JOIN COURSE_OFFERINGS           co  ON co.sub_dept_id  = s.sub_dept_id
JOIN COURSES                    c   ON c.course_id     = co.course_id
JOIN SEMESTERS                  sem ON sem.semester_id = co.semester_id
JOIN TEACHERS                   t   ON t.teacher_id    = co.teacher_id
JOIN COURSE_SUB_DEPT_ELIGIBILITY el
    ON el.course_id   = co.course_id
   AND el.sub_dept_id = s.sub_dept_id
WHERE sem.is_active = 1
  AND EXISTS (
      SELECT 1
      FROM   ENROLLMENTS      e4
      JOIN   COURSE_OFFERINGS co4 ON co4.offering_id = e4.offering_id
      WHERE  e4.student_id  = s.student_id
        AND  co4.course_id  = c.course_id
        AND  e4.status      = 'completed'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM   ENROLLMENTS e5
      WHERE  e5.student_id  = s.student_id
        AND  e5.offering_id = co.offering_id
        AND  e5.status      = 'active'
  );
GO

-- 5. Align active semester with offerings (registration requires both)
IF NOT EXISTS (
    SELECT 1
    FROM   COURSE_OFFERINGS co
    JOIN   SEMESTERS sem ON sem.semester_id = co.semester_id
    WHERE  sem.is_active = 1
)
BEGIN
    DECLARE @target_semester INT;

    SELECT TOP 1 @target_semester = semester_id
    FROM   COURSE_OFFERINGS
    GROUP  BY semester_id
    ORDER  BY semester_id DESC;

    UPDATE SEMESTERS SET is_active = 0, enrollment_open = 0;
    UPDATE SEMESTERS
    SET    is_active = 1, enrollment_open = 1
    WHERE  semester_id = @target_semester;
END
GO

PRINT 'Migration complete: recommended_semester column + views updated.';
GO
