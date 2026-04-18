package ui;

import bl.DashboardStatsService;
import java.awt.*;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;


public class AdminDashboard extends BaseDashboard {

    private final DashboardStatsService statsService = new DashboardStatsService();

    public AdminDashboard(String username) {
        super(username, "Admin", 1100, 680);
    }

    @Override
    protected void onMenuClick(String label) {
        contentArea.removeAll();
        contentArea.setBackground(AppTheme.PALE_BLUE);

        switch (label) {
            case "Home" -> {
                super.onMenuClick("Home");
            }
            case "Fee Records" -> {
                contentArea.add(new AdminFeePanel(), BorderLayout.CENTER);
            }
            case "Manage Users" -> {
                contentArea.add(new ManageUsersPanel(username), BorderLayout.CENTER);
            }
            case "Handle Requests" -> {
                contentArea.add(new AdminWithdrawalPanel(username), BorderLayout.CENTER);
            }
            case "Manage Courses" -> {
                contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new ManageCoursesPanel(username), BorderLayout.CENTER);
            }
            case "Assign Teachers" -> {
                contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new AssignTeachersPanel(username), BorderLayout.CENTER);
            }
            case "Manage Semesters" -> {
                contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new ManageSemestersPanel(username), BorderLayout.CENTER);
            }
            case "Send Announcements" -> {
            	contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new AdminAnnouncementsPanel(username), BorderLayout.CENTER);
            }
            case "Reports" -> {
            	contentArea.setBackground(AppTheme.PALE_BLUE);
                contentArea.add(new AdminReportsPanel(), BorderLayout.CENTER);
            }
           
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
            {"👥", "Manage Users"},
            {"📚", "Manage Courses"},
            {"👨‍🏫", "Assign Teachers"},
            {"📅", "Manage Semesters"},
            {"💰", "Fee Records"},
            {"📄", "Generate Transcripts"},
            {"🗓️", "Create Timetables"},
            {"📣", "Send Announcements"},
            {"📝", "Handle Requests"},
            {"📊", "Reports"},
        };
    }

    @Override
    protected String[][] getDashboardStats() {
        return new String[][] {
            {"…", "Students", "#1565C0"},
            {"…", "Teachers", "#2E7D32"},
            {"…", "Courses",  "#F57C00"},
        };
    }

    @Override
    protected String[][] fetchLiveStats() {
        return statsService.getAdminStats();
    }
}