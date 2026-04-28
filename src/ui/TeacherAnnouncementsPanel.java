package ui;

import bl.AnnouncementService;
import bl.AnnouncementService.Outcome;
import bl.AnnouncementService.PostResult;
import model.AnnouncementItem;
import model.TeacherOfferingItem;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TeacherAnnouncementsPanel — Pure UI layer.
 *
 * What this class does:
 *   - Renders the form card (course dropdown, title field, message area,
 *     quick-insert buttons, post button)
 *   - Renders the posted-announcements card (scrollable list + count badge)
 *   - Delegates ALL validation and DB operations to AnnouncementService
 *
 * What this class does NOT do:
 *   - No SQL
 *   - No business rules (blank checks, length limit, etc.)
 *   - No direct DAO calls
 *
 * Integration in TeacherDashboard:
 *   contentArea.add(new TeacherAnnouncementsPanel(username), BorderLayout.CENTER);
 */
public class TeacherAnnouncementsPanel extends JPanel {

    private static final DateTimeFormatter ROW_FMT =
        DateTimeFormatter.ofPattern("MMM d, h:mm a");

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String              username;
    private final AnnouncementService service;

    // ── UI component refs ─────────────────────────────────────────────────────
    private JComboBox<TeacherOfferingItem> courseDropdown;
    private JTextField                     titleField;
    private JTextArea                      messageArea;
    private JLabel                         charCountLabel;
    private JLabel                         statusLabel;
    private JPanel                         announcementListPanel;
    private JLabel                         countBadge;

    // ── Running posted count (for badge) ─────────────────────────────────────
    private int postedCount = 0;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public TeacherAnnouncementsPanel(String username) {
        this.username = username;
        this.service  = new AnnouncementService();
        init();
    }

