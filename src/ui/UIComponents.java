package ui;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

// ============================================================
//  UIComponents.java
//  Contains: AppTheme, StyledButton, StyledTextField,
//            StyledPasswordField, RoundedBorder
// ============================================================

class AppTheme {
    // Blues
    static final Color NAVY       = new Color(10, 25, 60);
    static final Color DEEP_BLUE  = new Color(15, 52, 96);
    static final Color MID_BLUE   = new Color(21, 101, 192);
    static final Color SKY_BLUE   = new Color(66, 165, 245);
    static final Color LIGHT_BLUE = new Color(179, 229, 252);
    static final Color PALE_BLUE  = new Color(227, 242, 253);

    // Accent
    static final Color GOLD       = new Color(255, 193, 7);
    static final Color GOLD_DARK  = new Color(255, 160, 0);

    // Neutrals
    static final Color WHITE      = Color.WHITE;
    static final Color TEXT_DARK  = new Color(13, 27, 52);
    static final Color TEXT_MUTED = new Color(100, 130, 180);
    static final Color CARD_BG    = new Color(240, 247, 255);
    static final Color ERROR_RED  = new Color(211, 47, 47);

    // Fonts
    static Font titleFont(int size)   { return new Font("Georgia", Font.BOLD, size); }
    static Font headingFont(int size) { return new Font("SansSerif", Font.BOLD, size); }
    static Font bodyFont(int size)    { return new Font("SansSerif", Font.PLAIN, size); }
}

// ============================================================

class RoundedBorder extends AbstractBorder {
    private int radius, thickness;
    private Color color;

    RoundedBorder(int radius, Color color, int thickness) {
        this.radius = radius; this.color = color; this.thickness = thickness;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));
        g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, radius, radius);
        g2.dispose();
    }

    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }
}

// ============================================================

class StyledButton extends JButton {
    private Color bgColor, hoverColor;
    private boolean hovered = false;

    StyledButton(String text, Color bg, Color hover) {
        super(text);
        this.bgColor = bg; this.hoverColor = hover;
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFont(AppTheme.headingFont(14));
        setForeground(AppTheme.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(260, 46));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(hovered ? hoverColor : bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        if (hovered) {
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 12, 12);
        }
        super.paintComponent(g2);
        g2.dispose();
    }
}

// ============================================================

class StyledTextField extends JTextField {
    private String placeholder;

    StyledTextField(String placeholder, int cols) {
        super(cols);
        this.placeholder = placeholder;
        setFont(AppTheme.bodyFont(14));
        setForeground(AppTheme.TEXT_DARK);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isFocusOwner() ? new Color(227, 242, 253) : Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        super.paintComponent(g2);
        if (getText().isEmpty() && !isFocusOwner()) {
            g2.setColor(AppTheme.TEXT_MUTED);
            g2.setFont(AppTheme.bodyFont(13));
            g2.drawString(placeholder, 14, getHeight() / 2 + 5);
        }
        g2.dispose();
    }
}

// ============================================================

class StyledPasswordField extends JPasswordField {

    StyledPasswordField(int cols) {
        super(cols);
        setFont(AppTheme.bodyFont(14));
        setForeground(AppTheme.TEXT_DARK);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isFocusOwner() ? new Color(227, 242, 253) : Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        super.paintComponent(g2);
        g2.dispose();
    }
}
