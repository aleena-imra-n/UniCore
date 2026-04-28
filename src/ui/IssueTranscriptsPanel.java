package ui;

import bl.IssueTranscriptService;
import dao.TranscriptDAO.TranscriptData;
import model.StudentSearchResult;
import model.TranscriptCourseRow;
import model.TranscriptSemesterRow;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.List;
import java.util.Map;

/**
 * IssueTranscriptsPanel — Admin UI for US-3.14a.
 *
 * Layout:
 *
 *  ┌──────────────────────────────────────────────────────────────────┐
 *  │  HEADER — title, subtitle, gold accent                           │
 *  ├───────────────────────┬──────────────────────────────────────────┤
 *  │  LEFT PANE  (320 px)  │  RIGHT PANE  (remainder)                 │
 *  │  ─────────────────    │  ─────────────────────────────────────   │
 *  │  Search field         │  Transcript preview (scrollable)         │
 *  │  Results list         │  mirrors StudentTranscriptPanel visuals  │
 *  │  [selected student    │                                          │
 *  │   info chip]          │  Export / Print button (bottom bar)      │
 *  └───────────────────────┴──────────────────────────────────────────┘
 *
 * Integration in AdminDashboard:
 *   contentArea.add(new IssueTranscriptsPanel(username), BorderLayout.CENTER);
 */
