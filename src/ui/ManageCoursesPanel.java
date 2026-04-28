package ui;

import bl.ManageCoursesService;
import bl.ManageCoursesService.ActionResult;
import model.CategoryOption;
import model.CourseRow;
import model.DeptOption;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * ManageCoursesPanel  —  UC-17 / US-3.9a  (updated)
 *
 * Changes from original:
 *   1. Accepts adminUsername — service.init() scopes ALL data to that admin's faculty.
 *      Computing admin sees only Computing courses, Engineering only Engineering, etc.
 *   2. "Pre-Requisite" column added — shows "No Pre Req" or the prereq course code(s).
 *   3. Department dropdown removed from Add/Edit — admin works only in their faculty.
 *   4. Panel subtitle shows the admin's faculty name dynamically.
 *
 * Integration in AdminDashboard.onMenuClick():
 *   case "Manage Courses":
 *       contentArea.removeAll();
 *       contentArea.add(new ManageCoursesPanel(username), BorderLayout.CENTER);
 *       contentArea.revalidate();
 *       contentArea.repaint();
 *       break;
 */
public class ManageCoursesPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG_PANEL   = new Color(245, 249, 255);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BG_HEADER  = new Color(232, 244, 253);
    private static final Color BORDER_CLR = new Color(200, 220, 240);
    private static final Color GREEN_BG   = new Color(232, 245, 233);
    private static final Color GREEN_FG   = new Color(46, 125, 50);
    private static final Color RED_BG     = new Color(255, 235, 238);
    private static final Color RED_FG     = new Color(198, 40, 40);
    private static final Color AMBER_FG   = new Color(180, 100, 0);

    // ── Table column indices ──────────────────────────────────────────────────
    private static final int COL_CODE     = 0;
    private static final int COL_NAME     = 1;
    private static final int COL_CREDITS  = 2;
    private static final int COL_CATEGORY = 3;
    private static final int COL_PREREQ   = 4;
    private static final int COL_STATUS   = 5;
    private static final int COL_EDIT     = 6;   // ← NEW Edit button
    private static final int COL_ACTION   = 7;   // Deactivate / Reactivate

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String               adminUsername;
    private final ManageCoursesService service;

    // ── Data ──────────────────────────────────────────────────────────────────
    private List<CourseRow>      allCourses;
    private List<CategoryOption> categories;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JTable           table;
    private CourseTableModel tableModel;
    private JTextField       searchField;
    private JLabel           footerLabel;
    private JLabel           statusLabel;
    private JLabel           subtitleLabel;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public ManageCoursesPanel(String adminUsername) {
        this(adminUsername, new ManageCoursesService());
    }

    /** Injection constructor for unit tests. */
    public ManageCoursesPanel(String adminUsername, ManageCoursesService service) {
        this.adminUsername = adminUsername;
        this.service       = service;

        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        if (!service.init(adminUsername)) {
            showStatus("Failed to load admin account. Please re-login.", RED_FG);
        } else {
            subtitleLabel.setText(
                "Managing courses for the " + service.getDeptName() + " faculty");
            loadData();
        }
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

        JLabel title = new JLabel("Manage Course Catalog");
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

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        searchField = new JTextField(18);
        searchField.setFont(AppTheme.bodyFont(13));
        searchField.setForeground(AppTheme.TEXT_DARK);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_CLR, 1),
            BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            });

        StyledButton addBtn = new StyledButton("+ Add Course",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        addBtn.setPreferredSize(new Dimension(140, 38));
        addBtn.setFont(AppTheme.headingFont(13));
        addBtn.addActionListener(e -> openAddDialog());

        rightPanel.add(searchField);
        rightPanel.add(addBtn);

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

        tableModel = new CourseTableModel();
        table      = new JTable(tableModel);
        styleTable();

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = table.getSelectedRow();
                    if (viewRow >= 0) {
                        openEditDialog(tableModel.getCourse(
                            table.convertRowIndexToModel(viewRow)));
                    }
                }
            }
        });

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

        JTableHeader header = table.getTableHeader();
        header.setFont(AppTheme.headingFont(12));
        header.setBackground(BG_HEADER);
        header.setForeground(AppTheme.TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_CLR));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(COL_CODE).setPreferredWidth(80);
        cm.getColumn(COL_NAME).setPreferredWidth(210);
        cm.getColumn(COL_CREDITS).setPreferredWidth(65);
        cm.getColumn(COL_CATEGORY).setPreferredWidth(100);
        cm.getColumn(COL_PREREQ).setPreferredWidth(150);
        cm.getColumn(COL_STATUS).setPreferredWidth(80);
        cm.getColumn(COL_EDIT).setPreferredWidth(70);
        cm.getColumn(COL_ACTION).setPreferredWidth(120);

        TableRowSorter<CourseTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortable(COL_ACTION, false);
        sorter.setSortable(COL_STATUS, false);
        sorter.setSortable(COL_EDIT,   false);
        table.setRowSorter(sorter);

        table.setDefaultRenderer(Object.class,    new CourseRowRenderer());
        cm.getColumn(COL_PREREQ).setCellRenderer( new PrereqCellRenderer());
        cm.getColumn(COL_STATUS).setCellRenderer( new StatusBadgeRenderer());
        cm.getColumn(COL_EDIT).setCellRenderer(   new EditButtonRenderer());
        cm.getColumn(COL_EDIT).setCellEditor(     new EditButtonEditor());
        cm.getColumn(COL_ACTION).setCellRenderer( new ActionButtonRenderer());
        cm.getColumn(COL_ACTION).setCellEditor(   new ActionButtonEditor());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadData() {
        allCourses = service.loadCourses();
        categories = service.loadCategories();
        applyFilter();
        updateFooter();
    }

    private void applyFilter() {
        String q = searchField == null ? ""
                 : searchField.getText().trim().toLowerCase();
        List<CourseRow> filtered = allCourses == null ? List.of()
            : allCourses.stream()
                .filter(c -> q.isEmpty()
                    || c.getCourseCode().toLowerCase().contains(q)
                    || c.getCourseName().toLowerCase().contains(q))
                .toList();
        tableModel.setData(filtered);
        updateFooter();
    }

    private void updateFooter() {
        if (allCourses == null) return;
        long active = allCourses.stream().filter(CourseRow::isActive).count();
        footerLabel.setText("Showing " + tableModel.getRowCount()
            + " course(s)   |   " + active + " active   |   "
            + (allCourses.size() - active) + " inactive");
    }

    private void showStatus(String msg, Color color) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        javax.swing.Timer t = new javax.swing.Timer(5000, e -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADD / EDIT DIALOGS
    // ─────────────────────────────────────────────────────────────────────────
    private void openAddDialog() {
        JDialog dlg = createDialog("Add New Course", 460, 580);
        dlg.add(buildFormPanel(dlg, null));
        dlg.setVisible(true);
    }

    private void openEditDialog(CourseRow course) {
        JDialog dlg = createDialog("Edit Course — " + course.getCourseCode(), 460, 560);
        dlg.add(buildFormPanel(dlg, course));
        dlg.setVisible(true);
    }

    /**
     * Shared Add / Edit form.
     * Department field is intentionally absent — admin scope is locked to their faculty.
     */
    private JPanel buildFormPanel(JDialog dlg, CourseRow existing) {
        boolean isEdit = (existing != null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        // Course Code
        addFormLabel(panel, "Course Code" + (isEdit ? "  (not editable)" : ""));
        JTextField codeField = new StyledTextField(
            isEdit ? existing.getCourseCode() : "e.g. CS401", 20);
        codeField.setText(isEdit ? existing.getCourseCode() : "");
        codeField.setEnabled(!isEdit);
        codeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        codeField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(codeField);
        panel.add(Box.createVerticalStrut(14));

        // Course Name
        addFormLabel(panel, "Course Name");
        JTextField nameField = new StyledTextField("e.g. Software Engineering", 20);
        nameField.setText(isEdit ? existing.getCourseName() : "");
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(14));

        // Credit Hours
        addFormLabel(panel, "Credit Hours  (1–" + ManageCoursesService.MAX_CREDITS + ")");
        SpinnerNumberModel spinModel = new SpinnerNumberModel(
            isEdit ? existing.getCreditHours() : 3,
            ManageCoursesService.MIN_CREDITS,
            ManageCoursesService.MAX_CREDITS, 1);
        JSpinner creditSpinner = new JSpinner(spinModel);
        creditSpinner.setFont(AppTheme.bodyFont(13));
        creditSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        creditSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(creditSpinner);
        panel.add(Box.createVerticalStrut(14));

        // Category
        addFormLabel(panel, "Course Category");
        JComboBox<CategoryOption> catCombo = new JComboBox<>();
        categories.forEach(catCombo::addItem);
        if (isEdit) {
            for (int i = 0; i < catCombo.getItemCount(); i++) {
                if (catCombo.getItemAt(i).getCategoryId() == existing.getCategoryId()) {
                    catCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        styleCombo(catCombo);
        panel.add(catCombo);
        panel.add(Box.createVerticalStrut(20));
     // // Pre-requisite Course
        addFormLabel(panel, "Pre-requisite Course (optional)");

        JComboBox<CourseRow> prereqCombo = new JComboBox<>();

        // Add "None"
        prereqCombo.addItem(null);

        // Populate
        for (CourseRow c : service.loadCourses()) {
            if (!isEdit || c.getCourseId() != existing.getCourseId()) {
                prereqCombo.addItem(c);
            }
        }

        // Set selected in edit mode
        if (isEdit && existing.getPreReqCode() != null && !existing.getPreReqCode().isEmpty()) {
            for (int i = 0; i < prereqCombo.getItemCount(); i++) {
                CourseRow c = prereqCombo.getItemAt(i);
                if (c != null && c.getCourseCode().equals(existing.getPreReqCode())) {
                    prereqCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        styleCombo(prereqCombo);
        panel.add(prereqCombo);
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

        StyledButton saveBtn = new StyledButton(
            isEdit ? "Save Changes" : "Add Course",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        saveBtn.setPreferredSize(new Dimension(140, 40));
        saveBtn.addActionListener(e -> {
            CategoryOption selCat = (CategoryOption) catCombo.getSelectedItem();
            int catId   = selCat != null ? selCat.getCategoryId() : -1;
            int credits = (Integer) creditSpinner.getValue();
            CourseRow selectedPrereq = (CourseRow) prereqCombo.getSelectedItem();
            String prereqCode = (selectedPrereq != null) ? selectedPrereq.getCourseCode() : "";
           
            ActionResult result = isEdit
                ? service.updateCourse(existing.getCourseId(),
                                       nameField.getText(), credits ,catId,prereqCode)
                : service.addCourse(catId, codeField.getText(),
                                    nameField.getText(), credits,prereqCode);

            if (result.success()) {
                dlg.dispose();
                loadData();
                showStatus("✅  " + result.message(), GREEN_FG);
            } else {
                JOptionPane.showMessageDialog(dlg, result.message(),
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        btnRow.add(saveBtn);
        panel.add(btnRow);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DEACTIVATE / REACTIVATE
    // ─────────────────────────────────────────────────────────────────────────
    private void onToggleActive(CourseRow course) {
        if (course.isActive()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "<html>Deactivate <b>" + course.getCourseCode()
                    + " — " + course.getCourseName() + "</b>?<br>"
                    + "Students will no longer see this course in the catalog.</html>",
                "Confirm Deactivation",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            ActionResult result = service.deactivateCourse(
                course.getCourseId(), choice == JOptionPane.YES_OPTION);
            if (result.success())                { loadData(); showStatus("✅  " + result.message(), GREEN_FG); }
            else if (choice == JOptionPane.YES_OPTION) showStatus("✖  " + result.message(), RED_FG);
        } else {
            ActionResult result = service.reactivateCourse(course.getCourseId());
            if (result.success()) { loadData(); showStatus("✅  " + result.message(), GREEN_FG); }
            else                  showStatus("✖  " + result.message(), RED_FG);
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
    private class CourseTableModel extends AbstractTableModel {

        private static final String[] COLS =
            {"Code", "Course Name", "Credits", "Category", "Pre-Requisite", "Status", "Edit", "Action"};

        private List<CourseRow> rows = List.of();

        void setData(List<CourseRow> data) { this.rows = data; fireTableDataChanged(); }
        CourseRow getCourse(int row)       { return rows.get(row); }

        @Override public int     getRowCount()              { return rows.size(); }
        @Override public int     getColumnCount()           { return COLS.length; }
        @Override public String  getColumnName(int col)     { return COLS[col]; }
        @Override public boolean isCellEditable(int r, int c) { return c == COL_ACTION || c == COL_EDIT; }

        @Override public Object getValueAt(int row, int col) {
            CourseRow c = rows.get(row);
            return switch (col) {
                case COL_CODE     -> c.getCourseCode();
                case COL_NAME     -> c.getCourseName();
                case COL_CREDITS  -> c.getCreditHours() + " cr";
                case COL_CATEGORY -> c.getCategoryName();
                case COL_PREREQ   -> c.getPrereqDisplay();
                case COL_STATUS   -> c.isActive() ? "Active" : "Inactive";
                case COL_EDIT     -> "Edit";
                case COL_ACTION   -> c.isActive() ? "Deactivate" : "Reactivate";
                default           -> "";
            };
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDERERS
    // ═════════════════════════════════════════════════════════════════════════

    private class CourseRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            setFont(AppTheme.bodyFont(13));
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
            if (!sel) {
                int modelRow = t.convertRowIndexToModel(row);
                CourseRow course = tableModel.getCourse(modelRow);
                setBackground(course.isActive()
                    ? (row % 2 == 0 ? BG_CARD : new Color(249, 252, 255))
                    : new Color(255, 250, 250));
                setForeground(course.isActive() ? AppTheme.TEXT_DARK : new Color(180, 180, 180));
            }
            if (col == COL_CODE) setFont(AppTheme.headingFont(13));
            return this;
        }
    }

    /**
     * "No Pre Req" → muted italic grey.
     * Actual code(s) like "CS101" or "CS101, CS201" → bold deep-blue.
     */
    private class PrereqCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            String text    = value == null ? "" : value.toString();
            boolean noPreq = "No Pre Req".equals(text);
            setText(text);
            setFont(noPreq ? new Font("SansSerif", Font.ITALIC, 12)
                           : AppTheme.headingFont(12));
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
            if (!sel) {
                int modelRow = t.convertRowIndexToModel(row);
                boolean active = tableModel.getCourse(modelRow).isActive();
                setBackground(active
                    ? (row % 2 == 0 ? BG_CARD : new Color(249, 252, 255))
                    : new Color(255, 250, 250));
                setForeground(noPreq ? AppTheme.TEXT_MUTED
                            : active ? AppTheme.DEEP_BLUE
                                     : new Color(180, 180, 180));
            }
            return this;
        }
    }

    private class StatusBadgeRenderer extends JLabel implements TableCellRenderer {
        StatusBadgeRenderer() { setOpaque(true); setHorizontalAlignment(CENTER); }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            String  status = value == null ? "" : value.toString();
            boolean active = "Active".equals(status);
            setText(status);
            setFont(AppTheme.headingFont(11));
            setForeground(active ? GREEN_FG : RED_FG);
            setBackground(sel ? t.getSelectionBackground()
                              : active ? GREEN_BG : RED_BG);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return this;
        }
    }

    private class EditButtonRenderer extends JButton implements TableCellRenderer {
        EditButtonRenderer() { setOpaque(true); setFocusPainted(false); setBorderPainted(false); }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            setText("Edit");
            setFont(AppTheme.headingFont(11));
            setForeground(Color.WHITE);
            setBackground(AppTheme.MID_BLUE);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            return this;
        }
    }

    private class EditButtonEditor extends DefaultCellEditor {
        private final JButton  btn;
        private       CourseRow currentCourse;

        EditButtonEditor() {
            super(new JCheckBox());
            btn = new JButton("Edit");
            btn.setOpaque(true);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setBackground(AppTheme.MID_BLUE);
            btn.setForeground(Color.WHITE);
            btn.setFont(AppTheme.headingFont(11));
            btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> { fireEditingStopped(); openEditDialog(currentCourse); });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object value,
                boolean sel, int row, int col) {
            int modelRow = t.convertRowIndexToModel(row);
            currentCourse = tableModel.getCourse(modelRow);
            return btn;
        }

        @Override public Object getCellEditorValue() { return "Edit"; }
    }

    private class ActionButtonRenderer extends JButton implements TableCellRenderer {
        ActionButtonRenderer() { setOpaque(true); setFocusPainted(false); setBorderPainted(false); }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            String  label = value == null ? "" : value.toString();
            boolean deact = "Deactivate".equals(label);
            setText(label);
            setFont(AppTheme.headingFont(11));
            setForeground(Color.WHITE);
            setBackground(deact ? RED_FG : GREEN_FG);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            return this;
        }
    }

    private class ActionButtonEditor extends DefaultCellEditor {
        private final JButton  btn;
        private       CourseRow currentCourse;

        ActionButtonEditor() {
            super(new JCheckBox());
            btn = new JButton();
            btn.setOpaque(true);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> { fireEditingStopped(); onToggleActive(currentCourse); });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object value,
                boolean sel, int row, int col) {
            int modelRow = t.convertRowIndexToModel(row);
            currentCourse = tableModel.getCourse(modelRow);
            boolean deact = currentCourse.isActive();
            btn.setText(deact ? "Deactivate" : "Reactivate");
            btn.setFont(AppTheme.headingFont(11));
            btn.setForeground(Color.WHITE);
            btn.setBackground(deact ? RED_FG : GREEN_FG);
            btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            return btn;
        }

        @Override public Object getCellEditorValue() { return btn.getText(); }
    }
}
