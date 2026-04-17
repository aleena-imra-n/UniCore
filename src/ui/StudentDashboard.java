package ui;

import javax.swing.*;
import java.awt.*;

public class StudentDashboard extends BaseDashboard {
    public StudentDashboard(String username) {
        super(username, "Student", 1100, 680);
    }

    @Override
    protected void onMenuClick(String label) {
        contentArea.removeAll();
        switch (label) {
            case "My Courses" -> {
                contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new CourseRegistrationPanel(username), BorderLayout.CENTER);
            }
            case "Announcements" -> {
                contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new StudentAnnouncementsPanel(username), BorderLayout.CENTER);
            }
            case "Attendance" -> {
                contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new StudentAttendancePanel(username), BorderLayout.CENTER);
            }
            case "Marks & Grades" -> {
            	contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new StudentMarksPanel(username), BorderLayout.CENTER);
                
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
        return new String[][] {
            {"5",   "Courses",  "#1565C0"},
            {"88%", "Attend.",  "#2E7D32"},
            {"3.4", "GPA",      "#F57C00"},
        };
    }
}