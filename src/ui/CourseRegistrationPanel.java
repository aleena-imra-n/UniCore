package ui;

import bl.CourseRegistrationService;
import bl.CourseRegistrationService.Outcome;
import bl.CourseRegistrationService.RegisterResult;
import model.CourseItem;
import model.EnrolledCourse;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.UIManager;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * CourseRegistrationPanel — Pure UI layer.
 *
 * What this class does:
 *   - Renders two tabs: "Available Courses" and "For Improvement"
 *   - Renders info tiles, status label, and enrolled list
 *   - Captures student interactions (selection, register click)
 *   - Delegates ALL validation, duplicate checks, and DB writes to
 *     CourseRegistrationService
 *
 * What this class does NOT do:
 *   - No SQL
 *   - No business rules (duplicate check, eligibility, capacity)
 *   - No direct DAO calls
 *
 * Integration in StudentDashboard:
 *   contentArea.add(new CourseRegistrationPanel(username), BorderLayout.CENTER);
 */
public class CourseRegistrationPanel extends JPanel {

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String                    username;
    private final CourseRegistrationService service;

    // ── UI component refs ─────────────────────────────────────────────────────
    private JComboBox<CourseItem> courseDropdown;
    private JComboBox<CourseItem> improvementDropdown;
    private JLabel                codeValueLabel;
    private JLabel                creditsValueLabel;
    private JLabel                teacherValueLabel;
    private JLabel                scheduleValueLabel;
    private JLabel                statusLabel;
    private JPanel                registeredListPanel;
    private JLabel                countBadge;
    private JLabel                creditHoursBadge;
    private StyledButton          registerBtn;
    private JTabbedPane           tabbedPane;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public CourseRegistrationPanel(String username) {
        this.username = username;
        this.service  = new CourseRegistrationService();
        init();
    }

    /** Injection constructor for unit tests or custom service wiring. */
    public CourseRegistrationPanel(String username, CourseRegistrationService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        // Resolve student_id and populate both panels off the EDT so the UI
        // stays responsive while the DB round-trip completes.
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.init(username);   // DB call — off EDT
            }

