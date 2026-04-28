package ui;

import bl.StudentAttendanceService;
import bl.StudentAttendanceService.AttendanceSummary;
import model.StudentAttendanceItem;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * StudentAttendancePanel — Pure UI layer.
 *
 * Displays per-course attendance for the logged-in student:
 *   - Header summary cards: overall %, courses, eligible count
 *   - One card per enrolled course showing:
 *       Course Name, Section, Teacher
 *       Total Classes | Classes Attended | Absent | Late
 *       Attendance % with a colour-coded progress bar
 *       Eligibility status badge (≥75% = eligible, <75% = at risk)
 *       "X more classes needed" hint when below threshold
 *
 * Integration in StudentDashboard:
 *   contentArea.add(new StudentAttendancePanel(username), BorderLayout.CENTER);
 */
public class StudentAttendancePanel extends JPanel {

    // ── Colour palette ────────────────────────────────────────────────────────
    private static final Color GREEN_BG   = new Color(220, 252, 231);
    private static final Color GREEN_FG   = new Color(21, 128, 61);
    private static final Color GREEN_BAR  = new Color(34, 197, 94);
    private static final Color AMBER_BG   = new Color(255, 248, 225);
    private static final Color AMBER_FG   = new Color(180, 100, 0);
    private static final Color AMBER_BAR  = new Color(251, 191, 36);
    private static final Color RED_BG     = new Color(255, 235, 238);
    private static final Color RED_FG     = new Color(198, 40, 40);
    private static final Color RED_BAR    = new Color(239, 68, 68);
    private static final Color CARD_BORDER = new Color(220, 230, 245);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String                   username;
    private final StudentAttendanceService service;

    // ── UI refs (header badges updated after data loads) ──────────────────────
    private JLabel overallPctLabel;
    private JLabel eligibleLabel;
    private JLabel totalCoursesLabel;
    private JPanel courseGrid;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public StudentAttendancePanel(String username) {
        this.username = username;
        this.service  = new StudentAttendanceService();
        init();
    }

    /** Injection constructor for tests or custom service wiring. */
    public StudentAttendancePanel(String username, StudentAttendanceService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildGridScroll(), BorderLayout.CENTER);

