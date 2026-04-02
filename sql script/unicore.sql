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
