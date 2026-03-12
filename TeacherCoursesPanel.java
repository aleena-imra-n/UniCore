import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class TeacherCoursesPanel extends JPanel {

    // ── Sample data: {courseID, courseName, enrolledStudents} ─────────────────
    private static final String[][] COURSES = {
        {"CS3009", "Software Engineering",         "32"},
        {"CS3011", "Operating Systems",            "28"},
        {"CS3015", "Data Structures & Algorithms", "40"},
    };

    public TeacherCoursesPanel(String teacherName) {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(teacherName), BorderLayout.NORTH);
        add(buildCourseGrid(),        BorderLayout.CENTER);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader(String teacherName) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

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

        // Summary badge — total courses
        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        JLabel badgeNum = new JLabel(String.valueOf(COURSES.length));
        badgeNum.setFont(AppTheme.titleFont(22));
        badgeNum.setForeground(AppTheme.NAVY);
        badgeNum.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel badgeLbl = new JLabel("Assigned Courses");
        badgeLbl.setFont(AppTheme.bodyFont(11));
        badgeLbl.setForeground(AppTheme.TEXT_MUTED);
        badgeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        badge.add(badgeNum);
        badge.add(badgeLbl);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(badge,      BorderLayout.EAST);
        return header;
    }

    // ── Course Grid ───────────────────────────────────────────────────────────
    private JScrollPane buildCourseGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 1, 0, 16));
        grid.setOpaque(false);

        for (String[] course : COURSES) {
            grid.add(buildCourseCard(course[0], course[1], course[2]));
        }

        // Pad bottom
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

    // ── Course Card ───────────────────────────────────────────────────────────
    private JPanel buildCourseCard(String id, String name, String students) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Left accent bar in gold
                g2.setColor(AppTheme.GOLD);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // LEFT — course info
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        // Course name
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(AppTheme.titleFont(16));
        nameLbl.setForeground(AppTheme.NAVY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Course ID badge + students row
        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel idBadge = new JLabel(id) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        idBadge.setFont(AppTheme.headingFont(11));
        idBadge.setForeground(AppTheme.DEEP_BLUE);
        idBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        idBadge.setOpaque(false);

        JLabel studentLbl = new JLabel("👥  " + students + " students enrolled");
        studentLbl.setFont(AppTheme.bodyFont(12));
        studentLbl.setForeground(AppTheme.TEXT_MUTED);

        metaRow.add(idBadge);
        metaRow.add(studentLbl);

        info.add(nameLbl);
        info.add(Box.createVerticalStrut(6));
        info.add(metaRow);

        // RIGHT — action button
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        actions.add(makeActionButton("👥  View Students", new Color(21, 128, 61), new Color(14, 100, 45), id));

        card.add(info,    BorderLayout.CENTER);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private JButton makeActionButton(String label, Color bg, Color hover, String courseId) {
        JButton btn = new JButton(label) {
            boolean hovered = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? hover : bg);
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
        btn.addActionListener(e -> JOptionPane.showMessageDialog(
            this,
            label.trim() + " for course " + courseId + "\n(Coming in future sprint)",
            "Feature Preview",
            JOptionPane.INFORMATION_MESSAGE
        ));
        return btn;
    }
}