package groupcreator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.Timer;

/**
 * The name wheel: one slice per name, a pointer at the top, and a spin that slows down over a few seconds and stops
 * on a name chosen before it starts. Pegs click past the pointer as it turns, and the pointer flicks at each one.
 */
final class PickWheel {
   static final int SPIN_MS = 4200;
   private static final int TURNS = 5;
   private static final double TOP = -Math.PI / 2;
   private static final Color RIM = new Color(52, 56, 66);
   private static final Font HUB = new Font("SansSerif", Font.BOLD, 13);

   private final WheelSound sound = new WheelSound();
   private final Timer timer;
   private final Random random = new Random();
   private final Color[] colors;

   /** The names on the wheel while it spins and after it lands; null shows the names given to paint. */
   private List<String> slices;
   private String winner;
   private double rotation;
   private double from;
   private double to;
   private long spinStart;
   private int lastUnder = -1;
   private long lastClick;
   private boolean landed;

   private int centerX;
   private int centerY;
   private int radius;

   PickWheel(Runnable repaint, Color[] colors) {
      this.colors = colors;
      timer = new Timer(16, event -> {
         step();
         repaint.run();
      });
   }

   /** Spins the wheel of {@code pool} so it stops on {@code name}, a few turns from where it is now. */
   void spin(List<String> pool, String name) {
      slices = new ArrayList<>(pool);
      winner = name;
      landed = false;
      double slice = 2 * Math.PI / slices.size();
      int index = slices.indexOf(name);
      // Somewhere inside the slice, away from its pegs, under the pointer.
      double target = TOP - (index + 0.15 + 0.7 * random.nextDouble()) * slice;
      from = rotation;
      double ahead = (target - from) % (2 * Math.PI);
      if (ahead < 0) {
         ahead += 2 * Math.PI;
      }
      to = from + ahead + TURNS * 2 * Math.PI;
      spinStart = System.currentTimeMillis();
      lastUnder = under(from);
      timer.start();
   }

   boolean spinning() {
      return timer.isRunning();
   }

   /** The name the wheel stopped on, once it has stopped; null before. */
   String landedOn() {
      return landed ? winner : null;
   }

   /** The name under the pointer while the wheel spins; null when it is not spinning. */
   String underPointer() {
      return spinning() ? slices.get(under(rotation)) : null;
   }

   /** The color of the slice the wheel stopped on. */
   Color landedColor() {
      return landed ? color(slices.indexOf(winner), slices.size()) : RIM;
   }

   /** Slice colors go round the card colors; the last slice skips the first slice's color, its neighbor. */
   private Color color(int i, int n) {
      if (i == n - 1 && n > 1 && i % colors.length == 0) {
         return colors[(i + 1) % colors.length];
      }
      return colors[i % colors.length];
   }

   /** Lets the wheel show the class again (after a put back, a new time around or a change of names). */
   void reset() {
      timer.stop();
      slices = null;
      winner = null;
      landed = false;
   }

   /** Stops the spin where it was going to stop. */
   void finish() {
      if (spinning()) {
         spinStart = System.currentTimeMillis() - SPIN_MS;
         step();
      }
   }

   boolean contains(int x, int y) {
      long dx = x - centerX;
      long dy = y - centerY;
      return radius > 0 && dx * dx + dy * dy <= (long) radius * radius;
   }

   private void step() {
      double t = Math.min(1, (System.currentTimeMillis() - spinStart) / (double) SPIN_MS);
      // A quick start that slows smoothly to a stop: the last turns crawl, so the pegs click slower and slower.
      double ease = 1 - Math.pow(1 - t, 4);
      rotation = from + (to - from) * ease;
      int now = under(rotation);
      if (now != lastUnder) {
         lastUnder = now;
         lastClick = System.currentTimeMillis();
         sound.click();
      }
      if (t >= 1) {
         timer.stop();
         rotation = to % (2 * Math.PI);
         landed = true;
         sound.chime();
      }
   }

