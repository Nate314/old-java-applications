package clock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/** The Stopwatch tab: a stopwatch dial racing each lap against the best, and laps as bars with the fastest and slowest marked. */
final class StopwatchView implements Tab {
   private static final long MINUTE = 60_000;
   /** The lap band before there is a best lap to race. */
   private static final Color UNTIMED = new Color(170, 215, 220);
   static final Color AHEAD = new Color(60, 175, 90);
   private static final Color SUBDIAL = new Color(232, 236, 236);
   /** Lap bars behind the times: the fastest, the slowest, the rest. */
   private static final Color[] BARS = {new Color(195, 235, 205), new Color(250, 205, 205), new Color(220, 226, 226)};
   private final Main clock;
   private Rectangle[] buttons;

   StopwatchView(Main clock) {
      this.clock = clock;
   }

   private Stopwatch stopwatch() {
      return this.clock.state.stopwatch;
   }

   @Override
   public String label(long now) {
      Stopwatch watch = this.stopwatch();
      return watch.isRunning() ? "Stopwatch " + Ui.minutesSeconds(watch.elapsed(now) / 1000) : "Stopwatch";
   }

   @Override
   public String hint() {
      return "Space: start/stop   L: lap   R: reset";
   }

   @Override
   public void paint(Graphics2D g, Rectangle area, long now) {
      Stopwatch watch = this.stopwatch();
      int gap = Math.max(4, area.height / 50);
      int buttonHeight = Math.max(24, Math.min(44, area.height / 9));
      Rectangle buttonRow = new Rectangle(area.x, area.y + area.height - buttonHeight, area.width, buttonHeight);
      int ringSize = Math.min(area.width, (area.height - buttonHeight - gap) * 7 / 10);
      Rectangle ring = new Rectangle(area.x + (area.width - ringSize) / 2, area.y, ringSize, ringSize);
      Rectangle list = new Rectangle(area.x, ring.y + ring.height + gap, area.width, buttonRow.y - gap - ring.y - ring.height - gap);

      paintRing(g, ring, watch, now);

      this.paintLaps(g, list);

      this.buttons = Ui.row(buttonRow, 2, gap);
      Ui.button(g, this.buttons[0], watch.isRunning() ? "Stop" : watch.elapsed(now) > 0 ? "Resume" : "Start", true);
      Ui.button(g, this.buttons[1], watch.isRunning() ? "Lap" : "Reset", false);
   }

