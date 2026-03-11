public class TeacherDashboard extends BaseDashboard {
    public TeacherDashboard(String username) {
        super(username, "Teacher", 1000, 640);
    }

    @Override
    protected String[][] getMenuItems() {
        return new String[][] {
            {"🏠", "Home"},
            {"📚", "My Courses"},
            {"📋", "Mark Attendance"},
            {"📊", "Upload Marks"},
            {"📣", "Post Announcement"},
            {"📁", "Upload Materials"},
            {"💬", "Student Feedback"},
            {"📅", "My Timetable"},
            {"📝", "Handle Requests"},
        };
    }

    @Override
    protected String[][] getDashboardStats() {
        return new String[][] {
            {"3",  "Courses",  "#1565C0"},
            {"72", "Students", "#2E7D32"},
            {"2",  "Pending",  "#C62828"},
        };
    }
}
