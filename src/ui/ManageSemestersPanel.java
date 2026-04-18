package ui;

import bl.ManageSemestersService;
import bl.ManageSemestersService.ActionResult;
import model.SemesterRow;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * ManageSemestersPanel  —  UC-20 / US-3.12a
 * Admin panel for semester lifecycle management.
 *
 * Features:
 *   - Sortable table: name, start date, end date, active status, enrollment status, actions
 *   - "Create Semester" button → dialog (name, start date, end date)
 *   - "Set Active" button per row → confirmation → deactivates all, activates target
 *   - "Open / Close Enrollment" toggle button per row (only enabled on active semester)
 *   - Status badges: Active/Inactive  +  Open/Closed enrollment pills
 *   - Footer shows total / active / open counts
 *   - Auto-refreshes after every change
 *
 * Wire-in (AdminDashboard.onMenuClick):
 *   case "Manage Semesters" -> {
 *       contentArea.setBackground(AppTheme.PALE_BLUE);
 *       contentArea.add(new ManageSemestersPanel(username), BorderLayout.CENTER);
 *   }
 */
public class ManageSemestersPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG_PANEL    = new Color(245, 249, 255);
    private static final Color BG_CARD     = Color.WHITE;
    private static final Color BG_HEADER   = new Color(232, 244, 253);
    private static final Color BORDER_CLR  = new Color(200, 220, 240);
    private static final Color GREEN_BG    = new Color(232, 245, 233);
    private static final Color GREEN_FG    = new Color(46, 125, 50);
    private static final Color RED_BG      = new Color(255, 235, 238);
    private static final Color RED_FG      = new Color(198, 40, 40);
    private static final Color AMBER_BG    = new Color(255, 248, 225);
    private static final Color AMBER_FG    = new Color(180, 100, 0);
    private static final Color TEAL_BG     = new Color(224, 242, 241);
    private static final Color TEAL_FG     = new Color(0, 105, 92);

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter INPUT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Table column indices ──────────────────────────────────────────────────
    private static final int COL_NAME       = 0;
    private static final int COL_START      = 1;
    private static final int COL_END        = 2;
    private static final int COL_ACTIVE     = 3;
    private static final int COL_ENROLLMENT = 4;
    private static final int COL_ACTIONS    = 5;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final ManageSemestersService service;

    // ── Data ──────────────────────────────────────────────────────────────────
    private List<SemesterRow> allSemesters;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JTable            table;
    private SemesterTableModel tableModel;
    private JLabel            footerLabel;
    private JLabel            statusLabel;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────
    public ManageSemestersPanel(String username) {
        this(new ManageSemestersService());
    }

    public ManageSemestersPanel(ManageSemestersService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        loadData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        // ── Left: title block ─────────────────────────────────────────────────
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Manage Semesters");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Create semesters and control enrollment status");
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

        // ── Right: create button ───────────────────────────────────────────────
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        StyledButton createBtn = new StyledButton("+ Create Semester",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        createBtn.setPreferredSize(new Dimension(170, 38));
        createBtn.setFont(AppTheme.headingFont(13));
        createBtn.addActionListener(e -> openCreateDialog());

        rightPanel.add(createBtn);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONTENT
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.headingFont(13));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        content.add(statusLabel, BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new RoundedBorder(12, BORDER_CLR, 1));

        tableModel = new SemesterTableModel();
        table = new JTable(tableModel);
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_CARD);
        card.add(scroll, BorderLayout.CENTER);

        content.add(card, BorderLayout.CENTER);
        return content;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FOOTER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(10, 2, 0, 0)));

        footerLabel = new JLabel("Loading…");
        footerLabel.setFont(AppTheme.bodyFont(12));
        footerLabel.setForeground(AppTheme.TEXT_MUTED);
        footer.add(footerLabel, BorderLayout.WEST);
        return footer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TABLE STYLING
    // ─────────────────────────────────────────────────────────────────────────
    private void styleTable() {
        table.setFont(AppTheme.bodyFont(13));
        table.setRowHeight(48);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(227, 242, 253));
        table.setSelectionForeground(AppTheme.TEXT_DARK);
        table.setFocusable(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(AppTheme.headingFont(12));
        header.setBackground(BG_HEADER);
        header.setForeground(AppTheme.TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_CLR));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(COL_NAME).setPreferredWidth(200);
        cm.getColumn(COL_START).setPreferredWidth(110);
        cm.getColumn(COL_END).setPreferredWidth(110);
        cm.getColumn(COL_ACTIVE).setPreferredWidth(90);
        cm.getColumn(COL_ENROLLMENT).setPreferredWidth(110);
        cm.getColumn(COL_ACTIONS).setPreferredWidth(230);

        TableRowSorter<SemesterTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortable(COL_ACTIVE, false);
        sorter.setSortable(COL_ENROLLMENT, false);
        sorter.setSortable(COL_ACTIONS, false);
        table.setRowSorter(sorter);

        table.setDefaultRenderer(Object.class, new SemesterRowRenderer());
        cm.getColumn(COL_ACTIVE).setCellRenderer(new ActiveBadgeRenderer());
        cm.getColumn(COL_ENROLLMENT).setCellRenderer(new EnrollmentBadgeRenderer());
        cm.getColumn(COL_ACTIONS).setCellRenderer(new ActionRenderer());
        cm.getColumn(COL_ACTIONS).setCellEditor(new ActionEditor());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadData() {
        allSemesters = service.loadSemesters();
        tableModel.setData(allSemesters);
        updateFooter();
    }

    private void updateFooter() {
        if (allSemesters == null) return;
        long active = allSemesters.stream().filter(SemesterRow::isActive).count();
        long open   = allSemesters.stream().filter(SemesterRow::isEnrollmentOpen).count();
        footerLabel.setText("Total: " + allSemesters.size()
            + " semester(s)   |   " + active + " active"
            + "   |   " + open + " enrollment open");
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        javax.swing.Timer t = new javax.swing.Timer(5000, e -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void openCreateDialog() {
        JDialog dlg = createDialog("Create New Semester", 460, 360);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));

        // Semester Name
        addFormLabel(panel, "Semester Name");
        JTextField nameField = new StyledTextField("e.g. Spring 2026", 20);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(14));

        // Start Date
        addFormLabel(panel, "Start Date  (yyyy-MM-dd)");
        JTextField startField = new StyledTextField("e.g. 2026-01-20", 20);
        startField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        startField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(startField);
        panel.add(Box.createVerticalStrut(14));

        // End Date
        addFormLabel(panel, "End Date  (yyyy-MM-dd)");
        JTextField endField = new StyledTextField("e.g. 2026-05-30", 20);
        endField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        endField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(endField);
        panel.add(Box.createVerticalStrut(24));

        // Info note
        JLabel note = new JLabel("ℹ  New semesters are created inactive with enrollment closed.");
        note.setFont(AppTheme.bodyFont(11));
        note.setForeground(AppTheme.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(note);
        panel.add(Box.createVerticalStrut(20));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton cancelBtn = new StyledButton("Cancel",
            new Color(230, 230, 230), new Color(210, 210, 210));
        cancelBtn.setForeground(AppTheme.TEXT_DARK);
        cancelBtn.setPreferredSize(new Dimension(100, 40));
        cancelBtn.addActionListener(e -> dlg.dispose());
        btnRow.add(cancelBtn);

        StyledButton createBtn = new StyledButton("Create Semester",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        createBtn.setPreferredSize(new Dimension(160, 40));
        createBtn.addActionListener(e -> {
            LocalDate startDate = parseDate(startField.getText(), dlg, "Start Date");
            if (startDate == null) return;
            LocalDate endDate   = parseDate(endField.getText(),   dlg, "End Date");
            if (endDate == null) return;

            ActionResult result = service.createSemester(
                nameField.getText(), startDate, endDate);

            if (result.success()) {
                dlg.dispose();
                loadData();
                showStatus("✅  " + result.message(), GREEN_FG);
            } else {
                JOptionPane.showMessageDialog(dlg, result.message(),
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        btnRow.add(createBtn);

        panel.add(btnRow);

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        dlg.add(scroll);
        dlg.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SET ACTIVE
    // ─────────────────────────────────────────────────────────────────────────
    private void onSetActive(SemesterRow semester) {
        if (semester.isActive()) {
            JOptionPane.showMessageDialog(this,
                "\"" + semester.getSemesterName() + "\" is already the active semester.",
                "Already Active", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
            "<html>Set <b>" + semester.getSemesterName()
                + "</b> as the active semester?<br>"
                + "All other semesters will be deactivated and<br>"
                + "their enrollment will be <b>closed</b>.</html>",
            "Confirm Set Active",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        ActionResult result = service.setActiveSemester(
            semester.getSemesterId(),
            semester.getSemesterName(),
            choice == JOptionPane.YES_OPTION);

        if (result.success()) {
            loadData();
            showStatus("✅  " + result.message(), GREEN_FG);
        } else if (choice == JOptionPane.YES_OPTION) {
            showStatus("✖  " + result.message(), RED_FG);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TOGGLE ENROLLMENT
    // ─────────────────────────────────────────────────────────────────────────
    private void onToggleEnrollment(SemesterRow semester) {
        boolean willOpen = !semester.isEnrollmentOpen();
        String action = willOpen ? "Open" : "Close";

        if (!semester.isActive()) {
            JOptionPane.showMessageDialog(this,
                "Enrollment can only be changed for the active semester.",
                "Not Active", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
            "<html>" + action + " enrollment for <b>"
                + semester.getSemesterName() + "</b>?<br>"
                + (willOpen
                    ? "Students will be able to register for courses."
                    : "Students will <b>not</b> be able to register for courses.")
                + "</html>",
            "Confirm " + action + " Enrollment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        ActionResult result = service.toggleEnrollment(
            semester.getSemesterId(),
            semester.getSemesterName(),
            semester.isActive(),
            willOpen,
            choice == JOptionPane.YES_OPTION);

        if (result.success()) {
            loadData();
            showStatus("✅  " + result.message(), GREEN_FG);
        } else if (choice == JOptionPane.YES_OPTION) {
            showStatus("✖  " + result.message(), RED_FG);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JDialog createDialog(String title, int w, int h) {
        JDialog dlg = new JDialog(
            SwingUtilities.getWindowAncestor(this), title,
            Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(w, h);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(BG_CARD);
        return dlg;
    }

    private void addFormLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(5));
    }

    /** Parses a yyyy-MM-dd string, shows a dialog on error, returns null on failure. */
    private LocalDate parseDate(String text, JDialog parent, String fieldName) {
        try {
            return LocalDate.parse(text.trim(), INPUT_FMT);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(parent,
                fieldName + " must be in yyyy-MM-dd format  (e.g. 2026-01-20).",
                "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private String fmt(LocalDate d) {
        return d == null ? "—" : d.format(DATE_FMT);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TABLE MODEL
    // ═════════════════════════════════════════════════════════════════════════
    private class SemesterTableModel extends AbstractTableModel {

        private static final String[] COLS =
            {"Semester Name", "Start Date", "End Date",
             "Status", "Enrollment", "Actions"};

        private List<SemesterRow> rows = List.of();

        void setData(List<SemesterRow> data) {
            this.rows = data;
            fireTableDataChanged();
        }

        SemesterRow getSemester(int row) { return rows.get(row); }

        @Override public int    getRowCount()    { return rows.size(); }
        @Override public int    getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int col) { return COLS[col]; }
        @Override public boolean isCellEditable(int row, int col) {
            return col == COL_ACTIONS;
        }

        @Override public Object getValueAt(int row, int col) {
            SemesterRow s = rows.get(row);
            return switch (col) {
                case COL_NAME       -> s.getSemesterName();
                case COL_START      -> fmt(s.getStartDate());
                case COL_END        -> fmt(s.getEndDate());
                case COL_ACTIVE     -> s.isActive() ? "Active" : "Inactive";
                case COL_ENROLLMENT -> s.isEnrollmentOpen() ? "Open" : "Closed";
                case COL_ACTIONS    -> "";  // handled by renderer/editor
                default             -> "";
            };
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDERERS
    // ═════════════════════════════════════════════════════════════════════════

    private class SemesterRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            setFont(AppTheme.bodyFont(13));
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
            if (!sel) {
                int mr = t.convertRowIndexToModel(row);
                SemesterRow s = tableModel.getSemester(mr);
                setBackground(s.isActive()
                    ? new Color(240, 249, 255)
                    : (row % 2 == 0 ? BG_CARD : new Color(249, 252, 255)));
                setForeground(AppTheme.TEXT_DARK);
            }
            if (col == COL_NAME) setFont(AppTheme.headingFont(13));
            return this;
        }
    }

    /** Green "Active" / Grey "Inactive" badge. */
    private class ActiveBadgeRenderer extends JLabel implements TableCellRenderer {
        ActiveBadgeRenderer() { setOpaque(true); setHorizontalAlignment(CENTER); }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            String v = value == null ? "" : value.toString();
            boolean active = "Active".equals(v);
            setText(v);
            setFont(AppTheme.headingFont(11));
            setForeground(active ? GREEN_FG : AppTheme.TEXT_MUTED);
            setBackground(sel ? t.getSelectionBackground() : active ? GREEN_BG : BG_CARD);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return this;
        }
    }

    /** Teal "Open" / Red "Closed" badge. */
    private class EnrollmentBadgeRenderer extends JLabel implements TableCellRenderer {
        EnrollmentBadgeRenderer() { setOpaque(true); setHorizontalAlignment(CENTER); }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            String v = value == null ? "" : value.toString();
            boolean open = "Open".equals(v);
            setText(v);
            setFont(AppTheme.headingFont(11));
            setForeground(open ? TEAL_FG : RED_FG);
            setBackground(sel ? t.getSelectionBackground() : open ? TEAL_BG : RED_BG);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return this;
        }
    }

    /** Renders "Set Active" + "Open/Close Enrollment" buttons per row. */
    private class ActionRenderer extends JPanel implements TableCellRenderer {
        private final JButton setActiveBtn;
        private final JButton enrollBtn;

        ActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 6, 10));
            setOpaque(true);
            setActiveBtn = makeRenderBtn("Set Active",    new Color(21, 101, 192));
            enrollBtn    = makeRenderBtn("Open Enroll",   new Color(0, 105, 92));
            add(setActiveBtn);
            add(enrollBtn);
        }

        private JButton makeRenderBtn(String label, Color bg) {
            JButton b = new JButton(label) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isEnabled() ? bg : new Color(190, 190, 190));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    super.paintComponent(g2);
                    g2.dispose();
                }
            };
            b.setForeground(Color.WHITE);
            b.setFont(AppTheme.headingFont(11));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setPreferredSize(new Dimension(100, 28));
            return b;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            int mr = t.convertRowIndexToModel(row);
            SemesterRow s = tableModel.getSemester(mr);

            setActiveBtn.setEnabled(!s.isActive());
            enrollBtn.setText(s.isEnrollmentOpen() ? "Close Enroll" : "Open Enroll");
            enrollBtn.setEnabled(s.isActive());

            setBackground(sel ? t.getSelectionBackground()
                              : s.isActive() ? new Color(240, 249, 255)
                              : (row % 2 == 0 ? BG_CARD : new Color(249, 252, 255)));
            setActiveBtn.setBackground(new Color(21, 101, 192));
            enrollBtn.setBackground(s.isEnrollmentOpen()
                ? new Color(198, 40, 40) : new Color(0, 105, 92));
            return this;
        }
    }

    /** Clickable action cell editor — dispatches set-active / toggle-enrollment. */
    private class ActionEditor extends DefaultCellEditor {
        private final JPanel  panel;
        private final JButton setActiveBtn;
        private final JButton enrollBtn;
        private SemesterRow   currentSemester;

        ActionEditor() {
            super(new JCheckBox());
            setClickCountToStart(1);

            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 10));
            panel.setOpaque(true);

            setActiveBtn = makeEditorBtn("Set Active",  new Color(21, 101, 192));
            enrollBtn    = makeEditorBtn("Open Enroll", new Color(0, 105, 92));

            setActiveBtn.addActionListener(e -> {
                fireEditingStopped();
                onSetActive(currentSemester);
            });
            enrollBtn.addActionListener(e -> {
                fireEditingStopped();
                onToggleEnrollment(currentSemester);
            });

            panel.add(setActiveBtn);
            panel.add(enrollBtn);
        }

        private JButton makeEditorBtn(String label, Color bg) {
            JButton b = new JButton(label) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isEnabled() ? bg : new Color(190, 190, 190));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    super.paintComponent(g2);
                    g2.dispose();
                }
            };
            b.setForeground(Color.WHITE);
            b.setFont(AppTheme.headingFont(11));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setPreferredSize(new Dimension(100, 28));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object value,
                boolean sel, int row, int col) {
            int mr = t.convertRowIndexToModel(row);
            currentSemester = tableModel.getSemester(mr);

            setActiveBtn.setEnabled(!currentSemester.isActive());
            enrollBtn.setText(currentSemester.isEnrollmentOpen()
                ? "Close Enroll" : "Open Enroll");
            enrollBtn.setEnabled(currentSemester.isActive());
            enrollBtn.setBackground(currentSemester.isEnrollmentOpen()
                ? new Color(198, 40, 40) : new Color(0, 105, 92));

            panel.setBackground(t.getSelectionBackground());
            return panel;
        }

        @Override public Object getCellEditorValue() { return ""; }
    }
}
