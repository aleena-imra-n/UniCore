public class StudentDashboard extends BaseDashboard {
    public StudentDashboard(String username) {
        super(username, "Student", 1000, 640);
    }

    @Override
    protected String[][] getMenuItems() {
        return new String[][] {
            {"🏠", "Home"},
            {"📚", "My Courses"},
            {"📋", "Attendance"},
            {"📊", "Marks & Grades"},
            {"📄", "Transcript"},
            {"💰", "Fee Details"},
            {"📝", "Fee Challan"},
            {"📣", "Announcements"},
            {"📅", "Timetable"},
            {"✏️", "Submit Feedback"},
            {"🔄", "Course Withdrawal"},
        };
    }

    @Override
    protected String[][] getDashboardStats() {
        return new String[][] {
            {"5",   "Courses",  "#1565C0"},
            {"88%", "Attend.",  "#2E7D32"},
            {"3.4", "GPA",      "#F57C00"},
        };
    }
}
