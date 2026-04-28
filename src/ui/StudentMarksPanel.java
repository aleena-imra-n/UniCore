package ui;

import bl.StudentMarksService;
import model.MarksItem;
import model.StudentMarksRecord;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * StudentMarksPanel — Pure UI layer.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────┐
 *   │  CGPA summary tile (top)                    │
 *   ├─────────────────────────────────────────────┤
 *   │  One card per enrolled course (scrollable)  │
 *   │  Each card shows:                           │
 *   │   - Course name + code + teacher            │
 *   │   - Grade badge (letter grade, colour-coded)│
 *   │   - Per-component breakdown table           │
 *   │   - Weighted total bar                      │
 *   └─────────────────────────────────────────────┘
 *
 * Integration in StudentDashboard:
 *   contentArea.add(new StudentMarksPanel(username), BorderLayout.CENTER);
 */
public class StudentMarksPanel extends JPanel {

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String              username;
    private final StudentMarksService service;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JLabel cgpaValueLabel;
    private JLabel cgpaSubLabel;
    private JPanel cardsPanel;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public StudentMarksPanel(String username) {
        this.username = username;
        this.service  = new StudentMarksService();
        init();
    }

    public StudentMarksPanel(String username, StudentMarksService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildScrollArea(), BorderLayout.CENTER);

