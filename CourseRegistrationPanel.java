import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class CourseRegistrationPanel extends JPanel {

    // ── Sample course data: {code, name, credits} ─────────────────────────────
    private static final String[][] COURSES = {
        {"CS3009", "Software Engineering",         "3"},
        {"CS3010", "Database Systems",             "3"},
        {"CS3011", "Operating Systems",            "3"},
        {"CS3012", "Computer Networks",            "3"},
        {"CS3013", "Artificial Intelligence",      "3"},
        {"CS3014", "Web Technologies",             "3"},
        {"CS3015", "Data Structures & Algorithms", "4"},
        {"CS3016", "Human Computer Interaction",   "2"},
        {"CS3017", "Software Testing & QA",        "3"},
        {"CS3018", "Cloud Computing",              "2"},
    };

    private final List<String> registeredCourses = new ArrayList<>();

    private JComboBox<String> courseDropdown;
    private JLabel codeValueLabel;
    private JLabel creditsValueLabel;
    private JLabel statusLabel;
    private JPanel registeredListPanel;
    private JLabel countBadge;

    public CourseRegistrationPanel() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Course Registration");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Select a course from the dropdown and click Register to enroll");
        sub.setFont(AppTheme.bodyFont(13));
        sub.setForeground(AppTheme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel accent = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(AppTheme.GOLD);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            @Override public Dimension getPreferredSize() { return new Dimension(50, 3); }
            @Override public Dimension getMaximumSize()   { return new Dimension(50, 3); }
        };
        accent.setOpaque(false);
        accent.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(sub);
        titleBlock.add(Box.createVerticalStrut(10));
        titleBlock.add(accent);
        header.add(titleBlock, BorderLayout.WEST);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.weightx = 0.55;
        gbc.insets = new Insets(0, 0, 0, 14);
        body.add(buildRegistrationCard(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.45;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildEnrolledCard(), gbc);

        return body;
    }

    // ── Registration Card ─────────────────────────────────────────────────────
    private JPanel buildRegistrationCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JLabel sectionTitle = new JLabel("Select a Course");
        sectionTitle.setFont(AppTheme.headingFont(15));
        sectionTitle.setForeground(AppTheme.NAVY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sectionTitle);
        card.add(Box.createVerticalStrut(4));

        JLabel sectionSub = new JLabel("Choose from available courses this semester");
        sectionSub.setFont(AppTheme.bodyFont(12));
        sectionSub.setForeground(AppTheme.TEXT_MUTED);
        sectionSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sectionSub);
        card.add(Box.createVerticalStrut(20));

        addFieldLabel(card, "Available Courses");

        // Dropdown
        String[] items = new String[COURSES.length + 1];
        items[0] = "— Select a course —";
        for (int i = 0; i < COURSES.length; i++)
            items[i + 1] = COURSES[i][0] + "  |  " + COURSES[i][1];

        courseDropdown = new JComboBox<>(items);
        styleDropdown(courseDropdown);
        courseDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        courseDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        courseDropdown.addActionListener(e -> updateCourseInfo());
        card.add(courseDropdown);
        card.add(Box.createVerticalStrut(20));

        // Info tiles
        JPanel infoRow = new JPanel(new GridLayout(1, 2, 12, 0));
        infoRow.setOpaque(false);
        infoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        infoRow.add(buildInfoTile("Course Code",  "—", true));
        infoRow.add(buildInfoTile("Credit Hours", "—", false));
        card.add(infoRow);
        card.add(Box.createVerticalStrut(24));

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.headingFont(13));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(12));

        // Register button
        StyledButton registerBtn = new StyledButton("Register", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        registerBtn.addActionListener(e -> handleRegister());
        card.add(registerBtn);

        return card;
    }

    private JPanel buildInfoTile(String label, String value, boolean isCode) {
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.headingFont(10));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(AppTheme.headingFont(15));
        val.setForeground(AppTheme.DEEP_BLUE);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (isCode) codeValueLabel    = val;
        else        creditsValueLabel = val;

        tile.add(lbl);
        tile.add(Box.createVerticalStrut(4));
        tile.add(val);
        return tile;
    }

    // ── Enrolled Card ─────────────────────────────────────────────────────────
    private JPanel buildEnrolledCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        // Title row with count badge
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("Enrolled Courses");
        title.setFont(AppTheme.headingFont(15));
        title.setForeground(AppTheme.NAVY);

        countBadge = new JLabel("0") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.GOLD);
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        countBadge.setFont(AppTheme.headingFont(11));
        countBadge.setForeground(AppTheme.NAVY);
        countBadge.setHorizontalAlignment(SwingConstants.CENTER);
        countBadge.setPreferredSize(new Dimension(26, 26));
        countBadge.setOpaque(false);

        titleRow.add(title,      BorderLayout.WEST);
        titleRow.add(countBadge, BorderLayout.EAST);
        card.add(titleRow, BorderLayout.NORTH);

        // List
        registeredListPanel = new JPanel();
        registeredListPanel.setLayout(new BoxLayout(registeredListPanel, BoxLayout.Y_AXIS));
        registeredListPanel.setOpaque(false);

        JLabel emptyMsg = new JLabel("No courses registered yet.");
        emptyMsg.setFont(AppTheme.bodyFont(12));
        emptyMsg.setForeground(AppTheme.TEXT_MUTED);
        emptyMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        registeredListPanel.add(emptyMsg);

        JScrollPane scroll = new JScrollPane(registeredListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildEnrolledRow(String code, String name, String credits) {
        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(27, 130, 60));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(3, 8, 3, getHeight() - 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setLayout(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(AppTheme.headingFont(12));
        nameLbl.setForeground(AppTheme.TEXT_DARK);

        JLabel codeLbl = new JLabel(code + "  ·  " + credits + " credit hrs");
        codeLbl.setFont(AppTheme.bodyFont(11));
        codeLbl.setForeground(AppTheme.TEXT_MUTED);

        left.add(nameLbl);
        left.add(codeLbl);

        JLabel check = new JLabel("✅");
        check.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        row.add(left,  BorderLayout.CENTER);
        row.add(check, BorderLayout.EAST);
        return row;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────
    private void updateCourseInfo() {
        int idx = courseDropdown.getSelectedIndex();
        if (idx == 0) {
            codeValueLabel.setText("—");
            creditsValueLabel.setText("—");
        } else {
            codeValueLabel.setText(COURSES[idx - 1][0]);
            creditsValueLabel.setText(COURSES[idx - 1][2] + " hrs");
        }
    }

    private void handleRegister() {
        int idx = courseDropdown.getSelectedIndex();
        if (idx == 0) {
            showStatus("⚠  Please select a course first.", AppTheme.GOLD_DARK);
            return;
        }
        String code    = COURSES[idx - 1][0];
        String name    = COURSES[idx - 1][1];
        String credits = COURSES[idx - 1][2];

        if (registeredCourses.contains(code)) {
            showStatus("⚠  You are already registered in this course.", AppTheme.GOLD_DARK);
        } else {
            registeredCourses.add(code);
            showStatus("✅  " + name + " registered successfully!", new Color(27, 130, 60));
            addToEnrolledList(code, name, credits);
        }
    }

    private void addToEnrolledList(String code, String name, String credits) {
        if (registeredCourses.size() == 1) {
            registeredListPanel.removeAll();
            registeredListPanel.add(Box.createVerticalStrut(4));
        }
        registeredListPanel.add(buildEnrolledRow(code, name, credits));
        registeredListPanel.add(Box.createVerticalStrut(8));
        registeredListPanel.revalidate();
        registeredListPanel.repaint();
        countBadge.setText(String.valueOf(registeredCourses.size()));
        countBadge.repaint();
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
        javax.swing.Timer timer = new javax.swing.Timer(4000, e -> statusLabel.setText(" "));
        timer.setRepeats(false);
        timer.start();
    }

    private void addFieldLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(6));
    }

    private void styleDropdown(JComboBox<String> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }
}