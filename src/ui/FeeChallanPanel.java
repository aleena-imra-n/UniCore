package ui;

import bl.FeeService;
import bl.FeeService.ChallanResult;
import bl.FeeService.Outcome;
import model.FeeRecord;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FeeChallanPanel — Student Fee Challan UI.
 *
 * Layout:
 *   Top card   : current semester fee summary + Generate Challan button
 *   Middle     : generated challan reference card (shown after generation)
 *   Bottom     : scrollable list of all previous fee records + challan history
 *
 * Integration in StudentDashboard onMenuClick:
 *   case "Fee Challan" -> contentArea.add(new FeeChallanPanel(username), BorderLayout.CENTER);
 */
public class FeeChallanPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final Color GREEN   = new Color(21, 128, 61);
    private static final Color AMBER   = new Color(180, 100, 0);
    private static final Color RED_FG  = new Color(198, 40, 40);
    private static final Color GREY    = new Color(100, 120, 150);

    private final String     username;
    private final FeeService service;

    // UI refs
    private JLabel       currentAmountLabel;
    private JLabel       currentPaidLabel;
    private JLabel       currentOutstandingLabel;
    private JLabel       currentDueDateLabel;
    private JLabel       currentStatusLabel;
    private StyledButton generateBtn;
    private JLabel       statusLabel;
    private JPanel       challanCard;
    private JPanel       historyPanel;

    public FeeChallanPanel(String username) {
        this.username = username;
        this.service  = new FeeService();
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildHeader(),   BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);

        loadData();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Fee Challan");
        title.setFont(AppTheme.titleFont(24));
        title.setForeground(AppTheme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("View your fee summary and generate a payment challan");
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

    // ── Main area ─────────────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel area = new JPanel();
        area.setLayout(new BoxLayout(area, BoxLayout.Y_AXIS));
        area.setOpaque(false);

        area.add(buildCurrentSemesterCard());
        area.add(Box.createVerticalStrut(14));

        // Challan result card — hidden until generated
        challanCard = new JPanel(new BorderLayout());
        challanCard.setOpaque(false);
        challanCard.setVisible(false);
        challanCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        challanCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        area.add(challanCard);
        area.add(Box.createVerticalStrut(14));

        area.add(buildHistoryCard());
        return area;
    }

    // ── Current semester card ─────────────────────────────────────────────────
    private JPanel buildCurrentSemesterCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left: fee breakdown
        JPanel left = new JPanel(new GridLayout(2, 3, 20, 8));
        left.setOpaque(false);

        currentAmountLabel      = infoTileLabel("—");
        currentPaidLabel        = infoTileLabel("—");
        currentOutstandingLabel = infoTileLabel("—");
        currentDueDateLabel     = infoTileLabel("—");
        currentStatusLabel      = infoTileLabel("—");

        left.add(labeledTile("Total Fee",    currentAmountLabel));
        left.add(labeledTile("Paid",         currentPaidLabel));
        left.add(labeledTile("Outstanding",  currentOutstandingLabel));
        left.add(labeledTile("Due Date",     currentDueDateLabel));
        left.add(labeledTile("Status",       currentStatusLabel));
        left.add(new JPanel() {{ setOpaque(false); }}); // spacer

        card.add(left, BorderLayout.CENTER);

        // Right: generate button + status
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));

        generateBtn = new StyledButton("Generate Challan",
            AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        generateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateBtn.setMaximumSize(new Dimension(180, 44));
        generateBtn.addActionListener(e -> onGenerateClicked());

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.bodyFont(11));
        statusLabel.setForeground(AppTheme.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        right.add(Box.createVerticalGlue());
        right.add(generateBtn);
        right.add(Box.createVerticalStrut(8));
        right.add(statusLabel);
        right.add(Box.createVerticalGlue());

        card.add(right, BorderLayout.EAST);
        return card;
    }

    // ── Challan reference card (shown after generation) ───────────────────────
    private JPanel buildChallanReferenceCard(String challanNumber,
                                              String expiryDate,
                                              double totalAmount) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Navy gradient background
                GradientPaint gp = new GradientPaint(
                    0, 0, AppTheme.NAVY, getWidth(), 0, AppTheme.DEEP_BLUE);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Gold top stripe
                g2.setColor(AppTheme.GOLD);
                g2.fillRoundRect(0, 0, getWidth(), 5, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridLayout(1, 3, 20, 0));
        card.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(challanTile("📋  Challan No.",   challanNumber,   Color.WHITE));
        card.add(challanTile("📅  Expiry Date",   expiryDate,      AppTheme.LIGHT_BLUE));
        card.add(challanTile("💰  Amount Due",
            "PKR " + String.format("%,.2f", totalAmount), AppTheme.GOLD));

        return card;
    }

    private JPanel challanTile(String label, String value, Color valueColor) {
        JPanel tile = new JPanel();
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.bodyFont(11));
        lbl.setForeground(new Color(160, 190, 230));
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

    // ── History card ──────────────────────────────────────────────────────────
    private JPanel buildHistoryCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Fee History");
        title.setFont(AppTheme.headingFont(15));
        title.setForeground(AppTheme.NAVY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        card.add(title, BorderLayout.NORTH);

        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setOpaque(false);

        JLabel loading = new JLabel("Loading fee history…");
        loading.setFont(AppTheme.bodyFont(12));
        loading.setForeground(AppTheme.TEXT_MUTED);
        historyPanel.add(loading);

        card.setMinimumSize(new Dimension(0, 280));
        JScrollPane scroll = new JScrollPane(historyPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildHistoryRow(FeeRecord r) {
        JPanel row = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Status stripe
                Color stripe = r.getStatus().equals("paid") ? GREEN
                             : r.getStatus().equals("partial") ? AMBER : RED_FG;
                g2.setColor(stripe);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setLayout(new GridLayout(1, 5, 10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(rowLabel(r.getSemesterName(), AppTheme.NAVY, true));
        row.add(rowLabel("PKR " + String.format("%,.0f", r.getTotalAmount()),
            AppTheme.TEXT_DARK, false));
        row.add(rowLabel("PKR " + String.format("%,.0f", r.getOutstanding()),
            r.getOutstanding() > 0 ? RED_FG : GREEN, false));
        row.add(rowLabel(r.hasChallan() ? r.getChallanNumber() : "No challan",
            r.hasChallan() ? AppTheme.DEEP_BLUE : GREY, false));
        row.add(statusBadge(r.getStatus()));

        return row;
    }

    // ── Event handler ─────────────────────────────────────────────────────────
    private void onGenerateClicked() {
        generateBtn.setEnabled(false);
        showStatus("Generating challan…", AppTheme.TEXT_MUTED);

        new SwingWorker<ChallanResult, Void>() {
            @Override protected ChallanResult doInBackground() {
                return service.generateChallan();
            }
            @Override protected void done() {
                generateBtn.setEnabled(true);
                try {
                    ChallanResult result = get();
                    if (result.outcome() == Outcome.SUCCESS) {
                        showStatus("✅  " + result.message(), GREEN);
                        // Show challan reference card
                        List<FeeRecord> records = service.getFeeRecords();
                        double total = records.stream()
                            .filter(r -> r.getSemesterId() == service.getSemesterId())
                            .mapToDouble(FeeRecord::getTotalAmount)
                            .findFirst().orElse(0.0);
                        challanCard.removeAll();
                        challanCard.add(buildChallanReferenceCard(
                            result.challanNumber(), result.expiryDate(), total),
                            BorderLayout.CENTER);
                        challanCard.setVisible(true);
                        challanCard.revalidate();
                        challanCard.repaint();
                        // Reload history
                        populateHistory(records);
                    } else {
                        showStatus("⚠  " + result.message(), AMBER);
                    }
                } catch (Exception ex) {
                    showStatus("✖  Error: " + ex.getMessage(), RED_FG);
                }
            }
        }.execute();
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
                    // Populate current semester tile
                    records.stream()
                        .filter(r -> r.getSemesterId() == service.getSemesterId())
                        .findFirst()
                        .ifPresentOrElse(r -> {
                            currentAmountLabel.setText("PKR " +
                                String.format("%,.0f", r.getTotalAmount()));
                            currentPaidLabel.setText("PKR " +
                                String.format("%,.0f", r.getPaidAmount()));
                            currentOutstandingLabel.setText("PKR " +
                                String.format("%,.0f", r.getOutstanding()));
                            currentDueDateLabel.setText(
                                r.getDueDate() != null
                                ? r.getDueDate().format(FMT) : "—");
                            currentStatusLabel.setText(
                                r.getStatus().substring(0,1).toUpperCase()
                                + r.getStatus().substring(1));
                            currentStatusLabel.setForeground(
                                r.getStatus().equals("paid") ? GREEN
                                : r.getStatus().equals("partial") ? AMBER : RED_FG);
                            // Show existing challan card if one already exists
                            if (r.hasChallan()) {
                                challanCard.removeAll();
                                challanCard.add(buildChallanReferenceCard(
                                    r.getChallanNumber(),
                                    r.getExpiryDate() != null
                                        ? r.getExpiryDate().format(FMT) : "—",
                                    r.getTotalAmount()), BorderLayout.CENTER);
                                challanCard.setVisible(true);
                                challanCard.revalidate();
                            }
                        }, () -> {
                            showStatus("No fee record found for current semester.",
                                AppTheme.TEXT_MUTED);
                            generateBtn.setEnabled(false);
                        });
                    populateHistory(records);
                } catch (Exception ex) {
                    showStatus("Error loading fee data: " + ex.getMessage(), RED_FG);
                }
            }
        }.execute();
    }

    private void populateHistory(List<FeeRecord> records) {
        historyPanel.removeAll();
        if (records.isEmpty()) {
            JLabel empty = new JLabel("No fee records found.");
            empty.setFont(AppTheme.bodyFont(12));
            empty.setForeground(AppTheme.TEXT_MUTED);
            historyPanel.add(empty);
        } else {
            // Column headers
            JPanel headers = new JPanel(new GridLayout(1, 5, 10, 0));
            headers.setOpaque(false);
            headers.setBorder(BorderFactory.createEmptyBorder(0, 16, 6, 12));
            headers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            headers.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (String h : new String[]{"Semester","Total Fee","Outstanding","Challan No.","Status"}) {
                JLabel lbl = new JLabel(h);
                lbl.setFont(AppTheme.headingFont(10));
                lbl.setForeground(AppTheme.TEXT_MUTED);
                headers.add(lbl);
            }
            historyPanel.add(headers);
            historyPanel.add(Box.createVerticalStrut(4));
            for (FeeRecord r : records) {
                historyPanel.add(buildHistoryRow(r));
                historyPanel.add(Box.createVerticalStrut(6));
            }
        }
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    // ── Widget helpers ────────────────────────────────────────────────────────
    private JPanel labeledTile(String label, JLabel valueLabel) {
        JPanel tile = new JPanel();
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.headingFont(10));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(lbl);
        tile.add(Box.createVerticalStrut(3));
        tile.add(valueLabel);
        return tile;
    }

    private JLabel infoTileLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.headingFont(14));
        lbl.setForeground(AppTheme.NAVY);
        return lbl;
    }

    private JLabel rowLabel(String text, Color color, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(bold ? AppTheme.headingFont(12) : AppTheme.bodyFont(12));
        lbl.setForeground(color);
        return lbl;
    }

    private JLabel statusBadge(String status) {
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        lbl.setFont(AppTheme.headingFont(11));
        lbl.setForeground(status.equals("paid") ? GREEN
            : status.equals("partial") ? AMBER : RED_FG);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        lbl.setOpaque(false);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

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
}