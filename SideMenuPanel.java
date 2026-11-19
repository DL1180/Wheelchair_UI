import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Clean white slide-in menu: logo header, page links, a LOCATIONS section
 * (switches the map floor) and a device card at the bottom.
 * Icons are drawn in code, so no image files are needed.
 */
public class SideMenuPanel extends JPanel {

    public interface Listener {
        void onPage(String page);
        void onFloor(String floor);
        void onClose();
    }

    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color SOFT = new Color(71, 85, 105);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color NAVY = new Color(22, 74, 108);
    private static final Color TEAL = new Color(39, 166, 154);
    private static final Color TINT = new Color(225, 238, 247);
    private static final Color LINE = new Color(226, 232, 240);

    static final int HOME = 0, HEALTH = 1, SETTINGS = 2, PIN = 3, LOGO = 4, CLOSE = 5, MENU = 6;

    private final List<Row> pageRows = new ArrayList<>();
    private final List<Row> floorRows = new ArrayList<>();


    public SideMenuPanel(Listener l) {

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(28, 22, 22, 22));
        addMouseListener(new MouseAdapter() { }); // swallow taps on the panel itself

        add(createHeader(l));
        add(Box.createVerticalStrut(28));

        addPageRow(l, HOME, "Home", "HOME");
        addPageRow(l, HEALTH, "System Health", "STATUS");
        addPageRow(l, SETTINGS, "Settings", "SETTINGS");

        add(Box.createVerticalStrut(18));
        add(divider());
        add(Box.createVerticalStrut(20));
        add(sectionLabel("LOCATIONS"));
        add(Box.createVerticalStrut(8));

        for (String floor : IndoorMapPanel.floorNames()) {
            Row r = new Row(PIN, floor, floor, true);
            r.addActionListener(e -> l.onFloor(floor));
            floorRows.add(r);
            add(r);
            add(Box.createVerticalStrut(4));
        }

        add(Box.createVerticalGlue());

