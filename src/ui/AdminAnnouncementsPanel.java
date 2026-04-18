package ui;

import bl.AdminAnnouncementService;
import bl.AdminAnnouncementService.ActionResult;
import bl.AdminAnnouncementService.PostResult;
import model.AdminAnnouncementItem;
import model.AdminAnnouncementItem.Scope;
import model.DeptOption;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AdminAnnouncementsPanel  —  Admin Send Announcements
 *
 * Left card  : compose form (scope toggle, dept dropdown, target audience,
 *              title, message, quick-insert buttons, Post button)
 * Right card : scrollable list of announcements this admin has posted,
 *              each with a scope badge and a Delete button
 *
 * Integration in AdminDashboard.onMenuClick():
 *   case "Send Announcement":
 *       contentArea.removeAll();
 *       contentArea.add(new AdminAnnouncementsPanel(username), BorderLayout.CENTER);
 *       contentArea.revalidate(); contentArea.repaint();
 *       break;
 */
public class AdminAnnouncementsPanel extends JPanel {

    private static final DateTimeFormatter ROW_FMT =
        DateTimeFormatter.ofPattern("MMM d, h:mm a");

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG_PANEL   = new Color(245, 249, 255);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BORDER_CLR = new Color(200, 220, 240);
    private static final Color GREEN_FG   = new Color(46, 125, 50);
    private static final Color RED_FG     = new Color(198, 40, 40);
    private static final Color AMBER_FG   = new Color(180, 100, 0);
    private static final Color PURPLE_FG  = new Color(106, 27, 154);
    private static final Color PURPLE_BG  = new Color(243, 229, 251);
    private static final Color NAVY_BG    = new Color(232, 244, 253);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String                  adminUsername;
    private final AdminAnnouncementService service;

    // ── Data ──────────────────────────────────────────────────────────────────
    private List<DeptOption> depts;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JRadioButton       radioUniversity, radioDept;
    private JComboBox<DeptOption>   deptCombo;
    private JLabel             deptLabel;
    private JComboBox<String>  audienceCombo;
    private JTextField         titleField;
    private JTextArea          messageArea;
    private JLabel             charCountLabel;
    private JLabel             statusLabel;
    private JPanel             postedListPanel;
    private JLabel             countBadge;
    private int                postedCount = 0;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public AdminAnnouncementsPanel(String adminUsername) {
        this(adminUsername, new AdminAnnouncementService());
    }

    public AdminAnnouncementsPanel(String adminUsername, AdminAnnouncementService svc) {
        this.adminUsername = adminUsername;
        this.service       = svc;

        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        if (!service.init(adminUsername)) {
            showStatus("Failed to load admin account. Please re-login.", RED_FG);
        } else {
            depts = service.loadDepts();
            depts.forEach(deptCombo::addItem);
            loadPostedAnnouncements();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Send Announcement");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel(
            "Post university-wide or department-targeted announcements");
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
        titleBlock.add(Box.createVerticalStrut(5));
        titleBlock.add(sub);
        titleBlock.add(Box.createVerticalStrut(8));
        titleBlock.add(accent);
        header.add(titleBlock, BorderLayout.WEST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BODY  (left form | right posted list)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0; gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.weightx = 0.54;
        gbc.insets = new Insets(0, 0, 0, 14);
        body.add(buildFormCard(), gbc);

        gbc.gridx = 1; gbc.weightx = 0.46;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildPostedCard(), gbc);

        return body;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LEFT: COMPOSE FORM CARD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFormCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(26, 28, 20, 28));

        // ── TOP: scope + dept + audience + title ──────────────────────────────
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Section heading
        addCardLabel(topPanel, "New Announcement", AppTheme.NAVY, 15, true);
        topPanel.add(Box.createVerticalStrut(2));
        addCardLabel(topPanel, "Select scope, audience, and compose your message",
            AppTheme.TEXT_MUTED, 12, false);
        topPanel.add(Box.createVerticalStrut(18));

        // ── Scope toggle (radio buttons) ──────────────────────────────────────
        addFieldLabel(topPanel, "Scope");
        ButtonGroup scopeGroup = new ButtonGroup();
        radioUniversity = new JRadioButton("University-wide");
        radioDept       = new JRadioButton("Department only");
        for (JRadioButton rb : new JRadioButton[]{ radioUniversity, radioDept }) {
            rb.setFont(AppTheme.bodyFont(13));
            rb.setForeground(AppTheme.TEXT_DARK);
            rb.setOpaque(false);
            rb.setAlignmentX(Component.LEFT_ALIGNMENT);
            scopeGroup.add(rb);
        }
        radioUniversity.setSelected(true);

        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        radioRow.setOpaque(false);
        radioRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioRow.add(radioUniversity);
        radioRow.add(radioDept);
        topPanel.add(radioRow);
        topPanel.add(Box.createVerticalStrut(12));

        // ── Department dropdown (hidden until "Department only" selected) ──────
        deptLabel = new JLabel("Target Department");
        deptLabel.setFont(AppTheme.headingFont(12));
        deptLabel.setForeground(AppTheme.DEEP_BLUE);
        deptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        deptLabel.setVisible(false);

