package ui;

import bl.StudentAnnouncementService;
import model.StudentAnnouncementItem;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * StudentAnnouncementsPanel — Pure UI layer.
 *
 * Displays all announcements visible to the logged-in student:
 *   - Course-specific (from enrolled courses)
 *   - Department-wide
 *   - University-wide
 *
 * Each card shows: Title, Message, Course/Scope, Date Posted, Posted By.
 * Announcements are loaded newest-first (sorted at the DB layer).
 *
 * Integration in StudentDashboard:
 *   contentArea.add(new StudentAnnouncementsPanel(username), BorderLayout.CENTER);
 */
public class StudentAnnouncementsPanel extends JPanel {

    // ── Scope badge colours ───────────────────────────────────────────────────
    private static final Color COURSE_BG     = new Color(232, 245, 233);
    private static final Color COURSE_FG     = new Color(27, 130, 60);
    private static final Color DEPT_BG       = new Color(232, 240, 254);
    private static final Color DEPT_FG       = new Color(25, 80, 180);
    private static final Color UNI_BG        = new Color(255, 248, 225);
    private static final Color UNI_FG        = new Color(180, 100, 0);
    private static final Color ACCENT_COURSE = new Color(27, 130, 60);
    private static final Color ACCENT_DEPT   = new Color(25, 80, 180);
    private static final Color ACCENT_UNI    = new Color(255, 193, 7);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String                     username;
    private final StudentAnnouncementService service;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JLabel  countBadge;
    private JPanel  listPanel;
    private JLabel  emptyLabel;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public StudentAnnouncementsPanel(String username) {
        this.username = username;
        this.service  = new StudentAnnouncementService();
        init();
    }

    /** Injection constructor for tests or custom service wiring. */
    public StudentAnnouncementsPanel(String username, StudentAnnouncementService service) {
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

        // Load data off the EDT so the UI stays responsive
        new SwingWorker<List<StudentAnnouncementItem>, Void>() {
            @Override
            protected List<StudentAnnouncementItem> doInBackground() {
                if (!service.init(username)) return null;
                return service.getAnnouncements();
            }

            @Override
            protected void done() {
                try {
                    List<StudentAnnouncementItem> items = get();
                    if (items == null) {
                        showError("Failed to load student account. Please re-login.");
                    } else {
                        populateList(items);
                    }
                } catch (Exception ex) {
                    showError("Error loading announcements: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        // Title block (left)
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Announcements");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Course, department, and university announcements");
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

        // Count badge (right)
        JPanel badgePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        badgePanel.setOpaque(false);
        badgePanel.setLayout(new BoxLayout(badgePanel, BoxLayout.Y_AXIS));
        badgePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        countBadge = new JLabel("—");
        countBadge.setFont(AppTheme.titleFont(22));
        countBadge.setForeground(AppTheme.NAVY);
        countBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel badgeLbl = new JLabel("Announcements");
        badgeLbl.setFont(AppTheme.bodyFont(11));
        badgeLbl.setForeground(AppTheme.TEXT_MUTED);
        badgeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        badgePanel.add(countBadge);
        badgePanel.add(badgeLbl);

        // Legend row (right-bottom)
        JPanel legendRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        legendRow.setOpaque(false);
        legendRow.add(makeLegendDot(COURSE_FG, "Course"));
        legendRow.add(makeLegendDot(DEPT_FG,   "Department"));
        legendRow.add(makeLegendDot(UNI_FG,    "University"));

        JPanel rightSide = new JPanel(new BorderLayout(0, 8));
        rightSide.setOpaque(false);
        rightSide.add(badgePanel, BorderLayout.NORTH);
        rightSide.add(legendRow,  BorderLayout.SOUTH);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(rightSide,  BorderLayout.EAST);
        return header;
    }

    private JPanel makeLegendDot(Color color, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 2, 10, 10);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(10, 14); }
        };
        dot.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.bodyFont(11));
        lbl.setForeground(AppTheme.TEXT_MUTED);

