-- ================================================================
--  UMS_MERGED.sql
--  University Management System — SQL Server
--  ONE FILE: Schema + Views + Stored Procedures + Dummy Data
--  Run entirely in SSMS with F5 (Execute All)
--  CS3009 Software Engineering
--
--  Merged from UMS_DB.sql + UMS_Complete.sql
--  All tables, views, SPs, and data from both files are included.
-- ================================================================

-- ────────────────────────────────────────────────────────────────
-- STEP 0: DATABASE SETUP
-- ────────────────────────────────────────────────────────────────
USE master;
GO

ALTER DATABASE UMS_DB
SET SINGLE_USER
WITH ROLLBACK IMMEDIATE;
GO

DROP DATABASE UMS_DB;
GO

IF EXISTS (SELECT name FROM sys.databases WHERE name = N'UMS_DB')
    DROP DATABASE UMS_DB;
GO

CREATE DATABASE UMS_DB;
PRINT 'Created Successfully'
GO

USE UMS_DB;
GO

-- ================================================================
-- PART 1: SCHEMA
-- ================================================================

-- ────────────────────────────────────────────────────────────────
-- MODULE 1: DEPARTMENT HIERARCHY
-- ────────────────────────────────────────────────────────────────
CREATE TABLE MAJOR_DEPARTMENTS (
    major_dept_id   INT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(100)  NOT NULL,
    code            NVARCHAR(10)   NOT NULL UNIQUE,
    description     NVARCHAR(255),
    created_at      DATETIME       DEFAULT GETDATE()
);
GO

