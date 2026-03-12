import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TeacherAnnouncementsPanel extends JPanel {

    private static final String[] COURSES = {
        "— Select a course —",
        "CS3009  |  Software Engineering",
        "CS3011  |  Operating Systems",
        "CS3015  |  Data Structures & Algorithms",
    };

    private final List<String[]> announcements = new ArrayList<>();

    private JTextArea messageArea;
    private JComboBox<String> courseDropdown;
    private JLabel statusLabel;
    private JPanel announcementListPanel;
    private JLabel countBadge;

    public TeacherAnnouncementsPanel() {
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

        JLabel title = new JLabel("Post Announcement");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Create and post announcements for your courses");
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
        gbc.weightx = 0.52;
        gbc.insets = new Insets(0, 0, 0, 14);
        body.add(buildFormCard(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.48;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildPostedCard(), gbc);

        return body;
    }

    // ── Form Card ─────────────────────────────────────────────────────────────
    private JPanel buildFormCard() {
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

        // ── TOP: static fields (course dropdown) ──
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel sectionTitle = new JLabel("New Announcement");
        sectionTitle.setFont(AppTheme.headingFont(15));
        sectionTitle.setForeground(AppTheme.NAVY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(sectionTitle);
        topPanel.add(Box.createVerticalStrut(4));

        JLabel sectionSub = new JLabel("Fill in the details and select the course to post to");
        sectionSub.setFont(AppTheme.bodyFont(12));
        sectionSub.setForeground(AppTheme.TEXT_MUTED);
        sectionSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(sectionSub);
        topPanel.add(Box.createVerticalStrut(20));

        addFieldLabel(topPanel, "Course");
        courseDropdown = new JComboBox<>(COURSES);
        styleDropdown(courseDropdown);
        courseDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        courseDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        topPanel.add(courseDropdown);
        topPanel.add(Box.createVerticalStrut(16));

        addFieldLabel(topPanel, "Message");

        card.add(topPanel, BorderLayout.NORTH);

        // ── CENTER: message area expands to fill remaining space ──
        messageArea = new JTextArea();
        messageArea.setFont(AppTheme.bodyFont(13));
        messageArea.setForeground(AppTheme.TEXT_DARK);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2));

        card.add(messageScroll, BorderLayout.CENTER);

        // ── BOTTOM: quick insert + status + post button ──
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel quickLabel = new JLabel("Quick insert:");
        quickLabel.setFont(AppTheme.headingFont(11));
        quickLabel.setForeground(AppTheme.TEXT_MUTED);
        quickLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(quickLabel);
        bottomPanel.add(Box.createVerticalStrut(6));

        JPanel quickBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickBtns.setOpaque(false);
        quickBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String[] q : new String[][]{
            {"📅 Deadline",      "Please note the deadline for this assignment is "},
            {"❌ Cancellation",  "Class on [date] has been cancelled. "},
            {"📝 Exam Reminder", "Reminder: Exam is scheduled on [date]. Please be prepared. "},
        }) {
            quickBtns.add(makeQuickButton(q[0], q[1]));
        }
        bottomPanel.add(quickBtns);
        bottomPanel.add(Box.createVerticalStrut(12));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.headingFont(13));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(statusLabel);
        bottomPanel.add(Box.createVerticalStrut(8));

        StyledButton postBtn = new StyledButton("Post Announcement", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        postBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        postBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        postBtn.addActionListener(e -> handlePost());
        bottomPanel.add(postBtn);

        card.add(bottomPanel, BorderLayout.SOUTH);

        return card;
    }

    // ── Posted Announcements Card ─────────────────────────────────────────────
    private JPanel buildPostedCard() {
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

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("Posted Announcements");
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

        announcementListPanel = new JPanel();
        announcementListPanel.setLayout(new BoxLayout(announcementListPanel, BoxLayout.Y_AXIS));
        announcementListPanel.setOpaque(false);

        JLabel emptyMsg = new JLabel("No announcements posted yet.");
        emptyMsg.setFont(AppTheme.bodyFont(12));
        emptyMsg.setForeground(AppTheme.TEXT_MUTED);
        emptyMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        announcementListPanel.add(emptyMsg);

        JScrollPane scroll = new JScrollPane(announcementListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildAnnouncementRow(String message, String course, String time) {
        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(123, 31, 162));
                g2.setStroke(new BasicStroke(4f));
                g2.drawLine(4, 8, 4, getHeight() - 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        String preview = message.length() > 65 ? message.substring(0, 65) + "..." : message;
        JLabel msgLbl = new JLabel(preview);
        msgLbl.setFont(AppTheme.headingFont(12));
        msgLbl.setForeground(AppTheme.NAVY);
        msgLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel metaLbl = new JLabel(course + "  ·  " + time);
        metaLbl.setFont(AppTheme.bodyFont(10));
        metaLbl.setForeground(AppTheme.TEXT_MUTED);
        metaLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(msgLbl);
        row.add(Box.createVerticalStrut(5));
        row.add(metaLbl);

        return row;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────
    private void handlePost() {
        String message = messageArea.getText().trim();
        int courseIdx  = courseDropdown.getSelectedIndex();

        if (courseIdx == 0) {
            showStatus("⚠  Please select a course.", AppTheme.GOLD_DARK);
            return;
        }
        if (message.isEmpty()) {
            showStatus("⚠  Please enter a message.", AppTheme.GOLD_DARK);
            return;
        }

        String course = COURSES[courseIdx].split("  \\|  ")[0].trim();
        String time   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, h:mm a"));

        announcements.add(new String[]{message, course, time});
        addToPostedList(message, course, time);
        showStatus("✅  Announcement posted successfully!", new Color(27, 130, 60));

        messageArea.setText("");
        courseDropdown.setSelectedIndex(0);
    }

    private void addToPostedList(String message, String course, String time) {
        if (announcements.size() == 1) {
            announcementListPanel.removeAll();
            announcementListPanel.add(Box.createVerticalStrut(4));
        }
        announcementListPanel.add(buildAnnouncementRow(message, course, time));
        announcementListPanel.add(Box.createVerticalStrut(8));
        announcementListPanel.revalidate();
        announcementListPanel.repaint();
        countBadge.setText(String.valueOf(announcements.size()));
        countBadge.repaint();
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        javax.swing.Timer t = new javax.swing.Timer(4000, e -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JButton makeQuickButton(String label, String insert) {
        JButton btn = new JButton(label) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? AppTheme.LIGHT_BLUE : AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.bodyFont(11));
        btn.setForeground(AppTheme.DEEP_BLUE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> messageArea.append(insert));
        return btn;
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