        setActivePage("HOME");
        setActiveFloor(IndoorMapPanel.DEFAULT_FLOOR);
    }


    public void setActivePage(String page) {
        for (Row r : pageRows) r.setActive(r.key.equals(page));
    }


    public void setActiveFloor(String floor) {
        for (Row r : floorRows) r.setActive(r.key.equals(floor));
    }


    private void addPageRow(Listener l, int icon, String label, String page) {
        Row r = new Row(icon, label, page, false);
        r.addActionListener(e -> l.onPage(page));
        pageRows.add(r);
        add(r);
        add(Box.createVerticalStrut(4));
    }


    // ---------------- pieces ----------------

    private JPanel createHeader(Listener l) {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(100, 46));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

        JLabel name = new JLabel("Wheelchair");
        name.setFont(new Font("SansSerif", Font.BOLD, 18));
        name.setForeground(TEXT);

        left.add(new IconView(LOGO, 34));
        left.add(Box.createHorizontalStrut(10));
        left.add(name);

        JButton close = new JButton() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getModel().isPressed() ? new Color(203, 213, 225) : new Color(241, 245, 249));
                g.fillOval(0, 3, 40, 40);
                drawIcon(g, CLOSE, 13, 16, 14, SOFT, Color.WHITE);
                g.dispose();
            }
        };
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setPreferredSize(new Dimension(44, 46));
        close.addActionListener(e -> l.onClose());

        header.add(left, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);
        return header;
    }


    private JPanel divider() {
        JPanel d = new JPanel();
        d.setBackground(LINE);
        d.setPreferredSize(new Dimension(100, 1));
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        return d;
    }


    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12)
                .deriveFont(Map.of(TextAttribute.TRACKING, 0.14f)));
        l.setForeground(MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 6, 0, 0));
        return l;
    }


    // ---------------- painting the panel itself ----------------

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        // extra width pushes the right-hand corners off-screen: only the left corners show
        g.fill(new RoundRectangle2D.Double(0, 0, getWidth() + 44, getHeight(), 44, 44));
        g.dispose();
    }


    // ---------------- menu row ----------------

    private static class Row extends JButton {

        final String key;
        private final int icon;
        private final String label;
        private final boolean floorStyle;
        private boolean active;

        Row(int icon, String label, String key, boolean floorStyle) {
            this.icon = icon;
            this.label = label;
            this.key = key;
            this.floorStyle = floorStyle;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(100, 54));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setActive(boolean a) {
            active = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            boolean pill = active && !floorStyle;
            Color bg = pill ? TINT : Color.WHITE;

            if (pill) {
                g.setColor(TINT);
                g.fillRoundRect(0, 0, w, h, h, h);
            } else if (getModel().isPressed()) {
                g.setColor(new Color(241, 245, 249));
                g.fillRoundRect(0, 0, w, h, h, h);
                bg = new Color(241, 245, 249);
            }

            Color fg = pill ? NAVY : (active ? TEAL : SOFT);
            drawIcon(g, icon, 18, (h - 24) / 2.0, 24, fg, bg);

            g.setColor(active ? (floorStyle ? TEXT : NAVY) : SOFT);
            g.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 17));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, 58, (h - fm.getHeight()) / 2 + fm.getAscent());

            if (active && floorStyle) { // check mark
                Path2D check = new Path2D.Double();
                check.moveTo(w - 38, h / 2.0);
                check.lineTo(w - 32, h / 2.0 + 6);
                check.lineTo(w - 22, h / 2.0 - 6);
                g.setColor(TEAL);
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(check);
            }
            g.dispose();
        }
    }


    private static class IconView extends JComponent {

        private final int icon, size;

        IconView(int icon, int size) {
            this.icon = icon;
            this.size = size;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawIcon(g, icon, 0, 0, size, NAVY, Color.WHITE);
            g.dispose();
        }
    }


    // ---------------- outline icons (unit box, scaled to size) ----------------

    static void drawIcon(Graphics2D g, int icon, double x, double y, double s, Color fg, Color bg) {

        AffineTransform t = new AffineTransform();
        t.translate(x, y);
        t.scale(s, s);
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(fg);

        switch (icon) {
            case HOME -> {
                Path2D p = new Path2D.Double();
                p.moveTo(0.5, 0.10);
                p.lineTo(0.92, 0.48);
                p.lineTo(0.82, 0.48);
                p.lineTo(0.82, 0.90);
                p.lineTo(0.18, 0.90);
                p.lineTo(0.18, 0.48);
                p.lineTo(0.08, 0.48);
                p.closePath();
                g.draw(t.createTransformedShape(p));
                Path2D door = new Path2D.Double();
                door.moveTo(0.42, 0.90);
                door.lineTo(0.42, 0.62);
                door.lineTo(0.58, 0.62);
                door.lineTo(0.58, 0.90);
                g.draw(t.createTransformedShape(door));
            }
            case HEALTH -> {
                Path2D p = new Path2D.Double();
                p.moveTo(0.5, 0.90);
                p.curveTo(0.05, 0.58, 0.0, 0.28, 0.2, 0.15);
                p.curveTo(0.35, 0.05, 0.47, 0.15, 0.5, 0.26);
                p.curveTo(0.53, 0.15, 0.65, 0.05, 0.8, 0.15);
                p.curveTo(1.0, 0.28, 0.95, 0.58, 0.5, 0.90);
                g.draw(t.createTransformedShape(p));
            }
            case SETTINGS -> { // sliders
                double[] ys = {0.22, 0.50, 0.78};
                double[] kx = {0.66, 0.34, 0.70};
                for (double yy : ys) {
                    g.draw(t.createTransformedShape(new Line2D.Double(0.08, yy, 0.92, yy)));
                }
                for (int i = 0; i < 3; i++) {
                    Shape knob = t.createTransformedShape(
                            new Ellipse2D.Double(kx[i] - 0.1, ys[i] - 0.1, 0.2, 0.2));
                    g.setColor(bg);
                    g.fill(knob);
                    g.setColor(fg);
                    g.draw(knob);
                }
            }
            case PIN -> {
                Path2D p = new Path2D.Double();
                p.moveTo(0.5, 0.96);
                p.curveTo(0.22, 0.68, 0.2, 0.52, 0.2, 0.38);
                p.curveTo(0.2, 0.18, 0.34, 0.06, 0.5, 0.06);
                p.curveTo(0.66, 0.06, 0.8, 0.18, 0.8, 0.38);
                p.curveTo(0.8, 0.52, 0.78, 0.68, 0.5, 0.96);
                p.closePath();
                g.draw(t.createTransformedShape(p));
                g.draw(t.createTransformedShape(new Ellipse2D.Double(0.4, 0.28, 0.2, 0.2)));
            }
            case LOGO -> { // wheelchair
                g.fill(t.createTransformedShape(new Ellipse2D.Double(0.30, 0.05, 0.15, 0.15)));   // head
                Path2D body = new Path2D.Double();
                body.moveTo(0.375, 0.27); body.lineTo(0.375, 0.56); body.lineTo(0.66, 0.56); body.lineTo(0.78, 0.84);
                g.draw(t.createTransformedShape(body));
                g.draw(t.createTransformedShape(new Line2D.Double(0.375, 0.40, 0.60, 0.40)));   // arm
                g.draw(t.createTransformedShape(new Arc2D.Double(0.15, 0.36, 0.56, 0.56, 60, 270, Arc2D.OPEN))); // wheel
            }
            case MENU -> {
                for (double yy : new double[]{0.25, 0.5, 0.75}) {
                    g.draw(t.createTransformedShape(new Line2D.Double(0.12, yy, 0.88, yy)));
                }
            }
            default -> { // CLOSE
                g.draw(t.createTransformedShape(new Line2D.Double(0.1, 0.1, 0.9, 0.9)));
                g.draw(t.createTransformedShape(new Line2D.Double(0.9, 0.1, 0.1, 0.9)));
            }
        }
    }
}