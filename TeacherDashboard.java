public class TeacherDashboard extends BaseDashboard {
    public TeacherDashboard(String username) {
        super(username, "Teacher", 1000, 640);
    }

    @Override
    protected void onMenuClick(String label) {
        contentArea.removeAll();
        switch (label) {
            case "My Courses":
                contentArea.add(new TeacherCoursesPanel(username), java.awt.BorderLayout.CENTER);
                break;
            case "Post Announcement":
                contentArea.add(new TeacherAnnouncementsPanel(), java.awt.BorderLayout.CENTER);
                break;
            default:
                super.onMenuClick(label);
                return;
        }
        contentArea.revalidate();
        contentArea.repaint();
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