package ui;

import bl.WithdrawalService;
import bl.WithdrawalService.SubmitResult;
import model.EnrolledCourse;
import model.WithdrawalRequest;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * CourseWithdrawalPanel — Student UI for US-3.5b.
 *
 * Left column  — Submit a new withdrawal request:
 *   - Dropdown of currently active enrollments (pending ones excluded)
 *   - Reason text area
 *   - Submit button + status label
 *
 * Right column — History of submitted requests:
 *   - One card per request showing course, date, status badge, admin comment
 *
 * Integration in StudentDashboard:
 *   contentArea.add(new CourseWithdrawalPanel(username), BorderLayout.CENTER);
 */
public class CourseWithdrawalPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color AMBER_FG   = new Color(180, 100,  0);
    private static final Color AMBER_BG   = new Color(255, 248, 225);
    private static final Color GREEN_FG   = new Color( 21, 128,  61);
    private static final Color GREEN_BG   = new Color(220, 252, 231);
    private static final Color RED_FG     = new Color(198,  40,  40);
    private static final Color RED_BG     = new Color(255, 235, 238);
    private static final Color DIVIDER    = new Color(215, 228, 248);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String            username;
    private final WithdrawalService service;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JComboBox<EnrolledCourse> courseCombo;
    private JTextArea                 reasonArea;
    private JLabel                    statusLabel;
    private StyledButton              submitBtn;
    private JPanel                    historyPanel;

    // ─────────────────────────────────────────────────────────────────────────
    public CourseWithdrawalPanel(String username) {
        this(username, new WithdrawalService());
    }

    public CourseWithdrawalPanel(String username, WithdrawalService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildBody(),    BorderLayout.CENTER);

        loadData();
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

        JLabel title = new JLabel("Course Withdrawal");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Submit a withdrawal request for a current semester course");
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
    //  BODY — two-column split
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new GridLayout(1, 2, 24, 0));
        body.setOpaque(false);
        body.add(buildFormCard());
        body.add(buildHistoryScroll());
        return body;
    }

    // ── LEFT: submission form ─────────────────────────────────────────────────
    private JPanel buildFormCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JLabel formTitle = new JLabel("New Request");
        formTitle.setFont(AppTheme.headingFont(15));
        formTitle.setForeground(AppTheme.NAVY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formTitle);
        card.add(Box.createVerticalStrut(6));
        card.add(makeDivider());
        card.add(Box.createVerticalStrut(18));

        // Course dropdown
        addFieldLabel(card, "Select Course");
        courseCombo = new JComboBox<>();
        styleCombo(courseCombo);
        courseCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        courseCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(courseCombo);
        card.add(Box.createVerticalStrut(16));

        // Reason text area
        addFieldLabel(card, "Reason for Withdrawal");
        reasonArea = new JTextArea(5, 20);
        reasonArea.setFont(AppTheme.bodyFont(13));
        reasonArea.setForeground(AppTheme.TEXT_DARK);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        reasonScroll.setBorder(BorderFactory.createEmptyBorder());
        reasonScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        reasonScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(reasonScroll);
        card.add(Box.createVerticalStrut(6));

        // Char counter hint
        JLabel charHint = new JLabel("Max " + WithdrawalService.MAX_REASON_LENGTH + " characters");
        charHint.setFont(AppTheme.bodyFont(11));
        charHint.setForeground(AppTheme.TEXT_MUTED);
        charHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(charHint);
        card.add(Box.createVerticalStrut(16));

        // Warning note
        JPanel warnBox = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AMBER_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        warnBox.setOpaque(false);
        warnBox.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        warnBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        warnBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel warnIcon = new JLabel("⚠");
        warnIcon.setFont(AppTheme.bodyFont(14));
        warnIcon.setForeground(AMBER_FG);
        JLabel warnText = new JLabel("<html>Withdrawal requests are reviewed by an admin.<br>"
            + "Approval will remove you from the course.</html>");
        warnText.setFont(AppTheme.bodyFont(11));
        warnText.setForeground(AMBER_FG);
        warnBox.add(warnIcon, BorderLayout.WEST);
        warnBox.add(warnText, BorderLayout.CENTER);
        card.add(warnBox);
        card.add(Box.createVerticalStrut(18));

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.bodyFont(12));
        statusLabel.setForeground(AppTheme.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(6));

        // Submit button
        submitBtn = new StyledButton("Submit Request", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        submitBtn.addActionListener(e -> handleSubmit());
        card.add(submitBtn);

        return card;
    }

    // ── RIGHT: history scroll ─────────────────────────────────────────────────
    private JScrollPane buildHistoryScroll() {
        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setOpaque(false);

        JLabel loading = new JLabel("Loading requests…");
        loading.setFont(AppTheme.bodyFont(13));
        loading.setForeground(AppTheme.TEXT_MUTED);
        historyPanel.add(loading);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        // Column header for the history
        JPanel histHeader = new JPanel(new BorderLayout());
        histHeader.setOpaque(false);
        histHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel histTitle = new JLabel("My Requests");
        histTitle.setFont(AppTheme.headingFont(15));
        histTitle.setForeground(AppTheme.NAVY);
        histHeader.add(histTitle, BorderLayout.WEST);

        JPanel outerWrapper = new JPanel(new BorderLayout(0, 0));
        outerWrapper.setOpaque(false);
        outerWrapper.add(histHeader,           BorderLayout.NORTH);
        outerWrapper.add(wrapper,              BorderLayout.CENTER);

        wrapper.add(historyPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(outerWrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadData() {
        submitBtn.setEnabled(false);
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                boolean ok = service.initStudent(username);
                if (!ok) return null;
                List<EnrolledCourse>    active   = service.getActiveEnrollments();
                List<WithdrawalRequest> requests = service.getMyRequests();
                return new Object[]{ active, requests };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] result = get();
                    if (result == null) {
                        setStatus("Failed to load student account. Please re-login.", RED_FG);
                        return;
                    }
                    populateCombo((List<EnrolledCourse>) result[0]);
                    populateHistory((List<WithdrawalRequest>) result[1]);
                    submitBtn.setEnabled(true);
                } catch (Exception ex) {
                    setStatus("Error loading data: " + ex.getMessage(), RED_FG);
                }
            }
        }.execute();
    }

    private void populateCombo(List<EnrolledCourse> courses) {
        courseCombo.removeAllItems();
        courseCombo.addItem(null);   // placeholder
        courses.forEach(courseCombo::addItem);

        // Renderer to show placeholder text
        courseCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("— Select a course —");
                    setForeground(AppTheme.TEXT_MUTED);
                } else {
                    setText(value.toString());
                    setForeground(AppTheme.TEXT_DARK);
                }
                return this;
            }
        });

        if (courses.isEmpty()) {
            setStatus("No active courses available for withdrawal.", AppTheme.TEXT_MUTED);
            submitBtn.setEnabled(false);
        }
    }

    private void populateHistory(List<WithdrawalRequest> requests) {
        historyPanel.removeAll();
        if (requests.isEmpty()) {
            JLabel empty = new JLabel("No withdrawal requests submitted yet.");
            empty.setFont(AppTheme.bodyFont(13));
            empty.setForeground(AppTheme.TEXT_MUTED);
            historyPanel.add(empty);
        } else {
            for (WithdrawalRequest req : requests) {
                historyPanel.add(buildRequestCard(req));
                historyPanel.add(Box.createVerticalStrut(12));
            }
        }
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUBMIT HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    private void handleSubmit() {
        EnrolledCourse selected = (EnrolledCourse) courseCombo.getSelectedItem();
        String reason = reasonArea.getText();

        submitBtn.setEnabled(false);
        submitBtn.setText("Submitting…");
        statusLabel.setText(" ");

        new SwingWorker<SubmitResult, Void>() {
            @Override
            protected SubmitResult doInBackground() {
                return service.submitRequest(selected, reason);
            }

            @Override
            protected void done() {
                try {
                    SubmitResult result = get();
                    if (result.success()) {
                        setStatus("✓  " + result.message(), GREEN_FG);
                        reasonArea.setText("");
                        courseCombo.setSelectedIndex(0);
                        // Refresh both panels
                        refreshAfterSubmit();
                    } else {
                        setStatus("⚠  " + result.message(), RED_FG);
                        submitBtn.setEnabled(true);
                        submitBtn.setText("Submit Request");
                    }
                } catch (Exception ex) {
                    setStatus("⚠  Error: " + ex.getMessage(), RED_FG);
                    submitBtn.setEnabled(true);
                    submitBtn.setText("Submit Request");
                }
            }
        }.execute();
    }

    private void refreshAfterSubmit() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                return new Object[]{
                    service.getActiveEnrollments(),
                    service.getMyRequests()
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] r = get();
                    populateCombo((List<EnrolledCourse>) r[0]);
                    populateHistory((List<WithdrawalRequest>) r[1]);
                } catch (Exception ignored) {}
                submitBtn.setEnabled(true);
                submitBtn.setText("Submit Request");
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REQUEST CARD  (history panel)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildRequestCard(WithdrawalRequest req) {
        Color accentColor = req.isPending()  ? AMBER_FG
                          : req.isApproved() ? GREEN_FG
                          :                    RED_FG;

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Top row: course + status badge ───────────────────────────────────
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel courseLbl = new JLabel(req.getCourseLabel());
        courseLbl.setFont(AppTheme.headingFont(13));
        courseLbl.setForeground(AppTheme.NAVY);

        JLabel statusBadge = makeStatusBadge(req.getStatus());
        topRow.add(courseLbl,   BorderLayout.WEST);
        topRow.add(statusBadge, BorderLayout.EAST);

        // ── Middle: semester + date ───────────────────────────────────────────
        JLabel metaLbl = new JLabel(req.getSemesterName()
            + "  ·  Submitted: " + req.getFormattedRequestedAt());
        metaLbl.setFont(AppTheme.bodyFont(11));
        metaLbl.setForeground(AppTheme.TEXT_MUTED);

        // ── Reason ────────────────────────────────────────────────────────────
        JLabel reasonLbl = new JLabel("<html><b>Reason:</b> "
            + escapeHtml(req.getReason()) + "</html>");
        reasonLbl.setFont(AppTheme.bodyFont(12));
        reasonLbl.setForeground(AppTheme.TEXT_DARK);

        // ── Admin comment (if any) ────────────────────────────────────────────
        JPanel infoBlock = new JPanel();
        infoBlock.setOpaque(false);
        infoBlock.setLayout(new BoxLayout(infoBlock, BoxLayout.Y_AXIS));
        infoBlock.add(topRow);
        infoBlock.add(Box.createVerticalStrut(4));
        infoBlock.add(metaLbl);
        infoBlock.add(Box.createVerticalStrut(6));
        infoBlock.add(reasonLbl);

        if (req.getAdminComment() != null && !req.getAdminComment().isBlank()) {
            infoBlock.add(Box.createVerticalStrut(6));
            JLabel commentLbl = new JLabel("<html><b>Admin:</b> "
                + escapeHtml(req.getAdminComment()) + "</html>");
            commentLbl.setFont(AppTheme.bodyFont(12));
            commentLbl.setForeground(accentColor);
            infoBlock.add(commentLbl);
        }

        card.add(infoBlock, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  WIDGET HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel makeStatusBadge(String status) {
        Color bg, fg;
        String label;
        switch (status.toLowerCase()) {
            case "approved" -> { bg = GREEN_BG; fg = GREEN_FG; label = "✓  Approved"; }
            case "rejected" -> { bg = RED_BG;   fg = RED_FG;   label = "✗  Rejected"; }
            default         -> { bg = AMBER_BG; fg = AMBER_FG; label = "⏳  Pending"; }
        }
        JLabel lbl = new JLabel(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        lbl.setFont(AppTheme.headingFont(11));
        lbl.setForeground(fg);
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        lbl.setOpaque(false);
        return lbl;
    }

    private JPanel makeCard() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
    }

    private JPanel makeDivider() {
        JPanel d = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(DIVIDER);
                g.fillRect(0, 0, getWidth(), 1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 1); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 1); }
        };
        d.setOpaque(false);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        return d;
    }

    private void addFieldLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(6));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}