        p.add(dot);
        p.add(lbl);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BODY: scrollable announcement list
    // ─────────────────────────────────────────────────────────────────────────
    private JScrollPane buildBody() {
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        // Loading placeholder
        emptyLabel = new JLabel("Loading announcements…");
        emptyLabel.setFont(AppTheme.bodyFont(13));
        emptyLabel.setForeground(AppTheme.TEXT_MUTED);
        emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(emptyLabel);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(listPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANNOUNCEMENT CARD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildAnnouncementCard(StudentAnnouncementItem item) {

        // Choose accent colour by scope
        Color accentColor = switch (item.getScopeType()) {
            case "Course"     -> ACCENT_COURSE;
            case "Department" -> ACCENT_DEPT;
            default           -> ACCENT_UNI;
        };
        Color badgeBg = switch (item.getScopeType()) {
            case "Course"     -> COURSE_BG;
            case "Department" -> DEPT_BG;
            default           -> UNI_BG;
        };
        Color badgeFg = switch (item.getScopeType()) {
            case "Course"     -> COURSE_FG;
            case "Department" -> DEPT_FG;
            default           -> UNI_FG;
        };

        final Color accent = accentColor;

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Left accent bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── TOP ROW: title + scope badge ──────────────────────────────────────
        JPanel topRow = new JPanel(new BorderLayout(10, 0));
        topRow.setOpaque(false);

        JLabel titleLbl = new JLabel(item.getTitle());
        titleLbl.setFont(AppTheme.headingFont(15));
        titleLbl.setForeground(AppTheme.NAVY);

        JLabel scopeBadge = new JLabel(item.getScopeLabel()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(badgeBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        scopeBadge.setFont(AppTheme.headingFont(10));
        scopeBadge.setForeground(badgeFg);
        scopeBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        scopeBadge.setOpaque(false);

        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(scopeBadge);

        topRow.add(titleLbl,    BorderLayout.CENTER);
        topRow.add(badgeWrapper, BorderLayout.EAST);

        // ── COURSE NAME ROW ───────────────────────────────────────────────────
        JLabel courseLbl = new JLabel(item.getCourseName());
        courseLbl.setFont(AppTheme.bodyFont(12));
        courseLbl.setForeground(accentColor);
        courseLbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        // ── CONTENT ───────────────────────────────────────────────────────────
        JTextArea contentArea = new JTextArea(item.getContent());
        contentArea.setFont(AppTheme.bodyFont(13));
        contentArea.setForeground(AppTheme.TEXT_DARK);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setOpaque(false);
        contentArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentArea.setFocusable(false);

        // ── BOTTOM META ROW: date + posted by ────────────────────────────────
        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setOpaque(false);
        metaRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
            new Color(220, 230, 245)));
        metaRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 230, 245)),
            BorderFactory.createEmptyBorder(8, 0, 0, 0)));

        JLabel dateLbl = new JLabel("🕐  " + item.getFormattedDate());
        dateLbl.setFont(AppTheme.bodyFont(11));
        dateLbl.setForeground(AppTheme.TEXT_MUTED);

        JLabel byLbl = new JLabel("Posted by: " + item.getPostedByName());
        byLbl.setFont(AppTheme.bodyFont(11));
        byLbl.setForeground(AppTheme.TEXT_MUTED);

        metaRow.add(dateLbl, BorderLayout.WEST);
        metaRow.add(byLbl,   BorderLayout.EAST);

        // ── Assemble card ──────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(topRow);
        body.add(courseLbl);
        body.add(contentArea);
        body.add(metaRow);

        card.add(body, BorderLayout.CENTER);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(14, new Color(180, 210, 255), 1),
                    BorderFactory.createEmptyBorder(17, 23, 17, 19)));
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 20));
                card.repaint();
            }
        });

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA POPULATION (called on EDT after SwingWorker finishes)
    // ─────────────────────────────────────────────────────────────────────────
    private void populateList(List<StudentAnnouncementItem> items) {
        listPanel.removeAll();

        if (items.isEmpty()) {
            JLabel none = new JLabel("No announcements at the moment.");
            none.setFont(AppTheme.bodyFont(13));
            none.setForeground(AppTheme.TEXT_MUTED);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            none.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            listPanel.add(none);
            countBadge.setText("0");
        } else {
            for (StudentAnnouncementItem item : items) {
                listPanel.add(buildAnnouncementCard(item));
                listPanel.add(Box.createVerticalStrut(14));
            }
            countBadge.setText(String.valueOf(items.size()));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void showError(String msg) {
        listPanel.removeAll();
        JLabel err = new JLabel("⚠  " + msg);
        err.setFont(AppTheme.bodyFont(13));
        err.setForeground(new Color(198, 40, 40));
        err.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(err);
        listPanel.revalidate();
        listPanel.repaint();
        countBadge.setText("!");
    }
}