        // Load DB data off the EDT
        new SwingWorker<List<StudentAttendanceItem>, Void>() {
            @Override
            protected List<StudentAttendanceItem> doInBackground() {
                if (!service.init(username)) return null;
                return service.getAttendanceSummary();
            }

            @Override
            protected void done() {
                try {
                    List<StudentAttendanceItem> courses = get();
                    if (courses == null) {
                        showGridError("Failed to load student account. Please re-login.");
                        return;
                    }
                    AttendanceSummary summary = service.computeSummary(courses);
                    updateHeaderBadges(summary);
                    populateCourseGrid(courses);
                } catch (Exception ex) {
                    showGridError("Error loading attendance: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        // ── Title block ───────────────────────────────────────────────────────
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("My Attendance");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Attendance records for the current semester");
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

        // ── Summary stat cards (right) ────────────────────────────────────────
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        statsRow.setOpaque(false);

        overallPctLabel   = addStatCard(statsRow, "—",  "Overall Attendance", AppTheme.MID_BLUE);
        totalCoursesLabel = addStatCard(statsRow, "—",  "Courses",            AppTheme.DEEP_BLUE);
        eligibleLabel     = addStatCard(statsRow, "—",  "Eligible",           GREEN_FG);

        header.add(statsRow, BorderLayout.EAST);

        // ── Threshold info bar ────────────────────────────────────────────────
        JPanel thresholdBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        thresholdBar.setOpaque(false);
        thresholdBar.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JLabel thresholdIcon = new JLabel("ℹ");
        thresholdIcon.setFont(AppTheme.bodyFont(13));
        thresholdIcon.setForeground(AppTheme.MID_BLUE);

        JLabel thresholdText = new JLabel(
            "Minimum " + StudentAttendanceItem.ELIGIBILITY_THRESHOLD
            + "% attendance required for exam eligibility. "
            + "Late arrivals count as attended.");
        thresholdText.setFont(AppTheme.bodyFont(12));
        thresholdText.setForeground(AppTheme.TEXT_MUTED);

        thresholdBar.add(thresholdIcon);
        thresholdBar.add(thresholdText);

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.add(header,       BorderLayout.CENTER);
        headerWrapper.add(thresholdBar, BorderLayout.SOUTH);
        return headerWrapper;
    }

    /**
     * Creates one stat card and returns the value JLabel so it can be
     * updated later when data loads.
     */
    private JLabel addStatCard(JPanel parent, String value, String label, Color numColor) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        card.setPreferredSize(new Dimension(120, 72));

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(AppTheme.titleFont(20));
        valLbl.setForeground(numColor);
        valLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel txtLbl = new JLabel(label);
        txtLbl.setFont(AppTheme.bodyFont(11));
        txtLbl.setForeground(AppTheme.TEXT_MUTED);
        txtLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(valLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(txtLbl);
        parent.add(card);
        return valLbl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COURSE GRID (scrollable)
    // ─────────────────────────────────────────────────────────────────────────
    private JScrollPane buildGridScroll() {
        courseGrid = new JPanel();
        courseGrid.setLayout(new GridLayout(0, 1, 0, 14));
        courseGrid.setOpaque(false);

        // Loading placeholder
        JLabel loading = new JLabel("Loading attendance records…");
        loading.setFont(AppTheme.bodyFont(13));
        loading.setForeground(AppTheme.TEXT_MUTED);
        loading.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        courseGrid.add(loading);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(courseGrid, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COURSE CARD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCourseCard(StudentAttendanceItem item) {

        double pct        = item.getAttendancePct();
        boolean eligible  = item.isEligible();
        boolean atRisk    = pct > 0 && pct < StudentAttendanceItem.ELIGIBILITY_THRESHOLD;
        boolean noData    = item.getTotalClasses() == 0;

        Color accentColor = noData    ? AppTheme.TEXT_MUTED
                          : eligible  ? GREEN_FG
                          : atRisk    ? RED_FG
                          : AMBER_FG;
        Color barColor    = noData    ? new Color(200, 210, 225)
                          : eligible  ? GREEN_BAR
                          : atRisk    ? RED_BAR
                          : AMBER_BAR;

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Left accent bar
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // ── LEFT: course info ─────────────────────────────────────────────────
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel courseNameLbl = new JLabel(item.getCourseName());
        courseNameLbl.setFont(AppTheme.titleFont(16));
        courseNameLbl.setForeground(AppTheme.NAVY);
        courseNameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel codeBadge = makePill(item.getCourseCode(), AppTheme.PALE_BLUE, AppTheme.DEEP_BLUE);
        JLabel sectionLbl = new JLabel("Section " + item.getSection());
        sectionLbl.setFont(AppTheme.bodyFont(12));
        sectionLbl.setForeground(AppTheme.TEXT_MUTED);
        JLabel teacherLbl = new JLabel("👤  " + item.getTeacherName());
        teacherLbl.setFont(AppTheme.bodyFont(12));
        teacherLbl.setForeground(AppTheme.TEXT_MUTED);

        metaRow.add(codeBadge);
        metaRow.add(sectionLbl);
        metaRow.add(teacherLbl);

        // Progress bar
        JPanel barTrack = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Track
                g2.setColor(new Color(230, 235, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Fill
                int fillW = noData ? 0 : (int) (getWidth() * Math.min(pct / 100.0, 1.0));
                if (fillW > 0) {
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 0, fillW, getHeight(), 8, 8);
                }
                // Threshold marker at 75%
                int markerX = (int) (getWidth() * 0.75);
                g2.setColor(new Color(100, 130, 180, 180));
                g2.setStroke(new java.awt.BasicStroke(1.5f,
                    java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER,
                    1f, new float[]{4, 3}, 0));
                g2.drawLine(markerX, 0, markerX, getHeight());
                g2.dispose();
            }
        };
        barTrack.setOpaque(false);
        barTrack.setPreferredSize(new Dimension(Integer.MAX_VALUE, 10));
        barTrack.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        barTrack.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Hint: classes needed
        String hintText = "";
        if (!noData && !eligible) {
            int needed = item.classesNeededForEligibility();
            hintText = "⚠  Attend " + needed + " more consecutive class"
                       + (needed == 1 ? "" : "es") + " to reach eligibility";
        } else if (noData) {
            hintText = "No classes recorded yet for this course";
        }
        JLabel hintLbl = new JLabel(hintText);
        hintLbl.setFont(AppTheme.bodyFont(11));
        hintLbl.setForeground(noData ? AppTheme.TEXT_MUTED : RED_FG);
        hintLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(courseNameLbl);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(metaRow);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(barTrack);
        infoPanel.add(Box.createVerticalStrut(4));
        if (!hintText.isEmpty()) infoPanel.add(hintLbl);

        // ── RIGHT: stats block ────────────────────────────────────────────────
        JPanel statsBlock = new JPanel();
        statsBlock.setOpaque(false);
        statsBlock.setLayout(new BoxLayout(statsBlock, BoxLayout.Y_AXIS));

        // Big percentage number
        JLabel pctLbl = new JLabel(item.getAttendancePctStr());
        pctLbl.setFont(AppTheme.titleFont(28));
        pctLbl.setForeground(accentColor);
        pctLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Eligibility badge
        String badgeText   = noData ? "No Data" : eligible ? "✓  Eligible" : "✗  At Risk";
        Color  badgeBg     = noData ? new Color(240,240,245) : eligible ? GREEN_BG : RED_BG;
        Color  badgeFg     = noData ? AppTheme.TEXT_MUTED    : eligible ? GREEN_FG : RED_FG;
        JLabel eligBadge   = makePill(badgeText, badgeBg, badgeFg);
        eligBadge.setFont(AppTheme.headingFont(11));
        eligBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        statsBlock.add(pctLbl);
        statsBlock.add(Box.createVerticalStrut(6));
        statsBlock.add(eligBadge);

        // Four count boxes in a 2×2 grid
        JPanel countsGrid = new JPanel(new GridLayout(2, 2, 6, 6));
        countsGrid.setOpaque(false);
        countsGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
        countsGrid.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        countsGrid.add(makeCountBox("Total",    item.getTotalClasses(),    AppTheme.DEEP_BLUE));
        countsGrid.add(makeCountBox("Attended", item.getClassesAttended(), GREEN_FG));
        countsGrid.add(makeCountBox("Absent",   item.getClassesAbsent(),   RED_FG));
        countsGrid.add(makeCountBox("Late",     item.getClassesLate(),     AMBER_FG));

        statsBlock.add(countsGrid);

        card.add(infoPanel,  BorderLayout.CENTER);
        card.add(statsBlock, BorderLayout.EAST);

        // Subtle hover lift
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(14, new Color(180, 210, 255), 1),
                    BorderFactory.createEmptyBorder(19, 23, 19, 23)));
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
                card.repaint();
            }
        });

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  WIDGET HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Small rounded count box with a number and a label below it. */
    private JPanel makeCountBox(String label, int value, Color numColor) {
        JPanel box = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        box.setPreferredSize(new Dimension(62, 44));

        JLabel numLbl = new JLabel(String.valueOf(value));
        numLbl.setFont(AppTheme.headingFont(16));
        numLbl.setForeground(numColor);
        numLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel txtLbl = new JLabel(label);
        txtLbl.setFont(AppTheme.bodyFont(10));
        txtLbl.setForeground(AppTheme.TEXT_MUTED);
        txtLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(numLbl);
        box.add(txtLbl);
        return box;
    }

    /** Small rounded pill label. */
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
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        lbl.setOpaque(false);
        return lbl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA POPULATION (called on EDT after SwingWorker finishes)
    // ─────────────────────────────────────────────────────────────────────────

    private void updateHeaderBadges(AttendanceSummary summary) {
        overallPctLabel.setText(summary.overallPctStr());
        overallPctLabel.setForeground(
            summary.totalClasses() == 0          ? AppTheme.TEXT_MUTED
            : summary.overallPct() >= StudentAttendanceItem.ELIGIBILITY_THRESHOLD
                                                 ? GREEN_FG
            : summary.overallPct() >= 60         ? AMBER_FG
                                                 : RED_FG);
        totalCoursesLabel.setText(String.valueOf(summary.totalCourses()));
        eligibleLabel.setText(summary.eligibleCourses() + "/" + summary.totalCourses());
        eligibleLabel.setForeground(
            summary.eligibleCourses() == summary.totalCourses() ? GREEN_FG : AMBER_FG);
    }

    private void populateCourseGrid(List<StudentAttendanceItem> courses) {
        courseGrid.removeAll();
        if (courses.isEmpty()) {
            JLabel empty = new JLabel("No enrolled courses found for this semester.");
            empty.setFont(AppTheme.bodyFont(13));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
            courseGrid.add(empty);
        } else {
            courses.forEach(c -> courseGrid.add(buildCourseCard(c)));
        }
        courseGrid.revalidate();
        courseGrid.repaint();
    }

    private void showGridError(String msg) {
        courseGrid.removeAll();
        JLabel err = new JLabel("⚠  " + msg);
        err.setFont(AppTheme.bodyFont(13));
        err.setForeground(RED_FG);
        err.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        courseGrid.add(err);
        courseGrid.revalidate();
        courseGrid.repaint();
    }
}