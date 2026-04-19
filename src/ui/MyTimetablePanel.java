package ui;

import bl.MyTimetableService;
import model.TimetableEntry;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * MyTimetablePanel — Shared weekly grid view panel (US-3.7b).
 *
 * Used by both Teacher Dashboard ("My Timetable") and Student Dashboard
 * ("Timetable"). The only difference between the two modes is the data
 * source and the 4th line of each slot card:
 *   Teacher mode → dept code below the room
 *   Student mode → teacher name below the room
 *
 * Layout:
 *  ┌───────────────────────────────────────────────────────────────┐
 *  │  HEADER — title · stat badges (Courses | Slots | Hours)       │
 *  ├──────────┬──────────┬──────────┬──────────┬───────────────────┤
 *  │  Monday  │ Tuesday  │Wednesday │ Thursday │     Friday        │
 *  │ [slot]   │ [slot]   │ [slot]   │          │                   │
 *  │ [slot]   │          │ [slot]   │ [slot]   │ [slot]            │
 *  └──────────┴──────────┴──────────┴──────────┴───────────────────┘
 *
 * Construction:
 *   Teacher: new MyTimetablePanel(username, MyTimetablePanel.Role.TEACHER)
 *   Student: new MyTimetablePanel(username, MyTimetablePanel.Role.STUDENT)
 */
public class MyTimetablePanel extends JPanel {

    // ── Role flag ─────────────────────────────────────────────────────────────
    public enum Role { TEACHER, STUDENT }

    // ── Day colour palette (bg, accent) — one per weekday ────────────────────
    private static final Color[][] DAY_PALETTE = {
        { new Color(232, 240, 254), new Color( 66, 105, 225) },  // Monday:    indigo
        { new Color(224, 247, 242), new Color(  0, 150, 120) },  // Tuesday:   teal
        { new Color(243, 232, 255), new Color(130,  60, 200) },  // Wednesday: purple
        { new Color(255, 248, 225), new Color(200, 130,   0) },  // Thursday:  amber
        { new Color(255, 232, 236), new Color(200,  30,  70) },  // Friday:    rose
    };

    private static final Color RED_FG   = new Color(198,  40,  40);
    private static final Color EMPTY_FG = new Color(190, 205, 225);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String             username;
    private final Role               role;
    private final MyTimetableService service;

    // ── Header stat refs (updated after data loads) ───────────────────────────
    private JLabel statCourses;
    private JLabel statSlots;
    private JLabel statHours;

    // ── Grid container ────────────────────────────────────────────────────────
    private JPanel gridArea;

    // ─────────────────────────────────────────────────────────────────────────
    public MyTimetablePanel(String username, Role role) {
        this(username, role, new MyTimetableService());
    }

    public MyTimetablePanel(String username, Role role, MyTimetableService service) {
        this.username = username;
        this.role     = role;
        this.service  = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildGridScroll(), BorderLayout.CENTER);

        loadData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Title + subtitle + gold accent
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("My Timetable");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel(role == Role.TEACHER
            ? "Your scheduled classes this semester · Monday – Friday"
            : "Your enrolled courses this semester · Monday – Friday");
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

        // Stat badges
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        statsRow.setOpaque(false);

        JLabel[] c = new JLabel[1], s = new JLabel[1], h = new JLabel[1];
        statsRow.add(buildStatCard("—", "Courses",       AppTheme.MID_BLUE,           c));
        statsRow.add(buildStatCard("—", "Weekly Slots",  AppTheme.DEEP_BLUE,          s));
        statsRow.add(buildStatCard("—", "Contact Hours", new Color(21, 128, 61),      h));
        statCourses = c[0]; statSlots = s[0]; statHours = h[0];

