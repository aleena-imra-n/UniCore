package ui;

import bl.UserManagementService;
import bl.UserManagementService.*;
import model.SubDeptItem;
import model.UserRecord;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ManageUsersPanel — Admin UI for US-3.8b.
 *
 * Layout (three horizontal zones):
 *
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  HEADER — title + stat badges + "Add User" button           │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  FILTER BAR — Search field | Role combo | Dept combo | toggle│
 *  ├────────────────────────────────┬─────────────────────────────┤
 *  │  USER TABLE (left, 60%)        │  DETAIL / FORM CARD (right) │
 *  │  Scrollable JTable             │  Shows user info OR         │
 *  │  columns: Name | Role | Dept   │  Add / Edit form            │
 *  │           | Code | Status      │                             │
 *  │  Buttons below table:          │                             │
 *  │    Edit Selected | Deactivate  │                             │
 *  └────────────────────────────────┴─────────────────────────────┘
 *
 * Integration in AdminDashboard:
 *   contentArea.add(new ManageUsersPanel(username), BorderLayout.CENTER);
 */
public class ManageUsersPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color GREEN_FG  = new Color( 21, 128,  61);
    private static final Color GREEN_BG  = new Color(220, 252, 231);
    private static final Color RED_FG    = new Color(198,  40,  40);
    private static final Color RED_BG    = new Color(255, 235, 238);
    private static final Color AMBER_FG  = new Color(180, 100,   0);
    private static final Color DIVIDER   = new Color(215, 228, 248);
    private static final Color ROW_ALT   = new Color(245, 250, 255);
    private static final Color ROW_SEL   = new Color(210, 232, 255);

    // ── Table column indices ──────────────────────────────────────────────────
    private static final int COL_NAME   = 0;
    private static final int COL_ROLE   = 1;
    private static final int COL_DEPT   = 2;
    private static final int COL_CODE   = 3;
    private static final int COL_STATUS = 4;
    private static final String[] COL_HEADERS = {"Full Name","Role","Department","Roll/Emp Code","Status"};

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String                username;
    private final UserManagementService service;

    // ── Cached data ───────────────────────────────────────────────────────────
    private List<UserRecord>  allUsers    = new ArrayList<>();
    private List<UserRecord>  filtered    = new ArrayList<>();
    private List<SubDeptItem> allSubDepts = new ArrayList<>();

    // ── Header badges ─────────────────────────────────────────────────────────
    private JLabel totalLabel;
    private JLabel studentLabel;
    private JLabel teacherLabel;

    // ── Filter bar ────────────────────────────────────────────────────────────
    private JTextField        searchField;
    private JComboBox<String> roleCombo;
    private JComboBox<String> deptCombo;
    private JCheckBox         activeOnlyBox;

    // ── Table ─────────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable            userTable;
    private JLabel            tableStatusLabel;

    // ── Action buttons ────────────────────────────────────────────────────────
    private StyledButton editBtn;
    private StyledButton deactivateBtn;

    // ── Right-side detail/form card ───────────────────────────────────────────
    private JPanel      rightCard;
    private CardLayout  rightCards;
    private static final String CARD_EMPTY  = "empty";
    private static final String CARD_DETAIL = "detail";
    private static final String CARD_FORM   = "form";

    // Form fields (shared between Add and Edit modes)
    private boolean      formIsAdd;        // true = Add, false = Edit
    private UserRecord   editingUser;      // non-null in Edit mode

    private JLabel           formTitleLabel;
    private JTextField       fFullName;
    private JTextField       fUsername;
    private JPasswordField   fPassword;
    private JTextField       fEmail;
    private JComboBox<SubDeptItem> fDept;
    private JComboBox<String>      fRole;
    // Student-only
    private JPanel       studentFields;
    private JTextField   fRoll;
    private JSpinner     fBatch;
    private JSpinner     fSemester;
    // Teacher-only
    private JPanel       teacherFields;
    private JTextField   fEmpCode;
    private JTextField   fDesignation;

    private JLabel       formStatusLabel;
    private StyledButton formSaveBtn;

    // ─────────────────────────────────────────────────────────────────────────
    public ManageUsersPanel(String username) {
        this(username, new UserManagementService());
    }

    public ManageUsersPanel(String username, UserManagementService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildFilterBar(), BorderLayout.NORTH); // will be part of north composite
        add(buildBody(),      BorderLayout.CENTER);

        // Rebuild layout: header + filter stacked in NORTH
        removeAll();
        JPanel northStack = new JPanel(new BorderLayout(0, 12));
        northStack.setOpaque(false);
        northStack.add(buildHeader(),    BorderLayout.NORTH);
        northStack.add(buildFilterBar(), BorderLayout.SOUTH);
        add(northStack,    BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        loadData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        // Title block
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Manage Users");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Add, edit, and deactivate student and teacher accounts");
        sub.setFont(AppTheme.bodyFont(13));
        sub.setForeground(AppTheme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel accent = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(AppTheme.GOLD); g.fillRect(0,0,getWidth(),getHeight());
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

        // Right side: stat badges + Add User button
        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightSide.setOpaque(false);

        totalLabel   = makeStatBadge("—", "Total",    AppTheme.DEEP_BLUE);
        studentLabel = makeStatBadge("—", "Students", AppTheme.MID_BLUE);
        teacherLabel = makeStatBadge("—", "Teachers", GREEN_FG);

        StyledButton addBtn = new StyledButton("＋  Add User", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        addBtn.setPreferredSize(new Dimension(130, 36));
        addBtn.setFont(AppTheme.headingFont(13));
        addBtn.addActionListener(e -> openAddForm());

        rightSide.add(totalLabel);
        rightSide.add(studentLabel);
        rightSide.add(teacherLabel);
        rightSide.add(Box.createHorizontalStrut(8));
        rightSide.add(addBtn);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(rightSide,  BorderLayout.EAST);
        return header;
    }

    private JLabel makeStatBadge(String value, String label, Color color) {
        JLabel lbl = new JLabel(value + "  " + label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(color);
        lbl.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        lbl.setOpaque(false);
        return lbl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FILTER BAR
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        searchField = new JTextField(18);
        searchField.setFont(AppTheme.bodyFont(13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, AppTheme.LIGHT_BLUE, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        searchField.setToolTipText("Search by name, username, roll/emp code, email…");

        roleCombo = new JComboBox<>(new String[]{"All Roles", "Student", "Teacher"});
        deptCombo = new JComboBox<>(new String[]{"All Departments"});

        styleCombo(roleCombo);
        styleCombo(deptCombo);

        activeOnlyBox = new JCheckBox("Active only");
        activeOnlyBox.setOpaque(false);
        activeOnlyBox.setFont(AppTheme.bodyFont(13));
        activeOnlyBox.setForeground(AppTheme.TEXT_DARK);
        activeOnlyBox.setSelected(true);

        // Wire all filters to reapplyFilter()
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { reapplyFilter(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { reapplyFilter(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { reapplyFilter(); }
            });
        roleCombo.addActionListener(e -> reapplyFilter());
        deptCombo.addActionListener(e -> reapplyFilter());
        activeOnlyBox.addActionListener(e -> reapplyFilter());

        bar.add(new JLabel("🔍") {{setFont(new Font("Segoe UI Emoji",Font.PLAIN,14));}}); 
        bar.add(searchField);
        bar.add(new JLabel("Role:") {{setFont(AppTheme.bodyFont(12));setForeground(AppTheme.TEXT_MUTED);}});
        bar.add(roleCombo);
        bar.add(new JLabel("Dept:") {{setFont(AppTheme.bodyFont(12));setForeground(AppTheme.TEXT_MUTED);}});
        bar.add(deptCombo);
        bar.add(activeOnlyBox);

        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BODY — table (left 60%) + detail/form card (right 40%)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        body.add(buildTablePanel(), BorderLayout.CENTER);
        body.add(buildRightCard(),  BorderLayout.EAST);
        return body;
    }

    // ── Left: table + action buttons ──────────────────────────────────────────
    private JPanel buildTablePanel() {
        // Table model — all cells non-editable
        tableModel = new DefaultTableModel(COL_HEADERS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        userTable = new JTable(tableModel);
        userTable.setFont(AppTheme.bodyFont(13));
        userTable.setRowHeight(34);
        userTable.setShowGrid(false);
        userTable.setIntercellSpacing(new Dimension(0, 0));
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setFillsViewportHeight(true);

        // Header
        JTableHeader header = userTable.getTableHeader();
        header.setFont(AppTheme.headingFont(12));
        header.setBackground(AppTheme.PALE_BLUE);
        header.setForeground(AppTheme.DEEP_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder());
        header.setReorderingAllowed(false);

        // Column widths
        int[] widths = {180, 70, 140, 110, 70};
        for (int i = 0; i < widths.length; i++) {
            userTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Custom renderer for alternating rows + status cell colouring
        userTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    setBackground(ROW_SEL);
                    setForeground(AppTheme.NAVY);
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                    if (col == COL_STATUS) {
                        String v = value == null ? "" : value.toString();
                        setForeground("Active".equals(v) ? GREEN_FG : RED_FG);
                        setFont(AppTheme.headingFont(11));
                    } else if (col == COL_ROLE) {
                        setForeground(AppTheme.MID_BLUE);
                        setFont(AppTheme.headingFont(11));
                    } else {
                        setForeground(AppTheme.TEXT_DARK);
                        setFont(AppTheme.bodyFont(13));
                    }
                }
                return this;
            }
        });

        // Selection listener → update action button states
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateButtonStates();
        });

        // Double-click → open edit
        userTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEditForm();
            }
        });

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
        scroll.getViewport().setBackground(Color.WHITE);

        // Buttons below table
        editBtn       = new StyledButton("✏  Edit", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        deactivateBtn = new StyledButton("⊘  Deactivate", new Color(180,40,40), new Color(150,20,20));
        editBtn.setPreferredSize(new Dimension(120, 36));
        deactivateBtn.setPreferredSize(new Dimension(140, 36));
        editBtn.setFont(AppTheme.headingFont(12));
        deactivateBtn.setFont(AppTheme.headingFont(12));
        editBtn.setEnabled(false);
        deactivateBtn.setEnabled(false);

        editBtn.addActionListener(e -> openEditForm());
        deactivateBtn.addActionListener(e -> handleDeactivate());

        tableStatusLabel = new JLabel(" ");
        tableStatusLabel.setFont(AppTheme.bodyFont(12));
        tableStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        btnBar.setOpaque(false);
        btnBar.add(editBtn);
        btnBar.add(deactivateBtn);
        btnBar.add(tableStatusLabel);

        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(btnBar, BorderLayout.SOUTH);
        return panel;
    }

    // ── Right: card panel (empty / detail / form) ─────────────────────────────
    private JPanel buildRightCard() {
        rightCards = new CardLayout();
        rightCard  = new JPanel(rightCards);
        rightCard.setOpaque(false);
        rightCard.setPreferredSize(new Dimension(340, 0));

        rightCard.add(buildEmptyCard(), CARD_EMPTY);
        rightCard.add(buildFormCard(),  CARD_FORM);

        rightCards.show(rightCard, CARD_EMPTY);
        return rightCard;
    }

    private JPanel buildEmptyCard() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("👤");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel msg = new JLabel("Select a user to view details");
        msg.setFont(AppTheme.bodyFont(13));
        msg.setForeground(AppTheme.TEXT_MUTED);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel msg2 = new JLabel("or click ＋ Add User");
        msg2.setFont(AppTheme.bodyFont(12));
        msg2.setForeground(AppTheme.TEXT_MUTED);
        msg2.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(10));
        inner.add(msg);
        inner.add(Box.createVerticalStrut(4));
        inner.add(msg2);
        p.add(inner);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADD / EDIT FORM CARD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFormCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 0));

        // ── Scrollable form body ───────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 22, 10, 22));

        formTitleLabel = new JLabel("Add User");
        formTitleLabel.setFont(AppTheme.headingFont(15));
        formTitleLabel.setForeground(AppTheme.NAVY);
        formTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(formTitleLabel);
        body.add(Box.createVerticalStrut(4));
        body.add(makeDivider());
        body.add(Box.createVerticalStrut(14));

        // Role selector (shown only in Add mode)
        fRole = new JComboBox<>(new String[]{"student", "teacher"});
        styleCombo(fRole);
        fRole.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        fRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        fRole.addActionListener(e -> toggleRoleFields());
        addFormRow(body, "Role *", fRole, true);

        // Common fields
        fFullName = makeTextField();
        addFormRow(body, "Full Name *", fFullName, true);

        fUsername = makeTextField();
        addFormRow(body, "Username *", fUsername, true);

        fPassword = new JPasswordField();
        fPassword.setFont(AppTheme.bodyFont(13));
        fPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        fPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        fPassword.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, AppTheme.LIGHT_BLUE, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        addFormRow(body, "Password *", fPassword, true);

        fEmail = makeTextField();
        addFormRow(body, "Email *", fEmail, true);

        fDept = new JComboBox<>();
        styleCombo(fDept);
        fDept.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        fDept.setAlignmentX(Component.LEFT_ALIGNMENT);
        addFormRow(body, "Department *", fDept, true);

        // ── Student-only fields ───────────────────────────────────────────────
        studentFields = new JPanel();
        studentFields.setOpaque(false);
        studentFields.setLayout(new BoxLayout(studentFields, BoxLayout.Y_AXIS));
        studentFields.setAlignmentX(Component.LEFT_ALIGNMENT);

        fRoll = makeTextField();
        addFormRow(studentFields, "Roll Number *", fRoll, true);

        fBatch = new JSpinner(new SpinnerNumberModel(2024, 2000, 2100, 1));
        fBatch.setFont(AppTheme.bodyFont(13));
        fBatch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        fBatch.setAlignmentX(Component.LEFT_ALIGNMENT);
        addFormRow(studentFields, "Batch Year *", fBatch, true);

        fSemester = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        fSemester.setFont(AppTheme.bodyFont(13));
        fSemester.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        fSemester.setAlignmentX(Component.LEFT_ALIGNMENT);
        addFormRow(studentFields, "Current Semester", fSemester, false);

        body.add(studentFields);

        // ── Teacher-only fields ───────────────────────────────────────────────
        teacherFields = new JPanel();
        teacherFields.setOpaque(false);
        teacherFields.setLayout(new BoxLayout(teacherFields, BoxLayout.Y_AXIS));
        teacherFields.setAlignmentX(Component.LEFT_ALIGNMENT);

        fEmpCode = makeTextField();
        addFormRow(teacherFields, "Employee Code *", fEmpCode, true);

        fDesignation = makeTextField();
        addFormRow(teacherFields, "Designation", fDesignation, false);

        body.add(teacherFields);
        teacherFields.setVisible(false);   // default = student form shown

        body.add(Box.createVerticalStrut(12));

        // Status label
        formStatusLabel = new JLabel(" ");
        formStatusLabel.setFont(AppTheme.bodyFont(12));
        formStatusLabel.setForeground(AppTheme.TEXT_MUTED);
        formStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(formStatusLabel);
        body.add(Box.createVerticalStrut(8));

        // Buttons
        formSaveBtn = new StyledButton("Save", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        formSaveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        formSaveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        formSaveBtn.addActionListener(e -> handleFormSave());
        body.add(formSaveBtn);
        body.add(Box.createVerticalStrut(6));

        StyledButton cancelBtn = new StyledButton("Cancel",
            new Color(130,130,150), new Color(100,100,120));
        cancelBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancelBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cancelBtn.addActionListener(e -> rightCards.show(rightCard, CARD_EMPTY));
        body.add(cancelBtn);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadData() {
        tableStatusLabel.setText("Loading…");
        tableStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                boolean ok = service.init(username);
                if (!ok) return null;
                List<UserRecord>  users    = service.getAllUsers();
                List<SubDeptItem> subDepts = service.getAllSubDepts();
                return new Object[]{ users, subDepts };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] result = get();
                    if (result == null) {
                        tableStatusLabel.setText("Failed to load. Please re-login.");
                        tableStatusLabel.setForeground(RED_FG);
                        return;
                    }
                    allUsers    = (List<UserRecord>)  result[0];
                    allSubDepts = (List<SubDeptItem>) result[1];

                    populateDeptCombo();
                    populateFormDeptCombo();
                    updateHeaderBadges();
                    reapplyFilter();
                    tableStatusLabel.setText(" ");
                } catch (Exception ex) {
                    tableStatusLabel.setText("Error: " + ex.getMessage());
                    tableStatusLabel.setForeground(RED_FG);
                }
            }
        }.execute();
    }

    private void populateDeptCombo() {
        deptCombo.removeAllItems();
        deptCombo.addItem("All Departments");
        List<String> seen = new ArrayList<>();
        for (SubDeptItem sd : allSubDepts) {
            if (!seen.contains(sd.getMajorDeptName())) {
                seen.add(sd.getMajorDeptName());
                deptCombo.addItem(sd.getMajorDeptName());
            }
        }
    }

    private void populateFormDeptCombo() {
        fDept.removeAllItems();
        for (SubDeptItem sd : allSubDepts) fDept.addItem(sd);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FILTERING + TABLE POPULATION
    // ─────────────────────────────────────────────────────────────────────────
    private void reapplyFilter() {
        String roleSel  = (String) roleCombo.getSelectedItem();
        String deptSel  = (String) deptCombo.getSelectedItem();
        String roleFilter = "All Roles".equals(roleSel) ? null : roleSel.toLowerCase();
        int    majorDeptId = 0;

        // Match selected dept name to a majorDeptId
        if (deptSel != null && !"All Departments".equals(deptSel)) {
            for (SubDeptItem sd : allSubDepts) {
                if (sd.getMajorDeptName().equals(deptSel)) {
                    majorDeptId = sd.getMajorDeptId();
                    break;
                }
            }
        }

        filtered = service.filter(allUsers, roleFilter, majorDeptId,
                                   searchField.getText(), activeOnlyBox.isSelected());
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (UserRecord u : filtered) {
            tableModel.addRow(new Object[]{
                u.getFullName(),
                capitalize(u.getRole()),
                u.getSubDeptCode() + " — " + u.getMajorDeptName(),
                u.isStudent() ? u.getRollNumber() : u.getEmployeeCode(),
                u.isActive() ? "Active" : "Inactive"
            });
        }
        updateButtonStates();
        tableStatusLabel.setText(filtered.size() + " user" + (filtered.size() == 1 ? "" : "s") + " shown");
        tableStatusLabel.setForeground(AppTheme.TEXT_MUTED);
    }

    private void updateHeaderBadges() {
        long students = allUsers.stream().filter(UserRecord::isStudent).count();
        long teachers = allUsers.stream().filter(UserRecord::isTeacher).count();
        totalLabel.setText(allUsers.size() + "  Total");
        studentLabel.setText(students + "  Students");
        teacherLabel.setText(teachers + "  Teachers");
    }

    private void updateButtonStates() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= filtered.size()) {
            editBtn.setEnabled(false);
            deactivateBtn.setEnabled(false);
            return;
        }
        UserRecord sel = filtered.get(row);
        editBtn.setEnabled(true);
        deactivateBtn.setEnabled(sel.isActive());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FORM OPEN HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void openAddForm() {
        formIsAdd    = true;
        editingUser  = null;
        formTitleLabel.setText("Add New User");
        formSaveBtn.setText("Create User");

        // Reset all fields
        fRole.setSelectedIndex(0);
        fRole.setEnabled(true);
        fFullName.setText("");
        fUsername.setText("");
        fUsername.setEnabled(true);
        fPassword.setText("");
        fEmail.setText("");
        if (fDept.getItemCount() > 0) fDept.setSelectedIndex(0);
        fRoll.setText("");
        fBatch.setValue(2024);
        fSemester.setValue(1);
        fEmpCode.setText("");
        fDesignation.setText("");
        formStatusLabel.setText(" ");

        toggleRoleFields();
        rightCards.show(rightCard, CARD_FORM);
    }

    private void openEditForm() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= filtered.size()) return;

        editingUser = filtered.get(row);
        formIsAdd   = false;
        formTitleLabel.setText("Edit User");
        formSaveBtn.setText("Save Changes");

        // Pre-fill
        fRole.setSelectedItem(editingUser.getRole());
        fRole.setEnabled(false);   // role is immutable after creation
        fFullName.setText(editingUser.getFullName());
        fUsername.setText(editingUser.getUsername());
        fUsername.setEnabled(false); // username immutable
        fPassword.setText("");       // blank = keep existing
        fEmail.setText(editingUser.getEmail());

        // Select matching dept
        for (int i = 0; i < fDept.getItemCount(); i++) {
            if (fDept.getItemAt(i).getSubDeptId() == editingUser.getSubDeptId()) {
                fDept.setSelectedIndex(i);
                break;
            }
        }

        if (editingUser.isStudent()) {
            fRoll.setText(editingUser.getRollNumber() != null ? editingUser.getRollNumber() : "");
            if (editingUser.getBatchYear() != null) fBatch.setValue(editingUser.getBatchYear());
            if (editingUser.getCurrentSemester() != null) fSemester.setValue(editingUser.getCurrentSemester());
        } else {
            fEmpCode.setText(editingUser.getEmployeeCode() != null ? editingUser.getEmployeeCode() : "");
            fDesignation.setText(editingUser.getDesignation() != null ? editingUser.getDesignation() : "");
        }
        formStatusLabel.setText(" ");

        toggleRoleFields();
        rightCards.show(rightCard, CARD_FORM);
    }

    private void toggleRoleFields() {
        String role = (String) fRole.getSelectedItem();
        boolean isStudent = "student".equals(role);
        studentFields.setVisible(isStudent);
        teacherFields.setVisible(!isStudent);
        rightCard.revalidate();
        rightCard.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FORM SAVE
    // ─────────────────────────────────────────────────────────────────────────
    private void handleFormSave() {
        formSaveBtn.setEnabled(false);
        formSaveBtn.setText("Saving…");
        formStatusLabel.setText(" ");
        formStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        String role        = (String) fRole.getSelectedItem();
        String fullName    = fFullName.getText();
        String username2   = fUsername.getText();
        String password    = new String(fPassword.getPassword());
        String email       = fEmail.getText();
        SubDeptItem deptSel = (SubDeptItem) fDept.getSelectedItem();
        Integer subDeptId  = deptSel != null ? deptSel.getSubDeptId() : null;
        String  rollNumber = fRoll.getText();
        Integer batchYear  = (Integer) fBatch.getValue();
        Integer curSem     = (Integer) fSemester.getValue();
        String  empCode    = fEmpCode.getText();
        String  designation = fDesignation.getText();

        if (formIsAdd) {
            new SwingWorker<CreateResult, Void>() {
                @Override protected CreateResult doInBackground() {
                    return service.createUser(role, fullName, username2,
                        password, email, subDeptId,
                        rollNumber, batchYear, empCode, designation);
                }
                @Override protected void done() {
                    try {
                        CreateResult r = get();
                        handleFormResult(r.success(), r.message());
                        if (r.success()) reloadAfterChange();
                    } catch (Exception ex) { handleFormResult(false, ex.getMessage()); }
                    resetSaveBtn();
                }
            }.execute();
        } else {
            // Edit mode — blank password = keep existing
            String pw = password.isBlank() ? null : password;
            new SwingWorker<UpdateResult, Void>() {
                @Override protected UpdateResult doInBackground() {
                    return service.updateUser(editingUser, fullName, email,
                        pw, subDeptId, designation,
                        rollNumber, batchYear, curSem);
                }
                @Override protected void done() {
                    try {
                        UpdateResult r = get();
                        handleFormResult(r.success(), r.message());
                        if (r.success()) reloadAfterChange();
                    } catch (Exception ex) { handleFormResult(false, ex.getMessage()); }
                    resetSaveBtn();
                }
            }.execute();
        }
    }

    private void handleFormResult(boolean success, String msg) {
        formStatusLabel.setText((success ? "✓  " : "⚠  ") + msg);
        formStatusLabel.setForeground(success ? GREEN_FG : RED_FG);
        if (success) {
            tableStatusLabel.setText(msg);
            tableStatusLabel.setForeground(GREEN_FG);
        }
    }

    private void resetSaveBtn() {
        formSaveBtn.setEnabled(true);
        formSaveBtn.setText(formIsAdd ? "Create User" : "Save Changes");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DEACTIVATE
    // ─────────────────────────────────────────────────────────────────────────
    private void handleDeactivate() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= filtered.size()) return;
        UserRecord sel = filtered.get(row);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Deactivate account for " + sel.getFullName() + " (" + sel.getUsername() + ")?\n"
            + "The user will no longer be able to log in.",
            "Confirm Deactivation",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        deactivateBtn.setEnabled(false);
        tableStatusLabel.setText("Deactivating…");

        new SwingWorker<DeactivateResult, Void>() {
            @Override protected DeactivateResult doInBackground() {
                return service.deactivateUser(sel.getUserId());
            }
            @Override protected void done() {
                try {
                    DeactivateResult r = get();
                    tableStatusLabel.setText((r.success() ? "✓  " : "⚠  ") + r.message());
                    tableStatusLabel.setForeground(r.success() ? GREEN_FG : RED_FG);
                    if (r.success()) reloadAfterChange();
                } catch (Exception ex) {
                    tableStatusLabel.setText("⚠  " + ex.getMessage());
                    tableStatusLabel.setForeground(RED_FG);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RELOAD AFTER MUTATION
    // ─────────────────────────────────────────────────────────────────────────
    private void reloadAfterChange() {
        new SwingWorker<List<UserRecord>, Void>() {
            @Override protected List<UserRecord> doInBackground() {
                return service.getAllUsers();
            }
            @Override protected void done() {
                try {
                    allUsers = get();
                    updateHeaderBadges();
                    reapplyFilter();
                    rightCards.show(rightCard, CARD_EMPTY);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  WIDGET HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JTextField makeTextField() {
        JTextField f = new JTextField();
        f.setFont(AppTheme.bodyFont(13));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, AppTheme.LIGHT_BLUE, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return f;
    }

    private void addFormRow(JPanel panel, String labelText,
                             JComponent field, boolean required) {
        JLabel lbl = new JLabel(required ? labelText : labelText);
        lbl.setFont(AppTheme.headingFont(11));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(4));
        panel.add(field);
        panel.add(Box.createVerticalStrut(10));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    private JPanel makeDivider() {
        JPanel d = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(DIVIDER); g.fillRect(0, 0, getWidth(), 1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 1); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 1); }
        };
        d.setOpaque(false);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        return d;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}