public class IssueTranscriptsPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color GREEN_FG   = new Color( 21, 128,  61);
    private static final Color GREEN_BG   = new Color(220, 252, 231);
    private static final Color AMBER_FG   = new Color(180, 100,   0);
    private static final Color AMBER_BG   = new Color(255, 248, 225);
    private static final Color RED_FG     = new Color(198,  40,  40);
    private static final Color RED_BG     = new Color(255, 235, 238);
    private static final Color TABLE_HEAD = new Color(227, 242, 253);
    private static final Color TABLE_ALT  = new Color(245, 250, 255);
    private static final Color DIVIDER    = new Color(215, 228, 248);
    private static final Color LIST_SEL   = new Color(210, 232, 255);
    private static final Color LIST_HOV   = new Color(235, 245, 255);

    // Transcript column widths — must match StudentTranscriptPanel exactly
    private static final int[]    COL_WIDTHS = { 80, 200, 55, 100, 70, 55, 60 };
    private static final String[] COL_HEADS  = { "Code","Course Name","Credits","Marks","%","Grade","Pts" };

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final String                 adminUsername;
    private final IssueTranscriptService service;

    // ── State ─────────────────────────────────────────────────────────────────
    private StudentSearchResult   selectedStudent;
    private TranscriptData        currentData;
    private Timer                 searchDebounce;

    // ── Left pane refs ────────────────────────────────────────────────────────
    private JTextField  searchField;
    private JLabel      searchStatusLabel;
    private JPanel      resultsList;
    private JPanel      studentChip;
    private JLabel      chipName;
    private JLabel      chipMeta;

    // ── Right pane refs ───────────────────────────────────────────────────────
    private JPanel      previewContent;
    private JLabel      previewCgpaLabel;
    private JLabel      previewCreditsLabel;
    private JLabel      previewCoursesLabel;
    private JPanel      emptyPreview;
    private JPanel      transcriptView;
    private StyledButton printBtn;
    private JLabel      printStatusLabel;

    // ─────────────────────────────────────────────────────────────────────────
    public IssueTranscriptsPanel(String adminUsername) {
        this(adminUsername, new IssueTranscriptService());
    }

    public IssueTranscriptsPanel(String adminUsername, IssueTranscriptService service) {
        this.adminUsername = adminUsername;
        this.service       = service;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
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

        JLabel title = new JLabel("Issue Transcripts");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Search a student, preview their transcript, and export for official issuance");
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
        header.add(titleBlock, BorderLayout.WEST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BODY
    // ─────────────────────────────────────────────────────────────────────────
    private JSplitPane buildBody() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildLeftPane(), buildRightPane());
        split.setDividerLocation(310);
        split.setDividerSize(6);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false);
        split.getLeftComponent().setMinimumSize(new Dimension(240, 0));
        split.getRightComponent().setMinimumSize(new Dimension(300, 0));
        return split;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LEFT PANE — search + results list
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildLeftPane() {
        JPanel pane = new JPanel(new BorderLayout(0, 12));
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        // ── Search field ──────────────────────────────────────────────────────
        JPanel searchBar = new JPanel(new BorderLayout(6, 0));
        searchBar.setOpaque(false);

        searchField = new JTextField();
        searchField.setFont(AppTheme.bodyFont(13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        searchField.setOpaque(false);
        searchField.putClientProperty("JTextField.placeholderText",
            "Name or roll number…");

        searchStatusLabel = new JLabel("Type at least 2 characters");
        searchStatusLabel.setFont(AppTheme.bodyFont(11));
        searchStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        searchBar.add(searchField, BorderLayout.CENTER);

        // Debounced search: fire 350ms after the user stops typing
        searchDebounce = new Timer(350, e -> fireSearch());
        searchDebounce.setRepeats(false);
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { debounce(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { debounce(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { debounce(); }
            });

        // ── Results list (scrollable) ─────────────────────────────────────────
        resultsList = new JPanel();
        resultsList.setLayout(new BoxLayout(resultsList, BoxLayout.Y_AXIS));
        resultsList.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(resultsList, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        // ── Student chip (shown when a result is selected) ────────────────────
        studentChip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.MID_BLUE);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        studentChip.setOpaque(false);
        studentChip.setLayout(new BoxLayout(studentChip, BoxLayout.Y_AXIS));
        studentChip.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        studentChip.setVisible(false);

        chipName = new JLabel("—");
        chipName.setFont(AppTheme.headingFont(13));
        chipName.setForeground(AppTheme.NAVY);
        chipName.setAlignmentX(Component.LEFT_ALIGNMENT);

        chipMeta = new JLabel("—");
        chipMeta.setFont(AppTheme.bodyFont(11));
        chipMeta.setForeground(AppTheme.TEXT_MUTED);
        chipMeta.setAlignmentX(Component.LEFT_ALIGNMENT);

        studentChip.add(chipName);
        studentChip.add(Box.createVerticalStrut(3));
        studentChip.add(chipMeta);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(searchBar,    BorderLayout.NORTH);
        top.add(searchStatusLabel, BorderLayout.CENTER);
        top.add(studentChip,  BorderLayout.SOUTH);

        pane.add(top,    BorderLayout.NORTH);
        pane.add(scroll, BorderLayout.CENTER);
        return pane;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RIGHT PANE — transcript preview + print toolbar
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildRightPane() {
        JPanel pane = new JPanel(new BorderLayout(0, 0));
        pane.setOpaque(false);

        // ── Preview area ──────────────────────────────────────────────────────
        // Empty state
        emptyPreview = new JPanel(new GridBagLayout());
        emptyPreview.setOpaque(false);
        JPanel emptyInner = new JPanel();
        emptyInner.setOpaque(false);
        emptyInner.setLayout(new BoxLayout(emptyInner, BoxLayout.Y_AXIS));
        JLabel emptyIcon = new JLabel("📄");
        emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emptyMsg = new JLabel("Search and select a student");
        emptyMsg.setFont(AppTheme.titleFont(16));
        emptyMsg.setForeground(AppTheme.NAVY);
        emptyMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emptyMsg2 = new JLabel("to preview and issue their transcript");
        emptyMsg2.setFont(AppTheme.bodyFont(13));
        emptyMsg2.setForeground(AppTheme.TEXT_MUTED);
        emptyMsg2.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyInner.add(emptyIcon);
        emptyInner.add(Box.createVerticalStrut(12));
        emptyInner.add(emptyMsg);
        emptyInner.add(Box.createVerticalStrut(4));
        emptyInner.add(emptyMsg2);
        emptyPreview.add(emptyInner);

        // Transcript view (shown when data is loaded)
        transcriptView = new JPanel(new BorderLayout(0, 0));
        transcriptView.setOpaque(false);
        transcriptView.setVisible(false);

        // Stat badges row inside transcript view (top)
        JPanel statBadgesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        statBadgesRow.setOpaque(false);
        statBadgesRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        previewCgpaLabel    = addStatBadge(statBadgesRow, "—", "Overall CGPA",   AppTheme.MID_BLUE);
        previewCreditsLabel = addStatBadge(statBadgesRow, "—", "Credit Hours",    AppTheme.DEEP_BLUE);
        previewCoursesLabel = addStatBadge(statBadgesRow, "—", "Courses Done",    GREEN_FG);

        // Scrollable content
        previewContent = new JPanel();
        previewContent.setLayout(new BoxLayout(previewContent, BoxLayout.Y_AXIS));
        previewContent.setOpaque(false);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(previewContent, BorderLayout.NORTH);

        JScrollPane previewScroll = new JScrollPane(contentWrapper);
        previewScroll.setOpaque(false);
        previewScroll.getViewport().setOpaque(false);
        previewScroll.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
        previewScroll.getVerticalScrollBar().setUnitIncrement(16);

        transcriptView.add(statBadgesRow, BorderLayout.NORTH);
        transcriptView.add(previewScroll, BorderLayout.CENTER);

        // Stack: emptyPreview / transcriptView using CardLayout
        JPanel previewStack = new JPanel(new CardLayout());
        previewStack.setOpaque(false);
        previewStack.add(emptyPreview,   "empty");
        previewStack.add(transcriptView, "transcript");

        pane.add(previewStack, BorderLayout.CENTER);

        // ── Bottom toolbar ────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER),
            BorderFactory.createEmptyBorder(4, 0, 0, 0)));

        printBtn = new StyledButton("🖨  Print / Export PDF",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        printBtn.setPreferredSize(new Dimension(200, 40));
        printBtn.setFont(AppTheme.headingFont(13));
        printBtn.setEnabled(false);
        printBtn.addActionListener(e -> handlePrint());

        printStatusLabel = new JLabel(" ");
        printStatusLabel.setFont(AppTheme.bodyFont(12));
        printStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        toolbar.add(printBtn);
        toolbar.add(printStatusLabel);
        pane.add(toolbar, BorderLayout.SOUTH);

        return pane;
    }

    private JLabel addStatBadge(JPanel parent, String value,
                                 String label, Color numColor) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setPreferredSize(new Dimension(120, 60));

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(AppTheme.titleFont(18));
        valLbl.setForeground(numColor);
        valLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel txtLbl = new JLabel(label);
        txtLbl.setFont(AppTheme.bodyFont(10));
        txtLbl.setForeground(AppTheme.TEXT_MUTED);
        txtLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(valLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(txtLbl);
        parent.add(card);
        return valLbl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SEARCH
    // ─────────────────────────────────────────────────────────────────────────
    private void debounce() {
        searchDebounce.restart();
    }

    private void fireSearch() {
        String term = searchField.getText().trim();
        if (term.length() < IssueTranscriptService.MIN_SEARCH_LENGTH) {
            resultsList.removeAll();
            searchStatusLabel.setText("Type at least 2 characters to search");
            searchStatusLabel.setForeground(AppTheme.TEXT_MUTED);
            resultsList.revalidate();
            resultsList.repaint();
            return;
        }

        searchStatusLabel.setText("Searching…");
        searchStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        new SwingWorker<List<StudentSearchResult>, Void>() {
            @Override
            protected List<StudentSearchResult> doInBackground() {
                return service.search(term);
            }

            @Override
            protected void done() {
                try {
                    List<StudentSearchResult> results = get();
                    populateResultsList(results);
                } catch (Exception ex) {
                    searchStatusLabel.setText("Search error: " + ex.getMessage());
                    searchStatusLabel.setForeground(RED_FG);
                }
            }
        }.execute();
    }

    private void populateResultsList(List<StudentSearchResult> results) {
        resultsList.removeAll();

        if (results.isEmpty()) {
            JLabel empty = new JLabel("  No students found");
            empty.setFont(AppTheme.bodyFont(12));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
            resultsList.add(empty);
            searchStatusLabel.setText("No results");
            searchStatusLabel.setForeground(AppTheme.TEXT_MUTED);
        } else {
            for (StudentSearchResult r : results) {
                resultsList.add(buildResultRow(r));
            }
            searchStatusLabel.setText(results.size() + " result"
                + (results.size() == 1 ? "" : "s"));
            searchStatusLabel.setForeground(AppTheme.TEXT_MUTED);
        }

        resultsList.revalidate();
        resultsList.repaint();
    }

    private JPanel buildResultRow(StudentSearchResult r) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            boolean hovered = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { selectStudent(r); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                g.setColor(hovered ? LIST_HOV : Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                // Bottom separator
                g.setColor(DIVIDER);
                g.fillRect(0, getHeight()-1, getWidth(), 1);
            }
        };
        row.setOpaque(false);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(r.getFullName());
        nameLbl.setFont(AppTheme.headingFont(13));
        nameLbl.setForeground(AppTheme.NAVY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel metaLbl = new JLabel(r.getSummaryLine());
        metaLbl.setFont(AppTheme.bodyFont(11));
        metaLbl.setForeground(AppTheme.TEXT_MUTED);
        metaLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBlock.add(nameLbl);
        textBlock.add(Box.createVerticalStrut(2));
        textBlock.add(metaLbl);

        // Active / inactive badge
        JLabel badge = makePill(r.isActive() ? "Active" : "Inactive",
            r.isActive() ? GREEN_BG : RED_BG,
            r.isActive() ? GREEN_FG : RED_FG);

        row.add(textBlock, BorderLayout.CENTER);
        row.add(badge,     BorderLayout.EAST);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STUDENT SELECTION — load transcript
    // ─────────────────────────────────────────────────────────────────────────
    private void selectStudent(StudentSearchResult r) {
        selectedStudent = r;
        currentData     = null;
        printBtn.setEnabled(false);

        // Update chip
        chipName.setText(r.getFullName());
        chipMeta.setText(r.getRollNumber() + "  ·  " + r.getSubDeptCode()
            + "  ·  " + r.getMajorDeptName());
        studentChip.setVisible(true);

        // Show transcript area with loading state
        CardLayout cl = (CardLayout) ((JPanel)transcriptView.getParent()).getLayout();
        cl.show((JPanel)transcriptView.getParent(), "transcript");
        transcriptView.setVisible(true);
        emptyPreview.setVisible(false);

        previewContent.removeAll();
        JLabel loading = new JLabel("Loading transcript for " + r.getFullName() + "…");
        loading.setFont(AppTheme.bodyFont(13));
        loading.setForeground(AppTheme.TEXT_MUTED);
        loading.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        previewContent.add(loading);
        previewContent.revalidate();
        previewContent.repaint();

        printStatusLabel.setText(" ");

        // Load off EDT
        new SwingWorker<TranscriptData, Void>() {
            @Override
            protected TranscriptData doInBackground() {
                return service.loadTranscript(r.getStudentId());
            }

            @Override
            protected void done() {
                try {
                    TranscriptData data = get();
                    if (data == null) {
                        showPreviewError("No graded transcript data found for "
                            + r.getFullName()
                            + ". Marks may not have been uploaded yet.");
                        return;
                    }
                    currentData = data;
                    populatePreview(data);
                    printBtn.setEnabled(true);
                } catch (Exception ex) {
                    showPreviewError("Error loading transcript: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TRANSCRIPT PREVIEW RENDERER
    //  Mirrors StudentTranscriptPanel's visual style exactly so the admin
    //  sees what the student sees, plus the stat badges at the top.
    // ─────────────────────────────────────────────────────────────────────────
    private void populatePreview(TranscriptData data) {
        // Update stat badges
        previewCgpaLabel.setText(String.format("%.2f", data.getOverallCgpa()));
        previewCgpaLabel.setForeground(gpaColor(data.getOverallCgpa()));
        previewCreditsLabel.setText(String.valueOf(data.getTotalCreditHours()));
        previewCoursesLabel.setText(String.valueOf(data.getTotalCourses()));

        Map<String, List<TranscriptCourseRow>> grouped =
            service.groupBySemester(data.courseRows());
        Map<String, TranscriptSemesterRow> semIndex =
            service.buildSemesterIndex(data.semesterRows());

        previewContent.removeAll();

        for (Map.Entry<String, List<TranscriptCourseRow>> entry : grouped.entrySet()) {
            String semName  = entry.getKey();
            List<TranscriptCourseRow> courses = entry.getValue();
            TranscriptSemesterRow semRow = semIndex.get(semName);
            previewContent.add(buildSemesterBlock(semName, courses, semRow));
            previewContent.add(Box.createVerticalStrut(18));
        }

        // Final CGPA banner
        if (!data.semesterRows().isEmpty()) {
            TranscriptSemesterRow last = data.semesterRows()
                                             .get(data.semesterRows().size() - 1);
            previewContent.add(buildCgpaBanner(last.getCgpaSoFar(),
                data.getTotalCreditHours(), data.getTotalCourses()));
        }

        previewContent.revalidate();
        previewContent.repaint();
    }

    // ── Semester block (same visuals as StudentTranscriptPanel) ───────────────
    private JPanel buildSemesterBlock(String semName,
                                       List<TranscriptCourseRow> courses,
                                       TranscriptSemesterRow semRow) {
        JPanel block = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(AppTheme.DEEP_BLUE);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Semester header
        JPanel semHeader = new JPanel(new BorderLayout());
        semHeader.setOpaque(false);
        semHeader.setBorder(BorderFactory.createEmptyBorder(14, 22, 8, 22));

        JLabel semLabel = new JLabel(semName);
        semLabel.setFont(AppTheme.headingFont(14));
        semLabel.setForeground(AppTheme.NAVY);

        String meta = courses.size() + (courses.size()==1?" course":" courses")
            + (semRow!=null? "  ·  " + semRow.getSemCreditHours() + " credit hours":"");
        JLabel semMeta = new JLabel(meta);
        semMeta.setFont(AppTheme.bodyFont(11));
        semMeta.setForeground(AppTheme.TEXT_MUTED);

        semHeader.add(semLabel, BorderLayout.WEST);
        semHeader.add(semMeta,  BorderLayout.EAST);

        // Separator
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(DIVIDER); g.fillRect(22, 0, getWidth()-44, 1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 1); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 1); }
        };
        sep.setOpaque(false);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Column header row
        JPanel tableHead = buildTableRow(COL_HEADS, null, TABLE_HEAD, true);

        block.add(semHeader);
        block.add(sep);
        block.add(tableHead);

        boolean alt = false;
        for (TranscriptCourseRow row : courses) {
            String[] cells = {
                row.getCourseCode(),
                row.getCourseName(),
                String.valueOf(row.getCreditHours()),
                String.format("%.0f / %.0f", row.getMarksObtained(), row.getMarksTotal()),
                row.getPercentageStr(),
                row.getLetterGrade(),
                String.format("%.2f", row.getGradePoints())
            };
            block.add(buildTableRow(cells, gradeColor(row.getLetterGrade()),
                alt ? TABLE_ALT : Color.WHITE, false));
            alt = !alt;
        }

        if (semRow != null) block.add(buildSemGpaFooter(semRow));
        block.add(Box.createVerticalStrut(4));
        return block;
    }

    private JPanel buildTableRow(String[] cells, Color gradeColor,
                                  Color bg, boolean isHeader) {
        final Color rowBg = bg;
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(rowBg); g.fillRect(0,0,getWidth(),getHeight());
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 22, 5, 22));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isHeader ? 30 : 34));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Font font = isHeader ? AppTheme.headingFont(11) : AppTheme.bodyFont(11);
        Color defaultFg = isHeader ? AppTheme.DEEP_BLUE : AppTheme.TEXT_DARK;

        for (int i = 0; i < cells.length && i < COL_WIDTHS.length; i++) {
            JLabel lbl;
            if (!isHeader && i == 5 && gradeColor != null) {
                lbl = makePill(cells[i], gradeBackground(cells[i]), gradeColor);
            } else {
                lbl = new JLabel(cells[i]);
                lbl.setFont(font);
                lbl.setForeground(defaultFg);
            }
            lbl.setPreferredSize(new Dimension(COL_WIDTHS[i], isHeader ? 18 : 22));
            row.add(lbl);
        }
        return row;
    }

    private JPanel buildSemGpaFooter(TranscriptSemesterRow semRow) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(TABLE_HEAD); g.fillRect(0,0,getWidth(),getHeight());
            }
        };
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 22, 8, 22));
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        footer.add(makeGpaStat("Semester GPA", semRow.getSemesterGpaStr(),
            gpaColor(semRow.getSemesterGpa())));
        footer.add(makeSep());
        footer.add(makeGpaStat("CGPA so far", semRow.getCgpaSoFarStr(),
            gpaColor(semRow.getCgpaSoFar())));
        footer.add(makeSep());
        footer.add(makeGpaStat("Credit Hours",
            String.valueOf(semRow.getSemCreditHours()), AppTheme.DEEP_BLUE));
        return footer;
    }

    private JPanel buildCgpaBanner(double cgpa, int totalCredits, int totalCourses) {
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, AppTheme.DEEP_BLUE, getWidth(), 0, AppTheme.MID_BLUE);
                g2.setPaint(gp);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(18, getHeight()-2, getWidth()-18, getHeight()-2);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 26, 20, 26));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Cumulative GPA (CGPA)");
        t.setFont(AppTheme.headingFont(12));
        t.setForeground(AppTheme.LIGHT_BLUE);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(totalCourses + " courses  ·  " + totalCredits + " credit hours");
        s.setFont(AppTheme.bodyFont(11));
        s.setForeground(new Color(179, 229, 252, 200));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(t); left.add(Box.createVerticalStrut(4)); left.add(s);

        JLabel cgpaLbl = new JLabel(String.format("%.2f", cgpa));
        cgpaLbl.setFont(AppTheme.titleFont(32));
        cgpaLbl.setForeground(AppTheme.GOLD);

        JLabel standingLbl = new JLabel(standing(cgpa));
        standingLbl.setFont(AppTheme.headingFont(11));
        standingLbl.setForeground(Color.WHITE);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        cgpaLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        standingLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(cgpaLbl);
        right.add(standingLbl);

        banner.add(left,  BorderLayout.CENTER);
        banner.add(right, BorderLayout.EAST);
        return banner;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRINT / EXPORT
    // ─────────────────────────────────────────────────────────────────────────
    private void handlePrint() {
        if (selectedStudent == null || currentData == null) return;

        printStatusLabel.setText("Opening print dialog…");
        printStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        // Run off EDT so the dialog is responsive
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                TranscriptPrintable printable =
                    new TranscriptPrintable(selectedStudent, currentData);

                PrinterJob job = PrinterJob.getPrinterJob();
                job.setJobName("Transcript — " + selectedStudent.getFullName()
                    + " (" + selectedStudent.getRollNumber() + ")");
                PageFormat pf = printable.getA4PageFormat(job);
                job.setPrintable(printable, pf);

                if (job.printDialog()) {
                    job.print();
                    SwingUtilities.invokeLater(() -> {
                        printStatusLabel.setText("✓  Transcript sent to printer / exported.");
                        printStatusLabel.setForeground(GREEN_FG);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        printStatusLabel.setText("Print cancelled.");
                        printStatusLabel.setForeground(AppTheme.TEXT_MUTED);
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                try { get(); }
                catch (Exception ex) {
                    printStatusLabel.setText("⚠  Print error: " + ex.getMessage());
                    printStatusLabel.setForeground(RED_FG);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel makePill(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        lbl.setFont(AppTheme.headingFont(10));
        lbl.setForeground(fg);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        lbl.setOpaque(false);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private JPanel makeGpaStat(String label, String value, Color valueColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(AppTheme.bodyFont(11));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(AppTheme.headingFont(12));
        val.setForeground(valueColor);
        p.add(lbl); p.add(val);
        return p;
    }

    private JPanel makeSep() {
        JPanel s = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(DIVIDER); g.fillRect(0,4,1,getHeight()-8);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(1,26); }
        };
        s.setOpaque(false);
        return s;
    }

    private void showPreviewError(String msg) {
        previewContent.removeAll();
        JLabel err = new JLabel("⚠  " + msg);
        err.setFont(AppTheme.bodyFont(12));
        err.setForeground(RED_FG);
        err.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        previewContent.add(err);
        previewContent.revalidate();
        previewContent.repaint();
    }

    private Color gradeColor(String grade) {
        if (grade == null) return AppTheme.TEXT_MUTED;
        return switch (grade) {
            case "A+","A","A-" -> GREEN_FG;
            case "B+","B","B-" -> AppTheme.MID_BLUE;
            case "C+","C","C-" -> AMBER_FG;
            case "D"           -> new Color(150, 80, 0);
            default            -> RED_FG;
        };
    }

    private Color gradeBackground(String grade) {
        if (grade == null) return AppTheme.PALE_BLUE;
        return switch (grade) {
            case "A+","A","A-" -> GREEN_BG;
            case "B+","B","B-" -> AppTheme.PALE_BLUE;
            case "C+","C","C-" -> AMBER_BG;
            case "D"           -> new Color(255, 243, 220);
            default            -> RED_BG;
        };
    }

    private Color gpaColor(double gpa) {
        if (gpa >= 3.5) return GREEN_FG;
        if (gpa >= 2.5) return AppTheme.MID_BLUE;
        if (gpa >= 1.5) return AMBER_FG;
        return RED_FG;
    }

    private String standing(double cgpa) {
        if (cgpa >= 3.7) return "Distinction";
        if (cgpa >= 3.3) return "High Merit";
        if (cgpa >= 3.0) return "Merit";
        if (cgpa >= 2.0) return "Satisfactory";
        if (cgpa >= 1.0) return "Probation";
        return "Academic Warning";
    }
}