   /**
    * A stopwatch dial: the seconds round the rim, a red hand sweeping it once a minute, a small dial counting the minutes
    * (one turn in 30) and the time to the hundredth. Once a lap is done, a band runs round the rim from where the lap
    * under way began: green while it is ahead of the best lap, red for the time it has lost, with a green mark at the
    * point the best lap would have ended.
    */
   static void paintRing(Graphics2D g, Rectangle box, Stopwatch watch, long now) {
      int r = box.width / 2;
      int cx = box.x + r;
      int cy = box.y + r;
      int stroke = Math.max(2, r / 30);
      long elapsed = watch.elapsed(now);
      long lap = watch.currentLap(now);
      int best = watch.bestLap();
      Ui.face(g, cx, cy, r);

      // The lap under way, at most one turn of it.
      double lapStart = (elapsed - lap) % MINUTE / (double) MINUTE;
      long shown = Math.min(lap, MINUTE);
      long ahead = best < 0 ? shown : Math.min(shown, watch.lapTime(best));
      int outer = r * 98 / 100;
      int inner = r * 91 / 100;
      g.setColor(best < 0 ? UNTIMED : AHEAD);
      Ui.band(g, cx, cy, outer, inner, lapStart, ahead / (double) MINUTE);
      g.setColor(Ui.WEDGE);
      Ui.band(g, cx, cy, outer, inner, lapStart + ahead / (double) MINUTE, (shown - ahead) / (double) MINUTE);

      g.setColor(Ui.INK);
      for (int second = 0; second < 60; second++) {
         boolean five = second % 5 == 0;
         g.setStroke(new BasicStroke(five ? stroke : Math.max(1, stroke / 2)));
         spoke(g, cx, cy, second / 60.0, r * (five ? 0.80 : 0.85), r * 0.90);
      }
      int size = Math.max(8, r * 11 / 100);
      Font numbers = new Font(Ui.DIGITS, Font.BOLD, size);
      for (int second = 5; second <= 60; second += 5) {
         double angle = second * Math.PI / 30;
         int x = cx + (int) Math.round(r * 0.69 * Math.sin(angle));
         int y = cy - (int) Math.round(r * 0.69 * Math.cos(angle));
         Ui.center(g, String.valueOf(second), numbers, new Rectangle(x - size, y - size / 2, size * 2, size));
      }
      Ui.rim(g, cx, cy, r, stroke * 2);
      if (best >= 0) {
         paceMark(g, cx, cy, r, lapStart + watch.lapTime(best) % MINUTE / (double) MINUTE);
      }

      // The minutes counter above the middle.
      int sr = r * 19 / 100;
      int sy = cy - r * 36 / 100;
      g.setColor(SUBDIAL);
      g.fillOval(cx - sr, sy - sr, 2 * sr, 2 * sr);
      g.setColor(Ui.INK);
      g.setStroke(new BasicStroke(Math.max(1, stroke / 2)));
      g.drawOval(cx - sr, sy - sr, 2 * sr, 2 * sr);
      for (int minute = 0; minute < 30; minute += 5) {
         spoke(g, cx, sy, minute / 30.0, sr * 0.72, sr);
      }
      Ui.line(g, "min", Ui.LABELS, new Rectangle(cx - sr / 2, sy + sr * 3 / 10, sr, sr / 3));
      g.setColor(Ui.HAND);
      g.setStroke(new BasicStroke(Math.max(1.5f, stroke * 0.8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      spoke(g, cx, sy, elapsed % (30 * MINUTE) / (30.0 * MINUTE), 0, sr * 0.85);

      // The time, in one size whatever its figures, so it does not jump as they change.
      g.setColor(Ui.INK);
      Rectangle time = new Rectangle(cx - r / 2, cy + r * 8 / 100, r, r * 22 / 100);
      String text = Ui.stopwatch(elapsed);
      Ui.center(g, text, Ui.fit(g, Ui.DIGITS, text.length() > 7 ? "0:00:00.00" : "0:00.00", time.width, time.height, time.height), time);
      Rectangle status = new Rectangle(cx - r / 2, cy + r * 33 / 100, r, r * 11 / 100);
      g.setColor(Ui.FAINT);
      Ui.line(g, watch.lapCount() > 0 || watch.isRunning() ? "Lap " + (watch.lapCount() + 1) + "  " + Ui.stopwatch(lap) : "Ready", Ui.LABELS, status);
      if (best >= 0) {
         long pace = watch.pace(now);
         g.setColor(pace <= 0 ? Ui.FASTEST : Ui.SLOWEST);
         Ui.line(g, Ui.seconds(Math.abs(pace)) + (pace <= 0 ? " ahead of best" : " behind best"), Ui.LABELS,
               new Rectangle(status.x, status.y + status.height + r / 50, status.width, status.height));
      }

      // The hand, right across the dial, with a short tail and a cap.
      g.setStroke(new BasicStroke(Math.max(1.5f, r / 60f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      int shadow = Math.max(2, r / 40);
      g.setColor(Ui.SHADOW);
      spoke(g, cx + shadow / 2, cy + shadow, elapsed % MINUTE / (double) MINUTE, -r * 0.2, r * 0.9);
      g.setColor(Ui.HAND);
      spoke(g, cx, cy, elapsed % MINUTE / (double) MINUTE, -r * 0.2, r * 0.9);
      int cap = Math.max(3, r / 22);
      g.fillOval(cx - cap, cy - cap, 2 * cap, 2 * cap);
      Ui.glass(g, cx, cy, r);
   }

   /** A line along the radius at {@code turn} (from 12 o'clock, clockwise), from {@code from} to {@code to} out. */
   private static void spoke(Graphics2D g, int cx, int cy, double turn, double from, double to) {
      double sin = Math.sin(turn * 2 * Math.PI);
      double cos = Math.cos(turn * 2 * Math.PI);
      g.drawLine(cx + (int) Math.round(from * sin), cy - (int) Math.round(from * cos), cx + (int) Math.round(to * sin), cy - (int) Math.round(to * cos));
   }

   /** A green triangle across the rim, pointing in, where the best lap would have ended. */
   private static void paceMark(Graphics2D g, int cx, int cy, int r, double turn) {
      double angle = turn * 2 * Math.PI;
      double spread = Math.PI / 45;
      double[] reach = {r * 0.84, r, r};
      double[] at = {angle, angle - spread, angle + spread};
      int[] xs = new int[3];
      int[] ys = new int[3];
      for (int i = 0; i < 3; i++) {
         xs[i] = cx + (int) Math.round(reach[i] * Math.sin(at[i]));
         ys[i] = cy - (int) Math.round(reach[i] * Math.cos(at[i]));
      }
      g.setColor(Ui.FASTEST);
      g.fillPolygon(xs, ys, 3);
   }

   /** The laps, newest first, as many as fit: number, lap time and total, the fastest green and the slowest red. */
   private void paintLaps(Graphics2D g, Rectangle box) {
      Stopwatch watch = this.stopwatch();
      if (box.height < 12) {
         return;
      }
      int rowHeight = Math.max(14, Math.min(26, box.height / 4));
      Font font = new Font(Ui.LABELS, Font.BOLD, rowHeight * 2 / 3);
      if (watch.lapCount() == 0) {
         g.setColor(Ui.FAINT);
         Ui.center(g, watch.isRunning() ? "Press L or Lap to split" : "Laps show here", font, new Rectangle(box.x, box.y, box.width, rowHeight));
         return;
      }
      int fastest = watch.extremeLap(false);
      int slowest = watch.extremeLap(true);
      long longest = watch.lapTime(Math.max(0, slowest));
      int rows = Math.max(1, box.height / rowHeight);
      int[] columns = {box.x + box.width / 20, box.x + box.width * 3 / 10, box.x + box.width * 3 / 5};
      for (int row = 0; row < rows && row < watch.lapCount(); row++) {
         int lap = watch.lapCount() - 1 - row;
         int y = box.y + row * rowHeight;
         if (row % 2 == 0) {
            g.setColor(Ui.FACE);
            g.fillRoundRect(box.x, y, box.width, rowHeight, rowHeight / 2, rowHeight / 2);
         }
         // A bar as long as the lap, the slowest filling the column, so the laps compare at a glance.
         g.setColor(BARS[lap == fastest ? 0 : lap == slowest ? 1 : 2]);
         int bar = (int) ((columns[2] + box.width / 6 - columns[1] - rowHeight / 2) * watch.lapTime(lap) / Math.max(1, longest));
         g.fillRoundRect(columns[1] - rowHeight / 4, y + rowHeight / 8, bar, rowHeight * 3 / 4, rowHeight / 3, rowHeight / 3);
         g.setColor(lap == fastest ? Ui.FASTEST : lap == slowest ? Ui.SLOWEST : Ui.INK);
         g.setFont(font);
         int baseline = y + (rowHeight + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
         String tag = lap == fastest ? "  fastest" : lap == slowest ? "  slowest" : "";
         g.drawString("Lap " + (lap + 1), columns[0], baseline);
         g.drawString(Ui.stopwatch(watch.lapTime(lap)) + tag, columns[1], baseline);
         g.drawString(Ui.stopwatch(watch.lapEnd(lap)), columns[2] + box.width / 6, baseline);
      }
   }

   @Override
   public void press(int x, int y, boolean rightButton, long now) {
      int button = Ui.indexOf(this.buttons, Ui.hit(this.buttons, x, y));
      if (button == 0) {
         this.stopwatch().toggle(now);
      } else if (button == 1) {
         this.lapOrReset(now);
      }
   }

   @Override
   public void drag(int x, int y, long now) {
   }

   @Override
   public void release(int x, int y, long now) {
   }

   @Override
   public boolean key(int keyCode, long now) {
      switch (keyCode) {
         case KeyEvent.VK_SPACE:
         case KeyEvent.VK_ENTER:
            this.stopwatch().toggle(now);
            return true;
         case KeyEvent.VK_L:
            this.stopwatch().lap(now);
            return true;
         case KeyEvent.VK_R:
            this.stopwatch().reset();
            return true;
         default:
            return false;
      }
   }

   private void lapOrReset(long now) {
      if (this.stopwatch().isRunning()) {
         this.stopwatch().lap(now);
      } else {
         this.stopwatch().reset();
      }
   }
}
