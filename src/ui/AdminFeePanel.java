package ui;

import bl.AdminFeeService;
import bl.AdminFeeService.MarkPaidResult;
import model.AdminChallanRecord;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminFeePanel — Admin Fee Payment Tracking UI.
 *
 * Layout:
 *   Top    : filter bar (semester dropdown + status filter + search)
 *   Middle : summary tiles (total, paid, pending, overdue counts)
 *   Bottom : scrollable table of all challans with Mark as Paid button per row
 *
 * Integration in AdminDashboard onMenuClick:
 *   case "Fee Records" -> contentArea.add(new AdminFeePanel(), BorderLayout.CENTER);
 */
public class AdminFeePanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final Color GREEN  = new Color(21, 128, 61);
    private static final Color AMBER  = new Color(180, 100, 0);
    private static final Color RED_FG = new Color(198, 40, 40);

    private static final String[] STATUS_FILTERS = {"All", "Pending", "Paid", "Overdue"};

    // ── Table column indices ──────────────────────────────────────────────────
    private static final int COL_CHALLAN  = 0;
    private static final int COL_STUDENT  = 1;
    private static final int COL_ROLL     = 2;
    private static final int COL_SEMESTER = 3;
    private static final int COL_AMOUNT   = 4;
    private static final int COL_GENERATED= 5;
    private static final int COL_EXPIRY   = 6;
    private static final int COL_STATUS   = 7;
    private static final int COL_ACTION   = 8;

    private final AdminFeeService service;

    // Filter state
    private int    selectedSemesterId = -1;
    private String selectedStatus     = "All";

    // UI refs
    private JComboBox<String> semesterCombo;
    private JComboBox<String> statusCombo;
    private JLabel            statusLabel;
    private JLabel            totalLabel;
    private JLabel            paidLabel;
    private JLabel            pendingLabel;
    private JLabel            overdueLabel;
    private JPanel            tableContainer;

    // Loaded data
    private List<AdminChallanRecord> currentRecords = new ArrayList<>();
    private List<String[]>           semesterList   = new ArrayList<>();
    private boolean                  suppressFilter = false; // guard during combo reload

    public AdminFeePanel() {
        this.service = new AdminFeeService();
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(),   BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);

        loadSemesters();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Fee Payment Tracking");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("View and manage all student fee challans");
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

    // ── Main area ─────────────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel area = new JPanel(new BorderLayout(0, 14));
        area.setOpaque(false);

        area.add(buildFilterAndSummaryRow(), BorderLayout.NORTH);
        area.add(buildTableCard(),           BorderLayout.CENTER);
        return area;
    }

    // ── Filter bar + summary tiles ────────────────────────────────────────────
