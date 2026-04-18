package ui;

import dao.TranscriptDAO.TranscriptData;
import model.StudentSearchResult;
import model.TranscriptCourseRow;
import model.TranscriptSemesterRow;

import java.awt.*;
import java.awt.print.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * TranscriptPrintable — Renders an official academic transcript using the
 * standard Java {@link java.awt.print.Printable} API.
 *
 * Why java.awt.print instead of an external library?
 *   - Zero external dependencies: works with any JDK 11+
 *   - Feeds directly into the OS print dialog, so the admin can:
 *       • Send to a physical printer
 *       • Save as PDF via "Microsoft Print to PDF" / "Save as PDF" (macOS/Linux)
 *   - No classpath additions needed in the student project
 *
 * Layout per page (A4 portrait, 72 dpi):
 *   ┌─────────────────────────────────────┐
 *   │  UniCore UMS letterhead (logo + name)│
 *   │  Horizontal rule                     │
 *   │  "OFFICIAL ACADEMIC TRANSCRIPT"      │
 *   │  Student info block (2 columns)      │
 *   │  ─────────────────────────────────── │
 *   │  [Semester heading]                  │
 *   │  [Course table: Code|Name|Cr|%|Grd|GP│
 *   │  [GPA footer row]                    │
 *   │  … (continues across pages)          │
 *   │  ─────────────────────────────────── │
 *   │  CGPA summary box (last page)        │
 *   │  Signature block (last page)         │
 *   │  Page n of N (footer)                │
 *   └─────────────────────────────────────┘
 *
 * Usage:
 *   TranscriptPrintable p = new TranscriptPrintable(student, data);
 *   PrinterJob job = PrinterJob.getPrinterJob();
 *   job.setPrintable(p, p.getA4PageFormat(job));
 *   if (job.printDialog()) job.print();
 */
public class TranscriptPrintable implements Printable {

    // ── Page geometry (points, 72pt = 1 inch) ─────────────────────────────────
    private static final double A4_W_PT  = 595.0;   // 210 mm
    private static final double A4_H_PT  = 842.0;   // 297 mm
    private static final double MARGIN_X = 50.0;
    private static final double MARGIN_T = 48.0;
    private static final double MARGIN_B = 52.0;
    private static final double CONTENT_W = A4_W_PT - 2 * MARGIN_X;
    private static final double CONTENT_H = A4_H_PT - MARGIN_T - MARGIN_B;

    // ── Fonts ──────────────────────────────────────────────────────────────────
    private static final Font F_INSTITUTION = new Font("SansSerif", Font.BOLD,  14);
    private static final Font F_HEADING     = new Font("SansSerif", Font.BOLD,  11);
    private static final Font F_TITLE       = new Font("SansSerif", Font.BOLD,  13);
    private static final Font F_LABEL       = new Font("SansSerif", Font.BOLD,   9);
    private static final Font F_VALUE       = new Font("SansSerif", Font.PLAIN,  9);
    private static final Font F_SEM_HEAD    = new Font("SansSerif", Font.BOLD,  10);
    private static final Font F_TABLE_HEAD  = new Font("SansSerif", Font.BOLD,   8);
    private static final Font F_TABLE_BODY  = new Font("SansSerif", Font.PLAIN,  8);
    private static final Font F_GPA_LABEL   = new Font("SansSerif", Font.BOLD,   9);
    private static final Font F_CGPA_BIG    = new Font("SansSerif", Font.BOLD,  18);
    private static final Font F_FOOTER      = new Font("SansSerif", Font.PLAIN,  7);

    // ── Colours ────────────────────────────────────────────────────────────────
    private static final Color C_NAVY      = new Color(10,  25,  60);
    private static final Color C_DEEP_BLUE = new Color(15,  52,  96);
    private static final Color C_MID_BLUE  = new Color(21, 101, 192);
    private static final Color C_GOLD      = new Color(200, 150,   0);
    private static final Color C_RULE      = new Color(180, 200, 230);
    private static final Color C_TBL_HEAD  = new Color(220, 235, 250);
    private static final Color C_TBL_ALT   = new Color(245, 250, 255);
    private static final Color C_GREEN     = new Color(21,  128,  61);
    private static final Color C_AMBER     = new Color(150,  80,   0);
    private static final Color C_RED       = new Color(180,  30,  30);

    // ── Column layout for the course table ────────────────────────────────────
    // [Code, Course Name, Cr, Marks, %, Grade, GP]
    private static final double[] COL_FRACTIONS = { 0.11, 0.36, 0.07, 0.14, 0.10, 0.10, 0.12 };
    private static final String[] COL_LABELS    = { "Code", "Course Name", "Cr", "Marks", "%", "Grade", "GP" };
    private static final int Graphics2D_ALIGN_LEFT   = 0;
    private static final int Graphics2D_ALIGN_CENTER = 1;
    private static final int Graphics2D_ALIGN_RIGHT  = 2;
    private static final int[]    COL_ALIGN     = {
        Graphics2D_ALIGN_LEFT, Graphics2D_ALIGN_LEFT,
        Graphics2D_ALIGN_CENTER, Graphics2D_ALIGN_CENTER,
        Graphics2D_ALIGN_CENTER, Graphics2D_ALIGN_CENTER,
        Graphics2D_ALIGN_CENTER
    };

    private static final double ROW_H        = 13.0;   // table data row height
    private static final double HEAD_ROW_H   = 14.0;   // table header row height
    private static final double SEM_HEAD_H   = 20.0;   // semester heading height
    private static final double GPA_ROW_H    = 14.0;   // GPA footer row height
    private static final double SEM_GAP      = 10.0;   // gap between semester blocks

    // ── Rendering state (built during first pass) ─────────────────────────────
    private final StudentSearchResult student;
    private final TranscriptData      data;
    private final String              issuedDate;

    /** Pre-computed list of rendering commands, grouped by page. */
    private List<List<DrawCmd>> pages;
    private int totalPages = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    public TranscriptPrintable(StudentSearchResult student, TranscriptData data) {
        this.student   = student;
        this.data      = data;
        this.issuedDate = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        buildPageLayout();
    }

    // ── PageFormat helper ────────────────────────────────────────────────────

    /** Returns an A4 portrait PageFormat for use with PrinterJob. */
    public PageFormat getA4PageFormat(PrinterJob job) {
        PageFormat pf = job.defaultPage();
        Paper paper   = new Paper();
        paper.setSize(A4_W_PT, A4_H_PT);
        paper.setImageableArea(MARGIN_X, MARGIN_T, CONTENT_W, CONTENT_H);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

    // ── Printable.print ───────────────────────────────────────────────────────

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
            throws PrinterException {
        if (pageIndex >= totalPages) return NO_SUCH_PAGE;

        Graphics2D g2 = (Graphics2D) graphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Translate to imageable area origin
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        // Draw all commands for this page
        for (DrawCmd cmd : pages.get(pageIndex)) {
            cmd.draw(g2);
        }

        return PAGE_EXISTS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LAYOUT ENGINE
    //  Simulates rendering without a Graphics context to compute page breaks.
    //  Produces a list-of-pages where each page is a list of DrawCmd lambdas.
    // ─────────────────────────────────────────────────────────────────────────

    private void buildPageLayout() {
        pages = new ArrayList<>();

        // Group transcript data
        Map<String, List<TranscriptCourseRow>> grouped = new LinkedHashMap<>();
        for (TranscriptCourseRow r : data.courseRows()) {
            grouped.computeIfAbsent(r.getSemesterName(), k -> new ArrayList<>()).add(r);
        }
        Map<String, TranscriptSemesterRow> semIndex = new LinkedHashMap<>();
        for (TranscriptSemesterRow r : data.semesterRows()) semIndex.put(r.getSemesterName(), r);

        // ── Page builder state ────────────────────────────────────────────────
        List<DrawCmd> curPage = new ArrayList<>();
        pages.add(curPage);

        double y = 0;  // current Y within content area

        // ── Letterhead ────────────────────────────────────────────────────────
        y = addLetterhead(curPage, y);
        y = addStudentInfoBlock(curPage, y);

        // ── Divider before semesters ──────────────────────────────────────────
        double fy = y;
        curPage.add(g2 -> { g2.setColor(C_RULE); g2.drawLine(0,(int)fy,(int)CONTENT_W,(int)fy); });
        y += 10;

        // ── Semester blocks ────────────────────────────────────────────────────
        for (Map.Entry<String, List<TranscriptCourseRow>> entry : grouped.entrySet()) {
            String semName    = entry.getKey();
            List<TranscriptCourseRow> courses = entry.getValue();
            TranscriptSemesterRow semRow = semIndex.get(semName);

            // Height needed for this entire semester block
            double blockH = SEM_HEAD_H + HEAD_ROW_H
                          + courses.size() * ROW_H
                          + GPA_ROW_H + SEM_GAP;

            // Page break if not enough room (keep at least sem header + 2 rows together)
            double minKeepH = SEM_HEAD_H + HEAD_ROW_H + 2 * ROW_H;
            if (y + minKeepH > CONTENT_H - 40) {
                addPageFooter(curPage, pages.size(), -1);   // total not known yet
                curPage = new ArrayList<>();
                pages.add(curPage);
                y = 0;
                addPageHeader(curPage);
                y += 26;
            }

            y = addSemesterBlock(curPage, y, semName, courses, semRow);
        }

        // ── CGPA summary + signature (last page) ─────────────────────────────
        double cgpaSectionH = 90;
        if (y + cgpaSectionH > CONTENT_H - 40) {
            addPageFooter(curPage, pages.size(), -1);
            curPage = new ArrayList<>();
            pages.add(curPage);
            y = 0;
            addPageHeader(curPage);
            y += 26;
        }
        y = addCgpaSummary(curPage, y);
        y = addSignatureBlock(curPage, y + 12);

        // ── Now that totalPages is known, patch page footers ──────────────────
        totalPages = pages.size();
        for (int i = 0; i < totalPages; i++) {
            final int pg = i + 1;
            final int tot = totalPages;
            pages.get(i).add(g2 -> drawPageFooter(g2, pg, tot));
        }
    }

    // ── HEADER (continuation pages) ───────────────────────────────────────────
    private void addPageHeader(List<DrawCmd> page) {
        page.add(g2 -> {
            g2.setFont(F_HEADING);
            g2.setColor(C_NAVY);
            g2.drawString("UniCore University Management System  ·  Official Academic Transcript", 0, 10);
            g2.setColor(C_RULE);
            g2.drawLine(0, 14, (int)CONTENT_W, 14);
        });
    }

    // ── LETTERHEAD (first page only) ─────────────────────────────────────────
    private double addLetterhead(List<DrawCmd> page, double startY) {
        page.add(g2 -> {
            // Gold circle "U" logo
            g2.setColor(C_GOLD);
            g2.fillOval(0, 0, 36, 36);
            g2.setColor(C_NAVY);
            g2.setFont(new Font("Georgia", Font.BOLD, 18));
            g2.drawString("U", 10, 25);

            // Institution name
            g2.setFont(F_INSTITUTION);
            g2.setColor(C_NAVY);
            g2.drawString("UNICORE", 44, 14);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
            g2.setColor(C_DEEP_BLUE);
            g2.drawString("University Management System", 44, 26);

            // Issue date (right-aligned)
            g2.setFont(F_VALUE);
            g2.setColor(Color.GRAY);
            String dateStr = "Issue Date: " + LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(dateStr, (int)(CONTENT_W - fm.stringWidth(dateStr)), 20);

            // Full-width gold underline
            g2.setColor(C_GOLD);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(0, 42, (int)CONTENT_W, 42);

            // "OFFICIAL ACADEMIC TRANSCRIPT" centred title
            g2.setFont(F_TITLE);
            g2.setColor(C_NAVY);
            FontMetrics fmT = g2.getFontMetrics();
            String titleStr = "OFFICIAL ACADEMIC TRANSCRIPT";
            g2.drawString(titleStr, (int)((CONTENT_W - fmT.stringWidth(titleStr)) / 2), 58);

            // Rule under title
            g2.setColor(C_RULE);
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawLine(0, 62, (int)CONTENT_W, 62);
        });
        return 70;
    }

    // ── STUDENT INFO BLOCK ────────────────────────────────────────────────────
    private double addStudentInfoBlock(List<DrawCmd> page, double startY) {
        final double sy = startY;
        page.add(g2 -> {
            double col1x = 0;
            double col2x = CONTENT_W / 2 + 10;
            double y = sy;

            // Left column
            drawLabelValue(g2, col1x, y,     "Student Name:", student.getFullName());
            drawLabelValue(g2, col1x, y + 14, "Roll Number:",  student.getRollNumber());
            drawLabelValue(g2, col1x, y + 28, "Department:",   student.getSubDeptName());
            drawLabelValue(g2, col1x, y + 42, "Programme:",    student.getMajorDeptName());

            // Right column
            drawLabelValue(g2, col2x, y,     "Username:",      student.getUsername());
            drawLabelValue(g2, col2x, y + 14, "Batch Year:",   String.valueOf(student.getBatchYear()));
            drawLabelValue(g2, col2x, y + 28, "Semester:",     String.valueOf(student.getCurrentSemester()));
            drawLabelValue(g2, col2x, y + 42, "Status:",
                student.isActive() ? "Active" : "Inactive");
        });
        return startY + 62;
    }

    private void drawLabelValue(Graphics2D g2, double x, double y,
                                 String label, String value) {
        g2.setFont(F_LABEL);
        g2.setColor(Color.GRAY);
        g2.drawString(label, (int)x, (int)y);
        g2.setFont(F_VALUE);
        g2.setColor(C_NAVY);
        FontMetrics fm = g2.getFontMetrics(F_LABEL);
        g2.drawString(value, (int)(x + fm.stringWidth(label) + 4), (int)y);
    }

    // ── SEMESTER BLOCK ────────────────────────────────────────────────────────
    private double addSemesterBlock(List<DrawCmd> page, double startY,
                                     String semName,
                                     List<TranscriptCourseRow> courses,
                                     TranscriptSemesterRow semRow) {
        final double sy = startY;

        page.add(g2 -> {
            double y = sy;

            // Semester heading bar
            g2.setColor(new Color(230, 240, 255));
            g2.fillRect(0, (int)y, (int)CONTENT_W, (int)SEM_HEAD_H);
            g2.setFont(F_SEM_HEAD);
            g2.setColor(C_NAVY);
            g2.drawString(semName, 6, (int)(y + SEM_HEAD_H - 5));
            if (semRow != null) {
                String meta = semRow.getCourseCount() + " courses  ·  "
                    + semRow.getSemCreditHours() + " credit hours";
                g2.setFont(F_VALUE);
                g2.setColor(Color.GRAY);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(meta, (int)(CONTENT_W - fm.stringWidth(meta) - 4),
                    (int)(y + SEM_HEAD_H - 5));
            }
            y += SEM_HEAD_H;

            // Column header row
            y = drawTableHeaderRow(g2, y);

            // Data rows
            boolean alt = false;
            for (TranscriptCourseRow row : courses) {
                y = drawTableDataRow(g2, y, row, alt);
                alt = !alt;
            }

            // GPA footer
            if (semRow != null) {
                drawGpaFooter(g2, y, semRow);
            }
        });

        return startY + SEM_HEAD_H + HEAD_ROW_H
               + courses.size() * ROW_H + GPA_ROW_H + SEM_GAP;
    }

    private double drawTableHeaderRow(Graphics2D g2, double y) {
        g2.setColor(C_TBL_HEAD);
        g2.fillRect(0, (int)y, (int)CONTENT_W, (int)HEAD_ROW_H);

        double x = 0;
        for (int i = 0; i < COL_LABELS.length; i++) {
            double colW = CONTENT_W * COL_FRACTIONS[i];
            g2.setFont(F_TABLE_HEAD);
            g2.setColor(C_DEEP_BLUE);
            drawAligned(g2, COL_LABELS[i], x, y + HEAD_ROW_H - 4, colW, COL_ALIGN[i]);
            x += colW;
        }

        // Bottom border
        g2.setColor(C_RULE);
        g2.drawLine(0, (int)(y + HEAD_ROW_H), (int)CONTENT_W, (int)(y + HEAD_ROW_H));
        return y + HEAD_ROW_H;
    }

    private double drawTableDataRow(Graphics2D g2, double y,
                                     TranscriptCourseRow row, boolean alt) {
        if (alt) {
            g2.setColor(C_TBL_ALT);
            g2.fillRect(0, (int)y, (int)CONTENT_W, (int)ROW_H);
        }

        String[] cells = {
            row.getCourseCode(),
            row.getCourseName(),
            String.valueOf(row.getCreditHours()),
            String.format("%.0f/%.0f", row.getMarksObtained(), row.getMarksTotal()),
            row.getPercentageStr(),
            row.getLetterGrade(),
            String.format("%.2f", row.getGradePoints())
        };

        double x = 0;
        for (int i = 0; i < cells.length; i++) {
            double colW = CONTENT_W * COL_FRACTIONS[i];

            // Grade column gets colour
            if (i == 5) {
                g2.setFont(F_TABLE_BODY);
                g2.setColor(gradeColor(row.getLetterGrade()));
            } else {
                g2.setFont(F_TABLE_BODY);
                g2.setColor(C_NAVY);
            }
            drawAligned(g2, cells[i], x, y + ROW_H - 3, colW, COL_ALIGN[i]);
            x += colW;
        }

        // Light row separator
        g2.setColor(new Color(220, 228, 240));
        g2.drawLine(0, (int)(y + ROW_H), (int)CONTENT_W, (int)(y + ROW_H));
        return y + ROW_H;
    }

    private void drawGpaFooter(Graphics2D g2, double y, TranscriptSemesterRow semRow) {
        g2.setColor(new Color(210, 228, 250));
        g2.fillRect(0, (int)y, (int)CONTENT_W, (int)GPA_ROW_H);

        // Right-aligned GPA stats
        String stats = String.format("Sem GPA: %s   |   CGPA: %s   |   Credits: %d",
            semRow.getSemesterGpaStr(), semRow.getCgpaSoFarStr(), semRow.getSemCreditHours());
        g2.setFont(F_GPA_LABEL);
        g2.setColor(C_DEEP_BLUE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(stats, (int)(CONTENT_W - fm.stringWidth(stats) - 6),
            (int)(y + GPA_ROW_H - 3));
    }

    // ── CGPA SUMMARY ─────────────────────────────────────────────────────────
    private double addCgpaSummary(List<DrawCmd> page, double startY) {
        final double sy = startY + 6;
        page.add(g2 -> {
            // Box
            g2.setColor(new Color(225, 240, 255));
            g2.fillRoundRect(0, (int)sy, (int)CONTENT_W, 56, 10, 10);
            g2.setColor(C_MID_BLUE);
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawRoundRect(0, (int)sy, (int)CONTENT_W, 56, 10, 10);

            // Left side labels
            g2.setFont(F_HEADING);
            g2.setColor(C_NAVY);
            g2.drawString("CUMULATIVE ACADEMIC SUMMARY", 12, (int)(sy + 16));

            g2.setFont(F_VALUE);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Total Courses Completed: " + data.getTotalCourses(),
                12, (int)(sy + 30));
            g2.drawString("Total Credit Hours Earned: " + data.getTotalCreditHours(),
                12, (int)(sy + 43));

            // Right side: big CGPA
            double cgpa = data.getOverallCgpa();
            g2.setFont(F_CGPA_BIG);
            g2.setColor(gpaColor(cgpa));
            String cgpaStr = String.format("%.2f", cgpa);
            FontMetrics fmBig = g2.getFontMetrics();
            g2.drawString(cgpaStr, (int)(CONTENT_W - fmBig.stringWidth(cgpaStr) - 50),
                (int)(sy + 36));

            g2.setFont(new Font("SansSerif", Font.PLAIN, 8));
            g2.setColor(Color.GRAY);
            String standingStr = "CGPA  ·  " + standing(cgpa);
            FontMetrics fmS = g2.getFontMetrics();
            g2.drawString(standingStr,
                (int)(CONTENT_W - fmS.stringWidth(standingStr) - 6),
                (int)(sy + 50));
        });
        return startY + 6 + 56;
    }

    // ── SIGNATURE BLOCK ───────────────────────────────────────────────────────
    private double addSignatureBlock(List<DrawCmd> page, double startY) {
        final double sy = startY;
        page.add(g2 -> {
            g2.setColor(C_RULE);
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawLine(0, (int)sy, (int)CONTENT_W, (int)sy);

            double col1 = 0;
            double col2 = CONTENT_W / 2 + 10;
            double sigLineY = sy + 36;

            // Left signature line
            g2.setColor(C_NAVY);
            g2.drawLine((int)col1, (int)sigLineY, (int)(col1 + 140), (int)sigLineY);
            g2.setFont(F_LABEL);
            g2.setColor(Color.GRAY);
            g2.drawString("Registrar's Signature", (int)col1, (int)(sigLineY + 10));

            // Right signature line
            g2.drawLine((int)col2, (int)sigLineY, (int)(col2 + 140), (int)sigLineY);
            g2.drawString("Controller of Examinations", (int)col2, (int)(sigLineY + 10));

            // Official stamp placeholder
            g2.setColor(new Color(200, 210, 230));
            g2.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 1f, new float[]{3, 3}, 0));
            g2.drawOval((int)(CONTENT_W - 60), (int)(sy + 8), 54, 54);
            g2.setFont(F_LABEL);
            g2.setColor(new Color(180, 190, 210));
            g2.drawString("Official", (int)(CONTENT_W - 48), (int)(sy + 34));
            g2.drawString("Stamp",    (int)(CONTENT_W - 42), (int)(sy + 45));

            // Disclaimer
            g2.setFont(F_FOOTER);
            g2.setColor(Color.GRAY);
            String disc = "This transcript is issued by UniCore University Management System. "
                        + "Verify authenticity at transcripts.unicore.edu.pk";
            g2.drawString(disc, 0, (int)(sy + 72));
        });
        return startY + 80;
    }

    // ── PAGE FOOTER ───────────────────────────────────────────────────────────
    private void addPageFooter(List<DrawCmd> page, int pageNum, int totalPgs) {
        // placeholder — real footer is added after totalPages is known
    }

    private void drawPageFooter(Graphics2D g2, int pageNum, int totalPages) {
        double footerY = CONTENT_H + 10;
        g2.setColor(C_RULE);
        g2.setStroke(new BasicStroke(0.5f));
        g2.drawLine(0, (int)footerY, (int)CONTENT_W, (int)footerY);

        g2.setFont(F_FOOTER);
        g2.setColor(Color.GRAY);
        g2.drawString("UniCore UMS  ·  Official Academic Transcript  ·  "
            + student.getFullName() + "  ·  " + student.getRollNumber(),
            0, (int)(footerY + 12));

        String pgStr = "Page " + pageNum + " of " + totalPages;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(pgStr, (int)(CONTENT_W - fm.stringWidth(pgStr)), (int)(footerY + 12));
    }

    // ── DRAW HELPERS ─────────────────────────────────────────────────────────

    private void drawAligned(Graphics2D g2, String text, double cellX,
                              double baselineY, double cellW, int align) {
        if (text == null) return;
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        double tx;
        switch (align) {
            case Graphics2D_ALIGN_CENTER -> tx = cellX + (cellW - tw) / 2.0;
            case Graphics2D_ALIGN_RIGHT  -> tx = cellX + cellW - tw - 2;
            default                      -> tx = cellX + 2;
        }
        // Clip to cell width
        Shape oldClip = g2.getClip();
        g2.setClip((int)cellX, (int)(baselineY - fm.getAscent()),
                   (int)cellW, fm.getHeight() + 2);
        g2.drawString(text, (int)tx, (int)baselineY);
        g2.setClip(oldClip);
    }

    private Color gradeColor(String grade) {
        if (grade == null) return Color.GRAY;
        return switch (grade) {
            case "A+","A","A-" -> C_GREEN;
            case "B+","B","B-" -> C_MID_BLUE;
            case "C+","C","C-" -> C_AMBER;
            case "D"           -> new Color(140, 70, 0);
            default            -> C_RED;
        };
    }

    private Color gpaColor(double gpa) {
        if (gpa >= 3.5) return C_GREEN;
        if (gpa >= 2.5) return C_MID_BLUE;
        if (gpa >= 1.5) return C_AMBER;
        return C_RED;
    }

    private String standing(double cgpa) {
        if (cgpa >= 3.7) return "Distinction";
        if (cgpa >= 3.3) return "High Merit";
        if (cgpa >= 3.0) return "Merit";
        if (cgpa >= 2.0) return "Satisfactory";
        if (cgpa >= 1.0) return "Probation";
        return "Academic Warning";
    }

    // ── DrawCmd functional interface ─────────────────────────────────────────

    @FunctionalInterface
    private interface DrawCmd {
        void draw(Graphics2D g2);
    }
}