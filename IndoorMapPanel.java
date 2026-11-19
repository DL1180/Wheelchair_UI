import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tappable indoor map with several floors/locations (Java 17+).
 * All coordinates are normalized 0..1 inside the map rectangle.
 */
public class IndoorMapPanel extends JPanel {

    /** A destination. "top" rooms have their door on the bottom edge, others on the top edge. */
    public record Room(String name, double x, double y, double w, double h, boolean top) {
        double doorX() { return x + w / 2; }
        double doorY() { return top ? y + h : y; }
    }

    public static final String DEFAULT_FLOOR = "Grove 6th Floor";
    private static final Map<String, List<Room>> FLOORS = new LinkedHashMap<>();

    static {
        FLOORS.put(DEFAULT_FLOOR, List.of(
                new Room("Elevator",  0.04, 0.04, 0.17, 0.20, true),
                new Room("Door 1",    0.03, 0.28, 0.13, 0.11, true),
                new Room("Door 2",    0.03, 0.62, 0.13, 0.11, false),
                new Room("Elevators", 0.27, 0.04, 0.40, 0.27, true),
                new Room("Lab Rooms", 0.75, 0.04, 0.22, 0.37, true),
                new Room("Door 3",    0.24, 0.82, 0.14, 0.13, false),
                new Room("Bathroom",  0.46, 0.80, 0.22, 0.15, false)));

        // Placeholder layouts: replace with the real floor plans
        FLOORS.put("Grove Lobby", List.of(
                new Room("Elevators",  0.05, 0.05, 0.20, 0.32, true),
                new Room("Front Desk", 0.33, 0.05, 0.34, 0.25, true),
                new Room("Restrooms",  0.75, 0.05, 0.21, 0.30, true),
                new Room("Entrance",   0.38, 0.80, 0.24, 0.15, false),
                new Room("Cafe",       0.72, 0.78, 0.24, 0.17, false)));

        FLOORS.put("Exhibition Hall", List.of(
                new Room("Exhibit A",  0.05, 0.05, 0.26, 0.30, true),
                new Room("Exhibit B",  0.37, 0.05, 0.26, 0.30, true),
                new Room("Exhibit C",  0.69, 0.05, 0.26, 0.30, true),
                new Room("Info Desk",  0.20, 0.80, 0.24, 0.15, false),
                new Room("Exit",       0.58, 0.80, 0.20, 0.15, false)));
    }

    public static List<String> floorNames() { return new ArrayList<>(FLOORS.keySet()); }

    private static final double HALL_Y = 0.55;
    private static final double START_X = 0.30;

    private static final Color WALL = new Color(148, 163, 184);
    private static final Color ROOM = new Color(232, 239, 244);
    private static final Color ELEVATOR = new Color(205, 211, 216);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color TEAL = new Color(39, 166, 154);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color RED = new Color(220, 38, 38);

    private List<Room> rooms = FLOORS.get(DEFAULT_FLOOR);
    private double robotX = START_X, robotY = HALL_Y;

    // memory: where the chair last stopped, remembered per floor
    private String floorName = DEFAULT_FLOOR;
    private String spot;                       // room the chair is currently at (null = in the hallway)
    private final Map<String, double[]> savedPose = new HashMap<>();
    private final Map<String, String> savedSpot = new HashMap<>();
    private Room selected;
    private Consumer<Room> onRoomTapped;
    private final Timer animTimer;

    public IndoorMapPanel() {
        setPreferredSize(new Dimension(440, 320));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        setOpaque(false); // the rounded white card is painted in paintComponent

        animTimer = new Timer(40, e -> repaint()); // keeps the "you are here" ring pulsing

        addMouseListener(new MouseAdapter() {
            Point down;

            @Override public void mousePressed(MouseEvent e) { down = e.getPoint(); }

            @Override public void mouseReleased(MouseEvent e) {
                // Touchscreens jitter: treat small movement as a tap
                if (down == null || down.distance(e.getPoint()) > 24) return;
                Room r = roomAt(e.getX(), e.getY());
                if (r != null && onRoomTapped != null) onRoomTapped.accept(r);
            }
        });
    }

    // ---------------- public API ----------------

    public void setOnRoomTapped(Consumer<Room> c) { onRoomTapped = c; }

    /** Switch to another floor/location. The chair keeps its last position on each floor. */
    public void setFloor(String name) {
        List<Room> r = FLOORS.get(name);
        if (r == null) return;

        savedPose.put(floorName, new double[]{robotX, robotY});
        savedSpot.put(floorName, spot);

        floorName = name;
        rooms = r;
        selected = null;
        animTimer.stop();

        double[] p = savedPose.get(name);
        robotX = p != null ? p[0] : START_X;
        robotY = p != null ? p[1] : HALL_Y;
        spot = savedSpot.get(name);
        repaint();
    }

    /** Name of the room the chair is at right now, or null if it is in the hallway / mid-trip. */
    public String getSpot() { return spot; }

    /** Robot pose in normalized map coordinates (yaw is unused by the pin marker). */
    public void setRobotPose(double x, double y, double yaw) {
        robotX = x; robotY = y; repaint();
    }

    /** Select a destination by name (null clears). Returns false if unknown on this floor. */
    public boolean setDestination(String name) {
        if (name == null) {
            selected = null;
            animTimer.stop();
            repaint();
            return true;
        }
        for (Room r : rooms) {
            if (r.name().equals(name)) {
                selected = r;
                animTimer.start();
                repaint();
                return true;
            }
        }
        return false;
    }

    /** Demo: the chair has left its last room. Its real position will come from the sensors. */
    public void beginTrip() {
        spot = null;
        repaint();
    }

