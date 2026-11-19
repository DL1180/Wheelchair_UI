import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import java.util.Map;

public class Main {

    // Shared look: white surfaces on a soft slate background, navy/teal accents,
    // pill shapes, thin outline icons (same as the side menu).
    private static final Color NAVY = new Color(22, 74, 108);
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color RED = new Color(220, 38, 38);

    private static final Color LIGHT_BG = new Color(245, 247, 250);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color SOFT = new Color(71, 85, 105);
    private static final Color INK = new Color(17, 24, 39); // near-black text
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color TINT = new Color(225, 238, 247);
    private static final Color LINE = new Color(226, 232, 240);

    private enum NavState { IDLE, NAVIGATING, ARRIVED }

    private JFrame frame;
    private JLayeredPane layeredPane;
    private JPanel mainPanel;
    private SideMenuPanel sideMenu;
    private JPanel scrim;
    private static final int MENU_W = 300;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private NavButton[] tabs;

    private JLabel esp32Label;
private JLabel motionLabel;
private JLabel obstacleLabel;
private JLabel communicationLabel;
private JLabel distanceLabel;

private IndoorMapPanel mapPanel;

  

    // location pull-down
    private LocationPill locationButton;
    private String currentFloor = IndoorMapPanel.DEFAULT_FLOOR;

    // navigation
    private NavState navState = NavState.IDLE;
    private String destName;
    private Timer navTimer;
    private Timer arriveTimer;
    private static final int DEMO_WAIT_MS = 5000; // demo: pretend the trip takes 5 seconds
    private Banner banner;
    private Timer hintTimer;

    // Battery pack: LGECOLFP 12 V 50 Ah LiFePO4 with 50 A BMS.
    // ASSUMPTION: two batteries in series (24 V system). Change PACK_BATTERIES if that's wrong.
    private static final int PACK_BATTERIES = 2;
    private static final double BATTERY_AH = 50;
    private static final double DEMO_SOC = 0.82;   // demo state of charge
    private static final double DEMO_AMPS = 2.4;   // demo draw (Pi + LiDAR + idle drive electronics)
    // resting voltage of ONE 12 V LiFePO4 battery (4 cells) at each state of charge (%)
    private static final double[][] LIFEPO4 = {{0, 10.0}, {10, 12.5}, {20, 12.8}, {30, 12.9}, {40, 13.0},
            {50, 13.0}, {60, 13.1}, {70, 13.2}, {80, 13.3}, {90, 13.4}, {100, 13.6}};

    private static double packVolts(double socPercent) {
        for (int i = 1; i < LIFEPO4.length; i++) {
            if (socPercent <= LIFEPO4[i][0]) {
                double t = (socPercent - LIFEPO4[i - 1][0]) / (LIFEPO4[i][0] - LIFEPO4[i - 1][0]);
                return PACK_BATTERIES * (LIFEPO4[i - 1][1] + t * (LIFEPO4[i][1] - LIFEPO4[i - 1][1]));
            }
        }
        return PACK_BATTERIES * LIFEPO4[LIFEPO4.length - 1][1];
    }


    public static void main(String[] args) {
        // one font everywhere (matches the side menu)
        UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 15));
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new Main().createGUI());
    }

    private static Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    private static Font spaced(Font f, float tracking) {
        return f.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }

    private static Graphics2D smooth(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }


    private void createGUI() {

        frame = new JFrame("Wheelchair Interface");
        frame.setSize(500, 850);
        frame.setMinimumSize(new Dimension(500, 850));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // Kiosk mode (optional): uncomment for a fullscreen tablet
        // frame.dispose();
        // frame.setUndecorated(true);
        // GraphicsEnvironment.getLocalGraphicsEnvironment()
        //         .getDefaultScreenDevice().setFullScreenWindow(frame);

        layeredPane = new JLayeredPane();
        mainPanel = createMainPanel();
        sideMenu = createSideMenu();

        scrim = new JPanel() { // translucent dim overlay behind the menu
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(15, 23, 42, 110));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        scrim.setOpaque(false);
        scrim.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { setMenuOpen(false); }
        });

        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(scrim, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(sideMenu, JLayeredPane.MODAL_LAYER);
        scrim.setVisible(false);
        sideMenu.setVisible(false);

        frame.setContentPane(layeredPane);
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateBounds();
            }
        });

        frame.setVisible(true);
        updateBounds();
        showPage("HOME");

    ESP32Comm esp32 = new ESP32Comm(
        esp32Label,
        motionLabel,
        obstacleLabel,
        communicationLabel,
        distanceLabel
);

