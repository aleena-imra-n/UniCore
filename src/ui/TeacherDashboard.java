package ui;

import bl.DashboardStatsService;
import java.awt.*;

public class TeacherDashboard extends BaseDashboard {

    private final DashboardStatsService statsService = new DashboardStatsService();

    public TeacherDashboard(String username) {
        super(username, "Teacher", 1100, 680);
    }

    @Override
    protected void onMenuClick(String label) {
        contentArea.removeAll();
        contentArea.setBackground(AppTheme.PALE_BLUE);
        switch (label) {
            case "Home" ->
            	super.onMenuClick("Home");
            case "My Courses" ->
                contentArea.add(new TeacherCoursesPanel(username), BorderLayout.CENTER);
            case "Post Announcement" ->
                contentArea.add(new TeacherAnnouncementsPanel(username), BorderLayout.CENTER);
            case "Mark Attendance" ->
                contentArea.add(new MarkAttendancePanel(username), BorderLayout.CENTER);
            case "Upload Marks" ->
                contentArea.add(new UploadMarksPanel(username), BorderLayout.CENTER);
            default -> {
                javax.swing.JPanel placeholder = new javax.swing.JPanel(new java.awt.GridBagLayout());
                placeholder.setOpaque(false);
                javax.swing.JLabel msg = new javax.swing.JLabel(label + " — Coming in future sprint");
                msg.setFont(AppTheme.titleFont(18));
                msg.setForeground(AppTheme.MID_BLUE);
                placeholder.add(msg);
                contentArea.add(placeholder, BorderLayout.CENTER);
            }
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
            {"…", "Courses",  "#1565C0"},
            {"…", "Students", "#2E7D32"},
            {"…", "Pending",  "#C62828"},
        };
    }

    @Override
    protected String[][] fetchLiveStats() {
        return statsService.getTeacherStats(username);
    }
}