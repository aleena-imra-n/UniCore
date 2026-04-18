package ui;

import bl.DashboardStatsService;
import javax.swing.*;
import java.awt.*;

public class StudentDashboard extends BaseDashboard {

    private final DashboardStatsService statsService = new DashboardStatsService();

    public StudentDashboard(String username) {
        super(username, "Student", 1100, 680);
    }

    @Override
    protected void onMenuClick(String label) {
        contentArea.removeAll();
        contentArea.setBackground(AppTheme.PALE_BLUE);
        switch (label) {
            case "Home" -> {
            	super.onMenuClick("Home");
            }
            case "My Courses" -> {
                contentArea.add(new CourseRegistrationPanel(username), BorderLayout.CENTER);
            }
            case "Announcements" -> {
                contentArea.add(new StudentAnnouncementsPanel(username), BorderLayout.CENTER);
            }
            case "Attendance" -> {
                contentArea.add(new StudentAttendancePanel(username), BorderLayout.CENTER);
            }
            case "Marks & Grades" -> {
                contentArea.add(new StudentMarksPanel(username), BorderLayout.CENTER);
            }
            case "Fee Challan" ->{
            	contentArea.add(new FeeChallanPanel(username), BorderLayout.CENTER);
            }
            case "Fee Details" ->{
            	contentArea.add(new FeeDetailsPanel(username), BorderLayout.CENTER);
            }
            case "Submit Feedback" ->{
            	contentArea.add(new SubmitFeedbackPanel(username), BorderLayout.CENTER);
            }
            default -> {
                JPanel placeholder = new JPanel(new GridBagLayout());
                placeholder.setOpaque(false);
                JLabel msg = new JLabel(label + " — Coming in future sprint");
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
        // Shown immediately — replaced by live values once DB loads
        return new String[][] {
            {"…", "Courses",  "#1565C0"},
            {"…", "Attend.",  "#2E7D32"},
            {"…", "Marks",    "#F57C00"},
        };
    }

    @Override
    protected String[][] fetchLiveStats() {
        return statsService.getStudentStats(username);
    }
}