esp32.start();
    }


    private void updateBounds() {
        int w = layeredPane.getWidth();
        int h = layeredPane.getHeight();
        mainPanel.setBounds(0, 0, w, h);
        scrim.setBounds(0, 0, w, h);
        sideMenu.setBounds(w - MENU_W, 0, MENU_W, h); // panel on the right edge
    }


    // ============================================================
    // MAIN AREA: top bar / pages / bottom bar
    // ============================================================

   private JPanel createMainPanel() {

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(LIGHT_BG);

    panel.add(createTopBar(), BorderLayout.NORTH);

    cardLayout = new CardLayout();

    contentPanel = new JPanel(cardLayout);
    contentPanel.setBackground(LIGHT_BG);

    contentPanel.add(createHomePage(), "HOME");
    contentPanel.add(createStatusScrollPane(), "STATUS");
    contentPanel.add(createSettingsPage(), "SETTINGS");

    panel.add(contentPanel, BorderLayout.CENTER);

    panel.add(createBottomNav(), BorderLayout.SOUTH);

    return panel;
}


// ============================================================
// STATUS PAGE SCROLL WRAPPER
// ============================================================

private JScrollPane createStatusScrollPane() {

    JPanel page = createStatusPage();

    JScrollPane scrollPane = new JScrollPane(page);

    scrollPane.setBorder(null);
    scrollPane.setBackground(LIGHT_BG);

    scrollPane.getViewport().setBackground(LIGHT_BG);

    scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    );

    scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
    );

    scrollPane.getVerticalScrollBar().setUnitIncrement(16);

    return scrollPane;
}


// ============================================================
// STATUS PAGE
// ============================================================

