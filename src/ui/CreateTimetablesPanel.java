package ui;

import bl.TimetableService;
import bl.TimetableService.*;
import model.OfferingDropdownItem;
import model.TimetableConflict;
import model.TimetableSlot;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * CreateTimetablesPanel — Admin UI for US-3.11a.
 *
 * Layout:
 *
 *  ┌──────────────────────────────────────────────────────────────────┐
 *  │  HEADER — title, subtitle, slot-count badge                      │
 *  ├──────────────────────────┬───────────────────────────────────────┤
 *  │  FORM CARD  (left 38%)   │  SLOTS TABLE  (right 62%)             │
 *  │  ─────────────────────   │  ────────────────────────────────────  │
 *  │  Offering dropdown       │  JTable: Day | Course | Room |         │
 *  │  Day-of-week combo       │          Time | Teacher | Actions      │
 *  │  Start / End time fields │  Filter bar (search + offering filter) │
 *  │  Room field              │  Delete button below table             │
 *  │  [Add Slot] button       │                                        │
 *  │  Status label            │                                        │
 *  └──────────────────────────┴───────────────────────────────────────┘
 *
 * Two-phase add flow:
 *   1. Admin clicks "Add Slot" → service probes for conflicts (force=false).
 *   2a. No conflict → slot inserted, table refreshes.
 *   2b. Conflict found → confirmation dialog lists clashing slots.
 *       "Add Anyway" → service inserts with force=true.
 *       "Cancel"     → nothing happens.
 *
 * Integration in AdminDashboard:
 *   contentArea.add(new CreateTimetablesPanel(username), BorderLayout.CENTER);
 */
