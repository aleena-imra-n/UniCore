-- ================================================================
--  UMS_COMPLETE.sql
--  University Management System — SQL Server
--  ONE FILE: Schema + Views + Stored Procedures + Dummy Data
--  Run entirely in SSMS with F5 (Execute All)
--  CS3009 Software Engineering
-- ================================================================

-- ────────────────────────────────────────────────────────────────
-- STEP 0: DATABASE SETUP
-- ────────────────────────────────────────────────────────────────
USE master;
GO

IF EXISTS (SELECT name FROM sys.databases WHERE name = N'UMS_DB')
    DROP DATABASE UMS_DB;
GO

CREATE DATABASE UMS_DB;
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
-- MODULE 3: COURSES & VISIBILITY
-- ────────────────────────────────────────────────────────────────
CREATE TABLE COURSES (
    course_id       INT IDENTITY(1,1) PRIMARY KEY,
    major_dept_id   INT            NOT NULL,
    course_code     NVARCHAR(15)   NOT NULL UNIQUE,
    course_name     NVARCHAR(150)  NOT NULL,
    credit_hours    TINYINT        NOT NULL DEFAULT 3,
    description     NVARCHAR(500),
    is_active       BIT            DEFAULT 1,
    CONSTRAINT FK_Course_MajorDept
        FOREIGN KEY (major_dept_id) REFERENCES MAJOR_DEPARTMENTS(major_dept_id)
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
-- MODULE 4: SEMESTERS & OFFERINGS
-- ────────────────────────────────────────────────────────────────
CREATE TABLE SEMESTERS (
    semester_id     INT IDENTITY(1,1) PRIMARY KEY,
    semester_name   NVARCHAR(50)   NOT NULL,
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL,
    is_active       BIT            DEFAULT 0
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
-- MODULE 5: ENROLLMENTS, ATTENDANCE & MARKS
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
-- MODULE 6: FEES & CHALLANS
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
-- MODULE 7: COMMUNICATION & MATERIALS
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
-- MODULE 8: REQUESTS, FEEDBACK & TIMETABLES
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

CREATE TABLE TIMETABLES (
    timetable_id    INT IDENTITY(1,1) PRIMARY KEY,
    offering_id     INT            NOT NULL,
    day_of_week     NVARCHAR(10)   NOT NULL
                        CHECK (day_of_week IN
                        ('Monday','Tuesday','Wednesday','Thursday','Friday')),
    start_time      TIME           NOT NULL,
    end_time        TIME           NOT NULL,
    room_number     NVARCHAR(20),
    CONSTRAINT FK_TT_Offering
        FOREIGN KEY (offering_id) REFERENCES COURSE_OFFERINGS(offering_id)
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

-- ================================================================
-- PART 3: STORED PROCEDURES
-- ================================================================

-- SP1: Register a student (validates eligibility + capacity + duplicates)
CREATE PROCEDURE SP_RegisterStudent
    @student_id  INT,
    @offering_id INT
AS BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (
        SELECT 1
        FROM COURSE_OFFERINGS co
        JOIN COURSE_SUB_DEPT_ELIGIBILITY e
            ON e.course_id = co.course_id
        JOIN STUDENTS s
            ON s.student_id   = @student_id
           AND s.sub_dept_id  = co.sub_dept_id
           AND s.sub_dept_id  = e.sub_dept_id
        WHERE co.offering_id  = @offering_id
    )
    BEGIN
        RAISERROR('Student not eligible for this offering.', 16, 1);
        RETURN;
    END

    DECLARE @enrolled INT, @cap INT;
    SELECT @enrolled = COUNT(*), @cap = MAX(co.max_capacity)
    FROM ENROLLMENTS e
    JOIN COURSE_OFFERINGS co ON co.offering_id = @offering_id
    WHERE e.offering_id = @offering_id AND e.status = 'active';

    IF @enrolled >= @cap
    BEGIN
        RAISERROR('Course offering is at full capacity.', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1 FROM ENROLLMENTS
        WHERE student_id = @student_id AND offering_id = @offering_id
    )
    BEGIN
        RAISERROR('Student already enrolled in this offering.', 16, 1);
        RETURN;
    END

    INSERT INTO ENROLLMENTS (student_id, offering_id)
    VALUES (@student_id, @offering_id);

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
-- 4.3 USERS  (password_hash is placeholder for bcrypt of "Pass@123")
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
-- 4.4 STUDENTS  (one per sub-department)
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
-- 4.7 COURSES
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT COURSES ON;
INSERT INTO COURSES (course_id, major_dept_id, course_code, course_name, credit_hours) VALUES
    -- Computing
    (1,  1, N'CS101', N'Programming Fundamentals',        3),
    (2,  1, N'CS201', N'Data Structures & Algorithms',    3),
    (3,  1, N'CS301', N'Database Systems',                3),
    (4,  1, N'AI201', N'Machine Learning',                3),
    (5,  1, N'DS201', N'Data Analytics & Visualization',  3),
    (6,  1, N'CY201', N'Network Security',                3),
    (7,  1, N'SE201', N'Software Design Patterns',        3),
    -- Engineering
    (8,  2, N'CE101', N'Digital Logic Design',            3),
    (9,  2, N'EE101', N'Circuit Analysis',                3),
    (10, 2, N'CE201', N'Microprocessor Systems',          3),
    -- Management
    (11, 3, N'BA101', N'Principles of Management',        3),
    (12, 3, N'AF201', N'Financial Accounting',            3),
    (13, 3, N'FT201', N'FinTech Fundamentals',            3);
SET IDENTITY_INSERT COURSES OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.8 COURSE VISIBILITY (ELIGIBILITY)
-- ────────────────────────────────────────────────────────────────
INSERT INTO COURSE_SUB_DEPT_ELIGIBILITY (course_id, sub_dept_id) VALUES
    -- CS101: visible to ALL 5 Computing sub-depts
    (1,1),(1,2),(1,3),(1,4),(1,5),
    -- CS201: CS, AI, DS, SE
    (2,1),(2,2),(2,3),(2,5),
    -- CS301 (DB Systems): CS, DS, SE
    (3,1),(3,3),(3,5),
    -- AI201: AI, DS
    (4,2),(4,3),
    -- DS201: DS, AI
    (5,2),(5,3),
    -- CY201: CYBER, CS
    (6,1),(6,4),
    -- SE201: SE, CS
    (7,1),(7,5),
    -- CE101: CE only
    (8,6),
    -- EE101: EE, CE
    (9,6),(9,7),
    -- CE201: CE only
    (10,6),
    -- BA101: AF, BA, FINTECH
    (11,8),(11,9),(11,10),
    -- AF201: AF, FINTECH
    (12,8),(12,10),
    -- FT201: FINTECH only
    (13,10);
GO

-- ────────────────────────────────────────────────────────────────
-- 4.9 SEMESTERS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT SEMESTERS ON;
INSERT INTO SEMESTERS (semester_id, semester_name, start_date, end_date, is_active) VALUES
    (1, N'Fall 2023',   '2023-09-01', '2024-01-15', 0),
    (2, N'Spring 2024', '2024-02-01', '2024-06-30', 0),
    (3, N'Fall 2024',   '2024-09-01', '2025-01-15', 1);
SET IDENTITY_INSERT SEMESTERS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.10 COURSE OFFERINGS
--      sub_dept_id = who can REGISTER
--      CS101 has 3 separate offerings: one each for CS, DS, SE
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
    (15,12, 8, 3, 5, N'A', N'MG-201', N'Fri',     '10:00', 35);  -- AF201 for AF
SET IDENTITY_INSERT COURSE_OFFERINGS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.11 ENROLLMENTS
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT ENROLLMENTS ON;
INSERT INTO ENROLLMENTS (enrollment_id, student_id, offering_id, status) VALUES
    -- Ali Raza (CS)   → CS101-CS, CS201-CS, CS301-CS
    (1,  1,  1,  N'active'),
    (2,  1,  4,  N'active'),
    (3,  1,  6,  N'active'),
    -- Sara Khan (CS)  → CS101-CS, CS201-CS
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
    (17, 10, 14, N'active');
SET IDENTITY_INSERT ENROLLMENTS OFF;
GO

-- ────────────────────────────────────────────────────────────────
-- 4.12 ATTENDANCE
-- ────────────────────────────────────────────────────────────────
INSERT INTO ATTENDANCE (enrollment_id, class_date, status) VALUES
    -- Enrollment 1 (Ali - CS101)
    (1, '2024-09-02', N'present'),
    (1, '2024-09-04', N'present'),
    (1, '2024-09-09', N'absent'),
    -- Enrollment 2 (Ali - CS201)
    (2, '2024-09-03', N'present'),
    (2, '2024-09-05', N'present'),
    (2, '2024-09-10', N'present'),
    -- Enrollment 3 (Ali - CS301)
    (3, '2024-09-02', N'present'),
    (3, '2024-09-04', N'late'),
    (3, '2024-09-09', N'present'),
    -- Enrollment 4 (Sara - CS101)
    (4, '2024-09-02', N'present'),
    (4, '2024-09-04', N'present'),
    (4, '2024-09-09', N'present'),
    -- Enrollment 7 (Hina - CS101 DS section)
    (7, '2024-09-03', N'absent'),
    (7, '2024-09-05', N'present'),
    (7, '2024-09-10', N'present'),
    -- Enrollment 13 (Fahad - CE101)
    (13,'2024-09-02', N'present'),
    (13,'2024-09-04', N'present'),
    (13,'2024-09-09', N'present'),
    -- Enrollment 15 (Bilal - BA101)
    (15,'2024-09-02', N'present'),
    (15,'2024-09-04', N'absent'),
    (15,'2024-09-09', N'present');
GO

-- ────────────────────────────────────────────────────────────────
-- 4.13 MARKS
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
    (15,N'mid',        44,  50);
GO

-- ────────────────────────────────────────────────────────────────
-- 4.14 FEE RECORDS & CHALLANS
--      IDENTITY_INSERT used so fee_id values are predictable
--      and challan fee_id references are guaranteed correct
-- ────────────────────────────────────────────────────────────────
SET IDENTITY_INSERT FEE_RECORDS ON;
INSERT INTO FEE_RECORDS
    (fee_id, student_id, semester_id, total_amount, paid_amount, due_date, status)
VALUES
    (1, 1, 3, 45000, 45000, '2024-09-15', N'paid'),    -- Ali Raza
    (2, 2, 3, 45000, 0,     '2024-09-15', N'unpaid'),  -- Sara Khan
    (3, 3, 3, 47000, 47000, '2024-09-15', N'paid'),    -- Usman Tariq
    (4, 4, 3, 47000, 20000, '2024-09-15', N'partial'), -- Hina Malik
    (5, 5, 3, 45000, 0,     '2024-09-15', N'unpaid'),  -- Zaid Shah
    (6, 7, 3, 50000, 50000, '2024-09-15', N'paid'),    -- Fahad Ahmed
    (7, 9, 3, 42000, 42000, '2024-09-15', N'paid');    -- Bilal Ch
SET IDENTITY_INSERT FEE_RECORDS OFF;
GO

INSERT INTO FEE_CHALLANS (fee_id, challan_number, generated_date, expiry_date, is_paid)
VALUES
    (1, N'CH-1-3-202409010830', '2024-09-01', '2024-09-15', 1),  -- Ali: paid
    (2, N'CH-2-3-202409011000', '2024-09-01', '2024-09-15', 0),  -- Sara: unpaid
    (3, N'CH-3-3-202409010845', '2024-09-01', '2024-09-15', 1),  -- Usman: paid
    (4, N'CH-4-3-202409011015', '2024-09-01', '2024-09-15', 0),  -- Hina: partial
    (6, N'CH-6-3-202409010900', '2024-09-01', '2024-09-15', 1),  -- Fahad: paid
    (7, N'CH-7-3-202409010915', '2024-09-01', '2024-09-15', 1);  -- Bilal: paid
GO

-- ────────────────────────────────────────────────────────────────
-- 4.15 ANNOUNCEMENTS
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
-- 4.16 COURSE MATERIALS
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
-- 4.17 ACADEMIC REQUESTS
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
-- 4.18 FEEDBACK
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
-- 4.19 TIMETABLES
-- ────────────────────────────────────────────────────────────────
INSERT INTO TIMETABLES
    (offering_id, day_of_week, start_time, end_time, room_number)
VALUES
    (1,  N'Monday',    '08:30', '10:00', N'CS-101'),
    (1,  N'Wednesday', '08:30', '10:00', N'CS-101'),
    (4,  N'Tuesday',   '10:30', '12:00', N'CS-201'),
    (4,  N'Thursday',  '10:30', '12:00', N'CS-201'),
    (7,  N'Tuesday',   '09:00', '10:30', N'AI-201'),
    (7,  N'Thursday',  '09:00', '10:30', N'AI-201'),
    (11, N'Monday',    '08:00', '09:30', N'EG-101'),
    (11, N'Wednesday', '08:00', '09:30', N'EG-101'),
    (13, N'Monday',    '09:00', '10:30', N'MG-101'),
    (13, N'Wednesday', '09:00', '10:30', N'MG-101');
GO

-- ================================================================
-- PART 5: VERIFICATION QUERIES
-- Run these after loading to confirm everything is correct
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
--    MUST NOT show Engineering or Management courses
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

-- 8. All announcements (scoped correctly)
SELECT a.title,
       COALESCE(md.name, 'University-wide') AS scope,
       a.target_role,
       a.posted_at
FROM ANNOUNCEMENTS a
LEFT JOIN MAJOR_DEPARTMENTS md ON md.major_dept_id = a.major_dept_id
ORDER BY a.posted_at;
GO

PRINT '================================================================';
PRINT ' UMS_COMPLETE.sql finished with no errors.';
PRINT ' All 18 tables created, views and stored procedures ready.';
PRINT ' Dummy data loaded across all tables.';
PRINT '================================================================';
GO