        header.add(titleBlock, BorderLayout.WEST);
        header.add(statsRow,   BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatCard(String initValue, String label,
                                  Color color, JLabel[] outRef) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setPreferredSize(new Dimension(122, 62));

        JLabel valLbl = new JLabel(initValue);
        valLbl.setFont(AppTheme.titleFont(18));
        valLbl.setForeground(color);
        valLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel txtLbl = new JLabel(label);
        txtLbl.setFont(AppTheme.bodyFont(10));
        txtLbl.setForeground(AppTheme.TEXT_MUTED);
        txtLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(valLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(txtLbl);

        if (outRef != null && outRef.length > 0) outRef[0] = valLbl;
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SCROLLABLE GRID WRAPPER
    // ─────────────────────────────────────────────────────────────────────────
    private JScrollPane buildGridScroll() {
        gridArea = new JPanel(new BorderLayout());
        gridArea.setOpaque(false);

        JLabel loading = new JLabel("Loading timetable…");
        loading.setFont(AppTheme.bodyFont(13));
        loading.setForeground(AppTheme.TEXT_MUTED);
        loading.setHorizontalAlignment(SwingConstants.CENTER);
        gridArea.add(loading, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(gridArea);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────
    private void loadData() {
        new SwingWorker<List<TimetableEntry>, Void>() {
            @Override protected List<TimetableEntry> doInBackground() {
                return role == Role.TEACHER
                    ? service.loadForTeacher(username)
                    : service.loadForStudent(username);
            }
            @Override protected void done() {
                try { populateGrid(get()); }
                catch (Exception ex) { showError("Error: " + ex.getMessage()); }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GRID POPULATION
    // ─────────────────────────────────────────────────────────────────────────
    private void populateGrid(List<TimetableEntry> entries) {
        // Update badges
        statCourses.setText(String.valueOf(service.totalCourses(entries)));
        statSlots.setText(String.valueOf(service.totalSlots(entries)));
        statHours.setText(service.formatTotalTime(service.totalMinutes(entries)));

        gridArea.removeAll();

        if (entries.isEmpty()) {
            gridArea.add(buildEmptyState(), BorderLayout.CENTER);
        } else {
            Map<String, List<TimetableEntry>> byDay = service.groupByDay(entries);

            // Five-column grid — one column per weekday
            JPanel grid = new JPanel(new GridLayout(1, 5, 10, 0));
            grid.setOpaque(false);

            String[] days = MyTimetableService.DAYS;
            for (int di = 0; di < days.length; di++) {
                grid.add(buildDayColumn(days[di],
                    byDay.getOrDefault(days[di], List.of()), di));
            }

            gridArea.add(grid, BorderLayout.NORTH);
        }

        gridArea.revalidate();
        gridArea.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DAY COLUMN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildDayColumn(String day, List<TimetableEntry> entries,
                                   int dayIndex) {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(buildDayHeader(day, dayIndex, entries.size()));
        col.add(Box.createVerticalStrut(8));

        if (entries.isEmpty()) {
            col.add(buildEmptyColumnPlaceholder(dayIndex));
        } else {
            for (int i = 0; i < entries.size(); i++) {
                col.add(buildSlotCard(entries.get(i), dayIndex));
                if (i < entries.size() - 1) col.add(Box.createVerticalStrut(8));
            }
        }

        col.add(Box.createVerticalGlue());
        return col;
    }

    // ── Day column header ─────────────────────────────────────────────────────
    private JPanel buildDayHeader(String day, int dayIndex, int slotCount) {
        Color bg     = DAY_PALETTE[dayIndex][0];
        Color accent = DAY_PALETTE[dayIndex][1];

        JPanel hdr = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(accent);
                g2.fillRect(0, getHeight() - 4, getWidth(), 4);
                g2.dispose();
            }
        };
        hdr.setOpaque(false);
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.setBorder(BorderFactory.createEmptyBorder(10, 12, 14, 12));
        hdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JLabel dayLbl = new JLabel(day);
        dayLbl.setFont(AppTheme.headingFont(13));
        dayLbl.setForeground(accent);
        dayLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        String countStr = slotCount == 0 ? "No classes"
            : slotCount + (slotCount == 1 ? " class" : " classes");
        JLabel countLbl = new JLabel(countStr);
        countLbl.setFont(AppTheme.bodyFont(10));
        countLbl.setForeground(slotCount == 0 ? EMPTY_FG : accent);
        countLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        hdr.add(dayLbl);
        hdr.add(Box.createVerticalStrut(2));
        hdr.add(countLbl);
        return hdr;
    }

    // ── Slot card ─────────────────────────────────────────────────────────────
    private JPanel buildSlotCard(TimetableEntry e, int dayIndex) {
        Color bg     = DAY_PALETTE[dayIndex][0];
        Color accent = DAY_PALETTE[dayIndex][1];

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Accent left bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                // Subtle tint on the rest
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 55));
                g2.fillRect(5, 0, getWidth() - 5, getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Code + section
        JLabel codeLbl = new JLabel(e.getCourseCode() + "  (Sec " + e.getSection() + ")");
        codeLbl.setFont(AppTheme.headingFont(12));
        codeLbl.setForeground(accent);
        codeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Course name — clip if too long
        String name = e.getCourseName().length() > 28
            ? e.getCourseName().substring(0, 26) + "…"
            : e.getCourseName();
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(AppTheme.bodyFont(11));
        nameLbl.setForeground(AppTheme.NAVY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Time
        JLabel timeLbl = new JLabel("🕐  " + e.getStartTime() + " – " + e.getEndTime()
            + "  (" + e.getDurationLabel() + ")");
        timeLbl.setFont(AppTheme.bodyFont(10));
        timeLbl.setForeground(AppTheme.TEXT_MUTED);
        timeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Room
        JLabel roomLbl = new JLabel("📍  " + e.getRoomNumber());
        roomLbl.setFont(AppTheme.bodyFont(10));
        roomLbl.setForeground(AppTheme.TEXT_MUTED);
        roomLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 4th line: teacher name (student) or dept code (teacher)
        JLabel extraLbl = new JLabel(role == Role.STUDENT
            ? "👤  " + e.getTeacherName()
            : "🏛  " + e.getSubDeptCode());
        extraLbl.setFont(AppTheme.bodyFont(10));
        extraLbl.setForeground(AppTheme.TEXT_MUTED);
        extraLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(codeLbl);
        card.add(Box.createVerticalStrut(3));
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(timeLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(roomLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(extraLbl);

        // Hover border highlight
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent ev) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(12, accent, 1),
                    BorderFactory.createEmptyBorder(9, 13, 9, 9)));
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent ev) {
                card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
                card.repaint();
            }
        });

        return card;
    }

    // ── Empty column placeholder ──────────────────────────────────────────────
    private JPanel buildEmptyColumnPlaceholder(int dayIndex) {
        Color accent = DAY_PALETTE[dayIndex][1];
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 70));
        JLabel lbl = new JLabel("—");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 20));
        lbl.setForeground(new Color(accent.getRed(), accent.getGreen(),
            accent.getBlue(), 60));
        p.add(lbl);
        return p;
    }

    // ── Full empty state ──────────────────────────────────────────────────────
    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("📅");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 38));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel(role == Role.TEACHER
            ? "No timetable slots found for your courses."
            : "No timetable slots found for your enrolled courses.");
        msg.setFont(AppTheme.titleFont(16));
        msg.setForeground(AppTheme.NAVY);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Contact the admin to schedule timetable slots.");
        hint.setFont(AppTheme.bodyFont(12));
        hint.setForeground(EMPTY_FG);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(14));
        inner.add(msg);
        inner.add(Box.createVerticalStrut(6));
        inner.add(hint);
        p.add(inner);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ERROR STATE
    // ─────────────────────────────────────────────────────────────────────────
    private void showError(String msg) {
        gridArea.removeAll();
        JLabel err = new JLabel("⚠  " + msg);
        err.setFont(AppTheme.bodyFont(13));
        err.setForeground(RED_FG);
        err.setHorizontalAlignment(SwingConstants.CENTER);
        gridArea.add(err, BorderLayout.CENTER);
        gridArea.revalidate();
        gridArea.repaint();
    }
}