private JPanel buildFilterAndSummaryRow() {
    JPanel outer = new JPanel();
    outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
    outer.setOpaque(false);

    // ── Row 1: filters + summary tiles ───────────────────────────────────
    JPanel row = new JPanel(new BorderLayout(16, 0));
    row.setOpaque(false);

    JPanel filterCard = roundedCard();
    filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 12));

    JLabel semLbl = new JLabel("Semester:");
    semLbl.setFont(AppTheme.headingFont(12));
    semLbl.setForeground(AppTheme.DEEP_BLUE);

    semesterCombo = new JComboBox<>();
    semesterCombo.addItem("All");
    styleCombo(semesterCombo);
    semesterCombo.setPreferredSize(new Dimension(100, 34));
    semesterCombo.addActionListener(e -> onFilterChanged());

    JLabel statusLbl = new JLabel("Status:");
    statusLbl.setFont(AppTheme.headingFont(12));
    statusLbl.setForeground(AppTheme.DEEP_BLUE);

    statusCombo = new JComboBox<>(STATUS_FILTERS);
    styleCombo(statusCombo);
    statusCombo.setPreferredSize(new Dimension(100, 34));
    statusCombo.addActionListener(e -> onFilterChanged());

    filterCard.add(semLbl);
    filterCard.add(semesterCombo);
    filterCard.add(statusLbl);
    filterCard.add(statusCombo);

    JPanel tiles = new JPanel(new GridLayout(1, 4, 10, 0));
    tiles.setOpaque(false);
    totalLabel   = tileValueLabel("—");
    paidLabel    = tileValueLabel("—");
    pendingLabel = tileValueLabel("—");
    overdueLabel = tileValueLabel("—");
    tiles.add(buildSummaryTile("Total",   totalLabel,   "#1565C0"));
    tiles.add(buildSummaryTile("Paid",    paidLabel,    "#2E7D32"));
    tiles.add(buildSummaryTile("Pending", pendingLabel, "#F57C00"));
    tiles.add(buildSummaryTile("Overdue", overdueLabel, "#C62828"));

    row.add(filterCard, BorderLayout.CENTER);
    row.add(tiles,      BorderLayout.EAST);

    // ── Row 2: refresh button (full width, left-aligned) ─────────────────
    JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
    btnRow.setOpaque(false);

    StyledButton refreshBtn = new StyledButton("↻  Refresh",
        AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
    refreshBtn.setPreferredSize(new Dimension(110, 34));
    refreshBtn.setFont(AppTheme.headingFont(12));
    refreshBtn.addActionListener(e -> loadChallans());
    btnRow.add(refreshBtn);

    outer.add(row);
    outer.add(btnRow);
    return outer;
}
    private JPanel buildSummaryTile(String label, JLabel valueLabel, String hex) {
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Color.decode(hex));
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(12, getHeight()-4, getWidth()-12, getHeight()-4);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        tile.setPreferredSize(new Dimension(100, 66));

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.headingFont(10));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(lbl);
        tile.add(Box.createVerticalStrut(4));
        tile.add(valueLabel);
        return tile;
    }

    private JLabel tileValueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.titleFont(18));
        l.setForeground(AppTheme.NAVY);
        return l;
    }

    // ── Table card ────────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        cardHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel cardTitle = new JLabel("Challan Records");
        cardTitle.setFont(AppTheme.headingFont(15));
        cardTitle.setForeground(AppTheme.NAVY);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.bodyFont(12));
        statusLabel.setForeground(AppTheme.TEXT_MUTED);

        cardHeader.add(cardTitle,    BorderLayout.WEST);
        cardHeader.add(statusLabel,  BorderLayout.EAST);
        card.add(cardHeader, BorderLayout.NORTH);

        tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        JLabel loading = new JLabel("Loading challans…");
        loading.setFont(AppTheme.bodyFont(13));
        loading.setForeground(AppTheme.TEXT_MUTED);
        loading.setHorizontalAlignment(SwingConstants.CENTER);
        tableContainer.add(loading, BorderLayout.CENTER);

        card.add(tableContainer, BorderLayout.CENTER);
        return card;
    }

    // ── Table rendering ───────────────────────────────────────────────────────
    private void renderTable(List<AdminChallanRecord> records) {
        String[] columns = {
            "Challan No.", "Student", "Roll No.", "Semester",
            "Amount (PKR)", "Generated", "Expiry", "Status", "Action"
        };

        Object[][] data = new Object[records.size()][columns.length];
        for (int i = 0; i < records.size(); i++) {
            AdminChallanRecord r = records.get(i);
            data[i][COL_CHALLAN]  = r.getChallanNumber();
            data[i][COL_STUDENT]  = r.getStudentName();
            data[i][COL_ROLL]     = r.getRollNumber();
            data[i][COL_SEMESTER] = r.getSemesterName();
            data[i][COL_AMOUNT]   = String.format("%,.0f", r.getTotalAmount());
            data[i][COL_GENERATED]= r.getGeneratedDate() != null
                ? r.getGeneratedDate().format(FMT) : "—";
            data[i][COL_EXPIRY]   = r.getExpiryDate() != null
                ? r.getExpiryDate().format(FMT) : "—";
            data[i][COL_STATUS]   = r.getDisplayStatus();
            data[i][COL_ACTION]   = r.isPaid() ? "—" : "Mark Paid";
        }

        JTable table = new JTable(new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });

        styleTable(table, records);

        // Button column renderer + click handler
        table.getColumn("Action").setCellRenderer(new ActionCellRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col == COL_ACTION && row >= 0) {
                    AdminChallanRecord r = records.get(row);
                    if (!r.isPaid()) onMarkPaidClicked(r, row, table);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        tableContainer.removeAll();
        tableContainer.add(scroll, BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }

    private void styleTable(JTable table, List<AdminChallanRecord> records) {
        table.setFont(AppTheme.bodyFont(12));
        table.setRowHeight(36);
        table.setGridColor(new Color(220, 235, 255));
        table.setSelectionBackground(AppTheme.PALE_BLUE);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.getTableHeader().setFont(AppTheme.headingFont(11));
        table.getTableHeader().setBackground(AppTheme.DEEP_BLUE);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        // Column widths
        int[] widths = {140, 150, 90, 110, 100, 110, 110, 80, 100};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Status column colour renderer
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(
            new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                    super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                    String status = val == null ? "" : val.toString();
                    setForeground(status.equals("Paid")    ? GREEN
                                : status.equals("Overdue") ? RED_FG : AMBER);
                    setFont(AppTheme.headingFont(11));
                    setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    return this;
                }
            });

        // Alternating rows
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                if (!sel)
                    setBackground(r % 2 == 0 ? Color.WHITE : new Color(247, 251, 255));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        // Re-apply status renderer after default renderer override
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(
            new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                    super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                    if (!sel)
                        setBackground(r % 2 == 0 ? Color.WHITE : new Color(247,251,255));
                    String status = val == null ? "" : val.toString();
                    setForeground(status.equals("Paid")    ? GREEN
                                : status.equals("Overdue") ? RED_FG : AMBER);
                    setFont(AppTheme.headingFont(11));
                    setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    return this;
                }
            });
    }

    // ── Action cell renderer (button-style) ───────────────────────────────────
    private static class ActionCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            String text = value == null ? "—" : value.toString();
            if (text.equals("Mark Paid")) {
                JButton btn = new JButton("Mark Paid") {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(21, 128, 61));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        super.paintComponent(g2);
                        g2.dispose();
                    }
                };
                btn.setFont(new Font("SansSerif", Font.BOLD, 11));
                btn.setForeground(Color.WHITE);
                btn.setFocusPainted(false);
                btn.setBorderPainted(false);
                btn.setContentAreaFilled(false);
                btn.setPreferredSize(new Dimension(90, 28));
                return btn;
            }
            JLabel lbl = new JLabel("✅  Paid");
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lbl.setForeground(new Color(100, 130, 160));
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return lbl;
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────
    private void onFilterChanged() {
        if (suppressFilter) return;           // ignore events fired during removeAllItems()
        int semIdx = semesterCombo.getSelectedIndex();
        if (semIdx < 0) return;               // safety: no item selected yet
        selectedSemesterId = semIdx == 0 ? -1
            : Integer.parseInt(semesterList.get(semIdx - 1)[0]);
        selectedStatus = (String) statusCombo.getSelectedItem();
        applyFilters();
    }

    private void onMarkPaidClicked(AdminChallanRecord record, int row, JTable table) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Mark challan " + record.getChallanNumber() + " as paid?\n" +
            "Student: " + record.getStudentName() + "\n" +
            "Amount: PKR " + String.format("%,.0f", record.getTotalAmount()),
            "Confirm Payment",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<MarkPaidResult, Void>() {
            @Override protected MarkPaidResult doInBackground() {
                return service.markAsPaid(record.getChallanId(), record.getFeeId());
            }
            @Override protected void done() {
                try {
                    MarkPaidResult result = get();
                    if (result.success()) {
                        showStatus("✅  " + result.message(), GREEN);
                        loadChallans(); // reload full table
                    } else {
                        showStatus("⚠  " + result.message(), RED_FG);
                    }
                } catch (Exception ex) {
                    showStatus("✖  Error: " + ex.getMessage(), RED_FG);
                }
            }
        }.execute();
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadSemesters() {
        new SwingWorker<List<String[]>, Void>() {
            @Override protected List<String[]> doInBackground() {
                return service.getSemesters();
            }
            @Override protected void done() {
                try {
                    semesterList = new ArrayList<>(get());
                    suppressFilter = true;
                    semesterCombo.removeAllItems();
                    semesterCombo.addItem("All");
                    semesterList.forEach(s -> semesterCombo.addItem(s[1]));
                    suppressFilter = false;
                } catch (Exception ignored) {}
                loadChallans();
            }
        }.execute();
    }

    private void loadChallans() {
        showStatus("Loading…", AppTheme.TEXT_MUTED);
        new SwingWorker<List<AdminChallanRecord>, Void>() {
            @Override protected List<AdminChallanRecord> doInBackground() {
                return service.getChallans(selectedSemesterId, selectedStatus);
            }
            @Override protected void done() {
                try {
                    currentRecords = new ArrayList<>(get());
                    updateSummaryTiles(currentRecords);
                    renderTable(currentRecords);
                    showStatus(currentRecords.size() + " records", AppTheme.TEXT_MUTED);
                } catch (Exception ex) {
                    showStatus("Error: " + ex.getMessage(), RED_FG);
                }
            }
        }.execute();
    }

    private void applyFilters() {
        List<AdminChallanRecord> filtered = service.getChallans(
            selectedSemesterId, selectedStatus);
        updateSummaryTiles(filtered);
        renderTable(filtered);
        showStatus(filtered.size() + " records", AppTheme.TEXT_MUTED);
    }

    private void updateSummaryTiles(List<AdminChallanRecord> records) {
        long total   = records.size();
        long paid    = records.stream().filter(AdminChallanRecord::isPaid).count();
        long overdue = records.stream().filter(AdminChallanRecord::isOverdue).count();
        long pending = total - paid - overdue;

        totalLabel.setText(String.valueOf(total));
        paidLabel.setText(String.valueOf(paid));
        pendingLabel.setText(String.valueOf(Math.max(pending, 0)));
        overdueLabel.setText(String.valueOf(overdue));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
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

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(12));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2));
    }
}