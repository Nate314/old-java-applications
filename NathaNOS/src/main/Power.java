package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * Restart and Shut Down: every app window closes into the middle of the screen while the desktop fades to black.
 * Shut Down then ends NathaNOS; Restart shows the NathaNOS logo with a spinner, then fades back to the desktop.
 */
final class Power extends JInternalFrame {
   static final int DARKEN = 600;
   static final int OFF = 400;
   static final int LOGO_IN = 400;
   static final int LOGO_HOLD = 1500;
   static final int LOGO_OUT = 300;
   static final int LIGHTEN = 500;
   private static final int TICK_MS = 30;

   private final Main screen;
   private final TaskList tasks;
   private final Image logo = new ImageIcon("assets/NathaNOS.png").getImage();
   private final Timer timer = new Timer(TICK_MS, e -> this.tick());
   private boolean restart;
   private boolean still;
   private long start;
   private int ms;
   private final JComponent face = new JComponent() {
      @Override
      protected void paintComponent(Graphics g) {
         Power.this.paintScreen(g, this.getWidth(), this.getHeight());
      }
   };

   Power(Main screen, TaskList tasks) {
      super("Power", false, false, false);
      this.screen = screen;
      this.tasks = tasks;
      // A borderless internal frame, like the taskbar, so it covers the taskbar and app windows inside the desktop.
      this.setBorder(null);
      ((BasicInternalFrameUI)this.getUI()).setNorthPane(null);
      this.setCursor(screen.getCursor());
      this.add(this.face);
      // See-through, so the desktop shows under the fading black.
      this.setOpaque(false);
      if (this.getContentPane() instanceof JComponent) {
         ((JComponent)this.getContentPane()).setOpaque(false);
      }
      this.setVisible(false);
   }

   /** How long the sequence runs, in milliseconds. */
   static int length(boolean restart) {
      return restart ? DARKEN + OFF + LOGO_IN + LOGO_HOLD + LOGO_OUT + LIGHTEN : DARKEN + OFF;
   }

   /** How black the desktop is (0 to 255) at ms into the sequence. */
   static int shade(int ms, boolean restart, boolean still) {
      int lighten = length(true) - LIGHTEN;
      if (restart && ms >= lighten) {
         return 255 - ramp(ms - lighten, LIGHTEN, still);
      }
      return ramp(ms, DARKEN, still);
   }

   /** How much of the logo shows (0 to 255) at ms into the sequence; none for Shut Down. */
   static int logo(int ms, boolean restart, boolean still) {
      int in = DARKEN + OFF;
      int out = in + LOGO_IN + LOGO_HOLD;
      if (!restart || ms < in || ms >= out + LOGO_OUT) {
         return 0;
      }
      return ms < out ? ramp(ms - in, LOGO_IN, still) : 255 - ramp(ms - out, LOGO_OUT, still);
   }

   /** 0 to 255 over span ms, easing out; with reduced motion it jumps straight to 255. */
   private static int ramp(int ms, int span, boolean still) {
      if (still) {
         return ms >= 0 ? 255 : 0;
      }
      double t = Math.max(0, Math.min(1, ms / (double) span));
      return (int) Math.round(255 * (1 - (1 - t) * (1 - t)));
   }

   /** Starts Restart (restart true) or Shut Down. Does nothing while a sequence is already running. */
   void begin(boolean restart) {
      if (this.timer.isRunning()) {
         return;
      }
      this.restart = restart;
      this.still = Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
      this.screen.startMenu.hideMenu();
      this.screen.desktopMenu.hideMenu();
      int w = this.screen.desktopPane.getWidth();
      int h = this.screen.desktopPane.getHeight();
      Rectangle middle = new Rectangle(w / 2 - 20, h / 2 - 15, 40, 30);
      for (Rectangle window : this.tasks.closeAll()) {
         if (!this.still) {
            this.screen.zoom.fly(window, middle, null);
         }
      }
      this.setBounds(0, 0, w, h);
      this.start = System.currentTimeMillis();
      this.ms = 0;
      this.setVisible(true);
      this.moveToFront();
      this.timer.start();
      this.face.repaint();
   }

   private void tick() {
      this.ms = (int) (System.currentTimeMillis() - this.start);
      if (this.ms < length(this.restart)) {
         this.face.repaint();
      } else if (!this.restart) {
         System.exit(0);
      } else if (this.ms < length(true) + 2 * TICK_MS) {
         // Painted clear once more before hiding: in the browser a hidden component shows its last drawing again.
         this.face.repaint();
      } else {
         this.timer.stop();
         this.setVisible(false);
      }
   }

   private void paintScreen(Graphics g, int w, int h) {
      AppIcon.smooth(g);
      int shade = shade(this.ms, this.restart, this.still);
      g.setColor(new Color(0, 0, 0, shade));
      g.fillRect(0, 0, w, h);
      int logo = logo(this.ms, this.restart, this.still);
      boolean booting = this.ms >= DARKEN + OFF;
      int size = Math.min(320, Math.min(w, h) / 2);
      int top = h / 2 - size / 2 - 30;
      if (logo > 0) {
         // The logo's own background is black, so a black veil over it fades it in and out.
         g.drawImage(this.logo, (w - size) / 2, top, size, size, null);
         g.setColor(new Color(0, 0, 0, 255 - logo));
         g.fillRect((w - size) / 2, top, size, size);
      }
      int words = booting ? logo : shade;
      if (words == 0) {
         return;
      }
      String text = booting ? "Starting NathaNOS" : this.restart ? "Restarting" : "Shutting down";
      int y = booting ? top + size + 28 : h / 2;
      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
      FontMetrics metrics = g.getFontMetrics();
      g.setColor(new Color(235, 241, 250, words));
      g.drawString(text, (w - metrics.stringWidth(text)) / 2, y);
      this.spinner(g, w / 2, y + 34, words);
   }

   /** Six dots chasing each other around a ring, the brightest one leading. */
   private void spinner(Graphics g, int cx, int cy, int alpha) {
      int dots = 6;
      double lead = this.ms / 140.0;
      for (int i = 0; i < dots; i++) {
         double angle = (Math.floor(lead) - i) * 2 * Math.PI / 8;
         int x = (int) Math.round(cx + 12 * Math.cos(angle));
         int y = (int) Math.round(cy + 12 * Math.sin(angle));
         g.setColor(new Color(150, 205, 255, alpha * (dots - i) / dots));
         g.fillOval(x - 3, y - 3, 6, 6);
      }
   }
}