public class CreateTimetablesPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color GREEN_FG  = new Color( 21, 128,  61);
    private static final Color GREEN_BG  = new Color(220, 252, 231);
    private static final Color AMBER_FG  = new Color(180, 100,   0);
    private static final Color AMBER_BG  = new Color(255, 248, 225);
    private static final Color RED_FG    = new Color(198,  40,  40);
    private static final Color DIVIDER   = new Color(215, 228, 248);
    private static final Color ROW_ALT   = new Color(245, 250, 255);
    private static final Color ROW_SEL   = new Color(210, 232, 255);

    // ── Table column indices ──────────────────────────────────────────────────
    private static final int COL_DAY     = 0;
    private static final int COL_COURSE  = 1;
    private static final int COL_SECTION = 2;
    private static final int COL_TIME    = 3;
    private static final int COL_ROOM    = 4;
    private static final int COL_TEACHER = 5;
    private static final String[] COL_HEADERS =
        { "Day", "Course", "Sec", "Time", "Room", "Teacher" };

    // Day ordering for the combo box
    private static final String[] DAYS =
        { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };

    // Common time presets for the spinners
    private static final String[] TIME_PRESETS = {
        "07:00","07:30","08:00","08:30","09:00","09:30",
        "10:00","10:30","11:00","11:30","12:00","12:30",
        "13:00","13:30","14:00","14:30","15:00","15:30",
        "16:00","16:30","17:00","17:30","18:00"
    };

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String            username;
    private final TimetableService  service;

    // ── Cached data ───────────────────────────────────────────────────────────
    private List<TimetableSlot>         allSlots    = List.of();
    private List<OfferingDropdownItem>  allOfferings = List.of();

    // ── Header ────────────────────────────────────────────────────────────────
    private JLabel slotCountLabel;

    // ── Form fields ───────────────────────────────────────────────────────────
    private JComboBox<OfferingDropdownItem> offeringCombo;
    private JComboBox<String>               dayCombo;
    private JComboBox<String>               startCombo;
    private JComboBox<String>               endCombo;
    private JTextField                      roomField;
    private StyledButton                    addBtn;
    private JLabel                          formStatus;

    // ── Table ─────────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable            slotTable;
    private JTextField        filterField;
    private JComboBox<String> filterOffering;  // "All Offerings" or course code
    private JLabel            tableStatus;
    private StyledButton      deleteBtn;

    // ─────────────────────────────────────────────────────────────────────────
    public CreateTimetablesPanel(String username) {
        this(username, new TimetableService());
    }

    public CreateTimetablesPanel(String username, TimetableService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        loadData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Create Timetables");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Add and manage weekly class slots for course offerings");
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
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(sub);
        titleBlock.add(Box.createVerticalStrut(8));
        titleBlock.add(accent);

        // Slot count badge (top right)
        slotCountLabel = new JLabel("—  Slots");
        slotCountLabel.setFont(AppTheme.headingFont(13));
        slotCountLabel.setForeground(AppTheme.MID_BLUE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(slotCountLabel);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(right,      BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BODY — form (left) + table (right)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);

        body.add(buildFormCard(),  BorderLayout.WEST);
        body.add(buildTablePanel(), BorderLayout.CENTER);
        return body;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FORM CARD (left)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFormCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        card.setPreferredSize(new Dimension(310, 0));

        // ── Title ─────────────────────────────────────────────────────────────
        JLabel formTitle = new JLabel("Add Timetable Slot");
        formTitle.setFont(AppTheme.headingFont(15));
        formTitle.setForeground(AppTheme.NAVY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(makeDivider());
        card.add(Box.createVerticalStrut(16));

        // ── Offering ─────────────────────────────────────────────────────────
        addFieldLabel(card, "Course Offering  *");
        offeringCombo = new JComboBox<>();
        offeringCombo.addItem(null);   // placeholder
        offeringCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) { setText("— Select offering —"); setForeground(AppTheme.TEXT_MUTED); }
                else               { setText(value.toString());       setForeground(AppTheme.TEXT_DARK);  }
                return this;
            }
        });
        styleCombo(offeringCombo);
        offeringCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        offeringCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(offeringCombo);
        card.add(Box.createVerticalStrut(14));

        // ── Day ───────────────────────────────────────────────────────────────
        addFieldLabel(card, "Day of Week  *");
        dayCombo = new JComboBox<>(DAYS);
        styleCombo(dayCombo);
        dayCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        dayCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(dayCombo);
        card.add(Box.createVerticalStrut(14));

        // ── Start / End time (side by side) ──────────────────────────────────
        JPanel timeRow = new JPanel(new GridLayout(1, 2, 10, 0));
        timeRow.setOpaque(false);
        timeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        timeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel startBlock = new JPanel();
        startBlock.setOpaque(false);
        startBlock.setLayout(new BoxLayout(startBlock, BoxLayout.Y_AXIS));
        JLabel startLbl = new JLabel("Start Time  *");
        startLbl.setFont(AppTheme.headingFont(11));
        startLbl.setForeground(AppTheme.DEEP_BLUE);
        startCombo = new JComboBox<>(TIME_PRESETS);
        startCombo.setSelectedItem("08:00");
        styleCombo(startCombo);
        startCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        startBlock.add(startLbl);
        startBlock.add(Box.createVerticalStrut(4));
        startBlock.add(startCombo);

        JPanel endBlock = new JPanel();
        endBlock.setOpaque(false);
        endBlock.setLayout(new BoxLayout(endBlock, BoxLayout.Y_AXIS));
        JLabel endLbl = new JLabel("End Time  *");
        endLbl.setFont(AppTheme.headingFont(11));
        endLbl.setForeground(AppTheme.DEEP_BLUE);
        endCombo = new JComboBox<>(TIME_PRESETS);
        endCombo.setSelectedItem("09:30");
        styleCombo(endCombo);
        endCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        endBlock.add(endLbl);
        endBlock.add(Box.createVerticalStrut(4));
        endBlock.add(endCombo);

        timeRow.add(startBlock);
        timeRow.add(endBlock);
        card.add(timeRow);
        card.add(Box.createVerticalStrut(14));

        // ── Room ─────────────────────────────────────────────────────────────
        addFieldLabel(card, "Room Number");
        roomField = new JTextField();
        roomField.setFont(AppTheme.bodyFont(13));
        roomField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        roomField.setAlignmentX(Component.LEFT_ALIGNMENT);
        roomField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, AppTheme.LIGHT_BLUE, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        card.add(roomField);
        card.add(Box.createVerticalStrut(6));

        // Room conflict note
        JLabel conflictNote = new JLabel("ⓘ  Room conflicts trigger a warning, not a hard block.");
        conflictNote.setFont(AppTheme.bodyFont(10));
        conflictNote.setForeground(AppTheme.TEXT_MUTED);
        conflictNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(conflictNote);
        card.add(Box.createVerticalStrut(18));

        // ── Status label ──────────────────────────────────────────────────────
        formStatus = new JLabel(" ");
        formStatus.setFont(AppTheme.bodyFont(12));
        formStatus.setForeground(AppTheme.TEXT_MUTED);
        formStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formStatus);
        card.add(Box.createVerticalStrut(6));

        // ── Add button ────────────────────────────────────────────────────────
        addBtn = new StyledButton("＋  Add Slot", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        addBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        addBtn.addActionListener(e -> handleAdd(false));
        card.add(addBtn);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TABLE PANEL (right)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTablePanel() {
        // ── Filter bar ────────────────────────────────────────────────────────
        filterField    = new JTextField(16);
        filterOffering = new JComboBox<>(new String[]{ "All Offerings" });
        filterField.setFont(AppTheme.bodyFont(12));
        filterField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, AppTheme.LIGHT_BLUE, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        styleCombo(filterOffering);

        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });
        filterOffering.addActionListener(e -> applyFilter());

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.setOpaque(false);
        filterBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        filterBar.add(new JLabel("🔍") {{ setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13)); }});
        filterBar.add(filterField);
        filterBar.add(new JLabel("Offering:") {{ setFont(AppTheme.bodyFont(12)); setForeground(AppTheme.TEXT_MUTED); }});
        filterBar.add(filterOffering);

        // ── Table ─────────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(COL_HEADERS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        slotTable = new JTable(tableModel);
        slotTable.setFont(AppTheme.bodyFont(12));
        slotTable.setRowHeight(32);
        slotTable.setShowGrid(false);
        slotTable.setIntercellSpacing(new Dimension(0, 0));
        slotTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        slotTable.setFillsViewportHeight(true);

        JTableHeader header = slotTable.getTableHeader();
        header.setFont(AppTheme.headingFont(11));
        header.setBackground(AppTheme.PALE_BLUE);
        header.setForeground(AppTheme.DEEP_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder());
        header.setReorderingAllowed(false);

        // Column widths
        int[] colWidths = { 90, 140, 45, 110, 80, 130 };
        for (int i = 0; i < colWidths.length; i++) {
            slotTable.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
        }

        // Custom renderer — day column gets a day-colour pill
        slotTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setBackground(isSelected ? ROW_SEL : (row % 2 == 0 ? Color.WHITE : ROW_ALT));
                if (col == COL_DAY) {
                    setForeground(dayColor(value == null ? "" : value.toString()));
                    setFont(AppTheme.headingFont(11));
                } else if (col == COL_TIME) {
                    setForeground(AppTheme.MID_BLUE);
                    setFont(AppTheme.headingFont(11));
                } else {
                    setForeground(AppTheme.TEXT_DARK);
                    setFont(AppTheme.bodyFont(12));
                }
                return this;
            }
        });

        slotTable.getSelectionModel().addListSelectionListener(
            e -> { if (!e.getValueIsAdjusting()) updateDeleteBtn(); });

        JScrollPane scroll = new JScrollPane(slotTable);
        scroll.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
        scroll.getViewport().setBackground(Color.WHITE);

        // ── Bottom bar ────────────────────────────────────────────────────────
        deleteBtn = new StyledButton("🗑  Delete Selected",
            new Color(180, 40, 40), new Color(150, 20, 20));
        deleteBtn.setPreferredSize(new Dimension(170, 36));
        deleteBtn.setFont(AppTheme.headingFont(12));
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> handleDelete());

        tableStatus = new JLabel(" ");
        tableStatus.setFont(AppTheme.bodyFont(12));
        tableStatus.setForeground(AppTheme.TEXT_MUTED);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bottomBar.setOpaque(false);
        bottomBar.add(deleteBtn);
        bottomBar.add(tableStatus);

        // ── Assemble ──────────────────────────────────────────────────────────
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);
        panel.add(filterBar,  BorderLayout.NORTH);
        panel.add(scroll,     BorderLayout.CENTER);
        panel.add(bottomBar,  BorderLayout.SOUTH);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadData() {
        addBtn.setEnabled(false);
        tableStatus.setText("Loading…");

        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                List<OfferingDropdownItem> offerings = service.getAllOfferings();
                List<TimetableSlot>        slots     = service.getAllSlots();
                return new Object[]{ offerings, slots };
            }

            @Override @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] r = get();
                    allOfferings = (List<OfferingDropdownItem>) r[0];
                    allSlots     = (List<TimetableSlot>)        r[1];

                    populateOfferingCombo();
                    populateFilterOfferingCombo();
                    refreshTable(allSlots);
                    updateBadge();
                    addBtn.setEnabled(true);
                    tableStatus.setText(" ");
                } catch (Exception ex) {
                    tableStatus.setText("Error loading data: " + ex.getMessage());
                    tableStatus.setForeground(RED_FG);
                }
            }
        }.execute();
    }

    private void populateOfferingCombo() {
        offeringCombo.removeAllItems();
        offeringCombo.addItem(null);
        allOfferings.forEach(offeringCombo::addItem);
    }

    private void populateFilterOfferingCombo() {
        filterOffering.removeAllItems();
        filterOffering.addItem("All Offerings");
        allOfferings.forEach(o -> filterOffering.addItem(o.getCourseCode() + " (" + o.getSection() + ")"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FILTER + TABLE REFRESH
    // ─────────────────────────────────────────────────────────────────────────
    private void applyFilter() {
        String term   = filterField.getText().trim().toLowerCase();
        String selOff = (String) filterOffering.getSelectedItem();
        boolean allOf = "All Offerings".equals(selOff);

        List<TimetableSlot> filtered = allSlots.stream()
            .filter(s -> allOf || (s.getCourseCode() + " (" + s.getSection() + ")").equals(selOff))
            .filter(s -> term.isEmpty()
                || s.getCourseCode().toLowerCase().contains(term)
                || s.getCourseName().toLowerCase().contains(term)
                || s.getDayOfWeek().toLowerCase().contains(term)
                || s.getRoomNumber().toLowerCase().contains(term)
                || s.getTeacherName().toLowerCase().contains(term))
            .toList();

        refreshTable(filtered);
    }

    private void refreshTable(List<TimetableSlot> slots) {
        tableModel.setRowCount(0);
        for (TimetableSlot s : slots) {
            tableModel.addRow(new Object[]{
                s.getDayOfWeek(),
                s.getCourseCode() + "  " + s.getCourseName(),
                s.getSection(),
                s.getStartTime() + "–" + s.getEndTime(),
                s.getRoomNumber(),
                s.getTeacherName()
            });
        }
        // Store slot objects in a parallel list for row→id lookup
        // Use client property to carry the ordered slot list
        slotTable.putClientProperty("slotList", slots);
        updateDeleteBtn();
        tableStatus.setText(slots.size() + " slot" + (slots.size() == 1 ? "" : "s"));
        tableStatus.setForeground(AppTheme.TEXT_MUTED);
    }

    private void updateBadge() {
        slotCountLabel.setText(allSlots.size() + "  Slot" + (allSlots.size() == 1 ? "" : "s"));
    }

    private void updateDeleteBtn() {
        deleteBtn.setEnabled(slotTable.getSelectedRow() >= 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADD SLOT HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    private void handleAdd(boolean force) {
        OfferingDropdownItem offering =
            (OfferingDropdownItem) offeringCombo.getSelectedItem();
        String day   = (String) dayCombo.getSelectedItem();
        String start = (String) startCombo.getSelectedItem();
        String end   = (String) endCombo.getSelectedItem();
        String room  = roomField.getText().trim();

        int offeringId = (offering != null) ? offering.getOfferingId() : -1;

        addBtn.setEnabled(false);
        addBtn.setText("Adding…");
        setFormStatus(" ", AppTheme.TEXT_MUTED);

        new SwingWorker<AddResult, Void>() {
            @Override protected AddResult doInBackground() {
                return force
                    ? service.addSlotForced(offeringId, day, start, end,
                                             room.isEmpty() ? null : room)
                    : service.addSlot(offeringId, day, start, end,
                                       room.isEmpty() ? null : room);
            }

            @Override protected void done() {
                try {
                    AddResult result = get();
                    switch (result.outcome()) {
                        case SUCCESS -> {
                            setFormStatus("✓  " + result.message(), GREEN_FG);
                            reloadSlots();
                        }
                        case CONFLICT -> showConflictDialog(
                            result.conflicts(), offeringId, day, start, end,
                            room.isEmpty() ? null : room);
                        case DUPLICATE ->
                            setFormStatus("⚠  " + result.message(), AMBER_FG);
                        default ->
                            setFormStatus("⚠  " + result.message(), RED_FG);
                    }
                } catch (Exception ex) {
                    setFormStatus("⚠  Error: " + ex.getMessage(), RED_FG);
                } finally {
                    addBtn.setEnabled(true);
                    addBtn.setText("＋  Add Slot");
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONFLICT DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void showConflictDialog(List<TimetableConflict> conflicts,
                                     int offeringId, String day,
                                     String start, String end, String room) {
        // Build message
        StringBuilder sb = new StringBuilder();
        sb.append("<html><b>Room conflict detected!</b><br><br>");
        sb.append("The following slot(s) already occupy this room at this time:<br><br>");
        for (TimetableConflict c : conflicts) {
            sb.append("&nbsp;&nbsp;• ").append(c.describe()).append("<br>");
        }
        sb.append("<br>Do you want to add the slot anyway?</html>");

        // Warning panel
        JPanel msgPanel = new JPanel(new BorderLayout(0, 8));
        msgPanel.setOpaque(false);

        JLabel icon = new JLabel("⚠");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        icon.setForeground(AMBER_FG);
        icon.setVerticalAlignment(SwingConstants.TOP);
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JLabel msg = new JLabel(sb.toString());
        msg.setFont(AppTheme.bodyFont(12));
        msg.setForeground(AppTheme.TEXT_DARK);

        msgPanel.add(icon, BorderLayout.WEST);
        msgPanel.add(msg,  BorderLayout.CENTER);

        Object[] options = { "Add Anyway", "Cancel" };
        int choice = JOptionPane.showOptionDialog(
            this, msgPanel, "Room Conflict Warning",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null, options, options[1]);

        if (choice == 0) {
            // Force insert
            setFormStatus("Adding slot despite conflict…", AMBER_FG);
            addBtn.setEnabled(false);
            addBtn.setText("Adding…");

            new SwingWorker<AddResult, Void>() {
                @Override protected AddResult doInBackground() {
                    return service.addSlotForced(offeringId, day, start, end, room);
                }
                @Override protected void done() {
                    try {
                        AddResult r = get();
                        if (r.outcome() == TimetableService.Outcome.SUCCESS) {
                            setFormStatus("✓  " + r.message(), GREEN_FG);
                            reloadSlots();
                        } else {
                            setFormStatus("⚠  " + r.message(), RED_FG);
                        }
                    } catch (Exception ex) {
                        setFormStatus("⚠  " + ex.getMessage(), RED_FG);
                    } finally {
                        addBtn.setEnabled(true);
                        addBtn.setText("＋  Add Slot");
                    }
                }
            }.execute();
        } else {
            setFormStatus("Add cancelled.", AppTheme.TEXT_MUTED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void handleDelete() {
        int row = slotTable.getSelectedRow();
        if (row < 0) return;

        List<TimetableSlot> visibleSlots =
            (List<TimetableSlot>) slotTable.getClientProperty("slotList");
        if (visibleSlots == null || row >= visibleSlots.size()) return;

        TimetableSlot slot = visibleSlots.get(row);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this slot?\n\n" + slot.getSummary(),
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        deleteBtn.setEnabled(false);
        tableStatus.setText("Deleting…");

        new SwingWorker<TimetableService.DeleteResult, Void>() {
            @Override protected TimetableService.DeleteResult doInBackground() {
                return service.deleteSlot(slot.getTimetableId());
            }
            @Override protected void done() {
                try {
                    TimetableService.DeleteResult r = get();
                    tableStatus.setText((r.success() ? "✓  " : "⚠  ") + r.message());
                    tableStatus.setForeground(r.success() ? GREEN_FG : RED_FG);
                    if (r.success()) reloadSlots();
                } catch (Exception ex) {
                    tableStatus.setText("⚠  " + ex.getMessage());
                    tableStatus.setForeground(RED_FG);
                    deleteBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RELOAD AFTER MUTATION
    // ─────────────────────────────────────────────────────────────────────────
    private void reloadSlots() {
        new SwingWorker<List<TimetableSlot>, Void>() {
            @Override protected List<TimetableSlot> doInBackground() {
                return service.getAllSlots();
            }
            @Override protected void done() {
                try {
                    allSlots = get();
                    populateFilterOfferingCombo();
                    applyFilter();
                    updateBadge();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  WIDGET HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void addFieldLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.headingFont(11));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(5));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    }

    private JPanel makeDivider() {
        JPanel d = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(DIVIDER);
                g.fillRect(0, 0, getWidth(), 1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 1); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 1); }
        };
        d.setOpaque(false);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        return d;
    }

    private void setFormStatus(String text, Color color) {
        formStatus.setText(text);
        formStatus.setForeground(color);
    }

    /** Returns a distinct colour for each weekday column cell. */
    private Color dayColor(String day) {
        return switch (day) {
            case "Monday"    -> new Color( 30, 100, 200);
            case "Tuesday"   -> new Color( 20, 130,  80);
            case "Wednesday" -> new Color(140,  60, 180);
            case "Thursday"  -> new Color(180,  90,  20);
            case "Friday"    -> new Color(180,  30,  60);
            default          -> AppTheme.TEXT_DARK;
        };
    }
}