            @Override
            protected void done() {             // back on EDT
                try {
                    if (!get()) {
                        showStatus("Failed to load student account. Please re-login.",
                                   Color.RED);
                    } else {
                        loadAvailableCourses();
                        loadImprovementCourses();
                        loadEnrolledCourses();
                    }
                } catch (Exception ex) {
                    showStatus("Unexpected error loading account: " + ex.getMessage(),
                               Color.RED);
                }
            }
        }.execute();
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

        JLabel title = new JLabel("Course Registration");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Select a course from the dropdown and click Register to enroll");
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
    //  BODY: left card (tabbed registration) + right card (enrolled)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.gridy   = 0;
        gbc.weighty = 1.0;

        gbc.gridx   = 0;
        gbc.weightx = 0.55;
        gbc.insets  = new Insets(0, 0, 0, 14);
        body.add(buildRegistrationCard(), gbc);

        gbc.gridx   = 1;
        gbc.weightx = 0.45;
        gbc.insets  = new Insets(0, 0, 0, 0);
        body.add(buildEnrolledCard(), gbc);

        return body;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LEFT CARD: tabbed (Available + Improvement) + info tiles + register
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildRegistrationCard() {
        JPanel card = roundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JLabel sectionTitle = new JLabel("Select a Course");
        sectionTitle.setFont(AppTheme.headingFont(15));
        sectionTitle.setForeground(AppTheme.NAVY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sectionTitle);
        card.add(Box.createVerticalStrut(4));

        JLabel sectionSub = new JLabel("Choose from available courses or courses for improvement");
        sectionSub.setFont(AppTheme.bodyFont(12));
        sectionSub.setForeground(AppTheme.TEXT_MUTED);
        sectionSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sectionSub);
        card.add(Box.createVerticalStrut(16));

        // ── Tabbed Pane ───────────────────────────────────────────────────────
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(AppTheme.headingFont(12));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabbedPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Tab 1: Available Courses
        JPanel availableTab = new JPanel();
        availableTab.setOpaque(false);
        availableTab.setLayout(new BoxLayout(availableTab, BoxLayout.Y_AXIS));
        availableTab.setBorder(BorderFactory.createEmptyBorder(10, 4, 4, 4));

        addFieldLabel(availableTab, "Available Courses (Semester-filtered)");
        courseDropdown = new JComboBox<>();
        courseDropdown.addItem(null);
        styleDropdown(courseDropdown);
        courseDropdown.setRenderer(new CourseDropdownRenderer("— Select a course —"));
        courseDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        courseDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        courseDropdown.addActionListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) onCourseSelected(courseDropdown);
        });
        availableTab.add(courseDropdown);

        // Tab 2: Improvement Courses
        JPanel improvementTab = new JPanel();
        improvementTab.setOpaque(false);
        improvementTab.setLayout(new BoxLayout(improvementTab, BoxLayout.Y_AXIS));
        improvementTab.setBorder(BorderFactory.createEmptyBorder(10, 4, 4, 4));

        addFieldLabel(improvementTab, "Courses for Improvement (Already Passed)");
        improvementDropdown = new JComboBox<>();
        improvementDropdown.addItem(null);
        styleDropdown(improvementDropdown);
        improvementDropdown.setRenderer(new CourseDropdownRenderer("— Select a course for improvement —"));
        improvementDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        improvementDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        improvementDropdown.addActionListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) onCourseSelected(improvementDropdown);
        });
        improvementTab.add(improvementDropdown);

        tabbedPane.addTab("  Available Courses", availableTab);
        tabbedPane.addTab("  For Improvement", improvementTab);

        // When switching tabs, update info tiles from the active tab's dropdown
        tabbedPane.addChangeListener(e -> {
            JComboBox<CourseItem> activeDropdown = getActiveDropdown();
            onCourseSelected(activeDropdown);
        });

        card.add(tabbedPane);
        card.add(Box.createVerticalStrut(16));

        // ── Info tiles ────────────────────────────────────────────────────────
        JPanel infoRow = new JPanel(new GridLayout(1, 2, 12, 0));
        infoRow.setOpaque(false);
        infoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        infoRow.add(buildInfoTile("Course Code",  "—", TileTarget.CODE));
        infoRow.add(buildInfoTile("Credit Hours", "—", TileTarget.CREDITS));
        card.add(infoRow);
        card.add(Box.createVerticalStrut(10));

        JPanel infoRow2 = new JPanel(new GridLayout(1, 2, 12, 0));
        infoRow2.setOpaque(false);
        infoRow2.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoRow2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        infoRow2.add(buildInfoTile("Teacher",  "—", TileTarget.TEACHER));
        infoRow2.add(buildInfoTile("Schedule", "—", TileTarget.SCHEDULE));
        card.add(infoRow2);
        card.add(Box.createVerticalStrut(24));

        // ── Status label ──────────────────────────────────────────────────────
        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.headingFont(13));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(12));

        // ── Register button ───────────────────────────────────────────────────
        registerBtn = new StyledButton("Register",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        registerBtn.addActionListener(e -> onRegisterClicked());
        card.add(registerBtn);

        return card;
    }

    /** Returns whichever dropdown is currently active based on the selected tab. */
    private JComboBox<CourseItem> getActiveDropdown() {
        return tabbedPane.getSelectedIndex() == 0 ? courseDropdown : improvementDropdown;
    }

    /** Custom renderer for course dropdowns with placeholder text. */
    private static class CourseDropdownRenderer extends DefaultListCellRenderer {
        private final String placeholder;

        CourseDropdownRenderer(String placeholder) { this.placeholder = placeholder; }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(value == null ? placeholder : value.toString());
            return this;
        }
    }

    // Enum to identify which label each info tile should update
    private enum TileTarget { CODE, CREDITS, TEACHER, SCHEDULE }

    private JPanel buildInfoTile(String label, String initialValue, TileTarget target) {
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.headingFont(10));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(initialValue);
        val.setFont(AppTheme.headingFont(15));
        val.setForeground(AppTheme.DEEP_BLUE);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Assign to the correct field reference
        switch (target) {
            case CODE     -> codeValueLabel     = val;
            case CREDITS  -> creditsValueLabel  = val;
            case TEACHER  -> teacherValueLabel  = val;
            case SCHEDULE -> scheduleValueLabel = val;
        }

        tile.add(lbl);
        tile.add(Box.createVerticalStrut(4));
        tile.add(val);
        return tile;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RIGHT CARD: enrolled courses list
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildEnrolledCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        // Title row + count badge
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("Enrolled Courses");
        title.setFont(AppTheme.headingFont(15));
        title.setForeground(AppTheme.NAVY);

        // Credit-hours summary — updated whenever the enrolled list changes
        creditHoursBadge = new JLabel("0 cr.");
        creditHoursBadge.setFont(AppTheme.bodyFont(11));
        creditHoursBadge.setForeground(AppTheme.TEXT_MUTED);

        countBadge = new JLabel("0") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.GOLD);
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        countBadge.setFont(AppTheme.headingFont(11));
        countBadge.setForeground(AppTheme.NAVY);
        countBadge.setHorizontalAlignment(SwingConstants.CENTER);
        countBadge.setPreferredSize(new Dimension(26, 26));
        countBadge.setOpaque(false);

        JPanel eastBadges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        eastBadges.setOpaque(false);
        eastBadges.add(creditHoursBadge);
        eastBadges.add(countBadge);

        titleRow.add(title,      BorderLayout.WEST);
        titleRow.add(eastBadges, BorderLayout.EAST);
        card.add(titleRow, BorderLayout.NORTH);

        // Scrollable list
        registeredListPanel = new JPanel();
        registeredListPanel.setLayout(new BoxLayout(registeredListPanel, BoxLayout.Y_AXIS));
        registeredListPanel.setOpaque(false);
        showEmptyEnrolledMessage();

        JScrollPane scroll = new JScrollPane(registeredListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildEnrolledRow(EnrolledCourse course) {
        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(27, 130, 60));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(3, 8, 3, getHeight() - 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setLayout(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(course.getCourseName());
        nameLbl.setFont(AppTheme.headingFont(12));
        nameLbl.setForeground(AppTheme.TEXT_DARK);

        JLabel codeLbl = new JLabel(
            course.getCourseCode() + "  ·  " + course.getCreditHours() + " credit hrs");
        codeLbl.setFont(AppTheme.bodyFont(11));
        codeLbl.setForeground(AppTheme.TEXT_MUTED);

        left.add(nameLbl);
        left.add(codeLbl);

        JLabel check = new JLabel("\u2705");
        check.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        row.add(left,  BorderLayout.CENTER);
        row.add(check, BorderLayout.EAST);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EVENT HANDLERS  (call service, react to result — no logic here)
    // ─────────────────────────────────────────────────────────────────────────

    /** Called when the student picks a different course in either dropdown. */
    private void onCourseSelected(JComboBox<CourseItem> dropdown) {
        CourseItem selected = (CourseItem) dropdown.getSelectedItem();
        if (selected == null) {
            clearInfoTiles();
        } else {
            codeValueLabel.setText(selected.getCourseCode());
            creditsValueLabel.setText(selected.getCreditHours() + " hrs");
            teacherValueLabel.setText(selected.getTeacherName());
            scheduleValueLabel.setText(
                selected.getScheduleDays() + "  " + selected.getClassTime());
        }
    }

    /** Called when the student clicks "Register". */
    private void onRegisterClicked() {
        JComboBox<CourseItem> activeDropdown = getActiveDropdown();
        CourseItem selected = (CourseItem) activeDropdown.getSelectedItem();

        // Guard: nothing selected — fast path, no DB trip needed
        if (selected == null) {
            showStatus("\u26A0  Please select a course first.", AppTheme.GOLD_DARK);
            return;
        }

        // Disable the button for the duration of the DB call so double-clicking
        // cannot fire a second registration before the first completes.
        registerBtn.setEnabled(false);
        showStatus("Registering\u2026", AppTheme.TEXT_MUTED);

        new SwingWorker<RegisterResult, Void>() {
            @Override
            protected RegisterResult doInBackground() {
                return service.register(selected);      // DB call — off EDT
            }

            @Override
            protected void done() {                     // back on EDT
                registerBtn.setEnabled(true);
                try {
                    RegisterResult result = get();
                    switch (result.outcome()) {
                        case NONE_SELECTED    ->
                            showStatus("\u26A0  " + result.message(), AppTheme.GOLD_DARK);
                        case ALREADY_ENROLLED ->
                            showStatus("\u26A0  " + result.message(), AppTheme.GOLD_DARK);
                        case ERROR            ->
                            showStatus("\u2716  " + result.message(), Color.RED);
                        case SUCCESS          -> {
                            showStatus("\u2705  " + result.message(),
                                       new Color(27, 130, 60));
                            // Add to enrolled panel
                            appendEnrolledRow(result.course());
                            // Remove from active dropdown so it cannot be re-registered
                            activeDropdown.removeItem(selected);
                            // Reset dropdown to placeholder and clear info tiles
                            activeDropdown.setSelectedIndex(0);
                            clearInfoTiles();
                        }
                    }
                } catch (Exception ex) {
                    showStatus("\u2716  Unexpected error: " + ex.getMessage(), Color.RED);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING  (calls service, no SQL here)
    // ─────────────────────────────────────────────────────────────────────────

    /** Populates the available courses dropdown from the service. */
    private void loadAvailableCourses() {
        // Keep placeholder at index 0; clear stale items synchronously on EDT
        while (courseDropdown.getItemCount() > 1) courseDropdown.removeItemAt(1);

        new SwingWorker<List<CourseItem>, Void>() {
            @Override
            protected List<CourseItem> doInBackground() {
                return service.getAvailableCourses();   // DB call — off EDT
            }

            @Override
            protected void done() {                     // back on EDT
                try {
                    List<CourseItem> courses = get();
                    courses.forEach(courseDropdown::addItem);
                } catch (Exception ex) {
                    showStatus("Could not load available courses: " + ex.getMessage(),
                               Color.RED);
                }
            }
        }.execute();
    }

    /** Populates the improvement courses dropdown from the service. */
    private void loadImprovementCourses() {
        while (improvementDropdown.getItemCount() > 1) improvementDropdown.removeItemAt(1);

        new SwingWorker<List<CourseItem>, Void>() {
            @Override
            protected List<CourseItem> doInBackground() {
                return service.getImprovementCourses();  // DB call — off EDT
            }

            @Override
            protected void done() {
                try {
                    List<CourseItem> courses = get();
                    courses.forEach(improvementDropdown::addItem);
                    // Update the tab title with count
                    String tabTitle = "  For Improvement";
                    if (!courses.isEmpty()) {
                        tabTitle += " (" + courses.size() + ")";
                    }
                    tabbedPane.setTitleAt(1, tabTitle);
                } catch (Exception ex) {
                    showStatus("Could not load improvement courses: " + ex.getMessage(),
                               Color.RED);
                }
            }
        }.execute();
    }

    /** Populates the enrolled list from the service off the EDT. */
    private void loadEnrolledCourses() {
        new SwingWorker<List<EnrolledCourse>, Void>() {
            @Override
            protected List<EnrolledCourse> doInBackground() {
                return service.getEnrolledCourses();    // DB call — off EDT
            }

            @Override
            protected void done() {                     // back on EDT
                try {
                    List<EnrolledCourse> enrolled = get();
                    if (enrolled.isEmpty()) {
                        showEmptyEnrolledMessage();
                        return;
                    }
                    registeredListPanel.removeAll();
                    registeredListPanel.add(Box.createVerticalStrut(4));
                    enrolled.forEach(course -> {
                        registeredListPanel.add(buildEnrolledRow(course));
                        registeredListPanel.add(Box.createVerticalStrut(8));
                    });
                    registeredListPanel.revalidate();
                    registeredListPanel.repaint();
                    countBadge.setText(String.valueOf(enrolled.size()));
                    countBadge.repaint();
                    int totalCr = service.getTotalCreditHours(enrolled);
                    creditHoursBadge.setText(totalCr + " cr.");
                } catch (Exception ex) {
                    showStatus("Could not load enrolled courses: " + ex.getMessage(),
                               Color.RED);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RENDER HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Appends one row to the enrolled panel after a successful registration. */
    private void appendEnrolledRow(CourseItem course) {
        // Convert CourseItem -> a lightweight EnrolledCourse for rendering
        model.EnrolledCourse ec = new model.EnrolledCourse(
            -1, course.getOfferingId(),
            course.getCourseCode(), course.getCourseName(),
            course.getCreditHours(), course.getSection(),
            course.getTeacherName(), java.time.LocalDate.now(), "active");

        if (countBadge.getText().equals("0")) {
            registeredListPanel.removeAll();
            registeredListPanel.add(Box.createVerticalStrut(4));
        }
        registeredListPanel.add(buildEnrolledRow(ec));
        registeredListPanel.add(Box.createVerticalStrut(8));
        registeredListPanel.revalidate();
        registeredListPanel.repaint();

        int newCount = Integer.parseInt(countBadge.getText()) + 1;
        countBadge.setText(String.valueOf(newCount));
        countBadge.repaint();
        // Update credit-hours total — parse current value and add new course
        String crText = creditHoursBadge.getText().replace(" cr.", "").trim();
        int currentCr = crText.isEmpty() ? 0 : Integer.parseInt(crText);
        creditHoursBadge.setText((currentCr + course.getCreditHours()) + " cr.");
    }

    private void showEmptyEnrolledMessage() {
        registeredListPanel.removeAll();
        JLabel emptyMsg = new JLabel("No courses registered yet.");
        emptyMsg.setFont(AppTheme.bodyFont(12));
        emptyMsg.setForeground(AppTheme.TEXT_MUTED);
        emptyMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        registeredListPanel.add(emptyMsg);
        registeredListPanel.revalidate();
        registeredListPanel.repaint();
        countBadge.setText("0");
        countBadge.repaint();
        creditHoursBadge.setText("0 cr.");
    }

    private void clearInfoTiles() {
        codeValueLabel.setText("\u2014");
        creditsValueLabel.setText("\u2014");
        teacherValueLabel.setText("\u2014");
        scheduleValueLabel.setText("\u2014");
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
        javax.swing.Timer timer = new javax.swing.Timer(4000,
            e -> statusLabel.setText(" "));
        timer.setRepeats(false);
        timer.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SHARED WIDGET HELPERS
    // ─────────────────────────────────────────────────────────────────────────

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

    private void addFieldLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(6));
    }

    private void styleDropdown(JComboBox<?> combo) {
        combo.setFont(AppTheme.bodyFont(13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }
}