        deptCombo = new JComboBox<>();
        styleCombo(deptCombo);
        deptCombo.setVisible(false);

        topPanel.add(deptLabel);
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(deptCombo);

        // Show/hide dept combo when scope changes
        ActionListener scopeToggle = e -> {
            boolean deptMode = radioDept.isSelected();
            deptLabel.setVisible(deptMode);
            deptCombo.setVisible(deptMode);
        };
        radioUniversity.addActionListener(scopeToggle);
        radioDept.addActionListener(scopeToggle);

        topPanel.add(Box.createVerticalStrut(14));

        // ── Target audience ───────────────────────────────────────────────────
        addFieldLabel(topPanel, "Target Audience");
        audienceCombo = new JComboBox<>(new String[]{"All Users", "Students Only", "Teachers Only"});
        styleCombo(audienceCombo);
        topPanel.add(audienceCombo);
        topPanel.add(Box.createVerticalStrut(14));

        // ── Title ─────────────────────────────────────────────────────────────
        addFieldLabel(topPanel, "Title");
        titleField = new StyledTextField("Enter announcement title…", 25);
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(titleField);
        topPanel.add(Box.createVerticalStrut(12));

        addFieldLabel(topPanel, "Message");
        card.add(topPanel, BorderLayout.NORTH);

        // ── CENTRE: expanding message textarea ────────────────────────────────
        messageArea = new JTextArea();
        messageArea.setFont(AppTheme.bodyFont(13));
        messageArea.setForeground(AppTheme.TEXT_DARK);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        charCountLabel = new JLabel("0 / " + AdminAnnouncementService.MAX_CONTENT_LENGTH);
        charCountLabel.setFont(AppTheme.bodyFont(10));
        charCountLabel.setForeground(AppTheme.TEXT_MUTED);

