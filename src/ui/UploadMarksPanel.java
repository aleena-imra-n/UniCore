package ui;

import bl.MarksService;
import bl.MarksService.SaveResult;
import bl.MarksService.Outcome;
import model.MarksItem;
import model.OfferingItem;
import model.RosterStudent;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UploadMarksPanel — Pure UI layer.
 *
 * What this class does:
 *   - Step 1: Teacher selects a course offering + assessment type
 *   - Step 2: A table loads with all enrolled students
 *   - Teacher fills in Marks Obtained, Total Marks, and optional Remarks per row
 *   - Clicking "Save All Marks" delegates to MarksService for batch validation + DB save
 *   - Existing marks are pre-filled if teacher revisits the same course + assessment
 *
 * What this class does NOT do:
 *   - No SQL
 *   - No business rules
 *   - No direct DAO calls
 *
 * Integration in TeacherDashboard:
 *   contentArea.add(new UploadMarksPanel(username), BorderLayout.CENTER);
 */
public class UploadMarksPanel extends JPanel {

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String       username;
    private final MarksService service;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<RosterStudent>      roster       = new ArrayList<>();
    private Map<Integer, MarksItem>  existingMarks = new HashMap<>(); // enrollmentId → mark

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JComboBox<OfferingItem>  courseDropdown;
    private JComboBox<String>        assessmentDropdown;
    private JLabel                   statusLabel;
    private JPanel                   tableContainer;
    private MarksTableModel          tableModel;
    private JTable                   marksTable;
    private StyledButton             saveBtn;
    private JLabel                   summaryLabel;

    // ── Table column indices ──────────────────────────────────────────────────
    private static final int COL_ROLL     = 0;
    private static final int COL_NAME     = 1;
    private static final int COL_OBTAINED = 2;
    private static final int COL_TOTAL    = 3;
    private static final int COL_REMARKS  = 4;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public UploadMarksPanel(String username) {
        this.username = username;
        this.service  = new MarksService();
        init();
    }

    public UploadMarksPanel(String username, MarksService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildMainArea(),   BorderLayout.CENTER);

        service.init(username);
        loadOfferings();
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