CREATE TABLE SUB_DEPARTMENTS (
    sub_dept_id     INT IDENTITY(1,1) PRIMARY KEY,
    major_dept_id   INT            NOT NULL,
    name            NVARCHAR(100)  NOT NULL,
    code            NVARCHAR(20)   NOT NULL UNIQUE,
    description     NVARCHAR(255),
    created_at      DATETIME       DEFAULT GETDATE(),
    CONSTRAINT FK_SubDept_MajorDept
        FOREIGN KEY (major_dept_id) REFERENCES MAJOR_DEPARTMENTS(major_dept_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 2: USERS & ROLE PROFILES
-- ────────────────────────────────────────────────────────────────
CREATE TABLE USERS (
    user_id         INT IDENTITY(1,1) PRIMARY KEY,
    username        NVARCHAR(50)   NOT NULL UNIQUE,
    password_hash   NVARCHAR(255)  NOT NULL,
    role            NVARCHAR(20)   NOT NULL
                        CHECK (role IN ('student','teacher','admin')),
    email           NVARCHAR(100)  NOT NULL UNIQUE,
    is_active       BIT            DEFAULT 1,
    created_at      DATETIME       DEFAULT GETDATE()
);
GO

CREATE TABLE STUDENTS (
    student_id       INT IDENTITY(1,1) PRIMARY KEY,
    user_id          INT            NOT NULL UNIQUE,
    sub_dept_id      INT            NOT NULL,
    full_name        NVARCHAR(100)  NOT NULL,
    roll_number      NVARCHAR(20)   NOT NULL UNIQUE,
    batch_year       SMALLINT       NOT NULL,
    current_semester TINYINT        DEFAULT 1,
    CONSTRAINT FK_Student_User
        FOREIGN KEY (user_id)     REFERENCES USERS(user_id),
    CONSTRAINT FK_Student_SubDept
        FOREIGN KEY (sub_dept_id) REFERENCES SUB_DEPARTMENTS(sub_dept_id)
);
GO

CREATE TABLE TEACHERS (
    teacher_id      INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT            NOT NULL UNIQUE,
    sub_dept_id     INT            NOT NULL,
    full_name       NVARCHAR(100)  NOT NULL,
    employee_code   NVARCHAR(20)   NOT NULL UNIQUE,
    designation     NVARCHAR(60),
    CONSTRAINT FK_Teacher_User
        FOREIGN KEY (user_id)     REFERENCES USERS(user_id),
    CONSTRAINT FK_Teacher_SubDept
        FOREIGN KEY (sub_dept_id) REFERENCES SUB_DEPARTMENTS(sub_dept_id)
);
GO

CREATE TABLE ADMINS (
    admin_id        INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT            NOT NULL UNIQUE,
    major_dept_id   INT,
    full_name       NVARCHAR(100)  NOT NULL,
    CONSTRAINT FK_Admin_User
        FOREIGN KEY (user_id)         REFERENCES USERS(user_id),
    CONSTRAINT FK_Admin_MajorDept
        FOREIGN KEY (major_dept_id)   REFERENCES MAJOR_DEPARTMENTS(major_dept_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 3: COURSE CATEGORIES (from UMS_DB)
-- ────────────────────────────────────────────────────────────────
CREATE TABLE COURSE_CATEGORIES (
    category_id   INT IDENTITY(1,1) PRIMARY KEY,
    name          NVARCHAR(50)  NOT NULL UNIQUE,  -- "Core", "Elective", "Lab", "Final Year Project"
    description   NVARCHAR(255),
    is_active     BIT DEFAULT 1
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 4: COURSES & VISIBILITY
-- ────────────────────────────────────────────────────────────────
CREATE TABLE COURSES (
    course_id       INT IDENTITY(1,1) PRIMARY KEY,
    major_dept_id   INT            NOT NULL,
    category_id     INT            NOT NULL,   -- from UMS_DB
    course_code     NVARCHAR(15)   NOT NULL UNIQUE,
    course_name     NVARCHAR(150)  NOT NULL,
    credit_hours    TINYINT        NOT NULL DEFAULT 3,
    description     NVARCHAR(500),
    is_active       BIT            DEFAULT 1,
    CONSTRAINT FK_Course_MajorDept
        FOREIGN KEY (major_dept_id)
        REFERENCES MAJOR_DEPARTMENTS(major_dept_id),
    CONSTRAINT FK_Course_Category
        FOREIGN KEY (category_id)
        REFERENCES COURSE_CATEGORIES(category_id)
);
GO

-- Degree requirements per sub-dept per category (from UMS_DB)
CREATE TABLE DEGREE_REQUIREMENTS (
    requirement_id     INT IDENTITY(1,1) PRIMARY KEY,
    sub_dept_id        INT NOT NULL,
    category_id        INT NOT NULL,
    required_credits   INT NOT NULL,   -- minimum credit hours needed from this category
    min_courses        INT DEFAULT 1,  -- minimum number of distinct courses

    CONSTRAINT UQ_DegreeReq UNIQUE (sub_dept_id, category_id),
    CONSTRAINT FK_DegReq_SubDept
        FOREIGN KEY (sub_dept_id)  REFERENCES SUB_DEPARTMENTS(sub_dept_id),
    CONSTRAINT FK_DegReq_Category
        FOREIGN KEY (category_id)  REFERENCES COURSE_CATEGORIES(category_id)
);
GO

-- Controls which sub-departments can SEE a course (visibility)
CREATE TABLE COURSE_SUB_DEPT_ELIGIBILITY (
    eligibility_id  INT IDENTITY(1,1) PRIMARY KEY,
    course_id       INT NOT NULL,
    sub_dept_id     INT NOT NULL,
    CONSTRAINT UQ_Eligibility UNIQUE (course_id, sub_dept_id),
    CONSTRAINT FK_Elig_Course
        FOREIGN KEY (course_id)   REFERENCES COURSES(course_id),
    CONSTRAINT FK_Elig_SubDept
        FOREIGN KEY (sub_dept_id) REFERENCES SUB_DEPARTMENTS(sub_dept_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 5: SEMESTERS & OFFERINGS
-- ────────────────────────────────────────────────────────────────
CREATE TABLE SEMESTERS (
    semester_id     INT IDENTITY(1,1) PRIMARY KEY,
    semester_name   NVARCHAR(50)   NOT NULL,
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL,
    is_active       BIT            DEFAULT 0,
    enrollment_open BIT            DEFAULT 0   -- from UMS_DB
);
GO

-- A specific run of a course for one sub-dept in one semester
-- sub_dept_id here = who can REGISTER (registration guard)
CREATE TABLE COURSE_OFFERINGS (
    offering_id     INT IDENTITY(1,1) PRIMARY KEY,
    course_id       INT            NOT NULL,
    sub_dept_id     INT            NOT NULL,
    semester_id     INT            NOT NULL,
    teacher_id      INT            NOT NULL,
    section         NVARCHAR(5)    NOT NULL,
    room_number     NVARCHAR(20),
    schedule_days   NVARCHAR(30),
    class_time      TIME,
    max_capacity    SMALLINT       DEFAULT 40,
    CONSTRAINT UQ_Offering UNIQUE (course_id, sub_dept_id, semester_id, section),
    CONSTRAINT FK_Offering_Course
        FOREIGN KEY (course_id)   REFERENCES COURSES(course_id),
    CONSTRAINT FK_Offering_SubDept
        FOREIGN KEY (sub_dept_id) REFERENCES SUB_DEPARTMENTS(sub_dept_id),
    CONSTRAINT FK_Offering_Semester
        FOREIGN KEY (semester_id) REFERENCES SEMESTERS(semester_id),
    CONSTRAINT FK_Offering_Teacher
        FOREIGN KEY (teacher_id)  REFERENCES TEACHERS(teacher_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 6: ENROLLMENTS, ATTENDANCE & MARKS
-- ────────────────────────────────────────────────────────────────
CREATE TABLE ENROLLMENTS (
    enrollment_id   INT IDENTITY(1,1) PRIMARY KEY,
    student_id      INT            NOT NULL,
    offering_id     INT            NOT NULL,
    enrollment_date DATE           DEFAULT CAST(GETDATE() AS DATE),
    status          NVARCHAR(20)   DEFAULT 'active'
                        CHECK (status IN ('active','withdrawn','completed')),
    CONSTRAINT UQ_Enrollment UNIQUE (student_id, offering_id),
    CONSTRAINT FK_Enroll_Student
        FOREIGN KEY (student_id)  REFERENCES STUDENTS(student_id),
    CONSTRAINT FK_Enroll_Offering
        FOREIGN KEY (offering_id) REFERENCES COURSE_OFFERINGS(offering_id)
);
GO

CREATE TABLE ATTENDANCE (
    attendance_id   INT IDENTITY(1,1) PRIMARY KEY,
    enrollment_id   INT            NOT NULL,
    class_date      DATE           NOT NULL,
    status          NVARCHAR(10)   NOT NULL
                        CHECK (status IN ('present','absent','late')),
    remarks         NVARCHAR(200),
    CONSTRAINT UQ_Attendance UNIQUE (enrollment_id, class_date),
    CONSTRAINT FK_Attend_Enrollment
        FOREIGN KEY (enrollment_id) REFERENCES ENROLLMENTS(enrollment_id)
);
GO

CREATE TABLE MARKS (
    mark_id         INT IDENTITY(1,1) PRIMARY KEY,
    enrollment_id   INT            NOT NULL,
    assessment_type NVARCHAR(30)   NOT NULL
                        CHECK (assessment_type IN
                        ('quiz','assignment','mid','final','lab','project')),
    marks_obtained  DECIMAL(5,2)   NOT NULL,
    total_marks     DECIMAL(5,2)   NOT NULL,
    remarks         NVARCHAR(200),
    CONSTRAINT FK_Marks_Enrollment
        FOREIGN KEY (enrollment_id) REFERENCES ENROLLMENTS(enrollment_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 7: FEES & CHALLANS
-- ────────────────────────────────────────────────────────────────
CREATE TABLE FEE_RECORDS (
    fee_id          INT IDENTITY(1,1) PRIMARY KEY,
    student_id      INT            NOT NULL,
    semester_id     INT            NOT NULL,
    total_amount    DECIMAL(10,2)  NOT NULL,
    paid_amount     DECIMAL(10,2)  DEFAULT 0,
    due_date        DATE           NOT NULL,
    status          NVARCHAR(20)   DEFAULT 'unpaid'
                        CHECK (status IN ('unpaid','partial','paid')),
    CONSTRAINT UQ_Fee UNIQUE (student_id, semester_id),
    CONSTRAINT FK_Fee_Student
        FOREIGN KEY (student_id)  REFERENCES STUDENTS(student_id),
    CONSTRAINT FK_Fee_Semester
        FOREIGN KEY (semester_id) REFERENCES SEMESTERS(semester_id)
);
GO

CREATE TABLE FEE_CHALLANS (
    challan_id      INT IDENTITY(1,1) PRIMARY KEY,
    fee_id          INT            NOT NULL,
    challan_number  NVARCHAR(30)   NOT NULL UNIQUE,
    generated_date  DATE           DEFAULT CAST(GETDATE() AS DATE),
    expiry_date     DATE           NOT NULL,
    is_paid         BIT            DEFAULT 0,
    CONSTRAINT FK_Challan_Fee
        FOREIGN KEY (fee_id) REFERENCES FEE_RECORDS(fee_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 8: COMMUNICATION & MATERIALS
-- ────────────────────────────────────────────────────────────────
CREATE TABLE ANNOUNCEMENTS (
    announcement_id INT IDENTITY(1,1) PRIMARY KEY,
    posted_by       INT            NOT NULL,
    major_dept_id   INT,           -- NULL = university-wide
    offering_id     INT,           -- NULL = not course-specific
    title           NVARCHAR(200)  NOT NULL,
    content         NVARCHAR(MAX)  NOT NULL,
    target_role     NVARCHAR(20)   DEFAULT 'all'
                        CHECK (target_role IN ('all','student','teacher')),
    posted_at       DATETIME       DEFAULT GETDATE(),
    is_active       BIT            DEFAULT 1,
    CONSTRAINT FK_Ann_PostedBy
        FOREIGN KEY (posted_by)     REFERENCES USERS(user_id),
    CONSTRAINT FK_Ann_MajorDept
        FOREIGN KEY (major_dept_id) REFERENCES MAJOR_DEPARTMENTS(major_dept_id),
    CONSTRAINT FK_Ann_Offering
        FOREIGN KEY (offering_id)   REFERENCES COURSE_OFFERINGS(offering_id)
);
GO

CREATE TABLE COURSE_MATERIALS (
    material_id     INT IDENTITY(1,1) PRIMARY KEY,
    offering_id     INT            NOT NULL,
    uploaded_by     INT            NOT NULL,
    title           NVARCHAR(200)  NOT NULL,
    file_path       NVARCHAR(500)  NOT NULL,
    file_type       NVARCHAR(20),
    uploaded_at     DATETIME       DEFAULT GETDATE(),
    CONSTRAINT FK_Mat_Offering
        FOREIGN KEY (offering_id)  REFERENCES COURSE_OFFERINGS(offering_id),
    CONSTRAINT FK_Mat_UploadedBy
        FOREIGN KEY (uploaded_by)  REFERENCES USERS(user_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 9: REQUESTS, FEEDBACK & TIMETABLES
-- ────────────────────────────────────────────────────────────────
CREATE TABLE ACADEMIC_REQUESTS (
    request_id      INT IDENTITY(1,1) PRIMARY KEY,
    student_id      INT            NOT NULL,
    offering_id     INT            NOT NULL,
    request_type    NVARCHAR(30)   NOT NULL
                        CHECK (request_type IN
                        ('retake','grade_change','withdrawal','other')),
    description     NVARCHAR(500),
    status          NVARCHAR(20)   DEFAULT 'pending'
                        CHECK (status IN ('pending','approved','rejected')),
    submitted_at    DATETIME       DEFAULT GETDATE(),
    reviewed_by     INT,
    review_remarks  NVARCHAR(300),
    CONSTRAINT FK_Req_Student
        FOREIGN KEY (student_id)  REFERENCES STUDENTS(student_id),
    CONSTRAINT FK_Req_Offering
        FOREIGN KEY (offering_id) REFERENCES COURSE_OFFERINGS(offering_id),
    CONSTRAINT FK_Req_ReviewedBy
        FOREIGN KEY (reviewed_by) REFERENCES USERS(user_id)
);
GO

CREATE TABLE FEEDBACK (
    feedback_id     INT IDENTITY(1,1) PRIMARY KEY,
    student_id      INT            NOT NULL,
    offering_id     INT            NOT NULL,
    rating          TINYINT        NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments        NVARCHAR(500),
    submitted_at    DATETIME       DEFAULT GETDATE(),
    is_anonymous    BIT            DEFAULT 1,
    CONSTRAINT UQ_Feedback UNIQUE (student_id, offering_id),
    CONSTRAINT FK_FB_Student
        FOREIGN KEY (student_id)  REFERENCES STUDENTS(student_id),
    CONSTRAINT FK_FB_Offering
        FOREIGN KEY (offering_id) REFERENCES COURSE_OFFERINGS(offering_id)
);
GO

-- US-3.7a: TIMETABLES stores weekly recurring slots per course offering.
-- One row = one day/time slot (a Mon+Wed course = 2 rows).
-- Room-conflict detection is advisory only; enforced by SP_AddTimetableSlot.
CREATE TABLE TIMETABLES (
    timetable_id    INT IDENTITY(1,1) PRIMARY KEY,
    offering_id     INT            NOT NULL,
    day_of_week     NVARCHAR(10)   NOT NULL
                        CHECK (day_of_week IN
                        ('Monday','Tuesday','Wednesday','Thursday','Friday')),
    start_time      TIME           NOT NULL,
    end_time        TIME           NOT NULL,
    room_number     NVARCHAR(20),
    -- Prevent exact duplicate slot for the same offering on same day/start
    CONSTRAINT UQ_TimetableSlot   UNIQUE (offering_id, day_of_week, start_time),
    -- End must be after start
    CONSTRAINT CHK_TT_Times       CHECK  (end_time > start_time),
    CONSTRAINT FK_TT_Offering
        FOREIGN KEY (offering_id) REFERENCES COURSE_OFFERINGS(offering_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 10: COURSE PREREQUISITES (from UMS_DB)
-- ────────────────────────────────────────────────────────────────
CREATE TABLE COURSE_PREREQUISITES (
    prereq_id        INT IDENTITY(1,1) PRIMARY KEY,
    course_id        INT NOT NULL,   -- the course that HAS a prerequisite
    prereq_course_id INT NOT NULL,   -- the course that MUST be cleared first
    min_grade_pct    DECIMAL(5,2)    DEFAULT 50.00,  -- minimum % to consider "cleared"
    is_mandatory     BIT             DEFAULT 1,       -- 1=hard block, 0=advisory warning only

    CONSTRAINT UQ_Prereq UNIQUE (course_id, prereq_course_id),
    CONSTRAINT FK_Prereq_Course
        FOREIGN KEY (course_id)        REFERENCES COURSES(course_id),
    CONSTRAINT FK_Prereq_PrereqCourse
        FOREIGN KEY (prereq_course_id) REFERENCES COURSES(course_id),
    CONSTRAINT CHK_NoSelfPrereq
        CHECK (course_id <> prereq_course_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 11: CREDIT POLICIES (from UMS_DB)
-- ────────────────────────────────────────────────────────────────
CREATE TABLE CREDIT_POLICIES (
    policy_id        INT IDENTITY(1,1) PRIMARY KEY,
    sub_dept_id      INT            NOT NULL,
    min_credits      TINYINT        NOT NULL DEFAULT 12,  -- minimum load
    max_credits      TINYINT        NOT NULL DEFAULT 21,  -- maximum load
    overload_credits TINYINT        NOT NULL DEFAULT 24,  -- allowed with dean approval
    effective_from   DATE           NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    is_active        BIT            DEFAULT 1,

    CONSTRAINT UQ_CreditPolicy UNIQUE (sub_dept_id, effective_from),
    CONSTRAINT FK_CreditPolicy_SubDept
        FOREIGN KEY (sub_dept_id) REFERENCES SUB_DEPARTMENTS(sub_dept_id),
    CONSTRAINT CHK_CreditOrder
        CHECK (min_credits <= max_credits AND max_credits <= overload_credits)
);
GO

-- ────────────────────────────────────────────────────────────────
-- MODULE 12: WITHDRAWAL REQUESTS (from UMS_Complete, US-3.5a)
-- Stores student-initiated course withdrawal requests.
-- Linked to an enrollment; admin reviews and decides.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE WITHDRAWAL_REQUESTS (
    request_id      INT IDENTITY(1,1) PRIMARY KEY,
    enrollment_id   INT            NOT NULL,
    student_id      INT            NOT NULL,   -- denormalised for fast admin queries
    reason          NVARCHAR(500)  NOT NULL,
    status          NVARCHAR(20)   NOT NULL    DEFAULT 'pending'
                        CHECK (status IN ('pending','approved','rejected')),
    admin_comment   NVARCHAR(300),
    requested_at    DATETIME       NOT NULL    DEFAULT GETDATE(),
    reviewed_by     INT,                       -- USERS.user_id of the admin
    reviewed_at     DATETIME,
    CONSTRAINT UQ_WithdrawalRequest UNIQUE (enrollment_id),   -- one live request per enrollment
    CONSTRAINT FK_WR_Enrollment
        FOREIGN KEY (enrollment_id) REFERENCES ENROLLMENTS(enrollment_id),
    CONSTRAINT FK_WR_Student
        FOREIGN KEY (student_id)    REFERENCES STUDENTS(student_id),
    CONSTRAINT FK_WR_ReviewedBy
        FOREIGN KEY (reviewed_by)   REFERENCES USERS(user_id)
);
GO

-- ────────────────────────────────────────────────────────────────
-- INDEXES
-- ────────────────────────────────────────────────────────────────
CREATE INDEX IX_Student_SubDept    ON STUDENTS(sub_dept_id);
CREATE INDEX IX_Teacher_SubDept    ON TEACHERS(sub_dept_id);
CREATE INDEX IX_Course_MajorDept   ON COURSES(major_dept_id);
CREATE INDEX IX_Offering_SubDept   ON COURSE_OFFERINGS(sub_dept_id);
CREATE INDEX IX_Offering_Semester  ON COURSE_OFFERINGS(semester_id);
CREATE INDEX IX_Enroll_Student     ON ENROLLMENTS(student_id);
CREATE INDEX IX_Enroll_Offering    ON ENROLLMENTS(offering_id);
CREATE INDEX IX_Attend_Enroll      ON ATTENDANCE(enrollment_id);
CREATE INDEX IX_Marks_Enroll       ON MARKS(enrollment_id);
CREATE INDEX IX_FeeRec_Student     ON FEE_RECORDS(student_id);
CREATE INDEX IX_Elig_Course        ON COURSE_SUB_DEPT_ELIGIBILITY(course_id);
CREATE INDEX IX_Elig_SubDept       ON COURSE_SUB_DEPT_ELIGIBILITY(sub_dept_id);
-- From UMS_Complete
CREATE INDEX IX_WR_Student         ON WITHDRAWAL_REQUESTS(student_id);
CREATE INDEX IX_WR_Status          ON WITHDRAWAL_REQUESTS(status);
CREATE INDEX IX_TT_Offering        ON TIMETABLES(offering_id);
CREATE INDEX IX_TT_Room_Day        ON TIMETABLES(room_number, day_of_week);
GO

-- ================================================================
-- PART 2: VIEWS
-- ================================================================

-- V1: All courses a student can browse (same faculty + eligibility)
CREATE VIEW VW_STUDENT_VISIBLE_COURSES AS
SELECT
    s.student_id,
    s.roll_number,
    c.course_id,
    c.course_code,
    c.course_name,
    c.credit_hours,
    md.name  AS major_dept,
    sd.name  AS eligible_sub_dept
FROM STUDENTS s
JOIN SUB_DEPARTMENTS           sd  ON s.sub_dept_id    = sd.sub_dept_id
JOIN MAJOR_DEPARTMENTS         md  ON sd.major_dept_id = md.major_dept_id
JOIN COURSE_SUB_DEPT_ELIGIBILITY e ON e.sub_dept_id    = s.sub_dept_id
JOIN COURSES                   c   ON c.course_id      = e.course_id
                                  AND c.major_dept_id   = md.major_dept_id
WHERE c.is_active = 1;
GO

-- V2: Active offerings a student can register into
CREATE VIEW VW_STUDENT_REGISTRABLE_OFFERINGS AS
SELECT
    s.student_id,
    s.roll_number,
    co.offering_id,
    c.course_code,
    c.course_name,
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
WHERE sem.is_active = 1;
GO

-- V3: Student marks/percentage summary per course
CREATE VIEW VW_STUDENT_GPA AS
SELECT
    s.student_id,
    s.roll_number,
    s.full_name,
    co.semester_id,
    c.course_name,
    SUM(m.marks_obtained) AS total_obtained,
    SUM(m.total_marks)    AS total_possible,
    CAST(
        100.0 * SUM(m.marks_obtained) / NULLIF(SUM(m.total_marks),0)
    AS DECIMAL(5,2))      AS percentage
FROM STUDENTS       s
JOIN ENROLLMENTS    e   ON e.student_id    = s.student_id
JOIN COURSE_OFFERINGS co ON co.offering_id = e.offering_id
JOIN COURSES        c   ON c.course_id     = co.course_id
JOIN MARKS          m   ON m.enrollment_id = e.enrollment_id
GROUP BY s.student_id, s.roll_number, s.full_name,
         co.semester_id, c.course_name;
GO

-- V4: Student credit load per semester (from UMS_DB)
CREATE VIEW VW_STUDENT_CREDIT_LOAD AS
SELECT
    e.student_id,
    co.semester_id,
    COUNT(DISTINCT e.enrollment_id)    AS enrolled_courses,
    SUM(c.credit_hours)                AS total_credits,
    s.sub_dept_id
FROM ENROLLMENTS        e
JOIN COURSE_OFFERINGS   co ON co.offering_id = e.offering_id
JOIN COURSES             c ON c.course_id    = co.course_id
JOIN STUDENTS            s ON s.student_id   = e.student_id
WHERE e.status = 'active'
GROUP BY e.student_id, co.semester_id, s.sub_dept_id;
GO

CREATE VIEW VW_REPORT_ENROLLMENT AS
SELECT 
    s.semester_id,
    s.semester_name,
    d.name AS dept_name,
    c.course_code,
    c.course_name,
    sec.section,
    t.full_name AS teacher_name,

    COUNT(e.student_id) AS total_enrolled,

    SUM(CASE WHEN e.status = 'Active' THEN 1 ELSE 0 END)     AS active_count,
    SUM(CASE WHEN e.status = 'Withdrawn' THEN 1 ELSE 0 END)  AS withdrawn_count,
    SUM(CASE WHEN e.status = 'Completed' THEN 1 ELSE 0 END)  AS completed_count

FROM ENROLLMENTS e
JOIN COURSE_OFFERINGS sec ON sec.offering_id = e.offering_id
JOIN COURSES c            ON c.course_id = sec.course_id
JOIN SEMESTERS s          ON s.semester_id = sec.semester_id
JOIN TEACHERS t           ON t.teacher_id = sec.teacher_id
JOIN SUB_DEPARTMENTS sd   ON sd.sub_dept_id = sec.sub_dept_id
JOIN MAJOR_DEPARTMENTS d  ON d.major_dept_id = sd.major_dept_id

GROUP BY 
    s.semester_id,
    s.semester_name,
    d.name,
    c.course_code,
    c.course_name,
    sec.section,
    t.full_name;
GO

CREATE VIEW VW_REPORT_ACADEMIC_PERFORMANCE AS
SELECT 
    sem.semester_name,
    md.name AS dept_name,
    c.course_code,
    c.course_name,
    co.section,
    COUNT(DISTINCT e.student_id) AS student_count,
    AVG(CAST(100.0 * m.marks_obtained / NULLIF(m.total_marks, 0) AS DECIMAL(5,2))) AS avg_percentage,
    MIN(CAST(100.0 * m.marks_obtained / NULLIF(m.total_marks, 0) AS DECIMAL(5,2))) AS min_percentage,
    MAX(CAST(100.0 * m.marks_obtained / NULLIF(m.total_marks, 0) AS DECIMAL(5,2))) AS max_percentage,
    sem.semester_id
FROM ENROLLMENTS e
JOIN COURSE_OFFERINGS co ON e.offering_id = co.offering_id
JOIN COURSES c ON co.course_id = c.course_id
JOIN SEMESTERS sem ON co.semester_id = sem.semester_id
JOIN MAJOR_DEPARTMENTS md ON c.major_dept_id = md.major_dept_id
JOIN MARKS m ON e.enrollment_id = m.enrollment_id
GROUP BY sem.semester_name, md.name, c.course_code, c.course_name,
         co.section, sem.semester_id;
GO

CREATE VIEW VW_REPORT_ATTENDANCE AS
SELECT 
    sem.semester_name,
    md.name AS dept_name,
    c.course_code,
    c.course_name,
    co.section,
    t.full_name AS teacher_name,
    COUNT(DISTINCT a.class_date) AS total_sessions,
    COUNT(DISTINCT e.student_id) AS student_count,
    CAST(100.0 * SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) AS avg_attendance_pct,
    (SELECT COUNT(DISTINCT sub.student_id) 
     FROM (
        SELECT e2.student_id,
               CAST(100.0 * SUM(CASE WHEN a2.status = 'present' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) AS att_pct
        FROM ATTENDANCE a2
        JOIN ENROLLMENTS e2 ON a2.enrollment_id = e2.enrollment_id
        WHERE e2.offering_id = co.offering_id
        GROUP BY e2.student_id
     ) sub
     WHERE sub.att_pct < 75) AS students_below_75,
    sem.semester_id
FROM ATTENDANCE a
JOIN ENROLLMENTS e ON a.enrollment_id = e.enrollment_id
JOIN COURSE_OFFERINGS co ON e.offering_id = co.offering_id
JOIN COURSES c ON co.course_id = c.course_id
JOIN SEMESTERS sem ON co.semester_id = sem.semester_id
JOIN MAJOR_DEPARTMENTS md ON c.major_dept_id = md.major_dept_id
JOIN TEACHERS t ON co.teacher_id = t.teacher_id
GROUP BY sem.semester_name, md.name, c.course_code, c.course_name,
         co.section, t.full_name, sem.semester_id, co.offering_id;
GO

-- ================================================================
-- PART 3: STORED PROCEDURES
-- ================================================================

-- SP1: Register a student
--      Validates eligibility + prerequisites + capacity + duplicates + credit limit
CREATE OR ALTER PROCEDURE SP_RegisterStudent
    @student_id  INT,
    @offering_id INT
AS
BEGIN
    SET NOCOUNT ON;

    -- ────────────────────────────────────────────────
    -- 1. ELIGIBILITY CHECK
    -- ────────────────────────────────────────────────
    IF NOT EXISTS (
        SELECT 1
        FROM COURSE_OFFERINGS co
        JOIN COURSE_SUB_DEPT_ELIGIBILITY e
            ON e.course_id = co.course_id
        JOIN STUDENTS s
            ON s.student_id  = @student_id
           AND s.sub_dept_id = co.sub_dept_id
           AND s.sub_dept_id = e.sub_dept_id
        WHERE co.offering_id = @offering_id
    )
    BEGIN
        RAISERROR('Student not eligible for this offering.', 16, 1);
        RETURN;
    END

    -- ────────────────────────────────────────────────
    -- 2. GET COURSE ID
    -- ────────────────────────────────────────────────
    DECLARE @course_id INT;

    SELECT @course_id = course_id
    FROM COURSE_OFFERINGS
    WHERE offering_id = @offering_id;

    -- ────────────────────────────────────────────────
    -- 3. PREREQUISITE CHECK
    -- ────────────────────────────────────────────────
    IF EXISTS (
        SELECT 1
        FROM COURSE_PREREQUISITES cp
        WHERE cp.course_id = @course_id
          AND cp.is_mandatory = 1
          AND NOT EXISTS (
              SELECT 1
              FROM ENROLLMENTS e
              JOIN COURSE_OFFERINGS co2
                    ON co2.offering_id = e.offering_id
              JOIN (
                  SELECT m.enrollment_id,
                         CAST(100.0 * SUM(m.marks_obtained)
                              / NULLIF(SUM(m.total_marks), 0)
                         AS DECIMAL(5,2)) AS pct
                  FROM MARKS m
                  GROUP BY m.enrollment_id
              ) grades
                    ON grades.enrollment_id = e.enrollment_id
              WHERE e.student_id  = @student_id
                AND co2.course_id = cp.prereq_course_id
                AND e.status      = 'completed'
                AND grades.pct   >= cp.min_grade_pct
          )
    )
    BEGIN
        RAISERROR('Prerequisite course not completed with required grade.', 16, 1);
        RETURN;
    END

    -- ────────────────────────────────────────────────
    -- 4. CAPACITY CHECK
    -- ────────────────────────────────────────────────
    DECLARE @enrolled INT, @cap INT;

    SELECT
        @enrolled = COUNT(*),
        @cap      = MAX(co.max_capacity)
    FROM ENROLLMENTS e
    JOIN COURSE_OFFERINGS co
        ON co.offering_id = @offering_id
    WHERE e.offering_id = @offering_id
      AND e.status = 'active';

    IF @enrolled >= @cap
    BEGIN
        RAISERROR('Course offering is at full capacity.', 16, 1);
        RETURN;
    END

    -- ────────────────────────────────────────────────
    -- 5. DUPLICATE ENROLLMENT CHECK
    -- ────────────────────────────────────────────────
    IF EXISTS (
        SELECT 1
        FROM ENROLLMENTS
        WHERE student_id = @student_id
          AND offering_id = @offering_id
    )
    BEGIN
        RAISERROR('Student already enrolled in this offering.', 16, 1);
        RETURN;
    END

    -- ────────────────────────────────────────────────
    -- 6. CREDIT LIMIT CHECK
    -- ────────────────────────────────────────────────
    DECLARE @new_course_credits TINYINT;
    DECLARE @current_credits    TINYINT;
    DECLARE @max_credits        TINYINT;
    DECLARE @overload_credits   TINYINT;
    DECLARE @semester_id        INT;
    DECLARE @sub_dept_id        INT;

    -- Get course credits + semester
    SELECT
        @semester_id        = co.semester_id,
        @new_course_credits = c.credit_hours
    FROM COURSE_OFFERINGS co
    JOIN COURSES c
        ON c.course_id = co.course_id
    WHERE co.offering_id = @offering_id;

    -- Get student sub dept
    SELECT @sub_dept_id = sub_dept_id
    FROM STUDENTS
    WHERE student_id = @student_id;

    -- Get credit policy
    SELECT
        @max_credits      = max_credits,
        @overload_credits = overload_credits
    FROM CREDIT_POLICIES
    WHERE sub_dept_id = @sub_dept_id
      AND is_active = 1;

    -- Current enrolled credits
    SELECT @current_credits = ISNULL(SUM(c2.credit_hours), 0)
    FROM ENROLLMENTS e2
    JOIN COURSE_OFFERINGS co2
        ON co2.offering_id = e2.offering_id
    JOIN COURSES c2
        ON c2.course_id = co2.course_id
    WHERE e2.student_id   = @student_id
      AND co2.semester_id = @semester_id
      AND e2.status       = 'active';

    -- Hard limit: block if exceeds overload credits
    IF (@current_credits + @new_course_credits) > @overload_credits
    BEGIN
        RAISERROR(
            'Credit limit exceeded. Max allowed: %d, Current: %d, Course: %d',
            16, 1,
            @overload_credits,
            @current_credits,
            @new_course_credits
        );
        RETURN;
    END

    -- Soft warning: still enrolls but warns
    IF (@current_credits + @new_course_credits) > @max_credits
    BEGIN
        PRINT 'WARNING: Exceeding standard credit limit (overload). Dean approval may be required.';
    END

    -- ────────────────────────────────────────────────
    -- 7. INSERT ENROLLMENT
    -- ────────────────────────────────────────────────
    INSERT INTO ENROLLMENTS (student_id, offering_id, status)
    VALUES (@student_id, @offering_id, 'active');

    PRINT 'Enrollment successful.';
END
GO

-- SP2: Bulk mark attendance for a class session (JSON input)
CREATE PROCEDURE SP_MarkAttendance
    @offering_id INT,
    @class_date  DATE,
    @status_list NVARCHAR(MAX)
AS BEGIN
    SET NOCOUNT ON;
    INSERT INTO ATTENDANCE (enrollment_id, class_date, status)
    SELECT enrollment_id, @class_date, status
    FROM OPENJSON(@status_list)
    WITH (enrollment_id INT, status NVARCHAR(10));
END
GO

-- SP3: Generate a fee challan for a student/semester
CREATE PROCEDURE SP_GenerateChallan
    @student_id  INT,
    @semester_id INT
AS BEGIN
    SET NOCOUNT ON;
    DECLARE @fee_id INT;

    SELECT @fee_id = fee_id
    FROM FEE_RECORDS
    WHERE student_id = @student_id AND semester_id = @semester_id;

    IF @fee_id IS NULL
    BEGIN
        RAISERROR('No fee record found for this student/semester.', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1 FROM FEE_CHALLANS
        WHERE fee_id = @fee_id AND is_paid = 0
    )
    BEGIN
        RAISERROR('An unpaid challan already exists.', 16, 1);
        RETURN;
    END

    DECLARE @challan_no NVARCHAR(30) =
        N'CH-' + CAST(@student_id  AS NVARCHAR) +
        N'-'   + CAST(@semester_id AS NVARCHAR) +
        N'-'   + FORMAT(GETDATE(),'yyyyMMddHHmm');

    INSERT INTO FEE_CHALLANS (fee_id, challan_number, expiry_date)
    VALUES (@fee_id, @challan_no, DATEADD(DAY, 15, GETDATE()));

    SELECT challan_number, expiry_date
    FROM FEE_CHALLANS
    WHERE fee_id = @fee_id
    ORDER BY generated_date DESC;
END
GO

-- SP4: Generate a full academic transcript for a student (from UMS_Complete)
--      Returns two result sets:
--        RS1 – per-course rows (one per enrolled course with a final grade)
--        RS2 – per-semester GPA summary + overall CGPA
--
--  Grade-point mapping (percentage-based, standard 4.0 scale):
--      >= 90  → A+  4.00
--      >= 85  → A   4.00
--      >= 80  → A-  3.70
--      >= 75  → B+  3.30
--      >= 70  → B   3.00
--      >= 65  → B-  2.70
--      >= 60  → C+  2.30
--      >= 55  → C   2.00
--      >= 50  → C-  1.70
--      >= 45  → D   1.00
--      <  45  → F   0.00
CREATE PROCEDURE SP_GetTranscript
    @student_id INT
AS
BEGIN
    SET NOCOUNT ON;

    -- 0. Validate student exists
    IF NOT EXISTS (SELECT 1 FROM STUDENTS WHERE student_id = @student_id)
    BEGIN
        RAISERROR('Student not found.', 16, 1);
        RETURN;
    END

    -- 1. Build per-course transcript rows
    ;WITH CourseMarks AS (
        SELECT
            e.enrollment_id,
            e.student_id,
            co.semester_id,
            sem.semester_name,
            sem.start_date                          AS sem_start,
            c.course_code,
            c.course_name,
            c.credit_hours,
            co.section,
            t.full_name                             AS teacher_name,
            SUM(m.marks_obtained)                   AS marks_obtained,
            SUM(m.total_marks)                      AS marks_total,
            CAST(
                100.0 * SUM(m.marks_obtained)
                / NULLIF(SUM(m.total_marks), 0)
            AS DECIMAL(5,2))                        AS percentage
        FROM ENROLLMENTS        e
        JOIN COURSE_OFFERINGS   co  ON co.offering_id  = e.offering_id
        JOIN COURSES            c   ON c.course_id     = co.course_id
        JOIN SEMESTERS          sem ON sem.semester_id = co.semester_id
        JOIN TEACHERS           t   ON t.teacher_id    = co.teacher_id
        JOIN MARKS              m   ON m.enrollment_id = e.enrollment_id
        WHERE e.student_id = @student_id
          AND e.status IN ('active', 'completed')
        GROUP BY
            e.enrollment_id, e.student_id,
            co.semester_id, sem.semester_name, sem.start_date,
            c.course_code, c.course_name, c.credit_hours,
            co.section, t.full_name
    ),
    GradedCourses AS (
        SELECT
            enrollment_id, student_id, semester_id, semester_name, sem_start,
            course_code, course_name, credit_hours, section, teacher_name,
            marks_obtained, marks_total, percentage,
            CASE
                WHEN percentage >= 90 THEN 'A+'
                WHEN percentage >= 85 THEN 'A'
                WHEN percentage >= 80 THEN 'A-'
                WHEN percentage >= 75 THEN 'B+'
                WHEN percentage >= 70 THEN 'B'
                WHEN percentage >= 65 THEN 'B-'
                WHEN percentage >= 60 THEN 'C+'
                WHEN percentage >= 55 THEN 'C'
                WHEN percentage >= 50 THEN 'C-'
                WHEN percentage >= 45 THEN 'D'
                ELSE                       'F'
            END AS letter_grade,
            CAST(CASE
                WHEN percentage >= 90 THEN 4.00
                WHEN percentage >= 85 THEN 4.00
                WHEN percentage >= 80 THEN 3.70
                WHEN percentage >= 75 THEN 3.30
                WHEN percentage >= 70 THEN 3.00
                WHEN percentage >= 65 THEN 2.70
                WHEN percentage >= 60 THEN 2.30
                WHEN percentage >= 55 THEN 2.00
                WHEN percentage >= 50 THEN 1.70
                WHEN percentage >= 45 THEN 1.00
                ELSE                       0.00
            END AS DECIMAL(4,2)) AS grade_points
        FROM CourseMarks
    )
    -- RS1: Per-course rows
    SELECT
        semester_id, semester_name, sem_start,
        course_code, course_name, credit_hours, section, teacher_name,
        marks_obtained, marks_total, percentage, letter_grade, grade_points,
        CAST(grade_points * credit_hours AS DECIMAL(6,2)) AS quality_points
    FROM GradedCourses
    ORDER BY sem_start ASC, course_code ASC;

    -- RS2: Per-semester GPA + running CGPA
    ;WITH CourseMarks2 AS (
        SELECT
            co.semester_id,
            sem.semester_name,
            sem.start_date                          AS sem_start,
            c.credit_hours,
            CAST(
                100.0 * SUM(m.marks_obtained)
                / NULLIF(SUM(m.total_marks), 0)
            AS DECIMAL(5,2))                        AS percentage
        FROM ENROLLMENTS        e
        JOIN COURSE_OFFERINGS   co  ON co.offering_id  = e.offering_id
        JOIN COURSES            c   ON c.course_id     = co.course_id
        JOIN SEMESTERS          sem ON sem.semester_id = co.semester_id
        JOIN MARKS              m   ON m.enrollment_id = e.enrollment_id
        WHERE e.student_id = @student_id
          AND e.status IN ('active', 'completed')
        GROUP BY
            e.enrollment_id,
            co.semester_id, sem.semester_name, sem.start_date,
            c.credit_hours
    ),
    GradedCourses2 AS (
        SELECT
            semester_id, semester_name, sem_start, credit_hours,
            CAST(CASE
                WHEN percentage >= 90 THEN 4.00
                WHEN percentage >= 85 THEN 4.00
                WHEN percentage >= 80 THEN 3.70
                WHEN percentage >= 75 THEN 3.30
                WHEN percentage >= 70 THEN 3.00
                WHEN percentage >= 65 THEN 2.70
                WHEN percentage >= 60 THEN 2.30
                WHEN percentage >= 55 THEN 2.00
                WHEN percentage >= 50 THEN 1.70
                WHEN percentage >= 45 THEN 1.00
                ELSE                       0.00
            END AS DECIMAL(4,2)) AS grade_points
        FROM CourseMarks2
    ),
    SemesterGPA AS (
        SELECT
            semester_id, semester_name, sem_start,
            SUM(credit_hours)                        AS sem_credit_hours,
            SUM(grade_points * credit_hours)         AS sem_quality_points,
            CAST(
                SUM(grade_points * credit_hours)
                / NULLIF(SUM(credit_hours), 0)
            AS DECIMAL(4,2))                         AS semester_gpa,
            COUNT(*)                                 AS course_count
        FROM GradedCourses2
        GROUP BY semester_id, semester_name, sem_start
    )
    SELECT
        semester_id, semester_name, sem_start, course_count, sem_credit_hours,
        CAST(sem_quality_points AS DECIMAL(6,2))     AS sem_quality_points,
        semester_gpa,
        CAST(
            SUM(sem_quality_points) OVER (ORDER BY sem_start
                                          ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
            / NULLIF(
                SUM(sem_credit_hours) OVER (ORDER BY sem_start
                                            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
              , 0)
        AS DECIMAL(4,2))                             AS cgpa_so_far
    FROM SemesterGPA
    ORDER BY sem_start ASC;
END
GO

-- SP5: Student submits a course withdrawal request (US-3.5a, from UMS_Complete)
CREATE PROCEDURE SP_RequestWithdrawal
    @enrollment_id  INT,
    @reason         NVARCHAR(500)
AS BEGIN
    SET NOCOUNT ON;

    DECLARE @student_id INT, @status NVARCHAR(20);
    SELECT @student_id = student_id,
           @status     = status
    FROM   ENROLLMENTS
    WHERE  enrollment_id = @enrollment_id;

    IF @student_id IS NULL
    BEGIN
        RAISERROR('Enrollment not found.', 16, 1);
        RETURN;
    END

    IF @status <> 'active'
    BEGIN
        RAISERROR('Only active enrollments can be withdrawn.', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1 FROM WITHDRAWAL_REQUESTS
        WHERE enrollment_id = @enrollment_id
          AND status = 'pending'
    )
    BEGIN
        RAISERROR('A pending withdrawal request already exists for this enrollment.', 16, 1);
        RETURN;
    END

    -- Delete old rejected request so student can re-apply
    DELETE FROM WITHDRAWAL_REQUESTS
    WHERE enrollment_id = @enrollment_id
      AND status = 'rejected';

    INSERT INTO WITHDRAWAL_REQUESTS (enrollment_id, student_id, reason)
    VALUES (@enrollment_id, @student_id, @reason);

    PRINT 'Withdrawal request submitted.';
END
GO

-- SP6: Admin approves or rejects a withdrawal request (US-3.5c, from UMS_Complete)
CREATE PROCEDURE SP_ReviewWithdrawal
    @request_id     INT,
    @admin_user_id  INT,
    @decision       NVARCHAR(20),   -- 'approved' | 'rejected'
    @admin_comment  NVARCHAR(300)   = NULL
AS BEGIN
    SET NOCOUNT ON;

    IF @decision NOT IN ('approved', 'rejected')
    BEGIN
        RAISERROR('Decision must be ''approved'' or ''rejected''.', 16, 1);
        RETURN;
    END

    DECLARE @enrollment_id INT, @current_status NVARCHAR(20);
    SELECT @enrollment_id  = enrollment_id,
           @current_status = status
    FROM   WITHDRAWAL_REQUESTS
    WHERE  request_id = @request_id;

    IF @enrollment_id IS NULL
    BEGIN
        RAISERROR('Withdrawal request not found.', 16, 1);
        RETURN;
    END

    IF @current_status <> 'pending'
    BEGIN
        RAISERROR('Only pending requests can be reviewed.', 16, 1);
        RETURN;
    END

    UPDATE WITHDRAWAL_REQUESTS
    SET    status        = @decision,
           admin_comment = @admin_comment,
           reviewed_by   = @admin_user_id,
           reviewed_at   = GETDATE()
    WHERE  request_id    = @request_id;

    IF @decision = 'approved'
    BEGIN
        UPDATE ENROLLMENTS
        SET    status = 'withdrawn'
        WHERE  enrollment_id = @enrollment_id;
    END

    PRINT 'Withdrawal request reviewed.';
END
GO

-- ================================================================
-- USER MANAGEMENT STORED PROCEDURES (US-3.8a, from UMS_Complete)
-- ================================================================

-- SP7: Create a new student or teacher account
CREATE PROCEDURE SP_CreateUser
    @role          NVARCHAR(20),
    @full_name     NVARCHAR(100),
    @username      NVARCHAR(50),
    @password      NVARCHAR(255),
    @email         NVARCHAR(100),
    @sub_dept_id   INT            = NULL,
    @major_dept_id INT            = NULL,
    @roll_number   NVARCHAR(20)   = NULL,
    @batch_year    SMALLINT       = NULL,
    @emp_code      NVARCHAR(20)   = NULL,
    @designation   NVARCHAR(60)   = NULL
AS BEGIN
    SET NOCOUNT ON;

    IF @role NOT IN ('student', 'teacher')
    BEGIN
        RAISERROR('Role must be ''student'' or ''teacher''.', 16, 1);
        RETURN;
    END

    IF EXISTS (SELECT 1 FROM USERS WHERE username = @username)
    BEGIN
        RAISERROR('Username already exists. Choose a different username.', 16, 1);
        RETURN;
    END

    IF EXISTS (SELECT 1 FROM USERS WHERE email = @email)
    BEGIN
        RAISERROR('Email address is already registered.', 16, 1);
        RETURN;
    END

    IF @role = 'student'
    BEGIN
        IF @roll_number IS NULL OR LTRIM(RTRIM(@roll_number)) = ''
        BEGIN
            RAISERROR('Roll number is required for students.', 16, 1);
            RETURN;
        END
        IF EXISTS (SELECT 1 FROM STUDENTS WHERE roll_number = @roll_number)
        BEGIN
            RAISERROR('Roll number already exists.', 16, 1);
            RETURN;
        END
        IF @sub_dept_id IS NULL
        BEGIN
            RAISERROR('Sub-department is required for students.', 16, 1);
            RETURN;
        END
        IF @batch_year IS NULL
        BEGIN
            RAISERROR('Batch year is required for students.', 16, 1);
            RETURN;
        END
    END

    IF @role = 'teacher'
    BEGIN
        IF @emp_code IS NULL OR LTRIM(RTRIM(@emp_code)) = ''
        BEGIN
            RAISERROR('Employee code is required for teachers.', 16, 1);
            RETURN;
        END
        IF EXISTS (SELECT 1 FROM TEACHERS WHERE employee_code = @emp_code)
        BEGIN
            RAISERROR('Employee code already exists.', 16, 1);
            RETURN;
        END
        IF @sub_dept_id IS NULL
        BEGIN
            RAISERROR('Sub-department is required for teachers.', 16, 1);
            RETURN;
        END
    END

    DECLARE @new_user_id INT;
    INSERT INTO USERS (username, password_hash, role, email)
    VALUES (@username, @password, @role, @email);
    SET @new_user_id = SCOPE_IDENTITY();

    IF @role = 'student'
    BEGIN
        INSERT INTO STUDENTS (user_id, sub_dept_id, full_name, roll_number, batch_year)
        VALUES (@new_user_id, @sub_dept_id, @full_name, @roll_number, @batch_year);
    END
    ELSE IF @role = 'teacher'
    BEGIN
        INSERT INTO TEACHERS (user_id, sub_dept_id, full_name, employee_code, designation)
        VALUES (@new_user_id, @sub_dept_id, @full_name, @emp_code, @designation);
    END

    SELECT @new_user_id AS new_user_id;
END
GO

-- SP8: Update editable fields for a student or teacher
CREATE PROCEDURE SP_UpdateUser
    @user_id          INT,
    @full_name        NVARCHAR(100)  = NULL,
    @email            NVARCHAR(100)  = NULL,
    @new_password     NVARCHAR(255)  = NULL,
    @sub_dept_id      INT            = NULL,
    @designation      NVARCHAR(60)   = NULL,   -- teachers only
    @roll_number      NVARCHAR(20)   = NULL,   -- students only
    @batch_year       SMALLINT       = NULL,   -- students only
    @current_semester TINYINT        = NULL    -- students only
AS BEGIN
    SET NOCOUNT ON;

    DECLARE @role NVARCHAR(20), @is_active BIT;
    SELECT @role = role, @is_active = is_active
    FROM   USERS WHERE user_id = @user_id;

    IF @role IS NULL
    BEGIN
        RAISERROR('User not found.', 16, 1);
        RETURN;
    END

    IF @email IS NOT NULL
       AND EXISTS (SELECT 1 FROM USERS WHERE email = @email AND user_id <> @user_id)
    BEGIN
        RAISERROR('Email address is already in use by another account.', 16, 1);
        RETURN;
    END

    IF @roll_number IS NOT NULL AND @role = 'student'
       AND EXISTS (SELECT 1 FROM STUDENTS
                   WHERE roll_number = @roll_number
                     AND user_id <> @user_id)
    BEGIN
        RAISERROR('Roll number is already assigned to another student.', 16, 1);
        RETURN;
    END

    UPDATE USERS
    SET email         = ISNULL(@email,        email),
        password_hash = ISNULL(@new_password, password_hash)
    WHERE user_id = @user_id;

    IF @role = 'student'
    BEGIN
        UPDATE STUDENTS
        SET full_name        = ISNULL(@full_name,        full_name),
            sub_dept_id      = ISNULL(@sub_dept_id,      sub_dept_id),
            roll_number      = ISNULL(@roll_number,      roll_number),
            batch_year       = ISNULL(@batch_year,       batch_year),
            current_semester = ISNULL(@current_semester, current_semester)
        WHERE user_id = @user_id;
    END
    ELSE IF @role = 'teacher'
    BEGIN
        UPDATE TEACHERS
        SET full_name   = ISNULL(@full_name,   full_name),
            sub_dept_id = ISNULL(@sub_dept_id, sub_dept_id),
            designation = ISNULL(@designation, designation)
        WHERE user_id = @user_id;
    END

    PRINT 'User updated successfully.';
END
GO

-- SP9: Soft-delete a user account
CREATE PROCEDURE SP_DeactivateUser
    @user_id                  INT,
    @requesting_admin_user_id INT
AS BEGIN
    SET NOCOUNT ON;

    DECLARE @is_active BIT, @role NVARCHAR(20);
    SELECT @is_active = is_active, @role = role
    FROM   USERS WHERE user_id = @user_id;

    IF @is_active IS NULL
    BEGIN
        RAISERROR('User not found.', 16, 1);
        RETURN;
    END

    IF @user_id = @requesting_admin_user_id
    BEGIN
        RAISERROR('You cannot deactivate your own account.', 16, 1);
        RETURN;
    END

    IF @is_active = 0
    BEGIN
        RAISERROR('User account is already inactive.', 16, 1);
        RETURN;
    END

    UPDATE USERS SET is_active = 0 WHERE user_id = @user_id;

    PRINT 'User deactivated successfully.';
END
GO

-- ================================================================
-- TIMETABLE STORED PROCEDURES (US-3.7a / US-3.11a, from UMS_Complete)
-- ================================================================

-- SP10: Insert one weekly timetable slot for a course offering
--       Advisory room-conflict detection (warn but not hard-block)
CREATE PROCEDURE SP_AddTimetableSlot
    @offering_id  INT,
    @day_of_week  NVARCHAR(10),
    @start_time   TIME,
    @end_time     TIME,
    @room_number  NVARCHAR(20)  = NULL,
    @force        BIT           = 0       -- 1 = insert even if room conflict exists
AS BEGIN
    SET NOCOUNT ON;

    IF @day_of_week NOT IN ('Monday','Tuesday','Wednesday','Thursday','Friday')
    BEGIN
        RAISERROR('day_of_week must be a weekday (Monday–Friday).', 16, 1);
        RETURN;
    END

    IF @end_time <= @start_time
    BEGIN
        RAISERROR('end_time must be after start_time.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (SELECT 1 FROM COURSE_OFFERINGS WHERE offering_id = @offering_id)
    BEGIN
        RAISERROR('Course offering not found.', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1 FROM TIMETABLES
        WHERE offering_id = @offering_id
          AND day_of_week = @day_of_week
          AND start_time  = @start_time
    )
    BEGIN
        RAISERROR('This offering already has a slot on that day at that start time.', 16, 1);
        RETURN;
    END

    -- Room conflict check (overlapping time on same day in same room)
    SELECT
        co.offering_id                           AS conflicting_offering,
        c.course_code,
        co.section,
        t.day_of_week,
        CONVERT(VARCHAR(5), t.start_time, 108)   AS start_time,
        CONVERT(VARCHAR(5), t.end_time,   108)   AS end_time,
        t.room_number
    FROM   TIMETABLES       t
    JOIN   COURSE_OFFERINGS co ON co.offering_id = t.offering_id
    JOIN   COURSES           c ON c.course_id    = co.course_id
    WHERE  @room_number IS NOT NULL
      AND  t.room_number  = @room_number
      AND  t.day_of_week  = @day_of_week
      AND  t.offering_id <> @offering_id
      AND  t.start_time   < @end_time
      AND  t.end_time     > @start_time;

    DECLARE @conflict_count INT = @@ROWCOUNT;

    IF @force = 1 OR @conflict_count = 0
    BEGIN
        INSERT INTO TIMETABLES (offering_id, day_of_week, start_time, end_time, room_number)
        VALUES (@offering_id, @day_of_week, @start_time, @end_time, @room_number);

        SELECT 1 AS inserted, SCOPE_IDENTITY() AS new_timetable_id;
    END
    ELSE
    BEGIN
        SELECT 0 AS inserted, NULL AS new_timetable_id;
    END
END
GO

-- SP11: Remove one timetable slot by timetable_id
CREATE PROCEDURE SP_DeleteTimetableSlot
    @timetable_id  INT
AS BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (SELECT 1 FROM TIMETABLES WHERE timetable_id = @timetable_id)
    BEGIN
        RAISERROR('Timetable slot not found.', 16, 1);
        RETURN;
    END

    DELETE FROM TIMETABLES WHERE timetable_id = @timetable_id;
    PRINT 'Timetable slot deleted.';
END
GO

-- ================================================================
-- PART 4: DUMMY DATA
-- ================================================================

-- ────────────────────────────────────────────────────────────────
-- 4.1 MAJOR DEPARTMENTS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT MAJOR_DEPARTMENTS ON;
INSERT INTO MAJOR_DEPARTMENTS (major_dept_id, name, code, description) VALUES
    (1, N'Computing',           N'COMP', N'Faculty of Computing'),
    (2, N'Engineering',         N'ENGR', N'Faculty of Engineering'),
    (3, N'Business/Management', N'MGMT', N'Faculty of Business and Management');
SET IDENTITY_INSERT MAJOR_DEPARTMENTS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.2 SUB DEPARTMENTS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT SUB_DEPARTMENTS ON;
INSERT INTO SUB_DEPARTMENTS (sub_dept_id, major_dept_id, name, code) VALUES
    (1,  1, N'Computer Science',         N'CS'),
    (2,  1, N'Artificial Intelligence',  N'AI'),
    (3,  1, N'Data Science',             N'DS'),
    (4,  1, N'Cyber Security',           N'CYBER'),
    (5,  1, N'Software Engineering',     N'SE'),
    (6,  2, N'Computer Engineering',     N'CE'),
    (7,  2, N'Electrical Engineering',   N'EE'),
    (8,  3, N'Accounting & Finance',     N'AF'),
    (9,  3, N'Business Administration',  N'BA'),
    (10, 3, N'BBA FinTech',              N'FINTECH');
SET IDENTITY_INSERT SUB_DEPARTMENTS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.3 USERS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT USERS ON;
INSERT INTO USERS (user_id, username, password_hash, role, email) VALUES
    -- Students
    (1,  N'ali.raza',    N'$2b$10$DUMMY_HASH_01', N'student', N'ali.raza@ums.edu.pk'),
    (2,  N'sara.khan',   N'$2b$10$DUMMY_HASH_02', N'student', N'sara.khan@ums.edu.pk'),
    (3,  N'usman.tariq', N'$2b$10$DUMMY_HASH_03', N'student', N'usman.tariq@ums.edu.pk'),
    (4,  N'hina.malik',  N'$2b$10$DUMMY_HASH_04', N'student', N'hina.malik@ums.edu.pk'),
    (5,  N'zaid.shah',   N'$2b$10$DUMMY_HASH_05', N'student', N'zaid.shah@ums.edu.pk'),
    (6,  N'nadia.iqbal', N'$2b$10$DUMMY_HASH_06', N'student', N'nadia.iqbal@ums.edu.pk'),
    (7,  N'fahad.ahmed', N'$2b$10$DUMMY_HASH_07', N'student', N'fahad.ahmed@ums.edu.pk'),
    (8,  N'amna.butt',   N'$2b$10$DUMMY_HASH_08', N'student', N'amna.butt@ums.edu.pk'),
    (9,  N'bilal.ch',    N'$2b$10$DUMMY_HASH_09', N'student', N'bilal.ch@ums.edu.pk'),
    (10, N'sana.mir',    N'$2b$10$DUMMY_HASH_10', N'student', N'sana.mir@ums.edu.pk'),
    -- Teachers
    (11, N'dr.asif',     N'$2b$10$DUMMY_HASH_11', N'teacher', N'dr.asif@ums.edu.pk'),
    (12, N'ms.fatima',   N'$2b$10$DUMMY_HASH_12', N'teacher', N'ms.fatima@ums.edu.pk'),
    (13, N'dr.nadeem',   N'$2b$10$DUMMY_HASH_13', N'teacher', N'dr.nadeem@ums.edu.pk'),
    (14, N'mr.salman',   N'$2b$10$DUMMY_HASH_14', N'teacher', N'mr.salman@ums.edu.pk'),
    (15, N'ms.ayesha',   N'$2b$10$DUMMY_HASH_15', N'teacher', N'ms.ayesha@ums.edu.pk'),
    -- Admins
    (16, N'admin.comp',  N'$2b$10$DUMMY_HASH_16', N'admin',   N'admin.comp@ums.edu.pk'),
    (17, N'admin.engr',  N'$2b$10$DUMMY_HASH_17', N'admin',   N'admin.engr@ums.edu.pk'),
    (18, N'admin.mgmt',  N'$2b$10$DUMMY_HASH_18', N'admin',   N'admin.mgmt@ums.edu.pk');
SET IDENTITY_INSERT USERS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.4 STUDENTS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT STUDENTS ON;
INSERT INTO STUDENTS
    (student_id, user_id, sub_dept_id, full_name, roll_number, batch_year, current_semester)
VALUES
    (1,  1,  1,  N'Ali Raza',      N'CS-2023-001', 2023, 3),  -- CS
    (2,  2,  1,  N'Sara Khan',     N'CS-2023-002', 2023, 3),  -- CS
    (3,  3,  2,  N'Usman Tariq',   N'AI-2023-001', 2023, 3),  -- AI
    (4,  4,  3,  N'Hina Malik',    N'DS-2023-001', 2023, 3),  -- DS
    (5,  5,  4,  N'Zaid Shah',     N'CY-2023-001', 2023, 3),  -- CYBER
    (6,  6,  5,  N'Nadia Iqbal',   N'SE-2023-001', 2023, 3),  -- SE
    (7,  7,  6,  N'Fahad Ahmed',   N'CE-2023-001', 2023, 3),  -- CE
    (8,  8,  7,  N'Amna Butt',     N'EE-2023-001', 2023, 3),  -- EE
    (9,  9,  8,  N'Bilal Ch',      N'AF-2023-001', 2023, 3),  -- AF
    (10, 10, 9,  N'Sana Mir',      N'BA-2023-001', 2023, 3);  -- BA
SET IDENTITY_INSERT STUDENTS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.5 TEACHERS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT TEACHERS ON;
INSERT INTO TEACHERS
    (teacher_id, user_id, sub_dept_id, full_name, employee_code, designation)
VALUES
    (1, 11, 1, N'Dr. Asif Mehmood',  N'EMP-001', N'Associate Professor'),
    (2, 12, 2, N'Ms. Fatima Zahra',  N'EMP-002', N'Lecturer'),
    (3, 13, 3, N'Dr. Nadeem Hassan', N'EMP-003', N'Assistant Professor'),
    (4, 14, 6, N'Mr. Salman Rauf',   N'EMP-004', N'Lecturer'),
    (5, 15, 8, N'Ms. Ayesha Noor',   N'EMP-005', N'Lecturer');
SET IDENTITY_INSERT TEACHERS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.6 ADMINS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT ADMINS ON;
INSERT INTO ADMINS (admin_id, user_id, major_dept_id, full_name) VALUES
    (1, 16, 1, N'Computing Admin'),
    (2, 17, 2, N'Engineering Admin'),
    (3, 18, 3, N'Management Admin');
SET IDENTITY_INSERT ADMINS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.7 COURSE CATEGORIES (must be before COURSES due to FK)
-- ────────────────────────────────────────────────────────────────
INSERT INTO COURSE_CATEGORIES (name, description) VALUES
    (N'Core',               N'Mandatory courses required for degree completion'),
    (N'Elective',           N'Optional courses chosen by the student'),
    (N'Lab',                N'Practical/laboratory component'),
    (N'Final Year Project', N'Capstone project course');
GO

-- ────────────────────────────────────────────────────────────────
-- 4.8 COURSES (includes category_id from UMS_DB)
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT COURSES ON;
INSERT INTO COURSES
    (course_id, major_dept_id, course_code, course_name, credit_hours, category_id)
VALUES
    (1,  1, N'CS101', N'Programming Fundamentals',        3, 1),
    (2,  1, N'CS201', N'Data Structures & Algorithms',    3, 1),
    (3,  1, N'CS301', N'Database Systems',                3, 1),
    (4,  1, N'AI201', N'Machine Learning',                3, 2),
    (5,  1, N'DS201', N'Data Analytics & Visualization',  3, 2),
    (6,  1, N'CY201', N'Network Security',                3, 2),
    (7,  1, N'SE201', N'Software Design Patterns',        3, 2),
    (8,  2, N'CE101', N'Digital Logic Design',            3, 1),
    (9,  2, N'EE101', N'Circuit Analysis',                3, 1),
    (10, 2, N'CE201', N'Microprocessor Systems',          3, 2),
    (11, 3, N'BA101', N'Principles of Management',        3, 1),
    (12, 3, N'AF201', N'Financial Accounting',            3, 1),
    (13, 3, N'FT201', N'FinTech Fundamentals',            3, 2);
SET IDENTITY_INSERT COURSES OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.9 DEGREE REQUIREMENTS (from UMS_DB)
-- ────────────────────────────────────────────────────────────────
INSERT INTO DEGREE_REQUIREMENTS (sub_dept_id, category_id, required_credits, min_courses) VALUES
    (1, 1, 60, 10),   -- CS: 60 core credits
    (1, 2, 18,  6),   -- CS: 18 elective credits
    (1, 3,  6,  2);   -- CS: 6 lab credits
GO

-- ────────────────────────────────────────────────────────────────
-- 4.10 COURSE VISIBILITY (ELIGIBILITY)
-- ────────────────────────────────────────────────────────────────
INSERT INTO COURSE_SUB_DEPT_ELIGIBILITY (course_id, sub_dept_id) VALUES
    (1,1),(1,2),(1,3),(1,4),(1,5),  -- CS101: all 5 Computing sub-depts
    (2,1),(2,2),(2,3),(2,5),        -- CS201: CS, AI, DS, SE
    (3,1),(3,3),(3,5),              -- CS301: CS, DS, SE
    (4,2),(4,3),                    -- AI201: AI, DS
    (5,2),(5,3),                    -- DS201: DS, AI
    (6,1),(6,4),                    -- CY201: CYBER, CS
    (7,1),(7,5),                    -- SE201: SE, CS
    (8,6),                          -- CE101: CE only
    (9,6),(9,7),                    -- EE101: EE, CE
    (10,6),                         -- CE201: CE only
    (11,8),(11,9),(11,10),          -- BA101: AF, BA, FINTECH
    (12,8),(12,10),                 -- AF201: AF, FINTECH
    (13,10);                        -- FT201: FINTECH only
GO

-- ────────────────────────────────────────────────────────────────
-- 4.11 COURSE PREREQUISITES (from UMS_DB)
-- ────────────────────────────────────────────────────────────────
INSERT INTO COURSE_PREREQUISITES (course_id, prereq_course_id, min_grade_pct) VALUES
    (2,  1,  50.00),   -- CS201 needs CS101
    (3,  2,  50.00),   -- CS301 needs CS201
    (4,  1,  60.00),   -- AI201 needs CS101 at 60%+
    (5,  4,  50.00),   -- DS201 needs AI201
    (10, 8,  50.00);   -- CE201 needs CE101
GO

-- ────────────────────────────────────────────────────────────────
-- 4.12 SEMESTERS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT SEMESTERS ON;
INSERT INTO SEMESTERS (semester_id, semester_name, start_date, end_date, is_active, enrollment_open) VALUES
    (1, N'Fall 2023',   '2023-09-01', '2024-01-15', 0, 0),
    (2, N'Spring 2024', '2024-02-01', '2024-06-30', 0, 0),
    (3, N'Fall 2024',   '2024-09-01', '2025-01-15', 1, 1);
SET IDENTITY_INSERT SEMESTERS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.13 COURSE OFFERINGS (active semester = Fall 2024)
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT COURSE_OFFERINGS ON;
INSERT INTO COURSE_OFFERINGS
    (offering_id, course_id, sub_dept_id, semester_id, teacher_id,
     section, room_number, schedule_days, class_time, max_capacity)
VALUES
    (1,  1, 1, 3, 1, N'A', N'CS-101', N'Mon/Wed', '08:30', 40),  -- CS101 for CS
    (2,  1, 3, 3, 1, N'B', N'CS-102', N'Tue/Thu', '08:30', 40),  -- CS101 for DS
    (3,  1, 5, 3, 1, N'C', N'CS-103', N'Mon/Wed', '10:30', 40),  -- CS101 for SE
    (4,  2, 1, 3, 1, N'A', N'CS-201', N'Tue/Thu', '10:30', 35),  -- CS201 for CS
    (5,  2, 3, 3, 3, N'A', N'DS-201', N'Mon/Wed', '12:30', 35),  -- CS201 for DS
    (6,  3, 1, 3, 1, N'A', N'CS-301', N'Mon/Wed', '14:00', 40),  -- CS301 for CS
    (7,  4, 2, 3, 2, N'A', N'AI-201', N'Tue/Thu', '09:00', 30),  -- AI201 for AI
    (8,  5, 3, 3, 3, N'A', N'DS-301', N'Fri',     '09:00', 30),  -- DS201 for DS
    (9,  6, 4, 3, 1, N'A', N'CY-201', N'Mon/Wed', '11:00', 30),  -- CY201 for CYBER
    (10, 7, 5, 3, 1, N'A', N'SE-201', N'Tue/Thu', '11:00', 35),  -- SE201 for SE
    (11, 8, 6, 3, 4, N'A', N'EG-101', N'Mon/Wed', '08:00', 40),  -- CE101 for CE
    (12, 9, 7, 3, 4, N'A', N'EG-201', N'Tue/Thu', '08:00', 40),  -- EE101 for EE
    (13,11, 8, 3, 5, N'A', N'MG-101', N'Mon/Wed', '09:00', 45),  -- BA101 for AF
    (14,11, 9, 3, 5, N'B', N'MG-102', N'Tue/Thu', '09:00', 45),  -- BA101 for BA
    (15,12, 8, 3, 5, N'A', N'MG-201', N'Fri',     '10:00', 35),  -- AF201 for AF
    -- Past offerings: Fall 2023 (semester_id = 1)
    (16, 1, 1, 1, 1, N'A', N'CS-101', N'Mon/Wed', '08:30', 40),  -- CS101 for CS
    (17, 2, 1, 1, 1, N'A', N'CS-201', N'Tue/Thu', '10:30', 35),  -- CS201 for CS
    -- Past offerings: Spring 2024 (semester_id = 2)
    (18, 1, 1, 2, 1, N'A', N'CS-101', N'Mon/Wed', '08:30', 40),  -- CS101 for CS
    (19, 2, 1, 2, 1, N'A', N'CS-201', N'Tue/Thu', '10:30', 35),  -- CS201 for CS
    (20, 3, 1, 2, 1, N'A', N'CS-301', N'Mon/Wed', '14:00', 40);  -- CS301 for CS
SET IDENTITY_INSERT COURSE_OFFERINGS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.14 ENROLLMENTS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT ENROLLMENTS ON;
INSERT INTO ENROLLMENTS (enrollment_id, student_id, offering_id, status) VALUES
    -- Ali Raza (CS) → CS101-CS, CS201-CS, CS301-CS
    (1,  1,  1,  N'active'),
    (2,  1,  4,  N'active'),
    (3,  1,  6,  N'active'),
    -- Sara Khan (CS) → CS101-CS, CS201-CS
    (4,  2,  1,  N'active'),
    (5,  2,  4,  N'active'),
    -- Usman Tariq (AI) → AI201
    (6,  3,  7,  N'active'),
    -- Hina Malik (DS) → CS101-DS, CS201-DS, DS201
    (7,  4,  2,  N'active'),
    (8,  4,  5,  N'active'),
    (9,  4,  8,  N'active'),
    -- Zaid Shah (CYBER) → CY201
    (10, 5,  9,  N'active'),
    -- Nadia Iqbal (SE) → CS101-SE, SE201
    (11, 6,  3,  N'active'),
    (12, 6,  10, N'active'),
    -- Fahad Ahmed (CE) → CE101
    (13, 7,  11, N'active'),
    -- Amna Butt (EE) → EE101
    (14, 8,  12, N'active'),
    -- Bilal Ch (AF) → BA101-AF, AF201
    (15, 9,  13, N'active'),
    (16, 9,  15, N'active'),
    -- Sana Mir (BA) → BA101-BA
    (17, 10, 14, N'active'),
    -- Ali Raza historical: Fall 2023
    (18, 1, 16, N'completed'),
    (19, 1, 17, N'completed'),
    -- Ali Raza historical: Spring 2024
    (20, 1, 18, N'completed'),
    (21, 1, 19, N'completed'),
    (22, 1, 20, N'completed');
SET IDENTITY_INSERT ENROLLMENTS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.15 ATTENDANCE
-- ────────────────────────────────────────────────────────────────
INSERT INTO ATTENDANCE (enrollment_id, class_date, status) VALUES
    (1, '2024-09-02', N'present'),
    (1, '2024-09-04', N'present'),
    (1, '2024-09-09', N'absent'),
    (2, '2024-09-03', N'present'),
    (2, '2024-09-05', N'present'),
    (2, '2024-09-10', N'present'),
    (3, '2024-09-02', N'present'),
    (3, '2024-09-04', N'late'),
    (3, '2024-09-09', N'present'),
    (4, '2024-09-02', N'present'),
    (4, '2024-09-04', N'present'),
    (4, '2024-09-09', N'present'),
    (7, '2024-09-03', N'absent'),
    (7, '2024-09-05', N'present'),
    (7, '2024-09-10', N'present'),
    (13,'2024-09-02', N'present'),
    (13,'2024-09-04', N'present'),
    (13,'2024-09-09', N'present'),
    (15,'2024-09-02', N'present'),
    (15,'2024-09-04', N'absent'),
    (15,'2024-09-09', N'present');
GO

-- ────────────────────────────────────────────────────────────────
-- 4.16 MARKS (current semester + historical)
-- ────────────────────────────────────────────────────────────────
INSERT INTO MARKS (enrollment_id, assessment_type, marks_obtained, total_marks) VALUES
    -- Ali - CS101 (enrollment 1)
    (1, N'quiz',       8.5, 10),
    (1, N'assignment', 18,  20),
    (1, N'mid',        38,  50),
    -- Ali - CS201 (enrollment 2)
    (2, N'quiz',       9,   10),
    (2, N'assignment', 17,  20),
    (2, N'mid',        42,  50),
    -- Ali - CS301 (enrollment 3)
    (3, N'quiz',       7,   10),
    (3, N'assignment', 15,  20),
    (3, N'mid',        35,  50),
    -- Sara - CS101 (enrollment 4)
    (4, N'quiz',       10,  10),
    (4, N'assignment', 20,  20),
    (4, N'mid',        47,  50),
    -- Hina - CS101 DS section (enrollment 7)
    (7, N'quiz',       6,   10),
    (7, N'assignment', 14,  20),
    (7, N'mid',        30,  50),
    -- Fahad - CE101 (enrollment 13)
    (13,N'quiz',       8,   10),
    (13,N'assignment', 16,  20),
    (13,N'mid',        40,  50),
    -- Bilal - BA101 (enrollment 15)
    (15,N'quiz',       9,   10),
    (15,N'assignment', 17,  20),
    (15,N'mid',        44,  50),
    -- Historical: Fall 2023 CS101 (enrollment 18)
    (18, N'quiz',       9,   10),
    (18, N'assignment', 19,  20),
    (18, N'mid',        44,  50),
    (18, N'final',      72,  100),
    -- Historical: Fall 2023 CS201 (enrollment 19)
    (19, N'quiz',       8,   10),
    (19, N'assignment', 17,  20),
    (19, N'mid',        40,  50),
    (19, N'final',      65,  100),
    -- Historical: Spring 2024 CS101 (enrollment 20)
    (20, N'quiz',       10,  10),
    (20, N'assignment', 20,  20),
    (20, N'mid',        47,  50),
    (20, N'final',      80,  100),
    -- Historical: Spring 2024 CS201 (enrollment 21)
    (21, N'quiz',       9,   10),
    (21, N'assignment', 18,  20),
    (21, N'mid',        43,  50),
    (21, N'final',      74,  100),
    -- Historical: Spring 2024 CS301 (enrollment 22)
    (22, N'quiz',       7,   10),
    (22, N'assignment', 15,  20),
    (22, N'mid',        36,  50),
    (22, N'final',      60,  100);
GO

-- ────────────────────────────────────────────────────────────────
-- 4.17 FEE RECORDS & CHALLANS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT FEE_RECORDS ON;
INSERT INTO FEE_RECORDS
    (fee_id, student_id, semester_id, total_amount, paid_amount, due_date, status)
VALUES
    (1, 1, 3, 45000, 45000, '2024-09-15', N'paid'),
    (2, 2, 3, 45000, 0,     '2024-09-15', N'unpaid'),
    (3, 3, 3, 47000, 47000, '2024-09-15', N'paid'),
    (4, 4, 3, 47000, 20000, '2024-09-15', N'partial'),
    (5, 5, 3, 45000, 0,     '2024-09-15', N'unpaid'),
    (6, 7, 3, 50000, 50000, '2024-09-15', N'paid'),
    (7, 9, 3, 42000, 42000, '2024-09-15', N'paid');
SET IDENTITY_INSERT FEE_RECORDS OFF;
GO

INSERT INTO FEE_CHALLANS (fee_id, challan_number, generated_date, expiry_date, is_paid)
VALUES
    (1, N'CH-1-3-202409010830', '2024-09-01', '2024-09-15', 1),
    (2, N'CH-2-3-202409011000', '2024-09-01', '2024-09-15', 0),
    (3, N'CH-3-3-202409010845', '2024-09-01', '2024-09-15', 1),
    (4, N'CH-4-3-202409011015', '2024-09-01', '2024-09-15', 0),
    (6, N'CH-6-3-202409010900', '2024-09-01', '2024-09-15', 1),
    (7, N'CH-7-3-202409010915', '2024-09-01', '2024-09-15', 1);
GO

-- ────────────────────────────────────────────────────────────────
-- 4.18 ANNOUNCEMENTS
-- ────────────────────────────────────────────────────────────────
INSERT INTO ANNOUNCEMENTS
    (posted_by, major_dept_id, offering_id, title, content, target_role)
VALUES
    (16, NULL, NULL,
     N'Welcome to Fall 2024',
     N'Classes commence Monday 2 September 2024. Please check your timetables.',
     N'all'),
    (16, 1, NULL,
     N'Computing Lab Booking Open',
     N'All computing students can now book lab slots via the portal.',
     N'student'),
    (11, NULL, 1,
     N'Assignment 1 Released',
     N'Programming Fundamentals Assignment 1 is uploaded. Due: 20 Sep.',
     N'student'),
    (17, 2, NULL,
     N'Engineering Lab Induction',
     N'Mandatory lab induction on 5 September at 9am in Lab EE-01.',
     N'student');
GO

-- ────────────────────────────────────────────────────────────────
-- 4.19 COURSE MATERIALS
-- ────────────────────────────────────────────────────────────────
INSERT INTO COURSE_MATERIALS
    (offering_id, uploaded_by, title, file_path, file_type)
VALUES
    (1,  11, N'Lecture 1 - Intro to Programming',  N'/materials/cs101/lec1.pdf',    N'pdf'),
    (1,  11, N'Assignment 1 Instructions',          N'/materials/cs101/asgn1.pdf',   N'pdf'),
    (7,  12, N'ML Foundations Slides',              N'/materials/ai201/week1.pptx',  N'pptx'),
    (11,  4, N'Digital Logic Lecture Notes',        N'/materials/ce101/lec1.pdf',    N'pdf'),
    (13,  5, N'Management Principles Week 1',       N'/materials/ba101/week1.pdf',   N'pdf');
GO

-- ────────────────────────────────────────────────────────────────
-- 4.20 ACADEMIC REQUESTS
-- ────────────────────────────────────────────────────────────────
INSERT INTO ACADEMIC_REQUESTS
    (student_id, offering_id, request_type, description, status)
VALUES
    (1, 1, N'grade_change',
     N'Quiz 1 marks appear incorrect. Requesting re-check.',
     N'pending'),
    (4, 2, N'withdrawal',
     N'Unable to manage workload. Requesting course withdrawal.',
     N'approved'),
    (9, 13, N'retake',
     N'Medical emergency during mid exam. Applying for retake.',
     N'pending');
GO

-- ────────────────────────────────────────────────────────────────
-- 4.21 WITHDRAWAL REQUESTS (from UMS_Complete)
-- ────────────────────────────────────────────────────────────────
INSERT INTO WITHDRAWAL_REQUESTS
    (enrollment_id, student_id, reason, status, admin_comment, reviewed_by, reviewed_at)
VALUES
    -- Sara Khan (student 2, enrollment 5 = CS201-CS) — pending
    (5,  2,
     N'Unable to keep up with the course workload this semester.',
     N'pending', NULL, NULL, NULL),
    -- Hina Malik (student 4, enrollment 8 = CS201-DS) — approved
    (8,  4,
     N'Scheduling conflict with another mandatory course.',
     N'approved',
     N'Request approved. Enrollment marked as withdrawn.',
     16, GETDATE()),
    -- Nadia Iqbal (student 6, enrollment 12 = SE201) — rejected
    (12, 6,
     N'Would like to switch to a different section.',
     N'rejected',
     N'Section transfers are not processed via withdrawal. Please contact the registrar.',
     16, GETDATE());
GO

-- Reflect the approved withdrawal in ENROLLMENTS
UPDATE ENROLLMENTS SET status = 'withdrawn' WHERE enrollment_id = 8;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.22 FEEDBACK
-- ────────────────────────────────────────────────────────────────
INSERT INTO FEEDBACK
    (student_id, offering_id, rating, comments, is_anonymous)
VALUES
    (1,  1,  5, N'Excellent teaching style, very clear explanations.', 1),
    (2,  1,  4, N'Good course overall, more examples would help.',      1),
    (4,  2,  3, N'Pace was a bit fast, lecture notes were helpful.',    1),
    (7,  11, 5, N'Very well structured course, loved the labs.',        0),
    (9,  13, 4, N'Engaging lectures, real-world case studies valued.',  1);
GO

-- ────────────────────────────────────────────────────────────────
-- 4.23 TIMETABLES (comprehensive from UMS_Complete)
-- ────────────────────────────────────────────────────────────────
INSERT INTO TIMETABLES
    (offering_id, day_of_week, start_time, end_time, room_number)
VALUES
    -- Offering  1: CS101-CS  Mon/Wed 08:30-10:00
    (1,  N'Monday',    '08:30', '10:00', N'CS-101'),
    (1,  N'Wednesday', '08:30', '10:00', N'CS-101'),
    -- Offering  2: CS101-DS  Tue/Thu 08:30-10:00
    (2,  N'Tuesday',   '08:30', '10:00', N'CS-102'),
    (2,  N'Thursday',  '08:30', '10:00', N'CS-102'),
    -- Offering  3: CS101-SE  Mon/Wed 10:30-12:00
    (3,  N'Monday',    '10:30', '12:00', N'CS-103'),
    (3,  N'Wednesday', '10:30', '12:00', N'CS-103'),
    -- Offering  4: CS201-CS  Tue/Thu 10:30-12:00
    (4,  N'Tuesday',   '10:30', '12:00', N'CS-201'),
    (4,  N'Thursday',  '10:30', '12:00', N'CS-201'),
    -- Offering  5: CS201-DS  Mon/Wed 12:30-14:00
    (5,  N'Monday',    '12:30', '14:00', N'DS-201'),
    (5,  N'Wednesday', '12:30', '14:00', N'DS-201'),
    -- Offering  6: CS301-CS  Mon/Wed 14:00-15:30
    (6,  N'Monday',    '14:00', '15:30', N'CS-301'),
    (6,  N'Wednesday', '14:00', '15:30', N'CS-301'),
    -- Offering  7: AI201-AI  Tue/Thu 09:00-10:30
    (7,  N'Tuesday',   '09:00', '10:30', N'AI-201'),
    (7,  N'Thursday',  '09:00', '10:30', N'AI-201'),
    -- Offering  8: DS201-DS  Friday 09:00-12:00
    (8,  N'Friday',    '09:00', '12:00', N'DS-301'),
    -- Offering  9: CY201-CYBER  Mon/Wed 11:00-12:30
    (9,  N'Monday',    '11:00', '12:30', N'CY-201'),
    (9,  N'Wednesday', '11:00', '12:30', N'CY-201'),
    -- Offering 10: SE201-SE  Tue/Thu 11:00-12:30
    (10, N'Tuesday',   '11:00', '12:30', N'SE-201'),
    (10, N'Thursday',  '11:00', '12:30', N'SE-201'),
    -- Offering 11: CE101-CE  Mon/Wed 08:00-09:30
    (11, N'Monday',    '08:00', '09:30', N'EG-101'),
    (11, N'Wednesday', '08:00', '09:30', N'EG-101'),
    -- Offering 12: EE101-EE  Tue/Thu 08:00-09:30
    (12, N'Tuesday',   '08:00', '09:30', N'EG-201'),
    (12, N'Thursday',  '08:00', '09:30', N'EG-201'),
    -- Offering 13: BA101-AF  Mon/Wed 09:00-10:30
    (13, N'Monday',    '09:00', '10:30', N'MG-101'),
    (13, N'Wednesday', '09:00', '10:30', N'MG-101'),
    -- Offering 14: BA101-BA  Tue/Thu 09:00-10:30
    (14, N'Tuesday',   '09:00', '10:30', N'MG-102'),
    (14, N'Thursday',  '09:00', '10:30', N'MG-102'),
    -- Offering 15: AF201-AF  Friday 10:00-13:00
    (15, N'Friday',    '10:00', '13:00', N'MG-201');
GO

-- ────────────────────────────────────────────────────────────────
-- 4.24 CREDIT POLICIES (from UMS_DB)
-- ────────────────────────────────────────────────────────────────
INSERT INTO CREDIT_POLICIES (sub_dept_id, min_credits, max_credits, overload_credits) VALUES
    (1,  12, 21, 24),   -- CS
    (2,  12, 21, 24),   -- AI
    (3,  12, 21, 24),   -- DS
    (4,  12, 21, 24),   -- CYBER
    (5,  12, 21, 24),   -- SE
    (6,  12, 21, 24),   -- CE
    (7,  12, 21, 24),   -- EE
    (8,  12, 18, 21),   -- AF
    (9,  12, 18, 21),   -- BA
    (10, 12, 18, 21);   -- FINTECH
GO

-- ================================================================
-- PART 5: VERIFICATION QUERIES
-- ================================================================

-- 1. Full department tree
SELECT md.name AS Faculty, sd.name AS Program, sd.code
FROM   MAJOR_DEPARTMENTS md
JOIN   SUB_DEPARTMENTS   sd ON sd.major_dept_id = md.major_dept_id
ORDER  BY md.name, sd.name;
GO

-- 2. Courses visible to Ali Raza (CS student, student_id = 1)
SELECT course_code, course_name
FROM   VW_STUDENT_VISIBLE_COURSES
WHERE  student_id = 1
ORDER  BY course_code;
GO

-- 3. Offerings Ali Raza can register (student_id = 1)
SELECT course_code, course_name, section, schedule_days, class_time, teacher_name
FROM   VW_STUDENT_REGISTRABLE_OFFERINGS
WHERE  student_id = 1;
GO

-- 4. Courses visible to Hina Malik (DS student, student_id = 4)
SELECT course_code, course_name
FROM   VW_STUDENT_VISIBLE_COURSES
WHERE  student_id = 4
ORDER  BY course_code;
GO

-- 5. Attendance percentage per student
SELECT s.roll_number, s.full_name,
       COUNT(*)  AS total_classes,
       SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) AS present_count,
       CAST(100.0 * SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END)
           / NULLIF(COUNT(*),0) AS DECIMAL(5,2)) AS attendance_pct
FROM STUDENTS    s
JOIN ENROLLMENTS e  ON e.student_id    = s.student_id
JOIN ATTENDANCE  a  ON a.enrollment_id = e.enrollment_id
GROUP BY s.student_id, s.roll_number, s.full_name
ORDER BY s.roll_number;
GO

-- 6. Marks summary per student per course
SELECT s.roll_number, c.course_name,
       SUM(m.marks_obtained) AS obtained,
       SUM(m.total_marks)    AS total,
       CAST(100.0 * SUM(m.marks_obtained) / NULLIF(SUM(m.total_marks),0)
           AS DECIMAL(5,2))  AS percentage
FROM STUDENTS        s
JOIN ENROLLMENTS     e   ON e.student_id    = s.student_id
JOIN COURSE_OFFERINGS co ON co.offering_id  = e.offering_id
JOIN COURSES         c   ON c.course_id     = co.course_id
JOIN MARKS           m   ON m.enrollment_id = e.enrollment_id
GROUP BY s.student_id, s.roll_number, c.course_name
ORDER BY s.roll_number, c.course_name;
GO

-- 7. Fee status overview
SELECT s.roll_number, s.full_name, sem.semester_name,
       fr.total_amount, fr.paid_amount,
       fr.total_amount - fr.paid_amount AS balance,
       fr.status
FROM FEE_RECORDS fr
JOIN STUDENTS  s   ON s.student_id   = fr.student_id
JOIN SEMESTERS sem ON sem.semester_id = fr.semester_id
ORDER BY s.roll_number;
GO

-- 8. All announcements
SELECT a.title,
       COALESCE(md.name, 'University-wide') AS scope,
       a.target_role,
       a.posted_at
FROM ANNOUNCEMENTS a
LEFT JOIN MAJOR_DEPARTMENTS md ON md.major_dept_id = a.major_dept_id
ORDER BY a.posted_at;
GO

-- 9. All users (quick sanity check)
SELECT * FROM USERS;
GO

-- 10. Teachers column info
SELECT COLUMN_NAME, DATA_TYPE
FROM   INFORMATION_SCHEMA.COLUMNS
WHERE  TABLE_NAME = 'TEACHERS'
ORDER  BY ORDINAL_POSITION;

SELECT TOP 5 * FROM TEACHERS;
GO

PRINT '================================================================';
PRINT ' UMS_MERGED.sql finished with no errors.';
PRINT ' Tables: MAJOR_DEPARTMENTS, SUB_DEPARTMENTS, USERS, STUDENTS,';
PRINT '         TEACHERS, ADMINS, COURSE_CATEGORIES, COURSES,';
PRINT '         DEGREE_REQUIREMENTS, COURSE_SUB_DEPT_ELIGIBILITY,';
PRINT '         COURSE_PREREQUISITES, CREDIT_POLICIES, SEMESTERS,';
PRINT '         COURSE_OFFERINGS, ENROLLMENTS, ATTENDANCE, MARKS,';
PRINT '         FEE_RECORDS, FEE_CHALLANS, ANNOUNCEMENTS,';
PRINT '         COURSE_MATERIALS, ACADEMIC_REQUESTS, FEEDBACK,';
PRINT '         TIMETABLES, WITHDRAWAL_REQUESTS (25 tables total)';
PRINT ' Views: VW_STUDENT_VISIBLE_COURSES, VW_STUDENT_REGISTRABLE_OFFERINGS,';
PRINT '        VW_STUDENT_GPA, VW_STUDENT_CREDIT_LOAD';
PRINT ' SPs: SP_RegisterStudent, SP_MarkAttendance, SP_GenerateChallan,';
PRINT '      SP_GetTranscript, SP_RequestWithdrawal, SP_ReviewWithdrawal,';
PRINT '      SP_CreateUser, SP_UpdateUser, SP_DeactivateUser,';
PRINT '      SP_AddTimetableSlot, SP_DeleteTimetableSlot (11 SPs total)';
PRINT ' All dummy data loaded across all tables.';
PRINT '================================================================';
GO

select * from VW_REPORT_ACADEMIC_PERFORMANCE;
select * from VW_REPORT_ATTENDANCE;