    /** Injection constructor for tests or custom service wiring. */
    public TeacherAnnouncementsPanel(String username, AnnouncementService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        if (!service.init(username)) {
            showStatus("Failed to load teacher account. Please re-login.", Color.RED);
        } else {
            loadOfferings();
            loadPostedAnnouncements();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    //  BODY: left form card + right posted list card
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.gridy   = 0;
        gbc.weighty = 1.0;

        gbc.gridx   = 0;
        gbc.weightx = 0.52;
        gbc.insets  = new Insets(0, 0, 0, 14);
        body.add(buildFormCard(), gbc);

        gbc.gridx   = 1;
        gbc.weightx = 0.48;
        gbc.insets  = new Insets(0, 0, 0, 0);
        body.add(buildPostedCard(), gbc);

        return body;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LEFT CARD: form
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFormCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        // ── TOP: course dropdown + title field ────────────────────────────────
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

        // Course dropdown
        addFieldLabel(topPanel, "Course");
        courseDropdown = new JComboBox<>();
        courseDropdown.addItem(null); // placeholder
        courseDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "— Select a course —" : value.toString());
                return this;
            }
        });
        styleDropdown(courseDropdown);
        courseDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        courseDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        topPanel.add(courseDropdown);
        topPanel.add(Box.createVerticalStrut(16));

        // Title field (new — was missing in original)
        addFieldLabel(topPanel, "Title");
        titleField = new JTextField();
        titleField.setFont(AppTheme.bodyFont(13));
        titleField.setForeground(AppTheme.TEXT_DARK);
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        topPanel.add(titleField);
        topPanel.add(Box.createVerticalStrut(16));

        addFieldLabel(topPanel, "Message");
        card.add(topPanel, BorderLayout.NORTH);

        // ── CENTRE: expanding message text area ───────────────────────────────
        messageArea = new JTextArea();
        messageArea.setFont(AppTheme.bodyFont(13));
        messageArea.setForeground(AppTheme.TEXT_DARK);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // Live character counter
        charCountLabel = new JLabel("0 / " + AnnouncementService.MAX_MESSAGE_LENGTH);
        charCountLabel.setFont(AppTheme.bodyFont(10));
        charCountLabel.setForeground(AppTheme.TEXT_MUTED);

        messageArea.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { updateCharCount(); }
                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { updateCharCount(); }
                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCharCount(); }
            });

        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2));
        messageScroll.setPreferredSize(new Dimension(400, 120)); 

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 4));
        centerWrapper.setOpaque(false);
        centerWrapper.add(messageScroll,  BorderLayout.CENTER);
        centerWrapper.add(charCountLabel, BorderLayout.SOUTH);
        card.add(centerWrapper, BorderLayout.CENTER);

        // ── BOTTOM: quick-insert buttons + status + post button ───────────────
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

        StyledButton postBtn = new StyledButton("Post Announcement",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        postBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        postBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        postBtn.addActionListener(e -> onPostClicked());
        bottomPanel.add(postBtn);

        card.add(bottomPanel, BorderLayout.SOUTH);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RIGHT CARD: posted announcements list
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPostedCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        // Title row + count badge
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("Posted Announcements");
        title.setFont(AppTheme.headingFont(15));
        title.setForeground(AppTheme.NAVY);

        countBadge = new JLabel("0") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
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
        showEmptyMessage();

        JScrollPane scroll = new JScrollPane(announcementListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildAnnouncementRow(AnnouncementItem item) {
        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
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
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Title (bold, truncated if long)
        String titleText = item.getTitle();
        if (titleText.length() > 55) titleText = titleText.substring(0, 55) + "…";
        JLabel titleLbl = new JLabel(titleText);
        titleLbl.setFont(AppTheme.headingFont(12));
        titleLbl.setForeground(AppTheme.NAVY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Preview of content body
        String preview = item.getContent();
        if (preview.length() > 60) preview = preview.substring(0, 60) + "…";
        JLabel previewLbl = new JLabel(preview);
        previewLbl.setFont(AppTheme.bodyFont(11));
        previewLbl.setForeground(AppTheme.TEXT_DARK);
        previewLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Meta: course + timestamp
        String time = item.getPostedAt().format(ROW_FMT);
        JLabel metaLbl = new JLabel(item.getCourseCode() + "  ·  " + time);
        metaLbl.setFont(AppTheme.bodyFont(10));
        metaLbl.setForeground(AppTheme.TEXT_MUTED);
        metaLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(titleLbl);
        row.add(Box.createVerticalStrut(2));
        row.add(previewLbl);
        row.add(Box.createVerticalStrut(4));
        row.add(metaLbl);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EVENT HANDLER
    // ─────────────────────────────────────────────────────────────────────────

    /** Called when the teacher clicks "Post Announcement". */
    private void onPostClicked() {
        TeacherOfferingItem selectedOffering =
            (TeacherOfferingItem) courseDropdown.getSelectedItem();
        String title   = titleField.getText();
        String message = messageArea.getText();

        PostResult result = service.post(selectedOffering, title, message);

        switch (result.outcome()) {
            case NO_COURSE,
                 NO_TITLE,
                 MESSAGE_TOO_LONG -> showStatus("⚠  " + result.message(), AppTheme.GOLD_DARK);
            case ERROR            -> showStatus("✖  " + result.message(), Color.RED);
            case SUCCESS          -> {
                showStatus("✅  " + result.message(), new Color(27, 130, 60));
                appendAnnouncementRow(result.announcement());
                // Reset form
                titleField.setText("");
                messageArea.setText("");
                courseDropdown.setSelectedIndex(0);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────

    private void loadOfferings() {
        while (courseDropdown.getItemCount() > 1) courseDropdown.removeItemAt(1);
        service.getOfferings().forEach(courseDropdown::addItem);
    }

    private void loadPostedAnnouncements() {
        List<AnnouncementItem> items = service.getPostedAnnouncements();
        if (items.isEmpty()) {
            showEmptyMessage();
            return;
        }
        announcementListPanel.removeAll();
        announcementListPanel.add(Box.createVerticalStrut(4));
        items.forEach(item -> {
            announcementListPanel.add(buildAnnouncementRow(item));
            announcementListPanel.add(Box.createVerticalStrut(8));
        });
        announcementListPanel.revalidate();
        announcementListPanel.repaint();
        postedCount = items.size();
        updateBadge();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RENDER HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Prepends a newly posted row at the top of the list (newest first). */
    private void appendAnnouncementRow(AnnouncementItem item) {
        if (postedCount == 0) {
            announcementListPanel.removeAll();
            announcementListPanel.add(Box.createVerticalStrut(4));
        }
        // Insert after the initial strut so newest appears at top
        announcementListPanel.add(buildAnnouncementRow(item), 1);
        announcementListPanel.add(Box.createVerticalStrut(8), 2);
        announcementListPanel.revalidate();
        announcementListPanel.repaint();
        postedCount++;
        updateBadge();
    }

    private void showEmptyMessage() {
        announcementListPanel.removeAll();
        JLabel emptyMsg = new JLabel("No announcements posted yet.");
        emptyMsg.setFont(AppTheme.bodyFont(12));
        emptyMsg.setForeground(AppTheme.TEXT_MUTED);
        emptyMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        announcementListPanel.add(emptyMsg);
        announcementListPanel.revalidate();
        announcementListPanel.repaint();
        postedCount = 0;
        updateBadge();
    }

    private void updateCharCount() {
        int len = messageArea.getText().length();
        int max = AnnouncementService.MAX_MESSAGE_LENGTH;
        charCountLabel.setText(len + " / " + max);
        charCountLabel.setForeground(len > max ? Color.RED : AppTheme.TEXT_MUTED);
    }

    private void updateBadge() {
        countBadge.setText(String.valueOf(postedCount));
        countBadge.repaint();
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        javax.swing.Timer t = new javax.swing.Timer(4000, e -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  WIDGET HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private JButton makeQuickButton(String label, String insert) {
        JButton btn = new JButton(label) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
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

    private JPanel roundedCard() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
    }

    private void addFieldLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(6));
    }

    private void styleDropdown(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }
}