        JLabel title = new JLabel("Upload Marks");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Select a course and assessment type, then enter marks for each student");
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
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MAIN AREA: selector card on top + marks table below
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel area = new JPanel(new BorderLayout(0, 16));
        area.setOpaque(false);
        area.add(buildSelectorCard(), BorderLayout.NORTH);
        area.add(buildTableCard(),    BorderLayout.CENTER);
        return area;
    }

    // ── Selector card ─────────────────────────────────────────────────────────
    private JPanel buildSelectorCard() {
        JPanel card = roundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        // Row: course + assessment + load button
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 14);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.gridy  = 0;

        // Course label + dropdown
        gbc.gridx = 0; gbc.weightx = 0;
        row.add(fieldLabel("Course"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.45;
        courseDropdown = new JComboBox<>();
        courseDropdown.addItem(null);
        courseDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "— Select a course —" : value.toString());
                return this;
            }
        });
        styleDropdown(courseDropdown);
        row.add(courseDropdown, gbc);

        // Assessment label + dropdown
        gbc.gridx = 2; gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 14);
        row.add(fieldLabel("Assessment Type"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.35;
        assessmentDropdown = new JComboBox<>(MarksService.ASSESSMENT_TYPES);
        styleDropdown(assessmentDropdown);
        row.add(assessmentDropdown, gbc);

        // Load button
        gbc.gridx  = 4; gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        StyledButton loadBtn = new StyledButton("Load Students",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        loadBtn.setPreferredSize(new Dimension(150, 40));
        loadBtn.addActionListener(e -> onLoadClicked());
        row.add(loadBtn, gbc);

        card.add(row);
        return card;
    }

    // ── Table card ────────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        // Card header row
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        cardHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel cardTitle = new JLabel("Student Marks");
        cardTitle.setFont(AppTheme.headingFont(15));
        cardTitle.setForeground(AppTheme.NAVY);

        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(AppTheme.bodyFont(12));
        summaryLabel.setForeground(AppTheme.TEXT_MUTED);

        cardHeader.add(cardTitle,    BorderLayout.WEST);
        cardHeader.add(summaryLabel, BorderLayout.EAST);
        card.add(cardHeader, BorderLayout.NORTH);

        // Table container (swapped when loaded)
        tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        // Placeholder shown before load
        JLabel placeholder = new JLabel(
            "Select a course and assessment type, then click Load Students");
        placeholder.setFont(AppTheme.bodyFont(13));
        placeholder.setForeground(AppTheme.TEXT_MUTED);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        tableContainer.add(placeholder, BorderLayout.CENTER);
        card.add(tableContainer, BorderLayout.CENTER);

        // Footer: status + save button
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.headingFont(13));
        footer.add(statusLabel, BorderLayout.WEST);

        saveBtn = new StyledButton("Save All Marks", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        saveBtn.setPreferredSize(new Dimension(180, 42));
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(e -> onSaveClicked());
        footer.add(saveBtn, BorderLayout.EAST);

        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MARKS TABLE
    // ─────────────────────────────────────────────────────────────────────────
    private void buildMarksTable() {
        tableModel = new MarksTableModel(roster);
        marksTable = new JTable(tableModel);

        // Pre-fill existing marks if any
        for (int i = 0; i < roster.size(); i++) {
            int eid = roster.get(i).getEnrollmentId();
            if (existingMarks.containsKey(eid)) {
                MarksItem m = existingMarks.get(eid);
                tableModel.setValueAt(m.getMarksObtained(), i, COL_OBTAINED);
                tableModel.setValueAt(m.getTotalMarks(),    i, COL_TOTAL);
                tableModel.setValueAt(m.getRemarks(),       i, COL_REMARKS);
            }
        }

        styleTable();

        JScrollPane scroll = new JScrollPane(marksTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        tableContainer.removeAll();
        tableContainer.add(scroll, BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();

        saveBtn.setEnabled(true);
        summaryLabel.setText(roster.size() + " students enrolled");
    }

    private void styleTable() {
        marksTable.setFont(AppTheme.bodyFont(13));
        marksTable.setRowHeight(36);
        marksTable.setGridColor(new Color(220, 235, 255));
        marksTable.setSelectionBackground(AppTheme.PALE_BLUE);
        marksTable.setSelectionForeground(AppTheme.TEXT_DARK);
        marksTable.setShowVerticalLines(true);
        marksTable.setShowHorizontalLines(true);
        marksTable.getTableHeader().setFont(AppTheme.headingFont(12));
        marksTable.getTableHeader().setBackground(AppTheme.DEEP_BLUE);
        marksTable.getTableHeader().setForeground(Color.WHITE);
        marksTable.getTableHeader().setPreferredSize(new Dimension(0, 38));

        // Column widths
        marksTable.getColumnModel().getColumn(COL_ROLL).setPreferredWidth(100);
        marksTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(200);
        marksTable.getColumnModel().getColumn(COL_OBTAINED).setPreferredWidth(120);
        marksTable.getColumnModel().getColumn(COL_TOTAL).setPreferredWidth(120);
        marksTable.getColumnModel().getColumn(COL_REMARKS).setPreferredWidth(200);

        // Alternating row colours via renderer
        marksTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(247, 251, 255));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EVENT HANDLERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Called when teacher clicks "Load Students". */
    private void onLoadClicked() {
        OfferingItem offering = (OfferingItem) courseDropdown.getSelectedItem();
        if (offering == null) {
            showStatus("⚠  Please select a course first.", AppTheme.GOLD_DARK);
            return;
        }
        String assessment = (String) assessmentDropdown.getSelectedItem();

        showStatus("Loading…", AppTheme.TEXT_MUTED);
        saveBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                roster = service.getRoster(offering.getOfferingId());
                List<model.MarksItem> existing =
                    service.getExistingMarks(offering.getOfferingId(), assessment);
                existingMarks.clear();
                for (model.MarksItem m : existing) {
                    existingMarks.put(m.getEnrollmentId(), m);
                }
                return null;
            }
            @Override
            protected void done() {
                if (roster.isEmpty()) {
                    showStatus("No students enrolled in this course.", AppTheme.TEXT_MUTED);
                    tableContainer.removeAll();
                    JLabel msg = new JLabel("No students enrolled in this offering.");
                    msg.setFont(AppTheme.bodyFont(13));
                    msg.setForeground(AppTheme.TEXT_MUTED);
                    msg.setHorizontalAlignment(SwingConstants.CENTER);
                    tableContainer.add(msg, BorderLayout.CENTER);
                    tableContainer.revalidate();
                    tableContainer.repaint();
                    return;
                }
                buildMarksTable();
                boolean hasExisting = !existingMarks.isEmpty();
                showStatus(hasExisting
                    ? "✅  Existing marks loaded. You can edit and re-save."
                    : "Enter marks below and click Save All Marks.",
                    hasExisting ? new Color(27, 130, 60) : AppTheme.TEXT_MUTED);
            }
        }.execute();
    }

    /** Called when teacher clicks "Save All Marks". */
    private void onSaveClicked() {
        if (tableModel == null || roster.isEmpty()) return;

        String assessment = (String) assessmentDropdown.getSelectedItem();

        // Collect data from table
        List<Integer> enrollmentIds = new ArrayList<>();
        List<Double>  obtained      = new ArrayList<>();
        List<Double>  totals        = new ArrayList<>();
        List<String>  remarks       = new ArrayList<>();

        for (int i = 0; i < roster.size(); i++) {
            enrollmentIds.add(roster.get(i).getEnrollmentId());
            obtained.add(parseDouble(tableModel.getValueAt(i, COL_OBTAINED)));
            totals.add(parseDouble(tableModel.getValueAt(i, COL_TOTAL)));
            Object rem = tableModel.getValueAt(i, COL_REMARKS);
            remarks.add(rem == null ? "" : rem.toString());
        }

        saveBtn.setEnabled(false);
        showStatus("Saving…", AppTheme.TEXT_MUTED);

        new SwingWorker<SaveResult, Void>() {
            @Override
            protected SaveResult doInBackground() {
                return service.saveBatch(
                    enrollmentIds, assessment, obtained, totals, remarks);
            }
            @Override
            protected void done() {
                saveBtn.setEnabled(true);
                try {
                    SaveResult result = get();
                    if (result.outcome() == Outcome.SUCCESS) {
                        showStatus("✅  " + result.message(), new Color(27, 130, 60));
                    } else {
                        showStatus("⚠  " + result.message(), AppTheme.GOLD_DARK);
                    }
                } catch (Exception ex) {
                    showStatus("✖  Unexpected error: " + ex.getMessage(), Color.RED);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadOfferings() {
        new SwingWorker<List<OfferingItem>, Void>() {
            @Override
            protected List<OfferingItem> doInBackground() {
                return service.getOfferings();
            }
            @Override
            protected void done() {
                try {
                    List<OfferingItem> offerings = get();
                    offerings.forEach(courseDropdown::addItem);
                } catch (Exception ex) {
                    showStatus("Could not load courses: " + ex.getMessage(), Color.RED);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INNER CLASS: Table Model
    // ─────────────────────────────────────────────────────────────────────────
    private static class MarksTableModel extends AbstractTableModel {

        private static final String[] COLUMNS =
            {"Roll No.", "Student Name", "Marks Obtained", "Total Marks", "Remarks"};

        private final List<RosterStudent> roster;
        private final Object[][]          data;

        MarksTableModel(List<RosterStudent> roster) {
            this.roster = roster;
            this.data   = new Object[roster.size()][COLUMNS.length];
            for (int i = 0; i < roster.size(); i++) {
                data[i][COL_ROLL]     = roster.get(i).getRollNumber();
                data[i][COL_NAME]     = roster.get(i).getFullName();
                data[i][COL_OBTAINED] = "";
                data[i][COL_TOTAL]    = "";
                data[i][COL_REMARKS]  = "";
            }
        }

        @Override public int    getRowCount()              { return roster.size(); }
        @Override public int    getColumnCount()           { return COLUMNS.length; }
        @Override public String getColumnName(int col)     { return COLUMNS[col]; }
        @Override public Object getValueAt(int r, int c)   { return data[r][c]; }

        @Override
        public boolean isCellEditable(int row, int col) {
            // Roll No. and Name are read-only; marks + remarks are editable
            return col == COL_OBTAINED || col == COL_TOTAL || col == COL_REMARKS;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private double parseDouble(Object val) {
        if (val == null || val.toString().isBlank()) return 0.0;
        try { return Double.parseDouble(val.toString().trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        if (msg.startsWith("✅") || msg.startsWith("⚠") || msg.startsWith("✖")) {
            javax.swing.Timer t = new javax.swing.Timer(
                4000, e -> statusLabel.setText(" "));
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

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text + "  ");
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        return lbl;
    }

    private void styleDropdown(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setPreferredSize(new Dimension(0, 40));
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }
}