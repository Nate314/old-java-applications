package test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.Timer;

/**
 * A Windows key on the screen that works like the one on a keyboard: it goes down when the mouse button does and
 * comes up when it does, so a click opens Start and holding it while pressing a key on the keyboard (E, D, R, an
 * arrow) makes Win+that key. Sliding off it while held, left, right, up or down, makes Win+that arrow when it is let go, so a
 * window can be snapped with one finger and no keyboard. Space or Enter, when it has the keyboard focus, taps it.
 */
final class WindowsKey extends JButton {
   /** Where the presses go: java.awt.Robot in the app, a list in the self-check. */
   interface Keys {
      void press(int keyCode);

      void release(int keyCode);
   }

   static final int WIN = KeyEvent.VK_WINDOWS;
   static final Color ACCENT_TEXT = new Color(110, 186, 255);
   static final Color ACCENT_SIDE = new Color(0, 78, 150);
   static final Color LIT = new Color(96, 182, 255);
   static final Color FACE = new Color(66, 70, 82);
   private static final Color FACE_HOVER = new Color(82, 87, 102);
   static final Color SIDE = new Color(36, 38, 46);
   private static final Color TEXT = new Color(236, 239, 244);
   static final Color BACK = new Color(28, 30, 36);
   private static final int MARGIN = 8;
   /** How far the pointer must slide from where it pressed the key before the release makes Win+an arrow. */
   static final int SLIDE = 28;
   /** What letting go after a slide left, up, right or down does, shown on the key while it is held there. */
   static final String[] SLIDE_NAMES = {"Snap left", "Fill screen", "Snap right", "Shrink"};

   private final Keys keys;
   /** Hover to press, when it is on: resting on the key taps it. */
   private final Dwell dwell;
   private final BooleanSupplier dwellOn;
   private boolean down;
   private boolean hover;
   private int pressX;
   private int pressY;
   /** The arrow the pointer has slid toward while the key is held (KeyEvent.VK_LEFT and so on), or 0. */
   private int slide;
   /** A chord the shelf just made ("Win+E"), shown on the key for a moment as if it went down for it; null for none. */
   private String echo;
   /** The arrow of an echoed snap (KeyEvent.VK_LEFT and so on), drawn as where the window went; 0 for other chords. */
   private int echoArrow;
   private final Timer echoEnd = new Timer(ECHO_MS, e -> this.echo(null));
   /** Told when the key goes down or comes up, so the shelf can light the keys that go with it. */
   Runnable heldChanged = () -> { };
   static final int ECHO_MS = 700;
   /**
    * The slide lesson: until the user has snapped a window by sliding the key, a ghost fingertip shows it once each
    * start, pressing the key and sliding right, then left, with the key drawn held and slid as it would be. It
    * presses nothing, and the pointer coming onto the key ends it.
    */
   static final int LESSON_WAIT = 1200;
   static final int LESSON_LOOP = 3000;
   static final int[] LESSON_ARROWS = {KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT};
   static final String LESSON_CAPTION = "Slide to snap";
   /** When the lesson's first slide begins (System.currentTimeMillis), or -1 for no lesson. */
   private long lessonStart = -1;
   private final Timer lessonTick = new Timer(40, e -> this.lessonFrame());
   /** Told when a real slide makes Win+an arrow, so the lesson need not be shown again. */
   Runnable slid = () -> { };

   WindowsKey(Keys keys) {
      this(keys, () -> false);
   }

