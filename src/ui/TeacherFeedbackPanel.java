package ui;

import bl.FeedbackService;
import model.FeedbackItem;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TeacherFeedbackPanel — Teacher view of course feedback (anonymous).
 *
 * Layout:
 *   One card per course that has feedback.
 *   Each card shows:
 *     - Course name + code + feedback count badge
 *     - Average rating as stars + number
 *     - Visual rating bar (filled proportional to avg/5)
 *     - Expandable list of individual comments (no student names)
 *
 * Integration in TeacherDashboard:
 *   case "Student Feedback" -> contentArea.add(new TeacherFeedbackPanel(username), BorderLayout.CENTER);
 */
public class TeacherFeedbackPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private static final Color GREEN = new Color(21, 128, 61);
    private static final Color AMBER = new Color(180, 100, 0);
    private static final Color RED_C = new Color(198, 40, 40);

    private final String          username;
    private final FeedbackService service;

    private JPanel cardsPanel;

    public TeacherFeedbackPanel(String username) {
        this.username = username;
        this.service  = new FeedbackService();
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildScrollArea(), BorderLayout.CENTER);

        loadData();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel tb = new JPanel();
        tb.setOpaque(false);
        tb.setLayout(new BoxLayout(tb, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Student Feedback");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Anonymous feedback from students — only courses with submissions shown");
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

        tb.add(title);
        tb.add(Box.createVerticalStrut(6));
        tb.add(sub);
        tb.add(Box.createVerticalStrut(10));
        tb.add(accent);
        header.add(tb, BorderLayout.WEST);
        return header;
    }

    // ── Scroll area ───────────────────────────────────────────────────────────
    private JScrollPane buildScrollArea() {
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(cardsPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ── Course feedback card ──────────────────────────────────────────────────
    private JPanel buildCourseCard(Object[] offeringData,
                                    List<FeedbackItem> feedbacks) {
        int    offeringId    = (int)    offeringData[0];
        String courseCode    = (String) offeringData[1];
        String courseName    = (String) offeringData[2];
        String section       = (String) offeringData[3];
        int    feedbackCount = (int)    offeringData[4];
        double avgRating     = (double) offeringData[5];

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Left accent coloured by rating
                Color ac = avgRating >= 4.0 ? GREEN
                         : avgRating >= 3.0 ? AMBER : RED_C;
                g2.setColor(ac);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // ── Top row: course info + rating summary ─────────────────────────────
        JPanel topRow = new JPanel(new BorderLayout(16, 0));
        topRow.setOpaque(false);

        JPanel courseInfo = new JPanel();
        courseInfo.setOpaque(false);
        courseInfo.setLayout(new BoxLayout(courseInfo, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(courseName);
        nameLbl.setFont(AppTheme.titleFont(16));
        nameLbl.setForeground(AppTheme.NAVY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        metaRow.add(codeBadge(courseCode));

        JLabel secLbl = new JLabel("Section " + section);
        secLbl.setFont(AppTheme.bodyFont(11));
        secLbl.setForeground(AppTheme.TEXT_MUTED);
        metaRow.add(secLbl);

        JLabel countLbl = new JLabel("💬  " + feedbackCount + " responses");
        countLbl.setFont(AppTheme.bodyFont(11));
        countLbl.setForeground(AppTheme.TEXT_MUTED);
        metaRow.add(countLbl);

        courseInfo.add(nameLbl);
        courseInfo.add(Box.createVerticalStrut(4));
        courseInfo.add(metaRow);

        // Rating tile
        JPanel ratingTile = buildRatingTile(avgRating);

        topRow.add(courseInfo,  BorderLayout.CENTER);
        topRow.add(ratingTile,  BorderLayout.EAST);
        card.add(topRow);
        card.add(Box.createVerticalStrut(16));

        // ── Rating bar ────────────────────────────────────────────────────────
        card.add(buildRatingBar(avgRating));
        card.add(Box.createVerticalStrut(16));

        // ── Comments section ──────────────────────────────────────────────────
        card.add(buildCommentsSection(feedbacks));

        return card;
    }

    private JPanel buildRatingTile(double avgRating) {
        Color ratingColor = avgRating >= 4.0 ? GREEN
                          : avgRating >= 3.0 ? AMBER : RED_C;
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ratingColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        tile.setPreferredSize(new Dimension(120, 70));

        JLabel avgLbl = new JLabel(String.format("%.1f", avgRating) + " / 5");
        avgLbl.setFont(AppTheme.titleFont(20));
        avgLbl.setForeground(Color.WHITE);
        avgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel starsLbl = new JLabel(starsString(avgRating));
        starsLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        starsLbl.setForeground(new Color(255, 255, 255, 200));
        starsLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(avgLbl);
        tile.add(Box.createVerticalStrut(2));
        tile.add(starsLbl);
        return tile;
    }

    private JPanel buildRatingBar(double avgRating) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lbl = new JLabel("Average Rating");
        lbl.setFont(AppTheme.headingFont(11));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(110, 18));

        Color barColor = avgRating >= 4.0 ? GREEN
                       : avgRating >= 3.0 ? AMBER : RED_C;

        JPanel track = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                int fillW = (int)(getWidth() * (avgRating / 5.0));
                if (fillW > 0) {
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 0, fillW, getHeight(), 8, 8);
                }
                g2.dispose();
            }
        };
        track.setPreferredSize(new Dimension(0, 12));

        JLabel pctLbl = new JLabel(String.format("%.0f%%", avgRating / 5.0 * 100));
        pctLbl.setFont(AppTheme.bodyFont(10));
        pctLbl.setForeground(AppTheme.TEXT_MUTED);
        pctLbl.setPreferredSize(new Dimension(38, 14));

        wrapper.add(lbl,    BorderLayout.WEST);
        wrapper.add(track,  BorderLayout.CENTER);
        wrapper.add(pctLbl, BorderLayout.EAST);
        return wrapper;
    }

    private JPanel buildCommentsSection(List<FeedbackItem> feedbacks) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel commTitle = new JLabel("Student Comments");
        commTitle.setFont(AppTheme.headingFont(12));
        commTitle.setForeground(AppTheme.DEEP_BLUE);
        commTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(commTitle);
        section.add(Box.createVerticalStrut(8));

        List<FeedbackItem> withComments = feedbacks.stream()
            .filter(f -> !f.getComments().isBlank())
            .toList();

        if (withComments.isEmpty()) {
            JLabel none = new JLabel("No written comments for this course.");
            none.setFont(AppTheme.bodyFont(12));
            none.setForeground(AppTheme.TEXT_MUTED);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            section.add(none);
        } else {
            for (FeedbackItem f : withComments) {
                section.add(buildCommentRow(f));
                section.add(Box.createVerticalStrut(6));
            }
        }
        return section;
    }

    private JPanel buildCommentRow(FeedbackItem f) {
        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setLayout(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel comment = new JLabel("\"" + f.getComments() + "\"");
        comment.setFont(AppTheme.bodyFont(12));
        comment.setForeground(AppTheme.TEXT_DARK);

        JLabel meta = new JLabel("Anonymous  ·  " + f.getSubmittedAt().format(FMT));
        meta.setFont(AppTheme.bodyFont(10));
        meta.setForeground(AppTheme.TEXT_MUTED);

        left.add(comment);
        left.add(Box.createVerticalStrut(2));
        left.add(meta);

        JLabel stars = new JLabel(starsString((double)f.getRating()));
        stars.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        stars.setForeground(AppTheme.GOLD_DARK);

        row.add(left,  BorderLayout.CENTER);
        row.add(stars, BorderLayout.EAST);
        return row;
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadData() {
        new SwingWorker<Void, Void>() {
            List<Object[]> offerings;

            @Override protected Void doInBackground() {
                if (!service.initTeacher(username)) return null;
                offerings = service.getOfferingsWithFeedback();
                return null;
            }
            @Override protected void done() {
                cardsPanel.removeAll();
                if (offerings == null || offerings.isEmpty()) {
                    JLabel empty = new JLabel(
                        "No feedback received for your courses yet.");
                    empty.setFont(AppTheme.bodyFont(13));
                    empty.setForeground(AppTheme.TEXT_MUTED);
                    empty.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
                    cardsPanel.add(empty);
                } else {
                    for (Object[] offering : offerings) {
                        int offeringId = (int) offering[0];
                        List<FeedbackItem> feedbacks =
                            service.getFeedbackForOffering(offeringId);
                        cardsPanel.add(buildCourseCard(offering, feedbacks));
                        cardsPanel.add(Box.createVerticalStrut(14));
                    }
                }
                cardsPanel.revalidate();
                cardsPanel.repaint();
            }
        }.execute();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String starsString(double rating) {
        int full = (int) Math.round(rating);
        return "★".repeat(Math.min(full, 5)) + "☆".repeat(Math.max(0, 5 - full));
    }

    private JLabel codeBadge(String code) {
        JLabel lbl = new JLabel(code) {
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
        lbl.setFont(AppTheme.headingFont(11));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        lbl.setOpaque(false);
        return lbl;
    }
}