package ui;

import bl.TeacherCoursesService;
import bl.TeacherCoursesService.CourseSummary;
import model.CourseStudent;
import model.TeacherCourse;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * TeacherCoursesPanel — Pure UI layer.
 *
 * What this class does:
 *   - Renders the header with live course-count and enrolment summary badges
 *   - Renders one card per assigned course (code, name, section,
 *     schedule, room, enrolled/capacity, "View Students" button)
 *   - Opens a student-roster dialog when "View Students" is clicked
 *   - Delegates ALL data loading to TeacherCoursesService
 *
 * What this class does NOT do:
 *   - No SQL
 *   - No hardcoded course arrays
 *   - No direct DAO calls
 *
 * Integration in TeacherDashboard:
 *   contentArea.add(new TeacherCoursesPanel(username), BorderLayout.CENTER);
 */
public class TeacherCoursesPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color GREEN_BG = new Color(220, 252, 231);
    private static final Color GREEN_FG = new Color(21, 128, 61);
    private static final Color RED_FG   = new Color(198, 40, 40);
    private static final Color AMBER_FG = new Color(180, 100, 0);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String               username;
    private final TeacherCoursesService service;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JLabel badgeNum;
    private JLabel badgeEnrolled;
    private JPanel grid;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────
    public TeacherCoursesPanel(String username) {
        this.username = username;
        this.service  = new TeacherCoursesService();
        init();
    }

    /** Injection constructor for tests or custom service wiring. */
    public TeacherCoursesPanel(String username, TeacherCoursesService service) {
        this.username = username;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildGridScroll(), BorderLayout.CENTER);

        if (!service.init(username)) {
            // Replace grid with an error message
            removeAll();
            add(buildHeader(), BorderLayout.NORTH);
            JLabel err = new JLabel("Failed to load teacher account. Please re-login.");
            err.setFont(AppTheme.bodyFont(13));
            err.setForeground(RED_FG);
            add(err, BorderLayout.CENTER);
        } else {
            loadCourses();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        // Title block
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("My Courses");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Courses assigned to you this semester");
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

        // Summary badge (populated after data loads)
        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        badgeNum = new JLabel("—");
        badgeNum.setFont(AppTheme.titleFont(22));
        badgeNum.setForeground(AppTheme.NAVY);
        badgeNum.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel badgeLbl = new JLabel("Assigned Courses");
        badgeLbl.setFont(AppTheme.bodyFont(11));
        badgeLbl.setForeground(AppTheme.TEXT_MUTED);
        badgeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        badgeEnrolled = new JLabel("— students");
        badgeEnrolled.setFont(AppTheme.bodyFont(10));
        badgeEnrolled.setForeground(AppTheme.TEXT_MUTED);
        badgeEnrolled.setAlignmentX(Component.CENTER_ALIGNMENT);

        badge.add(badgeNum);
        badge.add(badgeLbl);
        badge.add(Box.createVerticalStrut(2));
        badge.add(badgeEnrolled);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(badge,      BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GRID (scrollable, one card per course)
    // ─────────────────────────────────────────────────────────────────────────
    private JScrollPane buildGridScroll() {
        grid = new JPanel(new GridLayout(0, 1, 0, 16));
        grid.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(grid, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COURSE CARD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCourseCard(TeacherCourse course) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(AppTheme.GOLD);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // ── LEFT: course info ─────────────────────────────────────────────────
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(course.getCourseName());
        nameLbl.setFont(AppTheme.titleFont(16));
        nameLbl.setForeground(AppTheme.NAVY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 1 meta: code badge + section + enrolment count
        JPanel metaRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        metaRow1.setOpaque(false);
        metaRow1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel codeBadge = new JLabel(course.getCourseCode()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        codeBadge.setFont(AppTheme.headingFont(11));
        codeBadge.setForeground(AppTheme.DEEP_BLUE);
        codeBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        codeBadge.setOpaque(false);

        JLabel sectionLbl = new JLabel("Section " + course.getSection());
        sectionLbl.setFont(AppTheme.bodyFont(12));
        sectionLbl.setForeground(AppTheme.TEXT_MUTED);

        // Enrolment pill: green if seats available, amber if nearly full, red if full
        int enrolled = course.getEnrolledCount();
        int capacity = course.getMaxCapacity();
        double fillRatio = capacity > 0 ? (double) enrolled / capacity : 1.0;
        Color enrolColor = fillRatio >= 1.0 ? RED_FG
                         : fillRatio >= 0.85 ? AMBER_FG
                         : GREEN_FG;
        JLabel enrolLbl = new JLabel("👥  " + enrolled + " / " + capacity + " enrolled");
        enrolLbl.setFont(AppTheme.bodyFont(12));
        enrolLbl.setForeground(enrolColor);

        metaRow1.add(codeBadge);
        metaRow1.add(sectionLbl);
        metaRow1.add(enrolLbl);

        // Row 2 meta: schedule + room
        JPanel metaRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        metaRow2.setOpaque(false);
        metaRow2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel scheduleLbl = new JLabel("🕐  " + course.getScheduleLabel());
        scheduleLbl.setFont(AppTheme.bodyFont(11));
        scheduleLbl.setForeground(AppTheme.TEXT_MUTED);
        metaRow2.add(scheduleLbl);

        info.add(nameLbl);
        info.add(Box.createVerticalStrut(5));
        info.add(metaRow1);
        info.add(Box.createVerticalStrut(3));
        info.add(metaRow2);

        // ── RIGHT: action button ──────────────────────────────────────────────
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(makeViewStudentsButton(course));

        card.add(info,    BorderLayout.CENTER);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VIEW STUDENTS BUTTON + DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private JButton makeViewStudentsButton(TeacherCourse course) {
        JButton btn = new JButton("👥  View Students") {
            boolean hovered = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(14, 100, 45) : GREEN_FG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.headingFont(11));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(160, 34));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onViewStudentsClicked(course));
        return btn;
    }

    /**
     * Opens the student-roster dialog for the clicked course.
     * Fetches data via the service, then delegates rendering to buildStudentDialog().
     */
    private void onViewStudentsClicked(TeacherCourse course) {
        List<CourseStudent> students = service.getStudentsForOffering(course.getOfferingId());
        showStudentDialog(course, students);
    }

    private void showStudentDialog(TeacherCourse course, List<CourseStudent> students) {
        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            course.getCourseCode() + " — " + course.getCourseName()
                + "  (Sec " + course.getSection() + ")",
            Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(560, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // ── Header row ────────────────────────────────────────────────────────
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(AppTheme.PALE_BLUE);
        headerRow.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel hTitle = new JLabel(course.getCourseName());
        hTitle.setFont(AppTheme.headingFont(14));
        hTitle.setForeground(AppTheme.NAVY);

        JLabel hMeta = new JLabel(
            course.getEnrolledCount() + " students  ·  "
            + course.getScheduleLabel());
        hMeta.setFont(AppTheme.bodyFont(11));
        hMeta.setForeground(AppTheme.TEXT_MUTED);

        JPanel hLeft = new JPanel();
        hLeft.setOpaque(false);
        hLeft.setLayout(new BoxLayout(hLeft, BoxLayout.Y_AXIS));
        hLeft.add(hTitle);
        hLeft.add(Box.createVerticalStrut(2));
        hLeft.add(hMeta);
        headerRow.add(hLeft, BorderLayout.CENTER);
        dialog.add(headerRow, BorderLayout.NORTH);

        // ── Student list ──────────────────────────────────────────────────────
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        if (students.isEmpty()) {
            JLabel empty = new JLabel("No students enrolled in this offering.");
            empty.setFont(AppTheme.bodyFont(12));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
            listPanel.add(empty);
        } else {
            // Column headers
            listPanel.add(buildDialogHeaderRow());
            listPanel.add(Box.createVerticalStrut(4));
            for (int i = 0; i < students.size(); i++) {
                listPanel.add(buildDialogStudentRow(students.get(i), i));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        dialog.add(scroll, BorderLayout.CENTER);

        // ── Close button ──────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(AppTheme.PALE_BLUE);
        JButton closeBtn = new StyledButton("Close", AppTheme.DEEP_BLUE, AppTheme.MID_BLUE);
        closeBtn.setPreferredSize(new Dimension(100, 34));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel buildDialogHeaderRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 8, 0));
        row.setBackground(new Color(240, 245, 255));
        row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        for (String col : new String[]{"#  Name", "Roll No.", "Dept", "Attendance"}) {
            JLabel lbl = new JLabel(col);
            lbl.setFont(AppTheme.bodyFont(10));
            lbl.setForeground(AppTheme.TEXT_MUTED);
            row.add(lbl);
        }
        return row;
    }

    private JPanel buildDialogStudentRow(CourseStudent s, int index) {
        JPanel row = new JPanel(new GridLayout(1, 4, 8, 0));
        Color bg = index % 2 == 0 ? Color.WHITE : new Color(249, 252, 255);
        row.setBackground(bg);
        row.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel nameLbl = new JLabel((index + 1) + ".  " + s.getFullName());
        nameLbl.setFont(AppTheme.bodyFont(12));
        nameLbl.setForeground(AppTheme.TEXT_DARK);

        JLabel rollLbl = new JLabel(s.getRollNumber());
        rollLbl.setFont(AppTheme.bodyFont(11));
        rollLbl.setForeground(AppTheme.TEXT_MUTED);

        JLabel deptLbl = new JLabel(s.getSubDeptCode());
        deptLbl.setFont(AppTheme.bodyFont(11));
        deptLbl.setForeground(AppTheme.TEXT_MUTED);

        int pct = s.getAttendancePct();
        JLabel attLbl = new JLabel(pct + "%");
        attLbl.setFont(AppTheme.headingFont(11));
        attLbl.setForeground(pct >= 80 ? GREEN_FG : pct >= 65 ? AMBER_FG : RED_FG);

        row.add(nameLbl);
        row.add(rollLbl);
        row.add(deptLbl);
        row.add(attLbl);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadCourses() {
        List<TeacherCourse> courses = service.getCourses();
        CourseSummary summary       = service.getSummary(courses);

        // Update header badge with real data
        badgeNum.setText(String.valueOf(summary.totalCourses()));
        badgeEnrolled.setText(summary.totalEnrolled() + " students total");

        // Populate grid
        grid.removeAll();
        if (courses.isEmpty()) {
            JLabel empty = new JLabel("No courses assigned this semester.");
            empty.setFont(AppTheme.bodyFont(13));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            grid.add(empty);
        } else {
            courses.forEach(c -> grid.add(buildCourseCard(c)));
        }
        grid.revalidate();
        grid.repaint();
    }
}
