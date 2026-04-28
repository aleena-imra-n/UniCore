package ui;

import bl.FeedbackService;
import bl.FeedbackService.SubmitResult;
import bl.FeedbackService.Outcome;
import model.FeedbackItem;
import model.OfferingItem;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * SubmitFeedbackPanel — Student feedback submission UI.
 *
 * Layout:
 *   Left card  : form — course dropdown, 4 criteria star selectors, comments, submit
 *   Right card : submitted feedback history (read-only confirmation cards)
 *
 * Integration in StudentDashboard:
 *   case "Submit Feedback" -> contentArea.add(new SubmitFeedbackPanel(username), BorderLayout.CENTER);
 */
public class SubmitFeedbackPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private static final String[] CRITERIA = {
        "Teaching Quality", "Course Content", "Clarity & Communication", "Overall Satisfaction"
    };

    private static final Color GREEN = new Color(21, 128, 61);
    private static final Color AMBER = new Color(180, 100, 0);

    private final String          username;
    private final FeedbackService service;

    // Track which offeringIds already have feedback submitted
    private final Set<Integer> submittedOfferingIds = new HashSet<>();

    // UI refs
    private JComboBox<OfferingItem> courseDropdown;
    private int[]                   criteriaRatings = {0, 0, 0, 0};
    private JTextArea               commentsArea;
    private JLabel                  statusLabel;
    private StyledButton            submitBtn;
    private JPanel                  historyPanel;
    private JLabel                  historyCountBadge;

    public SubmitFeedbackPanel(String username) {
        this.username = username;
        this.service  = new FeedbackService();
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        loadData();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel tb = new JPanel();
        tb.setOpaque(false);
        tb.setLayout(new BoxLayout(tb, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Submit Feedback");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Rate your courses to help improve teaching quality");
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

        tb.add(title);
        tb.add(Box.createVerticalStrut(6));
        tb.add(sub);
        tb.add(Box.createVerticalStrut(10));
        tb.add(accent);
        header.add(tb, BorderLayout.WEST);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0; gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.weightx = 0.52;
        gbc.insets = new Insets(0, 0, 0, 14);
        body.add(buildFormCard(), gbc);

        gbc.gridx = 1; gbc.weightx = 0.48;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildHistoryCard(), gbc);

        return body;
    }

    // ── Form card ─────────────────────────────────────────────────────────────
    private JPanel buildFormCard() {
        JPanel card = roundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(26, 28, 12, 28));

        // Section title
        addSectionTitle(card, "New Feedback", "Select a course and rate each criterion");
        card.add(Box.createVerticalStrut(10));

        // Course dropdown
        addFieldLabel(card, "Course");
        courseDropdown = new JComboBox<>();
        courseDropdown.addItem(null);
        courseDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v,
                    int i, boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, v, i, sel, focus);
                setText(v == null ? "— Select a course —" : v.toString());
                return this;
            }
        });
        styleCombo(courseDropdown);
        courseDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        courseDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        card.add(courseDropdown);
        card.add(Box.createVerticalStrut(12));;

        // 4 criteria rows
        addFieldLabel(card, "Rating Criteria  (1 = Poor, 5 = Excellent)");
        card.add(Box.createVerticalStrut(8));
        for (int i = 0; i < CRITERIA.length; i++) {
            card.add(buildCriterionRow(i));
            card.add(Box.createVerticalStrut(8));
        }
        card.add(Box.createVerticalStrut(4));

        // Comments
        addFieldLabel(card, "Comments  (optional)");
        commentsArea = new JTextArea(4, 20);
        commentsArea.setFont(AppTheme.bodyFont(13));
        commentsArea.setForeground(AppTheme.TEXT_DARK);
        commentsArea.setLineWrap(true);
        commentsArea.setWrapStyleWord(true);
        commentsArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane commScroll = new JScrollPane(commentsArea);
        commScroll.setBorder(BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2));
        commScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        commScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.add(commScroll);
        card.add(Box.createVerticalStrut(8));

        // Status + submit
        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.headingFont(12));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(8));

        submitBtn = new StyledButton("Submit Feedback", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitBtn.setMaximumSize(new Dimension(220, 44));
        submitBtn.addActionListener(e -> onSubmitClicked());
        card.add(submitBtn);

        return card;
    }

    // ── Criterion row: label + 5 star buttons ─────────────────────────────────
    private JPanel buildCriterionRow(int criterionIndex) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lbl = new JLabel(CRITERIA[criterionIndex]);
        lbl.setFont(AppTheme.bodyFont(13));
        lbl.setForeground(AppTheme.TEXT_DARK);
        lbl.setPreferredSize(new Dimension(200, 30));
        row.add(lbl, BorderLayout.WEST);

        JPanel stars = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        stars.setOpaque(false);

        JButton[] starBtns = new JButton[5];
        for (int s = 1; s <= 5; s++) {
            final int starVal = s;
			JButton star = new JButton("") {
			    @Override protected void paintComponent(Graphics g) {
			        Graphics2D g2 = (Graphics2D) g.create();
			        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			            RenderingHints.VALUE_ANTIALIAS_ON);
			        boolean filled = criteriaRatings[criterionIndex] >= starVal;
			        g2.setColor(filled ? AppTheme.GOLD : new Color(200, 215, 235));
			        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
			        g2.setColor(Color.WHITE);
			        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
			        FontMetrics fm = g2.getFontMetrics();
			        String ch = "*";
			        int x = (getWidth()  - fm.stringWidth(ch)) / 2;
			        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
			        g2.drawString(ch, x, y);
			        g2.dispose();
			    }
			};
            //star.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            //star.setForeground(Color.WHITE);
            star.setFocusPainted(false);
            star.setBorderPainted(false);
            star.setContentAreaFilled(false);
            star.setPreferredSize(new Dimension(34, 30));
            star.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            star.addActionListener(e -> {
                criteriaRatings[criterionIndex] = starVal;
                // Repaint all stars in this row
                for (JButton b : starBtns) b.repaint();
            });
            starBtns[s - 1] = star;
            stars.add(star);
        }

        row.add(stars, BorderLayout.CENTER);
        return row;
    }

    // ── History card ──────────────────────────────────────────────────────────
    private JPanel buildHistoryCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("Submitted Feedback");
        title.setFont(AppTheme.headingFont(15));
        title.setForeground(AppTheme.NAVY);

        historyCountBadge = new JLabel("0") {
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
        historyCountBadge.setFont(AppTheme.headingFont(11));
        historyCountBadge.setForeground(AppTheme.NAVY);
        historyCountBadge.setHorizontalAlignment(SwingConstants.CENTER);
        historyCountBadge.setPreferredSize(new Dimension(26, 26));
        historyCountBadge.setOpaque(false);

        titleRow.add(title,             BorderLayout.WEST);
        titleRow.add(historyCountBadge, BorderLayout.EAST);
        card.add(titleRow, BorderLayout.NORTH);

        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setOpaque(false);

        JLabel empty = new JLabel("No feedback submitted yet.");
        empty.setFont(AppTheme.bodyFont(12));
        empty.setForeground(AppTheme.TEXT_MUTED);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyPanel.add(empty);

        JScrollPane scroll = new JScrollPane(historyPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildHistoryRow(FeedbackItem f) {
        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(4f));
                g2.drawLine(4, 8, 4, getHeight() - 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel course = new JLabel(f.getCourseCode() + "  —  " + f.getCourseName());
        course.setFont(AppTheme.headingFont(12));
        course.setForeground(AppTheme.NAVY);
        course.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel stars = new JLabel(starsString(f.getRating()) + "  (" + f.getRating() + "/5)");
        stars.setFont(AppTheme.bodyFont(12));
        stars.setForeground(AppTheme.GOLD_DARK);
        stars.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel meta = new JLabel("Submitted: " + f.getSubmittedAt().format(FMT)
            + "  ·  Anonymous");
        meta.setFont(AppTheme.bodyFont(10));
        meta.setForeground(AppTheme.TEXT_MUTED);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(course);
        row.add(Box.createVerticalStrut(3));
        row.add(stars);
        row.add(Box.createVerticalStrut(3));
        if (!f.getComments().isBlank()) {
            JLabel comm = new JLabel("\"" + truncate(f.getComments(), 55) + "\"");
            comm.setFont(AppTheme.bodyFont(11));
            comm.setForeground(AppTheme.TEXT_DARK);
            comm.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(comm);
            row.add(Box.createVerticalStrut(2));
        }
        row.add(meta);
        return row;
    }

    // ── Event handler ─────────────────────────────────────────────────────────
    private void onSubmitClicked() {
        OfferingItem selected = (OfferingItem) courseDropdown.getSelectedItem();

        submitBtn.setEnabled(false);
        showStatus("Submitting…", AppTheme.TEXT_MUTED);

        new SwingWorker<SubmitResult, Void>() {
            @Override protected SubmitResult doInBackground() {
                return service.submit(selected, criteriaRatings,
                    commentsArea.getText());
            }
            @Override protected void done() {
                submitBtn.setEnabled(true);
                try {
                    SubmitResult result = get();
                    if (result.outcome() == Outcome.SUCCESS) {
                        showStatus("✅  " + result.message(), GREEN);
                        // Add to submitted set so dropdown can be updated
                        if (selected != null)
                            submittedOfferingIds.add(selected.getOfferingId());
                        // Reload history
                        loadHistory();
                        // Reset form
                        resetForm();
                    } else {
                        showStatus("⚠  " + result.message(), AMBER);
                    }
                } catch (Exception ex) {
                    showStatus("✖  Error: " + ex.getMessage(), Color.RED);
                }
            }
        }.execute();
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadData() {
        new SwingWorker<Void, Void>() {
            List<OfferingItem>  offerings;
            List<FeedbackItem>  submitted;

            @Override protected Void doInBackground() {
                if (!service.initStudent(username)) return null;
                offerings = service.getEnrolledOfferings();
                submitted = service.getSubmittedFeedback();
                return null;
            }
            @Override protected void done() {
                if (offerings == null) {
                    showStatus("Failed to load data. Please re-login.", Color.RED);
                    return;
                }
                // Populate submitted set
                submitted.forEach(f -> submittedOfferingIds.add(f.getOfferingId()));
                // Populate dropdown (exclude already-submitted)
                offerings.stream()
                    .filter(o -> !submittedOfferingIds.contains(o.getOfferingId()))
                    .forEach(courseDropdown::addItem);

                populateHistory(submitted);
            }
        }.execute();
    }

    private void loadHistory() {
        new SwingWorker<List<FeedbackItem>, Void>() {
            @Override protected List<FeedbackItem> doInBackground() {
                return service.getSubmittedFeedback();
            }
            @Override protected void done() {
                try { populateHistory(get()); }
                catch (Exception ignored) {}
            }
        }.execute();
    }

    private void populateHistory(List<FeedbackItem> items) {
        historyPanel.removeAll();
        if (items.isEmpty()) {
            JLabel empty = new JLabel("No feedback submitted yet.");
            empty.setFont(AppTheme.bodyFont(12));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            historyPanel.add(empty);
        } else {
            historyPanel.add(Box.createVerticalStrut(4));
            items.forEach(f -> {
                historyPanel.add(buildHistoryRow(f));
                historyPanel.add(Box.createVerticalStrut(8));
            });
            historyCountBadge.setText(String.valueOf(items.size()));
            historyCountBadge.repaint();
        }
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private void resetForm() {
        criteriaRatings = new int[]{0, 0, 0, 0};
        commentsArea.setText("");
        courseDropdown.setSelectedIndex(0);
        // Remove just-submitted course from dropdown
        for (int i = courseDropdown.getItemCount() - 1; i >= 1; i--) {
            OfferingItem o = courseDropdown.getItemAt(i);
            if (o != null && submittedOfferingIds.contains(o.getOfferingId()))
                courseDropdown.removeItemAt(i);
        }
        repaint();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String starsString(int rating) {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        if (msg.startsWith("✅") || msg.startsWith("⚠") || msg.startsWith("✖")) {
            javax.swing.Timer t = new javax.swing.Timer(4000,
                e -> statusLabel.setText(" "));
            t.setRepeats(false);
            t.start();
        }
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

    private void addSectionTitle(JPanel p, String title, String sub) {
        JLabel t = new JLabel(title);
        t.setFont(AppTheme.headingFont(15));
        t.setForeground(AppTheme.NAVY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel s = new JLabel(sub);
        s.setFont(AppTheme.bodyFont(12));
        s.setForeground(AppTheme.TEXT_MUTED);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(3));
        p.add(s);
    }

    private void addFieldLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.headingFont(12));
        l.setForeground(AppTheme.DEEP_BLUE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(5));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    }
}