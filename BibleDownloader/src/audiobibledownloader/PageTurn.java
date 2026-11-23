package audiobibledownloader;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * A page turning over. On two pages the turned leaf lifts from one side, narrows to its edge over the spine and lands
 * on the other side showing its back; on one page the old page folds away to the left (forward) or the new one
 * unfolds from the left (back). Drawn over the new pages from pictures of the pages before and after, scaled across
 * (the browser has no clipping or image cropping), with shading as the leaf tilts.
 */
final class PageTurn {
   static final int MS = 460;
   /** Pictures are taken at twice the size, so the pages stay sharp on high density screens. */
   static final int SCALE = 2;

   private BufferedImage[] before;
   private BufferedImage[] after;
   private boolean forward;
   private long start;
   /** The turn runs from t0 to t1 (a whole turn 0 to 1; a let-go page from where the finger left it). */
   private double t0;
   private double t1 = 1;
   /** While a finger (or the mouse) holds the page, how far it has turned it; -1 when not held. */
   private double held = -1;
   /** Run once a let-go page has fallen back flat, or null. */
   private Runnable back;
   private final Timer timer;

   PageTurn(JComponent owner) {
      this.timer = new Timer(15, e -> {
         if (!this.running()) {
            this.before = null;
            this.after = null;
            ((Timer) e.getSource()).stop();
            Runnable then = this.back;
            this.back = null;
            if (then != null) {
               then.run();
            }
         }
         owner.repaint();
      });
   }

   /** Starts a turn between two pictures of the whole panel (taken at {@link #SCALE}), cut into the given sheets. */
   void start(boolean forward, BufferedImage before, BufferedImage after, Rectangle[] sheets) {
      this.begin(forward, before, after, sheets);
      this.t0 = 0;
      this.t1 = 1;
      this.timer.start();
   }

   /** Starts a turn that a finger holds: {@link #hold} moves it, {@link #let} lets it go. */
   void grab(boolean forward, BufferedImage before, BufferedImage after, Rectangle[] sheets) {
      this.begin(forward, before, after, sheets);
      this.held = 0;
   }

   private void begin(boolean forward, BufferedImage before, BufferedImage after, Rectangle[] sheets) {
      this.forward = forward;
      this.before = cut(before, sheets);
      this.after = cut(after, sheets);
      this.held = -1;
      this.back = null;
      this.start = System.currentTimeMillis();
   }

   boolean held() {
      return this.held >= 0 && this.before != null;
   }

   /** How far the held page is turned, from 0 (flat where it was) to 1 (over). */
   void hold(double t) {
      this.held = Math.max(0, Math.min(1, t));
   }

   /** Lets the held page go: it finishes the turn, or falls back flat and then runs {@code fallen}. */
   void let(boolean over, Runnable fallen) {
      if (!this.held()) {
         return;
      }
      this.t0 = this.held;
      this.t1 = over ? 1 : 0;
      this.held = -1;
      this.back = over ? null : fallen;
      this.start = System.currentTimeMillis();
      this.timer.start();
   }

   /** Ends a page still falling back at once (its {@code fallen} runs now), so another turn starts from where it lies. */
   void settle() {
      Runnable then = this.back;
      if (then != null) {
         this.back = null;
         this.before = null;
         this.after = null;
         then.run();
      }
   }

   private long duration() {
      return Math.max(1, Math.round(MS * Math.abs(this.t1 - this.t0)));
   }

   boolean running() {
      return this.before != null && (this.held >= 0 || System.currentTimeMillis() - this.start < this.duration());
   }

   /** Draws the turn at this moment over the new pages, if one is running. */
   void paint(Graphics2D g, Rectangle[] sheets) {
      if (this.running() && sheets.length == this.before.length) {
         double t;
         if (this.held >= 0) {
            t = this.held;
         } else {
            double u = (System.currentTimeMillis() - this.start) / (double) this.duration();
            // A whole turn eases in and out; a let-go page only eases out, as it is already moving.
            double eased = this.t0 == 0 && this.t1 == 1 ? (u < 0.5 ? 4 * u * u * u : 1 - Math.pow(-2 * u + 2, 3) / 2) : 1 - Math.pow(1 - u, 3);
            t = this.t0 + (this.t1 - this.t0) * eased;
         }
         paint(g, sheets, this.forward, this.before, this.after, t);
      }
   }

   /** One frame: t runs from 0 (the old pages) to 1 (the new pages, which are already drawn underneath). */
   static void paint(Graphics2D g, Rectangle[] sheets, boolean forward, BufferedImage[] before, BufferedImage[] after, double t) {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      int lift = (int) Math.round(6 * Math.sin(Math.PI * t));
      if (sheets.length == 1) {
         Rectangle s = sheets[0];
         if (!forward) {
            g.drawImage(before[0], s.x, s.y, s.width, s.height, null);
         }
         double k = forward ? 1 - t : t;
         leaf(g, forward ? before[0] : after[0], s, s.x, (int) Math.round(s.width * k), lift, k);
         return;
      }
      // The leaf lands on the "to" side, covering the old page there, which stays in view until it does.
      Rectangle from = sheets[forward ? 1 : 0];
      Rectangle to = sheets[forward ? 0 : 1];
      int spine = sheets[1].x;
      BufferedImage stays = before[forward ? 0 : 1];
      g.drawImage(stays, to.x, to.y, to.width, to.height, null);
      boolean rising = t < 0.5;
      double k = rising ? 1 - 2 * t : 2 * t - 1;
      Rectangle side = rising ? from : to;
      BufferedImage face = rising ? before[forward ? 1 : 0] : after[forward ? 0 : 1];
      int width = (int) Math.round(side.width * k);
      leaf(g, face, side, side.x == spine ? spine : spine - width, width, lift, k);
   }

   /** The turning leaf at x, width wide, with a shadow cast past its free edge and darker the more it tilts. */
   private static void leaf(Graphics2D g, BufferedImage face, Rectangle side, int x, int width, int lift, double k) {
      if (width <= 0) {
         return;
      }
      int free = x == side.x ? x + width : x;
      int away = x == side.x ? 1 : -1;
      int reach = 4 + (int) Math.round(22 * (1 - k));
      for (int i = 0; i < reach; i++) {
         int px = free + away * i;
         if (px >= side.x && px < side.x + side.width) {
            g.setColor(new Color(0, 0, 0, (reach - i) * 70 / reach));
            g.fillRect(px, side.y, 1, side.height);
         }
      }
      g.drawImage(face, x, side.y - lift, width, side.height + 2 * lift, null);
      g.setColor(new Color(0, 0, 0, (int) Math.round(85 * (1 - k))));
      g.fillRect(x, side.y - lift, width, side.height + 2 * lift);
   }

   /** Each sheet's part of a picture of the whole panel. */
   static BufferedImage[] cut(BufferedImage whole, Rectangle[] sheets) {
      BufferedImage[] parts = new BufferedImage[sheets.length];
      for (int i = 0; i < sheets.length; i++) {
         Rectangle s = sheets[i];
         parts[i] = new BufferedImage(s.width * SCALE, s.height * SCALE, BufferedImage.TYPE_INT_RGB);
         Graphics2D g = parts[i].createGraphics();
         g.drawImage(whole, -s.x * SCALE, -s.y * SCALE, null);
         g.dispose();
      }
      return parts;
   }
}
