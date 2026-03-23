package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public abstract class BaseDashboard extends JFrame {

    protected String username;
    protected String role;
    protected JPanel contentArea;

    public BaseDashboard(String username, String role, int w, int h) {
        this.username = username;
        this.role = role;
        setTitle("UniCore — " + role + " Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(w, h);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.add(buildSidebar(), BorderLayout.WEST);
        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(AppTheme.PALE_BLUE);
        contentArea.add(buildWelcomePanel(), BorderLayout.CENTER);
        body.add(contentArea, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    // ── Top Bar ──────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, AppTheme.NAVY, getWidth(), 0, AppTheme.DEEP_BLUE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Gold bottom line
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 58));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // Left: logo + title
        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSide.setOpaque(false);

        JLabel logo = new JLabel("U") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.GOLD);
                g2.fillOval(0, 0, 36, 36);
                g2.setColor(AppTheme.NAVY);
                g2.setFont(AppTheme.titleFont(16));
                g2.drawString("U", 10, 24);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(36, 36); }
        };

        JLabel title = new JLabel("UniCore UMS");
        title.setFont(AppTheme.titleFont(18));
        title.setForeground(Color.WHITE);

        leftSide.add(logo);
        leftSide.add(title);
        bar.add(leftSide, BorderLayout.WEST);

        // Right: user info + logout
        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightSide.setOpaque(false);

        JLabel roleTag = new JLabel(role.toUpperCase()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.GOLD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        roleTag.setFont(AppTheme.headingFont(10));
        roleTag.setForeground(AppTheme.NAVY);
        roleTag.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        roleTag.setOpaque(false);

        JLabel userLabel = new JLabel("👤  " + username);
        userLabel.setFont(AppTheme.bodyFont(13));
        userLabel.setForeground(AppTheme.LIGHT_BLUE);

        StyledButton logoutBtn = new StyledButton("Logout", new Color(180, 40, 40), new Color(150, 20, 20));
        logoutBtn.setPreferredSize(new Dimension(90, 32));
        logoutBtn.setFont(AppTheme.headingFont(12));
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                new LoginScreen().setVisible(true);
            }
        });

        rightSide.add(roleTag);
        rightSide.add(userLabel);
        rightSide.add(logoutBtn);
        bar.add(rightSide, BorderLayout.EAST);
        return bar;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, AppTheme.DEEP_BLUE, 0, getHeight(), new Color(10, 35, 80));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JLabel navTitle = new JLabel("NAVIGATION");
        navTitle.setFont(AppTheme.headingFont(10));
        navTitle.setForeground(AppTheme.TEXT_MUTED);
        navTitle.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 0));
        navTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(navTitle);

        for (String[] item : getMenuItems()) {
            sidebar.add(makeSidebarItem(item[0], item[1]));
        }
        return sidebar;
    }

    private JPanel makeSidebarItem(String icon, String label) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10)) {
            boolean hovered = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                public void mouseClicked(MouseEvent e) { onMenuClick(label); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 18));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(AppTheme.GOLD);
                    g2.setStroke(new BasicStroke(3));
                    g2.drawLine(0, 0, 0, getHeight());
                }
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ico = new JLabel(icon);
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.bodyFont(13));
        lbl.setForeground(new Color(200, 220, 255));

        item.add(ico);
        item.add(lbl);
        return item;
    }

    // ── Welcome panel ─────────────────────────────────────────────────────────
    private JPanel buildWelcomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(30, getHeight() - 6, 30 + 60, getHeight() - 6);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        card.setPreferredSize(new Dimension(500, 260));
        card.setOpaque(false);

        JLabel greet = new JLabel("Hello, " + username + "! 👋");
        greet.setFont(AppTheme.titleFont(26));
        greet.setForeground(AppTheme.NAVY);
        greet.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(greet);
        card.add(Box.createVerticalStrut(8));

        JLabel roleInfo = new JLabel("You are logged in as " + role);
        roleInfo.setFont(AppTheme.bodyFont(14));
        roleInfo.setForeground(AppTheme.TEXT_MUTED);
        roleInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(roleInfo);
        card.add(Box.createVerticalStrut(20));

        JLabel prompt = new JLabel("Select an option from the sidebar to get started.");
        prompt.setFont(AppTheme.bodyFont(13));
        prompt.setForeground(AppTheme.MID_BLUE);
        prompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(prompt);
        card.add(Box.createVerticalStrut(30));

        // Stats row
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String[] stat : getDashboardStats()) {
            stats.add(makeStatCard(stat[0], stat[1], stat[2]));
        }
        card.add(stats);

        panel.add(card);
        return panel;
    }

    private JPanel makeStatCard(String value, String label, String color) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Color.decode(color));
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(12, getHeight() - 4, getWidth() - 12, getHeight() - 4);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));
        card.setPreferredSize(new Dimension(110, 72));
        card.setOpaque(false);

        JLabel val = new JLabel(value);
        val.setFont(AppTheme.titleFont(20));
        val.setForeground(AppTheme.NAVY);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.bodyFont(11));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(val);
        card.add(Box.createVerticalStrut(4));
        card.add(lbl);
        return card;
    }

    protected void onMenuClick(String label) {
        contentArea.removeAll();
        JPanel placeholder = new JPanel(new GridBagLayout());
        placeholder.setOpaque(false);
        JLabel msg = new JLabel(label + " — Coming in future sprint");
        msg.setFont(AppTheme.titleFont(18));
        msg.setForeground(AppTheme.MID_BLUE);
        placeholder.add(msg);
        contentArea.add(placeholder, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    // Subclasses provide these
    protected abstract String[][] getMenuItems();
    protected abstract String[][] getDashboardStats();
}