        messageArea.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { updateCharCount(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { updateCharCount(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCharCount(); }
            });

        JScrollPane msgScroll = new JScrollPane(messageArea);
        msgScroll.setBorder(BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2));

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 4));
        centerWrapper.setOpaque(false);
        centerWrapper.add(msgScroll,  BorderLayout.CENTER);
        centerWrapper.add(charCountLabel, BorderLayout.SOUTH);
        card.add(centerWrapper, BorderLayout.CENTER);

        // ── BOTTOM: quick-insert + status + Post button ───────────────────────
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
            {"📅 Deadline",     "Please note the deadline is "},
            {"🎓 Exam Notice",  "All students are reminded that exams are scheduled on [date]. "},
            {"🏖 Holiday",      "The university will remain closed on [date]. "},
            {"📢 Event",        "You are invited to attend [event name] on [date] at [venue]. "},
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
    //  RIGHT: POSTED ANNOUNCEMENTS CARD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPostedCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(26, 28, 20, 28));

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

        postedListPanel = new JPanel();
        postedListPanel.setLayout(new BoxLayout(postedListPanel, BoxLayout.Y_AXIS));
        postedListPanel.setOpaque(false);
        showEmptyPostedMessage();

        JScrollPane scroll = new JScrollPane(postedListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildPostedRow(AdminAnnouncementItem item) {
        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Scope-coloured left accent bar
                g2.setColor(item.getScope() == Scope.UNIVERSITY
                    ? AppTheme.MID_BLUE : PURPLE_FG);
                g2.setStroke(new java.awt.BasicStroke(4f));
                g2.drawLine(4, 8, 4, getHeight() - 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setLayout(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left: title + preview + meta
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        String titleText = item.getTitle();
        if (titleText.length() > 45) titleText = titleText.substring(0, 45) + "…";
        JLabel titleLbl = new JLabel(titleText);
        titleLbl.setFont(AppTheme.headingFont(12));
        titleLbl.setForeground(AppTheme.NAVY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Scope badge
        JLabel scopeBadge = new JLabel(item.getScopeLabel());
        scopeBadge.setFont(AppTheme.bodyFont(10));
        scopeBadge.setForeground(item.getScope() == Scope.UNIVERSITY
            ? AppTheme.MID_BLUE : PURPLE_FG);
        scopeBadge.setOpaque(true);
        scopeBadge.setBackground(item.getScope() == Scope.UNIVERSITY
            ? NAVY_BG : PURPLE_BG);
        scopeBadge.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(6, new Color(180, 180, 220), 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        scopeBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Time
        JLabel metaLbl = new JLabel(
            item.getPostedAt() != null ? item.getPostedAt().format(ROW_FMT) : "");
        metaLbl.setFont(AppTheme.bodyFont(10));
        metaLbl.setForeground(AppTheme.TEXT_MUTED);
        metaLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(titleLbl);
        left.add(Box.createVerticalStrut(3));
        left.add(scopeBadge);
        left.add(Box.createVerticalStrut(3));
        left.add(metaLbl);

        // Right: Delete button
        JButton delBtn = new JButton("✕");
        delBtn.setFont(AppTheme.headingFont(11));
        delBtn.setForeground(RED_FG);
        delBtn.setBackground(new Color(255, 235, 238));
        delBtn.setFocusPainted(false);
        delBtn.setBorderPainted(false);
        delBtn.setPreferredSize(new Dimension(32, 32));
        delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delBtn.setToolTipText("Remove this announcement");
        delBtn.addActionListener(e -> onDeleteClicked(item, row));

        row.add(left,   BorderLayout.CENTER);
        row.add(delBtn, BorderLayout.EAST);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EVENT HANDLERS
    // ─────────────────────────────────────────────────────────────────────────
    private void onPostClicked() {
        Scope scope = radioUniversity.isSelected() ? Scope.UNIVERSITY : Scope.DEPARTMENT;
        DeptOption selDept = (DeptOption) deptCombo.getSelectedItem();
        Integer deptId = (scope == Scope.DEPARTMENT && selDept != null)
            ? selDept.getMajorDeptId() : null;

        String audienceStr = switch (audienceCombo.getSelectedIndex()) {
            case 1  -> "student";
            case 2  -> "teacher";
            default -> "all";
        };

        PostResult result = service.post(
            scope, deptId,
            titleField.getText(),
            messageArea.getText(),
            audienceStr);

        if (result.success()) {
            showStatus("✅  " + result.message(), GREEN_FG);
            appendPostedRow(result.created());
            titleField.setText("");
            messageArea.setText("");
            audienceCombo.setSelectedIndex(0);
            radioUniversity.setSelected(true);
            deptLabel.setVisible(false);
            deptCombo.setVisible(false);
        } else {
            showStatus("⚠  " + result.message(), AMBER_FG);
        }
    }

    private void onDeleteClicked(AdminAnnouncementItem item, JPanel row) {
        int choice = JOptionPane.showConfirmDialog(this,
            "<html>Remove announcement:<br><b>" + item.getTitle() + "</b>?<br>"
                + "It will disappear from all student and teacher views.</html>",
            "Confirm Remove", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        ActionResult result = service.deactivate(
            item.getAnnouncementId(), choice == JOptionPane.YES_OPTION);

        if (result.success()) {
            postedListPanel.remove(row);
            // Remove the spacer immediately after
            Component[] all = postedListPanel.getComponents();
            for (int i = 0; i < all.length; i++) {
                if (all[i] instanceof Box.Filler) {
                    postedListPanel.remove(all[i]);
                    break;
                }
            }
            postedListPanel.revalidate();
            postedListPanel.repaint();
            postedCount = Math.max(0, postedCount - 1);
            countBadge.setText(String.valueOf(postedCount));
            showStatus("✅  " + result.message(), GREEN_FG);
            if (postedCount == 0) showEmptyPostedMessage();
        } else if (choice == JOptionPane.YES_OPTION) {
            showStatus("✖  " + result.message(), RED_FG);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadPostedAnnouncements() {
        List<AdminAnnouncementItem> items = service.loadPosted();
        if (items.isEmpty()) { showEmptyPostedMessage(); return; }

        postedListPanel.removeAll();
        postedListPanel.add(Box.createVerticalStrut(4));
        items.forEach(item -> {
            postedListPanel.add(buildPostedRow(item));
            postedListPanel.add(Box.createVerticalStrut(8));
        });
        postedListPanel.revalidate();
        postedListPanel.repaint();
        postedCount = items.size();
        countBadge.setText(String.valueOf(postedCount));
    }

    private void appendPostedRow(AdminAnnouncementItem item) {
        if (postedCount == 0) {
            postedListPanel.removeAll();
            postedListPanel.add(Box.createVerticalStrut(4));
        }
        // Insert newest at top (index 1 = after the top strut)
        postedListPanel.add(buildPostedRow(item), 1);
        postedListPanel.add(Box.createVerticalStrut(8), 2);
        postedListPanel.revalidate();
        postedListPanel.repaint();
        postedCount++;
        countBadge.setText(String.valueOf(postedCount));
    }

    private void showEmptyPostedMessage() {
        postedListPanel.removeAll();
        JLabel empty = new JLabel("No announcements posted yet.");
        empty.setFont(AppTheme.bodyFont(12));
        empty.setForeground(AppTheme.TEXT_MUTED);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        postedListPanel.add(empty);
        postedListPanel.revalidate();
        postedListPanel.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void updateCharCount() {
        int len = messageArea.getText().length();
        int max = AdminAnnouncementService.MAX_CONTENT_LENGTH;
        charCountLabel.setText(len + " / " + max);
        charCountLabel.setForeground(len > max ? RED_FG : AppTheme.TEXT_MUTED);
    }

    private void showStatus(String msg, Color color) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        javax.swing.Timer t = new javax.swing.Timer(5000, e -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

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
                g2.setColor(BG_CARD);
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
        panel.add(Box.createVerticalStrut(5));
    }

    private void addCardLabel(JPanel panel, String text, Color color, int size, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(bold ? AppTheme.headingFont(size) : AppTheme.bodyFont(size));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }
}
