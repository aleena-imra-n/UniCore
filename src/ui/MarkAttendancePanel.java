package ui;

import bl.AttendanceService;
import bl.AttendanceService.LoadResult;
import bl.AttendanceService.SaveResult;
import model.AttendanceRecord.Status;
import model.OfferingItem;
import model.RosterStudent;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * MarkAttendancePanel — Pure UI layer.
 *
 * What this class does:
 *   - Renders the course combo, date field, roster, summary cards
 *   - Captures teacher interactions (toggles, bulk buttons, save)
 *   - Delegates ALL validation, DB reads, and DB writes to AttendanceService
 *
 * What this class does NOT do:
 *   - No SQL
 *   - No business-rule logic (unmarked check, future-date guard, etc.)
 *   - No direct DAO calls
 *
 * Integration: TeacherDashboard passes the teacher's username:
 *   contentArea.add(new MarkAttendancePanel(username), BorderLayout.CENTER);
 */
public class MarkAttendancePanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG_PANEL   = new Color(245, 249, 255);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BG_HEADER  = new Color(232, 244, 253);
    private static final Color BORDER_CLR = new Color(200, 220, 240);

    private static final Color GREEN_BG   = new Color(232, 245, 233);
    private static final Color GREEN_FG   = new Color(46, 125, 50);
    private static final Color RED_BG     = new Color(255, 235, 238);
    private static final Color RED_FG     = new Color(198, 40, 40);
    private static final Color AMBER_BG   = new Color(255, 248, 225);
    private static final Color AMBER_FG   = new Color(245, 127, 23);

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String            teacherUsername;
    private final AttendanceService service;

    // ── UI state ──────────────────────────────────────────────────────────────
    /** studentId → Status as currently shown on screen. */
    private final Map<Integer, Status> statusMap = new LinkedHashMap<>();
    private List<RosterStudent>        roster    = new ArrayList<>();
    private boolean                    editMode  = false;

    // ── UI component refs ─────────────────────────────────────────────────────
    private JComboBox<OfferingItem> courseCombo;
    private JTextField              dateField;
    private JLabel                  modeBadge;
    private JLabel                  lblTotal, lblPresent, lblAbsent, lblLate;
    private JPanel                  rosterPanel;
    private JLabel                  footerMsg;
    private JButton                 saveBtn;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────
    public MarkAttendancePanel(String teacherUsername) {
        this.teacherUsername = teacherUsername;
        this.service         = new AttendanceService();
        init();
    }

    /** Test / injection constructor. */
    public MarkAttendancePanel(String teacherUsername, AttendanceService service) {
        this.teacherUsername = teacherUsername;
        this.service         = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildFooterBar(), BorderLayout.SOUTH);

        loadOfferingsFromService();
        reloadRoster();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TOP BAR: course combo | date | Load button | mode badge | bulk buttons
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        // ── Row 1: selectors ─────────────────────────────────────────────────
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row1.setOpaque(false);

        courseCombo = new JComboBox<>();
        courseCombo.setFont(AppTheme.bodyFont(13));
        courseCombo.setPreferredSize(new Dimension(320, 36));
        courseCombo.addActionListener(e -> {
            applyEditMode(false);
            reloadRoster();
        });
        row1.add(courseCombo);

        dateField = new JTextField(LocalDate.now().format(DATE_FMT), 10);
        dateField.setFont(AppTheme.bodyFont(13));
        dateField.setPreferredSize(new Dimension(130, 36));
        dateField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_CLR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        // Any keystroke clears the "editing" badge
        dateField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { applyEditMode(false); }
            public void removeUpdate(DocumentEvent e)  { applyEditMode(false); }
            public void changedUpdate(DocumentEvent e) { applyEditMode(false); }
        });
        row1.add(dateField);

        JButton loadBtn = quickBtn("Load date", BG_HEADER, AppTheme.MID_BLUE,
            e -> onLoadDateClicked());
        loadBtn.setPreferredSize(new Dimension(100, 36));
        row1.add(loadBtn);

        modeBadge = new JLabel();
        modeBadge.setFont(AppTheme.bodyFont(11));
        modeBadge.setOpaque(true);
        modeBadge.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_CLR, 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        modeBadge.setVisible(false);
        row1.add(modeBadge);

        // ── Row 2: bulk actions ───────────────────────────────────────────────
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);
        row2.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        row2.add(quickBtn("All present", GREEN_BG, GREEN_FG, e -> {
            markAll(Status.PRESENT); renderRoster();
        }));
        row2.add(quickBtn("All absent", RED_BG, RED_FG, e -> {
            markAll(Status.ABSENT); renderRoster();
        }));
        row2.add(quickBtn("Reset", BG_HEADER, AppTheme.MID_BLUE, e -> {
            applyEditMode(false);
            statusMap.clear();
            renderRoster();
        }));

        wrapper.add(row1);
        wrapper.add(row2);
        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CENTRE: summary cards + scrollable roster
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(buildSummaryRow(),   BorderLayout.NORTH);
        center.add(buildRosterScroll(), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        lblTotal   = statCard("0", "Total students", AppTheme.DEEP_BLUE);
        lblPresent = statCard("0", "Present",        GREEN_FG);
        lblAbsent  = statCard("0", "Absent",         RED_FG);
        lblLate    = statCard("0", "Late",           AMBER_FG);

        row.add(lblTotal.getParent());
        row.add(lblPresent.getParent());
        row.add(lblAbsent.getParent());
        row.add(lblLate.getParent());
        return row;
    }

    private JLabel statCard(String value, String label, Color numColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, BORDER_CLR, 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel numLbl = new JLabel(value);
        numLbl.setFont(AppTheme.headingFont(26));
        numLbl.setForeground(numColor);
        numLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel txtLbl = new JLabel(label);
        txtLbl.setFont(AppTheme.bodyFont(12));
        txtLbl.setForeground(AppTheme.TEXT_MUTED);
        txtLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(numLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(txtLbl);
        return numLbl;
    }

    private JScrollPane buildRosterScroll() {
        rosterPanel = new JPanel();
        rosterPanel.setLayout(new BoxLayout(rosterPanel, BoxLayout.Y_AXIS));
        rosterPanel.setBackground(BG_CARD);
        rosterPanel.setBorder(new RoundedBorder(10, BORDER_CLR, 1));

        JScrollPane scroll = new JScrollPane(rosterPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FOOTER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFooterBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(12, 0, 0, 0)));

        footerMsg = new JLabel("Mark attendance then save.");
        footerMsg.setFont(AppTheme.bodyFont(12));
        footerMsg.setForeground(AppTheme.TEXT_MUTED);
        bar.add(footerMsg, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        JButton cancelBtn = new StyledButton("Cancel",
            new Color(230, 230, 230), new Color(210, 210, 210));
        cancelBtn.setForeground(AppTheme.TEXT_DARK);
        cancelBtn.setPreferredSize(new Dimension(100, 40));
        cancelBtn.addActionListener(e -> {
            applyEditMode(false);
            statusMap.clear();
            renderRoster();
        });
        btnRow.add(cancelBtn);

        saveBtn = new StyledButton("Save Attendance",
            AppTheme.DEEP_BLUE, AppTheme.MID_BLUE);
        saveBtn.setPreferredSize(new Dimension(175, 40));
        saveBtn.addActionListener(e -> onSaveClicked());
        btnRow.add(saveBtn);

        bar.add(btnRow, BorderLayout.EAST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EVENT HANDLERS  (call service, react to result — NO logic here)
    // ─────────────────────────────────────────────────────────────────────────

    /** Called when teacher clicks "Load date". */
    private void onLoadDateClicked() {
        OfferingItem offering = (OfferingItem) courseCombo.getSelectedItem();
        int offeringId = (offering != null) ? offering.getOfferingId() : -1;

        LoadResult result = service.loadAttendanceForDate(
            dateField.getText(), offeringId);

        if (result.found()) {
            // Pre-fill the toggle buttons with loaded statuses
            statusMap.clear();
            statusMap.putAll(result.statusMap());
            applyEditMode(true);
        } else {
            applyEditMode(false);
        }

        renderRoster();
        setFooter(result.message(), result.found() ? GREEN_FG : AMBER_FG);
    }

    /** Called when teacher clicks "Save Attendance" or "Update Attendance". */
    private void onSaveClicked() {
        OfferingItem offering = (OfferingItem) courseCombo.getSelectedItem();
        if (offering == null) {
            setFooter("Please select a course first.", RED_FG);
            return;
        }

        String dateText = dateField.getText().trim();
        SaveResult result = editMode
            ? service.updateAttendance(offering.getOfferingId(), dateText, roster, statusMap)
            : service.saveAttendance(offering.getOfferingId(), dateText, roster, statusMap);

        if (result.success()) {
            setFooter(result.message(), GREEN_FG);
            JOptionPane.showMessageDialog(this,
                result.message(),
                editMode ? "Attendance Updated" : "Attendance Saved",
                JOptionPane.INFORMATION_MESSAGE);
            // After first successful save, switch to edit mode
            if (!editMode) applyEditMode(true);
        } else {
            setFooter(result.message(), RED_FG);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING  (calls service, no SQL in this class)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadOfferingsFromService() {
        courseCombo.removeAllItems();
        try {
            List<OfferingItem> offerings = service.getOfferingsForTeacher(teacherUsername);
            offerings.forEach(courseCombo::addItem);
        } catch (SQLException e) {
            setFooter("Failed to load courses: " + e.getMessage(), RED_FG);
        }
    }

    private void reloadRoster() {
        statusMap.clear();
        applyEditMode(false);
        OfferingItem offering = (OfferingItem) courseCombo.getSelectedItem();
        if (offering == null) {
            roster = List.of();
            renderRoster();
            return;
        }
        try {
            roster = service.getRoster(offering.getOfferingId());
        } catch (SQLException e) {
            roster = List.of();
            setFooter("Failed to load students: " + e.getMessage(), RED_FG);
        }
        renderRoster();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ROSTER RENDERING
    // ─────────────────────────────────────────────────────────────────────────

    private void renderRoster() {
        rosterPanel.removeAll();
        rosterPanel.add(buildHeaderRow());
        for (int i = 0; i < roster.size(); i++) {
            rosterPanel.add(buildStudentRow(roster.get(i), i));
        }
        rosterPanel.revalidate();
        rosterPanel.repaint();
        refreshSummaryCards();
    }

    private JPanel buildHeaderRow() {
        JPanel hdr = new JPanel(new GridBagLayout());
        hdr.setBackground(BG_HEADER);
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));
        hdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        hdr.setPreferredSize(new Dimension(100, 34));

        String[] cols   = {"#", "Student", "Roll No.", "Attendance", "Prev %"};
        int[]    widths = {32, 160, 120, 220, 70};
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 8, 0, 8);

        for (int i = 0; i < cols.length; i++) {
            gc.gridx = i; gc.weightx = (i == 1) ? 1.0 : 0; gc.ipadx = widths[i];
            JLabel lbl = new JLabel(cols[i]);
            lbl.setFont(AppTheme.bodyFont(11));
            lbl.setForeground(AppTheme.TEXT_MUTED);
            hdr.add(lbl, gc);
        }
        return hdr;
    }

    private JPanel buildStudentRow(RosterStudent s, int index) {
        JPanel row = new JPanel(new GridBagLayout());
        Color bg = (index % 2 == 0) ? BG_CARD : new Color(249, 252, 255);
        row.setBackground(bg);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setPreferredSize(new Dimension(100, 52));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 8, 0, 8);
        gc.anchor = GridBagConstraints.WEST;

        // Col 0: row number
        gc.gridx = 0; gc.weightx = 0; gc.ipadx = 32;
        JLabel idxLbl = new JLabel(String.valueOf(index + 1));
        idxLbl.setFont(AppTheme.bodyFont(12));
        idxLbl.setForeground(AppTheme.TEXT_MUTED);
        row.add(idxLbl, gc);

        // Col 1: name + roll
        gc.gridx = 1; gc.weightx = 1; gc.ipadx = 0;
        JPanel nameCell = new JPanel();
        nameCell.setLayout(new BoxLayout(nameCell, BoxLayout.Y_AXIS));
        nameCell.setOpaque(false);
        JLabel nameLbl = new JLabel(s.getFullName());
        nameLbl.setFont(AppTheme.headingFont(13));
        nameLbl.setForeground(AppTheme.TEXT_DARK);
        JLabel rollLbl = new JLabel(s.getRollNumber());
        rollLbl.setFont(AppTheme.bodyFont(11));
        rollLbl.setForeground(AppTheme.TEXT_MUTED);
        nameCell.add(nameLbl);
        nameCell.add(rollLbl);
        row.add(nameCell, gc);

        // Col 2: dept pill
        gc.gridx = 2; gc.weightx = 0; gc.ipadx = 120;
        JLabel deptPill = new JLabel(s.getSubDeptCode());
        deptPill.setFont(AppTheme.bodyFont(11));
        deptPill.setForeground(new Color(12, 68, 124));
        deptPill.setBackground(new Color(230, 241, 251));
        deptPill.setOpaque(true);
        deptPill.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, new Color(181, 212, 244), 1),
            BorderFactory.createEmptyBorder(3, 9, 3, 9)));
        row.add(deptPill, gc);

        // Col 3: P / Late / A toggle group
        gc.gridx = 3; gc.weightx = 0; gc.ipadx = 0;
        row.add(buildToggleGroup(s.getStudentId()), gc);

        // Col 4: previous attendance %
        gc.gridx = 4; gc.weightx = 0; gc.ipadx = 70;
        int pct = s.getAttendancePct();
        JLabel pctLbl = new JLabel(pct + "%");
        pctLbl.setFont(AppTheme.bodyFont(12));
        pctLbl.setForeground(pct >= 80 ? GREEN_FG : pct >= 65 ? AMBER_FG : RED_FG);
        row.add(pctLbl, gc);

        Color hoverBg = new Color(235, 245, 255);
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(hoverBg); }
            public void mouseExited(MouseEvent e)  { row.setBackground(bg); }
        });

        return row;
    }

    private JPanel buildToggleGroup(int studentId) {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        group.setOpaque(false);

        Status current = statusMap.getOrDefault(studentId, Status.UNMARKED);

        JButton btnP = segBtn("Present", Status.PRESENT, current);
        JButton btnL = segBtn("Late",    Status.LATE,    current);
        JButton btnA = segBtn("Absent",  Status.ABSENT,  current);

        btnP.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        btnL.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        btnA.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 1, BORDER_CLR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        ActionListener toggle = e -> {
            Status clicked = (Status) ((JButton) e.getSource()).getClientProperty("status");
            // Toggle off if already selected; otherwise set
            if (clicked == statusMap.get(studentId)) {
                statusMap.put(studentId, Status.UNMARKED);
            } else {
                statusMap.put(studentId, clicked);
            }
            renderRoster();
        };
        btnP.addActionListener(toggle);
        btnL.addActionListener(toggle);
        btnA.addActionListener(toggle);

        group.add(btnP); group.add(btnL); group.add(btnA);
        return group;
    }

    private JButton segBtn(String label, Status status, Status current) {
        JButton btn = new JButton(label);
        btn.putClientProperty("status", status);
        btn.setFont(AppTheme.bodyFont(12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(70, 30));

        boolean selected = (status == current);
        switch (status) {
            case PRESENT -> {
                btn.setBackground(selected ? GREEN_BG : BG_CARD);
                btn.setForeground(selected ? GREEN_FG : AppTheme.TEXT_MUTED);
            }
            case ABSENT -> {
                btn.setBackground(selected ? RED_BG : BG_CARD);
                btn.setForeground(selected ? RED_FG : AppTheme.TEXT_MUTED);
            }
            case LATE -> {
                btn.setBackground(selected ? AMBER_BG : BG_CARD);
                btn.setForeground(selected ? AMBER_FG : AppTheme.TEXT_MUTED);
            }
            default -> {
                btn.setBackground(BG_CARD);
                btn.setForeground(AppTheme.TEXT_MUTED);
            }
        }
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUMMARY CARDS
    // ─────────────────────────────────────────────────────────────────────────
    private void refreshSummaryCards() {
        int[] counts = service.countByStatus(roster, statusMap);
        int p = counts[0], a = counts[1], l = counts[2];
        int marked = p + a + l;
        int total  = roster.size();

        lblTotal.setText(String.valueOf(total));
        lblPresent.setText(String.valueOf(p));
        lblAbsent.setText(String.valueOf(a));
        lblLate.setText(String.valueOf(l));

        if (!editMode) {
            if (marked == total && total > 0)
                setFooter("All " + total + " students marked. Ready to save.", GREEN_FG);
            else
                setFooter(marked + " of " + total + " students marked.", AppTheme.TEXT_MUTED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Switches the mode badge and save-button label. */
    private void applyEditMode(boolean edit) {
        editMode = edit;
        if (edit) {
            modeBadge.setText("Editing existing record");
            modeBadge.setBackground(AMBER_BG);
            modeBadge.setForeground(AMBER_FG);
            modeBadge.setVisible(true);
            saveBtn.setText("Update Attendance");
        } else {
            modeBadge.setVisible(false);
            saveBtn.setText("Save Attendance");
        }
    }

    private void markAll(Status status) {
        for (RosterStudent s : roster) statusMap.put(s.getStudentId(), status);
    }

    private void setFooter(String msg, Color color) {
        footerMsg.setText(msg);
        footerMsg.setForeground(color);
    }

    private JButton quickBtn(String text, Color bg, Color fg, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setFont(AppTheme.bodyFont(12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(100, 36));
        btn.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_CLR, 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        Color hoverBg = bg.darker();
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hoverBg); repaint(); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg);      repaint(); }
        });
        return btn;
    }
}
