package clock;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;

/**
 * The digital face as a bedside LED clock: red seven-segment figures on a black panel, the unlit segments faintly
 * showing as they do behind real glass, a colon that blinks each second, small seconds with AM and PM lights over
 * them, a row of weekdays with today lit, and the date in small figures.
 */
final class DigitalFace {
   static final Color PANEL = new Color(18, 14, 16);
   static final Color LIT = new Color(255, 52, 40);
   static final Color GHOST = new Color(46, 17, 17);
   /** Segments a (top) to g (middle) as bits 0 to 6, for the figures 0 to 9. */
   private static final int[] FIGURES = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};
   private static final String[] DAYS = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
   /** How far the figures lean, as a fraction of their height. */
   private static final double SLANT = 0.08;

   /** The segments lit for a figure, a dash (the middle one) or a blank. */
   static int segments(char figure) {
      return figure >= '0' && figure <= '9' ? FIGURES[figure - '0'] : figure == '-' ? 0x40 : 0;
   }

   void paint(Graphics2D g, Rectangle area, Main clock, String alarmLine, long now) {
      boolean twentyFour = clock.state.twentyFour;
      int pad = Math.max(6, area.width / 30);
      Rectangle panel = new Rectangle(area.x + pad, area.y + pad, area.width - 2 * pad, area.height - 2 * pad);
      g.setColor(PANEL);
      int arc = Math.max(10, Math.min(panel.width, panel.height) / 8);
      g.fillRoundRect(panel.x, panel.y, panel.width, panel.height, arc, arc);

      // In units of the big figures' height: 4 figures, 2 gaps, the colon, a space, 2 small figures and their gap across;
      // the figures, a space, the weekdays, a space, the date and the alarm line down.
      double h = Math.min(panel.width * 0.9 / 3.72, panel.height * 0.88 / 2.3);
      double w = h * 0.56;
      double gap = h * 0.16;
      double colon = h * 0.34;
      double sh = h * 0.46;
      double sw = sh * 0.56;
      double total = 4 * w + 2 * gap + colon + h * 0.22 + 2 * sw + h * 0.08;
      double x = panel.x + (panel.width - total) / 2;
      double top = panel.y + (panel.height - h * 2.3) / 2 + h * 0.06;

      char[] figures = FlipFace.figures(clock.hours, clock.minutes, clock.seconds, twentyFour);
      double[] lefts = {x, x + w + gap, x + 2 * w + gap + colon, x + 3 * w + 2 * gap + colon};
      for (int i = 0; i < 4; i++) {
         figure(g, figures[i], lefts[i], top, w, h);
      }
      double secondsX = x + 4 * w + 2 * gap + colon + h * 0.22;
      figure(g, figures[4], secondsX, top + h - sh, sw, sh);
      figure(g, figures[5], secondsX + sw + h * 0.08, top + h - sh, sw, sh);

      // The colon is lit for the first half of each second, or steadily when the page asks for less motion.
      boolean still = Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
      g.setColor(still || clock.milliseconds < 500 ? LIT : GHOST);
      int dot = (int)Math.round(h * 0.11);
      double colonX = x + 2 * w + gap + (colon - dot) / 2;
      for (double at : new double[]{0.3, 0.7}) {
         g.fillRect((int)Math.round(colonX + (0.5 - at) * h * SLANT), (int)Math.round(top + h * at - dot / 2.0), dot, dot);
      }

      // Over the seconds, AM and PM lights in 12-hour time.
      double label = h * 0.15;
      if (!twentyFour) {
         Rectangle am = new Rectangle((int)secondsX, (int)(top + h * 0.12), (int)(sw + h * 0.04), (int)label);
         Rectangle pm = new Rectangle((int)(secondsX + sw + h * 0.04), am.y, am.width, am.height);
         light(g, "AM", clock.hours < 12, am);
         light(g, "PM", clock.hours >= 12, pm);
      }

      // The week with today lit, the date, and when the alarm rings.
      double row = top + h + h * 0.24;
      for (int day = 0; day < 7; day++) {
         light(g, DAYS[day], day == clock.dayOfWeek,
               new Rectangle((int)(x + total * day / 7), (int)row, (int)(total / 7 * 0.85), (int)(label * 1.1)));
      }
      String date = Ui.twoDigits(clock.month + 1) + "-" + Ui.twoDigits(clock.dayOfMonth) + "-" + clock.year;
      double dh = h * 0.26;
      double dw = dh * 0.56;
      double dgap = dh * 0.2;
      double dx = x + (total - date.length() * dw - (date.length() - 1) * dgap) / 2;
      for (int i = 0; i < date.length(); i++) {
         figure(g, date.charAt(i), dx + i * (dw + dgap), row + label * 1.1 + h * 0.16, dw, dh);
      }
      if (alarmLine != null) {
         light(g, "ALARM  " + alarmLine.toUpperCase(), true,
               new Rectangle((int)x, (int)(row + label * 1.1 + h * 0.16 + dh + h * 0.12), (int)total, (int)(h * 0.16)));
      }
   }

   /** A label lit or unlit, as the panel's little indicator lights are. */
   private static void light(Graphics2D g, String text, boolean lit, Rectangle box) {
      g.setColor(lit ? LIT : GHOST);
      Ui.center(g, text, Ui.fit(g, Ui.LABELS, text, box.width, box.height, box.height), box);
   }

   /** One figure's seven segments in a {@code w} by {@code h} cell: lit ones bright, the rest faintly. */
   private static void figure(Graphics2D g, char figure, double x, double y, double w, double h) {
      int lit = segments(figure);
      double t = h * 0.12;
      double left = x + t / 2;
      double right = x + w - t / 2;
      double upper = y + t / 2;
      double middle = y + h / 2;
      double lower = y + h - t / 2;
      double[][] lines = {{left, upper, right, upper}, {right, upper, right, middle}, {right, middle, right, lower},
            {left, lower, right, lower}, {left, middle, left, lower}, {left, upper, left, middle}, {left, middle, right, middle}};
      for (int s = 0; s < 7; s++) {
         boolean on = (lit >> s & 1) != 0;
         if (on || figure != '-') { // the date's dashes are printed on the glass, not a whole figure
            g.setColor(on ? LIT : GHOST);
            segment(g, lines[s], t, middle, h);
         }
      }
   }

   /** A segment along a line, pointed at both ends and stopping short of its neighbours, leaning with the figure. */
   private static void segment(Graphics2D g, double[] line, double t, double middle, double h) {
      double length = Math.hypot(line[2] - line[0], line[3] - line[1]);
      double ux = (line[2] - line[0]) / length;
      double uy = (line[3] - line[1]) / length;
      double inset = t * 0.18;
      double half = t / 2;
      double[] along = {inset, inset + half, length - inset - half, length - inset, length - inset - half, inset + half};
      double[] across = {0, half, half, 0, -half, -half};
      int[] xs = new int[6];
      int[] ys = new int[6];
      for (int i = 0; i < 6; i++) {
         double px = line[0] + ux * along[i] - uy * across[i];
         double py = line[1] + uy * along[i] + ux * across[i];
         xs[i] = (int)Math.round(px + (middle - py) * SLANT);
         ys[i] = (int)Math.round(py);
      }
      g.fillPolygon(xs, ys, 6);
   }
}