   /** The slice under the pointer at this rotation. */
   private int under(double at) {
      int count = slices == null ? 1 : slices.size();
      double slice = 2 * Math.PI / count;
      double offset = (TOP - at) % (2 * Math.PI);
      if (offset < 0) {
         offset += 2 * Math.PI;
      }
      return Math.min(count - 1, (int) (offset / slice));
   }

   /** Paints the wheel of {@code pool} (or of the spin under way) centered in the given box. */
   void paint(Graphics2D g, List<String> pool, int x, int y, int width, int height) {
      List<String> names = slices != null ? slices : pool;
      centerX = x + width / 2;
      centerY = y + height / 2 + 8;
      radius = Math.max(0, Math.min(width, height - 16) / 2 - 6);
      int r = radius;
      g.setColor(new Color(0, 0, 0, 34));
      g.fillOval(centerX - r + 3, centerY - r + 5, 2 * r, 2 * r);
      if (names.isEmpty()) {
         g.setColor(Color.WHITE);
         g.fillOval(centerX - r, centerY - r, 2 * r, 2 * r);
         GroupBoard.centered(g, "Type some names above", GroupBoard.BOLD, GroupBoard.MUTED, centerX, centerY + 5);
         paintRim(g, r);
         return;
      }
      int n = names.size();
      double slice = 2 * Math.PI / n;
      int winnerIndex = landed ? names.indexOf(winner) : -1;
      for (int i = 0; i < n; i++) {
         Color color = color(i, n);
         if (winnerIndex >= 0 && i != winnerIndex) {
            color = GroupBoard.mix(color, GroupBoard.DESK, 0.62f);
         }
         g.setColor(color);
         fillSlice(g, r, rotation + i * slice, slice);
      }
      g.setColor(Color.WHITE);
      g.setStroke(new BasicStroke(n > 40 ? 1f : 2f));
      if (n > 1) {
         for (int i = 0; i < n; i++) {
            double a = rotation + i * slice;
            g.drawLine(centerX, centerY, centerX + (int) Math.round(r * Math.cos(a)), centerY + (int) Math.round(r * Math.sin(a)));
         }
      }
      paintNames(g, names, r, slice, winnerIndex);
      if (winnerIndex >= 0) {
         g.setColor(Color.WHITE);
         g.setStroke(new BasicStroke(4f));
         drawSliceEdge(g, r, rotation + winnerIndex * slice, slice);
      }
      g.setStroke(new BasicStroke(1f));
      paintRim(g, r);
      // The pegs, one at each slice edge.
      if (n > 1 && n <= 60) {
         int peg = r > 120 ? 6 : 4;
         for (int i = 0; i < n; i++) {
            double a = rotation + i * slice;
            int px = centerX + (int) Math.round((r - 7) * Math.cos(a));
            int py = centerY + (int) Math.round((r - 7) * Math.sin(a));
            g.setColor(new Color(250, 246, 236));
            g.fillOval(px - peg / 2, py - peg / 2, peg, peg);
            g.setColor(RIM);
            g.drawOval(px - peg / 2, py - peg / 2, peg, peg);
         }
      }
      paintHub(g, r);
      paintPointer(g, r);
   }

   private void paintNames(Graphics2D g, List<String> names, int r, double slice, int winnerIndex) {
      int n = names.size();
      // The text runs along the middle of each slice, from near the rim inward; its size follows the slice width.
      double across = slice * r * 0.62;
      int size = (int) Math.max(9, Math.min(18, across * 0.62));
      if (n == 1) {
         size = 18;
      }
      Font font = new Font("SansSerif", Font.BOLD, size);
      Font big = new Font("SansSerif", Font.BOLD, Math.min(22, size + 3));
      int room = (int) (r * 0.66) - 14;
      for (int i = 0; i < n; i++) {
         boolean win = i == winnerIndex;
         Font f = win ? big : font;
         Graphics2D t = (Graphics2D) g.create();
         t.translate(centerX, centerY);
         t.rotate(rotation + (i + 0.5) * slice);
         String name = GroupBoard.fit(t, names.get(i), f, room);
         int w = GroupBoard.width(t, name, f);
         Color ink = winnerIndex < 0 || win ? Color.WHITE : new Color(255, 255, 255, 170);
         GroupBoard.text(t, name, f, ink, r - 16 - w, f.getSize() * 35 / 100);
         t.dispose();
      }
   }