   WindowsKey(Keys keys, BooleanSupplier dwellOn) {
      this.keys = keys;
      this.dwellOn = dwellOn;
      this.dwell = new Dwell(this, dwellOn, i -> this.tap());
      this.echoEnd.setRepeats(false);
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.setToolTipText("Click for Start. Hold it and press a key for Win+that key, or slide off it for Win+that arrow.");
      this.getAccessibleContext().setAccessibleName("Windows key: click for Start, or hold it and press a key for Win+that key, or slide off it for Win+that arrow");
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               WindowsKey.this.pressX = e.getX();
               WindowsKey.this.pressY = e.getY();
               WindowsKey.this.slide = 0;
               WindowsKey.this.dwell.clicked();
               WindowsKey.this.endLesson();
               WindowsKey.this.setDown(true);
            }
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               WindowsKey.this.slideTo(e.getX(), e.getY());
               int arrow = WindowsKey.this.slide;
               if (arrow != 0 && WindowsKey.this.down) {
                  WindowsKey.this.keys.press(arrow);
                  WindowsKey.this.keys.release(arrow);
                  WindowsKey.this.slid.run();
               }
               WindowsKey.this.slide = 0;
               WindowsKey.this.setDown(false);
            }
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            WindowsKey.this.slideTo(e.getX(), e.getY());
         }

         @Override
         public void mouseEntered(MouseEvent e) {
            WindowsKey.this.hover = true;
            WindowsKey.this.endLesson();
            WindowsKey.this.dwell.at(0);
            WindowsKey.this.repaint();
         }

         @Override
         public void mouseExited(MouseEvent e) {
            WindowsKey.this.hover = false;
            WindowsKey.this.dwell.at(-1);
            WindowsKey.this.repaint();
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
               WindowsKey.this.tap();
            }
         }
      });
   }

   /** Down presses the Windows key, up lets go of it; a second down (or up) in a row does nothing. */
   void setDown(boolean down) {
      if (this.down == down) {
         return;
      }
      this.down = down;
      if (down) {
         this.keys.press(WIN);
      } else {
         this.keys.release(WIN);
      }
      this.heldChanged.run();
      this.repaint();
   }

   /** Shows a chord made elsewhere (the shelf) on the key for a moment: lit, pushed down, named under the logo. */
   void echo(String chord) {
      this.echo(chord, 0);
   }

   /** As {@link #echo(String)}, for a snap: the key shows where that arrow sent the window. */
   void echo(String chord, int arrow) {
      this.echo = chord;
      this.echoArrow = chord == null ? 0 : arrow;
      if (chord != null) {
         this.endLesson();
         this.echoEnd.restart();
      }
      this.repaint();
   }

   String echoed() {
      return this.echo;
   }

   /** Which arrow a slide to (x, y) from the press makes: the longer way it went, once past SLIDE; back near the key, none. */
   void slideTo(int x, int y) {
      if (!this.down) {
         return;
      }
      int dx = x - this.pressX;
      int dy = y - this.pressY;
      int arrow = Math.max(Math.abs(dx), Math.abs(dy)) < SLIDE ? 0
         : Math.abs(dx) >= Math.abs(dy) ? (dx < 0 ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT)
         : (dy < 0 ? KeyEvent.VK_UP : KeyEvent.VK_DOWN);
      if (arrow != this.slide) {
         this.slide = arrow;
         this.repaint();
      }
   }

   Dwell dwell() {
      return this.dwell;
   }

   /**
    * A little screen (w by h) with the window where Win+that arrow puts it: the left or right half, the whole screen,
    * or down to a sliver at the bottom; a dim outline marks the middle it left. Drawn on the shelf's arrows and on
    * the key while a slide or an arrow's chord shows, so a snap reads as a picture, not only an arrow.
    */
   static void drawSnap(Graphics2D g, int arrow, int x, int y, int w, int h, Color edge, Color window) {
      int ix = x + 2;
      int iy = y + 2;
      int iw = w - 4;
      int ih = h - 4;
      g.setStroke(new BasicStroke(1f));
      g.setColor(edge);
      g.drawRoundRect(x, y, w - 1, h - 1, 3, 3);
      // The stand, so it reads as a screen.
      g.fillRect(x + w / 2 - w / 6, y + h + 1, Math.max(2, w / 3), 1);
      g.setColor(ACCENT_SIDE);
      g.drawRect(ix + iw / 4, iy + ih / 4, iw / 2 - 1, ih / 2 - 1);
      g.setColor(window);
      int half = (iw + 1) / 2;
      if (arrow == KeyEvent.VK_LEFT) {
         g.fillRect(ix, iy, half, ih);
      } else if (arrow == KeyEvent.VK_RIGHT) {
         g.fillRect(ix + iw - half, iy, half, ih);
      } else if (arrow == KeyEvent.VK_UP) {
         g.fillRect(ix, iy, iw, ih);
      } else if (arrow == KeyEvent.VK_DOWN) {
         g.fillRect(ix + iw / 3, iy + ih - Math.max(2, ih / 5), Math.max(2, iw / 3), Math.max(2, ih / 5));
      }
   }

   /** a and b mixed, t of the way from a to b. */
   static Color mix(Color a, Color b, double t) {
      return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
         (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t), (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
   }

   static Color alpha(Color c, int a) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
   }

   /**
    * A backlit keycap, w by h at (x, y): a skirt depth px deep under a face lit from above, with a shallow dish in it
    * and a bright top edge; sink pushes the face down into the skirt. Light from under the cap (glow 0 to 1) spills
    * round the skirt in the accent, faint at rest and bright while it is pressed. The key and the shelf's caps share
    * it, so the window reads as one piece of a backlit keyboard.
    */
   static void drawCap(Graphics2D g, int x, int y, int w, int h, int arc, int depth, Color face, Color side, int sink, double glow) {
      int step = Math.max(1, h / 40);
      for (int i = 3; i >= 1 && glow > 0; i--) {
         int out = i * step;
         g.setColor(alpha(LIT, (int) Math.round(glow * (75 - 20 * i))));
         g.fillRoundRect(x - out, y + depth - out, w + 2 * out, h - depth + 2 * out, arc + 2 * out, arc + 2 * out);
      }
      g.setPaint(new GradientPaint(0, y + depth, mix(side, Color.WHITE, 0.12f), 0, y + h, mix(side, Color.BLACK, 0.3f)));
      g.fillRoundRect(x, y + depth, w, h - depth, arc, arc);
      int top = y + sink;
      int fh = h - depth - 1;
      g.setPaint(new GradientPaint(0, top, mix(face, Color.WHITE, 0.16f), 0, top + fh, mix(face, Color.BLACK, 0.12f)));
      g.fillRoundRect(x, top, w, fh, arc, arc);
      // The dish: darker at its top edge, as if the face curves in.
      int d = Math.max(2, w / 14);
      g.setPaint(new GradientPaint(0, top + d, mix(face, Color.BLACK, 0.14f), 0, top + fh - d, mix(face, Color.WHITE, 0.06f)));
      g.fillRoundRect(x + d, top + d, w - 2 * d, fh - 2 * d, Math.max(2, arc - d), Math.max(2, arc - d));
      g.setColor(alpha(Color.WHITE, 60));
      g.drawLine(x + arc / 2, top + 1, x + w - 1 - arc / 2, top + 1);
   }

   int echoArrow() {
      return this.echoArrow;
   }

   int slide() {
      return this.slide;
   }

   boolean isDown() {
      return this.down;
   }

   /** Starts the slide lesson in that many ms (a moment, so Start is seen first), unless motion is to be kept down. */
   void teach(long in) {
      if (Boolean.TRUE.equals(java.awt.Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"))) {
         return;
      }
      this.lessonStart = System.currentTimeMillis() + in;
      this.lessonTick.start();
   }

   void endLesson() {
      if (this.lessonStart >= 0) {
         this.lessonStart = -1;
         this.lessonTick.stop();
         this.repaint();
      }
   }

   boolean teaching() {
      return this.lessonStart >= 0;
   }

   private void lessonFrame() {
      long t = System.currentTimeMillis() - this.lessonStart;
      if (t >= LESSON_ARROWS.length * LESSON_LOOP) {
         this.endLesson();
      } else if (t >= 0) {
         this.repaint();
      }
   }

   /**
    * The lesson t ms into its slides: {held (0 or 1), the arrow picked (or 0), the fingertip's slide in px from
    * where it pressed, its opacity 0 to 255}; null before it starts and after it ends. Each loop fades the fingertip
    * in, presses, eases it SLIDE + 12 px one way (picking the arrow past SLIDE, as a real slide does), holds, lets
    * go and fades out, then rests.
    */
   static int[] lesson(long t) {
      if (t < 0 || t >= LESSON_ARROWS.length * LESSON_LOOP) {
         return null;
      }
      int u = (int) (t % LESSON_LOOP);
      int dir = LESSON_ARROWS[(int) (t / LESSON_LOOP)] == KeyEvent.VK_LEFT ? -1 : 1;
      double along = Math.max(0, Math.min(1, (u - 700) / 800.0));
      int dx = (int) Math.round(dir * (SLIDE + 12) * (along * along * (3 - 2 * along)));
      boolean held = u >= 400 && u < 2100;
      int arrow = held && Math.abs(dx) >= SLIDE ? LESSON_ARROWS[(int) (t / LESSON_LOOP)] : 0;
      int alpha = u < 400 ? u * 255 / 400 : u < 2100 ? 255 : u < 2400 ? (2400 - u) * 255 / 300 : 0;
      return new int[] {held ? 1 : 0, arrow, dx, alpha};
   }

   /** The lesson as it is now, or null. */
   private int[] lessonNow() {
      return this.lessonStart < 0 ? null : lesson(System.currentTimeMillis() - this.lessonStart);
   }

   void tap() {
      this.endLesson();
      this.setDown(true);
      this.setDown(false);
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(BACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      // Drawn at the small size and scaled, so a bigger key is the same key, bigger.
      double s = this.getWidth() / (double) Test.WIDTH;
      g.scale(s, s);
      g.translate(MARGIN, MARGIN);
      int w = (int) Math.round(this.getWidth() / s) - 2 * MARGIN;
      int h = (int) Math.round(this.getHeight() / s) - 2 * MARGIN;
      // The lesson's fingertip holds and slides the key as a real one would, but only in the drawing.
      int[] ghost = this.down ? null : this.lessonNow();
      boolean held = this.down || ghost != null && ghost[0] == 1;
      int slide = this.down ? this.slide : ghost != null ? ghost[1] : 0;
      // A chord from the shelf draws the key down too, so it reads as this key plus that one.
      boolean lit = held || this.echo != null;
      int sink = lit ? 4 : 0;
      drawCap(g, 0, 0, w, h, 16, 5, lit ? LIT : this.hover ? FACE_HOVER : FACE, lit ? ACCENT_SIDE : SIDE, sink, lit ? 1 : this.hover ? 0.6 : 0.35);
      if (this.isFocusOwner()) {
         g.setColor(TEXT);
         g.setStroke(new BasicStroke(1f));
         g.drawRoundRect(6, sink + 6, w - 13, h - 19, 10, 10);
      }
      int size = Math.min(w, h) / 3;
      int x = (w - size) / 2;
      int y = (h - 6) / 2 - size / 2 - 8 + sink;
      int half = (size - 3) / 2;
      // Slid (or echoing a snap from the shelf), the logo gives way to the screen that snap makes.
      int snap = held ? slide : this.echoArrow;
      if (this.dwellOn.getAsBoolean()) {
         // Hover to press: a ring round the logo, filling clockwise while the pointer rests on the key.
         int ring = size + 14;
         int rx = x + size / 2 - ring / 2;
         int ry = y + size / 2 - ring / 2;
         g.setStroke(new BasicStroke(3f));
         g.setColor(lit ? ACCENT_SIDE : SIDE);
         g.drawOval(rx, ry, ring, ring);
         int sweep = (int) Math.round(360 * this.dwell.progress(0));
         if (sweep > 0) {
            g.setColor(LIT);
            g.drawArc(rx, ry, ring, ring, 90, -sweep);
         }
      }
      if (snap != 0) {
         int sw = size + 12;
         drawSnap(g, snap, x + size / 2 - sw / 2, y + 2, sw, size - 4, Color.WHITE, Color.WHITE);
      } else {
         // A backlit legend: each pane over a soft halo of its own light.
         Color legend = lit ? Color.WHITE : ACCENT_TEXT;
         for (int o = 3; o >= 0; o--) {
            g.setColor(o == 0 ? legend : alpha(legend, 28));
            for (int p = 0; p < 4; p++) {
               g.fillRoundRect(x + p % 2 * (half + 3) - o, y + p / 2 * (half + 3) - o, half + 2 * o, half + 2 * o, 2 * o, 2 * o);
            }
         }
      }
      if (held) {
         // Held, the key shows the four ways it can slide, the one picked lit, so the slide is found by holding it.
         int cx = x + size / 2;
         int cy = y + size / 2;
         int reach = size / 2 + 12;
         for (int a = 0; a < 4; a++) {
            int dx = a == 0 ? -1 : a == 2 ? 1 : 0;
            int dy = a == 1 ? -1 : a == 3 ? 1 : 0;
            int tx = cx + dx * reach;
            int ty = cy + dy * (reach - 4);
            g.setColor(slide == KeyEvent.VK_LEFT + a ? Color.WHITE : ACCENT_SIDE);
            // A small triangle pointing that way.
            g.fillPolygon(new int[] {tx + 5 * dx, tx - 3 * dx + 4 * dy, tx - 3 * dx - 4 * dy},
               new int[] {ty + 5 * dy, ty - 3 * dy + 4 * dx, ty - 3 * dy - 4 * dx}, 3);
         }
      }
      String caption = !held ? (this.echo != null ? this.echo : ghost != null ? LESSON_CAPTION : "Start")
         : slide == 0 ? "+ a key" : SLIDE_NAMES[slide - KeyEvent.VK_LEFT];
      g.setColor(lit ? Color.WHITE : TEXT);
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      g.drawString(caption, (w - g.getFontMetrics().stringWidth(caption)) / 2, y + size + 22);
      if (ghost != null && ghost[3] > 0) {
         // The fingertip: a soft disc with a ring, pressed flatter (and rippling) while it holds the key.
         int fx = x + size / 2 + ghost[2];
         int fy = y + size / 2 + 6;
         int r = held ? 9 : 11;
         // Its shadow, nearer while it presses, so it reads over the lit blue cap too.
         g.setColor(alpha(Color.BLACK, ghost[3] / 3));
         g.fillOval(fx - r + 1, fy - r + (held ? 1 : 3), 2 * r, 2 * r);
         g.setColor(alpha(Color.WHITE, ghost[3] * 2 / 5));
         g.fillOval(fx - r, fy - r, 2 * r, 2 * r);
         g.setStroke(new BasicStroke(2f));
         g.setColor(alpha(Color.WHITE, ghost[3] * 4 / 5));
         g.drawOval(fx - r, fy - r, 2 * r, 2 * r);
         if (held) {
            g.setColor(alpha(LIT, ghost[3] / 3));
            g.drawOval(fx - r - 5, fy - r - 5, 2 * r + 10, 2 * r + 10);
         }
      }
   }
}
