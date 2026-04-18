package ui;

import bl.TranscriptService;
import dao.TranscriptDAO.TranscriptData;
import model.TranscriptCourseRow;
import model.TranscriptSemesterRow;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

/**
 * StudentTranscriptPanel — Pure UI layer.
 *
 * Displays a student's complete academic history:
 *   - Header summary cards: Overall CGPA, Total Credit Hours, Courses Completed
 *   - Chronological semester blocks, each containing:
 *       - Course table rows: Code | Name | Credits | Marks | % | Grade | Points
 *       - Semester GPA footer card
 *   - Final CGPA banner at the bottom
 *
 * Data is read-only (transcript is never edited here).
 * Loaded off the EDT via SwingWorker using SP_GetTranscript.
 *
 * Integration in StudentDashboard:
 *   contentArea.add(new StudentTranscriptPanel(username), BorderLayout.CENTER);
 */
public class StudentTranscriptPanel extends JPanel {

    // ── Colour constants ──────────────────────────────────────────────────────
    private static final Color GREEN_FG    = new Color(21, 128, 61);
    private static final Color GREEN_BG    = new Color(220, 252, 231);
    private static final Color AMBER_FG    = new Color(180, 100, 0);
    private static final Color AMBER_BG    = new Color(255, 248, 225);
    private static final Color RED_FG      = new Color(198, 40, 40);
    private static final Color RED_BG      = new Color(255, 235, 238);
    private static final Color GOLD_BANNER = new Color(255, 248, 220);
    private static final Color TABLE_HEAD  = new Color(227, 242, 253);
    private static final Color TABLE_ALT   = new Color(245, 250, 255);
    private static final Color DIVIDER     = new Color(215, 228, 248);

    // Column widths for the course table (px)
    private static final int[] COL_WIDTHS = { 80, 220, 55, 100, 70, 55, 60 };
    private static final String[] COL_HEADS = {
        "Code", "Course Name", "Credits", "Marks", "%", "Grade", "Pts"
    };

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String            username;
    private final TranscriptService service;

    // ── Header labels (updated after data loads) ──────────────────────────────
    private JLabel cgpaLabel;
    private JLabel creditsLabel;
    private JLabel coursesLabel;

    // ── Scrollable content area ───────────────────────────────────────────────
    private JPanel contentPanel;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public StudentTranscriptPanel(String username) {
        this.username = username;
        this.service  = new TranscriptService();
        init();
    }

    /** Injection constructor for tests or custom service wiring. */
    public StudentTranscriptPanel(String username, TranscriptService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(),      BorderLayout.NORTH);
        add(buildScroll(),      BorderLayout.CENTER);

