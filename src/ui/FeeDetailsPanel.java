package ui;

import bl.FeeService;
import model.FeeRecord;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FeeDetailsPanel — Student Fee Details UI (read-only).
 *
 * Layout:
 *   Top row    : summary tiles (total billed, total paid, outstanding balance)
 *   Below      : one card per semester showing full breakdown + challan status
 *
 * Integration in StudentDashboard onMenuClick:
 *   case "Fee Details" -> contentArea.add(new FeeDetailsPanel(username), BorderLayout.CENTER);
 */
public class FeeDetailsPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final Color GREEN  = new Color(21, 128, 61);
    private static final Color AMBER  = new Color(180, 100, 0);
    private static final Color RED_FG = new Color(198, 40, 40);
    private static final Color GREY   = new Color(100, 120, 150);

    private final String     username;
    private final FeeService service;

    // UI refs for summary tiles
    private JLabel totalBilledLabel;
    private JLabel totalPaidLabel;
    private JLabel totalOutstandingLabel;

    private JPanel cardsPanel;

    public FeeDetailsPanel(String username) {
        this.username = username;
        this.service  = new FeeService();
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
        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Title block
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Fee Details");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Complete fee history across all semesters");
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

        // Summary tiles row
        JPanel summaryRow = new JPanel(new GridLayout(1, 3, 14, 0));
        summaryRow.setOpaque(false);

        totalBilledLabel      = summaryValueLabel("—");
        totalPaidLabel        = summaryValueLabel("—");
        totalOutstandingLabel = summaryValueLabel("—");

        summaryRow.add(buildSummaryTile("Total Billed",      totalBilledLabel,      "#1565C0"));
        summaryRow.add(buildSummaryTile("Total Paid",        totalPaidLabel,        "#2E7D32"));
        summaryRow.add(buildSummaryTile("Outstanding",       totalOutstandingLabel, "#C62828"));

        header.add(titleBlock,  BorderLayout.WEST);
        header.add(summaryRow,  BorderLayout.EAST);
        return header;
    }

    private JPanel buildSummaryTile(String label, JLabel valueLabel, String hexColor) {
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Color.decode(hexColor));
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(12, getHeight() - 4, getWidth() - 12, getHeight() - 4);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        tile.setPreferredSize(new Dimension(160, 72));

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.headingFont(10));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        tile.add(lbl);
        tile.add(Box.createVerticalStrut(4));
        tile.add(valueLabel);
        return tile;
    }

    private JLabel summaryValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.titleFont(16));
        lbl.setForeground(AppTheme.NAVY);
        return lbl;
    }

    // ── Scrollable cards area ─────────────────────────────────────────────────
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

    // ── Semester card ─────────────────────────────────────────────────────────
    private JPanel buildSemesterCard(FeeRecord r) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Left accent — colour by status
                Color accent = r.getStatus().equals("paid")    ? GREEN
                             : r.getStatus().equals("partial") ? AMBER : RED_FG;
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // ── Top row: semester name + status badge ─────────────────────────────
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel semName = new JLabel(r.getSemesterName());
        semName.setFont(AppTheme.titleFont(16));
        semName.setForeground(AppTheme.NAVY);

        topRow.add(semName,           BorderLayout.WEST);
        topRow.add(buildStatusBadge(r.getStatus()), BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // ── Middle: fee breakdown grid ────────────────────────────────────────
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);

        grid.add(buildFeeTile("Total Fee",
            "PKR " + String.format("%,.0f", r.getTotalAmount()), AppTheme.NAVY));
        grid.add(buildFeeTile("Paid",
            "PKR " + String.format("%,.0f", r.getPaidAmount()), GREEN));
        grid.add(buildFeeTile("Outstanding",
            "PKR " + String.format("%,.0f", r.getOutstanding()),
            r.getOutstanding() > 0 ? RED_FG : GREEN));
        grid.add(buildFeeTile("Due Date",
            r.getDueDate() != null ? r.getDueDate().format(FMT) : "—",
            AppTheme.TEXT_DARK));

        card.add(grid, BorderLayout.CENTER);

        // ── Bottom: challan info ──────────────────────────────────────────────
        card.add(buildChallanInfoRow(r), BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildFeeTile(String label, String value, Color valueColor) {
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

        JLabel val = new JLabel(value);
        val.setFont(AppTheme.headingFont(14));
        val.setForeground(valueColor);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        tile.add(lbl);
        tile.add(Box.createVerticalStrut(4));
        tile.add(val);
        return tile;
    }

    private JPanel buildChallanInfoRow(FeeRecord r) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        if (!r.hasChallan()) {
            JLabel none = new JLabel("📋  No challan generated for this semester");
            none.setFont(AppTheme.bodyFont(12));
            none.setForeground(GREY);
            row.add(none);
        } else {
            row.add(challanInfoChip("📋  " + r.getChallanNumber(),
                AppTheme.PALE_BLUE, AppTheme.DEEP_BLUE));
            if (r.getGeneratedDate() != null)
                row.add(challanInfoChip(
                    "Generated: " + r.getGeneratedDate().format(FMT),
                    AppTheme.PALE_BLUE, AppTheme.TEXT_MUTED));
            if (r.getExpiryDate() != null) {
                boolean expired = r.getExpiryDate().isBefore(java.time.LocalDate.now());
                row.add(challanInfoChip(
                    (expired ? "⚠  Expired: " : "Expires: ")
                        + r.getExpiryDate().format(FMT),
                    expired ? new Color(254, 226, 226) : AppTheme.PALE_BLUE,
                    expired ? RED_FG : AppTheme.TEXT_MUTED));
            }
            row.add(challanInfoChip(
                r.isChallanPaid() ? "✅  Paid" : "🕐  Unpaid",
                r.isChallanPaid() ? new Color(220, 252, 231) : new Color(255, 243, 205),
                r.isChallanPaid() ? GREEN : AMBER));
        }
        return row;
    }

    private JLabel challanInfoChip(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        lbl.setFont(AppTheme.bodyFont(11));
        lbl.setForeground(fg);
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        lbl.setOpaque(false);
        return lbl;
    }

    private JLabel buildStatusBadge(String status) {
        String display = status.substring(0,1).toUpperCase() + status.substring(1);
        JLabel lbl = new JLabel(display) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = status.equals("paid")    ? new Color(220, 252, 231)
                         : status.equals("partial") ? new Color(255, 243, 205)
                         : new Color(254, 226, 226);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(status.equals("paid") ? GREEN
            : status.equals("partial") ? AMBER : RED_FG);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        lbl.setOpaque(false);
        return lbl;
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadData() {
        new SwingWorker<List<FeeRecord>, Void>() {
            @Override protected List<FeeRecord> doInBackground() {
                if (!service.init(username)) return List.of();
                return service.getFeeRecords();
            }
            @Override protected void done() {
                try {
                    List<FeeRecord> records = get();
                    cardsPanel.removeAll();

                    if (records.isEmpty()) {
                        JLabel empty = new JLabel("No fee records found.");
                        empty.setFont(AppTheme.bodyFont(13));
                        empty.setForeground(AppTheme.TEXT_MUTED);
                        empty.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
                        cardsPanel.add(empty);
                    } else {
                        // Update summary tiles
                        double billed      = records.stream().mapToDouble(FeeRecord::getTotalAmount).sum();
                        double paid        = records.stream().mapToDouble(FeeRecord::getPaidAmount).sum();
                        double outstanding = records.stream().mapToDouble(FeeRecord::getOutstanding).sum();

                        totalBilledLabel.setText("PKR " + String.format("%,.0f", billed));
                        totalPaidLabel.setText("PKR " + String.format("%,.0f", paid));
                        totalOutstandingLabel.setText("PKR " + String.format("%,.0f", outstanding));
                        totalOutstandingLabel.setForeground(outstanding > 0 ? RED_FG : GREEN);

                        // Build semester cards
                        for (FeeRecord r : records) {
                            cardsPanel.add(buildSemesterCard(r));
                            cardsPanel.add(Box.createVerticalStrut(14));
                        }
                    }
                    cardsPanel.revalidate();
                    cardsPanel.repaint();
                } catch (Exception ex) {
                    JLabel err = new JLabel("Error loading fee details: " + ex.getMessage());
                    err.setFont(AppTheme.bodyFont(12));
                    err.setForeground(RED_FG);
                    cardsPanel.add(err);
                    cardsPanel.revalidate();
                }
            }
        }.execute();
    }
}