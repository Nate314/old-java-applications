package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The outlines that fly between a window and the place it comes from or goes to: the icon that opened it, or its
 * taskbar button when it is minimized or restored. Drawn over the whole desktop, and hidden while nothing flies.
 */
final class Zoom extends JComponent {
   static final int STEPS = 10;
   private static final int TICK_MS = 18;
   private static final int TRAIL = 3;
   /** Ticks to stay up, empty, after the last flight lands, so the cleared drawing is painted before it hides. */
   private static final int LINGER = 4;
   private static final Color LINE = new Color(150, 205, 255);
   private static final Color EDGE = new Color(10, 20, 40);

   private static final class Flight {
      final Rectangle from;
      final Rectangle to;
      final Runnable done;
      int step;

      Flight(Rectangle from, Rectangle to, Runnable done) {
         this.from = from;
         this.to = to;
         this.done = done;
      }
   }

   private final List<Flight> flights = new ArrayList<>();
   private final Timer timer = new Timer(TICK_MS, e -> this.tick());
   private int idle;

   Zoom() {
      this.setVisible(false);
   }

   /** Flies outlines from one rectangle to the other (desktop coordinates), then runs done (which may be null). */
   void fly(Rectangle from, Rectangle to, Runnable done) {
      this.flights.add(new Flight(from, to, done));
      this.idle = 0;
      this.setBounds(0, 0, this.getParent().getWidth(), this.getParent().getHeight());
      this.setVisible(true);
      this.timer.start();
      this.repaint();
   }

   private void tick() {
      List<Flight> landed = new ArrayList<>();
      for (Flight flight : this.flights) {
         if (++flight.step > STEPS + TRAIL) {
            landed.add(flight);
         }
      }
      this.flights.removeAll(landed);
      // In the browser a hidden component keeps its last drawing and shows it again when shown, so it is cleared first.
      if (this.flights.isEmpty() && ++this.idle > LINGER) {
         this.timer.stop();
         this.setVisible(false);
      }
      this.repaint();
      for (Flight flight : landed) {
         if (flight.done != null) {
            flight.done.run();
         }
      }
   }

   /** Where the outline is at step (0 to STEPS), easing out so it slows as it lands. */
   static Rectangle at(Rectangle from, Rectangle to, int step) {
      double t = Math.max(0, Math.min(STEPS, step)) / (double) STEPS;
      t = 1 - (1 - t) * (1 - t);
      return new Rectangle(
         (int) Math.round(from.x + (to.x - from.x) * t), (int) Math.round(from.y + (to.y - from.y) * t),
         (int) Math.round(from.width + (to.width - from.width) * t), (int) Math.round(from.height + (to.height - from.height) * t));
   }

   /** A component's bounds in the coordinates of one of its ancestors (the desktop). */
   static Rectangle on(Component c, Component ancestor) {
      Rectangle r = new Rectangle(0, 0, c.getWidth(), c.getHeight());
      for (Component p = c; p != null && p != ancestor; p = p.getParent()) {
         r.x += p.getX();
         r.y += p.getY();
      }
      return r;
   }

   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      AppIcon.smooth(g2);
      BasicStroke thin = new BasicStroke(2f);
      BasicStroke wide = new BasicStroke(4f);
      for (Flight flight : this.flights) {
         // The outline in front, and fainter ones a step or two behind it, like a classic desktop's zoom.
         for (int behind = TRAIL - 1; behind >= 0; behind--) {
            int step = flight.step - behind;
            if (step < 0 || step > STEPS) {
               continue;
            }
            Rectangle r = at(flight.from, flight.to, step);
            int alpha = 230 - 70 * behind;
            g2.setColor(new Color(LINE.getRed(), LINE.getGreen(), LINE.getBlue(), alpha / 5));
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            // A dark edge under the light line keeps it readable on any wallpaper.
            g2.setStroke(wide);
            g2.setColor(new Color(EDGE.getRed(), EDGE.getGreen(), EDGE.getBlue(), alpha / 2));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setStroke(thin);
            g2.setColor(new Color(LINE.getRed(), LINE.getGreen(), LINE.getBlue(), alpha));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
         }
      }
   }
}