   private void paintRim(Graphics2D g, int r) {
      g.setColor(RIM);
      g.setStroke(new BasicStroke(5f));
      g.drawOval(centerX - r, centerY - r, 2 * r, 2 * r);
      g.setStroke(new BasicStroke(1f));
   }

   private void paintHub(Graphics2D g, int r) {
      int hub = Math.max(18, r / 6);
      g.setColor(new Color(0, 0, 0, 40));
      g.fillOval(centerX - hub + 1, centerY - hub + 2, 2 * hub, 2 * hub);
      g.setColor(Color.WHITE);
      g.fillOval(centerX - hub, centerY - hub, 2 * hub, 2 * hub);
      g.setColor(RIM);
      g.setStroke(new BasicStroke(3f));
      g.drawOval(centerX - hub, centerY - hub, 2 * hub, 2 * hub);
      g.setStroke(new BasicStroke(1f));
      if (hub >= 22) {
         GroupBoard.centered(g, spinning() ? "..." : "SPIN", HUB, RIM, centerX, centerY + 5);
      }
   }

   /** A pointer over the top of the rim, knocked aside for a moment by each peg that passes it. */
   private void paintPointer(Graphics2D g, int r) {
      long since = System.currentTimeMillis() - lastClick;
      double flick = spinning() && since < 120 ? -0.45 * (1 - since / 120.0) : 0;
      Graphics2D p = (Graphics2D) g.create();
      p.translate(centerX, centerY - r - 14);
      p.rotate(flick);
      int[] xs = {-13, 13, 0};
      int[] ys = {-4, -4, 30};
      p.setColor(new Color(0, 0, 0, 50));
      p.fillPolygon(new int[] {-11, 15, 2}, new int[] {-1, -1, 33}, 3);
      p.setColor(GroupBoard.ERROR);
      p.fillPolygon(xs, ys, 3);
      p.setColor(RIM);
      p.setStroke(new BasicStroke(2f));
      p.drawPolygon(xs, ys, 3);
      p.setColor(Color.WHITE);
      p.fillOval(-5, -2, 10, 10);
      p.setColor(RIM);
      p.drawOval(-5, -2, 10, 10);
      p.dispose();
   }

   private void fillSlice(Graphics2D g, int r, double start, double sweep) {
      int[][] points = slicePoints(r, start, sweep);
      g.fillPolygon(points[0], points[1], points[0].length);
   }

   private void drawSliceEdge(Graphics2D g, int r, double start, double sweep) {
      int[][] points = slicePoints(r - 2, start, sweep);
      g.drawPolygon(points[0], points[1], points[0].length);
   }

   /** The slice as a polygon: the center, then points along its arc a few degrees apart. */
   private int[][] slicePoints(int r, double start, double sweep) {
      int steps = Math.max(2, (int) Math.ceil(sweep / Math.toRadians(3)));
      boolean whole = sweep >= 2 * Math.PI - 1e-9;
      int count = steps + 1 + (whole ? 0 : 1);
      int[] xs = new int[count];
      int[] ys = new int[count];
      int k = 0;
      if (!whole) {
         xs[k] = centerX;
         ys[k++] = centerY;
      }
      for (int i = 0; i <= steps; i++) {
         double a = start + sweep * i / steps;
         xs[k] = centerX + (int) Math.round(r * Math.cos(a));
         ys[k++] = centerY + (int) Math.round(r * Math.sin(a));
      }
      return new int[][] {xs, ys};
   }
}
