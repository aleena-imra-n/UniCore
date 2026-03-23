package ui;

public class AdminDashboard extends BaseDashboard {
    public AdminDashboard(String username) {
        super(username, "Admin", 1000, 640);
    }

    @Override
    protected String[][] getMenuItems() {
        return new String[][] {
            {"🏠", "Home"},
            {"👥", "Manage Users"},
            {"📚", "Manage Courses"},
            {"👨‍🏫", "Assign Teachers"},
            {"📅", "Manage Semesters"},
            {"💰", "Fee Records"},
            {"📄", "Generate Transcripts"},
            {"🗓️", "Create Timetables"},
            {"📣", "Send Announcements"},
            {"📊", "Reports"},
        };
    }

    @Override
    protected String[][] getDashboardStats() {
        return new String[][] {
            {"320", "Students", "#1565C0"},
            {"28",  "Teachers", "#2E7D32"},
            {"45",  "Courses",  "#F57C00"},
        };
    }
}