        loadData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PAGE HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        // Title block
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Marks & Grades");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Your marks breakdown for the current semester");
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

        header.add(titleBlock, BorderLayout.CENTER);
        header.add(buildCGPATile(), BorderLayout.EAST);
        return header;
    }

    // ── CGPA summary tile ─────────────────────────────────────────────────────
    private JPanel buildCGPATile() {
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Gold gradient background
                GradientPaint gp = new GradientPaint(
                    0, 0, AppTheme.GOLD,
                    getWidth(), getHeight(), AppTheme.GOLD_DARK);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createEmptyBorder(16, 28, 16, 28));
        tile.setPreferredSize(new Dimension(180, 90));

        cgpaValueLabel = new JLabel("—");
        cgpaValueLabel.setFont(AppTheme.titleFont(32));
        cgpaValueLabel.setForeground(AppTheme.NAVY);
        cgpaValueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cgpaSubLabel = new JLabel("CGPA");
        cgpaSubLabel.setFont(AppTheme.headingFont(12));
        cgpaSubLabel.setForeground(AppTheme.NAVY);
        cgpaSubLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cgpaNote = new JLabel("Current Semester");
        cgpaNote.setFont(AppTheme.bodyFont(10));
        cgpaNote.setForeground(new Color(60, 40, 0));
        cgpaNote.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(cgpaValueLabel);
        tile.add(Box.createVerticalStrut(2));
        tile.add(cgpaSubLabel);
        tile.add(cgpaNote);
        return tile;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SCROLLABLE CARDS AREA
    // ─────────────────────────────────────────────────────────────────────────
    private JScrollPane buildScrollArea() {
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(cardsPanel, BorderLayout.NORTH);

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
    private JPanel buildCourseCard(StudentMarksRecord record) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Left accent in grade colour
                Color gc = StudentMarksService.gradeColor(record.getLetterGrade());
                g2.setColor(gc);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // ── TOP ROW: course info + grade badge ───────────────────────────────
        JPanel topRow = new JPanel(new BorderLayout(16, 0));
        topRow.setOpaque(false);

        // Course info
        JPanel courseInfo = new JPanel();
        courseInfo.setOpaque(false);
        courseInfo.setLayout(new BoxLayout(courseInfo, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(record.getCourseName());
        nameLbl.setFont(AppTheme.titleFont(16));
        nameLbl.setForeground(AppTheme.NAVY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        metaRow.add(codeBadge(record.getCourseCode()));

        JLabel sectionLbl = new JLabel("Section " + record.getSection());
        sectionLbl.setFont(AppTheme.bodyFont(11));
        sectionLbl.setForeground(AppTheme.TEXT_MUTED);
        metaRow.add(sectionLbl);

        JLabel teacherLbl = new JLabel("👨‍🏫  " + record.getTeacherName());
        teacherLbl.setFont(AppTheme.bodyFont(11));
        teacherLbl.setForeground(AppTheme.TEXT_MUTED);
        metaRow.add(teacherLbl);

        courseInfo.add(nameLbl);
        courseInfo.add(Box.createVerticalStrut(4));
        courseInfo.add(metaRow);

        topRow.add(courseInfo, BorderLayout.CENTER);
        topRow.add(buildGradeBadge(record), BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);

        // ── COMPONENTS TABLE ─────────────────────────────────────────────────
        if (!record.getComponents().isEmpty()) {
            JPanel divider = new JPanel();
            divider.setOpaque(false);
            divider.setPreferredSize(new Dimension(0, 14));
            card.add(divider, BorderLayout.CENTER);

            card.add(buildComponentsTable(record), BorderLayout.SOUTH);
        } else {
            JLabel noMarks = new JLabel("No marks uploaded yet for this course.");
            noMarks.setFont(AppTheme.bodyFont(12));
            noMarks.setForeground(AppTheme.TEXT_MUTED);
            noMarks.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
            card.add(noMarks, BorderLayout.SOUTH);
        }

        return card;
    }

    // ── Grade badge ───────────────────────────────────────────────────────────
    private JPanel buildGradeBadge(StudentMarksRecord record) {
        Color gc = StudentMarksService.gradeColor(record.getLetterGrade());

        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(gc);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        badge.setPreferredSize(new Dimension(110, 72));

        JLabel gradeLbl = new JLabel(record.getLetterGrade());
        gradeLbl.setFont(AppTheme.titleFont(26));
        gradeLbl.setForeground(Color.WHITE);
        gradeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        String pctText = record.getLetterGrade().equals("N/A")
            ? "No marks yet"
            : String.format("%.1f%%", record.getPercentage());
        JLabel pctLbl = new JLabel(pctText);
        pctLbl.setFont(AppTheme.bodyFont(11));
        pctLbl.setForeground(new Color(255, 255, 255, 200));
        pctLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        badge.add(gradeLbl);
        badge.add(pctLbl);
        return badge;
    }

    // ── Components breakdown table ────────────────────────────────────────────
    private JPanel buildComponentsTable(StudentMarksRecord record) {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Header row
        container.add(buildTableRow(
            "Assessment", "Obtained", "Total", "Percentage", true));
        container.add(Box.createVerticalStrut(2));

        // Component rows
        for (MarksItem m : record.getComponents()) {
            String pct = String.format("%.1f%%", m.getPercentage());
            container.add(buildTableRow(
                capitalize(m.getAssessmentType()),
                formatNum(m.getMarksObtained()),
                formatNum(m.getTotalMarks()),
                pct, false));
            container.add(Box.createVerticalStrut(3));
        }

        // Separator
        container.add(Box.createVerticalStrut(4));
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(AppTheme.LIGHT_BLUE);
                g.fillRect(0, 0, getWidth(), 1);
            }
            @Override public Dimension getPreferredSize() {
                return new Dimension(Integer.MAX_VALUE, 1);
            }
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, 1);
            }
        };
        sep.setOpaque(false);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(sep);
        container.add(Box.createVerticalStrut(4));

        // Totals row
        String totalPct = record.getTotalPossible() > 0
            ? String.format("%.1f%%", record.getPercentage()) : "—";
        JPanel totalRow = buildTableRow(
            "TOTAL",
            formatNum(record.getTotalObtained()),
            formatNum(record.getTotalPossible()),
            totalPct, false);

        // Bold the total row labels
        for (Component c : totalRow.getComponents()) {
            if (c instanceof JLabel lbl) {
                lbl.setFont(AppTheme.headingFont(12));
                lbl.setForeground(AppTheme.NAVY);
            }
        }
        container.add(totalRow);

        // Progress bar
        if (record.getTotalPossible() > 0) {
            container.add(Box.createVerticalStrut(8));
            container.add(buildProgressBar(record.getPercentage(),
                StudentMarksService.gradeColor(record.getLetterGrade())));
        }

        return container;
    }

    private JPanel buildTableRow(String assessment, String obtained,
                                  String total, String pct, boolean isHeader) {
        JPanel row = new JPanel(new GridLayout(1, 4, 8, 0));
        row.setOpaque(false);
        if (isHeader) {
            row.setBackground(AppTheme.PALE_BLUE);
            row.setOpaque(true);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        } else {
            row.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isHeader ? 32 : 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color fg = isHeader ? AppTheme.DEEP_BLUE : AppTheme.TEXT_DARK;
        Font  f  = isHeader ? AppTheme.headingFont(11) : AppTheme.bodyFont(12);

        for (String text : new String[]{assessment, obtained, total, pct}) {
            JLabel lbl = new JLabel(text);
            lbl.setFont(f);
            lbl.setForeground(fg);
            row.add(lbl);
        }
        return row;
    }

    private JPanel buildProgressBar(double percentage, Color barColor) {
        JPanel barWrapper = new JPanel(new BorderLayout(6, 0));
        barWrapper.setOpaque(false);
        barWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        barWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel track = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Track
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Fill
                int fillW = (int) (getWidth() * Math.min(percentage / 100.0, 1.0));
                if (fillW > 0) {
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 0, fillW, getHeight(), 8, 8);
                }
                g2.dispose();
            }
        };
        track.setPreferredSize(new Dimension(0, 10));

        JLabel pctLbl = new JLabel(String.format("%.1f%%", percentage));
        pctLbl.setFont(AppTheme.bodyFont(10));
        pctLbl.setForeground(AppTheme.TEXT_MUTED);
        pctLbl.setPreferredSize(new Dimension(45, 10));

        barWrapper.add(track,  BorderLayout.CENTER);
        barWrapper.add(pctLbl, BorderLayout.EAST);
        return barWrapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadData() {
        new SwingWorker<List<StudentMarksRecord>, Void>() {
            @Override
            protected List<StudentMarksRecord> doInBackground() {
                if (!service.init(username)) return List.of();
                return service.getMarksRecords();
            }

            @Override
            protected void done() {
                try {
                    List<StudentMarksRecord> records = get();
                    cardsPanel.removeAll();

                    if (records.isEmpty()) {
                        JLabel empty = new JLabel(
                            "No enrolled courses or marks found for this semester.");
                        empty.setFont(AppTheme.bodyFont(13));
                        empty.setForeground(AppTheme.TEXT_MUTED);
                        empty.setHorizontalAlignment(SwingConstants.CENTER);
                        cardsPanel.add(empty);
                        cgpaValueLabel.setText("—");
                        cgpaSubLabel.setText("No data");
                    } else {
                        // Update CGPA tile
                        double cgpa = service.computeCGPA(records);
                        cgpaValueLabel.setText(cgpa > 0
                            ? String.format("%.2f", cgpa) : "—");

                        // Build course cards
                        for (StudentMarksRecord r : records) {
                            cardsPanel.add(buildCourseCard(r));
                            cardsPanel.add(Box.createVerticalStrut(14));
                        }
                    }

                    cardsPanel.revalidate();
                    cardsPanel.repaint();

                } catch (Exception ex) {
                    JLabel err = new JLabel("Error loading marks: " + ex.getMessage());
                    err.setFont(AppTheme.bodyFont(12));
                    err.setForeground(Color.RED);
                    cardsPanel.add(err);
                    cardsPanel.revalidate();
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel codeBadge(String code) {
        JLabel badge = new JLabel(code) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        badge.setFont(AppTheme.headingFont(11));
        badge.setForeground(AppTheme.DEEP_BLUE);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        badge.setOpaque(false);
        return badge;
    }

    private String formatNum(double val) {
        // Show as integer if whole number, else 1 decimal
        return val == Math.floor(val)
            ? String.valueOf((int) val)
            : String.format("%.1f", val);
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}