    /** Demo: the chair reached the selected destination. Puts the pin at its door and remembers it. */
    public void arrive() {
        if (selected == null) return;
        robotX = selected.doorX();
        robotY = selected.doorY();
        spot = selected.name();
        repaint();
    }

    // ---------------- geometry ----------------

    private Rectangle2D mapRect() {
        return new Rectangle2D.Double(15, 15, getWidth() - 30, getHeight() - 30);
    }

    private Point2D.Double toPx(double nx, double ny) {
        Rectangle2D m = mapRect();
        return new Point2D.Double(m.getX() + nx * m.getWidth(), m.getY() + ny * m.getHeight());
    }

    private Rectangle2D roomRect(Room r) {
        Point2D.Double p = toPx(r.x(), r.y());
        Rectangle2D m = mapRect();
        return new Rectangle2D.Double(p.x, p.y, r.w() * m.getWidth(), r.h() * m.getHeight());
    }

    private Room roomAt(int px, int py) {
        for (Room r : rooms) {
            Rectangle2D rr = roomRect(r);
            // Grow hit area 6px so fingers don't miss
            rr.setRect(rr.getX() - 6, rr.getY() - 6, rr.getWidth() + 12, rr.getHeight() + 12);
            if (rr.contains(px, py)) return r;
        }
        return null;
    }

    // ---------------- painting ----------------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Rectangle2D m = mapRect();
        RoundRectangle2D outline = new RoundRectangle2D.Double(
                m.getX(), m.getY(), m.getWidth(), m.getHeight(), 60, 60);

        // rounded white card with a hairline border (matches the rest of the UI)
        RoundRectangle2D card = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 32, 32);
        g.setColor(Color.WHITE);
        g.fill(card);
        g.setColor(new Color(226, 232, 240));
        g.setStroke(new BasicStroke(1f));
        g.draw(card);

        g.setColor(Color.WHITE);
        g.fill(outline);

        // hallway band + label
        g.setColor(new Color(238, 243, 248));
        double bandTop = toPx(0, HALL_Y - 0.1).y;
        g.fill(new Rectangle2D.Double(m.getX() + 2, bandTop, m.getWidth() - 4, 0.2 * m.getHeight()));
        g.setColor(new Color(148, 163, 184));
        g.setFont(new Font("SansSerif", Font.ITALIC, 13));
        Point2D.Double lp = toPx(0.76, 0.64);
        g.drawString("Hallway", (float) lp.x, (float) lp.y);

        g.setColor(WALL);
        g.setStroke(new BasicStroke(2f));
        g.draw(outline);

        drawRooms(g);
        drawDestination(g);
        drawRobot(g);
        g.dispose();
    }

    private void drawRooms(Graphics2D g) {
        for (Room r : rooms) {
            Rectangle2D rr = roomRect(r);
            boolean sel = r == selected;
            boolean elevators = r.name().equals("Elevators");

            g.setColor(sel ? new Color(204, 240, 235) : elevators ? ELEVATOR : ROOM);
            g.fill(rr);
            g.setColor(sel ? TEAL : WALL);
            g.setStroke(new BasicStroke(sel ? 3f : 1.5f));
            g.draw(rr);

            if (elevators) { // three elevator shafts
                double third = rr.getWidth() / 3;
                for (int i = 1; i < 3; i++) {
                    double x = rr.getX() + i * third;
                    g.draw(new Line2D.Double(x, rr.getY(), x, rr.getMaxY()));
                }
            }

            // label, shrunk if it doesn't fit
            g.setColor(TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(r.name()) > rr.getWidth() - 6) {
                g.setFont(new Font("SansSerif", Font.BOLD, 11));
                fm = g.getFontMetrics();
            }
            g.drawString(r.name(),
                    (float) (rr.getCenterX() - fm.stringWidth(r.name()) / 2.0),
                    (float) (rr.getCenterY() + fm.getAscent() / 2.5));
        }
    }

    /** Marks the chosen destination only. The actual path is left to the chair's sensors and planner. */
    private void drawDestination(Graphics2D g) {
        if (selected == null) return;
        Point2D.Double end = toPx(selected.doorX(), selected.doorY());
        drawPin(g, end.x, end.y, RED);
    }

    private void drawRobot(Graphics2D g) {
        Point2D.Double p = toPx(robotX, robotY);

        // pulsing ground ring at the pin tip
        double pulse = (System.currentTimeMillis() % 1200) / 1200.0;
        double rx = 10 + 14 * pulse, ry = rx * 0.45;
        g.setColor(new Color(37, 99, 235, (int) (110 * (1 - pulse))));
        g.fill(new Ellipse2D.Double(p.x - rx, p.y - ry, rx * 2, ry * 2));

        drawPin(g, p.x, p.y, BLUE);

        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        String label = "You are here";
        // if the label would land on top of a room (e.g. chair at a bottom room's door), draw it above the pin
        Rectangle2D below = new Rectangle2D.Double(p.x - 40, p.y + 6, 80, 16);
        boolean above = false;
        for (Room r : rooms) if (roomRect(r).intersects(below)) above = true;
        g.drawString(label, (float) (p.x - g.getFontMetrics().stringWidth(label) / 2.0),
                (float) (above ? p.y - 38 : p.y + 18));
    }

    /** Teardrop map pin whose tip sits at (x, y). */
    private void drawPin(Graphics2D g, double x, double y, Color c) {
        double r = 10;
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.curveTo(x - r * 1.3, y - r * 1.2, x - r * 1.2, y - r * 3.0, x, y - r * 3.0);
        p.curveTo(x + r * 1.2, y - r * 3.0, x + r * 1.3, y - r * 1.2, x, y);
        g.setColor(c);
        g.fill(p);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.draw(p);
        g.fill(new Ellipse2D.Double(x - 4, y - r * 2 - 4, 8, 8));
    }
}