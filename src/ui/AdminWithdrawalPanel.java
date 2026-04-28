package ui;

import bl.WithdrawalService;
import bl.WithdrawalService.ReviewResult;
import model.WithdrawalRequest;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * AdminWithdrawalPanel — Admin UI for US-3.5c.
 *
 * Displays all pending course withdrawal requests from students.
 * For each request the admin can:
 *   - Read the student name, roll number, course, semester, and reason
 *   - Optionally type an admin comment
 *   - Click Approve or Reject
 *
 * Approving calls SP_ReviewWithdrawal with decision='approved',
 * which also flips the enrollment status to 'withdrawn'.
 *
 * Integration in AdminDashboard:
 *   contentArea.add(new AdminWithdrawalPanel(username), BorderLayout.CENTER);
 */
public class AdminWithdrawalPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color AMBER_FG  = new Color(180, 100,   0);
    private static final Color AMBER_BG  = new Color(255, 248, 225);
    private static final Color GREEN_FG  = new Color( 21, 128,  61);
    private static final Color GREEN_BG  = new Color(220, 252, 231);
    private static final Color RED_FG    = new Color(198,  40,  40);
    private static final Color RED_BG    = new Color(255, 235, 238);
    private static final Color DIVIDER   = new Color(215, 228, 248);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String            username;
    private final WithdrawalService service;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JLabel pendingCountLabel;
    private JPanel requestsPanel;

    // ─────────────────────────────────────────────────────────────────────────
    public AdminWithdrawalPanel(String username) {
        this(username, new WithdrawalService());
    }

    public AdminWithdrawalPanel(String username, WithdrawalService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScroll(), BorderLayout.CENTER);

        loadRequests();
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

        JLabel title = new JLabel("Withdrawal Requests");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Review and action pending student course withdrawal requests");
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

        // Pending count badge (top-right)
        pendingCountLabel = new JLabel("—  Pending");
        pendingCountLabel.setFont(AppTheme.headingFont(13));
        pendingCountLabel.setForeground(AMBER_FG);

        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightSide.setOpaque(false);
        rightSide.add(pendingCountLabel);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(rightSide,  BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SCROLL AREA
    // ─────────────────────────────────────────────────────────────────────────
    private JScrollPane buildScroll() {
        requestsPanel = new JPanel();
        requestsPanel.setLayout(new BoxLayout(requestsPanel, BoxLayout.Y_AXIS));
        requestsPanel.setOpaque(false);

        JLabel loading = new JLabel("Loading requests…");
        loading.setFont(AppTheme.bodyFont(13));
        loading.setForeground(AppTheme.TEXT_MUTED);
        requestsPanel.add(loading);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(requestsPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadRequests() {
        new SwingWorker<List<WithdrawalRequest>, Void>() {
            @Override
            protected List<WithdrawalRequest> doInBackground() {
                if (!service.initAdmin(username)) return null;
                return service.getAllPendingRequests();
            }

            @Override
            protected void done() {
                try {
                    List<WithdrawalRequest> requests = get();
                    if (requests == null) {
                        showError("Failed to load admin account. Please re-login.");
                        return;
                    }
                    populateRequests(requests);
                } catch (Exception ex) {
                    showError("Error loading requests: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateRequests(List<WithdrawalRequest> requests) {
        requestsPanel.removeAll();

        int count = requests.size();
        pendingCountLabel.setText(count + "  Pending");
        pendingCountLabel.setForeground(count > 0 ? AMBER_FG : GREEN_FG);

        if (requests.isEmpty()) {
            JPanel emptyState = buildEmptyState();
            requestsPanel.add(emptyState);
        } else {
            for (WithdrawalRequest req : requests) {
                requestsPanel.add(buildRequestCard(req));
                requestsPanel.add(Box.createVerticalStrut(16));
            }
        }

        requestsPanel.revalidate();
        requestsPanel.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REQUEST CARD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildRequestCard(WithdrawalRequest req) {

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Left amber stripe — pending indicator
                g2.setColor(AMBER_FG);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Row 1: Student info + pending badge ───────────────────────────────
        JPanel row1 = new JPanel(new BorderLayout());
        row1.setOpaque(false);

        JPanel studentInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        studentInfo.setOpaque(false);

        JLabel nameLbl = new JLabel(req.getStudentName());
        nameLbl.setFont(AppTheme.headingFont(15));
        nameLbl.setForeground(AppTheme.NAVY);

        JLabel rollLbl = makePill(req.getRollNumber(), AppTheme.PALE_BLUE, AppTheme.DEEP_BLUE);
        studentInfo.add(nameLbl);
        studentInfo.add(rollLbl);

        JLabel pendingBadge = makePill("⏳  Pending", AMBER_BG, AMBER_FG);
        pendingBadge.setFont(AppTheme.headingFont(11));

        row1.add(studentInfo,  BorderLayout.WEST);
        row1.add(pendingBadge, BorderLayout.EAST);
        card.add(row1);
        card.add(Box.createVerticalStrut(8));

        // ── Row 2: Course + semester meta ─────────────────────────────────────
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row2.setOpaque(false);

        JLabel courseLbl = new JLabel("📚  " + req.getCourseLabel());
        courseLbl.setFont(AppTheme.bodyFont(13));
        courseLbl.setForeground(AppTheme.TEXT_DARK);

        JLabel semLbl = new JLabel("📅  " + req.getSemesterName());
        semLbl.setFont(AppTheme.bodyFont(12));
        semLbl.setForeground(AppTheme.TEXT_MUTED);

        JLabel dateLbl = new JLabel("🕐  " + req.getFormattedRequestedAt());
        dateLbl.setFont(AppTheme.bodyFont(12));
        dateLbl.setForeground(AppTheme.TEXT_MUTED);

        row2.add(courseLbl);
        row2.add(semLbl);
        row2.add(dateLbl);
        card.add(row2);
        card.add(Box.createVerticalStrut(10));

        // ── Divider ───────────────────────────────────────────────────────────
        card.add(makeDivider());
        card.add(Box.createVerticalStrut(10));

        // ── Reason block ──────────────────────────────────────────────────────
        JLabel reasonHeader = new JLabel("Student's Reason");
        reasonHeader.setFont(AppTheme.headingFont(11));
        reasonHeader.setForeground(AppTheme.TEXT_MUTED);
        reasonHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(reasonHeader);
        card.add(Box.createVerticalStrut(4));

        JTextArea reasonArea = new JTextArea(req.getReason());
        reasonArea.setFont(AppTheme.bodyFont(13));
        reasonArea.setForeground(AppTheme.TEXT_DARK);
        reasonArea.setEditable(false);
        reasonArea.setOpaque(false);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setBorder(BorderFactory.createEmptyBorder());
        reasonArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(reasonArea);
        card.add(Box.createVerticalStrut(14));

        // ── Admin comment field ───────────────────────────────────────────────
        JLabel commentHeader = new JLabel("Admin Comment (optional)");
        commentHeader.setFont(AppTheme.headingFont(11));
        commentHeader.setForeground(AppTheme.TEXT_MUTED);
        commentHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(commentHeader);
        card.add(Box.createVerticalStrut(6));

        JTextArea commentArea = new JTextArea(3, 30);
        commentArea.setFont(AppTheme.bodyFont(13));
        commentArea.setForeground(AppTheme.TEXT_DARK);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        commentArea.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, AppTheme.LIGHT_BLUE, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        JScrollPane commentScroll = new JScrollPane(commentArea);
        commentScroll.setBorder(BorderFactory.createEmptyBorder());
        commentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        commentScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(commentScroll);
        card.add(Box.createVerticalStrut(14));

        // ── Action buttons + per-card status label ────────────────────────────
        JLabel cardStatus = new JLabel(" ");
        cardStatus.setFont(AppTheme.bodyFont(12));
        cardStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        StyledButton approveBtn = new StyledButton(
            "✓  Approve", new Color(21, 128, 61), new Color(15, 100, 48));
        approveBtn.setPreferredSize(new Dimension(140, 40));
        approveBtn.setFont(AppTheme.headingFont(13));

        StyledButton rejectBtn = new StyledButton(
            "✗  Reject", new Color(180, 40, 40), new Color(150, 20, 20));
        rejectBtn.setPreferredSize(new Dimension(140, 40));
        rejectBtn.setFont(AppTheme.headingFont(13));

        approveBtn.addActionListener(e ->
            handleReview(req, "approved", commentArea.getText(),
                         approveBtn, rejectBtn, cardStatus));

        rejectBtn.addActionListener(e ->
            handleReview(req, "rejected", commentArea.getText(),
                         approveBtn, rejectBtn, cardStatus));

        btnRow.add(approveBtn);
        btnRow.add(rejectBtn);

        card.add(btnRow);
        card.add(Box.createVerticalStrut(6));
        card.add(cardStatus);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REVIEW HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    private void handleReview(WithdrawalRequest req, String decision,
                               String comment,
                               StyledButton approveBtn, StyledButton rejectBtn,
                               JLabel cardStatus) {

        String verb = "approved".equals(decision) ? "Approve" : "Reject";
        int confirm = JOptionPane.showConfirmDialog(this,
            verb + " withdrawal request for " + req.getStudentName()
            + " (" + req.getCourseCode() + ")?\n"
            + ("approved".equals(decision)
               ? "This will remove the student from the course."
               : "The student's enrollment will remain active."),
            "Confirm " + verb,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        approveBtn.setEnabled(false);
        rejectBtn.setEnabled(false);
        cardStatus.setText("Processing…");
        cardStatus.setForeground(AppTheme.TEXT_MUTED);

        new SwingWorker<ReviewResult, Void>() {
            @Override
            protected ReviewResult doInBackground() {
                return service.reviewRequest(req.getRequestId(), decision, comment);
            }

            @Override
            protected void done() {
                try {
                    ReviewResult result = get();
                    if (result.success()) {
                        // Animate the card away and refresh the list
                        Timer t = new Timer(600, ev -> loadRequests());
                        t.setRepeats(false);
                        t.start();

                        cardStatus.setText("approved".equals(decision)
                            ? "✓  Approved — enrollment marked withdrawn"
                            : "✗  Rejected — enrollment unchanged");
                        cardStatus.setForeground(
                            "approved".equals(decision) ? GREEN_FG : RED_FG);
                    } else {
                        cardStatus.setText("⚠  " + result.message());
                        cardStatus.setForeground(RED_FG);
                        approveBtn.setEnabled(true);
                        rejectBtn.setEnabled(true);
                    }
                } catch (Exception ex) {
                    cardStatus.setText("⚠  Error: " + ex.getMessage());
                    cardStatus.setForeground(RED_FG);
                    approveBtn.setEnabled(true);
                    rejectBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EMPTY STATE
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildEmptyState() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 300));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("✅");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel("No pending withdrawal requests");
        msg.setFont(AppTheme.titleFont(18));
        msg.setForeground(AppTheme.NAVY);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("All requests have been reviewed.");
        sub.setFont(AppTheme.bodyFont(13));
        sub.setForeground(AppTheme.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(14));
        inner.add(msg);
        inner.add(Box.createVerticalStrut(6));
        inner.add(sub);

        panel.add(inner);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  WIDGET HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel makePill(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
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

    private void showError(String msg) {
        requestsPanel.removeAll();
        JLabel err = new JLabel("⚠  " + msg);
        err.setFont(AppTheme.bodyFont(13));
        err.setForeground(RED_FG);
        err.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        requestsPanel.add(err);
        requestsPanel.revalidate();
        requestsPanel.repaint();
    }
}