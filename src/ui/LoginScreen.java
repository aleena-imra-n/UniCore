package ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;
import bl.AuthService;     
import model.UserSession;

public class LoginScreen extends JFrame {

//    // Demo credentials: role -> {username, password}
//    private static final Map<String, String[]> USERS = new HashMap<>();
//    static {
//        USERS.put("Student", new String[]{"student1", "pass123"});
//        USERS.put("Teacher", new String[]{"teacher1", "pass123"});
//        USERS.put("Admin",   new String[]{"admin1",   "pass123"});
//    }

    private StyledTextField usernameField;
    private StyledPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private JLabel errorLabel;
    private JButton loginBtn;

    public LoginScreen() {
        setTitle("UniCore — University Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 580);
        setLocationRelativeTo(null);
        setResizable(false);
        setContentPane(buildContent());
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridLayout(1, 2)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };

        // LEFT PANEL — branding
        JPanel left = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Deep gradient background
                GradientPaint gp = new GradientPaint(0, 0, AppTheme.NAVY, getWidth(), getHeight(), AppTheme.DEEP_BLUE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Decorative circles
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(-60, -60, 280, 280);
                g2.fillOval(getWidth() - 120, getHeight() - 120, 240, 240);
                g2.setColor(new Color(66, 165, 245, 18));
                g2.fillOval(30, getHeight() - 200, 200, 200);
                // Gold accent line
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(40, getHeight() / 2 - 60, 40, getHeight() / 2 + 60);
                g2.dispose();
            }
        };
        left.setLayout(new GridBagLayout());
        left.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel leftContent = new JPanel();
        leftContent.setOpaque(false);
        leftContent.setLayout(new BoxLayout(leftContent, BoxLayout.Y_AXIS));

        // Logo circle
        JPanel logoCircle = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.GOLD);
                g2.fillOval(0, 0, 64, 64);
                g2.setColor(AppTheme.NAVY);
                g2.setFont(AppTheme.titleFont(26));
                FontMetrics fm = g2.getFontMetrics();
                String u = "U";
                g2.drawString(u, (64 - fm.stringWidth(u)) / 2, 64/2 + fm.getAscent()/2 - 2);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(64, 64); }
        };
        logoCircle.setOpaque(false);
        logoCircle.setMaximumSize(new Dimension(64, 64));
        leftContent.add(logoCircle);
        leftContent.add(Box.createVerticalStrut(24));

        JLabel teamLabel = new JLabel("UNICORE");
        teamLabel.setFont(AppTheme.titleFont(32));
        teamLabel.setForeground(Color.WHITE);
        teamLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftContent.add(teamLabel);

        JLabel subLabel = new JLabel("University Management System");
        subLabel.setFont(AppTheme.bodyFont(14));
        subLabel.setForeground(AppTheme.LIGHT_BLUE);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftContent.add(subLabel);

        leftContent.add(Box.createVerticalStrut(36));

        // Feature list
        String[] features = {"📚  Course Registration", "📋  Attendance Tracking", "📊  Marks & Transcripts", "🔔  Announcements", "💰  Fee Management"};
        for (String f : features) {
            JLabel fl = new JLabel(f);
            fl.setFont(AppTheme.bodyFont(13));
            fl.setForeground(new Color(179, 229, 252));
            fl.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftContent.add(fl);
            leftContent.add(Box.createVerticalStrut(10));
        }

        left.add(leftContent);
        root.add(left);

        // RIGHT PANEL — login form
        JPanel right = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.PALE_BLUE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(21, 101, 192, 8));
                g2.fillOval(getWidth() - 200, -100, 300, 300);
                g2.dispose();
            }
        };
        right.setLayout(new GridBagLayout());
        right.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setMaximumSize(new Dimension(320, 600));

        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(AppTheme.titleFont(26));
        welcome.setForeground(AppTheme.NAVY);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(welcome);

        JLabel sub = new JLabel("Sign in to your account");
        sub.setFont(AppTheme.bodyFont(13));
        sub.setForeground(AppTheme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sub);
        form.add(Box.createVerticalStrut(30));

        // Gold divider
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(AppTheme.GOLD);
                g.fillRect(0, 0, 40, 3);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(40, 3); }
            @Override public Dimension getMaximumSize()   { return new Dimension(40, 3); }
        };
        divider.setOpaque(false);
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(divider);
        form.add(Box.createVerticalStrut(24));

        // Username
        addLabel(form, "Username");
        usernameField = new StyledTextField("Enter your username", 20);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(usernameField);
        form.add(Box.createVerticalStrut(16));

        // Password
        addLabel(form, "Password");
        passwordField = new StyledPasswordField(20);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passwordField);
        form.add(Box.createVerticalStrut(16));

        // Role
        addLabel(form, "Role");
        roleCombo = new JComboBox<>(new String[]{"Student", "Teacher", "Admin"});
        styleCombo(roleCombo);
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(roleCombo);
        form.add(Box.createVerticalStrut(8));

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(AppTheme.bodyFont(12));
        errorLabel.setForeground(AppTheme.ERROR_RED);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(8));

        // Login button

        loginBtn = new StyledButton("Sign In", AppTheme.MID_BLUE, AppTheme.DEEP_BLUE);
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        loginBtn.addActionListener(e -> handleLogin());
        form.add(loginBtn);

        // Hint
        JLabel hint = new JLabel("Demo: student1 / teacher1 / admin1  |  pass: pass123");
        hint.setFont(AppTheme.bodyFont(11));
        hint.setForeground(AppTheme.TEXT_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        //form.add(hint);

        right.add(form);
        root.add(right);
        return root;
    }

    private void addLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.headingFont(12));
        lbl.setForeground(AppTheme.DEEP_BLUE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(6));
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(AppTheme.bodyFont(14));
        combo.setBackground(Color.WHITE);
        combo.setForeground(AppTheme.TEXT_DARK);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.LIGHT_BLUE, 2),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

     private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String selectedRole = (String) roleCombo.getSelectedItem();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("⚠  All fields are required.");
            return;
        }

        // Disable login button during authentication
        loginBtn.setEnabled(false);
        loginBtn.setText("Authenticating...");
        errorLabel.setText(" ");

        // Perform authentication in background thread
        new SwingWorker<UserSession, Void>() {
            @Override
            protected UserSession doInBackground() throws Exception {
                AuthService authService = new AuthService();
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    UserSession session = get();
                    
                    if (session != null) {
                        // Check if the role from database matches the selected role
                        String dbRole = session.getRole();
                        String selectedRoleLower = selectedRole.toLowerCase();
                        
                        if (!dbRole.equals(selectedRoleLower)) {
                            errorLabel.setText("⚠  Invalid role. This user is a " + dbRole);
                            loginBtn.setEnabled(true);
                            loginBtn.setText("Sign In");
                            return;
                        }
                        
                        // Authentication successful
                        errorLabel.setText(" ");
                        dispose();  // Close login window
                        
                        // Open appropriate dashboard
                        if (session.isStudent()) {
                            new StudentDashboard(session.getUsername()).setVisible(true);
                        } else if (session.isTeacher()) {
                            new TeacherDashboard(session.getUsername()).setVisible(true);
                        } else if (session.isAdmin()) {
                            new AdminDashboard(session.getUsername()).setVisible(true);
                        }
                        
                    } else {
                        // Authentication failed
                        errorLabel.setText("⚠  Invalid username or password. Please try again.");
                        passwordField.setText("");
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Sign In");
                    }
                } catch (Exception e) {
                    errorLabel.setText("⚠  Database error: " + e.getMessage());
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Sign In");
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}