        // Load off the EDT
        new SwingWorker<TranscriptData, Void>() {
            @Override
            protected TranscriptData doInBackground() {
                if (!service.init(username)) return null;
                return service.loadTranscript();
            }

            @Override
            protected void done() {
                try {
                    TranscriptData data = get();
                    if (data == null) {
                        showError("Failed to load transcript. Please re-login or contact admin.");
                        return;
                    }
                    if (data.isEmpty()) {
                        showError("No graded courses found. Marks may not have been uploaded yet.");
                        return;
                    }
                    populateTranscript(data);
                } catch (Exception ex) {
                    showError("Error loading transcript: " + ex.getMessage());
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

        JLabel title = new JLabel("Academic Transcript");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Complete academic history · Read-only");
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

        // ── Summary stat cards ────────────────────────────────────────────────
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        statsRow.setOpaque(false);

        cgpaLabel    = addStatCard(statsRow, "—",  "Overall CGPA",    AppTheme.MID_BLUE);
        creditsLabel = addStatCard(statsRow, "—",  "Credit Hours",     AppTheme.DEEP_BLUE);
        coursesLabel = addStatCard(statsRow, "—",  "Courses Done",     GREEN_FG);

        header.add(statsRow, BorderLayout.EAST);
        return header;
    }

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
        card.setPreferredSize(new Dimension(130, 72));

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
    //  SCROLL AREA
    // ─────────────────────────────────────────────────────────────────────────
    private JScrollPane buildScroll() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Loading placeholder
        JLabel loading = new JLabel("Loading transcript…");
        loading.setFont(AppTheme.bodyFont(13));
        loading.setForeground(AppTheme.TEXT_MUTED);
        loading.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        contentPanel.add(loading);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(contentPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TRANSCRIPT POPULATION (called on EDT)
    // ─────────────────────────────────────────────────────────────────────────
    private void populateTranscript(TranscriptData data) {
        // Update header stat cards
        cgpaLabel.setText(String.format("%.2f", data.getOverallCgpa()));
        cgpaLabel.setForeground(gpaColor(data.getOverallCgpa()));
        creditsLabel.setText(String.valueOf(data.getTotalCreditHours()));
        coursesLabel.setText(String.valueOf(data.getTotalCourses()));

        // Group course rows by semester and build index
        Map<String, List<TranscriptCourseRow>> grouped =
            service.groupBySemester(data.courseRows());
        Map<String, TranscriptSemesterRow> semIndex =
            service.buildSemesterIndex(data.semesterRows());

        contentPanel.removeAll();

        // One block per semester (chronological order)
        for (Map.Entry<String, List<TranscriptCourseRow>> entry : grouped.entrySet()) {
            String semName    = entry.getKey();
            List<TranscriptCourseRow> courses = entry.getValue();
            TranscriptSemesterRow semRow = semIndex.get(semName);

            contentPanel.add(buildSemesterBlock(semName, courses, semRow));
            contentPanel.add(Box.createVerticalStrut(20));
        }

        // Final CGPA banner
        if (!data.semesterRows().isEmpty()) {
            TranscriptSemesterRow last = data.semesterRows()
                                             .get(data.semesterRows().size() - 1);
            contentPanel.add(buildCgpaBanner(last.getCgpaSoFar(),
                                              data.getTotalCreditHours(),
                                              data.getTotalCourses()));
            contentPanel.add(Box.createVerticalStrut(10));
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SEMESTER BLOCK
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildSemesterBlock(String semName,
                                       List<TranscriptCourseRow> courses,
                                       TranscriptSemesterRow semRow) {
        JPanel block = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Left accent stripe in deep blue
                g2.setColor(AppTheme.DEEP_BLUE);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Semester header bar ───────────────────────────────────────────────
        JPanel semHeader = new JPanel(new BorderLayout());
        semHeader.setOpaque(false);
        semHeader.setBorder(BorderFactory.createEmptyBorder(16, 24, 10, 24));

        JLabel semLabel = new JLabel(semName);
        semLabel.setFont(AppTheme.headingFont(15));
        semLabel.setForeground(AppTheme.NAVY);

        String courseWord = courses.size() == 1 ? "course" : "courses";
        JLabel semMeta = new JLabel(courses.size() + " " + courseWord
            + (semRow != null ? "  ·  " + semRow.getSemCreditHours() + " credit hours" : ""));
        semMeta.setFont(AppTheme.bodyFont(12));
        semMeta.setForeground(AppTheme.TEXT_MUTED);

        semHeader.add(semLabel, BorderLayout.WEST);
        semHeader.add(semMeta,  BorderLayout.EAST);

        // ── Separator ─────────────────────────────────────────────────────────
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(DIVIDER);
                g.fillRect(24, 0, getWidth() - 48, 1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 1); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 1); }
        };
        sep.setOpaque(false);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Column headers ────────────────────────────────────────────────────
        JPanel tableHead = buildTableRow(COL_HEADS, null, TABLE_HEAD, true);

        block.add(semHeader);
        block.add(sep);
        block.add(tableHead);

        // ── Course rows ───────────────────────────────────────────────────────
        boolean alt = false;
        for (TranscriptCourseRow row : courses) {
            String[] cells = {
                row.getCourseCode(),
                row.getCourseName(),
                String.valueOf(row.getCreditHours()),
                String.format("%.0f / %.0f", row.getMarksObtained(), row.getMarksTotal()),
                row.getPercentageStr(),
                row.getLetterGrade(),
                String.format("%.2f", row.getGradePoints())
            };
            Color gradeColor = gradeColor(row.getLetterGrade());
            Color rowBg = alt ? TABLE_ALT : Color.WHITE;
            block.add(buildTableRow(cells, gradeColor, rowBg, false));
            alt = !alt;
        }

        // ── Semester GPA footer ───────────────────────────────────────────────
        if (semRow != null) {
            block.add(buildSemGpaFooter(semRow));
        }

        block.add(Box.createVerticalStrut(4));
        return block;
    }

    /**
     * Builds one table row (header or data) using fixed column widths.
     *
     * @param cells       text for each cell
     * @param gradeColor  colour for the Grade cell only (null = default)
     * @param bg          background colour for this row
     * @param isHeader    true → bold font; false → body font
     */
    private JPanel buildTableRow(String[] cells, Color gradeColor, Color bg, boolean isHeader) {
        final Color rowBg = bg;
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(rowBg);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 24, 6, 24));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isHeader ? 32 : 36));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Font font = isHeader ? AppTheme.headingFont(12) : AppTheme.bodyFont(12);
        Color defaultFg = isHeader ? AppTheme.DEEP_BLUE : AppTheme.TEXT_DARK;

        for (int i = 0; i < cells.length && i < COL_WIDTHS.length; i++) {
            JLabel lbl = new JLabel(cells[i]);
            lbl.setFont(font);
            int colIdx = i;
            // Grade column (index 5) gets colour coding on data rows
            if (!isHeader && colIdx == 5 && gradeColor != null) {
                lbl = makePill(cells[i],
                    gradeBackground(cells[i]),
                    gradeColor);
            } else {
                lbl.setForeground(defaultFg);
            }
            lbl.setPreferredSize(new Dimension(COL_WIDTHS[i], isHeader ? 20 : 24));
            row.add(lbl);
        }
        return row;
    }

    /** GPA footer card shown below each semester's course rows. */
    private JPanel buildSemGpaFooter(TranscriptSemesterRow semRow) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TABLE_HEAD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 24, 10, 24));
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        footer.add(makeGpaStat("Semester GPA",
            semRow.getSemesterGpaStr(), gpaColor(semRow.getSemesterGpa())));
        footer.add(makeSeparatorLine());
        footer.add(makeGpaStat("CGPA so far",
            semRow.getCgpaSoFarStr(), gpaColor(semRow.getCgpaSoFar())));
        footer.add(makeSeparatorLine());
        footer.add(makeGpaStat("Credit Hours",
            String.valueOf(semRow.getSemCreditHours()), AppTheme.DEEP_BLUE));

        return footer;
    }

    private JPanel makeGpaStat(String label, String value, Color valueColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(AppTheme.bodyFont(12));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(AppTheme.headingFont(13));
        val.setForeground(valueColor);
        p.add(lbl);
        p.add(val);
        return p;
    }

    private JPanel makeSeparatorLine() {
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(DIVIDER);
                g.fillRect(0, 4, 1, getHeight() - 8);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(1, 28); }
        };
        sep.setOpaque(false);
        return sep;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FINAL CGPA BANNER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCgpaBanner(double cgpa, int totalCredits, int totalCourses) {
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient from deep blue to mid blue
                GradientPaint gp = new GradientPaint(
                    0, 0, AppTheme.DEEP_BLUE,
                    getWidth(), 0, AppTheme.MID_BLUE);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Gold accent bottom
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new java.awt.BasicStroke(3));
                g2.drawLine(20, getHeight() - 2, getWidth() - 20, getHeight() - 2);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(BorderFactory.createEmptyBorder(20, 28, 22, 28));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left: label
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel("Cumulative GPA (CGPA)");
        titleLbl.setFont(AppTheme.headingFont(13));
        titleLbl.setForeground(AppTheme.LIGHT_BLUE);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLbl = new JLabel(totalCourses + " courses completed  ·  "
            + totalCredits + " credit hours earned");
        subLbl.setFont(AppTheme.bodyFont(12));
        subLbl.setForeground(new Color(179, 229, 252, 200));
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(titleLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(subLbl);

        // Right: big CGPA number
        JLabel cgpaLbl = new JLabel(String.format("%.2f", cgpa));
        cgpaLbl.setFont(AppTheme.titleFont(36));
        cgpaLbl.setForeground(AppTheme.GOLD);

        // Standing label
        JLabel standingLbl = new JLabel(standing(cgpa));
        standingLbl.setFont(AppTheme.headingFont(12));
        standingLbl.setForeground(Color.WHITE);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        cgpaLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        standingLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(cgpaLbl);
        right.add(standingLbl);

        banner.add(left,  BorderLayout.CENTER);
        banner.add(right, BorderLayout.EAST);
        return banner;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Pill label with rounded background. */
    private JLabel makePill(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        lbl.setFont(AppTheme.headingFont(11));
        lbl.setForeground(fg);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        lbl.setOpaque(false);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    /** Returns the foreground colour for a letter grade. */
    private Color gradeColor(String grade) {
        if (grade == null) return AppTheme.TEXT_MUTED;
        return switch (grade) {
            case "A+", "A", "A-" -> GREEN_FG;
            case "B+", "B", "B-" -> AppTheme.MID_BLUE;
            case "C+", "C", "C-" -> AMBER_FG;
            case "D"              -> new Color(150, 80, 0);
            default               -> RED_FG;    // F
        };
    }

    /** Returns the background colour for a grade pill. */
    private Color gradeBackground(String grade) {
        if (grade == null) return AppTheme.PALE_BLUE;
        return switch (grade) {
            case "A+", "A", "A-" -> GREEN_BG;
            case "B+", "B", "B-" -> AppTheme.PALE_BLUE;
            case "C+", "C", "C-" -> AMBER_BG;
            case "D"              -> new Color(255, 243, 220);
            default               -> RED_BG;    // F
        };
    }

    /** Returns a colour for a GPA value on the 0–4 scale. */
    private Color gpaColor(double gpa) {
        if (gpa >= 3.5) return GREEN_FG;
        if (gpa >= 2.5) return AppTheme.MID_BLUE;
        if (gpa >= 1.5) return AMBER_FG;
        return RED_FG;
    }

    /** Academic standing string. */
    private String standing(double cgpa) {
        if (cgpa >= 3.7) return "Distinction";
        if (cgpa >= 3.3) return "High Merit";
        if (cgpa >= 3.0) return "Merit";
        if (cgpa >= 2.0) return "Satisfactory";
        if (cgpa >= 1.0) return "Probation";
        return "Academic Warning";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ERROR STATE
    // ─────────────────────────────────────────────────────────────────────────
    private void showError(String msg) {
        contentPanel.removeAll();
        JLabel err = new JLabel("⚠  " + msg);
        err.setFont(AppTheme.bodyFont(13));
        err.setForeground(RED_FG);
        err.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        contentPanel.add(err);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}