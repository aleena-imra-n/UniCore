package ui;

import bl.AdminReportsService;
import bl.AdminReportsService.FilterOption;
import bl.AdminReportsService.ReportType;
import model.AcademicPerformanceRow;
import model.AttendanceReportRow;
import model.EnrollmentReportRow;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * AdminReportsPanel  —  Admin Reports Dashboard
 *
 * Three tabs, one per report:
 *   1. Enrollment Report   — students per course per semester
 *   2. Academic Performance — avg/min/max % per course
 *   3. Attendance Summary   — avg attendance % + students below 75%
 *
 * Each tab has semester + department filter dropdowns, a "Generate" button,
 * a sortable results table, and a summary footer row.
 *
 * Integration in AdminDashboard.onMenuClick():
 *   case "Reports":
 *       contentArea.removeAll();
 *       contentArea.add(new AdminReportsPanel(), BorderLayout.CENTER);
 *       contentArea.revalidate(); contentArea.repaint();
 *       break;
 */
public class AdminReportsPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG_PANEL   = new Color(245, 249, 255);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BG_HEADER  = new Color(232, 244, 253);
    private static final Color BORDER_CLR = new Color(200, 220, 240);
    private static final Color GREEN_FG   = new Color(46, 125, 50);
    private static final Color AMBER_FG   = new Color(180, 100, 0);
    private static final Color RED_FG     = new Color(198, 40, 40);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final AdminReportsService service;

    // ── Shared filter data ────────────────────────────────────────────────────
    private List<FilterOption> semesterOptions;
    private List<FilterOption> deptOptions;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public AdminReportsPanel() { this(new AdminReportsService()); }

    public AdminReportsPanel(AdminReportsService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        // Pre-load filter options once (shared across all three tabs)
        semesterOptions = service.getSemesterOptions();
        deptOptions     = service.getDeptOptions();

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
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

        JLabel title = new JLabel("Admin Reports");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Enrollment, Academic Performance, and Attendance summaries");
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
    //  TABS
    // ─────────────────────────────────────────────────────────────────────────
    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(AppTheme.headingFont(13));
        tabs.setBackground(BG_PANEL);
        tabs.setOpaque(false);

        tabs.addTab("📋  Enrollment",          buildEnrollmentTab());
        tabs.addTab("🎓  Academic Performance", buildAcademicTab());
        tabs.addTab("📅  Attendance",           buildAttendanceTab());

        return tabs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TAB 1: ENROLLMENT REPORT
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildEnrollmentTab() {
        String[]   cols   = {"Semester", "Department", "Code", "Course Name",
                             "Section", "Teacher", "Total", "Active", "Withdrawn", "Completed"};
        int[]      widths = {100, 120, 70, 200, 60, 150, 55, 55, 75, 80};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable     table   = buildStyledTable(model, widths);
        JLabel     footer  = buildFooterLabel();
        JLabel     status  = buildStatusLabel();

        JPanel filterBar = buildFilterBar(
            "Generate Enrollment Report",
            (semFilter, deptFilter) -> {
                model.setRowCount(0);
                List<EnrollmentReportRow> rows =
                    service.loadEnrollmentReport(semFilter, deptFilter);
                if (rows.isEmpty()) {
                    status.setText("No data found for the selected filters.");
                    status.setForeground(AMBER_FG);
                    footer.setText("");
                    return;
                }
                int totalStudents = 0;
                for (EnrollmentReportRow r : rows) {
                    model.addRow(new Object[]{
                        r.getSemesterName(), r.getDeptName(),
                        r.getCourseCode(),   r.getCourseName(),
                        r.getSection(),      r.getTeacherName(),
                        r.getTotalEnrolled(), r.getActiveCount(),
                        r.getWithdrawnCount(), r.getCompletedCount()
                    });
                    totalStudents += r.getTotalEnrolled();
                }
                status.setText("✅  Loaded " + rows.size() + " course(s).");
                status.setForeground(GREEN_FG);
                footer.setText("Total enrolments across all courses: " + totalStudents);
            });

        return assembleTabPanel(filterBar, table, status, footer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TAB 2: ACADEMIC PERFORMANCE REPORT
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildAcademicTab() {
        String[]   cols   = {"Semester", "Department", "Code", "Course Name",
                             "Section", "Students", "Avg %", "Min %", "Max %"};
        int[]      widths = {100, 120, 70, 200, 60, 70, 65, 65, 65};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table  = buildStyledTable(model, widths);
        JLabel footer = buildFooterLabel();
        JLabel status = buildStatusLabel();

        // Custom renderer: colour avg % cell green/amber/red
        table.getColumnModel().getColumn(6).setCellRenderer(
            new PctCellRenderer(50, 75));

        JPanel filterBar = buildFilterBar(
            "Generate Performance Report",
            (semFilter, deptFilter) -> {
                model.setRowCount(0);
                List<AcademicPerformanceRow> rows =
                    service.loadAcademicReport(semFilter, deptFilter);
                if (rows.isEmpty()) {
                    status.setText("No data found for the selected filters.");
                    status.setForeground(AMBER_FG);
                    footer.setText("");
                    return;
                }
                double sumAvg = 0;
                for (AcademicPerformanceRow r : rows) {
                    model.addRow(new Object[]{
                        r.getSemesterName(), r.getDeptName(),
                        r.getCourseCode(),   r.getCourseName(),
                        r.getSection(),      r.getStudentCount(),
                        String.format("%.1f%%", r.getAvgPercentage()),
                        String.format("%.1f%%", r.getMinPercentage()),
                        String.format("%.1f%%", r.getMaxPercentage())
                    });
                    sumAvg += r.getAvgPercentage();
                }
                double overallAvg = rows.isEmpty() ? 0 : sumAvg / rows.size();
                status.setText("✅  Loaded " + rows.size() + " course(s).");
                status.setForeground(GREEN_FG);
                footer.setText(String.format(
                    "Overall average across all courses: %.1f%%", overallAvg));
            });

        return assembleTabPanel(filterBar, table, status, footer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TAB 3: ATTENDANCE REPORT
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildAttendanceTab() {
        String[]   cols   = {"Semester", "Department", "Code", "Course Name",
                             "Section", "Teacher", "Sessions", "Students",
                             "Avg Att %", "Below 75%"};
        int[]      widths = {100, 120, 70, 190, 60, 140, 65, 65, 80, 75};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table  = buildStyledTable(model, widths);
        JLabel footer = buildFooterLabel();
        JLabel status = buildStatusLabel();

        // Colour the "Avg Att %" column
        table.getColumnModel().getColumn(8).setCellRenderer(
            new PctCellRenderer(65, 75));

        JPanel filterBar = buildFilterBar(
            "Generate Attendance Report",
            (semFilter, deptFilter) -> {
                model.setRowCount(0);
                List<AttendanceReportRow> rows =
                    service.loadAttendanceReport(semFilter, deptFilter);
                if (rows.isEmpty()) {
                    status.setText("No data found for the selected filters.");
                    status.setForeground(AMBER_FG);
                    footer.setText("");
                    return;
                }
                int totalBelow = 0;
                for (AttendanceReportRow r : rows) {
                    model.addRow(new Object[]{
                        r.getSemesterName(), r.getDeptName(),
                        r.getCourseCode(),   r.getCourseName(),
                        r.getSection(),      r.getTeacherName(),
                        r.getTotalSessions(), r.getStudentCount(),
                        String.format("%.1f%%", r.getAvgAttendancePct()),
                        r.getStudentsBelowThreshold()
                    });
                    totalBelow += r.getStudentsBelowThreshold();
                }
                status.setText("✅  Loaded " + rows.size() + " course(s).");
                status.setForeground(GREEN_FG);
                footer.setText("Total students below 75% attendance: " + totalBelow);
            });

        return assembleTabPanel(filterBar, table, status, footer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SHARED BUILDERS
    // ─────────────────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface ReportLoader {
        void load(FilterOption semFilter, FilterOption deptFilter);
    }

    /** Builds the filter bar: semester dropdown + dept dropdown + Generate button. */
    private JPanel buildFilterBar(String btnLabel, ReportLoader loader) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 10, 0));

        JComboBox<FilterOption> semCombo  = new JComboBox<>();
        JComboBox<FilterOption> deptCombo = new JComboBox<>();
        semesterOptions.forEach(semCombo::addItem);
        deptOptions.forEach(deptCombo::addItem);

        for (JComboBox<FilterOption> cb : new JComboBox[]{ semCombo, deptCombo }) {
            cb.setFont(AppTheme.bodyFont(12));
            cb.setPreferredSize(new Dimension(180, 36));
            cb.setBackground(Color.WHITE);
        }

        StyledButton genBtn = new StyledButton(btnLabel, AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        genBtn.setPreferredSize(new Dimension(220, 36));
        genBtn.setFont(AppTheme.headingFont(12));
        genBtn.addActionListener(e -> loader.load(
            (FilterOption) semCombo.getSelectedItem(),
            (FilterOption) deptCombo.getSelectedItem()));

        bar.add(new JLabel("Semester:") {{ setFont(AppTheme.bodyFont(12)); setForeground(AppTheme.TEXT_MUTED); }});
        bar.add(semCombo);
        bar.add(new JLabel("Department:") {{ setFont(AppTheme.bodyFont(12)); setForeground(AppTheme.TEXT_MUTED); }});
        bar.add(deptCombo);
        bar.add(genBtn);
        return bar;
    }

    /** Assembles a complete tab: filter bar on top, table in centre, footer at bottom. */
    private JPanel assembleTabPanel(JPanel filterBar, JTable table,
                                     JLabel status, JLabel footer) {
        JPanel tab = new JPanel(new BorderLayout(0, 0));
        tab.setBackground(BG_PANEL);
        tab.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // Top: filter bar + status
        JPanel topBlock = new JPanel();
        topBlock.setOpaque(false);
        topBlock.setLayout(new BoxLayout(topBlock, BoxLayout.Y_AXIS));
        topBlock.add(filterBar);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 0));
        topBlock.add(status);
        tab.add(topBlock, BorderLayout.NORTH);

        // Centre: table in a card
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new RoundedBorder(10, BORDER_CLR, 1));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_CARD);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);

        // Bottom: footer
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(8, 4, 0, 0)));
        tab.add(footer, BorderLayout.SOUTH);
        return tab;
    }

    /** Builds a styled, sortable JTable with the given column widths. */
    private JTable buildStyledTable(DefaultTableModel model, int[] widths) {
        JTable table = new JTable(model);
        table.setFont(AppTheme.bodyFont(12));
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(227, 242, 253));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(AppTheme.headingFont(12));
        header.setBackground(BG_HEADER);
        header.setForeground(AppTheme.TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_CLR));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new AlternatingRowRenderer());

        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < widths.length && i < cm.getColumnCount(); i++)
            cm.getColumn(i).setPreferredWidth(widths[i]);

        return table;
    }

    private JLabel buildFooterLabel() {
        JLabel lbl = new JLabel("Use the filters above and click Generate.");
        lbl.setFont(AppTheme.bodyFont(12));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        return lbl;
    }

    private JLabel buildStatusLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(AppTheme.headingFont(12));
        return lbl;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDERERS
    // ═════════════════════════════════════════════════════════════════════════

    /** Alternating row background renderer. */
    private static class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            setFont(AppTheme.bodyFont(12));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
            if (!sel)
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 252, 255));
            return this;
        }
    }

    /**
     * Colours percentage cells:
     *   >= highThreshold → green
     *   >= lowThreshold  → amber
     *   < lowThreshold   → red
     */
    private class PctCellRenderer extends DefaultTableCellRenderer {
        private final double low, high;
        PctCellRenderer(double low, double high) { this.low = low; this.high = high; }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            setFont(AppTheme.headingFont(12));
            setHorizontalAlignment(CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            if (!sel && value != null) {
                String str = value.toString().replace("%", "").trim();
                try {
                    double pct = Double.parseDouble(str);
                    setForeground(pct >= high ? GREEN_FG
                                : pct >= low  ? AMBER_FG
                                              : RED_FG);
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 252, 255));
                } catch (NumberFormatException ignored) {}
            }
            return this;
        }
    }
}
