package ui;

import bl.DashboardStatsService;
import java.awt.*;

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