private JPanel createStatusPage() {

    JPanel page = createPage("System Health");


    // --------------------------------------------------------
    // WHEELCHAIR POWER
    // --------------------------------------------------------

    page.add(createSectionTitle("Wheelchair Power"));
    page.add(Box.createVerticalStrut(10));

    JPanel batteryCard = createCard();

    batteryCard.add(
            createStatusRow(
                    "Charge",
                    Math.round(DEMO_SOC * 100) + "%",
                    GREEN
            )
    );

    batteryCard.add(Box.createVerticalStrut(10));

    batteryCard.add(
            createStatusRow(
                    "Capacity",
                    String.format(
                            Locale.US,
                            "%.0f of %.0f Ah",
                            DEMO_SOC * BATTERY_AH,
                            BATTERY_AH
                    ),
                    INK
            )
    );

    batteryCard.add(Box.createVerticalStrut(10));

    batteryCard.add(
            createStatusRow(
                    "Pack Voltage",
                    String.format(
                            Locale.US,
                            "%.1f V",
                            packVolts(DEMO_SOC * 100)
                    ),
                    INK
            )
    );

    batteryCard.add(Box.createVerticalStrut(10));

    batteryCard.add(
            createStatusRow(
                    "Current Draw",
                    String.format(
                            Locale.US,
                            "%.1f A",
                            DEMO_AMPS
                    ),
                    INK
            )
    );

    batteryCard.add(Box.createVerticalStrut(10));

    batteryCard.add(
            createStatusRow(
                    "Battery Health",
                    "Good",
                    GREEN
            )
    );

    page.add(batteryCard);


    // --------------------------------------------------------
    // CONNECTIONS
    // --------------------------------------------------------

    page.add(Box.createVerticalStrut(24));

    page.add(createSectionTitle("Connections"));
    page.add(Box.createVerticalStrut(10));

    JPanel connectionCard = createCard();

    ConnectionRow esp32 =
            new ConnectionRow(
                    "ESP32",
                    false
            );

    esp32Label = esp32;

    connectionCard.add(esp32);

    connectionCard.add(
            new ConnectionRow(
                    "ROS 2",
                    false
            )
    );

    connectionCard.add(
            new ConnectionRow(
                    "LiDAR",
                    false
            )
    );

    connectionCard.add(
            new ConnectionRow(
                    "RealSense",
                    false
            )
    );

    page.add(connectionCard);


    // --------------------------------------------------------
    // SAFETY
    // --------------------------------------------------------

    page.add(Box.createVerticalStrut(24));

    page.add(createSectionTitle("Safety"));
    page.add(Box.createVerticalStrut(10));

    JPanel safetyCard = createCard();

    motionLabel =
            createLiveValue(
                    "Stopped",
                    RED
            );

    obstacleLabel =
            createLiveValue(
                    "Unknown",
                    MUTED
            );

    communicationLabel =
            createLiveValue(
                    "Waiting",
                    MUTED
            );

    distanceLabel =
            createLiveValue(
                    "-- mm",
                    MUTED
            );


    safetyCard.add(
            createLiveStatusRow(
                    "Motion",
                    motionLabel
            )
    );

    safetyCard.add(Box.createVerticalStrut(10));

    safetyCard.add(
            createLiveStatusRow(
                    "Obstacle Detection",
                    obstacleLabel
            )
    );

    safetyCard.add(Box.createVerticalStrut(10));

    safetyCard.add(
            createLiveStatusRow(
                    "Communication",
                    communicationLabel
            )
    );

    safetyCard.add(Box.createVerticalStrut(10));

    safetyCard.add(
            createLiveStatusRow(
                    "Front Distance",
                    distanceLabel
            )
    );

    page.add(safetyCard);


    // --------------------------------------------------------
    // DEMO NOTICE
    // --------------------------------------------------------

    JLabel demoNotice =
            new JLabel(
                    "ROS and ultrasonic values are simulated for integration testing"
            );

    demoNotice.setFont(
            font(
                    Font.ITALIC,
                    12
            )
    );

    demoNotice.setForeground(MUTED);
    demoNotice.setAlignmentX(Component.LEFT_ALIGNMENT);

    page.add(Box.createVerticalStrut(12));
    page.add(demoNotice);

    page.add(Box.createVerticalStrut(20));

    return page;
}


    private JPanel createTopBar() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(10, 22, 10, 18)));

        RoundIconButton menuButton = new RoundIconButton(SideMenuPanel.MENU);
        menuButton.addActionListener(e -> setMenuOpen(!sideMenu.isVisible()));

        panel.add(new BrandLabel(), BorderLayout.WEST);
        panel.add(menuButton, BorderLayout.EAST);
        return panel;
    }


    private JPanel createBottomNav() {

        JPanel bar = new JPanel(new GridLayout(1, 3));
        bar.setBackground(Color.WHITE);
        bar.setPreferredSize(new Dimension(100, 78));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE));

        tabs = new NavButton[]{
                new NavButton(SideMenuPanel.HEALTH, "Health", "STATUS"),
                new NavButton(SideMenuPanel.HOME, "Home", "HOME"),
                new NavButton(SideMenuPanel.SETTINGS, "Settings", "SETTINGS")
        };
        for (NavButton t : tabs) {
            t.addActionListener(e -> showPage(t.page));
            bar.add(t);
        }
        return bar;
    }


    private void showPage(String page) {
        cardLayout.show(contentPanel, page);
        setMenuOpen(false);
        sideMenu.setActivePage(page);
        for (NavButton t : tabs) t.setActive(t.page.equals(page));
    }


    // ------------------------------------------------------------
    // small custom components (all share the side menu's look)
    // ------------------------------------------------------------

    /** Logo + app name, same as the side menu header. */
    private static class BrandLabel extends JComponent {

        BrandLabel() {
            setPreferredSize(new Dimension(220, 48));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            SideMenuPanel.drawIcon(g, SideMenuPanel.LOGO, 0, (getHeight() - 34) / 2.0, 34, NAVY, Color.WHITE);

            g.setFont(font(Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            int base = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.setColor(TEXT);
            g.drawString("Wheelchair", 46, base);
            g.dispose();
        }
    }


    /** Soft grey round button with a thin outline icon. */
    private static class RoundIconButton extends JButton {

        private final int icon;

        RoundIconButton(int icon) {
            this.icon = icon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(48, 48));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int d = Math.min(getWidth(), getHeight());
            Color bg = getModel().isPressed() ? new Color(226, 232, 240) : new Color(241, 245, 249);
            g.setColor(bg);
            g.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
            SideMenuPanel.drawIcon(g, icon, (getWidth() - 22) / 2.0, (getHeight() - 22) / 2.0, 22, SOFT, bg);
            g.dispose();
        }
    }


    /** White pill: blue pin, floor name, chevron at the far end. */
    private static class LocationPill extends JButton {

        private static final Color PIN_BLUE = new Color(37, 99, 235);
        private String floor = "";

        LocationPill() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        void setFloor(String f) {
            floor = f;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), ph = getHeight() - 3;

            g.setColor(new Color(15, 23, 42, 14));
            g.fillRoundRect(1, 3, w - 2, ph, ph, ph);
            g.setColor(getModel().isPressed() ? new Color(241, 245, 249) : Color.WHITE);
            g.fillRoundRect(0, 0, w - 1, ph, ph, ph);
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(0, 0, w - 1, ph, ph, ph);

            double r = 6.5, cx = 28, tipY = ph / 2.0 + 10;
            Path2D pin = new Path2D.Double();
            pin.moveTo(cx, tipY);
            pin.curveTo(cx - r * 1.3, tipY - r * 1.2, cx - r * 1.2, tipY - r * 3, cx, tipY - r * 3);
            pin.curveTo(cx + r * 1.2, tipY - r * 3, cx + r * 1.3, tipY - r * 1.2, cx, tipY);
            g.setColor(PIN_BLUE);
            g.fill(pin);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(cx - 2.5, tipY - r * 2 - 2.5, 5, 5));

            g.setColor(TEXT);
            g.setFont(font(Font.BOLD, 17));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(floor, 50, (ph - fm.getHeight()) / 2 + fm.getAscent());

            double ax = w - 30, ay = ph / 2.0;
            Path2D chevron = new Path2D.Double();
            chevron.moveTo(ax - 6, ay - 3);
            chevron.lineTo(ax, ay + 3);
            chevron.lineTo(ax + 6, ay - 3);
            g.setColor(MUTED);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(chevron);
            g.dispose();
        }
    }


    /** Bottom bar tab: outline icon in a soft pill when active, label underneath. */
    private static class NavButton extends JButton {

        final String page;
        private final int icon;
        private final String label;
        private boolean active;

        NavButton(int icon, String label, String page) {
            this.icon = icon;
            this.label = label;
            this.page = page;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        void setActive(boolean a) {
            active = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();

            Color bg = Color.WHITE;
            if (active) {
                bg = TINT;
                g.setColor(TINT);
                g.fillRoundRect((w - 68) / 2, 9, 68, 34, 34, 34);
            }
            SideMenuPanel.drawIcon(g, icon, (w - 24) / 2.0, 14, 24, active ? NAVY : MUTED, bg);

            g.setFont(font(active ? Font.BOLD : Font.PLAIN, 13));
            g.setColor(active ? NAVY : MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, (w - fm.stringWidth(label)) / 2, h - 14);
            g.dispose();
        }
    }


    /** The status banner under the map: question -> heading to X -> arrived. Text is centered. */
    private static class Banner extends JComponent {

        private String title = "", sub = "";
        private Color bg = Color.WHITE, fg = TEXT;

        Banner() {
            setPreferredSize(new Dimension(100, 96));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        }

        void set(String title, String sub, Color bg, Color fg) {
            this.title = title;
            this.sub = sub;
            this.bg = bg;
            this.fg = fg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();
            boolean light = bg.equals(Color.WHITE);

            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 36, 36);
            g.setColor(bg);
            g.fill(shape);
            if (light) {
                g.setColor(LINE);
                g.draw(shape);
            }

            Font tf = font(Font.BOLD, 20), sf = font(Font.PLAIN, 15);
            FontMetrics tm = g.getFontMetrics(tf), sm = g.getFontMetrics(sf);
            int y = (h - (tm.getHeight() + sm.getHeight() + 2)) / 2;

            g.setFont(tf);
            g.setColor(fg);
            g.drawString(title, (w - tm.stringWidth(title)) / 2, y + tm.getAscent());

            g.setFont(sf);
            g.setColor(light ? MUTED : new Color(255, 255, 255, 215));
            g.drawString(sub, (w - sm.stringWidth(sub)) / 2, y + tm.getHeight() + 2 + sm.getAscent());
            g.dispose();
        }
    }


    /** One connection: name in black, status shown as a green (connected) or red dot. */
    private static class ConnectionRow extends JLabel {

        private final String name;
        private boolean connected;

        ConnectionRow(String name, boolean connected) {
            this.name = name;
            this.connected = connected;
            setPreferredSize(new Dimension(100, 36));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setConnected(boolean c) {
            connected = c;
            repaint();
        }

        /** ESP32Comm reports its status as text (e.g. "ESP32: Connected"); turn that into the dot colour. */
        @Override
        public void setText(String t) {
            if (name != null && t != null) {
                String s = t.toLowerCase();
                connected = (s.contains("connected") && !s.contains("disconnected")) || s.contains("online");
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();

            g.setFont(font(Font.PLAIN, 16));
            g.setColor(INK);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(name, 0, (h - fm.getHeight()) / 2 + fm.getAscent());

            Color c = connected ? GREEN : RED;
            int d = 14, x = w - d - 4, y = (h - d) / 2;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
            g.fillOval(x - 4, y - 4, d + 8, d + 8);
            g.setColor(c);
            g.fillOval(x, y, d, d);
            g.dispose();
        }
    }


    /** Big red pill. Still an unmissable STOP, just cleaner. */
    private static class StopButton extends JButton {

        StopButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(100, 84));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight() - 5;

            g.setColor(new Color(220, 38, 38, 55));
            g.fillRoundRect(3, 5, w - 6, h, h, h);
            g.setColor(getModel().isPressed() ? new Color(185, 28, 28) : RED);
            g.fillRoundRect(0, 0, w, h, h, h);

            g.setFont(spaced(font(Font.BOLD, 28), 0.14f));
            FontMetrics fm = g.getFontMetrics();
            int icon = 22, gap = 14;
            int total = icon + gap + fm.stringWidth("STOP");
            int x = (w - total) / 2;

            g.setColor(Color.WHITE);
            g.fillRoundRect(x, (h - icon) / 2, icon, icon, 6, 6);
            g.drawString("STOP", x + icon + gap, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }


    /** White rounded card with a hairline border. */
    private static class RoundedCard extends JPanel {

        RoundedCard(Insets pad) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(pad));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() { // never stretch taller than content
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            RoundRectangle2D r = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.setColor(Color.WHITE);
            g.fill(r);
            g.setColor(LINE);
            g.draw(r);
            g.dispose();
        }
    }


    /** Accordion title row with a chevron that flips when open. */
    private static class AccordionHeader extends JButton {

        private final String title;
        private boolean open;

        AccordionHeader(String title) {
            this.title = title;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(100, 58));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setOpen(boolean o) {
            open = o;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();

            if (getModel().isPressed()) {
                g.setColor(new Color(241, 245, 249));
                g.fillRoundRect(2, 2, w - 4, h - 4, 24, 24);
            }
            g.setColor(TEXT);
            g.setFont(font(Font.PLAIN, 17));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, 20, (h - fm.getHeight()) / 2 + fm.getAscent());

            double ax = w - 32, ay = h / 2.0, d = open ? -3 : 3;
            Path2D chevron = new Path2D.Double();
            chevron.moveTo(ax - 6, ay - d);
            chevron.lineTo(ax, ay + d);
            chevron.lineTo(ax + 6, ay - d);
            g.setColor(MUTED);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(chevron);
            g.dispose();
        }
    }


    // ============================================================
    // SIDE MENU (hamburger, top right)
    // ============================================================

    private SideMenuPanel createSideMenu() {
        return new SideMenuPanel(new SideMenuPanel.Listener() {
            @Override public void onPage(String page) { showPage(page); }
            @Override public void onFloor(String floor) {
                if (navState == NavState.NAVIGATING) return; // don't switch maps mid-trip
                changeFloor(floor);
                setMenuOpen(false);
            }
            @Override public void onClose() { setMenuOpen(false); }
        });
    }


    private void setMenuOpen(boolean open) {
        sideMenu.setVisible(open);
        scrim.setVisible(open);
    }


    // ============================================================
    // HOME PAGE
    // ============================================================

    private JPanel createHomePage() {

        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(LIGHT_BG);
        page.setBorder(new EmptyBorder(16, 20, 16, 20));

        // location pill
        locationButton = new LocationPill();
        locationButton.setPreferredSize(new Dimension(100, 48));
        locationButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        locationButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        locationButton.addActionListener(e -> showLocationMenu());
        updateLocationText();
        page.add(locationButton);
        page.add(Box.createVerticalStrut(12));

        // tappable map
        mapPanel = new IndoorMapPanel();
        mapPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapPanel.setOnRoomTapped(room -> selectDestination(room.name()));
        page.add(mapPanel);
        page.add(Box.createVerticalStrut(14));

        // status banner
        banner = new Banner();
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(banner);
        page.add(Box.createVerticalStrut(14));

        // STOP
        StopButton stopButton = new StopButton();
        stopButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        stopButton.addActionListener(e -> resetNavigation()); // TODO: also send STOP to the ESP32
        page.add(stopButton);

        refreshWhereNext();
        return page;
    }


    private void updateLocationText() {
        locationButton.setFloor(currentFloor);
    }


    private void showLocationMenu() {
        if (navState == NavState.NAVIGATING) return; // don't switch maps mid-trip

        JPopupMenu menu = new JPopupMenu();
        for (String name : IndoorMapPanel.floorNames()) {
            JMenuItem item = new JMenuItem(name);
            item.setFont(font(Font.BOLD, 17));
            item.setPreferredSize(new Dimension(locationButton.getWidth(), 52));
            item.addActionListener(e -> changeFloor(name));
            menu.add(item);
        }
        menu.show(locationButton, 0, locationButton.getHeight());
    }


    private void changeFloor(String name) {
        currentFloor = name;
        mapPanel.setFloor(name);   // restores where the chair was left on that floor
        resetNavigation();
        updateLocationText();
        sideMenu.setActiveFloor(name);
    }


    // ============================================================
    // NAVIGATION FLOW  (tap room -> 5 s trip -> arrived -> back to default)
    // ============================================================

    private void selectDestination(String name) {

        if (navState != NavState.IDLE) return;   // ignore taps during a trip

        if (name.equals(mapPanel.getSpot())) {   // already there
            banner.set("You're already here", name, Color.WHITE, TEXT);
            if (hintTimer != null) hintTimer.stop();
            hintTimer = new Timer(2000, e -> { if (navState == NavState.IDLE) refreshWhereNext(); });
            hintTimer.setRepeats(false);
            hintTimer.start();
            return;
        }
        if (!mapPanel.setDestination(name)) return;

        destName = name;
        startNavigation();
    }


    private void startNavigation() {

        navState = NavState.NAVIGATING;
        mapPanel.beginTrip();
        refreshWhereNext();

        // DEMO: just wait. The real version waits for Nav2's "goal reached" and the live pose.
        navTimer = new Timer(DEMO_WAIT_MS, e -> {
            mapPanel.arrive();
            navState = NavState.ARRIVED;
            refreshWhereNext();

            // show "You have arrived" for 3 s, then back to the default screen
            arriveTimer = new Timer(3000, ev -> resetNavigation());
            arriveTimer.setRepeats(false);
            arriveTimer.start();
        });
        navTimer.setRepeats(false);
        navTimer.start();
    }


    private void resetNavigation() {

        if (navTimer != null) navTimer.stop();
        if (arriveTimer != null) arriveTimer.stop();
        mapPanel.setDestination(null);
        navState = NavState.IDLE;
        destName = null;
        refreshWhereNext();
    }


    private void refreshWhereNext() {

        switch (navState) {
            case IDLE -> {
                String spot = mapPanel.getSpot();   // remembered from the last trip
                banner.set("Where would you like to go?",
                        spot != null ? "You're at " + spot : "Tap a room on the map",
                        Color.WHITE, TEXT);
            }
            case NAVIGATING -> banner.set("Heading to " + destName, "Waiting\u2026", NAVY, Color.WHITE);
            case ARRIVED -> banner.set("You have arrived", destName, GREEN, Color.WHITE);
        }
    }


    // ============================================================
    // STATUS PAGE
    // ============================================================


    

    // ============================================================
    // SETTINGS
    // ============================================================

    private JPanel createSettingsPage() {

        JPanel page = createPage("Settings");

        JPanel nav = accordionContent();
        nav.add(new JLabel("Saved locations"));
        nav.add(Box.createVerticalStrut(8));
        JComboBox<String> locations = new JComboBox<>(IndoorMapPanel.floorNames().toArray(new String[0]));
        locations.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        locations.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(locations);

        JPanel safety = accordionContent();
        safety.add(new JLabel("Obstacle stop distance: 15 cm"));
        safety.add(Box.createVerticalStrut(8));
        safety.add(new JLabel("Emergency stop: Enabled"));

        JPanel system = accordionContent();
        system.add(new JLabel("ROS connection: Demo"));
        system.add(Box.createVerticalStrut(8));
        system.add(new JLabel("ESP32 communication: UART"));

        JPanel about = accordionContent();
        about.add(new JLabel("Self-Guiding Wheelchair"));
        about.add(Box.createVerticalStrut(6));
        about.add(new JLabel("Prototype Interface"));

        page.add(createAccordion("Navigation", nav));
        page.add(Box.createVerticalStrut(10));
        page.add(createAccordion("Safety", safety));
        page.add(Box.createVerticalStrut(10));
        page.add(createAccordion("System", system));
        page.add(Box.createVerticalStrut(10));
        page.add(createAccordion("About", about));
        return page;
    }


    private JPanel createAccordion(String title, JPanel content) {

        RoundedCard container = new RoundedCard(new Insets(0, 0, 0, 0));
        AccordionHeader header = new AccordionHeader(title);

        content.setVisible(false);
        header.addActionListener(e -> {
            boolean open = !content.isVisible();
            content.setVisible(open);
            header.setOpen(open);
            container.revalidate();
            container.repaint();
        });

        container.add(header);
        container.add(content);
        return container;
    }


    // ============================================================
    // HELPERS
    // ============================================================

    private JPanel createPage(String title) {

        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(LIGHT_BG);
        page.setBorder(new EmptyBorder(24, 22, 24, 22));

        JLabel heading = new JLabel(title);
        heading.setFont(font(Font.BOLD, 26));
        heading.setForeground(TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        page.add(heading);
        page.add(Box.createVerticalStrut(22));
        return page;
    }


    /** Small spaced-out caps label, like ACCOUNT / LOCATIONS in the side menu. */
    private JLabel createSectionTitle(String text) {

        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(spaced(font(Font.BOLD, 12), 0.14f));
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(0, 6, 0, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }


    private JPanel createCard() {
        return new RoundedCard(new Insets(16, 18, 16, 18));
    }

private JPanel createStatusRow(String name, String value, Color color) {

    JPanel row = new JPanel(new BorderLayout());

    row.setOpaque(false);
    row.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 32)
    );
    row.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel left = new JLabel(name);
    left.setFont(font(Font.PLAIN, 16));
    left.setForeground(INK);

    JLabel right = new JLabel(value);
    right.setForeground(color);
    right.setFont(font(Font.BOLD, 16));

    row.add(left, BorderLayout.WEST);
    row.add(right, BorderLayout.EAST);

    return row;
}
   
    private JLabel createLiveValue(String text, Color color) {

    JLabel label = new JLabel(text);

    label.setForeground(color);
    label.setFont(font(Font.BOLD, 16));

    return label;
}


private JPanel createLiveStatusRow(
        String name,
        JLabel value) {

    JPanel row = new JPanel(new BorderLayout());

    row.setOpaque(false);
    row.setMaximumSize(
            new Dimension(
                    Integer.MAX_VALUE,
                    32
            )
    );

    row.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel left = new JLabel(name);

    left.setFont(font(Font.PLAIN, 16));
    left.setForeground(INK);

    row.add(left, BorderLayout.WEST);
    row.add(value, BorderLayout.EAST);

    return row;
}


    private JPanel accordionContent() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(0, 20, 18, 20));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
}