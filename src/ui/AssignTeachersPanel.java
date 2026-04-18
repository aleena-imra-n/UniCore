package ui;

import bl.AssignTeachersService;
import bl.AssignTeachersService.ActionResult;
import model.OfferingRow;
import model.SemesterOption;
import model.TeacherOption;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * AssignTeachersPanel  —  UC-18 / US-3.10a
 * Admin panel to assign, reassign, or remove teachers from course offerings.
 *
 * Features:
 *   - Semester filter dropdown (active semester pre-selected)
 *   - Sortable table: code, name, section, department, credits, assigned teacher, action
 *   - Search / filter bar (live filter on code, name, teacher)
 *   - "Assign / Reassign" button per row → dialog with teacher dropdown
 *   - "Remove" button per row (only visible when assigned) → confirmation dialog
 *   - Status badge: green "Assigned" / amber "Unassigned"
 *   - Footer shows total / assigned / unassigned counts
 *   - Auto-refreshes the list after every change
 *
 * Wire-in (AdminDashboard.onMenuClick):
 *   case "Assign Teachers" -> {
 *       contentArea.setBackground(AppTheme.PALE_BLUE);
 *       contentArea.add(new AssignTeachersPanel(username), BorderLayout.CENTER);
 *   }
 */
public class AssignTeachersPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG_PANEL   = new Color(245, 249, 255);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BG_HEADER  = new Color(232, 244, 253);
    private static final Color BORDER_CLR = new Color(200, 220, 240);
    private static final Color GREEN_BG   = new Color(232, 245, 233);
    private static final Color GREEN_FG   = new Color(46, 125, 50);
    private static final Color AMBER_BG   = new Color(255, 248, 225);
    private static final Color AMBER_FG   = new Color(180, 100, 0);
    private static final Color RED_FG     = new Color(198, 40, 40);
    private static final Color BLUE_FG    = new Color(21, 101, 192);

    // ── Table column indices ──────────────────────────────────────────────────
    private static final int COL_CODE    = 0;
    private static final int COL_NAME    = 1;
    private static final int COL_SECTION = 2;
    private static final int COL_DEPT    = 3;
    private static final int COL_CREDITS = 4;
    private static final int COL_TEACHER = 5;
    private static final int COL_STATUS  = 6;
    private static final int COL_ACTION  = 7;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String                adminUsername;
    private final AssignTeachersService service;

    // ── Data ──────────────────────────────────────────────────────────────────
    private List<OfferingRow>    allOfferings;
    private List<TeacherOption>  teachers;
    private List<SemesterOption> semesters;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JTable             table;
    private OfferingTableModel tableModel;
    private JTextField         searchField;
    private JComboBox<SemesterOption> semesterCombo;
    private JLabel             footerLabel;
    private JLabel             statusLabel;
    private JLabel             subtitleLabel;   // shows faculty name after init()

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public AssignTeachersPanel(String adminUsername) {
        this(adminUsername, new AssignTeachersService());
    }

    /** Injection constructor for unit tests. */
    public AssignTeachersPanel(String adminUsername, AssignTeachersService service) {
        this.adminUsername = adminUsername;
        this.service       = service;

        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        // init() MUST run before loadStaticData() — it caches majorDeptId
        // which loadActiveTeachers() depends on via ensureInit().
        if (!service.init(adminUsername)) {
            showStatus("Failed to load admin account. Please re-login.", RED_FG);
            return;
        }
        subtitleLabel.setText(
            "Assigning teachers for the " + service.getDeptName() + " faculty");

        loadStaticData();   // semesters + dept-scoped teachers (once)
        loadOfferings();    // offerings for the selected semester
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER: title + search + semester picker
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        // ── Left: title block ─────────────────────────────────────────────────
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Assign Teachers");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleLabel = new JLabel("Loading…");
        subtitleLabel.setFont(AppTheme.bodyFont(13));
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

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
        titleBlock.add(subtitleLabel);
        titleBlock.add(Box.createVerticalStrut(8));
        titleBlock.add(accent);

        // ── Right: semester picker + search ───────────────────────────────────
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        // Semester label
        JLabel semLabel = new JLabel("Semester:");
        semLabel.setFont(AppTheme.headingFont(12));
        semLabel.setForeground(AppTheme.DEEP_BLUE);

        // Semester combo — populated in loadStaticData()
        semesterCombo = new JComboBox<>();
        semesterCombo.setFont(AppTheme.bodyFont(13));
        semesterCombo.setBackground(Color.WHITE);
        semesterCombo.setForeground(AppTheme.TEXT_DARK);
        semesterCombo.setPreferredSize(new Dimension(220, 36));
        semesterCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        semesterCombo.addActionListener(e -> {
            if (semesterCombo.getSelectedItem() != null) loadOfferings();
        });

        // Search field
        searchField = new JTextField(16);
        searchField.setFont(AppTheme.bodyFont(13));
        searchField.setForeground(AppTheme.TEXT_DARK);
        searchField.putClientProperty("placeholder", "Search code, name or teacher…");
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_CLR, 1),
            BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        rightPanel.add(semLabel);
        rightPanel.add(semesterCombo);
        rightPanel.add(Box.createHorizontalStrut(6));
        rightPanel.add(searchField);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONTENT: status label + table card
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.headingFont(13));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        content.add(statusLabel, BorderLayout.NORTH);

        // Table card
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new RoundedBorder(12, BORDER_CLR, 1));

        tableModel = new OfferingTableModel();
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
        table.setRowHeight(46);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(227, 242, 253));
        table.setSelectionForeground(AppTheme.TEXT_DARK);
        table.setFocusable(false);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(AppTheme.headingFont(12));
        header.setBackground(BG_HEADER);
        header.setForeground(AppTheme.TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_CLR));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));

        // Column widths
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(COL_CODE).setPreferredWidth(80);
        cm.getColumn(COL_NAME).setPreferredWidth(210);
        cm.getColumn(COL_SECTION).setPreferredWidth(65);
        cm.getColumn(COL_DEPT).setPreferredWidth(150);
        cm.getColumn(COL_CREDITS).setPreferredWidth(60);
        cm.getColumn(COL_TEACHER).setPreferredWidth(170);
        cm.getColumn(COL_STATUS).setPreferredWidth(100);
        cm.getColumn(COL_ACTION).setPreferredWidth(160);

        // Sortable on all text columns except Action
        TableRowSorter<OfferingTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortable(COL_STATUS, false);
        sorter.setSortable(COL_ACTION, false);
        table.setRowSorter(sorter);

        // Custom renderers
        table.setDefaultRenderer(Object.class, new RowRenderer());
        cm.getColumn(COL_STATUS).setCellRenderer(new StatusBadgeRenderer());
        cm.getColumn(COL_ACTION).setCellRenderer(new ActionButtonRenderer());
        cm.getColumn(COL_ACTION).setCellEditor(new ActionButtonEditor());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    /** Called once on construction — loads semesters and teachers. */
    private void loadStaticData() {
        semesters = service.loadSemesters();
        teachers  = service.loadActiveTeachers();

        semesterCombo.removeAllItems();
        semesters.forEach(semesterCombo::addItem);

        // Pre-select the active semester
        for (int i = 0; i < semesterCombo.getItemCount(); i++) {
            if (semesterCombo.getItemAt(i).isActive()) {
                semesterCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    /** Loads / reloads offerings for the currently selected semester. */
    private void loadOfferings() {
        SemesterOption sel = (SemesterOption) semesterCombo.getSelectedItem();
        int semId = sel != null ? sel.getSemesterId() : -1;
        allOfferings = service.loadOfferings(semId);
        applyFilter();
        updateFooter();
    }

    private void applyFilter() {
        if (allOfferings == null) return;
        String q = searchField.getText().trim().toLowerCase();
        List<OfferingRow> filtered = allOfferings.stream()
            .filter(o -> q.isEmpty()
                || o.getCourseCode().toLowerCase().contains(q)
                || o.getCourseName().toLowerCase().contains(q)
                || (o.getTeacherName() != null && o.getTeacherName().toLowerCase().contains(q)))
            .toList();
        tableModel.setData(filtered);
        updateFooter();
    }

    private void updateFooter() {
        if (allOfferings == null) return;
        long assigned   = allOfferings.stream().filter(OfferingRow::isAssigned).count();
        long unassigned = allOfferings.size() - assigned;
        footerLabel.setText("Showing " + tableModel.getRowCount()
            + " offering(s)   |   " + assigned   + " assigned"
            + "   |   "           + unassigned + " unassigned");
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        javax.swing.Timer t = new javax.swing.Timer(5000, e -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ASSIGN DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void openAssignDialog(OfferingRow offering) {
        JDialog dlg = createDialog(
            (offering.isAssigned() ? "Reassign Teacher — " : "Assign Teacher — ")
            + offering.getCourseCode() + " (" + offering.getSection() + ")",
            460, 280);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));

        // Info row
        JLabel info = new JLabel("<html><b>" + offering.getCourseCode()
            + "</b>  " + offering.getCourseName()
            + "  —  Section <b>" + offering.getSection() + "</b></html>");
        info.setFont(AppTheme.bodyFont(13));
        info.setForeground(AppTheme.TEXT_DARK);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(info);
        panel.add(Box.createVerticalStrut(6));

        // Current teacher chip
        String currentText = offering.isAssigned()
            ? "Currently: " + offering.getTeacherName()
            : "Currently: Unassigned";
        JLabel current = new JLabel(currentText);
        current.setFont(AppTheme.bodyFont(12));
        current.setForeground(offering.isAssigned() ? GREEN_FG : AMBER_FG);
        current.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(current);
        panel.add(Box.createVerticalStrut(20));

        // Teacher dropdown label
        addFormLabel(panel, "Select Teacher");

        // Teacher combo
        JComboBox<TeacherOption> teacherCombo = new JComboBox<>();
        teachers.forEach(teacherCombo::addItem);
        // Pre-select the current teacher if already assigned
        if (offering.isAssigned()) {
            for (int i = 0; i < teacherCombo.getItemCount(); i++) {
                if (teacherCombo.getItemAt(i).getTeacherId() == offering.getTeacherId()) {
                    teacherCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        styleCombo(teacherCombo);
        panel.add(teacherCombo);
        panel.add(Box.createVerticalStrut(24));

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

        StyledButton assignBtn = new StyledButton(
            offering.isAssigned() ? "Reassign" : "Assign",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        assignBtn.setPreferredSize(new Dimension(130, 40));
        assignBtn.addActionListener(e -> {
            TeacherOption selTeacher = (TeacherOption) teacherCombo.getSelectedItem();
            if (selTeacher == null) return;

            ActionResult result = service.assignTeacher(
                offering.getOfferingId(),
                selTeacher.getTeacherId(),
                selTeacher.getFullName(),
                offering.getCourseCode());

            if (result.success()) {
                dlg.dispose();
                loadOfferings();
                showStatus("✅  " + result.message(), GREEN_FG);
            } else {
                JOptionPane.showMessageDialog(dlg, result.message(),
                    "Assignment Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        btnRow.add(assignBtn);

        panel.add(btnRow);

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        dlg.add(scroll);
        dlg.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REMOVE TEACHER (with confirmation)
    // ─────────────────────────────────────────────────────────────────────────
    private void onRemoveTeacher(OfferingRow offering) {
        int choice = JOptionPane.showConfirmDialog(this,
            "<html>Remove <b>" + offering.getTeacherName()
                + "</b> from <b>" + offering.getCourseCode()
                + " — " + offering.getCourseName()
                + "</b> (" + offering.getSection() + ")?<br>"
                + "The offering will become unassigned.</html>",
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        ActionResult result = service.removeTeacher(
            offering.getOfferingId(),
            choice == JOptionPane.YES_OPTION,
            offering.getCourseCode());

        if (result.success()) {
            loadOfferings();
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

    // ═════════════════════════════════════════════════════════════════════════
    //  TABLE MODEL
    // ═════════════════════════════════════════════════════════════════════════
    private class OfferingTableModel extends AbstractTableModel {

        private static final String[] COLS =
            {"Code", "Course Name", "Section", "Department",
             "Credits", "Assigned Teacher", "Status", "Actions"};

        private List<OfferingRow> rows = List.of();

        void setData(List<OfferingRow> data) {
            this.rows = data;
            fireTableDataChanged();
        }

        OfferingRow getOffering(int row) { return rows.get(row); }

        @Override public int    getRowCount()    { return rows.size(); }
        @Override public int    getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int col) { return COLS[col]; }
        @Override public boolean isCellEditable(int row, int col) {
            return col == COL_ACTION;
        }

        @Override public Object getValueAt(int row, int col) {
            OfferingRow o = rows.get(row);
            return switch (col) {
                case COL_CODE    -> o.getCourseCode();
                case COL_NAME    -> o.getCourseName();
                case COL_SECTION -> o.getSection();
                case COL_DEPT    -> o.getDeptName();
                case COL_CREDITS -> o.getCreditHours() + " cr";
                case COL_TEACHER -> o.isAssigned() ? o.getTeacherName() : "— Unassigned —";
                case COL_STATUS  -> o.isAssigned() ? "Assigned" : "Unassigned";
                case COL_ACTION  -> o.isAssigned() ? "Reassign | Remove" : "Assign";
                default          -> "";
            };
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDERERS
    // ═════════════════════════════════════════════════════════════════════════

    /** Alternating rows + muted style for unassigned offerings. */
    private class RowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            setFont(AppTheme.bodyFont(13));
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));

            if (!sel) {
                int modelRow = t.convertRowIndexToModel(row);
                OfferingRow o = tableModel.getOffering(modelRow);
                setBackground(o.isAssigned()
                    ? (row % 2 == 0 ? BG_CARD : new Color(249, 252, 255))
                    : new Color(255, 253, 245));
                setForeground(o.isAssigned()
                    ? (col == COL_TEACHER ? BLUE_FG : AppTheme.TEXT_DARK)
                    : (col == COL_TEACHER ? AMBER_FG : AppTheme.TEXT_MUTED));
            }
            if (col == COL_CODE) setFont(AppTheme.headingFont(13));
            return this;
        }
    }

    /** Green "Assigned" / Amber "Unassigned" pill badge. */
    private class StatusBadgeRenderer extends JLabel implements TableCellRenderer {
        StatusBadgeRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            String status  = value == null ? "" : value.toString();
            boolean assigned = "Assigned".equals(status);
            setText(status);
            setFont(AppTheme.headingFont(11));
            setForeground(assigned ? GREEN_FG : AMBER_FG);
            setBackground(sel ? t.getSelectionBackground()
                              : assigned ? GREEN_BG : AMBER_BG);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return this;
        }
    }

    /** Action cell renderer — shows one or two rendered buttons. */
    private class ActionButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton assignBtn;
        private final JButton removeBtn;

        ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 4, 8));
            setOpaque(true);

            assignBtn = createBtn("Assign", AppTheme.MID_BLUE);
            removeBtn = createBtn("Remove", new Color(198, 40, 40));
            add(assignBtn);
            add(removeBtn);
        }

        private JButton createBtn(String label, Color bg) {
            JButton b = new JButton(label) {
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
            b.setForeground(Color.WHITE);
            b.setFont(AppTheme.headingFont(11));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setPreferredSize(new Dimension(72, 28));
            return b;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            int modelRow = t.convertRowIndexToModel(row);
            OfferingRow o = tableModel.getOffering(modelRow);
            boolean assigned = o.isAssigned();

            assignBtn.setText(assigned ? "Reassign" : "Assign");
            assignBtn.setPreferredSize(new Dimension(assigned ? 90 : 72, 28));
            
            removeBtn.setVisible(assigned);

            setBackground(sel ? t.getSelectionBackground()
                              : (row % 2 == 0 ? BG_CARD : new Color(249, 252, 255)));
            return this;
        }
    }

    /** Clickable action cell editor — dispatches assign / remove. */
    private class ActionButtonEditor extends DefaultCellEditor {
        private final JPanel    panel;
        private final JButton   assignBtn;
        private final JButton   removeBtn;
        private OfferingRow     currentOffering;

        ActionButtonEditor() {
            super(new JCheckBox());
            setClickCountToStart(1);

            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 8));
            panel.setOpaque(true);

            assignBtn = createEditorBtn("Assign", AppTheme.MID_BLUE);
            removeBtn = createEditorBtn("Remove", new Color(198, 40, 40));

            assignBtn.addActionListener(e -> {
                fireEditingStopped();
                openAssignDialog(currentOffering);
            });
            removeBtn.addActionListener(e -> {
                fireEditingStopped();
                onRemoveTeacher(currentOffering);
            });

            panel.add(assignBtn);
            panel.add(removeBtn);
        }

        private JButton createEditorBtn(String label, Color bg) {
            JButton b = new JButton(label) {
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
            b.setForeground(Color.WHITE);
            b.setFont(AppTheme.headingFont(11));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setPreferredSize(new Dimension(72, 28));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object value,
                boolean sel, int row, int col) {
            int modelRow = t.convertRowIndexToModel(row);
            currentOffering = tableModel.getOffering(modelRow);
            boolean assigned = currentOffering.isAssigned();

            assignBtn.setText(assigned ? "Reassign" : "Assign");
            assignBtn.setPreferredSize(new Dimension(assigned ? 82 : 72, 28));
            removeBtn.setVisible(assigned);

            panel.setBackground(t.getSelectionBackground());
            return panel;
        }

        @Override public Object getCellEditorValue() { return ""; }
    }
}